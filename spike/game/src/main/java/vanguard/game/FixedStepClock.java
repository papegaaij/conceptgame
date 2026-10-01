package vanguard.game;

/**
 * Converts variable frame times into a whole number of fixed simulation steps, keeping the
 * remainder for the next frame. {@link #alpha()} tells the renderer how far it is between the
 * last two steps.
 */
public final class FixedStepClock {
    private final double stepSeconds;
    private final int maxStepsPerFrame;
    private double accumulator;

    public FixedStepClock(double stepSeconds, int maxStepsPerFrame) {
        this.stepSeconds = stepSeconds;
        this.maxStepsPerFrame = maxStepsPerFrame;
    }

    /** Adds the frame time and returns how many steps to run; long stalls are clamped. */
    public int advance(double frameSeconds) {
        accumulator += Math.min(frameSeconds, stepSeconds * maxStepsPerFrame);
        int steps = (int) (accumulator / stepSeconds);
        accumulator -= steps * stepSeconds;
        return steps;
    }

    /** Interpolation factor in [0, 1) between the previous and the current step. */
    public float alpha() {
        return (float) (accumulator / stepSeconds);
    }
}
