package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Array;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import vanguard.sim.Layer;
import vanguard.sim.SimStep;

/**
 * M5 part C's unit looks (tools/art/hive_node.py, tools/art/ravager.py): the Hive Node's frame is its
 * iris state (round(iris × 5)) times four plus its pulse; the Ravager's leap shows its gallop near the
 * ground and one of four lift steps above it, the shadow sliding out with the lift; the node's creep
 * withers over 2 s and then lies dry for the stump's time; a Ravager killed in its air window dies in
 * the air.
 */
class HiveNodeRavagerLooksTest {
    private static final Path SPRITES = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "sprites");

    @Test
    void theNodesFrameIsItsIrisStateTimesFourPlusItsPulse() {
        assertEquals(0, EnemyLooks.irisFrame(0, 0, 0), "shut, first pulse frame");
        assertEquals(5 * 4, EnemyLooks.irisFrame(1, 0, 0), "open");
        assertEquals(2 * 4, EnemyLooks.irisFrame(0.4, 0, 0), "0.4 opens to state 2");
        assertEquals(3 * 4, EnemyLooks.irisFrame(0.55, 0, 0), "rounds to the nearest state");
        // The pulse loops at 4 fps: a frame every 15 steps, each node at its own phase.
        assertEquals(1, EnemyLooks.irisFrame(0, SimStep.PER_SECOND / 4, 0));
        assertEquals(3, EnemyLooks.irisFrame(0, 0, 3));
        assertEquals(0, EnemyLooks.irisFrame(0, SimStep.PER_SECOND, 0), "four frames a second, then round");
        for (double iris = 0; iris <= 1; iris += 0.05) {
            for (int tick = 0; tick < 120; tick += 7) {
                int frame = EnemyLooks.irisFrame(iris, tick, 5);
                assertTrue(frame >= 0 && frame < EnemyLooks.IRIS_STATES * EnemyLooks.PULSE_FRAMES);
            }
        }
    }

    @Test
    void theRavagerGallopsNearTheGroundAndLeapsInFourLiftSteps() {
        assertEquals(-1, EnemyLooks.leapStep(EnemyLooks.leapLift(0)), "at take-off it gallops");
        assertEquals(-1, EnemyLooks.leapStep(EnemyLooks.leapLift(1)), "and as it lands");
        assertEquals(-1, EnemyLooks.leapStep(0.12));
        assertEquals(0, EnemyLooks.leapStep(0.125));
        assertEquals(3, EnemyLooks.leapStep(EnemyLooks.leapLift(0.5)), "the apex: the 1.43x step");
        assertEquals(1, EnemyLooks.leapStep(0.5));
        assertEquals(1.43, EnemyLooks.LEAP_SCALES[3], 1e-9);
        for (int s = 1; s < EnemyLooks.LEAP_STEPS; s++) {
            assertTrue(EnemyLooks.LEAP_SCALES[s] > EnemyLooks.LEAP_SCALES[s - 1]);
        }
        // The shadow slides out to the air layer's offset at the apex.
        assertEquals(Shadows.AIR_DX, EnemyLooks.LEAP_SHADOW_DX);
        assertEquals(Shadows.AIR_DY, EnemyLooks.LEAP_SHADOW_DY);
    }

    @Test
    void theCreepWithersOverTwoSecondsThenLiesDryWithTheStump() {
        Array<String> creep = new Array<>();
        for (int i = 0; i < 9; i++) {
            creep.add("hive-node-creep_" + i);
        }
        int remains = SimStep.ticks(10);
        Array<String> wither = EnemyLooks.wither(creep, remains);
        assertEquals(creep.get(1), wither.first(), "the wither starts at frame 1 (frame 0 is the live patch)");
        assertEquals(creep.get(8), wither.get(7), "frame 8, the dry crust, after 2 s");
        assertEquals(SimStep.ticks(2), 8 * EnemyLooks.WITHER_FRAME_TICKS);
        assertEquals(remains, wither.size * EnemyLooks.WITHER_FRAME_TICKS, "it lies as long as the stump");
        assertEquals(creep.get(8), wither.peek());
        assertTrue(EnemyLooks.wither(new Array<String>(), remains).isEmpty());
    }

    @Test
    void aRavagerKilledInItsAirWindowDiesInTheAir() {
        AirborneWalkers airborne = new AirborneWalkers();
        airborne.add(3, 100, 200);
        assertEquals(Layer.AIR, airborne.layerOf(3, Layer.GROUND, 108, 195), "within a step's leap");
        assertEquals(Layer.GROUND, airborne.layerOf(3, Layer.GROUND, 140, 200), "another one, galloping");
        assertEquals(Layer.GROUND, airborne.layerOf(4, Layer.GROUND, 100, 200), "another kind");
        assertFalse(airborne.contains(3, 100, 260));
    }

    @Test
    void theProductionFramesMatchTheRenderersIndexing() throws IOException {
        assertEquals(EnemyLooks.IRIS_STATES * EnemyLooks.PULSE_FRAMES, count("hive-node_"));
        assertEquals(EnemyLooks.IRIS_STATES * EnemyLooks.PULSE_FRAMES, count("hive-node-glow_"));
        assertEquals(9, count("hive-node-creep_"));
        assertEquals(16 * EnemyLooks.LEAP_STEPS, count("ravager-leap_"));
        assertEquals(16 * EnemyLooks.LEAP_STEPS, count("ravager-leap-glow_"));
        assertEquals(16 * 8, count("ravager_"));
    }

    /** The frames {@code <prefix><n>.png} in the sprites folder. */
    private static long count(String prefix) throws IOException {
        try (Stream<Path> files = Files.list(SPRITES)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.startsWith(prefix)
                            && name.substring(prefix.length()).matches("\\d+\\.png"))
                    .count();
        }
    }
}
