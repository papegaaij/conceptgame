package vanguard.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/** The keyboard, the window focus and every connected gamepad, through libGDX. */
public final class GdxDevices implements DeviceState {
    private float deadZone = (float) ControlSettings.DEFAULT_DEAD_ZONE;

    /** The stick dead zone, 0..1, from the Controls tab. */
    public void deadZone(double share) {
        deadZone = (float) share;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        return Gdx.input.isKeyPressed(keyCode);
    }

    @Override
    public boolean gamepadPressed(GamepadControl control) {
        Array<Controller> pads = Controllers.getControllers();
        for (int i = 0; i < pads.size; i++) {
            if (control.pressedOn(pads.get(i), deadZone)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int gamepads() {
        return Controllers.getControllers().size;
    }

    @Override
    public boolean focused() {
        return ((Lwjgl3Graphics) Gdx.graphics).getWindow().isFocused();
    }
}
