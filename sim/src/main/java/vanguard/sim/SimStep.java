package vanguard.sim;

/** The fixed simulation step: 60 steps per second, whatever the frame rate. */
public final class SimStep {
    public static final int PER_SECOND = 60;
    public static final double SECONDS = 1.0 / PER_SECOND;

    private SimStep() {}

    /** The nearest whole number of steps for a duration. */
    public static int ticks(double seconds) {
        return (int) Math.round(seconds * PER_SECOND);
    }
}
