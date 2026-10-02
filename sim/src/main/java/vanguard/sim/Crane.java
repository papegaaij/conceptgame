package vanguard.sim;

/**
 * A crane hazard in play ({@link LevelScript.CraneSpec}): its arm's angle follows the level time,
 * so a retry replays it exactly. Folded along the gantry (out of the play field) until the
 * telegraph before its first swing, it is lowered to its first angle while its lights blink, swings
 * between its two angles at the scripted times (easing in and out), rests at the end of each swing,
 * and after its last swing is raised back along the gantry on that side. Its clamp counts hits
 * only while the arm swings.
 */
public final class Crane implements Hashed {
    /** Points along the arm tested against a box, this far apart, px. */
    private static final double SAMPLE_STEP = 4;
    /**
     * The clamp's hit box at the arm's tip: it hangs below the arm's end with the hook, so a shot
     * from below meets it before the arm.
     */
    static final Hitbox CLAMP = new Hitbox(16, 20);

    private static final double FOLDED = StrictMath.PI / 2;

    private final LevelScript.CraneSpec spec;
    private final int[] swingStarts;
    private final int swingTicks;
    private final int telegraphTicks;
    private double angle;
    private double prevAngle;
    private boolean present;
    private boolean swinging;
    private boolean telegraph;
    private int hitCooldown;
    private int clampHitsLeft;

    Crane(LevelScript.CraneSpec spec) {
        this.spec = spec;
        swingStarts = new int[spec.swings().size()];
        for (int i = 0; i < swingStarts.length; i++) {
            swingStarts[i] = SimStep.ticks(spec.swings().get(i));
        }
        swingTicks = Math.max(1, SimStep.ticks(spec.swingSeconds()));
        telegraphTicks = Math.max(1, SimStep.ticks(spec.telegraphSeconds()));
        reset();
    }

    /** Back to the level start: folded, the clamp whole. */
    void reset() {
        clampHitsLeft = spec.clampHits();
        hitCooldown = 0;
        update(0);
        prevAngle = angle;
    }

    /** Places the arm for {@code levelTick}. */
    void update(int levelTick) {
        prevAngle = angle;
        if (hitCooldown > 0) {
            hitCooldown--;
        }
        int first = swingStarts[0];
        int last = swingStarts.length - 1;
        double lastEnd = swingEnd(last);
        double foldedFrom = Math.copySign(FOLDED, spec.fromRadians());
        double foldedEnd = Math.copySign(FOLDED, lastEnd);
        int raised = swingStarts[last] + swingTicks + telegraphTicks;
        swinging = false;
        telegraph = false;
        present = levelTick >= first - telegraphTicks && levelTick < raised;
        if (levelTick < first - telegraphTicks) {
            angle = foldedFrom;
        } else if (levelTick < first) {
            telegraph = true;
            angle = ease(foldedFrom, spec.fromRadians(), 1 - (double) (first - levelTick) / telegraphTicks);
        } else if (levelTick >= swingStarts[last] + swingTicks) {
            angle = ease(
                    lastEnd,
                    foldedEnd,
                    Math.min(1, (double) (levelTick - swingStarts[last] - swingTicks) / telegraphTicks));
        } else {
            for (int i = last; i >= 0; i--) {
                if (levelTick >= swingStarts[i]) {
                    int into = levelTick - swingStarts[i];
                    if (into < swingTicks) {
                        swinging = true;
                        angle = ease(swingStart(i), swingEnd(i), (double) into / swingTicks);
                    } else {
                        angle = swingEnd(i);
                        telegraph = i < last && levelTick >= swingStarts[i + 1] - telegraphTicks;
                    }
                    break;
                }
            }
        }
    }

    private double swingStart(int i) {
        return i % 2 == 0 ? spec.fromRadians() : spec.toRadians();
    }

    private double swingEnd(int i) {
        return i % 2 == 0 ? spec.toRadians() : spec.fromRadians();
    }

    /** From {@code a} to {@code b}, easing in and out over {@code progress} 0..1. */
    private static double ease(double a, double b, double progress) {
        return a + (b - a) * (1 - Trig.cos(StrictMath.PI * progress)) / 2;
    }

    /** Whether the arm, as thick as it is, touches a box of half-extents (halfW, halfH) around (x, y). */
    boolean touches(double x, double y, double halfW, double halfH) {
        if (!present) {
            return false;
        }
        double dx = Trig.sin(angle);
        double dy = -Trig.cos(angle);
        double reachX = halfW + spec.width() / 2;
        double reachY = halfH + spec.width() / 2;
        for (double s = 0; s <= spec.length(); s += SAMPLE_STEP) {
            if (Math.abs(spec.pivotX() + dx * s - x) < reachX && Math.abs(spec.pivotY() + dy * s - y) < reachY) {
                return true;
            }
        }
        return false;
    }

    /** Whether a box around (x, y) hits the clamp while it can be hit (the arm swings, not yet released). */
    boolean clampHit(double x, double y, Hitbox box) {
        return swinging && clampHitsLeft > 0 && CLAMP.overlaps(tipX(), tipY(), box, x, y);
    }

    /** Counts a hit on the clamp; returns whether it released the crate. */
    boolean countClampHit() {
        return --clampHitsLeft == 0;
    }

    /** Whether the arm may hit the ship now; starts the wait until it may again. */
    boolean strike() {
        if (hitCooldown > 0) {
            return false;
        }
        hitCooldown = SimStep.ticks(LevelScript.CraneSpec.HIT_INTERVAL_SECONDS);
        return true;
    }

    double tipX() {
        return spec.pivotX() + Trig.sin(angle) * spec.length();
    }

    double tipY() {
        return spec.pivotY() - Trig.cos(angle) * spec.length();
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(angle).add(hitCooldown).add(clampHitsLeft);
    }

    public LevelScript.CraneSpec spec() {
        return spec;
    }

    /** The arm's angle between the previous and the current step, radians from straight down, positive to the right. */
    public double renderAngle(double alpha) {
        return prevAngle + (angle - prevAngle) * alpha;
    }

    /** Whether the arm is lowered into the play field. */
    public boolean present() {
        return present;
    }

    /** Whether its lights blink: the telegraph before a swing. */
    public boolean telegraph() {
        return telegraph;
    }

    /** Whether it swings now: its clamp lights up and can be hit. */
    public boolean swinging() {
        return swinging;
    }

    /** Whether the clamp still holds its crate. */
    public boolean holding() {
        return clampHitsLeft > 0;
    }
}
