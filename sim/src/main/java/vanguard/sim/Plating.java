package vanguard.sim;

/**
 * Armour plating, from design/player/armor/README.md.
 *
 * @param maxArmour armour points of an undamaged hull
 */
public record Plating(double maxArmour) {
    /** The starter plating. */
    public static final Plating STANDARD = new Plating(60);
}
