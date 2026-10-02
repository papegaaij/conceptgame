package vanguard.sim;

/**
 * The starting front gun's numbers at one upgrade level, from
 * design/player/weapons/pulse-cannon/data.yaml (built by {@code vanguard.content.SimSpecs}). The
 * simulation fires level 1 only so far: a single bolt from the front muzzle.
 *
 * @param shotsPerSecond volleys per second while firing
 * @param damage damage per bolt
 * @param boltSpeed bolt speed in px/s, straight up until it leaves the screen
 * @param bolt the bolt's hit box
 */
public record PulseCannon(double shotsPerSecond, double damage, double boltSpeed, Hitbox bolt) {
    int intervalTicks() {
        return SimStep.ticks(1 / shotsPerSecond);
    }
}
