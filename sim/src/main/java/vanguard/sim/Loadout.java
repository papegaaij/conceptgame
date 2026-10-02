package vanguard.sim;

/**
 * What the ship flies with: its hull, weapons, shield and plating.
 *
 * @param ship the hull's flight numbers, including the fitted engine's speed
 * @param armament the fitted weapons at their upgrade levels
 * @param shield the shield, its regeneration including the spare-power bonus (design/player/generator)
 */
public record Loadout(ShipSpec ship, Armament armament, ShieldModel shield, Plating plating) {}
