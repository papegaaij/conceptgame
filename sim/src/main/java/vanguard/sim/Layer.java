package vanguard.sim;

/**
 * The layers enemies live on, with the layer hit rules of design/enemies/README.md (Layer rules)
 * for weapons without the {@code anti-ground}, {@code homing} or {@code beam} traits, such as the
 * Pulse Cannon. M5 part E (user decision E2 = a): {@link #SUB}, under the water's surface, is hit
 * only by the torpedo ({@link WeaponSpec.Delivery#TORPEDO}, the {@code anti-sub} delivery) and the
 * Smart Bomb; nothing on it touches the ship. The {@code space} and {@code deep} layers follow with
 * the levels that use them.
 */
public enum Layer {
    GROUND(true, false),
    LOW_AIR(true, false),
    /** The player's plane. */
    AIR(true, true),
    HIGH_AIR(false, false),
    /**
     * M5 part E: under the water's surface (a submerged unit's current layer, a sunken trigger).
     * Appended last so the earlier layers keep their ordinals (and the replay hashes theirs).
     */
    SUB(false, false);

    private final boolean hitByStandardShots;
    private final boolean collidesWithPlayer;

    Layer(boolean hitByStandardShots, boolean collidesWithPlayer) {
        this.hitByStandardShots = hitByStandardShots;
        this.collidesWithPlayer = collidesWithPlayer;
    }

    /** Whether shots of a weapon without special traits hit enemies on this layer. */
    public boolean hitByStandardShots() {
        return hitByStandardShots;
    }

    /** Whether enemies on this layer deal contact damage to the player. */
    public boolean collidesWithPlayer() {
        return collidesWithPlayer;
    }
}
