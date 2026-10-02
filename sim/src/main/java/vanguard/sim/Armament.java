package vanguard.sim;

import java.util.List;

/**
 * The fitted weapons: one {@link Mount} per weapon slot that holds one (design/player, the slot
 * layout), each with its pattern at its upgrade level and its overdrive pattern, one level higher
 * (design/player, in-level pickups). Each mount fires on its own clock.
 */
public record Armament(List<Mount> mounts) {
    public Armament {
        mounts = List.copyOf(mounts);
    }

    /** The weapon slots of the Stormhawk. */
    public enum Slot {
        FRONT,
        REAR,
        LEFT_WING,
        RIGHT_WING
    }

    /**
     * A fitted weapon.
     *
     * @param weapon at its upgrade level
     * @param overdrive what it fires during an overdrive: the next level's pattern, or the overdrive
     *     pattern at L5
     */
    public record Mount(Slot slot, WeaponSpec weapon, WeaponSpec overdrive) {}

    public int size() {
        return mounts.size();
    }

    public Mount mount(int index) {
        return mounts.get(index);
    }
}
