package vanguard.content.campaign;

/**
 * What kind of item a shop entry, an owned item and a loadout slot are (design/player, Loadout):
 * the three weapon mounts, the four core parts, the utility modules, the specials and Rook's guns in
 * the escort slot (M5 part A).
 */
public enum ItemKind {
    FRONT,
    REAR,
    WING,
    GENERATOR,
    SHIELD,
    PLATING,
    ENGINE,
    UTILITY,
    SPECIAL,
    /** Rook's guns (design/player/wingmen): his own inventory, one fitted; they draw no power. */
    ESCORT;

    /**
     * Whether a slot of this kind may be empty: the front gun, the core parts and Rook's gun (once he
     * is hired) are always fitted.
     */
    public boolean optional() {
        return switch (this) {
            case REAR, WING, UTILITY, SPECIAL -> true;
            case FRONT, GENERATOR, SHIELD, PLATING, ENGINE, ESCORT -> false;
        };
    }

    /**
     * Whether items of this kind are the Stormhawk's weapons, with traits and five upgrade levels
     * (Rook's guns have both too, but they are not the ship's: their traits are his).
     */
    public boolean weapon() {
        return this == FRONT || this == REAR || this == WING;
    }
}
