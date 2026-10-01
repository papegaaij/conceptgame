package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;
import java.util.Set;

/**
 * What triggers one action: a primary and an alternative key (libGDX key codes, {@link #NO_KEY}
 * for none) and any of a set of gamepad controls.
 */
public record Binding(int primaryKey, int alternativeKey, Set<GamepadControl> gamepad) {
    public static final int NO_KEY = Keys.UNKNOWN;

    public Binding {
        gamepad = Set.copyOf(gamepad);
    }

    public static Binding of(int primaryKey, int alternativeKey, GamepadControl... gamepad) {
        return new Binding(primaryKey, alternativeKey, Set.of(gamepad));
    }
}
