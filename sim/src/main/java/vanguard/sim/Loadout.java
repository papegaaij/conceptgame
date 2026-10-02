package vanguard.sim;

/**
 * What the ship flies with: its hull, front gun, shield and plating.
 *
 * @param ship the hull's flight numbers, including the fitted engine's speed
 * @param gun the front gun at its upgrade level
 */
public record Loadout(ShipSpec ship, PulseCannon gun, ShieldModel shield, Plating plating) {}
