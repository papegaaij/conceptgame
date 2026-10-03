package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A huge set-piece unit flying its passes (design/enemies/space/leviathan): its centre follows
 * each pass's path with the pass's fixed heading; its parts are hit boxes at fixed offsets around
 * it that take the hits, fire while it is on the player's layer and stay wrecked once destroyed.
 * Its position is a function of the level time, so a restart only resets the parts. One instance
 * per {@link LevelScript.SetPieceSpec}, made when the sortie is.
 */
public final class SetPiece implements Hashed {
    private final LevelScript.SetPieceSpec spec;
    private final double extent;
    private final double[] partHp;
    private final int[] partTicksSinceHit;
    private final int[] volleyTicks;
    private final int[] burstLeft;
    private final int[] burstTicks;

    private int pass;
    private boolean present;
    private boolean destroyed;
    private boolean escaped;
    private boolean onPlane;
    private Layer layer;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double altitude;
    private double prevAltitude;
    private double cos;
    private double sin;
    private int hitCooldown;

    SetPiece(LevelScript.SetPieceSpec spec) {
        this.spec = spec;
        extent = Math.max(spec.size().width(), spec.size().height()) / 2;
        int parts = spec.parts().size();
        partHp = new double[parts];
        partTicksSinceHit = new int[parts];
        volleyTicks = new int[parts];
        burstLeft = new int[parts];
        burstTicks = new int[parts];
        reset();
    }

    /** Back to the level start: before its first pass, every part whole. */
    void reset() {
        pass = 0;
        present = false;
        destroyed = false;
        escaped = false;
        onPlane = false;
        layer = Layer.HIGH_AIR;
        x = y = prevX = prevY = 0;
        altitude = prevAltitude = 1;
        cos = 1;
        sin = 0;
        hitCooldown = 0;
        for (int p = 0; p < partHp.length; p++) {
            partHp[p] = spec.parts().get(p).hp();
            partTicksSinceHit[p] = Integer.MAX_VALUE;
            volleyTicks[p] = 0;
            burstLeft[p] = 0;
            burstTicks[p] = 0;
        }
    }

    /**
     * Moves it to where its pass has it at {@code levelTick}; returns whether its last pass ended
     * with it alive this step (it escaped).
     */
    boolean update(int levelTick) {
        prevX = x;
        prevY = y;
        prevAltitude = altitude;
        if (hitCooldown > 0) {
            hitCooldown--;
        }
        for (int p = 0; p < partTicksSinceHit.length; p++) {
            if (partTicksSinceHit[p] < Integer.MAX_VALUE) {
                partTicksSinceHit[p]++;
            }
        }
        if (destroyed || escaped) {
            return false;
        }
        double t = levelTick * SimStep.SECONDS;
        List<LevelScript.Pass> passes = spec.passes();
        while (pass < passes.size()) {
            LevelScript.Pass current = passes.get(pass);
            if (t < current.start()) {
                present = false;
                return false;
            }
            if (place(current, t)) {
                if (!present) {
                    // Entering a pass: it appears at its first point, without a jump from the last one.
                    present = true;
                    prevX = x;
                    prevY = y;
                    prevAltitude = altitude;
                    cos = Trig.cos(current.headingRadians());
                    sin = Trig.sin(current.headingRadians());
                }
                boolean plane = layer.collidesWithPlayer();
                if (plane && !onPlane) {
                    arm();
                }
                onPlane = plane;
                return false;
            }
            present = false;
            onPlane = false;
            pass++;
        }
        escaped = true;
        return true;
    }

    /** Places it on {@code pass} at {@code t}; returns false once the pass is over. */
    private boolean place(LevelScript.Pass current, double t) {
        List<LevelScript.Waypoint> path = current.path();
        if (!current.descends()) {
            if (t > path.getLast().t()) {
                return false;
            }
            along(path, t);
            layer = current.layer();
            altitude = layer == Layer.HIGH_AIR ? 1 : 0;
            return true;
        }
        if (t < current.leaveAt()) {
            along(path, t);
            if (t < current.descendAt() + current.descentSeconds()) {
                layer = Layer.HIGH_AIR;
                altitude = t < current.descendAt() ? 1 : 1 - (t - current.descendAt()) / current.descentSeconds();
            } else {
                layer = current.layer();
                altitude = 0;
            }
            return true;
        }
        along(path, current.leaveAt());
        y += (t - current.leaveAt()) * current.leaveSpeed();
        layer = Layer.HIGH_AIR;
        altitude = Math.min(1, (t - current.leaveAt()) / current.descentSeconds());
        return y - extent < PlayField.HEIGHT;
    }

    /** Its centre on the path at {@code t}: between the waypoints around it, at the last one after it. */
    private void along(List<LevelScript.Waypoint> path, double t) {
        LevelScript.Waypoint last = path.getLast();
        if (t >= last.t()) {
            x = last.x();
            y = last.y();
            return;
        }
        for (int i = 1; i < path.size(); i++) {
            LevelScript.Waypoint to = path.get(i);
            if (t < to.t()) {
                LevelScript.Waypoint from = path.get(i - 1);
                double p = Math.max(0, (t - from.t()) / (to.t() - from.t()));
                x = from.x() + (to.x() - from.x()) * p;
                y = from.y() + (to.y() - from.y()) * p;
                return;
            }
        }
    }

    /** It reached the player's layer: every living part's gun starts on its first-shot time. */
    private void arm() {
        for (int p = 0; p < partHp.length; p++) {
            LevelScript.PartSpec part = spec.parts().get(p);
            volleyTicks[p] = SimStep.ticks(part.firstShotSeconds()) + 1;
            burstLeft[p] = 0;
            burstTicks[p] = 0;
        }
    }

    /** Whether part {@code p} fires a shot this step: it lives, has a gun and the unit is on the player's layer. */
    boolean trigger(int p) {
        LevelScript.PartSpec part = spec.parts().get(p);
        if (!present || !onPlane || partHp[p] <= 0 || part.gun().isEmpty()) {
            return false;
        }
        EnemyGun gun = part.gun().get();
        if (burstLeft[p] > 0) {
            if (--burstTicks[p] > 0) {
                return false;
            }
            burstLeft[p]--;
            burstTicks[p] = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
            return true;
        }
        if (--volleyTicks[p] > 0) {
            return false;
        }
        volleyTicks[p] = SimStep.ticks(gun.intervalSeconds());
        burstLeft[p] = gun.burst() - 1;
        burstTicks[p] = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
        return true;
    }

    /** Part {@code p} takes damage; returns whether that destroyed it. */
    boolean damagePart(int p, double amount) {
        partTicksSinceHit[p] = 0;
        boolean alive = partHp[p] > 0;
        partHp[p] -= amount;
        return alive && partHp[p] <= 0;
    }

    /** Destroys part {@code p} outright (the vital part took the rest with it); returns whether it still lived. */
    boolean wreckPart(int p) {
        boolean alive = partHp[p] > 0;
        partHp[p] = Math.min(partHp[p], 0);
        return alive;
    }

    /** The unit is destroyed: it leaves the play field at once (its death plays where it was). */
    void destroy() {
        destroyed = true;
        present = false;
        onPlane = false;
    }

    /** The body touching the ship: whether it deals its contact damage now (at most once per its interval). */
    boolean strike() {
        if (hitCooldown > 0) {
            return false;
        }
        hitCooldown = SimStep.ticks(LevelScript.SetPieceSpec.HIT_INTERVAL_SECONDS);
        return true;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(pass)
                .add(present ? 1 : 0)
                .add(destroyed ? 1 : 0)
                .add(escaped ? 1 : 0)
                .add(onPlane ? 1 : 0)
                .add(layer.ordinal())
                .add(x)
                .add(y)
                .add(altitude)
                .add(hitCooldown);
        for (int p = 0; p < partHp.length; p++) {
            hash.add(partHp[p])
                    .add(partTicksSinceHit[p])
                    .add(volleyTicks[p])
                    .add(burstLeft[p])
                    .add(burstTicks[p]);
        }
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    public LevelScript.SetPieceSpec spec() {
        return spec;
    }

    /** Its enemy's slug ({@code leviathan}): the sprites and the death effect go by it. */
    public String slug() {
        return spec.slug();
    }

    /** Whether it is on screen in a pass (not before, between or after its passes, nor once destroyed). */
    public boolean present() {
        return present;
    }

    public boolean destroyed() {
        return destroyed;
    }

    /** Whether it ended its last pass alive. */
    public boolean escaped() {
        return escaped;
    }

    /** The index of the pass it flies (or will fly next) in {@link LevelScript.SetPieceSpec#passes()}. */
    public int pass() {
        return pass;
    }

    /** The name of that pass ({@code cross}, {@code descend}); empty after the last. */
    public String passName() {
        return pass < spec.passes().size() ? spec.passes().get(pass).name() : "";
    }

    /** The layer it flies on now: {@code high-air} out of reach, {@code air} once descended. */
    public Layer layer() {
        return layer;
    }

    /** Whether it is on the player's layer: its guns fire and its body collides. */
    public boolean onPlane() {
        return onPlane;
    }

    /** Where it faces, radians clockwise from straight down the screen, as {@link Enemy#facing()}; fixed per pass. */
    public double facing() {
        return present ? spec.passes().get(pass).headingRadians() : 0;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    /** How high above the player's layer it flies: 1 on {@code high-air}, 0 on {@code air}, in between while it descends or rises. */
    public double altitude(double alpha) {
        return prevAltitude + (altitude - prevAltitude) * alpha;
    }

    public int partCount() {
        return partHp.length;
    }

    /** Whether part {@code p} is destroyed: it no longer fires or takes hits and is drawn wrecked. */
    public boolean partWrecked(int p) {
        return partHp[p] <= 0;
    }

    /** Part {@code p}'s HP left (at most its spec's, 0 or less once wrecked). */
    public double partHp(int p) {
        return partHp[p];
    }

    /** Simulation steps since part {@code p} was last hit, for its hit flash; {@link Integer#MAX_VALUE} before. */
    public int partTicksSinceHit(int p) {
        return partTicksSinceHit[p];
    }

    /** Part {@code p}'s centre on the screen now: its offset turned with the heading. */
    public double partX(int p) {
        return x + partOffsetX(p);
    }

    /** Part {@code p}'s centre on the screen now (y up). */
    public double partY(int p) {
        return y + partOffsetY(p);
    }

    /**
     * Part {@code p}'s offset from the centre on the screen, px to the right: its data offset turned
     * with the pass's heading (fixed per pass), so {@code renderX(alpha) + partOffsetX(p)} draws it.
     */
    public double partOffsetX(int p) {
        LevelScript.PartSpec part = spec.parts().get(p);
        return part.dx() * cos + part.dy() * sin;
    }

    /** Part {@code p}'s offset from the centre on the screen, px up. */
    public double partOffsetY(int p) {
        LevelScript.PartSpec part = spec.parts().get(p);
        return -part.dx() * sin + part.dy() * cos;
    }

    /** Whether every part is wrecked. */
    boolean allWrecked() {
        for (double hp : partHp) {
            if (hp > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return spec.slug() + " pass " + pass + " at " + x + ", " + y + " parts " + Arrays.toString(partHp);
    }
}
