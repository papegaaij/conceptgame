package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import vanguard.game.input.ActionInput;
import vanguard.game.input.Bindings;
import vanguard.game.input.FakeDevices;
import vanguard.game.input.GamepadControl;
import vanguard.game.input.MenuInput;

/** On the debrief, Back (Esc, the gamepad's back button) goes on like confirm (user decision 2026-10-04). */
class DebriefExitTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);

    private boolean press(Runnable down) {
        devices.keys.clear();
        devices.gamepad.clear();
        input.update(devices);
        menu.update(1 / 60f);
        down.run();
        input.update(devices);
        menu.update(1 / 60f);
        return DebriefScreen.goesOn(menu);
    }

    @Test
    void escapeAndTheGamepadsBackButtonGoOnLikeConfirm() {
        assertTrue(press(() -> devices.keys.add(Keys.ENTER)));
        assertTrue(press(() -> devices.keys.add(Keys.ESCAPE)));
        assertTrue(press(() -> devices.gamepad.add(GamepadControl.BACK)));
        assertTrue(press(() -> devices.gamepad.add(GamepadControl.A)));
    }

    @Test
    void otherKeysDoNot() {
        assertFalse(press(() -> devices.keys.add(Keys.LEFT)));
        assertFalse(press(() -> {}));
    }
}
