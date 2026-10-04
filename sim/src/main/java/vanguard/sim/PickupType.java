package vanguard.sim;

/** The in-level pickups of design/player (In-level pickups) that the simulation handles so far. */
public enum PickupType {
    /** +credits (design/player/data.yaml, salvage small). */
    SMALL_SALVAGE,
    /** +credits (salvage medium). */
    MEDIUM_SALVAGE,
    /** All weapons +1 level for a while. */
    OVERDRIVE,
    /** Restores a share of the shield's capacity. */
    SHIELD_CELL,
    /** Restores armour points. */
    ARMOUR_PATCH,
    /** A secret's hidden crate, worth that secret's credits. */
    HIDDEN_CRATE,
    /** +credits (salvage large): a set piece's death drop. */
    LARGE_SALVAGE,
    /** +1 charge of the fitted special, up to its most; only dropped when a special is fitted. */
    SPECIAL_CHARGE,
    /** A data core: a lore entry that unlocks one shop item early (design/systems/economy); no credits. */
    DATA_CORE
}
