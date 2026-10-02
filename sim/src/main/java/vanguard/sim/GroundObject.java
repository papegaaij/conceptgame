package vanguard.sim;

/**
 * An object on the ground layer, scrolling down with the ground: a destructible (cargo container)
 * or a trigger (the crane beacon) that releases a secret after enough hits and is spent then.
 */
public final class GroundObject implements Hashed {
    private LevelScript.GroundObjectSpec spec;
    private double y;
    private double prevY;
    private double hp;
    private int hitsLeft;

    void place(LevelScript.GroundObjectSpec objectSpec) {
        spec = objectSpec;
        y = prevY = PlayField.HEIGHT + objectSpec.size().height() / 2;
        hp = objectSpec.hp();
        hitsLeft = objectSpec.hits();
    }

    /** Scrolls down by {@code distance}; returns false once it has left the bottom edge. */
    boolean scroll(double distance) {
        prevY = y;
        y -= distance;
        return y + spec.size().height() / 2 > 0;
    }

    /** Whether shots still hit it: always for a destructible, until it is spent for a trigger. */
    boolean hittable() {
        return !spec.trigger() || hitsLeft > 0;
    }

    /** A destructible takes damage; returns whether it was destroyed. */
    boolean damage(double amount) {
        hp -= amount;
        return hp <= 0;
    }

    /** A trigger counts a hit; returns whether that released its secret. */
    boolean countHit() {
        return --hitsLeft == 0;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(spec.x()).add(y).add(hp).add(hitsLeft);
    }

    double x() {
        return spec.x();
    }

    double y() {
        return y;
    }

    public LevelScript.GroundObjectSpec spec() {
        return spec;
    }

    /** Whether it is a trigger whose secret was found. */
    public boolean spent() {
        return spec.trigger() && hitsLeft <= 0;
    }

    public double renderX() {
        return spec.x();
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
