package vanguard.sim;

/**
 * The mass-driver sleds (design/world/luna, hazards; Level 05): a sled shoots up the rail every
 * period, a vertical line across the whole play field. The rail lights chase upward for their
 * telegraph before each sled; while a sled is on the screen it hits the ship whatever its layer
 * (once per sled) and blocks the player's shots and the enemy bullets. Its state follows the level
 * time, so a retry replays it exactly.
 */
public final class Sled implements Hashed {
    private final LevelScript.SledSpec spec;
    private final int firstTicks;
    private final int periodTicks;
    private final int untilTicks;
    private final int lightsTicks;
    private final int runTicks;
    private final Hitbox line;
    /** The launch of the current cycle, steps; -1 outside the sleds' stretch. */
    private int launch = -1;

    private int tick;
    /** The launch whose sled already struck the ship; -1 for none. */
    private int struck = -1;

    Sled(LevelScript.SledSpec spec) {
        this.spec = spec;
        firstTicks = SimStep.ticks(spec.firstSeconds());
        periodTicks = Math.max(1, SimStep.ticks(spec.periodSeconds()));
        untilTicks = SimStep.ticks(spec.untilSeconds());
        lightsTicks = SimStep.ticks(spec.lightsSeconds());
        runTicks = Math.max(1, SimStep.ticks(spec.runSeconds()));
        line = new Hitbox(spec.width(), 2 * PlayField.HEIGHT);
    }

    /** Back to the level start. */
    void reset() {
        struck = -1;
        update(0);
    }

    /**
     * Places the cycle for {@code levelTick}; returns {@link SimEvents.Type#SLED_LIGHTS} or
     * {@link SimEvents.Type#SLED_LAUNCHED} when the lights start or a sled launches in this step,
     * null otherwise.
     */
    SimEvents.Type update(int levelTick) {
        tick = levelTick;
        launch = -1;
        if (levelTick < firstTicks - lightsTicks) {
            return null;
        }
        // The next launch at or after now - run: the cycle the lights or the sled belong to.
        int k = Math.max(0, Math.floorDiv(levelTick - runTicks - firstTicks, periodTicks) + 1);
        int next = firstTicks + k * periodTicks;
        if (next >= untilTicks) {
            return null;
        }
        launch = next;
        if (levelTick == next - lightsTicks) {
            return SimEvents.Type.SLED_LIGHTS;
        }
        return levelTick == next ? SimEvents.Type.SLED_LAUNCHED : null;
    }

    /** Whether the rail lights chase now: the telegraph before a sled. */
    public boolean lights() {
        return launch >= 0 && tick >= launch - lightsTicks && tick < launch;
    }

    /** Whether a sled is on the screen now. */
    public boolean running() {
        return launch >= 0 && tick >= launch && tick < launch + runTicks;
    }

    /** Whether the rail is lit (the lights or a sled): the stuck sled's clamp cannot be hit then. */
    public boolean lit() {
        return lights() || running();
    }

    /** The running sled's way up the screen, 0..1; 0 when none runs. */
    public double runProgress(double alpha) {
        return running() ? Math.min(1, (tick - launch + alpha) / runTicks) : 0;
    }

    /** The lights' share of their telegraph, 0..1; 0 when they do not chase. */
    public double lightsProgress(double alpha) {
        return lights() ? Math.min(1, (tick - (launch - lightsTicks) + alpha) / lightsTicks) : 0;
    }

    /** Whether a box of {@code box} around (x, y) touches the line of a running sled. */
    boolean blocks(double x, double y, Hitbox box) {
        return running() && line.overlaps(spec.x(), PlayField.HEIGHT / 2.0, box, x, y);
    }

    /** Whether the running sled hits the ship's {@code hull} at (x, y) now: once per sled. */
    boolean strikes(Hull hull, double x, double y) {
        if (!running() || struck == launch || !hull.overlaps(x, y, line, spec.x(), PlayField.HEIGHT / 2.0)) {
            return false;
        }
        struck = launch;
        return true;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(launch).add(struck);
    }

    public LevelScript.SledSpec spec() {
        return spec;
    }
}
