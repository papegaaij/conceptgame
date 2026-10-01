package vanguard.game.input;

/** What the input devices report right now; the seam that lets the input logic run without libGDX. */
public interface DeviceState {
    boolean keyPressed(int keyCode);

    /** Whether the control is pressed on any connected gamepad. */
    boolean gamepadPressed(GamepadControl control);
}
