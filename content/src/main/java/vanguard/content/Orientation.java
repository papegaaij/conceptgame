package vanguard.content;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * How an enemy's sprite follows its flight (design/enemies/README.md, Orientation and rotation),
 * written {@code fixed}, {@code 16 angles}, {@code 32 angles} or {@code radial}.
 */
public enum Orientation {
    /** Always faces down the screen. */
    FIXED("fixed", 1),
    /** Turns to face its movement: pre-rendered at 16 headings. */
    ANGLES_16("16 angles", 16),
    /** Turns to face its movement, large and slow units and turrets: 32 headings. */
    ANGLES_32("32 angles", 32),
    /** A radially symmetric spinner that needs no facing. */
    RADIAL("radial", 1);

    private final String text;
    private final int headings;

    Orientation(String text, int headings) {
        this.text = text;
        this.headings = headings;
    }

    /** The headings its sprite is pre-rendered at: 1 when it never turns to face its flight. */
    public int headings() {
        return headings;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Orientation of(String text) {
        for (Orientation orientation : values()) {
            if (orientation.text.equals(text)) {
                return orientation;
            }
        }
        throw new IllegalArgumentException(
                "orientation must be fixed, 16 angles, 32 angles or radial, was '" + text + "'");
    }
}
