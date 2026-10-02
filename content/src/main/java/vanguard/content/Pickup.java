package vanguard.content;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A pickup as a level names it, e.g. {@code small salvage} (design/player/README.md, In-level pickups). */
public enum Pickup {
    @JsonProperty("small salvage")
    SMALL_SALVAGE,
    @JsonProperty("medium salvage")
    MEDIUM_SALVAGE,
    @JsonProperty("large salvage")
    LARGE_SALVAGE,
    @JsonProperty("overdrive")
    OVERDRIVE,
    @JsonProperty("shield cell")
    SHIELD_CELL,
    @JsonProperty("armour patch")
    ARMOUR_PATCH,
    @JsonProperty("special charge")
    SPECIAL_CHARGE,
    @JsonProperty("data core")
    DATA_CORE
}
