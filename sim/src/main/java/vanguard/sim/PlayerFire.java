package vanguard.sim;

import java.util.List;

/**
 * The ship's weapons in flight: every {@link Armament.Mount} fires on its own clock while fire is
 * held, an overdrive switches every mount to its overdrive pattern, and the projectiles fly, seek,
 * fall and hit by the layer rules of design/enemies (Layer rules): bolts and homing missiles hit
 * what they pass on the layers their {@link WeaponSpec.Delivery} reaches and the ground objects
 * below; bombs and shells burst on the ground only. Hardened ground targets take damage from
 * {@code anti-ground} weapons only; other shots glance off. Armed spore mines on the player's
 * layer are shot like enemies; a set piece's parts take the hits on their layer and its armoured
 * body makes the rest glance; homing missiles lock onto parts too. A boss on {@code high-air} is
 * above the play field: a missile seeks its open parts all round (not only in its cone), climbs to
 * the one it locks onto, turning at {@link #CLIMB_TURN} times its rate, and passes beneath the hull
 * and every other part. What a hit destroys is handed to {@link Hits}, which the {@link Sortie}
 * implements.
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

        /** The spore mine at {@code index} was shot. */
        void mineDestroyed(int index);

        /** Part {@code part} of set piece {@code piece} was destroyed. */
        void partDestroyed(int piece, int part);
    }

    /** Homing locks on a set piece's part use serials from here: unit serials stay far below it. */
    private static final int PART_SERIAL = 1 << 24;

    /**
     * How many times faster than its rate a missile turns while it climbs to a high-air boss's part:
     * an open sac beside the ship (the Brood Carrier's first pair over a ship under it) lies inside
     * the turning circle of the weapon's own rate.
     */
    static final double CLIMB_TURN = 2;

    private final Ship ship;
    private final Armament armament;
    private final SimEvents events;
    private final Hits hits;
    private final SetPiece[] setPieces;
    private final Pool<Shot> shots = new Pool<>(SHOT_CAPACITY, Shot::new, Shot[]::new);
    private final int[] cooldowns;
    private final int[] sinceShot;
    private int overdriveTicks;

    /** The homing target found by {@link #locked} or {@link #nearestInCone}. */
    private double targetX;

    private double targetY;

    PlayerFire(Ship ship, Armament armament, SimEvents events, Hits hits, SetPiece[] setPieces) {
        this.ship = ship;
        this.armament = armament;
        this.events = events;
        this.hits = hits;
        this.setPieces = setPieces;
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

    /**
     * Steers at the locked target, or locks onto the nearest enemy (or set-piece part) in range and
     * in the cone when it has none.
     */
    private void home(Shot shot, Pool<Enemy> enemies) {
        boolean found = locked(shot, enemies);
        if (!found) {
            int target = nearestInCone(shot, enemies);
            shot.lock(target);
            found = target >= 0;
        }
        if (found) {
            shot.steer(targetX, targetY, overhead(shot.target()) >= 0 ? CLIMB_TURN : 1);
        }
    }

    /**
     * The index of the set piece whose part {@code target} (a lock's serial) is, if that set piece
     * is a boss on high air (the missile climbs to it); -1 otherwise.
     */
    private int overhead(int target) {
        if (target < PART_SERIAL) {
            return -1;
        }
        int piece = (target - PART_SERIAL) / LevelScript.SetPieceSpec.MAX_PARTS;
        return piece < setPieces.length && overhead(setPieces[piece]) ? piece : -1;
    }

    /** Whether {@code piece} is a boss above the play field: missiles climb to the part they lock onto. */
    private static boolean overhead(SetPiece piece) {
        return piece.layer() == Layer.HIGH_AIR && piece.boss().isPresent();
    }

    /** Whether the shot's locked target is still on the field; it is then at the target position. */
    private boolean locked(Shot shot, Pool<Enemy> enemies) {
        int target = shot.target();
        if (target < 0) {
            return false;
        }
        if (target >= PART_SERIAL) {
            int piece = (target - PART_SERIAL) / LevelScript.SetPieceSpec.MAX_PARTS;
            int part = (target - PART_SERIAL) % LevelScript.SetPieceSpec.MAX_PARTS;
            return piece < setPieces.length && partTarget(setPieces[piece], part);
        }
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            if (enemy.serial() == target) {
                if (!onField(enemy)) {
                    return false;
                }
                targetX = enemy.x();
                targetY = enemy.y();
                return true;
            }
        }
        return false;
    }

    /** Whether a set piece's part can be a homing target: present, alive and on the field; it is then the target position. */
    private boolean partTarget(SetPiece piece, int part) {
        if (!piece.present() || piece.partWrecked(part) || piece.partShielded(part)) {
            return false;
        }
        double x = piece.partX(part);
        double y = piece.partY(part);
        if (!PlayField.overlaps(x, y, piece.spec().parts().get(part).box())) {
            return false;
        }
        targetX = x;
        targetY = y;
        return true;
    }

    /**
     * The serial of the nearest target in range and in the cone (a high-air boss's parts in range
     * all round), -1 for none; the target position is set.
     */
    private int nearestInCone(Shot shot, Pool<Enemy> enemies) {
        WeaponSpec weapon = shot.weapon();
        double best = weapon.range() * weapon.range();
        int nearest = -1;
        double nearestX = 0;
        double nearestY = 0;
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            if (onField(enemy) && inCone(shot, enemy.x(), enemy.y(), best)) {
                best = distanceSquared(shot.x(), shot.y(), enemy.x(), enemy.y());
                nearest = enemy.serial();
                nearestX = enemy.x();
                nearestY = enemy.y();
            }
        }
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            for (int p = 0; p < piece.partCount(); p++) {
                // A high-air boss's open parts are sought all round: the missile climbs to them.
                if (partTarget(piece, p)
                        && (overhead(piece)
                                ? distanceSquared(shot.x(), shot.y(), targetX, targetY) <= best
                                : inCone(shot, targetX, targetY, best))) {
                    best = distanceSquared(shot.x(), shot.y(), targetX, targetY);
                    nearest = PART_SERIAL + k * LevelScript.SetPieceSpec.MAX_PARTS + p;
                    nearestX = targetX;
                    nearestY = targetY;
                }
            }
        }
        targetX = nearestX;
        targetY = nearestY;
        return nearest;
    }

    /** Whether (x, y) is within {@code best} (squared) of the shot and inside its cone. */
    private static boolean inCone(Shot shot, double x, double y, double best) {
        double dx = x - shot.x();
        double dy = y - shot.y();
        double d = dx * dx + dy * dy;
        if (d > best) {
            return false;
        }
        double off = Math.IEEEremainder(StrictMath.atan2(dx, dy) - shot.heading(), 2 * StrictMath.PI);
        return Math.abs(off) <= shot.weapon().coneHalfAngle();
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
                        || !weapon.size().overlaps(shot.x(), shot.y(), enemy.hitbox(), enemy.x(), enemy.y())
                        || (weapon.pierce() > 1 && shot.struck(2 * enemy.serial()))) {
                    continue;
                }
                if (enemy.glances(shot.vx(), shot.vy())) {
                    // A walker's frontal armour: a direct shot from ahead sparks off.
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                    break;
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

    /** Bolts and missiles hit the armed spore mines on the player's layer. */
    void hitMines(Pool<Mine> mines) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            WeaponSpec weapon = shot.weapon();
            if (weapon.delivery().landing() || !weapon.delivery().reaches(Layer.AIR)) {
                continue;
            }
            for (int j = mines.size() - 1; j >= 0; j--) {
                Mine mine = mines.get(j);
                if (!mine.armed()
                        || !weapon.size().overlaps(shot.x(), shot.y(), EnemyGun.MineSpec.BOX, mine.x(), mine.y())) {
                    continue;
                }
                events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y(), shot.mount());
                boolean spent = shot.pierced();
                if (mine.damage(shot.damage())) {
                    hits.mineDestroyed(j);
                }
                if (spent) {
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /**
     * Bolts and missiles that reach a set piece's layer hit its living parts; what touches its
     * armoured body (a boss's in its current pose) instead glances off, as does a shot on a part
     * that takes no damage now (a boss's fire-only turrets, a shut window). A boss on high air is
     * hit only by a missile on the part it is locked onto; it passes beneath the rest.
     */
    void hitSetPieces() {
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            if (!piece.present()) {
                continue;
            }
            List<LevelScript.PartSpec> parts = piece.spec().parts();
            boolean overhead = overhead(piece);
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                WeaponSpec weapon = shot.weapon();
                if (weapon.delivery().landing()
                        || !weapon.delivery().reaches(piece.layer())
                        || (overhead && overhead(shot.target()) != k)) {
                    continue;
                }
                int lockedPart = overhead ? (shot.target() - PART_SERIAL) % LevelScript.SetPieceSpec.MAX_PARTS : -1;
                boolean gone = false;
                for (int p = 0; p < parts.size() && piece.present(); p++) {
                    if (overhead && p != lockedPart) {
                        continue;
                    }
                    double px = piece.partX(p);
                    double py = piece.partY(p);
                    if (piece.partWrecked(p)
                            || !weapon.size()
                                    .overlaps(shot.x(), shot.y(), parts.get(p).box(), px, py)
                            || !PlayField.overlaps(px, py, parts.get(p).box())
                            || (weapon.pierce() > 1 && shot.struck(-1 - k * LevelScript.SetPieceSpec.MAX_PARTS - p))) {
                        continue;
                    }
                    if (piece.partShielded(p)) {
                        // A boss part that takes no damage yet (its descent, a core before its phase).
                        events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                        shots.free(i);
                        gone = true;
                        break;
                    }
                    events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y(), shot.mount());
                    boolean spent = shot.pierced();
                    if (piece.damagePart(p, shot.damage())) {
                        hits.partDestroyed(k, p);
                    }
                    if (spent) {
                        shots.free(i);
                        gone = true;
                        break;
                    }
                }
                if (!gone
                        && !overhead
                        && piece.present()
                        && ((weapon.size().overlaps(shot.x(), shot.y(), piece.body(), piece.x(), piece.y())
                                        && !partAhead(piece, shot))
                                || piece.neckTouches(weapon.size(), shot.x(), shot.y()))) {
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                }
            }
        }
    }

    /**
     * Whether a living part on the field lies ahead of the shot in its line of flight: a shot over
     * the armoured body flies on over the unit's back towards it (the vents and the blowhole sit on
     * the body); a shot with nothing ahead of it glances off the armour.
     */
    private static boolean partAhead(SetPiece piece, Shot shot) {
        double dirX = Trig.sin(shot.heading());
        double dirY = Trig.cos(shot.heading());
        List<LevelScript.PartSpec> parts = piece.spec().parts();
        for (int p = 0; p < parts.size(); p++) {
            Hitbox box = parts.get(p).box();
            double px = piece.partX(p);
            double py = piece.partY(p);
            if (piece.partWrecked(p) || !PlayField.overlaps(px, py, box)) {
                continue;
            }
            double relX = px - shot.x();
            double relY = py - shot.y();
            double reach =
                    (Math.max(box.width(), box.height()) + shot.weapon().size().width()) / 2;
            if (relX * dirX + relY * dirY > 0 && Math.abs(relX * dirY - relY * dirX) < reach) {
                return true;
            }
        }
        return false;
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
                    && inBlast(x, y, weapon.blast(), enemy.x(), enemy.y(), enemy.hitbox())
                    && enemy.damage(shot.damage(), true)) {
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
    static boolean inBlast(double x, double y, double radius, double bx, double by, Hitbox box) {
        double dx = Math.max(Math.abs(x - bx) - box.width() / 2, 0);
        double dy = Math.max(Math.abs(y - by) - box.height() / 2, 0);
        return dx * dx + dy * dy <= radius * radius;
    }

    /**
     * Whether shots, blasts and specials can reach {@code enemy}: on the play field, and not a chain
     * member waiting for its burst ({@link Chain#doomed}).
     */
    static boolean onField(Enemy enemy) {
        return PlayField.overlaps(enemy.x(), enemy.y(), enemy.hitbox()) && !Chain.doomed(enemy);
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
