package vanguard.game.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import java.util.List;
import java.util.Locale;

/** A gamepad button or stick direction, named by its position on a standard (Xbox-layout) pad. */
public enum GamepadControl {
    A,
    B,
    X,
    Y,
    START,
    BACK,
    LEFT_BUMPER,
    RIGHT_BUMPER,
    LEFT_TRIGGER,
    RIGHT_TRIGGER,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    LEFT_STICK_UP,
    LEFT_STICK_DOWN,
    LEFT_STICK_LEFT,
    LEFT_STICK_RIGHT;

    /** The buttons a remapping can capture; Back cancels a capture, sticks and D-pad stay with moving. */
    public static final List<GamepadControl> BUTTONS =
            List.of(A, B, X, Y, START, LEFT_BUMPER, RIGHT_BUMPER, LEFT_TRIGGER, RIGHT_TRIGGER);

    /** The name the Controls tab shows: {@code R-TRIGGER}, {@code D-PAD UP}. */
    public String label() {
        return switch (this) {
            case LEFT_BUMPER -> "L-BUMPER";
            case RIGHT_BUMPER -> "R-BUMPER";
            case LEFT_TRIGGER -> "L-TRIGGER";
            case RIGHT_TRIGGER -> "R-TRIGGER";
            case DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT -> "D-PAD " + direction();
            case LEFT_STICK_UP, LEFT_STICK_DOWN, LEFT_STICK_LEFT, LEFT_STICK_RIGHT -> "L-STICK " + direction();
            default -> name();
        };
    }

    private String direction() {
        return name().substring(name().lastIndexOf('_') + 1).toUpperCase(Locale.ROOT);
    }

    /** Whether this control is pressed on the pad; a stick counts beyond the dead zone (0..1). */
    boolean pressedOn(Controller pad, float deadZone) {
        ControllerMapping m = pad.getMapping();
        return switch (this) {
            case A -> pad.getButton(m.buttonA);
            case B -> pad.getButton(m.buttonB);
            case X -> pad.getButton(m.buttonX);
            case Y -> pad.getButton(m.buttonY);
            case START -> pad.getButton(m.buttonStart);
            case BACK -> pad.getButton(m.buttonBack);
            case LEFT_BUMPER -> pad.getButton(m.buttonL1);
            case RIGHT_BUMPER -> pad.getButton(m.buttonR1);
            case LEFT_TRIGGER -> pad.getButton(m.buttonL2);
            case RIGHT_TRIGGER -> pad.getButton(m.buttonR2);
            case DPAD_UP -> pad.getButton(m.buttonDpadUp);
            case DPAD_DOWN -> pad.getButton(m.buttonDpadDown);
            case DPAD_LEFT -> pad.getButton(m.buttonDpadLeft);
            case DPAD_RIGHT -> pad.getButton(m.buttonDpadRight);
            // Stick y points down on every mapping gdx-controllers supports.
            case LEFT_STICK_UP -> pad.getAxis(m.axisLeftY) < -deadZone;
            case LEFT_STICK_DOWN -> pad.getAxis(m.axisLeftY) > deadZone;
            case LEFT_STICK_LEFT -> pad.getAxis(m.axisLeftX) < -deadZone;
            case LEFT_STICK_RIGHT -> pad.getAxis(m.axisLeftX) > deadZone;
        };
    }
}
