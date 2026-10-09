package vanguard.sim;

/**
 * A projectile of one of the ship's weapons: a bolt flying straight (a turret's shot too), a homing
 * missile, a bomb or shell on its way to the ground, a proximity mine holding its place or (M5 part
 * E) a torpedo running under the water (see {@link WeaponSpec.Delivery}). Pooled: {@link #fire}
 * reuses the instance.
 */
public final class Shot implements Hashed {
    /** The most targets a piercing bolt remembers, so it hits each only once. */
    static final int MAX_STRUCK = 8;

    private WeaponSpec weapon;
    private int mount;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double vx;
    private double vy;
    /** Radians clockwise from straight up the screen. */
    private double heading;

    private double damage;
    private double travelled;
    private int ticks;
    private int pierceLeft;
    private final int[] struck = new int[MAX_STRUCK];
    private int struckCount;
    /** A homing shot's (or a torpedo's) locked target, a {@link Enemy#serial()}; -1 while it has none. */
    private int target;

    private double startX;
    private double startY;
    private double landX;
    private double landY;
    private int airTicks;
    /** The share of the full lob it flies, 0..1: its arc's height; 1 for a full lob or a bomb. */
    private double arc = 1;

    /** A bolt, a homing missile, a mine or a torpedo leaving (x, y) at {@code angle}. */
    void fire(WeaponSpec spec, int mountIndex, double startX, double startY, double angle) {
        start(spec, mountIndex, startX, startY);
        heading = angle;
        vx = spec.speed() * Trig.sin(angle);
        vy = spec.speed() * Trig.cos(angle);
    }

    /** A bomb or shell released at (x, y) that lands at (landX, landY) on the ground. */
    void lob(WeaponSpec spec, int mountIndex, double releaseX, double releaseY, double groundX, double groundY) {
        lob(spec, mountIndex, releaseX, releaseY, groundX, groundY, 1);
    }

    /**
     * A shell lobbed a {@code share} (0..1) of its full range: it flies that share of the weapon's
     * air time, on an arc that much lower (Rook's aimed Mortar).
     */
    void lob(
            WeaponSpec spec,
            int mountIndex,
            double releaseX,
            double releaseY,
            double groundX,
            double groundY,
            double share) {
        start(spec, mountIndex, releaseX, releaseY);
        landX = groundX;
        landY = groundY;
        arc = share;
        airTicks = Math.max(1, SimStep.ticks(spec.airSeconds() * share));
        heading = 0;
        vx = vy = 0;
        placeInAir();
        prevX = x;
        prevY = y;
    }

    private void start(WeaponSpec spec, int mountIndex, double startX, double startY) {
        weapon = spec;
        mount = mountIndex;
        x = prevX = this.startX = startX;
        y = prevY = this.startY = startY;
        damage = spec.damage();
        travelled = 0;
        ticks = 0;
        pierceLeft = spec.pierce();
        struckCount = 0;
        target = -1;
        arc = 1;
    }

    /** One step of straight flight; an accelerating missile speeds up along its heading. */
    void move() {
        prevX = x;
        prevY = y;
        double speed = speed();
        if (weapon.accelSeconds() > 0) {
            vx = speed * Trig.sin(heading);
            vy = speed * Trig.cos(heading);
        }
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
        travelled += speed * SimStep.SECONDS;
        ticks++;
    }

    /** Its speed now, px/s: an accelerating missile's grows from the weapon's speed to its end speed. */
    double speed() {
        if (weapon.accelSeconds() <= 0) {
            return weapon.speed();
        }
        double share = Math.min(1, ticks * SimStep.SECONDS / weapon.accelSeconds());
        return weapon.speed() + (weapon.endSpeed() - weapon.speed()) * share;
    }

    /**
     * One step of a mine: its drift decays to nothing over the weapon's drift time, then it holds its
     * screen position.
     */
    void drift() {
        prevX = x;
        prevY = y;
        double left = 1 - ticks * SimStep.SECONDS / weapon.mines().driftSeconds();
        if (left > 0) {
            x += vx * left * SimStep.SECONDS;
            y += vy * left * SimStep.SECONDS;
        }
        ticks++;
    }

    /** Whether a mine has armed: it bursts when an enemy comes close. */
    public boolean armed() {
        return ticks >= SimStep.ticks(weapon.mines().armSeconds());
    }

    /** Whether a mine armed in the step it just took (its arming beep). */
    boolean armsNow() {
        return ticks == SimStep.ticks(weapon.mines().armSeconds());
    }

    /** Steps since it left the muzzle. */
    public int age() {
        return ticks;
    }

    /** Its velocity across the screen, px/s (y up); 0 for a bomb or shell. */
    double vx() {
        return vx;
    }

    double vy() {
        return vy;
    }

    /** Turns towards (tx, ty) by at most the weapon's turn rate times {@code turnFactor} for one step. */
    void steer(double tx, double ty, double turnFactor) {
        double wanted = StrictMath.atan2(tx - x, ty - y);
        double delta = Math.IEEEremainder(wanted - heading, 2 * StrictMath.PI);
        double most = weapon.turnRate() * turnFactor * SimStep.SECONDS;
        heading = Math.IEEEremainder(heading + Math.clamp(delta, -most, most), 2 * StrictMath.PI);
        double speed = speed();
        vx = speed * Trig.sin(heading);
        vy = speed * Trig.cos(heading);
    }

    /**
     * One step on the way to the ground: the landing point scrolls down with the ground by
     * {@code scroll}. Returns whether it lands in this step.
     */
    boolean fall(double scroll) {
        prevX = x;
        prevY = y;
        landY -= scroll;
        ticks++;
        placeInAir();
        return ticks >= airTicks;
    }

    /** A dropped bomb is seen at its landing point; a lobbed shell between the muzzle and it. */
    private void placeInAir() {
        if (weapon.delivery() == WeaponSpec.Delivery.DROPPED) {
            x = landX;
            y = landY;
        } else {
            double p = (double) ticks / airTicks;
            x = startX + (landX - startX) * p;
            y = startY + (landY - startY) * p;
        }
    }

    /** Whether it already hit the target with {@code key}; otherwise remembers it if there is room. */
    boolean struck(int key) {
        for (int i = 0; i < struckCount; i++) {
            if (struck[i] == key) {
                return true;
            }
        }
        if (struckCount < MAX_STRUCK) {
            struck[struckCount++] = key;
        }
        return false;
    }

    /** Counts one target it passed through; returns whether it is spent. */
    boolean pierced() {
        return --pierceLeft <= 0;
    }

    /** Whether it has flown (or a torpedo run) its range, or its lifetime for a homing shot or a mine. */
    boolean spent() {
        if (weapon.delivery() == WeaponSpec.Delivery.HOMING || weapon.delivery() == WeaponSpec.Delivery.MINE) {
            return ticks * SimStep.SECONDS >= weapon.lifetimeSeconds();
        }
        return travelled >= weapon.range();
    }

    void lock(int serial) {
        target = serial;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(mount)
                .add(x)
                .add(y)
                .add(vx)
                .add(vy)
                .add(heading)
                .add(damage)
                .add(travelled)
                .add(ticks)
                .add(pierceLeft)
                .add(struckCount)
                .add(target)
                .add(landX)
                .add(landY);
        if (weapon.delivery() == WeaponSpec.Delivery.LOBBED) {
            // A shell's flight: Rook's aimed lobs fly shorter (user decision 2026-10-07).
            hash.add(airTicks);
        }
        for (int i = 0; i < struckCount; i++) {
            hash.add(struck[i]);
        }
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double damage() {
        return damage;
    }

    int target() {
        return target;
    }

    double landX() {
        return landX;
    }

    double landY() {
        return landY;
    }

    public WeaponSpec weapon() {
        return weapon;
    }

    /** The index of the mount that fired it, in the sortie's {@link Armament}. */
    public int mount() {
        return mount;
    }

    /** Its heading, radians clockwise from straight up the screen. */
    public double heading() {
        return heading;
    }

    /** How far a dropped or lobbed projectile is on its way to the ground, 0..1; 0 for others. */
    public double airProgress(double alpha) {
        if (!weapon.delivery().landing()) {
            return 0;
        }
        return Math.min(1, (ticks + alpha) / airTicks);
    }

    /** The height of a lobbed shell's arc as a share of a full-range lob's, 0..1 (Rook aims shorter lobs). */
    public double arc() {
        return arc;
    }

    /** The share of a bolt's range that is left, 1 for one that flies to the screen edge. */
    public double rangeLeft() {
        if ((weapon.delivery() != WeaponSpec.Delivery.BOLT && weapon.delivery() != WeaponSpec.Delivery.TURRET)
                || Double.isInfinite(weapon.range())) {
            return 1;
        }
        return Math.max(0, 1 - travelled / weapon.range());
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
