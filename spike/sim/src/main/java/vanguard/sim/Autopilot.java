package vanguard.sim;

/**
 * Generates commands for unattended runs (benchmarks, recorded replays): fires constantly and
 * weaves across the play field. Its commands depend only on the world state, so it is
 * deterministic too.
 */
public final class Autopilot {
    private static final double TOLERANCE = 4;

    /** The commands for the next step of {@code world}. */
    public int commands(World world) {
        double t = world.tick() * World.STEP_SECONDS;
        double targetX = World.WIDTH / 2.0 + 180 * Trig.sin(t * 0.7);
        double targetY = 110 + 50 * Trig.sin(t * 0.31);
        Player player = world.player();
        int commands = Command.FIRE.bit();
        commands |= axis(player.x(), targetX, Command.LEFT, Command.RIGHT);
        commands |= axis(player.y(), targetY, Command.DOWN, Command.UP);
        return commands;
    }

    private static int axis(double position, double target, Command decrease, Command increase) {
        if (position < target - TOLERANCE) {
            return increase.bit();
        }
        if (position > target + TOLERANCE) {
            return decrease.bit();
        }
        return Command.NONE;
    }
}
