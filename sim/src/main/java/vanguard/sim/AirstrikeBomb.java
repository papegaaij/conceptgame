package vanguard.sim;

/**
 * A falling Airstrike bomb, seen at the ground point under its release, which the scroll carries
 * down until it bursts. Pooled: {@link #release} reuses the instance.
 */
public final class AirstrikeBomb implements Hashed {
    private double x;
    private double y;
    private double prevY;
    private int ticks;
    private int fallTicks;

    void release(double atX, double atY, int fall) {
        x = atX;
        y = prevY = atY;
        ticks = 0;
        fallTicks = fall;
    }

    /** Falls one step while the ground scrolls {@code scroll} px; returns whether it has landed. */
    boolean fall(double scroll) {
        prevY = y;
        y -= scroll;
        ticks++;
        return ticks >= fallTicks;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    public double renderX() {
        return x;
    }

    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    /** How far it has fallen, 0 at the release and 1 at the blast. */
    public double progress(double alpha) {
        return Math.clamp((ticks - 1 + alpha) / fallTicks, 0, 1);
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(x).add(y).add(prevY).add(ticks).add(fallTicks);
    }
}
