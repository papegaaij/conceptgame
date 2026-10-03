package vanguard.sim;

/**
 * A friendly unit's spec at one difficulty (design/allies/data.yaml), built by
 * {@code vanguard.content.SimSpecs}: a ground-layer ally such as the civilian crawler. Player fire
 * never hurts it; what does is its {@code damagedBy} rules.
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
 */
public record AllySpec(
        String slug,
        Hitbox size,
        Hitbox hitbox,
        double hp,
        boolean objectiveAimed,
        double clawsPerSecond,
        double smokeBelow,
        double maxHeadingDegrees) {
    public AllySpec {
        if (!(hp > 0) || clawsPerSecond < 0) {
            throw new IllegalArgumentException(slug + ": an ally has HP and claws take no less than 0");
        }
    }
}
