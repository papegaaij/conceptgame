package vanguard.sim;

import java.util.List;

/**
 * The starting front gun's numbers at one upgrade level, from
 * design/player/weapons/pulse-cannon/data.yaml (built by {@code vanguard.content.SimSpecs}): a
 * volley of parallel bolts from the front muzzle (one at L1, four at L5).
 *
 * @param shotsPerSecond volleys per second while firing
 * @param damage damage per bolt
 * @param boltSpeed bolt speed in px/s, straight up until it leaves the screen
 * @param bolt the bolt's hit box
 * @param pattern each bolt's x offset from the muzzle, px
 */
public record PulseCannon(double shotsPerSecond, double damage, double boltSpeed, Hitbox bolt, List<Double> pattern) {
    public PulseCannon {
        pattern = List.copyOf(pattern);
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("a volley has at least one bolt");
        }
    }

    int intervalTicks() {
        return SimStep.ticks(1 / shotsPerSecond);
    }
}
