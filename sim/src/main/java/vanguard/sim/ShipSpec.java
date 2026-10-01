package vanguard.sim;

/**
 * The AF-12 Stormhawk's flight numbers, from design/player/ship/README.md (Movement, Hitbox,
 * mount points) and the banking rule of design/art-direction/README.md (Animation rules). They
 * move into the ship's data file in M2.
 *
 * @param speed full speed in px/s (engine Mk I)
 * @param accelerationSeconds time from standing still to full speed
 * @param stopSeconds time from full speed to standing still
 * @param precisionFactor speed factor while precision mode is held
 * @param size the edge length of the square hull sprite in px
 * @param edgeMargin the gap the hull keeps to the play field edges
 * @param hitbox the hit box around the cockpit, much smaller than the 48x48 sprite
 * @param mercySeconds invulnerability after armour damage
 * @param muzzleOffsetY the front muzzle above the ship's centre: (24, 3) on the 48x48 sprite
 * @param bankStepTicks steps per banking frame change; level to hard over takes two changes
 */
public record ShipSpec(
        double speed,
        double accelerationSeconds,
        double stopSeconds,
        double precisionFactor,
        double size,
        double edgeMargin,
        Hitbox hitbox,
        double mercySeconds,
        double muzzleOffsetY,
        int bankStepTicks) {
    /** The banking frames run from -2 (hard left) to 2 (hard right). */
    public static final int HARD_BANK = 2;

    /** How close the ship's centre may come to a play field edge: half the hull plus the margin. */
    public double edgeLimit() {
        return size / 2 + edgeMargin;
    }

    public static final ShipSpec STORMHAWK = new ShipSpec(270, 0.08, 0.06, 0.5, 48, 12, new Hitbox(9, 9), 0.25, 21, 3);
}
