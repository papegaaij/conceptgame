package vanguard.sim;

/** The in-level pickups of design/player (In-level pickups) that the simulation handles so far. */
public enum PickupType {
    /** +credits (design/player/data.yaml, salvage small). */
    SMALL_SALVAGE,
    /** Restores a share of the shield's capacity. */
    SHIELD_CELL,
    /** Restores armour points. */
    ARMOUR_PATCH,
    /** A secret's hidden crate, worth that secret's credits. */
    HIDDEN_CRATE
}
