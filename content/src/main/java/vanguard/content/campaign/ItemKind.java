package vanguard.content.campaign;

/**
 * What kind of item a shop entry, an owned item and a loadout slot are (design/player, Loadout):
 * the three weapon mounts, the four core parts, the utility modules and the specials.
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
    SPECIAL;

    /** Whether a slot of this kind may be empty: the front gun and the core parts are always fitted. */
    public boolean optional() {
        return switch (this) {
            case REAR, WING, UTILITY, SPECIAL -> true;
            case FRONT, GENERATOR, SHIELD, PLATING, ENGINE -> false;
        };
    }

    /** Whether items of this kind are weapons, with traits and five upgrade levels. */
    public boolean weapon() {
        return this == FRONT || this == REAR || this == WING;
    }
}
