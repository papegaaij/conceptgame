package vanguard.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Command;

/**
 * Samples keyboard (arrows / WASD, space or Z to fire) and every connected gamepad (left stick
 * or d-pad, A to fire) into a {@link Command} set. Logs gamepad connects and disconnects.
 */
public final class PlayerInput {
    private static final float DEAD_ZONE = 0.3f;
    private static final String TAG = "input";

    public PlayerInput() {
        Array<Controller> connected = Controllers.getControllers();
        for (int i = 0; i < connected.size; i++) {
            Gdx.app.log(TAG, "gamepad present: " + connected.get(i).getName());
        }
        Controllers.addListener(new ControllerAdapter() {
            @Override
            public void connected(Controller controller) {
                Gdx.app.log(TAG, "gamepad connected: " + controller.getName());
            }

            @Override
            public void disconnected(Controller controller) {
                Gdx.app.log(TAG, "gamepad disconnected: " + controller.getName());
            }
        });
    }

    /** The commands currently held on any device. */
    public int commands() {
        int commands = keyboard();
        Array<Controller> controllers = Controllers.getControllers();
        for (int i = 0; i < controllers.size; i++) {
            commands |= gamepad(controllers.get(i));
        }
        return commands;
    }

    private static int keyboard() {
        int commands = Command.NONE;
        if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
            commands |= Command.UP.bit();
        }
        if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
            commands |= Command.DOWN.bit();
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) {
            commands |= Command.LEFT.bit();
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) {
            commands |= Command.RIGHT.bit();
        }
        if (Gdx.input.isKeyPressed(Keys.SPACE) || Gdx.input.isKeyPressed(Keys.Z)) {
            commands |= Command.FIRE.bit();
        }
        return commands;
    }

    private static int gamepad(Controller pad) {
        ControllerMapping mapping = pad.getMapping();
        float x = pad.getAxis(mapping.axisLeftX);
        float y = pad.getAxis(mapping.axisLeftY);
        int commands = Command.NONE;
        if (y < -DEAD_ZONE || pad.getButton(mapping.buttonDpadUp)) {
            commands |= Command.UP.bit();
        }
        if (y > DEAD_ZONE || pad.getButton(mapping.buttonDpadDown)) {
            commands |= Command.DOWN.bit();
        }
        if (x < -DEAD_ZONE || pad.getButton(mapping.buttonDpadLeft)) {
            commands |= Command.LEFT.bit();
        }
        if (x > DEAD_ZONE || pad.getButton(mapping.buttonDpadRight)) {
            commands |= Command.RIGHT.bit();
        }
        if (pad.getButton(mapping.buttonA)) {
            commands |= Command.FIRE.bit();
        }
        return commands;
    }
}
