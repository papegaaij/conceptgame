package vanguard.game.settings;

/** How the 960x540 screen fills the window (design/art-direction, Internal resolution). */
public enum Scaling {
    /** The largest whole-number scale, nearest neighbour, black borders round it: crisp pixels. */
    INTEGER,
    /** Fills the window at any scale: integer pre-scale, then bilinear; slightly soft. */
    SHARP_BILINEAR
}
