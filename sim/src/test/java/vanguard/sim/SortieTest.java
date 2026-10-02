package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;

class SortieTest {
    /** Straight down the ship's start column, from just above it. */
    private static final SnakePath DOWN_THE_MIDDLE = SnakePath.through(Ship.START_X, 200, Ship.START_X, -40);

    @Test
    void oneBoltDestroysASkitter() {
        var sortie = TestSpecs.sortie(1);
        sortie.spawn(new SnakeWave(0, DOWN_THE_MIDDLE, 1, false), 0);

        int destroyed = 0;
        int hits = 0;
        for (int i = 0; i < 30; i++) {
            sortie.step(Command.FIRE.bit());
            hits += count(sortie, SimEvents.Type.ENEMY_HIT);
            destroyed += count(sortie, SimEvents.Type.ENEMY_DESTROYED);
        }

        assertEquals(1, hits);
        assertEquals(1, destroyed);
        assertEquals(0, sortie.skitterCount());
        assertEquals(20, sortie.ship().defences().shield(), "it never reached the ship");
    }

    @Test
    void aRammingSkitterIsDestroyedAndDealsContactDamage() {
        var sortie = TestSpecs.sortie(1);
        sortie.spawn(new SnakeWave(0, DOWN_THE_MIDDLE, 1, false), 0);

        int destroyed = 0;
        for (int i = 0; i < 60; i++) {
            sortie.step(Command.NONE);
            destroyed += count(sortie, SimEvents.Type.ENEMY_DESTROYED);
        }

        assertEquals(1, destroyed);
        assertEquals(17, sortie.ship().defences().shield());
        assertEquals(57, sortie.ship().defences().armour());
    }

    @Test
    void skittersMissAShipThatGetsOutOfTheWay() {
        var sortie = TestSpecs.sortie(1);
        sortie.spawn(new SnakeWave(0, DOWN_THE_MIDDLE, 1, false), 0);

        for (int i = 0; i < 60; i++) {
            sortie.step(Command.LEFT.bit());
        }

        assertEquals(20, sortie.ship().defences().shield());
    }

    @Test
    void theSortieRestartsWithFullDefencesAfterTheShipIsDestroyed() {
        var sortie = TestSpecs.sortie(1);
        sortie.spawn(new SnakeWave(0, DOWN_THE_MIDDLE, 40, false), 0);

        int steps = 0;
        while (sortie.flying()) {
            sortie.step(Command.NONE);
            steps++;
            assertTrue(steps < 20 * SimStep.PER_SECOND, "the snake should wear the ship down");
        }
        assertEquals(0, sortie.ship().defences().armour());
        assertEquals(1, sortie.attempt());

        for (int i = 0; i < SimStep.ticks(Sortie.RESTART_SECONDS) - 1; i++) {
            sortie.step(Command.LEFT.bit());
            assertFalse(sortie.flying());
        }
        sortie.step(Command.NONE);

        assertTrue(sortie.flying());
        assertEquals(2, sortie.attempt());
        assertEquals(1, count(sortie, SimEvents.Type.SORTIE_RESTARTED));
        assertEquals(20, sortie.ship().defences().shield());
        assertEquals(60, sortie.ship().defences().armour());
        assertEquals(Ship.START_X, sortie.ship().x());
        assertEquals(0, sortie.skitterCount(), "the waves start again from the beginning");
    }

    @Test
    void theWreckIgnoresCommands() {
        var sortie = TestSpecs.sortie(1);
        sortie.spawn(new SnakeWave(0, DOWN_THE_MIDDLE, 40, false), 0);
        while (sortie.flying()) {
            sortie.step(Command.NONE);
        }
        double x = sortie.ship().x();

        sortie.step(Command.of(Command.LEFT, Command.FIRE));

        assertEquals(x, sortie.ship().x());
        assertEquals(0, count(sortie, SimEvents.Type.SHOT_FIRED));
    }

    @Test
    void theTestSortieSendsItsFirstSnakeAfterOneSecond() {
        var sortie = TestSpecs.sortie(1);
        for (int i = 0; i < SimStep.PER_SECOND - 1; i++) {
            sortie.step(Command.NONE);
        }
        assertEquals(0, sortie.skitterCount());

        sortie.step(Command.NONE);

        assertEquals(6, sortie.skitterCount());
    }

    @Test
    void groundScrollsAtTheLevel01Speed() {
        var sortie = TestSpecs.sortie(1);
        for (int i = 0; i < SimStep.PER_SECOND; i++) {
            sortie.step(Command.NONE);
        }

        assertEquals(130, sortie.groundScroll(), 1e-9);
    }

    @Test
    void sameSeedAndCommandsGiveTheSameHash() {
        assertEquals(run(7, 1800, -1), run(7, 1800, -1));
    }

    @Test
    void oneDifferentCommandOrSeedChangesTheHash() {
        assertNotEquals(run(7, 901, -1), run(7, 901, 900));
        assertNotEquals(run(7, 1800, -1), run(8, 1800, -1));
    }

    @Test
    void steppingDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        var sortie = TestSpecs.sortie(3);
        for (int i = 0; i < 600; i++) {
            sortie.step(Pilot.commands(i));
        }
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 600; i < 600 + 3600; i++) {
            sortie.step(Pilot.commands(i));
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "3600 steps allocated " + allocated + " bytes");
    }

    /** Runs the scripted pilot; at step {@code changedStep} it presses left as well. */
    private static long run(long seed, int steps, int changedStep) {
        var sortie = TestSpecs.sortie(seed);
        for (int i = 0; i < steps; i++) {
            int commands = Pilot.commands(i);
            sortie.step(i == changedStep ? commands | Command.LEFT.bit() : commands);
        }
        return sortie.stateHash();
    }

    static int count(Sortie sortie, SimEvents.Type type) {
        int count = 0;
        for (int i = 0; i < sortie.events().size(); i++) {
            if (sortie.events().type(i) == type) {
                count++;
            }
        }
        return count;
    }

    /** A scripted pilot that weaves left and right, sometimes in precision mode, firing most of the time. */
    static final class Pilot {
        private Pilot() {}

        static int commands(int step) {
            int phase = step % 240;
            int commands = phase < 120 ? Command.LEFT.bit() : Command.RIGHT.bit();
            if (step % 600 > 450) {
                commands |= Command.PRECISION.bit();
            }
            if (step % 300 < 270) {
                commands |= Command.FIRE.bit();
            }
            if (step % 500 < 60) {
                commands |= Command.UP.bit();
            }
            return commands;
        }
    }
}
