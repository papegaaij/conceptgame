package vanguard.sim;

import java.util.Optional;

/**
 * What the ship flies with: its hull, weapons, shield, plating, special and utility modules, and the
 * wingman in its escort slot.
 *
 * @param ship the hull's flight numbers, including the fitted engine's speed
 * @param armament the fitted weapons at their upgrade levels
 * @param shield the shield, its regeneration including the spare-power bonus (design/player/generator)
 * @param special the fitted special with the charges carried into the level; empty for none
 * @param magnet the fitted Pickup magnet at its level; empty for none
 * @param wingman the wingman flying in the escort slot (Rook, from Level 08); empty for none
 * @param salvageBonus the fitted Salvage scanner's share added to salvage pickups' and hidden crates'
 *     credits (design/player/systems); 0 for none
 */
public record Loadout(
        ShipSpec ship,
        Armament armament,
        ShieldModel shield,
        Plating plating,
        Optional<SpecialSpec> special,
        Optional<Magnet> magnet,
        Optional<WingmanSpec> wingman,
        double salvageBonus) {
    public Loadout {
        if (!(salvageBonus >= 0)) {
            throw new IllegalArgumentException("the salvage bonus is a share of at least 0");
        }
    }

    /** A loadout without a special, a magnet, a wingman or a salvage bonus. */
    public Loadout(ShipSpec ship, Armament armament, ShieldModel shield, Plating plating) {
        this(ship, armament, shield, plating, Optional.empty(), Optional.empty(), Optional.empty(), 0);
    }

    /** This loadout with {@code fitted} in the special slot. */
    public Loadout withSpecial(SpecialSpec fitted) {
        return new Loadout(ship, armament, shield, plating, Optional.of(fitted), magnet, wingman, salvageBonus);
    }

    /** This loadout with {@code fitted} in a utility bay. */
    public Loadout withMagnet(Magnet fitted) {
        return new Loadout(ship, armament, shield, plating, special, Optional.of(fitted), wingman, salvageBonus);
    }

    /** This loadout with {@code flying} in the escort slot. */
    public Loadout withWingman(WingmanSpec flying) {
        return new Loadout(ship, armament, shield, plating, special, magnet, Optional.of(flying), salvageBonus);
    }

    /** This loadout with a Salvage scanner adding {@code bonus} (a share) to salvage and hidden crates. */
    public Loadout withSalvage(double bonus) {
        return new Loadout(ship, armament, shield, plating, special, magnet, wingman, bonus);
    }
}
