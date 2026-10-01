package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;

class WorldTest {
    private static final SimConfig CONFIG = SimConfig.gateLoad(42);

    @Test
    void sameSeedAndCommandsGiveTheSameHash() {
        assertEquals(run(CONFIG, 600, -1), run(CONFIG, 600, -1));
    }

    @Test
    void oneDifferentCommandChangesTheHash() {
        assertNotEquals(run(CONFIG, 600, -1), run(CONFIG, 600, 300));
    }

    @Test
    void enemyCountIsKeptAtTheTarget() {
        World world = new World(CONFIG);
        Autopilot autopilot = new Autopilot();
        for (int i = 0; i < 1200; i++) {
            world.step(autopilot.commands(world));
            assertEquals(CONFIG.enemyCount(), world.enemyCount());
        }
        assertTrue(world.score() > 0, "the autopilot should kill something in 20 s");
        assertTrue(world.bulletCount() > 100, "gate load should keep many bullets in flight");
    }

    @Test
    void steppingDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        World world = new World(CONFIG);
        Autopilot autopilot = new Autopilot();
        for (int i = 0; i < 600; i++) {
            world.step(autopilot.commands(world));
        }
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < 3600; i++) {
            world.step(autopilot.commands(world));
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;
        assertTrue(allocated < 1024, "3600 steps allocated " + allocated + " bytes");
    }

    /** Runs the autopilot; at step {@code changedStep} it injects a different command. */
    private static long run(SimConfig config, int steps, int changedStep) {
        World world = new World(config);
        Autopilot autopilot = new Autopilot();
        for (int i = 0; i < steps; i++) {
            int commands = autopilot.commands(world);
            world.step(i == changedStep ? commands ^ Command.LEFT.bit() : commands);
        }
        return world.stateHash();
    }
}
