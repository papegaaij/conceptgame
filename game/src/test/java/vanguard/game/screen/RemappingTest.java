package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.game.input.Action;
import vanguard.game.input.ActionInput;
import vanguard.game.input.BindingSlot;
import vanguard.game.input.Bindings;
import vanguard.game.input.FakeDevices;
import vanguard.game.input.GamepadControl;
import vanguard.game.input.MenuInput;

class RemappingTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);
    private final Remapping remapping = new Remapping();
    private final Bindings bindings = Bindings.defaults();

    /** One frame: sample the devices, then let the remapping look. */
    private Optional<Bindings> frame() {
        input.update(devices);
        menu.update(1 / 60f);
        return remapping.update(devices, menu, bindings);
    }

    private void press(int key) {
        devices.keys.add(key);
    }

    private void releaseAll() {
        devices.keys.clear();
        devices.gamepad.clear();
        frame();
    }

    @Test
    void aFreeKeyIsBoundAtOnce() {
        press(Keys.ENTER);
        input.update(devices);
        remapping.start(new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY), devices);
        assertEquals(Optional.of("PRESS A KEY...   ESC CANCELS"), remapping.message());
        assertEquals(Optional.empty(), frame(), "the held confirm is no capture");
        releaseAll();

        press(Keys.Q);
        Optional<Bindings> changed = frame();

        assertEquals(Keys.Q, changed.orElseThrow().get(Action.SPECIAL).primaryKey());
        assertFalse(remapping.active());
    }

    @Test
    void aConflictIsShownAndConfirmSwapsTheTwo() {
        remapping.start(new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY), devices);
        press(Keys.Z);
        assertEquals(Optional.empty(), frame());
        assertEquals(
                Optional.of("CONFLICT: 'Z' IS ALREADY FIRE (ALTERNATIVE) - ENTER SWAPS THE TWO"), remapping.message());
        releaseAll();

        press(Keys.ENTER);
        Bindings changed = frame().orElseThrow();

        assertEquals(Keys.Z, changed.get(Action.SPECIAL).primaryKey());
        assertEquals(Keys.X, changed.get(Action.FIRE).alternativeKey());
        assertFalse(remapping.active());
    }

    @Test
    void backAtAConflictKeepsTheOldBindings() {
        remapping.start(new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY), devices);
        press(Keys.Z);
        frame();
        releaseAll();

        press(Keys.ESCAPE);

        assertEquals(Optional.empty(), frame());
        assertFalse(remapping.active());
    }

    @Test
    void escapeCancelsTheCapture() {
        remapping.start(new Bindings.Assignment(Action.FIRE, BindingSlot.ALTERNATIVE), devices);
        press(Keys.ESCAPE);

        assertEquals(Optional.empty(), frame());
        assertFalse(remapping.active());
    }

    @Test
    void aGamepadButtonOfAnotherActionIsSwappedAfterConfirm() {
        remapping.start(new Bindings.Assignment(Action.PRECISION, BindingSlot.GAMEPAD), devices);
        devices.gamepad.add(GamepadControl.LEFT_BUMPER);
        frame();
        assertTrue(remapping.message().orElseThrow().contains("'L-BUMPER' IS ALREADY DASH (GAMEPAD)"));
        releaseAll();

        devices.gamepad.add(GamepadControl.A);
        Bindings changed = frame().orElseThrow();

        assertEquals(
                Set.of(GamepadControl.LEFT_BUMPER),
                changed.get(Action.PRECISION).gamepad());
        assertEquals(
                Set.of(GamepadControl.RIGHT_BUMPER), changed.get(Action.DASH).gamepad());
    }
}
