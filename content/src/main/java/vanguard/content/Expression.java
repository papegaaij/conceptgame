package vanguard.content;

import java.util.Locale;

/**
 * A speaker's portrait expression (design/ui/briefing, design/story/characters): the main cast has
 * all three, the minor speakers only the neutral one. A briefing page or radio line names one where
 * its text calls for it; otherwise the portrait is neutral.
 */
public enum Expression {
    NEUTRAL,
    GRIM,
    FIERCE;

    /** The name in the data and in the portraits' file names, {@code grim}. */
    public String slug() {
        return name().toLowerCase(Locale.ROOT);
    }
}
