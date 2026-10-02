package vanguard.content.campaign;

/**
 * The Stormhawk's loadout slots (design/player, the ship's slot layout). The third utility bay is
 * bought from Act 3; the escort slot follows with Act 2.
 */
public enum LoadoutSlot {
    FRONT(ItemKind.FRONT),
    REAR(ItemKind.REAR),
    LEFT_WING(ItemKind.WING),
    RIGHT_WING(ItemKind.WING),
    GENERATOR(ItemKind.GENERATOR),
    SHIELD(ItemKind.SHIELD),
    ARMOUR(ItemKind.PLATING),
    ENGINE(ItemKind.ENGINE),
    SPECIAL(ItemKind.SPECIAL),
    UTILITY_1(ItemKind.UTILITY),
    UTILITY_2(ItemKind.UTILITY),
    UTILITY_3(ItemKind.UTILITY);

    private final ItemKind kind;

    LoadoutSlot(ItemKind kind) {
        this.kind = kind;
    }

    /** What fits in the slot. */
    public ItemKind kind() {
        return kind;
    }
}
