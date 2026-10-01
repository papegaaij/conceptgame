package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.badlogic.gdx.Input.Keys;
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
}
