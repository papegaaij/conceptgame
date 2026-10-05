package vanguard.sim;

/**
 * A tow in play ({@link LevelScript.TowSpec}, part G: Level 07's lifeboat tow): a friendly craft
 * drifting down the screen on the air layer with a pod on a cable. Its place follows the level
 * time, so a retry replays it exactly. Shots, bullets and the ship pass the boat and the pod; the
 * cable counts the player's hits while the pod hangs on it, and the last one cuts it.
 *
 * <p>Cut loose, the pod falls away from the boat on its own: it keeps the tow's drift, slips aside
 * towards the middle of the field ({@link #SLIP}) and falls faster and faster ({@link #FALL}),
 * tumbling, until it has left the screen. The secret's crate
 * falls out of it once it is down in the ship's part of the screen ({@link #RELEASE_Y}), or at once
 * when it is cut there, and is then an ordinary hidden crate (a pickup that drifts and expires like
 * every other), so a ship that stays low can collect it.
 */
public final class Tow implements Hashed {
    /** How fast a loose pod falls faster, px/s² down the screen. */
    static final double FALL = 60;

    /** How fast a loose pod slips aside towards the middle of the field (clear of the boat), px/s. */
    static final double SLIP = 30;

    /** The height (px above the bottom edge, y up) at or below which the crate falls out of a loose pod. */
    static final double RELEASE_Y = 160;

    private final LevelScript.TowSpec spec;
    private final int startTick;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double podX;
    private double podY;
    private double prevPodX;
    private double prevPodY;
    private boolean present;
    private int hitsLeft;
    /** The level tick of the cut; -1 while the pod hangs on the cable. */
    private int cutTick;
    /** Whether the cut pod still holds the crate. */
    private boolean crateDue;
    /** The level tick it was last placed for. */
    private int tick;

    Tow(LevelScript.TowSpec spec) {
        this.spec = spec;
        startTick = SimStep.ticks(spec.t());
        reset();
    }

    /** Back to the level start: not yet entered, the pod on its cable. */
    void reset() {
        restore(spec.hits(), -1, false, 0);
    }

    /**
     * Back to a boss checkpoint at {@code levelTick}: the cable with {@code hitsLeft} hits to take,
     * cut at {@code cutTick} (-1 while it holds), the crate still in the loose pod if {@code crateDue}.
     */
    void restore(int hitsLeft, int cutTick, boolean crateDue, int levelTick) {
        this.hitsLeft = hitsLeft;
        this.cutTick = cutTick;
        this.crateDue = crateDue;
        update(levelTick);
        prevX = x;
        prevY = y;
        prevPodX = podX;
        prevPodY = podY;
    }

    /**
     * Places the tow for {@code levelTick}: it enters at the top edge at its time and drifts until
     * it has left the screen; a loose pod falls from where it was cut.
     */
    void update(int levelTick) {
        prevX = x;
        prevY = y;
        prevPodX = podX;
        prevPodY = podY;
        tick = levelTick;
        x = boatX(levelTick);
        y = boatY(levelTick);
        if (cutTick < 0) {
            podX = x + spec.podDx();
            podY = y + spec.podDy();
        } else {
            double loose = Math.max(0, levelTick - cutTick) * SimStep.SECONDS;
            double cutX = boatX(cutTick) + spec.podDx();
            double slip = cutX < PlayField.WIDTH / 2.0 ? SLIP : -SLIP;
            podX = cutX + (spec.vx() + slip) * loose;
            podY = boatY(cutTick) + spec.podDy() + spec.vy() * loose - FALL * loose * loose / 2;
        }
        present = levelTick >= startTick && !(gone(x, y, spec.boat()) && gone(podX, podY, spec.pod()));
    }

    private double boatX(int levelTick) {
        return spec.x() + spec.vx() * seconds(levelTick);
    }

    private double boatY(int levelTick) {
        return PlayField.HEIGHT + spec.boat().height() / 2 + spec.vy() * seconds(levelTick);
    }

    private double seconds(int levelTick) {
        return Math.max(0, levelTick - startTick) * SimStep.SECONDS;
    }

    /** Whether a box around (bx, by) lies wholly below the bottom edge or beside the field. */
    private static boolean gone(double bx, double by, Hitbox box) {
        return by + box.height() / 2 < 0 || bx + box.width() / 2 < 0 || bx - box.width() / 2 > PlayField.WIDTH;
    }

    /** Whether a shot's box around (sx, sy) hits the cable while it still holds the pod. */
    boolean cableHit(double sx, double sy, Hitbox box) {
        return present && hitsLeft > 0 && spec.cable().overlaps(cableX(), cableY(), box, sx, sy);
    }

    /** Counts a hit on the cable at {@code levelTick}; returns whether it cut it (the pod comes loose with the crate). */
    boolean countHit(int levelTick) {
        if (--hitsLeft > 0) {
            return false;
        }
        cutTick = levelTick;
        crateDue = true;
        return true;
    }

    /**
     * Whether the crate falls out of the loose pod now: once, when the pod is down at
     * {@link #RELEASE_Y} (or below it when cut); the pod's centre is where it drops.
     */
    boolean releaseCrate() {
        if (!crateDue || podY > RELEASE_Y) {
            return false;
        }
        crateDue = false;
        return true;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(x).add(y).add(present ? 1 : 0).add(hitsLeft).add(cutTick).add(crateDue ? 1 : 0);
    }

    public LevelScript.TowSpec spec() {
        return spec;
    }

    /** Whether it is on the screen (or entering it): the boat or the pod. */
    public boolean present() {
        return present;
    }

    /** Whether the pod still hangs on the cable. */
    public boolean holding() {
        return hitsLeft > 0;
    }

    /** The hits the cable takes before it is cut. */
    public int hitsLeft() {
        return hitsLeft;
    }

    /** The level tick of the cut, -1 while the pod hangs on the cable (for the boss checkpoint). */
    int cutTick() {
        return cutTick;
    }

    /** Whether the cut pod still holds the crate (it falls out at {@link #RELEASE_Y}). */
    public boolean crateDue() {
        return crateDue;
    }

    /** Seconds since the cut, to the step's render time ({@code alpha} in [0, 1]); 0 while the pod hangs on the cable. */
    public double looseSeconds(double alpha) {
        return cutTick < 0 ? 0 : Math.max(0, tick - cutTick - 1 + alpha) * SimStep.SECONDS;
    }

    /** The boat's centre, play-field px, y up. */
    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** The pod's centre: on the cable, or falling loose after the cut. */
    public double podX() {
        return podX;
    }

    public double podY() {
        return podY;
    }

    /** The cable's hit box centre: midway between the boat and the pod. */
    public double cableX() {
        return x + spec.podDx() / 2;
    }

    public double cableY() {
        return y + spec.podDy() / 2;
    }

    /** The boat's centre between the previous and the current step. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    /** The pod's centre between the previous and the current step. */
    public double podRenderX(double alpha) {
        return prevPodX + (podX - prevPodX) * alpha;
    }

    public double podRenderY(double alpha) {
        return prevPodY + (podY - prevPodY) * alpha;
    }
}
