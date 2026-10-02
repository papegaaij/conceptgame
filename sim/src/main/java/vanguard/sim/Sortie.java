package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A level in play, advanced in fixed 60 Hz steps by {@link #step(int)}: the launch, the scroll
 * through the sections, the waves of the {@link LevelScript}, the ground objects, the pickups,
 * the radio cues, the objectives and the tally. All entities live in pools allocated up front and
 * the waves are planned when the sortie is created, so stepping does not allocate, and the same
 * seed and commands give the same {@link #stateHash()} on every platform.
 *
 * <p>When armour reaches zero the ship explodes and, after a pause, the level restarts with the
 * level-start state (design/systems/retry): what the attempt earned is lost. The level is over
 * when the scroll reaches the end of the last section; the ship then flies on, out of harm's way,
 * until the presentation moves to the debrief.
 */
public final class Sortie {
    /** From destruction to the restart: the explosion, a second of slow motion and the failure sting. */
    static final double RESTART_SECONDS = 4;

    private static final int SHOT_CAPACITY = 64;
    private static final int ENEMY_CAPACITY = 128;
    private static final int BULLET_CAPACITY = 256;
    private static final int GROUND_CAPACITY = 32;
    private static final int PICKUP_CAPACITY = 64;
    private static final int EVENT_CAPACITY = 256;
    /** No enemy bullet spawns this close to the ship (design/enemies, bullet readability rules). */
    private static final double NO_FIRE_DISTANCE = 72;

    private final SplitMix64 rng;
    private final Ship ship;
    private final LevelScript script;
    private final Rules rules;
    private final WaveSchedule waves;
    private final Pool<Shot> shots = new Pool<>(SHOT_CAPACITY, Shot::new, Shot[]::new);
    private final Pool<Enemy> enemies = new Pool<>(ENEMY_CAPACITY, Enemy::new, Enemy[]::new);
    private final Pool<EnemyBullet> bullets = new Pool<>(BULLET_CAPACITY, EnemyBullet::new, EnemyBullet[]::new);
    private final Pool<GroundObject> ground = new Pool<>(GROUND_CAPACITY, GroundObject::new, GroundObject[]::new);
    private final Pool<Pickup> pickups = new Pool<>(PICKUP_CAPACITY, Pickup::new, Pickup[]::new);
    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private final Tally tally;
    private final int[] killsByKind;
    private final boolean[] cuesFired;
    private final int requiredKills;
    private final int launchTicks;
    private final int endTicks;
    private final int restartTicks = SimStep.ticks(RESTART_SECONDS);
    private final int pickupTicks;
    private long tick;
    private int levelTick;
    private int attempt = 1;
    private int wreckTicks;
    private double groundScroll;
    private int nextGroundObject;
    private int secretsFound;
    private int edgeWarnings;
    private boolean secondaryMet;
    private boolean complete;

    public Sortie(long seed, Loadout loadout, LevelScript script, Rules rules) {
        if (rules.bulletBudget() > BULLET_CAPACITY) {
            throw new IllegalArgumentException("the bullet budget exceeds the pool of " + BULLET_CAPACITY);
        }
        rng = new SplitMix64(seed);
        ship = new Ship(
                loadout.ship(),
                loadout.gun(),
                new Defences(loadout.shield(), loadout.plating(), loadout.ship().mercySeconds()));
        this.script = script;
        this.rules = rules;
        waves = new WaveSchedule(script.waves(), rng);
        tally = new Tally(rules.scoring());
        killsByKind = new int[waves.kinds().size()];
        cuesFired = new boolean[script.radio().size()];
        requiredKills = (int) Math.ceil(script.secondary().killRatio() * waves.units() - 1e-9);
        launchTicks = SimStep.ticks(script.launchSeconds());
        endTicks = SimStep.ticks(script.seconds());
        pickupTicks = SimStep.ticks(rules.pickups().seconds());
        startAttempt();
    }

    /** Advances the sortie by one step with the given {@link Command} set. */
    public void step(int commands) {
        tick++;
        events.clear();
        if (wreckTicks > 0 && --wreckTicks == 0) {
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
            if (ship.fireGun(commands)) {
                fire();
            }
        }
        tally.step();
        if (!complete) {
            spawnWaves();
            placeGroundObjects();
            cueByTime();
        }
        edgeWarnings = complete ? 0 : waves.warnings(levelTick);
        moveShots();
        moveEnemies();
        moveBullets();
        scrollGround(scrollStep);
        driftPickups();
        hitEnemies();
        hitGround();
        if (flying() && !complete) {
            hitShip();
            ramShip();
        }
        if (flying()) {
            collectPickups();
        }
        if (!complete && levelTick >= endTicks) {
            completeLevel();
        }
    }

    private void startAttempt() {
        ship.reset();
        if (launchTicks > 0) {
            ship.launch(0);
            ship.rememberPosition();
        }
    }

    private void restart() {
        clear(shots);
        clear(enemies);
        clear(bullets);
        clear(ground);
        clear(pickups);
        tally.reset();
        Arrays.fill(killsByKind, 0);
        Arrays.fill(cuesFired, false);
        waves.reset();
        levelTick = 0;
        groundScroll = 0;
        nextGroundObject = 0;
        secretsFound = 0;
        secondaryMet = false;
        attempt++;
        startAttempt();
        events.add(SimEvents.Type.SORTIE_RESTARTED, ship.x(), ship.y());
    }

    private static void clear(Pool<?> pool) {
        while (pool.size() > 0) {
            pool.free(pool.size() - 1);
        }
    }

    private double scrollSpeed() {
        return script.sections().get(section() - 1).speed();
    }

    private void fire() {
        double muzzleY = ship.y() + ship.spec().muzzleOffsetY();
        Shot shot = shots.obtain();
        if (shot != null) {
            shot.fire(ship.x(), muzzleY, ship.gun().damage());
        }
        events.add(SimEvents.Type.SHOT_FIRED, ship.x(), muzzleY);
    }

    private void spawnWaves() {
        for (Spawn spawn = waves.due(levelTick); spawn != null; spawn = waves.due(levelTick)) {
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.spawn(spawn);
            }
        }
    }

    private void placeGroundObjects() {
        List<LevelScript.GroundObjectSpec> objects = script.groundObjects();
        while (nextGroundObject < objects.size()
                && SimStep.ticks(objects.get(nextGroundObject).t()) <= levelTick) {
            GroundObject object = ground.obtain();
            if (object != null) {
                object.place(objects.get(nextGroundObject));
            }
            nextGroundObject++;
        }
    }

    private void cueByTime() {
        List<LevelScript.RadioCue> cues = script.radio();
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!cuesFired[i] && cue.trigger() == LevelScript.CueTrigger.TIME && SimStep.ticks(cue.t()) <= levelTick) {
                fireCue(i);
            }
        }
    }

    /** Starts the cues of an event; {@code subject} is the enemy slug or secret name it is about. */
    private void cue(LevelScript.CueTrigger trigger, String subject) {
        List<LevelScript.RadioCue> cues = script.radio();
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!cuesFired[i] && cue.trigger() == trigger && cue.subject().equals(subject)) {
                fireCue(i);
            }
        }
    }

    private void fireCue(int index) {
        cuesFired[index] = true;
        events.add(SimEvents.Type.RADIO, ship.x(), ship.y(), index);
    }

    private void moveShots() {
        double step = ship.gun().boltSpeed() * SimStep.SECONDS;
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            shot.move(step);
            if (!PlayField.overlaps(shot.x(), shot.y(), bolt)) {
                shots.free(i);
            }
        }
    }

    private void moveEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.move(ship.x(), ship.y())) {
                enemies.free(i);
            } else if (enemy.trigger() && flying() && !complete) {
                fireAt(enemy);
            }
        }
    }

    /**
     * An aimed shot at the ship, or where it is going for a unit that leads the target, turned by a
     * random angle within the difficulty's spread.
     */
    private void fireAt(Enemy enemy) {
        EnemyGun gun = enemy.spec().gun().orElseThrow();
        double dx = ship.x() - enemy.x();
        double dy = ship.y() - enemy.y();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < NO_FIRE_DISTANCE || bullets.size() >= rules.bulletBudget()) {
            return;
        }
        if (enemy.leadsTarget()) {
            double flight = distance / gun.bulletSpeed();
            dx += ship.vx() * flight;
            dy += ship.vy() * flight;
            distance = Math.sqrt(dx * dx + dy * dy);
        }
        if (rules.aimedSpread() > 0) {
            double angle = rng.range(-rules.aimedSpread(), rules.aimedSpread());
            double cos = Trig.cos(angle);
            double sin = Trig.sin(angle);
            double turned = dx * cos - dy * sin;
            dy = dx * sin + dy * cos;
            dx = turned;
        }
        EnemyBullet bullet = bullets.obtain();
        bullet.fire(
                enemy.x(),
                enemy.y(),
                dx / distance * gun.bulletSpeed(),
                dy / distance * gun.bulletSpeed(),
                gun.damage());
        events.add(SimEvents.Type.ENEMY_FIRED, enemy.x(), enemy.y());
    }

    private void moveBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            bullet.move();
            if (!PlayField.overlaps(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                bullets.free(i);
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

    private void hitEnemies() {
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                EnemySpec spec = enemy.spec();
                if (spec.layer().hitByStandardShots()
                        && PlayField.overlaps(enemy.x(), enemy.y(), spec.hitbox())
                        && bolt.overlaps(shot.x(), shot.y(), spec.hitbox(), enemy.x(), enemy.y())) {
                    events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y());
                    if (enemy.damage(shot.damage())) {
                        destroy(j);
                    }
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /** Shots that missed the air hit the ground layer below (design/enemies, layer rules). */
    private void hitGround() {
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            for (int j = ground.size() - 1; j >= 0; j--) {
                GroundObject object = ground.get(j);
                LevelScript.GroundObjectSpec spec = object.spec();
                if (object.hittable() && bolt.overlaps(shot.x(), shot.y(), spec.size(), object.x(), object.y())) {
                    events.add(SimEvents.Type.GROUND_HIT, shot.x(), shot.y(), spec.trigger() ? 1 : 0);
                    shots.free(i);
                    if (spec.trigger()) {
                        if (object.countHit()) {
                            revealSecret(object);
                        }
                    } else if (object.damage(shot.damage())) {
                        demolish(j);
                    }
                    break;
                }
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
        secretsFound++;
        LevelScript.GroundObjectSpec spec = trigger.spec();
        events.add(SimEvents.Type.SECRET_FOUND, trigger.x(), trigger.y());
        Pickup crate = pickups.obtain();
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, spec.crateCredits(), trigger.x(), trigger.y(), pickupTicks);
        }
        cue(LevelScript.CueTrigger.SECRET, spec.secret());
    }

    private void destroy(int index) {
        Enemy enemy = enemies.get(index);
        EnemySpec spec = enemy.spec();
        events.add(SimEvents.Type.ENEMY_DESTROYED, enemy.x(), enemy.y(), enemy.kind());
        tally.kill(spec.bounty());
        int kills = ++killsByKind[enemy.kind()];
        if (enemy.carried().isPresent()) {
            drop(enemy.carried().get(), enemy.x(), enemy.y());
        }
        if (spec.drop().isPresent() && kills % spec.drop().get().every() == 0) {
            drop(spec.drop().get().pickup(), enemy.x(), enemy.y());
        }
        enemies.free(index);
        if (kills == 1) {
            cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        }
        if (!secondaryMet && tally.kills() >= requiredKills) {
            secondaryMet = true;
            int credits = script.secondary().credits();
            tally.earn(CreditSource.OBJECTIVES, credits);
            tally.scoreValue(credits);
            events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
            cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
        }
    }

    private void drop(PickupType type, double x, double y) {
        Pickup pickup = pickups.obtain();
        if (pickup != null) {
            int credits = type == PickupType.SMALL_SALVAGE ? rules.pickups().smallSalvageCredits() : 0;
            pickup.drop(type, credits, x, y, pickupTicks);
        }
    }

    private void hitShip() {
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
    private boolean damaged(double lostBefore, boolean wrecked) {
        if (ship.defences().armourLost() > lostBefore) {
            tally.breakChain();
        }
        if (wrecked) {
            wreckTicks = restartTicks;
            events.add(SimEvents.Type.SHIP_DESTROYED, ship.x(), ship.y());
        }
        return wrecked;
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
            case SMALL_SALVAGE -> payPickup(CreditSource.SALVAGE, pickup);
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
        cue(LevelScript.CueTrigger.LEVEL_END, "");
    }

    /** A hash over the complete state; equal hashes mean equal replays. */
    public long stateHash() {
        StateHash hash = new StateHash()
                .add(tick)
                .add(levelTick)
                .add(attempt)
                .add(wreckTicks)
                .add(groundScroll)
                .add(nextGroundObject)
                .add(secretsFound)
                .add(secondaryMet ? 1 : 0)
                .add(complete ? 1 : 0)
                .add(rng.state());
        ship.addTo(hash);
        tally.addTo(hash);
        for (int kills : killsByKind) {
            hash.add(kills);
        }
        addAll(hash, shots);
        addAll(hash, enemies);
        addAll(hash, bullets);
        addAll(hash, ground);
        addAll(hash, pickups);
        return hash.value();
    }

    private static void addAll(StateHash hash, Pool<? extends Hashed> pool) {
        hash.add(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            pool.get(i).addTo(hash);
        }
    }

    /** The totals for the debrief; meaningful once the level is {@link #complete()}. */
    public LevelResult result() {
        Defences defences = ship.defences();
        return LevelResult.of(
                rules.scoring(),
                script,
                tally,
                waves.units(),
                defences.armourLost(),
                defences.maxArmour(),
                secretsFound,
                secondaryMet);
    }

    public LevelScript script() {
        return script;
    }

    /** The distinct enemies of the level; {@link Enemy#kind()} and enemy events index this list. */
    public List<EnemySpec> enemyKinds() {
        return waves.kinds();
    }

    public Ship ship() {
        return ship;
    }

    public int shotCount() {
        return shots.size();
    }

    public Shot shot(int index) {
        return shots.get(index);
    }

    public int enemyCount() {
        return enemies.size();
    }

    public Enemy enemy(int index) {
        return enemies.get(index);
    }

    public int bulletCount() {
        return bullets.size();
    }

    public EnemyBullet bullet(int index) {
        return bullets.get(index);
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

    /** Whether the ship is flying, rather than wrecked and waiting for the restart. */
    public boolean flying() {
        return wreckTicks == 0;
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
        return waves.units();
    }

    /** Kills needed for the secondary objective. */
    public int requiredKills() {
        return requiredKills;
    }

    public boolean secondaryMet() {
        return secondaryMet;
    }
}
