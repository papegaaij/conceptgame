package vanguard.game.screen;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.SoundTest;
import vanguard.game.input.Action;
import vanguard.game.settings.Settings;

/** A row of an Options tab; left and right change it, confirm acts on a button or a binding. */
sealed interface OptionRow {
    String label();

    /** Left (-1) or right (+1); returns whether something changed. */
    default boolean change(OptionsTarget target, int direction) {
        return false;
    }

    /**
     * Chips of which one is on, such as OFF / ON.
     *
     * @param note shown under the chips, one per chip; empty for none
     */
    record Choice(
            String label,
            List<String> chips,
            ToIntFunction<OptionsTarget> get,
            ObjIntConsumer<OptionsTarget> set,
            List<String> note)
            implements OptionRow {
        @Override
        public boolean change(OptionsTarget target, int direction) {
            int now = get.applyAsInt(target);
            int next = Math.clamp(now + direction, 0, chips.size() - 1);
            if (next != now) {
                set.accept(target, next);
            }
            return next != now;
        }
    }

    /**
     * A value between {@code min} and {@code max} in steps.
     *
     * @param feedback the sound a change plays, so a volume can be heard at its new level
     */
    record Slider(
            String label,
            double min,
            double max,
            double step,
            ToDoubleFunction<Settings> get,
            BiFunction<Settings, Double, Settings> set,
            DoubleFunction<String> format,
            Sfx feedback)
            implements OptionRow {
        @Override
        public boolean change(OptionsTarget target, int direction) {
            double now = get.applyAsDouble(target.settings());
            double steps = Math.rint((now - min) / step) + direction;
            // Whole millionths, so the settings file shows 0.15 rather than 0.15000000000000002.
            double next = Math.rint(Math.clamp(min + steps * step, min, max) * 1e6) / 1e6;
            if (Math.abs(next - now) < step / 2) {
                return false;
            }
            target.change(set.apply(target.settings(), next));
            return true;
        }

        /** The value's place between min and max, 0..1. */
        double share(Settings settings) {
            return (get.applyAsDouble(settings) - min) / (max - min);
        }
    }

    /** A list of the sound test: left and right pick an entry, confirm plays it. */
    record Sound(String label, SoundTest.Kind kind) implements OptionRow {
        @Override
        public boolean change(OptionsTarget target, int direction) {
            return target.soundTest().pick(kind, direction);
        }
    }

    /** Does something on confirm. */
    record Button(String label, Consumer<OptionsTarget> action) implements OptionRow {}

    /** An action's bindings in three columns; confirm captures a new key or button for the column. */
    record Remap(Action action) implements OptionRow {
        @Override
        public String label() {
            return action.name().replace('_', ' ');
        }
    }
}
