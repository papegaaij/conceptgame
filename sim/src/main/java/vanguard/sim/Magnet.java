package vanguard.sim;

/**
 * The fitted Pickup magnet at its level (design/player/systems, Utility modules): every pickup
 * within {@code radius} of the ship's centre flies straight at the ship at {@code pullSpeed}
 * instead of drifting down, until the ship's collection radius takes it. Built by {@code
 * vanguard.content.SimSpecs} from design/player/systems/data.yaml.
 *
 * @param radius the reach from the ship's centre, px
 * @param pullSpeed how fast a pickup in reach flies to the ship, px/s
 */
public record Magnet(double radius, double pullSpeed) {
    public Magnet {
        if (!(radius > 0) || !(pullSpeed > 0)) {
            throw new IllegalArgumentException("a magnet needs a positive radius and pull speed");
        }
    }
}
