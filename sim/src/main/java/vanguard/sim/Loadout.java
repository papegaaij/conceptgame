package vanguard.sim;

import java.util.Optional;

/**
 * What the ship flies with: its hull, weapons, shield, plating, special and utility modules.
 *
 * @param ship the hull's flight numbers, including the fitted engine's speed
 * @param armament the fitted weapons at their upgrade levels
 * @param shield the shield, its regeneration including the spare-power bonus (design/player/generator)
 * @param special the fitted special with the charges carried into the level; empty for none
 * @param magnet the fitted Pickup magnet at its level; empty for none
 */
public record Loadout(
        ShipSpec ship,
        Armament armament,
        ShieldModel shield,
        Plating plating,
        Optional<SpecialSpec> special,
        Optional<Magnet> magnet) {
    /** A loadout without a special or a magnet. */
    public Loadout(ShipSpec ship, Armament armament, ShieldModel shield, Plating plating) {
        this(ship, armament, shield, plating, Optional.empty(), Optional.empty());
    }

    /** This loadout with {@code fitted} in the special slot. */
    public Loadout withSpecial(SpecialSpec fitted) {
        return new Loadout(ship, armament, shield, plating, Optional.of(fitted), magnet);
    }

    /** This loadout with {@code fitted} in a utility bay. */
    public Loadout withMagnet(Magnet fitted) {
        return new Loadout(ship, armament, shield, plating, special, Optional.of(fitted));
    }
}
