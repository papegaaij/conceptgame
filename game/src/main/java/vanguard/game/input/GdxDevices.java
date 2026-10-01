package vanguard.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/** The keyboard and every connected gamepad, through libGDX. */
public final class GdxDevices implements DeviceState {
    /** The default stick dead zone of design/ui/controls; configurable in Options (M3). */
    private static final float DEAD_ZONE = 0.2f;

    @Override
    public boolean keyPressed(int keyCode) {
        return Gdx.input.isKeyPressed(keyCode);
    }

    @Override
    public boolean gamepadPressed(GamepadControl control) {
        Array<Controller> pads = Controllers.getControllers();
        for (int i = 0; i < pads.size; i++) {
            if (control.pressedOn(pads.get(i), DEAD_ZONE)) {
                return true;
            }
        }
        return false;
    }
}
