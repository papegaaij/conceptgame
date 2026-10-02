package vanguard.sim;

import java.util.List;

/**
 * The level's enemies: the units entering by the {@link WaveSchedule}, flying their paths and
 * firing at the ship, and their bullets. What a kill earns is the {@link Sortie}'s.
 */
final class EnemyForce {
    private static final int ENEMY_CAPACITY = 128;
    static final int BULLET_CAPACITY = 256;
    /** No enemy bullet spawns this close to the ship (design/enemies, bullet readability rules). */
    private static final double NO_FIRE_DISTANCE = 72;

    private final SplitMix64 rng;
    private final Rules rules;
    private final SimEvents events;
    private final WaveSchedule waves;
    private final Pool<Enemy> enemies = new Pool<>(ENEMY_CAPACITY, Enemy::new, Enemy[]::new);
    private final Pool<EnemyBullet> bullets = new Pool<>(BULLET_CAPACITY, EnemyBullet::new, EnemyBullet[]::new);
    private int spawned;

    /** Plans the waves with {@code rng}, which also spreads the aimed shots later. */
    EnemyForce(List<WaveSpec> waveSpecs, SplitMix64 rng, Rules rules, SimEvents events) {
        this.rng = rng;
        this.rules = rules;
        this.events = events;
        this.waves = new WaveSchedule(waveSpecs, rng);
    }

    /** Back to the level start: no enemies or bullets, the schedule at its first unit. */
    void reset() {
        Pools.clear(enemies);
        Pools.clear(bullets);
        waves.reset();
        spawned = 0;
    }

    /** Lets in every unit due at {@code levelTick}. */
    void spawn(int levelTick) {
        for (Spawn spawn = waves.due(levelTick); spawn != null; spawn = waves.due(levelTick)) {
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.spawn(spawn, spawned);
            }
            spawned++;
        }
    }

    /** Flies every unit; those whose gun is ready fire at the ship when {@code firing}. */
    void move(Ship ship, boolean firing) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.move(ship.x(), ship.y())) {
                enemies.free(i);
            } else if (enemy.trigger() && firing) {
                fireAt(enemy, ship);
            }
        }
    }

    /**
     * An aimed shot at the ship, or where it is going for a unit that leads the target, turned by a
     * random angle within the difficulty's spread.
     */
    private void fireAt(Enemy enemy, Ship ship) {
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

    void moveBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            bullet.move();
            if (!PlayField.overlaps(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                bullets.free(i);
            }
        }
    }

    /** The edges showing an edge warning at {@code levelTick}, as {@link WarningEdge} bits. */
    int warnings(int levelTick) {
        return waves.warnings(levelTick);
    }

    Pool<Enemy> enemies() {
        return enemies;
    }

    Pool<EnemyBullet> bullets() {
        return bullets;
    }

    /** The distinct enemies of the level; {@link Enemy#kind()} indexes this list. */
    List<EnemySpec> kinds() {
        return waves.kinds();
    }

    /** Every unit the level sends. */
    int units() {
        return waves.units();
    }
}
