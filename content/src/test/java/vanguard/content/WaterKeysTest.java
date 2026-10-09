package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;

/**
 * M5 part E, step E2a (design/tech/architecture, Data file schemas): the level flag {@code water}, the
 * layer {@code sub} on a trigger (Level 11's sunken pod) and the torpedo's keys ({@code hits:
 * anti-sub} with a speed, {@code accelerate}, a {@code cone}, a {@code turn} per level and a range),
 * loaded strictly, checked and mapped for the simulation. Level 11's data is written later, so the
 * level keys are tried on Level 08's file with its billboard trigger sunk.
 */
class WaterKeysTest {
    private static final String LEVEL = "campaign/act-2-homefront/level-08-neon-skyline/data.yaml";
    private static final String KEY = "act-2-homefront/level-08-neon-skyline";
    private static final String TORPEDO = "player/weapons/torpedo-pod/data.yaml";
    private static final String SKITTER = "enemies/air/skitter/data.yaml";

    private static final String BILLBOARD = "    layer: ground\n    size: [72, 40]\n    sprite: billboard\n    hits: 3";
    private static final String SCALE = "bounty_scale: 0.56";

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "the data has " + what);
        return text.replace(what, with);
    }

    private static String water(String text) {
        return replace(text, SCALE, SCALE + "\nwater: true");
    }

    private static String sunk(String text) {
        return replace(text, BILLBOARD, BILLBOARD.replace("layer: ground", "layer: sub"));
    }

    private static Content load(Map<String, UnaryOperator<String>> edits) {
        return ContentLoader.load(DesignTree.dataFiles().stream()
                .map(f -> edits.containsKey(f.path())
                        ? new DataFile(f.path(), edits.get(f.path()).apply(f.text()))
                        : f)
                .toList());
    }

    @Test
    void aLevelOverWaterWithASunkenTriggerLoadsAndMapsForTheSimulation() {
        Content content = load(Map.of(LEVEL, text -> sunk(water(text))));
        assertTrue(content.level(KEY).overWater());

        LevelScript script = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        assertTrue(script.water());
        List<LevelScript.GroundObjectSpec> triggers = script.groundObjects().stream()
                .filter(LevelScript.GroundObjectSpec::trigger)
                .toList();
        assertEquals(1, triggers.size());
        assertTrue(triggers.getFirst().submerged());
        assertEquals(3, triggers.getFirst().hits());
    }

    @Test
    void aLevelIsOverLandUnlessItSaysWater() {
        Content content = ContentLoader.fromClasspath();
        for (String key : content.levels().keySet()) {
            // M5 part E: Level 11 is the first level over water (its sunken pod lies under it).
            boolean water = key.equals(Level11Test.LEVEL);
            assertEquals(water, content.level(key).overWater(), key);
            assertEquals(water, SimSpecs.level(content, key, Difficulty.MEDIUM).water(), key);
            assertEquals(
                    water,
                    SimSpecs.level(content, key, Difficulty.MEDIUM).groundObjects().stream()
                            .anyMatch(LevelScript.GroundObjectSpec::submerged),
                    key);
        }
        assertEquals(Layer.SUB, Layers.of("sub"));
    }

    @Test
    void onlyATriggerLiesUnderTheWaterAndOnlyInALevelOverWater() {
        assertProblem(Map.of(LEVEL, WaterKeysTest::sunk), "ground_targets[2].layer", "water: true");
        assertProblem(
                Map.of(
                        LEVEL,
                        text -> replace(sunk(water(text)), "    hits: 3\n    reveals", "    hp: 10\n    reveals")),
                "ground_targets[2].layer",
                "only a trigger");
        assertProblem(
                Map.of(LEVEL, text -> replace(text, BILLBOARD, BILLBOARD.replace("layer: ground", "layer: air"))),
                "ground_targets[2].layer",
                "not air");
    }

    @Test
    void aUnitsStatBlockIsNotOnSub() {
        assertProblem(Map.of(SKITTER, text -> replace(text, "layer: air", "layer: sub")), "layer", "dives");
    }

    @Test
    void aTorpedoNeedsItsConeTurnRangeAndAcceleration() {
        assertProblem(Map.of(TORPEDO, text -> replace(text, "cone: 60\n", "")), "anti-sub", "cone");
        assertProblem(Map.of(TORPEDO, text -> replace(text, "accelerate: 0.5\n", "")), "anti-sub", "accelerate");
        assertProblem(
                Map.of(TORPEDO, text -> replace(text, "damage: 17, turn: 60}", "damage: 17}")), "anti-sub", "turn");
        assertProblem(Map.of(TORPEDO, text -> replace(text, "range: 500", "range: screen")), "anti-sub", "range");
    }

    /** Loads the design tree with the edits; exactly one problem is reported, containing every part. */
    private static void assertProblem(Map<String, UnaryOperator<String>> edits, String... parts) {
        var e = assertThrows(ContentException.class, () -> load(edits));

        assertEquals(1, e.problems().size(), e.getMessage());
        for (String part : parts) {
            assertTrue(
                    e.problems().getFirst().contains(part),
                    "'" + part + "' in " + e.problems().getFirst());
        }
    }
}
