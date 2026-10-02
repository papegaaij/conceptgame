package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;

class MenuInputTest {
    private static final float FRAME = 1 / 60f;

    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);

    private int framesDown(float seconds) {
        int fired = 0;
        for (float t = 0; t < seconds; t += FRAME) {
            input.update(devices);
            menu.update(FRAME);
            if (menu.down()) {
                fired++;
            }
        }
        return fired;
    }

    @Test
    void aHeldDirectionRepeatsAfterADelay() {
        devices.keys.add(Keys.DOWN);

        assertEquals(1, framesDown(MenuInput.REPEAT_DELAY - 2 * FRAME), "once when pressed");
        int repeats = framesDown(10 * MenuInput.REPEAT_INTERVAL);
        assertTrue(repeats >= 9 && repeats <= 11, "then every interval: " + repeats);
    }

    @Test
    void theDpadAndTheStickNavigateToo() {
        devices.gamepad.add(GamepadControl.DPAD_DOWN);
        assertEquals(1, framesDown(FRAME));

        devices.gamepad.clear();
        framesDown(FRAME);
        devices.gamepad.add(GamepadControl.LEFT_STICK_DOWN);

        assertEquals(1, framesDown(FRAME));
    }

    @Test
    void theTabKeysAndTheBumpersSwitchTabs() {
        devices.keys.add(Keys.E);
        input.update(devices);
        assertTrue(menu.nextTab());

        devices.keys.clear();
        devices.gamepad.add(GamepadControl.LEFT_BUMPER);
        input.update(devices);

        assertTrue(menu.previousTab());
    }
}
