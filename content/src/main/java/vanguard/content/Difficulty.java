package vanguard.content;

import java.util.Locale;

/** The campaign's difficulty (design/systems/difficulty), chosen when a new game starts. */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    /** The difficulty named {@code easy}, {@code medium} or {@code hard}. */
    public static Difficulty of(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
