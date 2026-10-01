package vanguard.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;

/**
 * The menus' Back action (design/ui/controls): Esc on the keyboard, B or Back on any gamepad. One
 * instance listens for the whole run; libGDX drops the listener with its controller manager at exit.
 */
public final class BackButton extends ControllerAdapter {
    private boolean gamepadPressed;

    public BackButton() {
        Controllers.addListener(this);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        ControllerMapping mapping = controller.getMapping();
        if (buttonCode == mapping.buttonB || buttonCode == mapping.buttonBack) {
            gamepadPressed = true;
        }
        return false;
    }

    /** Whether Back was pressed since the previous call. */
    public boolean pressed() {
        boolean pressed = gamepadPressed || Gdx.input.isKeyJustPressed(Keys.ESCAPE);
        gamepadPressed = false;
        return pressed;
    }
}
