package vanguard.sim;

import java.util.List;

/**
 * <strong>Temporary M1 sandbox</strong>, replaced by the Level 01 data in M2: a short, repeating
 * cycle of Skitter snakes over the Level 01 opening scroll
 * (design/campaign/act-1-first-contact/level-01-break-at-dawn, Layout). The paths are hand-made
 * here in the spirit of Level 01's first waves: curls from the left and right, a hook and a sweep
 * across the player's lane. Every wave gets a random horizontal offset, so repeats differ a little.
 */
public final class TestSortie {
    /** Level 01's ground scroll speed in px/s. */
    public static final double SCROLL_SPEED = 130;
    /** The cycle repeats after this many seconds. */
    static final double CYCLE_SECONDS = 26;
    /** The largest random horizontal offset of a wave, either way. */
    static final double MAX_OFFSET = 30;

    private static final int SNAKE = 6;
    /** Enters top left, curls towards the centre and leaves through the bottom. */
    private static final SnakePath CURL =
            SnakePath.through(100, 580, 100, 420, 170, 300, 300, 260, 360, 180, 330, 60, 300, -40);
    /** Dives from the top right into the lower third, hooks back and leaves through the left edge. */
    private static final SnakePath HOOK =
            SnakePath.through(330, 580, 330, 350, 280, 200, 180, 160, 90, 220, 40, 340, -40, 420);
    /** Enters from the left edge and sweeps down across the player's lane to the right edge. */
    private static final SnakePath SWEEP = SnakePath.through(-30, 470, 100, 420, 240, 260, 380, 200, 520, 230);

    static final List<SnakeWave> WAVES = List.of(
            new SnakeWave(1, CURL, SNAKE, false),
            new SnakeWave(6, CURL, SNAKE, true),
            new SnakeWave(11, HOOK, SNAKE, false),
            new SnakeWave(16, SWEEP, SNAKE, false),
            new SnakeWave(20, HOOK, SNAKE, true));

    private TestSortie() {}
}
