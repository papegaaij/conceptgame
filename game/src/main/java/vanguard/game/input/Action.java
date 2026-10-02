package vanguard.game.input;

import java.util.List;

/**
 * The player's actions (design/ui/controls). The in-level actions are the remappable ones; the
 * menu actions keep their fixed keys, so the menus stay usable whatever the flight keys are.
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
    MENU_UP,
    MENU_DOWN,
    MENU_LEFT,
    MENU_RIGHT,
    MENU_CONFIRM,
    MENU_BACK,
    /** Switches to the previous tab (Q / left bumper). */
    MENU_PREVIOUS_TAB,
    /** Switches to the next tab (E / right bumper). */
    MENU_NEXT_TAB;

    /** The actions the Controls tab remaps, in its table's order. */
    public static final List<Action> REMAPPABLE =
            List.of(MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, FIRE, SPECIAL, PRECISION, DASH, PAUSE);

    /** Whether the gamepad column of this action can be remapped: the move actions keep the stick and the D-pad. */
    public boolean gamepadRemappable() {
        return switch (this) {
            case FIRE, SPECIAL, PRECISION, DASH, PAUSE -> true;
            default -> false;
        };
    }

    /**
     * Whether a slot of this remappable action can be changed: Pause keeps Esc, a fixed system key
     * (design/ui/controls), as its primary key; every action keeps a remappable alternative key.
     */
    public boolean remappable(BindingSlot slot) {
        return switch (slot) {
            case PRIMARY -> this != PAUSE;
            case ALTERNATIVE -> true;
            case GAMEPAD -> gamepadRemappable();
        };
    }
}
