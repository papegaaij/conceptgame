package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.Input.Keys;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BindingsTest {
    private final Bindings defaults = Bindings.defaults();

    @Test
    void bindsEveryAction() {
        for (Action action : Action.values()) {
            assertNotNull(defaults.get(action), action.name());
        }
    }

    @Test
    void followsTheControlsTable() {
        assertEquals(
                Binding.of(Keys.LEFT, Keys.A, GamepadControl.LEFT_STICK_LEFT, GamepadControl.DPAD_LEFT),
                defaults.get(Action.MOVE_LEFT));
        assertEquals(Binding.of(Keys.X, Keys.CONTROL_LEFT, GamepadControl.B), defaults.get(Action.SPECIAL));
        assertEquals(Binding.of(Keys.SHIFT_LEFT, Keys.C, GamepadControl.RIGHT_BUMPER), defaults.get(Action.PRECISION));
        assertEquals(Binding.of(Binding.NO_KEY, Keys.V, GamepadControl.LEFT_BUMPER), defaults.get(Action.DASH));
        assertEquals(Binding.of(Keys.ESCAPE, Keys.P, GamepadControl.START), defaults.get(Action.PAUSE));
    }

    @Test
    void withChangesOneBindingInACopy() {
        Bindings changed = defaults.with(Action.SPECIAL, new Binding(Keys.Q, Keys.E, Set.of()));

        assertEquals(Keys.Q, changed.get(Action.SPECIAL).primaryKey());
        assertEquals(Keys.X, defaults.get(Action.SPECIAL).primaryKey());
        assertEquals(defaults.get(Action.FIRE), changed.get(Action.FIRE));
    }

    @Test
    void aFreeKeyIsSimplyBound() {
        var target = new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY);

        Bindings changed = defaults.withKey(target, Keys.Q);

        assertEquals(Keys.Q, changed.get(Action.SPECIAL).primaryKey());
        assertEquals(Optional.empty(), defaults.keyHolder(target, Keys.Q));
    }

    @Test
    void aKeyOfAnotherActionIsSwapped() {
        var target = new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY);
        assertEquals(
                Optional.of(new Bindings.Assignment(Action.FIRE, BindingSlot.ALTERNATIVE)),
                defaults.keyHolder(target, Keys.Z));

        Bindings changed = defaults.withKey(target, Keys.Z);

        assertEquals(Keys.Z, changed.get(Action.SPECIAL).primaryKey());
        assertEquals(Keys.X, changed.get(Action.FIRE).alternativeKey(), "Fire gets Special's old key");
        assertEquals(Keys.SPACE, changed.get(Action.FIRE).primaryKey());
    }

    @Test
    void theTwoKeysOfOneActionSwapToo() {
        Bindings changed = defaults.withKey(new Bindings.Assignment(Action.FIRE, BindingSlot.PRIMARY), Keys.Z);

        assertEquals(Keys.Z, changed.get(Action.FIRE).primaryKey());
        assertEquals(Keys.SPACE, changed.get(Action.FIRE).alternativeKey());
    }

    @Test
    void theMenuKeysAreNoConflict() {
        var target = new Bindings.Assignment(Action.FIRE, BindingSlot.PRIMARY);

        assertEquals(Optional.empty(), defaults.keyHolder(target, Keys.ENTER));
    }

    @Test
    void aGamepadButtonOfAnotherActionIsSwapped() {
        assertEquals(Optional.of(Action.SPECIAL), defaults.buttonHolder(Action.FIRE, GamepadControl.B));

        Bindings changed = defaults.withButton(Action.FIRE, GamepadControl.B);

        assertEquals(Set.of(GamepadControl.B), changed.get(Action.FIRE).gamepad());
        assertEquals(
                Set.of(GamepadControl.A, GamepadControl.RIGHT_TRIGGER),
                changed.get(Action.SPECIAL).gamepad(),
                "Special gets Fire's old buttons");
    }

    @Test
    void theMoveActionsKeepTheStickAndTheDpad() {
        assertThrows(IllegalArgumentException.class, () -> defaults.withButton(Action.MOVE_UP, GamepadControl.A));
    }
}
