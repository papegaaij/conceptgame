package vanguard.sim;

/**
 * Tuning of a simulation run.
 *
 * @param seed            random seed of the level
 * @param enemyCount      number of enemies kept alive on the layers
 * @param enemyFireTicks  mean number of steps between two shots of one enemy
 * @param volleySize      bullets per player volley (a fan)
 * @param volleyTicks     steps between two player volleys
 */
public record SimConfig(long seed, int enemyCount, int enemyFireTicks, int volleySize, int volleyTicks) {
    public SimConfig {
        if (enemyCount < 0 || enemyFireTicks < 1 || volleySize < 1 || volleyTicks < 1) {
            throw new IllegalArgumentException("invalid simulation config");
        }
    }

    /** The load of spike gate 1: 150 enemies and several hundred bullets. */
    public static SimConfig gateLoad(long seed) {
        return new SimConfig(seed, 150, 60, 7, 4);
    }
}
