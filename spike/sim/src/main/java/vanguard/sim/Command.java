package vanguard.sim;

/** The player commands sampled once per simulation step, packed into an {@code int} bit set. */
public enum Command {
    UP, DOWN, LEFT, RIGHT, FIRE;

    /** No command pressed. */
    public static final int NONE = 0;

    /** The bit of this command in a command set. */
    public int bit() {
        return 1 << ordinal();
    }

    /** Whether this command is part of the given command set. */
    public boolean in(int commands) {
        return (commands & bit()) != 0;
    }

    /** Builds a command set. */
    public static int of(Command... commands) {
        int bits = NONE;
        for (Command command : commands) {
            bits |= command.bit();
        }
        return bits;
    }
}
