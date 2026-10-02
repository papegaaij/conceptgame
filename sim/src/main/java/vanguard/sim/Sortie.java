package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A level in play, advanced in fixed 60 Hz steps by {@link #step(int)}: the launch, the scroll
 * through the sections, the ship and its {@link PlayerFire}, the {@link EnemyForce} of the
 * {@link LevelScript}, the ground objects, the pickups, the {@link Radio} cues, the
 * {@link Objectives} and the tally. All entities live in pools allocated up front and the waves
 * are planned when the sortie is created, so stepping does not allocate, and the same seed and
 * commands give the same {@link #stateHash()} on every platform.
 *
 * <p>When armour reaches zero the ship explodes and the level runs on without it until the
 * presentation, after the mission failed screen, restarts it with {@link #retry(double)} from the
 * level-start state (design/systems/retry): what the attempt earned is lost. The level is over
 * when the scroll reaches the end of the last section; the ship then flies on, out of harm's way,
 * until the presentation moves to the debrief.
 */
public final class Sortie {
    private static final int GROUND_CAPACITY = 32;
    private static final int PICKUP_CAPACITY = 64;
    private static final int EVENT_CAPACITY = 256;

    private final SplitMix64 rng;
    private final Ship ship;
    private final LevelScript script;
    private final Rules rules;
    private final PlayerFire fire;
    private final EnemyForce force;
    private final Pool<GroundObject> ground = new Pool<>(GROUND_CAPACITY, GroundObject::new, GroundObject[]::new);
    private final Pool<Pickup> pickups = new Pool<>(PICKUP_CAPACITY, Pickup::new, Pickup[]::new);
    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private final Tally tally;
    private final Objectives objectives;
    private final Radio radio;
    private final Crane[] cranes;
    /** Whether a group's outcome was paid and called in this attempt. */
    private final boolean[] groupCalled;

    private final int launchTicks;
    private final int endTicks;
    private final int pickupTicks;
    private long tick;
    private int levelTick;
    private int attempt = 1;
    private boolean wrecked;
    /** The armour of the next attempt, once {@link #retry(double)} asked for one; 0 while none is asked for. */
    private double retryArmour;

    private double groundScroll;
    private int nextGroundObject;
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
        fire = new PlayerFire(ship, loadout.armament(), events, new PlayerFire.Hits() {
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
                revealSecret(ground.get(index));
            }
        });
        force = new EnemyForce(script.waves(), script.groundUnits(), rng, rules, events, this::groupUnitEscaped);
        tally = new Tally(rules.scoring());
        objectives = new Objectives(script.secondary(), force.kinds().size(), force.units(), script.groundUnits());
        cranes = script.cranes().stream().map(Crane::new).toArray(Crane[]::new);
        groupCalled = new boolean[script.secondary().groups().size()];
        radio = new Radio(script.radio(), events, ship);
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
        tally.step();
        if (!complete) {
            force.spawn(levelTick);
            placeGroundObjects();
            radio.byTime(levelTick);
        }
        for (Crane crane : cranes) {
            crane.update(levelTick);
        }
        edgeWarnings = complete ? 0 : force.warnings(levelTick);
        fire.move(scrollStep, force.enemies());
        force.move(ship, flying() && !complete, scrollStep);
        force.moveBullets();
        blockShots();
        scrollGround(scrollStep);
        driftPickups();
        fire.hitEnemies(force.enemies());
        fire.hitGround(ground, force.enemies());
        if (flying() && !complete && !rules.invulnerableShip()) {
            hitShip();
            ramShip();
            hitByCranes();
        }
        if (flying()) {
            collectPickups();
        }
        if (!complete && !wrecked && levelTick >= endTicks) {
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
        force.reset();
        Pools.clear(ground);
        Pools.clear(pickups);
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
        complete = false;
        wrecked = false;
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

    private void demolish(int index) {
        GroundObject object = ground.get(index);
        LevelScript.GroundObjectSpec spec = object.spec();
        events.add(SimEvents.Type.GROUND_DESTROYED, object.x(), object.y());
        tally.earn(CreditSource.GROUND_TARGETS, spec.bounty());
        tally.scoreValue(spec.bounty());
        if (spec.drop().isPresent()) {
            drop(spec.drop().get(), object.x(), object.y());
        }
        ground.free(index);
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
        enemies.free(index);
        if (kills == 1) {
            radio.cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        }
        if (group >= 0) {
            decided(group, objectives.groupUnitDestroyed(group));
        }
        if (objectives.meetsSecondary(tally.kills())) {
            int credits = script.secondary().credits();
            tally.earn(CreditSource.OBJECTIVES, credits);
            tally.scoreValue(credits);
            events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
            radio.cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
        }
    }

    /** A unit of a ground group left the screen alive. */
    private void groupUnitEscaped(int group) {
        decided(group, objectives.groupUnitEscaped(group));
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
        Pickup pickup = pickups.obtain();
        if (pickup != null) {
            int credits =
                    switch (type) {
                        case SMALL_SALVAGE -> rules.pickups().smallSalvageCredits();
                        case MEDIUM_SALVAGE -> rules.pickups().mediumSalvageCredits();
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
            case SMALL_SALVAGE, MEDIUM_SALVAGE -> payPickup(CreditSource.SALVAGE, pickup);
            case OVERDRIVE -> fire.overdrive(SimStep.ticks(rules.pickups().overdriveSeconds()));
            case HIDDEN_CRATE -> payPickup(CreditSource.SECRETS, pickup);
            case SHIELD_CELL -> defences.restoreShield(rules.pickups().shieldCellShare() * defences.maxShield());
            case ARMOUR_PATCH -> defences.repair(rules.pickups().armourPatch());
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
        radio.cue(LevelScript.CueTrigger.LEVEL_END, "");
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
                .add(objectives.secretsFound())
                .add(objectives.secondaryMet() ? 1 : 0)
                .add(complete ? 1 : 0)
                .add(rng.state());
        ship.addTo(hash);
        fire.addTo(hash);
        tally.addTo(hash);
        objectives.addKillsTo(hash);
        for (Crane crane : cranes) {
            crane.addTo(hash);
        }
        Pools.addAll(hash, fire.shots());
        Pools.addAll(hash, force.enemies());
        Pools.addAll(hash, force.bullets());
        Pools.addAll(hash, ground);
        Pools.addAll(hash, pickups);
        return hash.value();
    }

    /** The totals for the debrief; meaningful once the level is {@link #complete()}. */
    public LevelResult result() {
        Defences defences = ship.defences();
        return LevelResult.of(
                rules.scoring(),
                script,
                tally,
                force.units(),
                defences.armourLost(),
                defences.maxArmour(),
                objectives.secretsFound(),
                objectives.secondaryMet());
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

    /** Seconds of overdrive left; 0 without one. */
    public double overdriveSeconds() {
        return fire.overdriveTicks() * SimStep.SECONDS;
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

    /** Every enemy the level sends. */
    public int enemyTotal() {
        return force.units();
    }

    /** Kills needed for the secondary objective. */
    public int requiredKills() {
        return objectives.requiredKills();
    }

    public boolean secondaryMet() {
        return objectives.secondaryMet();
    }
}
