package vanguard.sim;

import java.util.List;

/**
 * The ship's weapons in flight: every {@link Armament.Mount} fires on its own clock while fire is
 * held, an overdrive switches every mount to its overdrive pattern, and the projectiles fly, seek,
 * fall and hit by the layer rules of design/enemies (Layer rules): bolts and homing missiles hit
 * what they pass on the layers their {@link WeaponSpec.Delivery} reaches and the ground objects
 * below; bombs and shells burst on the ground only. Hardened ground targets take damage from
 * {@code anti-ground} weapons only; other shots glance off. What a hit destroys is handed to
 * {@link Hits}, which the {@link Sortie} implements.
 */
final class PlayerFire {
    private static final int SHOT_CAPACITY = 256;

    /** What the sortie does when a shot destroys something. */
    interface Hits {
        /** The enemy at {@code index} was destroyed. */
        void enemyDestroyed(int index);

        /** The destructible ground object at {@code index} was destroyed. */
        void groundDestroyed(int index);

        /** The trigger at {@code index} took its last hit and releases its secret. */
        void triggerReleased(int index);
    }

    private final Ship ship;
    private final Armament armament;
    private final SimEvents events;
    private final Hits hits;
    private final Pool<Shot> shots = new Pool<>(SHOT_CAPACITY, Shot::new, Shot[]::new);
    private final int[] cooldowns;
    private final int[] sinceShot;
    private int overdriveTicks;

    PlayerFire(Ship ship, Armament armament, SimEvents events, Hits hits) {
        this.ship = ship;
        this.armament = armament;
        this.events = events;
        this.hits = hits;
        cooldowns = new int[armament.size()];
        sinceShot = new int[armament.size()];
        reset();
    }

    /** Back to the level start: no shots, every weapon ready, no overdrive. */
    void reset() {
        Pools.clear(shots);
        for (int m = 0; m < armament.size(); m++) {
            cooldowns[m] = 0;
            sinceShot[m] = Integer.MAX_VALUE;
        }
        overdriveTicks = 0;
    }

    /** Starts an overdrive of {@code ticks} steps; a running one starts over. */
    void overdrive(int ticks) {
        overdriveTicks = ticks;
    }

    /**
     * One step of the guns while the ship flies: the overdrive runs down, and every mount whose
     * weapon is ready fires a volley if fire is held. A lobbed shell picks its ground target from
     * {@code enemies} and {@code ground} as it leaves.
     */
    void fire(int commands, Pool<Enemy> enemies, Pool<GroundObject> ground) {
        if (overdriveTicks > 0 && --overdriveTicks == 0) {
            events.add(SimEvents.Type.OVERDRIVE_ENDED, ship.x(), ship.y());
        }
        boolean held = Command.FIRE.in(commands);
        for (int m = 0; m < armament.size(); m++) {
            if (sinceShot[m] < Integer.MAX_VALUE) {
                sinceShot[m]++;
            }
            if (cooldowns[m] > 0) {
                cooldowns[m]--;
            }
            if (cooldowns[m] > 0 || !held) {
                continue;
            }
            Armament.Mount mount = armament.mount(m);
            WeaponSpec weapon = overdriveTicks > 0 ? mount.overdrive() : mount.weapon();
            cooldowns[m] = weapon.intervalTicks();
            sinceShot[m] = 0;
            volley(m, weapon, enemies, ground);
        }
    }

    private void volley(int mount, WeaponSpec weapon, Pool<Enemy> enemies, Pool<GroundObject> ground) {
        List<WeaponSpec.Muzzle> muzzles = weapon.muzzles();
        double sumX = 0;
        double sumY = 0;
        // Indexed: an iterator would allocate on every volley.
        for (int i = 0; i < muzzles.size(); i++) {
            WeaponSpec.Muzzle muzzle = muzzles.get(i);
            double x = ship.x() + muzzle.dx();
            double y = ship.y() + muzzle.dy();
            sumX += x;
            sumY += y;
            Shot shot = shots.obtain();
            if (shot == null) {
                continue;
            }
            switch (weapon.delivery()) {
                case BOLT, HOMING -> shot.fire(weapon, mount, x, y, muzzle.angle());
                case DROPPED -> shot.lob(weapon, mount, x, y, x, y);
                case LOBBED -> lob(shot, weapon, mount, x, y, enemies, ground);
            }
        }
        events.add(SimEvents.Type.SHOT_FIRED, sumX / muzzles.size(), sumY / muzzles.size(), mount);
    }

    /**
     * A shell lands {@code range} ahead of its muzzle, or on the nearest destructible ground target
     * (a ground enemy or a destructible ground object, never a trigger) within the snap radius.
     */
    private void lob(
            Shot shot,
            WeaponSpec weapon,
            int mount,
            double x,
            double y,
            Pool<Enemy> enemies,
            Pool<GroundObject> ground) {
        double landX = x;
        double landY = y + weapon.range();
        double best = weapon.snap() * weapon.snap();
        double snapX = landX;
        double snapY = landY;
        for (int j = 0; j < ground.size(); j++) {
            GroundObject object = ground.get(j);
            double d = distanceSquared(landX, landY, object.x(), object.y());
            if (!object.spec().trigger() && d <= best) {
                best = d;
                snapX = object.x();
                snapY = object.y();
            }
        }
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            double d = distanceSquared(landX, landY, enemy.x(), enemy.y());
            if (enemy.spec().layer() == Layer.GROUND && d <= best) {
                best = d;
                snapX = enemy.x();
                snapY = enemy.y();
            }
        }
        shot.lob(weapon, mount, x, y, snapX, snapY);
    }

    /**
     * Moves every projectile by one step: bolts fly on until their range is flown or they leave
     * the screen, homing missiles steer at their target first, bombs and shells fall towards their
     * landing point, which scrolls down with the ground by {@code groundScroll}.
     */
    void move(double groundScroll, Pool<Enemy> enemies) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            WeaponSpec weapon = shot.weapon();
            if (weapon.delivery().landing()) {
                shot.fall(groundScroll);
                continue;
            }
            if (weapon.delivery() == WeaponSpec.Delivery.HOMING) {
                home(shot, enemies);
            }
            shot.move();
            if (shot.spent() || !PlayField.overlaps(shot.x(), shot.y(), weapon.size())) {
                shots.free(i);
            }
        }
    }

    /** Steers at the locked target, or locks onto the nearest enemy in range and in the cone when it has none. */
    private void home(Shot shot, Pool<Enemy> enemies) {
        Enemy target = locked(shot, enemies);
        if (target == null) {
            target = nearestInCone(shot, enemies);
            shot.lock(target == null ? -1 : target.serial());
        }
        if (target != null) {
            shot.steer(target.x(), target.y());
        }
    }

    private static Enemy locked(Shot shot, Pool<Enemy> enemies) {
        if (shot.target() < 0) {
            return null;
        }
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            if (enemy.serial() == shot.target()) {
                return onField(enemy) ? enemy : null;
            }
        }
        return null;
    }

    private static Enemy nearestInCone(Shot shot, Pool<Enemy> enemies) {
        WeaponSpec weapon = shot.weapon();
        double best = weapon.range() * weapon.range();
        Enemy nearest = null;
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            double dx = enemy.x() - shot.x();
            double dy = enemy.y() - shot.y();
            double d = dx * dx + dy * dy;
            if (d > best || !onField(enemy)) {
                continue;
            }
            double off = Math.IEEEremainder(StrictMath.atan2(dx, dy) - shot.heading(), 2 * StrictMath.PI);
            if (Math.abs(off) <= weapon.coneHalfAngle()) {
                best = d;
                nearest = enemy;
            }
        }
        return nearest;
    }

    /** Bolts and missiles hit the enemies they pass on the layers they reach. */
    void hitEnemies(Pool<Enemy> enemies) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            WeaponSpec weapon = shot.weapon();
            if (weapon.delivery().landing()) {
                continue;
            }
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                EnemySpec spec = enemy.spec();
                if (!weapon.delivery().reaches(spec.layer())
                        || !onField(enemy)
                        || !weapon.size().overlaps(shot.x(), shot.y(), spec.hitbox(), enemy.x(), enemy.y())
                        || (weapon.pierce() > 1 && shot.struck(2 * enemy.serial()))) {
                    continue;
                }
                events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y(), shot.mount());
                boolean spent = shot.pierced();
                if (enemy.damage(shot.damage() * groundFactor(weapon, spec.layer()))) {
                    hits.enemyDestroyed(j);
                }
                if (spent) {
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /**
     * Bolts and missiles that missed the air hit the ground objects below (design/enemies, layer
     * rules); bombs and shells that have landed burst.
     */
    void hitGround(Pool<GroundObject> ground, Pool<Enemy> enemies) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            WeaponSpec weapon = shot.weapon();
            if (weapon.delivery().landing()) {
                if (shot.airProgress(0) >= 1) {
                    burst(shot, ground, enemies);
                    shots.free(i);
                }
                continue;
            }
            for (int j = ground.size() - 1; j >= 0; j--) {
                GroundObject object = ground.get(j);
                LevelScript.GroundObjectSpec spec = object.spec();
                if (!object.hittable()
                        || !weapon.size().overlaps(shot.x(), shot.y(), spec.size(), object.x(), object.y())
                        || (weapon.pierce() > 1 && shot.struck(2 * object.serial() + 1))) {
                    continue;
                }
                if (spec.hardened() && !weapon.antiGround()) {
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                    break;
                }
                events.add(SimEvents.Type.GROUND_HIT, shot.x(), shot.y(), shot.mount());
                boolean spent = shot.pierced();
                strike(j, object, shot.damage() * groundFactor(weapon, Layer.GROUND));
                if (spent) {
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /** A landed bomb or shell damages every ground object and ground enemy within its blast once. */
    private void burst(Shot shot, Pool<GroundObject> ground, Pool<Enemy> enemies) {
        WeaponSpec weapon = shot.weapon();
        double x = shot.landX();
        double y = shot.landY();
        events.add(SimEvents.Type.BLAST, x, y, shot.mount());
        for (int j = ground.size() - 1; j >= 0; j--) {
            GroundObject object = ground.get(j);
            LevelScript.GroundObjectSpec spec = object.spec();
            if (object.hittable()
                    && (weapon.antiGround() || !spec.hardened())
                    && inBlast(x, y, weapon.blast(), object.x(), object.y(), spec.size())) {
                strike(j, object, shot.damage());
            }
        }
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            EnemySpec spec = enemy.spec();
            if (spec.layer() == Layer.GROUND
                    && onField(enemy)
                    && inBlast(x, y, weapon.blast(), enemy.x(), enemy.y(), spec.hitbox())
                    && enemy.damage(shot.damage())) {
                hits.enemyDestroyed(j);
            }
        }
    }

    /** A hit on a ground object: a trigger counts it, a destructible takes the damage. */
    private void strike(int index, GroundObject object, double damage) {
        if (object.spec().trigger()) {
            if (object.countHit()) {
                hits.triggerReleased(index);
            }
        } else if (object.damage(damage)) {
            hits.groundDestroyed(index);
        }
    }

    /** Bolts and missiles of an {@code anti-ground} weapon do double damage on the ground layer. */
    private static double groundFactor(WeaponSpec weapon, Layer layer) {
        return layer == Layer.GROUND
                        && weapon.antiGround()
                        && !weapon.delivery().landing()
                ? 2
                : 1;
    }

    /** Whether a blast of {@code radius} around (x, y) reaches a box around (bx, by). */
    private static boolean inBlast(double x, double y, double radius, double bx, double by, Hitbox box) {
        double dx = Math.max(Math.abs(x - bx) - box.width() / 2, 0);
        double dy = Math.max(Math.abs(y - by) - box.height() / 2, 0);
        return dx * dx + dy * dy <= radius * radius;
    }

    private static boolean onField(Enemy enemy) {
        return PlayField.overlaps(enemy.x(), enemy.y(), enemy.spec().hitbox());
    }

    private static double distanceSquared(double x, double y, double ox, double oy) {
        return (x - ox) * (x - ox) + (y - oy) * (y - oy);
    }

    void addTo(StateHash hash) {
        for (int m = 0; m < armament.size(); m++) {
            hash.add(cooldowns[m]).add(sinceShot[m]);
        }
        hash.add(overdriveTicks);
    }

    Pool<Shot> shots() {
        return shots;
    }

    Armament armament() {
        return armament;
    }

    /** Steps since mount {@code m} last fired, for its muzzle flash; {@link Integer#MAX_VALUE} before its first shot. */
    int ticksSinceShot(int m) {
        return sinceShot[m];
    }

    int overdriveTicks() {
        return overdriveTicks;
    }
}
