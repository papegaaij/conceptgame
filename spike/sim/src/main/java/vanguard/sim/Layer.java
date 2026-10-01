package vanguard.sim;

/**
 * The parallax layers of the play field, back to front (see design/art-direction).
 * Scroll factors are relative to the ground layer; {@link #AIR} is the screen-space play plane.
 */
public enum Layer {
    DEEP(0.12),
    FAR(0.5),
    GROUND(1.0),
    SUB(0.85),
    LOW_AIR(1.35),
    AIR(0.0),
    HIGH_AIR(2.2);

    private final double scrollFactor;

    Layer(double scrollFactor) {
        this.scrollFactor = scrollFactor;
    }

    /** Scroll speed relative to the ground layer; 0 for the screen-space play plane. */
    public double scrollFactor() {
        return scrollFactor;
    }

    /** Whether the player's shots can hit enemies on this layer (submerged enemies are out of reach). */
    public boolean hittable() {
        return this != SUB;
    }
}
