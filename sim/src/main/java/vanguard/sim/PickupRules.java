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
 * @param largeSalvageCredits credits of a large salvage pickup before the credit factor
 * @param crateSeconds how long an uncollected secret's crate or data core stays: {@code seconds}
 *     unless the level sets its own (Level 10's ferry cache, which drifts down from the top edge)
 */
public record PickupRules(
        int smallSalvageCredits,
        int mediumSalvageCredits,
        double overdriveSeconds,
        double shieldCellShare,
        double armourPatch,
        double seconds,
        double driftSpeed,
        double collectionRadius,
        int largeSalvageCredits,
        double crateSeconds) {
    /** Rules whose secrets' crates stay as long as other pickups. */
    public PickupRules(
            int smallSalvageCredits,
            int mediumSalvageCredits,
            double overdriveSeconds,
            double shieldCellShare,
            double armourPatch,
            double seconds,
            double driftSpeed,
            double collectionRadius,
            int largeSalvageCredits) {
        this(
                smallSalvageCredits,
                mediumSalvageCredits,
                overdriveSeconds,
                shieldCellShare,
                armourPatch,
                seconds,
                driftSpeed,
                collectionRadius,
                largeSalvageCredits,
                seconds);
    }

    /** Rules without large salvage. */
    public PickupRules(
            int smallSalvageCredits,
            int mediumSalvageCredits,
            double overdriveSeconds,
            double shieldCellShare,
            double armourPatch,
            double seconds,
            double driftSpeed,
            double collectionRadius) {
        this(
                smallSalvageCredits,
                mediumSalvageCredits,
                overdriveSeconds,
                shieldCellShare,
                armourPatch,
                seconds,
                driftSpeed,
                collectionRadius,
                0);
    }
}
