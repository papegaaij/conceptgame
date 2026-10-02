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

    /** The key in a key slot. */
    public int key(BindingSlot slot) {
        return switch (slot) {
            case PRIMARY -> primaryKey;
            case ALTERNATIVE -> alternativeKey;
            case GAMEPAD -> throw new IllegalArgumentException("the gamepad slot holds no key");
        };
    }

    /** A copy with {@code key} in a key slot. */
    public Binding withKey(BindingSlot slot, int key) {
        return switch (slot) {
            case PRIMARY -> new Binding(key, alternativeKey, gamepad);
            case ALTERNATIVE -> new Binding(primaryKey, key, gamepad);
            case GAMEPAD -> throw new IllegalArgumentException("the gamepad slot holds no key");
        };
    }

    public Binding withGamepad(Set<GamepadControl> controls) {
        return new Binding(primaryKey, alternativeKey, controls);
    }
}
