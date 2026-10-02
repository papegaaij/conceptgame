package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;

class KeyCaptureTest {
    private final FakeDevices devices = new FakeDevices();

    @Test
    void aKeyHeldWhenTheCaptureStartedCountsOnlyOncePressedAgain() {
        devices.keys.add(Keys.SPACE);
        var capture = new KeyCapture(devices, false);
        assertEquals(new KeyCapture.Result.Waiting(), capture.poll(devices));

        devices.keys.clear();
        capture.poll(devices);
        devices.keys.add(Keys.SPACE);

        assertEquals(new KeyCapture.Result.Key(Keys.SPACE), capture.poll(devices), "pressed again it counts");
    }

    @Test
    void enterIsAFixedSystemKeyAndNeverCaptured() {
        var capture = new KeyCapture(devices, false);
        devices.keys.add(Keys.ENTER);
        devices.keys.add(Keys.NUMPAD_ENTER);

        assertEquals(new KeyCapture.Result.Waiting(), capture.poll(devices));
    }

    @Test
    void escapeCancels() {
        var capture = new KeyCapture(devices, false);
        devices.keys.add(Keys.ESCAPE);

        assertEquals(new KeyCapture.Result.Cancelled(), capture.poll(devices));
    }

    @Test
    void theFullScreenKeyIsNeverCaptured() {
        var capture = new KeyCapture(devices, false);
        devices.keys.add(Keys.F11);

        assertEquals(new KeyCapture.Result.Waiting(), capture.poll(devices));
    }

    @Test
    void aGamepadSlotCapturesAButtonButNoStick() {
        var capture = new KeyCapture(devices, true);
        devices.gamepad.add(GamepadControl.LEFT_STICK_UP);
        devices.keys.add(Keys.SPACE);
        assertEquals(new KeyCapture.Result.Waiting(), capture.poll(devices));

        devices.gamepad.add(GamepadControl.Y);

        assertEquals(new KeyCapture.Result.Button(GamepadControl.Y), capture.poll(devices));
    }
}
