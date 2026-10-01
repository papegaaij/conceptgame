package vanguard.game.input;

/**
 * The player's actions (design/ui/controls). The in-level actions are the remappable ones; the
 * two menu actions keep their fixed keys.
 */
public enum Action {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    /** Fires all weapons. */
    FIRE,
    SPECIAL,
    /** Held: slower, finer movement. */
    PRECISION,
    /** Evasive thrusters; only with the module fitted (not in M1). */
    DASH,
    PAUSE,
    MENU_CONFIRM,
    MENU_BACK
}
