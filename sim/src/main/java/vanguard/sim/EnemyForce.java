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

    /** What the sortie does when a unit leaves the screen alive (just before it is gone). */
    interface Escapes {
        void escaped(Enemy enemy);
    }

    private static final int MINE_CAPACITY = 96;

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
    private final Pool<Mine> mines = new Pool<>(MINE_CAPACITY, Mine::new, Mine[]::new);
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
        Pools.clear(mines);
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
                escapes.escaped(enemy);
                enemies.free(i);
            } else if (enemy.trigger() && firing) {
                EnemyGun gun = enemy.spec().gun().orElseThrow();
                if (gun.mine().isPresent()) {
                    dropMine(enemy, gun);
                } else {
                    fireAt(enemy, ship);
                }
            }
        }
    }

    /**
     * A shot at the ship (or where it is going for a unit that leads the target), a turret's along
     * its barrel, or a fan centred on that line; turned by a random angle within the difficulty's
     * spread.
     */
    private void fireAt(Enemy enemy, Ship ship) {
        fire(enemy.x(), enemy.y(), enemy.spec().gun().orElseThrow(), enemy.leadsTarget(), enemy.aim(), ship);
    }

    /** A set piece's part fires its gun from (x, y) at the ship, as a unit that aims at once. */
    void fireFrom(double x, double y, EnemyGun gun, Ship ship) {
        fire(x, y, gun, false, 0, ship);
    }

    private void fire(double x, double y, EnemyGun gun, boolean leads, double aim, Ship ship) {
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < NO_FIRE_DISTANCE || bullets.size() >= rules.bulletBudget()) {
            return;
        }
        if (Double.isFinite(gun.turnRate())) {
            // Along the barrel: radians clockwise from straight down.
            dx = -Trig.sin(aim);
            dy = -Trig.cos(aim);
            distance = 1;
        } else if (leads) {
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
                    x,
                    y,
                    (dx * cos - dy * sin) * gun.bulletSpeed(),
                    (dx * sin + dy * cos) * gun.bulletSpeed(),
                    gun.damage());
        }
        events.add(SimEvents.Type.ENEMY_FIRED, x, y);
    }

    /** A mine layer drops a spore under it, drifting in a random direction. */
    private void dropMine(Enemy enemy, EnemyGun gun) {
        dropMine(gun, enemy.x(), enemy.y(), rng.range(0, 2 * StrictMath.PI));
    }

    /** A spore of {@code gun}'s mine at (x, y), drifting at {@code angle} radians (0 = right, y up). */
    void dropMine(EnemyGun gun, double x, double y, double angle) {
        Mine mine = mines.obtain();
        if (mine != null) {
            mine.drop(gun, x, y, angle);
            events.add(SimEvents.Type.MINE_DROPPED, x, y);
        }
    }

    /**
     * Drifts the spores; one whose life ran out bursts into its ring (unless it never bursts),
     * away from the ship (no bullet spawns close to it).
     */
    void moveMines(Ship ship) {
        for (int i = mines.size() - 1; i >= 0; i--) {
            Mine mine = mines.get(i);
            if (!mine.move()) {
                EnemyGun.MineSpec spec = mine.spec();
                if (mine.expired() && spec.bursts()) {
                    burst(mine.x(), mine.y(), spec.ring(), mine.gun().bulletSpeed(), spec.ringDamage(), ship);
                    events.add(SimEvents.Type.MINE_BURST, mine.x(), mine.y());
                }
                mines.free(i);
            }
        }
    }

    /** A destroyed unit with a death burst pops into its puff of bullets. */
    void deathBurst(Enemy enemy, Ship ship) {
        EnemySpec.DeathBurst puff = enemy.spec().deathBurst().orElseThrow();
        burst(enemy.x(), enemy.y(), puff.count(), puff.speed(), puff.damage(), ship);
    }

    /**
     * A ring of {@code count} bullets from (x, y), within the bullet budget; none when the ship is
     * closer than the bullets may spawn (design/enemies, bullet readability rules).
     */
    void burst(double x, double y, int count, double speed, double damage, Ship ship) {
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        if (dx * dx + dy * dy < NO_FIRE_DISTANCE * NO_FIRE_DISTANCE) {
            return;
        }
        for (int k = 0; k < count && bullets.size() < rules.bulletBudget(); k++) {
            double angle = 2 * StrictMath.PI * k / count;
            bullets.obtain().fire(x, y, Trig.cos(angle) * speed, Trig.sin(angle) * speed, damage);
        }
    }

    Pool<Mine> mines() {
        return mines;
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

    /** The units of the enemy {@code slug} the level sends, flying and on the ground. */
    int unitsOf(String slug) {
        int count = waves.unitsOf(slug);
        for (LevelScript.GroundUnit unit : groundUnits) {
            count += unit.enemy().slug().equals(slug) ? 1 : 0;
        }
        return count;
    }

    /** Every unit the level sends, flying and on the ground. */
    int units() {
        return waves.units() + groundUnits.size();
    }
}
