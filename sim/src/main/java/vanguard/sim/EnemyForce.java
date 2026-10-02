package vanguard.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The level's enemies: the units entering by the {@link WaveSchedule} and the units fixed to the
 * ground ({@link LevelScript.GroundUnit}, entering at the top edge with the scroll), flying or
 * scrolling and firing at the ship, and their bullets. What a kill earns is the {@link Sortie}'s.
 */
final class EnemyForce {
    private static final int ENEMY_CAPACITY = 128;
    static final int BULLET_CAPACITY = 256;
    /** No enemy bullet spawns this close to the ship (design/enemies, bullet readability rules). */
    private static final double NO_FIRE_DISTANCE = 72;

    /** What the sortie does when a unit of a ground group leaves the screen alive. */
    interface Escapes {
        void escaped(int group);
    }

    private final SplitMix64 rng;
    private final Rules rules;
    private final SimEvents events;
    private final Escapes escapes;
    private final List<EnemySpec> kinds;
    private final WaveSchedule waves;
    private final List<LevelScript.GroundUnit> groundUnits;
    private final int[] groundKinds;
    private final int[] groundTicks;
    private final Pool<Enemy> enemies = new Pool<>(ENEMY_CAPACITY, Enemy::new, Enemy[]::new);
    private final Pool<EnemyBullet> bullets = new Pool<>(BULLET_CAPACITY, EnemyBullet::new, EnemyBullet[]::new);
    private int spawned;
    private int nextGround;

    /** Plans the waves with {@code rng}, which also spreads the aimed shots later. */
    EnemyForce(
            List<WaveSpec> waveSpecs,
            List<LevelScript.GroundUnit> groundUnits,
            SplitMix64 rng,
            Rules rules,
            SimEvents events,
            Escapes escapes) {
        this.rng = rng;
        this.rules = rules;
        this.events = events;
        this.escapes = escapes;
        List<EnemySpec> distinct = new ArrayList<>(
                waveSpecs.stream().map(WaveSpec::enemy).distinct().toList());
        for (LevelScript.GroundUnit unit : groundUnits) {
            if (!distinct.contains(unit.enemy())) {
                distinct.add(unit.enemy());
            }
        }
        kinds = List.copyOf(distinct);
        this.waves = new WaveSchedule(waveSpecs, kinds, rng);
        this.groundUnits = groundUnits;
        groundKinds = new int[groundUnits.size()];
        groundTicks = new int[groundUnits.size()];
        for (int i = 0; i < groundUnits.size(); i++) {
            groundKinds[i] = kinds.indexOf(groundUnits.get(i).enemy());
            groundTicks[i] = SimStep.ticks(groundUnits.get(i).t());
        }
    }

    /** Back to the level start: no enemies or bullets, the schedule at its first unit. */
    void reset() {
        Pools.clear(enemies);
        Pools.clear(bullets);
        waves.reset();
        spawned = 0;
        nextGround = 0;
    }

    /** Lets in every unit due at {@code levelTick}, flying and on the ground. */
    void spawn(int levelTick) {
        for (Spawn spawn = waves.due(levelTick); spawn != null; spawn = waves.due(levelTick)) {
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.spawn(spawn, spawned);
            }
            spawned++;
        }
        while (nextGround < groundTicks.length && groundTicks[nextGround] <= levelTick) {
            LevelScript.GroundUnit unit = groundUnits.get(nextGround);
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.root(unit.enemy(), groundKinds[nextGround], unit.x(), spawned, unit.group());
            }
            spawned++;
            nextGround++;
        }
    }

    /**
     * Flies every unit and scrolls the ground units by {@code groundScroll}; those whose gun is
     * ready fire at the ship when {@code firing}. A grouped ground unit that leaves the screen
     * alive is reported as escaped.
     */
    void move(Ship ship, boolean firing, double groundScroll) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (!enemy.move(ship.x(), ship.y(), groundScroll)) {
                int group = enemy.group();
                enemies.free(i);
                if (group >= 0) {
                    escapes.escaped(group);
                }
            } else if (enemy.trigger() && firing) {
                fireAt(enemy, ship);
            }
        }
    }

    /**
     * A shot at the ship (or where it is going for a unit that leads the target), a turret's along
     * its barrel, or a fan centred on that line; turned by a random angle within the difficulty's
     * spread.
     */
    private void fireAt(Enemy enemy, Ship ship) {
        EnemyGun gun = enemy.spec().gun().orElseThrow();
        double dx = ship.x() - enemy.x();
        double dy = ship.y() - enemy.y();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < NO_FIRE_DISTANCE || bullets.size() >= rules.bulletBudget()) {
            return;
        }
        if (Double.isFinite(gun.turnRate())) {
            // Along the barrel: radians clockwise from straight down.
            dx = -Trig.sin(enemy.aim());
            dy = -Trig.cos(enemy.aim());
            distance = 1;
        } else if (enemy.leadsTarget()) {
            double flight = distance / gun.bulletSpeed();
            dx += ship.vx() * flight;
            dy += ship.vy() * flight;
            distance = Math.sqrt(dx * dx + dy * dy);
        }
        dx /= distance;
        dy /= distance;
        double turn = rules.aimedSpread() > 0 ? rng.range(-rules.aimedSpread(), rules.aimedSpread()) : 0;
        for (int k = 0; k < gun.fan(); k++) {
            if (bullets.size() >= rules.bulletBudget()) {
                break;
            }
            double angle =
                    turn + (gun.fan() == 1 ? 0 : -gun.spreadRadians() / 2 + k * gun.spreadRadians() / (gun.fan() - 1));
            double cos = Trig.cos(angle);
            double sin = Trig.sin(angle);
            EnemyBullet bullet = bullets.obtain();
            bullet.fire(
                    enemy.x(),
                    enemy.y(),
                    (dx * cos - dy * sin) * gun.bulletSpeed(),
                    (dx * sin + dy * cos) * gun.bulletSpeed(),
                    gun.damage());
        }
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
        return kinds;
    }

    /** Every unit the level sends, flying and on the ground. */
    int units() {
        return waves.units() + groundUnits.size();
    }
}
