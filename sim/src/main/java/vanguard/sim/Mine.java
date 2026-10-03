package vanguard.sim;

/**
 * A spore mine dropped by a mine layer (design/enemies/air/spore-bomber): it drifts in a random
 * direction; below the player plane until it arms, then it can be shot and bursts on contact; at
 * the end of its life it bursts into a ring of bullets (or fades on easy). Pooled.
 */
public final class Mine implements Hashed {
    private EnemyGun gun;
    private EnemyGun.MineSpec spec;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double vx;
    private double vy;
    private double hp;
    private int age;
    private int armTicks;
    private int lifeTicks;

    /** Dropped at (x, y) by a layer with {@code layerGun}, drifting at {@code angle} radians. */
    void drop(EnemyGun layerGun, double startX, double startY, double angle) {
        gun = layerGun;
        spec = layerGun.mine().orElseThrow();
        x = prevX = startX;
        y = prevY = startY;
        vx = spec.drift() * Trig.cos(angle);
        vy = spec.drift() * Trig.sin(angle);
        hp = spec.hp();
        age = 0;
        armTicks = SimStep.ticks(spec.armSeconds());
        lifeTicks = SimStep.ticks(spec.lifeSeconds());
    }

    /** Drifts one step; returns false once its life is over or it has left the play field. */
    boolean move() {
        prevX = x;
        prevY = y;
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
        age++;
        return age < lifeTicks && PlayField.overlaps(x, y, EnemyGun.MineSpec.BOX);
    }

    /** Whether its life ran out (rather than it drifting off the screen). */
    boolean expired() {
        return age >= lifeTicks;
    }

    /** Takes damage; returns whether it was destroyed. */
    boolean damage(double amount) {
        hp -= amount;
        return hp <= 0;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(x).add(y).add(vx).add(vy).add(hp).add(age);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    EnemyGun gun() {
        return gun;
    }

    EnemyGun.MineSpec spec() {
        return spec;
    }

    /** Whether it has risen to the player plane: it can be hit and hits. */
    public boolean armed() {
        return age >= armTicks;
    }

    /** Its share of the time until it arms, 0..1, for the glow that brightens as it rises. */
    public double rising() {
        return Math.min(1, (double) age / Math.max(1, armTicks));
    }

    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
