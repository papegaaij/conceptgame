package vanguard.sim;

/**
 * A friendly unit's spec at one difficulty (design/allies/data.yaml), built by
 * {@code vanguard.content.SimSpecs}: a ground-layer ally such as the civilian crawler, or (M5 part D)
 * an air ally such as the evacuation shuttle. Player fire never hurts it; what does is its
 * {@code damagedBy} rules.
 *
 * @param slug its name in the design data, e.g. {@code civilian-crawler}
 * @param size the sprite's footprint, px (width across, length along the road)
 * @param hitbox axis-aligned at every heading
 * @param hp at the level's difficulty; no shield and no regeneration
 * @param objectiveAimed whether enemy shots aimed at an objective unit by the target-the-objective
 *     hook hurt it (shots aimed at the player never do)
 * @param clawsPerSecond damage per second while a walker's hitbox overlaps it; 0 for none
 * @param smokeBelow the share of its HP below which it smokes (presentation)
 * @param maxHeadingDegrees its rendered headings reach this far either side of straight up
 * @param bullets M5 part D (user decision D2 = a): every enemy bullet touching its hit box hurts it
 *     by its damage and is spent (an air ally on the player's plane)
 * @param contact M5 part D: an enemy body on the player's plane hurts it by its contact damage once
 *     per contact; one that is destroyed by ramming the ship is destroyed by the impact and paid
 * @param bankFrames M5 part D, presentation: its rendered banking frames (0 for none)
 * @param bankFull M5 part D, presentation: the sideways px/s of its full bank
 * @param glideSeconds M5 part D, presentation: how long a lost unit glides down into {@code far}
 */
public record AllySpec(
        String slug,
        Hitbox size,
        Hitbox hitbox,
        double hp,
        boolean objectiveAimed,
        double clawsPerSecond,
        double smokeBelow,
        double maxHeadingDegrees,
        boolean bullets,
        boolean contact,
        int bankFrames,
        double bankFull,
        double glideSeconds) {
    public AllySpec {
        if (!(hp > 0) || clawsPerSecond < 0) {
            throw new IllegalArgumentException(slug + ": an ally has HP and claws take no less than 0");
        }
        if (bankFrames < 0 || bankFull < 0 || glideSeconds < 0) {
            throw new IllegalArgumentException(slug + ": banks and the glide take no less than 0");
        }
    }

    /** A ground ally (Level 04's crawler): no bullets, no contact, no banks and no glide. */
    public AllySpec(
            String slug,
            Hitbox size,
            Hitbox hitbox,
            double hp,
            boolean objectiveAimed,
            double clawsPerSecond,
            double smokeBelow,
            double maxHeadingDegrees) {
        this(
                slug,
                size,
                hitbox,
                hp,
                objectiveAimed,
                clawsPerSecond,
                smokeBelow,
                maxHeadingDegrees,
                false,
                false,
                0,
                0,
                0);
    }
}
