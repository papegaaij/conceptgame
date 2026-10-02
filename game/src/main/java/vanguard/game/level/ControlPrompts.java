package vanguard.game.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vanguard.sim.Command;

/**
 * The contextual control prompts of a level's first section (design/campaign, Level 01: move,
 * fire, precision): each is shown until the player has done what it says, which also skips it.
 */
public final class ControlPrompts {
    /** A prompt and the commands that complete it. */
    public enum Prompt {
        MOVE(Command.of(Command.UP, Command.DOWN, Command.LEFT, Command.RIGHT)),
        FIRE(Command.FIRE.bit()),
        PRECISION(Command.PRECISION.bit());

        private final int commands;

        Prompt(int commands) {
            this.commands = commands;
        }

        /** The prompt a level's data names ({@code move}, {@code fire}, {@code precision}). */
        public static Prompt of(String name) {
            return valueOf(name.toUpperCase(Locale.ROOT));
        }
    }

    private final List<Prompt> pending = new ArrayList<>();

    public ControlPrompts(List<String> names) {
        names.forEach(name -> pending.add(Prompt.of(name)));
    }

    /** Ticks off the prompts the step's commands complete. */
    public void update(int commands) {
        pending.removeIf(prompt -> (prompt.commands & commands) != 0);
    }

    /** The prompts still to show, in their order. */
    public List<Prompt> pending() {
        return pending;
    }
}
