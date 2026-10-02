package vanguard.sim;

/**
 * An object on the ground layer, scrolling down with the ground: a destructible (cargo container)
 * or a trigger (the crane beacon) that releases a secret after enough hits and is spent then. It
 * counts the steps since its last hit, which its hit flash and damage state are drawn from.
 */
public final class GroundObject implements Hashed {
    private LevelScript.GroundObjectSpec spec;
    private double y;
    private double prevY;
    private double hp;
    private int hitsLeft;
    private int ticksSinceHit;

    void place(LevelScript.GroundObjectSpec objectSpec) {
        spec = objectSpec;
        y = prevY = PlayField.HEIGHT + objectSpec.size().height() / 2;
        hp = objectSpec.hp();
        hitsLeft = objectSpec.hits();
        ticksSinceHit = Integer.MAX_VALUE;
    }

    /** Scrolls down by {@code distance}; returns false once it has left the bottom edge. */
    boolean scroll(double distance) {
        prevY = y;
        y -= distance;
        if (ticksSinceHit < Integer.MAX_VALUE) {
            ticksSinceHit++;
        }
        return y + spec.size().height() / 2 > 0;
    }

    /** Whether shots still hit it: always for a destructible, until it is spent for a trigger. */
    boolean hittable() {
        return !spec.trigger() || hitsLeft > 0;
    }

    /** A destructible takes damage; returns whether it was destroyed. */
    boolean damage(double amount) {
        ticksSinceHit = 0;
        hp -= amount;
        return hp <= 0;
    }

    /** A trigger counts a hit; returns whether that released its secret. */
    boolean countHit() {
        ticksSinceHit = 0;
        return --hitsLeft == 0;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(spec.x()).add(y).add(hp).add(hitsLeft).add(ticksSinceHit);
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

    /** Simulation steps since the last hit; {@link Integer#MAX_VALUE} before the first. */
    public int ticksSinceHit() {
        return ticksSinceHit;
    }

    /** Whether it has taken a hit (its damage state shows from then on). */
    public boolean damaged() {
        return ticksSinceHit < Integer.MAX_VALUE;
    }

    public double renderX() {
        return spec.x();
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
