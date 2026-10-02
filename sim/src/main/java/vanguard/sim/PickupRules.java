package vanguard.sim;

/**
 * How pickups behave, from design/player/data.yaml and the ship's collection radius
 * (design/player/ship/data.yaml), built by {@code vanguard.content.SimSpecs}.
 *
 * @param smallSalvageCredits credits of a small salvage pickup before the credit factor
 * @param mediumSalvageCredits credits of a medium salvage pickup before the credit factor
 * @param overdriveSeconds how long an overdrive lasts
 * @param shieldCellShare share of the shield's capacity a shield cell restores
 * @param armourPatch armour points an armour patch restores
 * @param seconds how long an uncollected pickup stays before it is gone
 * @param driftSpeed how fast pickups drift down the screen in px/s
 * @param collectionRadius distance from the ship's centre within which pickups are collected
 */
public record PickupRules(
        int smallSalvageCredits,
        int mediumSalvageCredits,
        double overdriveSeconds,
        double shieldCellShare,
        double armourPatch,
        double seconds,
        double driftSpeed,
        double collectionRadius) {}
