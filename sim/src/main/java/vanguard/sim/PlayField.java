package vanguard.sim;

/**
 * The 480x540 play field (design/art-direction, screen geometry). Simulation coordinates are
 * play-field pixels: x from the left edge, y from the bottom edge upwards.
 */
public final class PlayField {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 540;

    private PlayField() {}

    /** Whether a box of the given size around (x, y) overlaps the play field. */
    public static boolean overlaps(double x, double y, Hitbox box) {
        return x + box.width() / 2 > 0
                && x - box.width() / 2 < WIDTH
                && y + box.height() / 2 > 0
                && y - box.height() / 2 < HEIGHT;
    }
}
