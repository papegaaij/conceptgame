package vanguard.sim;

import java.util.Optional;

/**
 * What the ship flies with: its hull, weapons, shield, plating and special.
 *
 * @param ship the hull's flight numbers, including the fitted engine's speed
 * @param armament the fitted weapons at their upgrade levels
 * @param shield the shield, its regeneration including the spare-power bonus (design/player/generator)
 * @param special the fitted special with the charges carried into the level; empty for none
 */
public record Loadout(
        ShipSpec ship, Armament armament, ShieldModel shield, Plating plating, Optional<SpecialSpec> special) {
    /** A loadout without a special. */
    public Loadout(ShipSpec ship, Armament armament, ShieldModel shield, Plating plating) {
        this(ship, armament, shield, plating, Optional.empty());
    }

    /** This loadout with {@code fitted} in the special slot. */
    public Loadout withSpecial(SpecialSpec fitted) {
        return new Loadout(ship, armament, shield, plating, Optional.of(fitted));
    }
}
