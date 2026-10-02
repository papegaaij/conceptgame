package vanguard.sim;

/**
 * The deterministic state of a sortie, advanced in fixed 60 Hz steps by {@link #step(int)}: the
 * ship, its shots and the Skitters of the {@link TestSortie} waves over the scrolling ground. All
 * entities live in pools allocated up front, so stepping does not allocate, and the same seed and
 * commands give the same {@link #stateHash()} on every platform.
 *
 * <p>When armour reaches zero the ship explodes and, after a pause, the sortie restarts with the
 * level-start state (design/systems/retry): full shield and armour and the wave cycle from the
 * beginning. The mission failed screen comes with the menus in M3.
 */
public final class Sortie {
    /** From destruction to the restart: the explosion, a second of slow motion and the failure sting. */
    static final double RESTART_SECONDS = 4;

    private static final int SHOT_CAPACITY = 64;
    private static final int SKITTER_CAPACITY = 128;
    private static final int EVENT_CAPACITY = 256;

    private final SplitMix64 rng;
    private final Ship ship;
    private final SkitterSpec skitterSpec;
    private final Pool<Shot> shots = new Pool<>(SHOT_CAPACITY, Shot::new, Shot[]::new);
    private final Pool<Skitter> skitters = new Pool<>(SKITTER_CAPACITY, Skitter::new, Skitter[]::new);
    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private final int cycleTicks = SimStep.ticks(TestSortie.CYCLE_SECONDS);
    private final int restartTicks = SimStep.ticks(RESTART_SECONDS);
    private long tick;
    private int attemptTick;
    private int attempt = 1;
    private int wreckTicks;
    private double groundScroll;

    public Sortie(long seed, Loadout loadout, SkitterSpec skitterSpec) {
        rng = new SplitMix64(seed);
        ship = new Ship(
                loadout.ship(),
                loadout.gun(),
                new Defences(loadout.shield(), loadout.plating(), loadout.ship().mercySeconds()));
        this.skitterSpec = skitterSpec;
    }

    /** Advances the sortie by one step with the given {@link Command} set. */
    public void step(int commands) {
        tick++;
        events.clear();
        if (wreckTicks > 0 && --wreckTicks == 0) {
            restart();
        }
        attemptTick++;
        groundScroll += TestSortie.SCROLL_SPEED * SimStep.SECONDS;
        ship.rememberPosition();
        if (flying()) {
            ship.fly(commands);
            if (ship.fireGun(commands)) {
                fire();
            }
        }
        spawnWaves();
        moveShots();
        moveSkitters();
        hitSkitters();
        if (flying()) {
            ramShip();
        }
    }

    private void restart() {
        while (shots.size() > 0) {
            shots.free(shots.size() - 1);
        }
        while (skitters.size() > 0) {
            skitters.free(skitters.size() - 1);
        }
        ship.reset();
        attemptTick = 0;
        groundScroll = 0;
        attempt++;
        events.add(SimEvents.Type.SORTIE_RESTARTED, ship.x(), ship.y());
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
        int cycleTick = attemptTick % cycleTicks;
        for (int i = 0; i < TestSortie.WAVES.size(); i++) {
            SnakeWave wave = TestSortie.WAVES.get(i);
            if (SimStep.ticks(wave.startSeconds()) == cycleTick) {
                spawn(wave, rng.range(-TestSortie.MAX_OFFSET, TestSortie.MAX_OFFSET));
            }
        }
    }

    /** Spawns the Skitters of a wave, all shifted sideways by {@code offset}. */
    void spawn(SnakeWave wave, double offset) {
        double spacing = skitterSpec.snakeSpeed() * skitterSpec.snakeSpacingSeconds();
        for (int i = 0; i < wave.count(); i++) {
            Skitter skitter = skitters.obtain();
            if (skitter != null) {
                skitter.spawn(wave.path(), -i * spacing, wave.mirrored(), offset, skitterSpec.hp());
            }
        }
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

    private void moveSkitters() {
        double step = skitterSpec.snakeSpeed() * SimStep.SECONDS;
        for (int i = skitters.size() - 1; i >= 0; i--) {
            if (!skitters.get(i).advance(step)) {
                skitters.free(i);
            }
        }
    }

    private void hitSkitters() {
        if (!skitterSpec.layer().hitByStandardShots()) {
            return;
        }
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            for (int j = skitters.size() - 1; j >= 0; j--) {
                Skitter skitter = skitters.get(j);
                if (onScreen(skitter)
                        && bolt.overlaps(shot.x(), shot.y(), skitterSpec.hitbox(), skitter.x(), skitter.y())) {
                    events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y());
                    if (skitter.damage(shot.damage())) {
                        destroy(j);
                    }
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /** A Skitter that rams the ship is destroyed by the impact; the ship takes its contact damage. */
    private void ramShip() {
        if (!skitterSpec.layer().collidesWithPlayer()) {
            return;
        }
        Hitbox hull = ship.spec().hitbox();
        for (int j = skitters.size() - 1; j >= 0; j--) {
            Skitter skitter = skitters.get(j);
            if (hull.overlaps(ship.x(), ship.y(), skitterSpec.hitbox(), skitter.x(), skitter.y())) {
                destroy(j);
                if (ship.defences().takeCollision(skitterSpec.contactDamage(), events, ship.x(), ship.y())) {
                    wreckTicks = restartTicks;
                    events.add(SimEvents.Type.SHIP_DESTROYED, ship.x(), ship.y());
                    return;
                }
            }
        }
    }

    private boolean onScreen(Skitter skitter) {
        return PlayField.overlaps(skitter.x(), skitter.y(), skitterSpec.hitbox());
    }

    private void destroy(int index) {
        Skitter skitter = skitters.get(index);
        events.add(SimEvents.Type.ENEMY_DESTROYED, skitter.x(), skitter.y());
        skitters.free(index);
    }

    /** Whether the ship is flying, rather than wrecked and waiting for the restart. */
    public boolean flying() {
        return wreckTicks == 0;
    }

    /** A hash over the complete state; equal hashes mean equal replays. */
    public long stateHash() {
        StateHash hash = new StateHash()
                .add(tick)
                .add(attemptTick)
                .add(attempt)
                .add(wreckTicks)
                .add(groundScroll)
                .add(rng.state());
        ship.addTo(hash);
        hash.add(shots.size());
        for (int i = 0; i < shots.size(); i++) {
            shots.get(i).addTo(hash);
        }
        hash.add(skitters.size());
        for (int i = 0; i < skitters.size(); i++) {
            skitters.get(i).addTo(hash);
        }
        return hash.value();
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

    public int skitterCount() {
        return skitters.size();
    }

    public Skitter skitter(int index) {
        return skitters.get(index);
    }

    /** Events of the last step. */
    public SimEvents events() {
        return events;
    }

    /** Steps since the sortie was created. */
    public long tick() {
        return tick;
    }

    /** 1 for the first attempt, counting up with every restart after the ship was destroyed. */
    public int attempt() {
        return attempt;
    }

    /** Distance the ground layer has scrolled in this attempt, in pixels. */
    public double groundScroll() {
        return groundScroll;
    }
}
