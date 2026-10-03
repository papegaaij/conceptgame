package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A level in play, advanced in fixed 60 Hz steps by {@link #step(int)}: the launch, the scroll
 * through the sections, the ship and its {@link PlayerFire}, the {@link EnemyForce} of the
 * {@link LevelScript}, its {@link SetPiece}s, the ground objects, the {@link Debris} chunks, the
 * pickups, the {@link Radio} cues, the {@link Objectives} and the tally. All entities live in pools allocated up front and the waves
 * are planned when the sortie is created, so stepping does not allocate, and the same seed and
 * commands give the same {@link #stateHash()} on every platform.
 *
 * <p>When armour reaches zero the ship explodes and the level runs on without it until the
 * presentation, after the mission failed screen, restarts it with {@link #retry(double)} from the
 * level-start state (design/systems/retry): what the attempt earned is lost. The level is over
 * when the scroll reaches the end of the last section; the ship then flies on, out of harm's way,
 * until the presentation moves to the debrief.
 *
 * <p>A level with an {@code escort} primary objective has a {@link Convoy} on its road; it fails
 * when the last unit is lost (design/systems/retry, on a failed primary objective): the ship flies
 * on but nothing can hurt it, and the presentation retries the level as after a wreck.
 */
public final class Sortie {
    private static final int GROUND_CAPACITY = 32;
    private static final int PICKUP_CAPACITY = 64;
    private static final int EVENT_CAPACITY = 256;
    private static final int DEBRIS_CAPACITY = 24;

    private final SplitMix64 rng;
    private final Ship ship;
    private final LevelScript script;
    private final Rules rules;
    private final PlayerFire fire;
    private final SpecialSlot special;
    private final EnemyForce force;
    private final Pool<GroundObject> ground = new Pool<>(GROUND_CAPACITY, GroundObject::new, GroundObject[]::new);
    private final Pool<Pickup> pickups = new Pool<>(PICKUP_CAPACITY, Pickup::new, Pickup[]::new);
    private final Pool<Debris> debris = new Pool<>(DEBRIS_CAPACITY, Debris::new, Debris[]::new);
    private final SetPiece[] setPieces;
    /** What the guns and the Airstrike destroy is handed here. */
    private final PlayerFire.Hits hits;
    /** Per secret, the triggers spent that reveal it together (Level 03's lifeboat lights). */
    private final int[] secretTriggersSpent;

    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private final Tally tally;
    private final Objectives objectives;
    private final Radio radio;
    private final Crane[] cranes;
    /** Whether a group's outcome was paid and called in this attempt. */
    private final boolean[] groupCalled;
    /** The escort objective's convoy; null without one. */
    private final Convoy convoy;

    private final int launchTicks;
    private final int endTicks;
    private final int pickupTicks;
    private long tick;
    private int levelTick;
    private int attempt = 1;
    private boolean wrecked;
    /** Whether the primary objective failed in this attempt (the convoy is lost). */
    private boolean failed;
    /** What the convoy earned at the level end, for the debrief. */
    private int escortCredits;
    /** The armour of the next attempt, once {@link #retry(double)} asked for one; 0 while none is asked for. */
    private double retryArmour;

    private double groundScroll;
    private int nextGroundObject;
    private int nextDebris;
    private int edgeWarnings;
    private boolean complete;

    /** @param armour the ship's armour at the level start (design/systems/retry: not full, unless it was full) */
    public Sortie(long seed, Loadout loadout, LevelScript script, Rules rules, double armour) {
        if (rules.bulletBudget() > EnemyForce.BULLET_CAPACITY) {
            throw new IllegalArgumentException("the bullet budget exceeds the pool of " + EnemyForce.BULLET_CAPACITY);
        }
        rng = new SplitMix64(seed);
        ship = new Ship(
                loadout.ship(),
                new Defences(loadout.shield(), loadout.plating(), loadout.ship().mercySeconds()));
        this.script = script;
        this.rules = rules;
        setPieces = script.setPieces().stream().map(SetPiece::new).toArray(SetPiece[]::new);
        secretTriggersSpent = new int[script.secrets()];
        PlayerFire.Hits hits = new PlayerFire.Hits() {
            @Override
            public void enemyDestroyed(int index) {
                destroy(index);
            }

            @Override
            public void groundDestroyed(int index) {
                demolish(index);
            }

            @Override
            public void triggerReleased(int index) {
                released(ground.get(index));
            }

            @Override
            public void mineDestroyed(int index) {
                shootMine(index);
            }

            @Override
            public void partDestroyed(int piece, int part) {
                wreck(piece, part);
            }
        };
        this.hits = hits;
        fire = new PlayerFire(ship, loadout.armament(), events, hits, setPieces);
        special = new SpecialSlot(loadout.special(), events);
        force = new EnemyForce(script.waves(), script.groundUnits(), rng, rules, events, new EnemyForce.Escapes() {
            @Override
            public void escaped(Enemy enemy) {
                Sortie.this.escaped(enemy);
            }

            @Override
            public void burst(Enemy enemy) {
                selfBurst(enemy);
            }
        });
        tally = new Tally(rules.scoring());
        int escapers = force.unitsOf(script.secondary().escapes());
        for (SetPiece piece : setPieces) {
            escapers += piece.slug().equals(script.secondary().escapes()) ? 1 : 0;
        }
        objectives =
                new Objectives(script.secondary(), force.kinds().size(), enemyTotal(), script.groundUnits(), escapers);
        cranes = script.cranes().stream().map(Crane::new).toArray(Crane[]::new);
        groupCalled = new boolean[script.secondary().groups().size()];
        radio = new Radio(script.radio(), events, ship, special.fitted());
        convoy = script.escort()
                .map(escort -> new Convoy(escort, script.road().orElseThrow()))
                .orElse(null);
        if (convoy != null) {
            force.hook(convoy, convoy.escort().targetedBy());
        }
        launchTicks = SimStep.ticks(script.launchSeconds());
        endTicks = SimStep.ticks(script.seconds());
        pickupTicks = SimStep.ticks(rules.pickups().seconds());
        startAttempt(armour);
    }

    /** Advances the sortie by one step with the given {@link Command} set. */
    public void step(int commands) {
        tick++;
        events.clear();
        if (retryArmour > 0) {
            restart();
        }
        double scrollStep = scrollSpeed() * SimStep.SECONDS;
        levelTick++;
        groundScroll += scrollStep;
        ship.rememberPosition();
        if (launching()) {
            ship.launch((double) levelTick / launchTicks);
        } else if (flying()) {
            ship.fly(commands);
            fire.fire(commands, force.enemies(), ground);
        }
        special.command(commands, !launching() && flying() && !complete, ship.x(), ship.y());
        tally.step();
        if (!complete) {
            force.spawn(levelTick);
            placeGroundObjects();
            placeDebris();
            radio.byTime(levelTick);
        }
        for (Crane crane : cranes) {
            crane.update(levelTick);
        }
        flySetPieces();
        edgeWarnings = complete ? 0 : force.warnings(levelTick);
        fire.move(scrollStep, force.enemies());
        boolean firing = flying() && !complete;
        force.move(ship, firing, scrollStep);
        fireSetPieces(firing);
        force.moveBullets();
        force.moveMines(ship);
        moveDebris();
        blockShots();
        scrollGround(scrollStep);
        driftPickups();
        fire.hitEnemies(force.enemies());
        fire.hitSetPieces();
        fire.hitMines(force.mines());
        fire.hitGround(ground, force.enemies());
        special.update(scrollStep, force.enemies(), ground, setPieces, hits);
        if (convoy != null) {
            convoy.update(levelTick, groundScroll, scrollStep);
        }
        if (flying() && !complete && !failed && !rules.invulnerableShip()) {
            hitShip();
            ramShip();
            hitByCranes();
            hitByDebris();
            hitBySetPieces();
        }
        if (convoy != null && !complete && !wrecked && !failed) {
            hitAllies();
        }
        if (flying()) {
            collectPickups();
        }
        if (!complete && !wrecked && !failed && levelTick >= endTicks) {
            completeLevel();
        }
    }

    /**
     * Restarts the level at the next step from its start state, after the ship's destruction or from
     * the pause menu (design/systems/retry): a new attempt with {@code armour} points of armour, and
     * what this one earned is lost.
     */
    public void retry(double armour) {
        if (!(armour > 0)) {
            throw new IllegalArgumentException("a retry needs armour");
        }
        retryArmour = armour;
    }

    private void startAttempt(double armour) {
        ship.reset(armour);
        if (launchTicks > 0) {
            ship.launch(0);
            ship.rememberPosition();
        }
    }

    private void restart() {
        fire.reset();
        special.reset();
        force.reset();
        Pools.clear(ground);
        Pools.clear(pickups);
        Pools.clear(debris);
        for (SetPiece piece : setPieces) {
            piece.reset();
        }
        Arrays.fill(secretTriggersSpent, 0);
        tally.reset();
        objectives.reset();
        radio.reset();
        for (Crane crane : cranes) {
            crane.reset();
        }
        Arrays.fill(groupCalled, false);
        levelTick = 0;
        groundScroll = 0;
        nextGroundObject = 0;
        nextDebris = 0;
        complete = false;
        wrecked = false;
        failed = false;
        escortCredits = 0;
        if (convoy != null) {
            convoy.reset();
        }
        attempt++;
        startAttempt(retryArmour);
        retryArmour = 0;
        events.add(SimEvents.Type.SORTIE_RESTARTED, ship.x(), ship.y());
    }

    private double scrollSpeed() {
        return script.sections().get(section() - 1).speed();
    }

    private void placeGroundObjects() {
        List<LevelScript.GroundObjectSpec> objects = script.groundObjects();
        while (nextGroundObject < objects.size()
                && SimStep.ticks(objects.get(nextGroundObject).t()) <= levelTick) {
            GroundObject object = ground.obtain();
            if (object != null) {
                object.place(objects.get(nextGroundObject), nextGroundObject);
            }
            nextGroundObject++;
        }
    }

    /**
     * Lets in the debris chunks due now, each at its x unless that is closer to the ship than its
     * clearance: then it enters as far to the side, away from the ship.
     */
    private void placeDebris() {
        List<LevelScript.DebrisSpec> specs = script.debris();
        while (nextDebris < specs.size() && SimStep.ticks(specs.get(nextDebris).t()) <= levelTick) {
            Debris chunk = debris.obtain();
            if (chunk != null) {
                LevelScript.DebrisSpec spec = specs.get(nextDebris);
                chunk.place(spec, nextDebris, clearOfShip(spec));
            }
            nextDebris++;
        }
    }

    /** Where a chunk enters: its x, or moved sideways away from the ship to its clearance. */
    private double clearOfShip(LevelScript.DebrisSpec spec) {
        double dy = PlayField.HEIGHT + spec.size().height() / 2 - ship.y();
        double dx = spec.x() - ship.x();
        double clearance = spec.clearance();
        if (dx * dx + dy * dy >= clearance * clearance) {
            return spec.x();
        }
        double shift = Math.sqrt(clearance * clearance - dy * dy);
        double half = spec.size().width() / 2;
        double away = Math.clamp(ship.x() + (dx >= 0 ? shift : -shift), half, PlayField.WIDTH - half);
        if (Math.abs(away - ship.x()) >= shift) {
            return away;
        }
        return Math.clamp(ship.x() + (dx >= 0 ? -shift : shift), half, PlayField.WIDTH - half);
    }

    private void moveDebris() {
        for (int i = debris.size() - 1; i >= 0; i--) {
            if (!debris.get(i).move()) {
                debris.free(i);
            }
        }
    }

    /**
     * The set pieces fly their passes: one reaching the player's layer is announced, one ending its
     * last pass alive has escaped (its line, and an escapes objective about it fails).
     */
    private void flySetPieces() {
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            boolean onPlane = piece.onPlane();
            if (piece.update(levelTick)) {
                events.add(SimEvents.Type.SET_PIECE_ESCAPED, piece.x(), piece.y(), k);
                if (objectives.escapeLost(piece.slug())) {
                    events.add(SimEvents.Type.OBJECTIVE_FAILED, piece.x(), piece.y());
                }
                radio.cue(LevelScript.CueTrigger.ENEMY_ESCAPED, piece.slug());
            } else if (!onPlane && piece.onPlane()) {
                events.add(SimEvents.Type.SET_PIECE_DESCENDED, piece.x(), piece.y(), k);
            }
        }
    }

    /** The set pieces' living parts fire their guns while on the player's layer. */
    private void fireSetPieces(boolean firing) {
        for (SetPiece piece : setPieces) {
            List<LevelScript.PartSpec> parts = piece.spec().parts();
            for (int p = 0; p < parts.size(); p++) {
                if (piece.trigger(p) && firing) {
                    force.fireFrom(
                            piece.partX(p), piece.partY(p), parts.get(p).gun().orElseThrow(), ship);
                }
            }
        }
    }

    private void scrollGround(double distance) {
        for (int i = ground.size() - 1; i >= 0; i--) {
            if (!ground.get(i).scroll(distance)) {
                ground.free(i);
            }
        }
    }

    private void driftPickups() {
        double distance = rules.pickups().driftSpeed() * SimStep.SECONDS;
        for (int i = pickups.size() - 1; i >= 0; i--) {
            if (!pickups.get(i).drift(distance)) {
                pickups.free(i);
            }
        }
    }

    /** How far to the right of a destroyed object its bonus drop lands. */
    private static final double BONUS_DROP_OFFSET = 16;

    private void demolish(int index) {
        GroundObject object = ground.get(index);
        LevelScript.GroundObjectSpec spec = object.spec();
        events.add(
                SimEvents.Type.GROUND_DESTROYED,
                object.x(),
                object.y(),
                script.groundObjects().indexOf(spec));
        tally.earn(CreditSource.GROUND_TARGETS, spec.bounty());
        tally.scoreValue(spec.bounty());
        if (spec.drop().isPresent()) {
            drop(spec.drop().get(), object.x(), object.y());
        }
        if (spec.bonusDrop().isPresent()) {
            // Beside the first, so both show.
            drop(spec.bonusDrop().get(), object.x() + BONUS_DROP_OFFSET, object.y());
        }
        if (spec.secretIndex() >= 0) {
            // A destructible that hides a secret (Level 04's dugout) reveals it when destroyed.
            released(object);
        }
        ground.free(index);
    }

    /**
     * A trigger took its last hit: it reveals its secret, or, when several triggers reveal it
     * together, counts towards it and the last one reveals it.
     */
    private void released(GroundObject trigger) {
        LevelScript.GroundObjectSpec spec = trigger.spec();
        if (spec.secretTriggers() > 1 && ++secretTriggersSpent[spec.secretIndex()] < spec.secretTriggers()) {
            return;
        }
        revealSecret(trigger);
    }

    private void revealSecret(GroundObject trigger) {
        objectives.secretFound();
        LevelScript.GroundObjectSpec spec = trigger.spec();
        events.add(SimEvents.Type.SECRET_FOUND, trigger.x(), trigger.y());
        Pickup crate = pickups.obtain();
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, spec.crateCredits(), trigger.x(), trigger.y(), pickupTicks);
        }
        radio.cue(LevelScript.CueTrigger.SECRET, spec.secret());
    }

    private void destroy(int index) {
        Pool<Enemy> enemies = force.enemies();
        Enemy enemy = enemies.get(index);
        EnemySpec spec = enemy.spec();
        events.add(SimEvents.Type.ENEMY_DESTROYED, enemy.x(), enemy.y(), enemy.kind());
        tally.kill(spec.bounty(), enemy.grounded() ? CreditSource.GROUND_TARGETS : CreditSource.KILLS);
        int kills = objectives.kill(enemy.kind());
        int group = enemy.group();
        if (enemy.carried().isPresent()) {
            drop(enemy.carried().get(), enemy.x(), enemy.y());
        }
        if (spec.drop().isPresent() && kills % spec.drop().get().every() == 0) {
            drop(spec.drop().get().pickup(), enemy.x(), enemy.y());
        }
        if (spec.deathBurst().isPresent()) {
            force.deathBurst(enemy, ship);
        }
        if (spec.brood().isPresent()) {
            force.hatch(enemy, ship);
        }
        if (enemy.walking()) {
            events.add(
                    SimEvents.Type.WALKER_DOWN,
                    enemy.x(),
                    enemy.y(),
                    SimEvents.walkerValue(enemy.kind(), enemy.facing()));
        }
        enemies.free(index);
        if (kills == 1) {
            radio.cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        }
        if (group >= 0) {
            decided(group, objectives.groupUnitDestroyed(group));
        }
        if (objectives.escapeDestroyed(spec.slug())) {
            paySecondary();
        }
        if (objectives.meetsSecondary(tally.kills())) {
            paySecondary();
        }
    }

    /** A spore mine was shot: it pays its credits (no kill, no chain). */
    private void shootMine(int index) {
        Pool<Mine> mines = force.mines();
        Mine mine = mines.get(index);
        events.add(SimEvents.Type.MINE_DESTROYED, mine.x(), mine.y());
        int credits = mine.spec().credits();
        tally.earn(CreditSource.KILLS, credits);
        tally.scoreValue(credits);
        mines.free(index);
    }

    /**
     * Part {@code part} of set piece {@code k} was destroyed: it pays its bounty; the vital part
     * takes the rest with it, paying theirs, and the unit is destroyed.
     */
    private void wreck(int k, int part) {
        SetPiece piece = setPieces[k];
        payPart(piece, part);
        if (!piece.spec().parts().get(part).vital()) {
            return;
        }
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.wreckPart(p)) {
                payPart(piece, p);
            }
        }
        LevelScript.SetPieceSpec spec = piece.spec();
        events.add(SimEvents.Type.SET_PIECE_DESTROYED, piece.x(), piece.y(), k);
        tally.countKill();
        if (spec.drop().isPresent()) {
            double half = EnemyGun.BULLET.width();
            drop(
                    spec.drop().get(),
                    Math.clamp(piece.x(), half, PlayField.WIDTH - half),
                    Math.clamp(piece.y(), half, PlayField.HEIGHT - half));
        }
        piece.destroy();
        radio.cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        if (objectives.escapeDestroyed(spec.slug())) {
            paySecondary();
        }
        if (objectives.meetsSecondary(tally.kills())) {
            paySecondary();
        }
    }

    private void payPart(SetPiece piece, int part) {
        tally.partKill(piece.spec().parts().get(part).bounty());
        events.add(SimEvents.Type.PART_DESTROYED, piece.partX(part), piece.partY(part), part);
    }

    /** The secondary objective is met: its credits, its event and its line. */
    private void paySecondary() {
        int credits = script.secondary().credits();
        tally.earn(CreditSource.OBJECTIVES, credits);
        tally.scoreValue(credits);
        events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
        radio.cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
    }

    /**
     * A spawner burst on its own (design/enemies/air/brood-pod): it pays its burst bounty, which is
     * not a kill (no kill counter, no chain), and counts as an escape for an escapes objective.
     */
    private void selfBurst(Enemy enemy) {
        int credits = enemy.spec().brood().orElseThrow().burstBounty();
        tally.unchained(credits);
        events.add(SimEvents.Type.BROOD_BURST, enemy.x(), enemy.y(), enemy.kind());
        escaped(enemy);
    }

    /**
     * A unit left the screen alive: a ground group's unit can lose its group, one of an escapes
     * objective's enemy fails it, and the first of an enemy calls its line.
     */
    private void escaped(Enemy enemy) {
        String slug = enemy.spec().slug();
        if (enemy.group() >= 0) {
            decided(enemy.group(), objectives.groupUnitEscaped(enemy.group()));
        }
        if (objectives.escapeLost(slug)) {
            events.add(SimEvents.Type.OBJECTIVE_FAILED, enemy.x(), enemy.y());
        }
        radio.cue(LevelScript.CueTrigger.ENEMY_ESCAPED, slug);
    }

    /**
     * A group's outcome once it is decided: a cleared group pays its credits and calls its line,
     * the objective is met when every group is cleared; a lost group calls its line, and the first
     * lost one also the first-loss line.
     */
    private void decided(int group, int state) {
        String name = script.secondary().groups().get(group);
        if (state == Objectives.CLEARED && !groupCalled[group]) {
            groupCalled[group] = true;
            int credits = script.secondary().credits();
            tally.earn(CreditSource.OBJECTIVES, credits);
            tally.scoreValue(credits);
            events.add(SimEvents.Type.GROUP_CLEARED, ship.x(), ship.y(), group);
            radio.cue(LevelScript.CueTrigger.GROUP_CLEARED, name);
            if (objectives.secondaryMet()) {
                events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
                radio.cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
            }
        } else if (state == Objectives.LOST && !groupCalled[group]) {
            groupCalled[group] = true;
            events.add(SimEvents.Type.GROUP_LOST, ship.x(), ship.y(), group);
            radio.cue(LevelScript.CueTrigger.GROUP_LOST, name);
            if (objectives.groupsLost() == 1) {
                radio.cue(LevelScript.CueTrigger.FIRST_GROUP_LOST, "");
            }
        }
    }

    private void drop(PickupType type, double x, double y) {
        if (type == PickupType.SPECIAL_CHARGE && !special.fitted()) {
            return;
        }
        Pickup pickup = pickups.obtain();
        if (pickup != null) {
            int credits =
                    switch (type) {
                        case SMALL_SALVAGE -> rules.pickups().smallSalvageCredits();
                        case MEDIUM_SALVAGE -> rules.pickups().mediumSalvageCredits();
                        case LARGE_SALVAGE -> rules.pickups().largeSalvageCredits();
                        default -> 0;
                    };
            pickup.drop(type, credits, x, y, pickupTicks);
        }
    }

    /** The cranes' arms stop every shot that touches them; the player's hits on a swinging arm's clamp count. */
    private void blockShots() {
        Pool<Shot> shots = fire.shots();
        Pool<EnemyBullet> bullets = force.bullets();
        for (Crane crane : cranes) {
            if (!crane.present()) {
                continue;
            }
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                Hitbox size = shot.weapon().size();
                if (shot.weapon().delivery().landing()) {
                    continue;
                }
                if (crane.clampHit(shot.x(), shot.y(), size)) {
                    events.add(SimEvents.Type.CLAMP_HIT, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                    if (crane.countClampHit()) {
                        releaseCrate(crane);
                    }
                } else if (crane.touches(shot.x(), shot.y(), size.width() / 2, size.height() / 2)) {
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                }
            }
            for (int i = bullets.size() - 1; i >= 0; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (crane.touches(bullet.x(), bullet.y(), EnemyGun.BULLET.width() / 2, EnemyGun.BULLET.height() / 2)) {
                    bullets.free(i);
                }
            }
        }
        blockByDebris(shots, bullets);
    }

    /**
     * The debris chunks stop every shot and enemy bullet that touches them: shots glance off a
     * large chunk and damage a small one, which breaks after its HP.
     */
    private void blockByDebris(Pool<Shot> shots, Pool<EnemyBullet> bullets) {
        for (int d = debris.size() - 1; d >= 0; d--) {
            Debris chunk = debris.get(d);
            boolean broken = false;
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                if (shot.weapon().delivery().landing()
                        || !chunk.touches(shot.x(), shot.y(), shot.weapon().size())) {
                    continue;
                }
                events.add(
                        chunk.large() ? SimEvents.Type.SHOT_GLANCED : SimEvents.Type.DEBRIS_HIT,
                        shot.x(),
                        shot.y(),
                        shot.mount());
                broken = chunk.damage(shot.damage());
                shots.free(i);
                if (broken) {
                    events.add(SimEvents.Type.DEBRIS_DESTROYED, chunk.x(), chunk.y());
                    debris.free(d);
                    break;
                }
            }
            if (broken) {
                continue;
            }
            for (int i = bullets.size() - 1; i >= 0; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (chunk.touches(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                    bullets.free(i);
                }
            }
        }
    }

    /** A large debris chunk touching the hull deals its contact damage, at most once per its interval. */
    private void hitByDebris() {
        Hull hull = ship.spec().hull();
        for (int d = 0; d < debris.size(); d++) {
            Debris chunk = debris.get(d);
            LevelScript.DebrisSpec spec = chunk.spec();
            if (spec.damage() > 0
                    && hull.overlaps(ship.x(), ship.y(), spec.size(), chunk.x(), chunk.y())
                    && chunk.strike()) {
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeCollision(spec.damage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /** A set piece's body on the player's layer touching the hull deals its contact damage, at most once per its interval. */
    private void hitBySetPieces() {
        Hull hull = ship.spec().hull();
        for (SetPiece piece : setPieces) {
            LevelScript.SetPieceSpec spec = piece.spec();
            if (piece.present()
                    && piece.onPlane()
                    && hull.overlaps(ship.x(), ship.y(), spec.body(), piece.x(), piece.y())
                    && piece.strike()) {
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeCollision(spec.contactDamage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /** A crane's clamp let go: its secret's crate drops from the hook. */
    private void releaseCrate(Crane crane) {
        objectives.secretFound();
        LevelScript.CraneSpec spec = crane.spec();
        events.add(SimEvents.Type.SECRET_FOUND, crane.tipX(), crane.tipY());
        Pickup crate = pickups.obtain();
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, spec.crateCredits(), crane.tipX(), crane.tipY(), pickupTicks);
        }
        radio.cue(LevelScript.CueTrigger.SECRET, spec.secret());
    }

    /** A crane's arm touching the hull deals its damage, shield first, at most once per its interval. */
    private void hitByCranes() {
        Hull hull = ship.spec().hull();
        for (Crane crane : cranes) {
            if (!crane.present()) {
                continue;
            }
            for (int p = 0; p < hull.parts().size(); p++) {
                Hull.Part part = hull.parts().get(p);
                if (crane.touches(
                                ship.x() + part.dx(),
                                ship.y() + part.dy(),
                                part.box().width() / 2,
                                part.box().height() / 2)
                        && crane.strike()) {
                    double lost = ship.defences().armourLost();
                    if (damaged(lost, ship.defences().takeShot(crane.spec().damage(), events, ship.x(), ship.y()))) {
                        return;
                    }
                    break;
                }
            }
        }
    }

    private void hitShip() {
        Pool<EnemyBullet> bullets = force.bullets();
        Hull hull = ship.spec().hull();
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            if (hull.overlaps(ship.x(), ship.y(), EnemyGun.BULLET, bullet.x(), bullet.y())) {
                bullets.free(i);
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeShot(bullet.damage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
        // An armed spore bursts on contact with the hull, dealing its layer's attack damage.
        Pool<Mine> mines = force.mines();
        for (int i = mines.size() - 1; i >= 0; i--) {
            Mine mine = mines.get(i);
            if (mine.armed() && hull.overlaps(ship.x(), ship.y(), EnemyGun.MineSpec.BOX, mine.x(), mine.y())) {
                double damage = mine.gun().damage();
                events.add(SimEvents.Type.MINE_BURST, mine.x(), mine.y());
                mines.free(i);
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeShot(damage, events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /** A rammer on the player's layer deals its contact damage; a small one is destroyed by the impact. */
    private void ramShip() {
        Pool<Enemy> enemies = force.enemies();
        Hull hull = ship.spec().hull();
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            EnemySpec spec = enemy.spec();
            if (spec.layer().collidesWithPlayer()
                    && hull.overlaps(ship.x(), ship.y(), spec.hitbox(), enemy.x(), enemy.y())) {
                double lost = ship.defences().armourLost();
                boolean wrecked = ship.defences().takeCollision(spec.contactDamage(), events, ship.x(), ship.y());
                if (spec.destroyedByRamming()) {
                    destroy(j);
                }
                if (damaged(lost, wrecked)) {
                    return;
                }
            }
        }
    }

    /**
     * After a hit: armour damage (more armour lost than {@code lostBefore}) ends the chain, and a
     * hit that took the last armour wrecks the ship. Returns whether it did.
     */
    private boolean damaged(double lostBefore, boolean destroyed) {
        if (ship.defences().armourLost() > lostBefore) {
            tally.breakChain();
        }
        if (destroyed) {
            wrecked = true;
            events.add(SimEvents.Type.SHIP_DESTROYED, ship.x(), ship.y());
        }
        return destroyed;
    }

    /**
     * The convoy takes its damage (design/allies, civilian crawler): an enemy shot the
     * target-the-objective hook aimed at a unit hits the first unit it touches and is spent (shots
     * aimed at the ship pass over), and a walker (a ground unit that is not fixed to the ground)
     * claws every unit its hit box overlaps, per second.
     */
    private void hitAllies() {
        AllySpec spec = convoy.escort().ally();
        if (spec.objectiveAimed()) {
            Pool<EnemyBullet> bullets = force.bullets();
            for (int i = bullets.size() - 1; i >= 0 && !failed; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (!bullet.objectiveAimed()) {
                    continue;
                }
                int k = convoy.touching(bullet.x(), bullet.y(), EnemyGun.BULLET);
                if (k >= 0) {
                    bullets.free(i);
                    hurt(k, bullet.damage(), true);
                }
            }
        }
        if (spec.clawsPerSecond() > 0) {
            double claws = spec.clawsPerSecond() * SimStep.SECONDS;
            Pool<Enemy> enemies = force.enemies();
            for (int j = 0; j < enemies.size() && !failed; j++) {
                Enemy enemy = enemies.get(j);
                if (enemy.spec().layer() != Layer.GROUND || enemy.grounded()) {
                    continue;
                }
                for (int k = 0; k < convoy.size() && !failed; k++) {
                    if (convoy.touches(k, enemy.x(), enemy.y(), enemy.spec().hitbox())) {
                        // A new contact flashes and sounds; a continuing one only takes its damage.
                        hurt(k, claws, convoy.get(k).ticksSinceHit() > 1);
                    }
                }
            }
        }
    }

    /** Unit {@code k} of the convoy takes {@code damage}: the first hit's line, a loss's line, the failure. */
    private void hurt(int k, double damage, boolean announce) {
        Ally ally = convoy.get(k);
        boolean first = convoy.firstHit() < 0;
        boolean destroyed = convoy.damage(k, damage);
        if (announce || destroyed) {
            events.add(SimEvents.Type.ALLY_HIT, ally.x(), ally.y(), k);
        }
        if (first) {
            radio.cue(LevelScript.CueTrigger.FIRST_ALLY_HIT, "");
        }
        if (!destroyed) {
            return;
        }
        events.add(SimEvents.Type.ALLY_LOST, ally.x(), ally.y(), k);
        if (convoy.firstLost() == k && convoy.alive() == convoy.size() - 1) {
            radio.cue(LevelScript.CueTrigger.FIRST_ALLY_LOST, "");
        }
        if (convoy.allLost()) {
            failed = true;
            events.add(SimEvents.Type.PRIMARY_FAILED, ship.x(), ship.y());
            radio.cue(LevelScript.CueTrigger.MISSION_FAILED, "");
        }
    }

    private void collectPickups() {
        double radius = rules.pickups().collectionRadius();
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pickup = pickups.get(i);
            double dx = pickup.x() - ship.x();
            double dy = pickup.y() - ship.y();
            if (dx * dx + dy * dy <= radius * radius) {
                apply(pickup);
                pickups.free(i);
            }
        }
    }

    private void apply(Pickup pickup) {
        Defences defences = ship.defences();
        switch (pickup.type()) {
            case SMALL_SALVAGE, MEDIUM_SALVAGE, LARGE_SALVAGE -> payPickup(CreditSource.SALVAGE, pickup);
            case OVERDRIVE -> fire.overdrive(SimStep.ticks(rules.pickups().overdriveSeconds()));
            case HIDDEN_CRATE -> payPickup(CreditSource.SECRETS, pickup);
            case SHIELD_CELL -> defences.restoreShield(rules.pickups().shieldCellShare() * defences.maxShield());
            case ARMOUR_PATCH -> defences.repair(rules.pickups().armourPatch());
            case SPECIAL_CHARGE -> special.collect();
        }
        events.add(
                SimEvents.Type.PICKUP_COLLECTED,
                pickup.x(),
                pickup.y(),
                pickup.type().ordinal());
    }

    private void payPickup(CreditSource source, Pickup pickup) {
        int paid = tally.earn(source, pickup.credits());
        tally.scoreValue(pickup.credits());
        events.add(SimEvents.Type.CREDITS_PICKED_UP, pickup.x(), pickup.y(), paid);
    }

    private void completeLevel() {
        complete = true;
        events.add(SimEvents.Type.LEVEL_COMPLETE, ship.x(), ship.y());
        int home = convoy == null ? 0 : convoy.alive();
        if (convoy != null) {
            // Each unit home is a payout of its own (design/campaign, Level 04: the escort objective).
            int credits = convoy.escort().credits();
            for (int k = 0; k < home; k++) {
                escortCredits += tally.earn(CreditSource.OBJECTIVES, credits);
                tally.scoreValue(credits);
            }
        }
        radio.end(home);
    }

    /** A hash over the complete state; equal hashes mean equal replays. */
    public long stateHash() {
        StateHash hash = new StateHash()
                .add(tick)
                .add(levelTick)
                .add(attempt)
                .add(wrecked ? 1 : 0)
                .add(groundScroll)
                .add(nextGroundObject)
                .add(nextDebris)
                .add(objectives.secretsFound())
                .add(objectives.secondaryMet() ? 1 : 0)
                .add(complete ? 1 : 0)
                .add(rng.state());
        ship.addTo(hash);
        fire.addTo(hash);
        special.addTo(hash);
        tally.addTo(hash);
        objectives.addKillsTo(hash);
        for (Crane crane : cranes) {
            crane.addTo(hash);
        }
        for (SetPiece piece : setPieces) {
            piece.addTo(hash);
        }
        for (int spent : secretTriggersSpent) {
            hash.add(spent);
        }
        Pools.addAll(hash, fire.shots());
        Pools.addAll(hash, force.enemies());
        Pools.addAll(hash, force.bullets());
        Pools.addAll(hash, ground);
        Pools.addAll(hash, pickups);
        Pools.addAll(hash, force.mines());
        Pools.addAll(hash, debris);
        if (convoy != null) {
            hash.add(failed ? 1 : 0).add(escortCredits);
            convoy.addTo(hash);
        }
        return hash.value();
    }

    /** The totals for the debrief; meaningful once the level is {@link #complete()}. */
    public LevelResult result() {
        Defences defences = ship.defences();
        return LevelResult.of(
                rules.scoring(),
                script,
                tally,
                enemyTotal(),
                defences.armourLost(),
                defences.maxArmour(),
                objectives.secretsFound(),
                objectives.secondaryMet(),
                convoy == null
                        ? LevelResult.Escort.NONE
                        : new LevelResult.Escort(
                                convoy.escort().ally().slug(), convoy.alive(), convoy.size(), escortCredits));
    }

    public LevelScript script() {
        return script;
    }

    /** The distinct enemies of the level; {@link Enemy#kind()} and enemy events index this list. */
    public List<EnemySpec> enemyKinds() {
        return force.kinds();
    }

    public Ship ship() {
        return ship;
    }

    /** The fitted weapons; shot events and {@link Shot#mount()} index its mounts. */
    public Armament armament() {
        return fire.armament();
    }

    /** Steps since mount {@code m} last fired, for its muzzle flash; {@link Integer#MAX_VALUE} before its first shot. */
    public int ticksSinceShot(int m) {
        return fire.ticksSinceShot(m);
    }

    /** The special slot: its charges, and the Airstrike in flight for the presentation. */
    public SpecialSlot special() {
        return special;
    }

    /** Seconds of overdrive left; 0 without one. */
    public double overdriveSeconds() {
        return fire.overdriveTicks() * SimStep.SECONDS;
    }

    /** Drops a spore of {@code gun}'s mine at (x, y) drifting at {@code angle}, as a mine layer does (for tests). */
    void dropMine(EnemyGun gun, double x, double y, double angle) {
        force.dropMine(gun, x, y, angle);
    }

    /** Starts an overdrive of {@code seconds}, as its pickup does; a running one starts over. */
    void overdrive(double seconds) {
        fire.overdrive(SimStep.ticks(seconds));
    }

    public int shotCount() {
        return fire.shots().size();
    }

    public Shot shot(int index) {
        return fire.shots().get(index);
    }

    public int enemyCount() {
        return force.enemies().size();
    }

    public Enemy enemy(int index) {
        return force.enemies().get(index);
    }

    public int bulletCount() {
        return force.bullets().size();
    }

    public EnemyBullet bullet(int index) {
        return force.bullets().get(index);
    }

    public int groundObjectCount() {
        return ground.size();
    }

    public GroundObject groundObject(int index) {
        return ground.get(index);
    }

    public int pickupCount() {
        return pickups.size();
    }

    public Pickup pickup(int index) {
        return pickups.get(index);
    }

    /** The spore mines drifting on the screen (below the player's layer until {@link Mine#armed()}). */
    public int mineCount() {
        return force.mines().size();
    }

    public Mine mine(int index) {
        return force.mines().get(index);
    }

    /** The debris chunks on the screen. */
    public int debrisCount() {
        return debris.size();
    }

    public Debris debris(int index) {
        return debris.get(index);
    }

    /** The level's set pieces, present or not; {@link SetPiece#present()} tells whether one is on screen. */
    public int setPieceCount() {
        return setPieces.length;
    }

    public SetPiece setPiece(int index) {
        return setPieces[index];
    }

    public int craneCount() {
        return cranes.length;
    }

    public Crane crane(int index) {
        return cranes[index];
    }

    /** The groups of a group objective (Level 02's docks); 0 otherwise. */
    public int groupCount() {
        return objectives.groupCount();
    }

    /** Whether group {@code g} is still open (0), cleared (1) or lost (2). */
    public int groupState(int g) {
        return objectives.groupState(g);
    }

    /** The units of the escort objective's convoy; 0 without one. */
    public int allyCount() {
        return convoy == null ? 0 : convoy.size();
    }

    /** Convoy unit {@code k}, the leading one first. */
    public Ally ally(int k) {
        return convoy.get(k);
    }

    /** The convoy's units still alive in this attempt; 0 without a convoy. */
    public int alliesAlive() {
        return convoy == null ? 0 : convoy.alive();
    }

    /** The convoy unit hit first in this attempt, for its radio line; -1 before. */
    public int firstAllyHit() {
        return convoy == null ? -1 : convoy.firstHit();
    }

    /** The convoy unit lost first in this attempt, for its radio line; -1 before. */
    public int firstAllyLost() {
        return convoy == null ? -1 : convoy.firstLost();
    }

    /** Whether the primary objective failed in this attempt: the level is lost though the ship flies on. */
    public boolean primaryFailed() {
        return failed;
    }

    /** Events of the last step. */
    public SimEvents events() {
        return events;
    }

    /** Steps since the sortie was created. */
    public long tick() {
        return tick;
    }

    /** Seconds since the start of this attempt. */
    public double levelSeconds() {
        return levelTick * SimStep.SECONDS;
    }

    /** 1 for the first attempt, counting up with every restart after the ship was destroyed. */
    public int attempt() {
        return attempt;
    }

    /** Distance the ground layer has scrolled in this attempt, in pixels. */
    public double groundScroll() {
        return groundScroll;
    }

    /** The 1-based number of the section the scroll is in; the last one after the end. */
    public int section() {
        double t = levelSeconds();
        List<LevelScript.Section> sections = script.sections();
        for (int i = 0; i < sections.size(); i++) {
            if (t < sections.get(i).end()) {
                return i + 1;
            }
        }
        return sections.size();
    }

    /** The ground layer's current scroll speed in px/s. */
    public double groundSpeed() {
        return scrollSpeed();
    }

    /** Whether the non-playable launch is still running. */
    public boolean launching() {
        return levelTick <= launchTicks;
    }

    /** Whether the ship is flying, rather than wrecked and waiting for a retry. */
    public boolean flying() {
        return !wrecked;
    }

    /** Whether the scroll has reached the end: the level is won. */
    public boolean complete() {
        return complete;
    }

    /** The edges showing an edge warning, as {@link WarningEdge} bits. */
    public int edgeWarnings() {
        return edgeWarnings;
    }

    public long score() {
        return tally.score();
    }

    /** Credits earned in this attempt. */
    public int credits() {
        return tally.credits();
    }

    public int chain() {
        return tally.chain();
    }

    public double chainMultiplier() {
        return tally.multiplier();
    }

    /** The share of the chain window left, 1 right after a kill. */
    public double chainWindow() {
        return tally.window();
    }

    public int kills() {
        return tally.kills();
    }

    /** Every enemy the level sends, its set pieces among them. */
    public int enemyTotal() {
        return force.units() + setPieces.length;
    }

    /** Kills needed for the secondary objective. */
    public int requiredKills() {
        return objectives.requiredKills();
    }

    public boolean secondaryMet() {
        return objectives.secondaryMet();
    }

    /** Whether the secondary objective is that none of an enemy gets through (Level 03's Spore Bombers). */
    public boolean secondaryByEscapes() {
        return script.secondary().byEscapes();
    }

    /** The units of an escapes objective's enemy destroyed so far in this attempt. */
    public int escapesDestroyed() {
        return objectives.escapesDestroyed();
    }

    /** The units of an escapes objective's enemy the level sends. */
    public int escapesTotal() {
        return objectives.escapesTotal();
    }

    /** Whether a unit of an escapes objective's enemy got away: the objective failed for this attempt. */
    public boolean secondaryFailed() {
        return objectives.escapesFailed();
    }
}
