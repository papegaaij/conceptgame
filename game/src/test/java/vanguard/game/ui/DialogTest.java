package vanguard.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import vanguard.game.input.ActionInput;
import vanguard.game.input.Bindings;
import vanguard.game.input.FakeDevices;
import vanguard.game.input.MenuInput;

class DialogTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);
    private final Dialog dialog = new Dialog("QUIT TO MAIN MENU?", "", "YES, QUIT", "NO, BACK");

    private Dialog.Answer press(int key) {
        devices.keys.clear();
        input.update(devices);
        menu.update(1 / 60f);
        devices.keys.add(key);
        input.update(devices);
        menu.update(1 / 60f);
        return dialog.update(menu);
    }

    @Test
    void confirmAtOnceAnswersTheSafeNo() {
        assertEquals(Dialog.Answer.NO, press(Keys.ENTER));
    }

    @Test
    void yesNeedsTheCursorMovedFirst() {
        assertEquals(Dialog.Answer.NONE, press(Keys.LEFT));

        assertEquals(Dialog.Answer.YES, press(Keys.ENTER));
    }

    @Test
    void backAnswersNo() {
        press(Keys.LEFT);

        assertEquals(Dialog.Answer.NO, press(Keys.ESCAPE));
    }
}
