package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import vanguard.game.input.ActionInput;
import vanguard.game.input.Bindings;
import vanguard.game.input.FakeDevices;
import vanguard.game.input.GamepadControl;
import vanguard.game.input.MenuInput;
import vanguard.game.ui.Dialog;

/**
 * On the mission failed screen, Back (Esc, the gamepad's back button) opens the quit question as
 * Quit to main menu does (user decision 2026-10-04); Back again or No stays.
 */
class MissionFailedExitTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);

    private MenuInput press(Runnable down) {
        devices.keys.clear();
        devices.gamepad.clear();
        input.update(devices);
        menu.update(1 / 60f);
        down.run();
        input.update(devices);
        menu.update(1 / 60f);
        return menu;
    }

    private MenuInput key(int key) {
        return press(() -> devices.keys.add(key));
    }

    @Test
    void escapeOrTheGamepadsBackButtonAsksOnAnyItem() {
        assertTrue(MissionFailedScreen.asksToQuit(key(Keys.ESCAPE), false));
        assertTrue(MissionFailedScreen.asksToQuit(press(() -> devices.gamepad.add(GamepadControl.BACK)), false));
        assertTrue(MissionFailedScreen.asksToQuit(key(Keys.ENTER), true), "confirm on Quit to main menu");
        assertFalse(MissionFailedScreen.asksToQuit(key(Keys.ENTER), false), "confirm on Retry retries");
        assertFalse(MissionFailedScreen.asksToQuit(key(Keys.DOWN), true));
    }

    @Test
    void escapeAgainOrNoStaysAndYesQuits() {
        assertEquals(Dialog.Answer.NO, MissionFailedScreen.quitDialog().update(key(Keys.ESCAPE)));
        assertEquals(
                Dialog.Answer.NO, MissionFailedScreen.quitDialog().update(key(Keys.ENTER)), "the cursor starts on no");

        Dialog dialog = MissionFailedScreen.quitDialog();
        assertEquals(Dialog.Answer.NONE, dialog.update(key(Keys.LEFT)), "the cursor moves to yes");
        assertEquals(Dialog.Answer.YES, dialog.update(key(Keys.ENTER)));
    }
}
