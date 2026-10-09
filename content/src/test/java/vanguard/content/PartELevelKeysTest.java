package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;

/**
 * M5 part E's level keys of step E3a (design/tech/architecture, Data file schemas), on Level 11's own
 * data: a destructible's {@code group} of its own, whose last one destroyed drops the pickup placed on
 * the group (the floating containers' armour patch), and a trigger's {@code easy} / {@code hard}
 * {@code hits} (the sunken pod's hard 5); loaded strictly, checked and mapped for the simulation. The
 * water flag, the arena, the convoy and the afloat secondary have their own tests (WaterKeysTest,
 * KrakenTest, PartEUnitsDataTest, Level11Test).
 */
class PartELevelKeysTest {
    private static final String KEY = Level11Test.LEVEL;
    private static final String PATH = "campaign/" + KEY + "/data.yaml";
    private static final String PATCH =
            "  - {pickup: armour patch, dropped_by: {group: containers, unit: last}}   # the last floating container destroyed\n";

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 11's data has " + what);
        return text.replace(what, with);
    }

    private static List<DataFile> files(UnaryOperator<String> change) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(PATH) ? new DataFile(f.path(), change.apply(f.text())) : f)
                .toList();
    }

    private static void assertProblem(UnaryOperator<String> change, String message) {
        Exception e = assertThrows(Exception.class, () -> ContentLoader.load(files(change)));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @Test
    void theContainersAreAGroupOfTheirOwn() {
        Content content = ContentLoader.fromClasspath();
        LevelData.GroundTarget containers = content.level(KEY).groundTargets().stream()
                .filter(target -> target.group().isPresent() && target.enemy().isEmpty())
                .findFirst()
                .orElseThrow();
        assertEquals(Optional.of("containers"), containers.group());
        assertEquals(List.of("containers"), SimSpecs.destructibleGroups(content.level(KEY)));
        assertTrue(content.level(KEY).objectives().groups().isEmpty(), "the objectives never count it");
        LevelScript level = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        assertTrue(level.groupDrops().isEmpty(), "not an objectives' group drop");
        assertTrue(level.groundObjects().stream()
                .filter(o -> o.group() == 0)
                .allMatch(o -> o.groupSize() == 8 && o.groupDrop().equals(Optional.of(PickupType.ARMOUR_PATCH))));
        assertTrue(level.groundObjects().stream()
                .filter(o -> o.group() < 0)
                .allMatch(o -> o.groupSize() == 0 && o.groupDrop().isEmpty()));
    }

    @Test
    void theTriggersHitsChangeOnHardAndEasy() {
        Content content = ContentLoader.load(
                files(text -> replace(text, "    hard: {hits: 5}\n", "    easy: {hits: 2}\n    hard: {hits: 5}\n")));
        for (Difficulty difficulty : Difficulty.values()) {
            int hits = SimSpecs.level(content, KEY, difficulty).groundObjects().stream()
                    .filter(o -> o.secret().equals("sunken pod"))
                    .findFirst()
                    .orElseThrow()
                    .hits();
            assertEquals(
                    switch (difficulty) {
                        case EASY -> 2;
                        case MEDIUM -> 3;
                        case HARD -> 5;
                    },
                    hits,
                    difficulty.name());
        }
    }

    @Test
    void theNewKeysAreChecked() {
        assertProblem(text -> replace(text, PATCH, ""), "no pickup is dropped by group 'containers'");
        assertProblem(
                text -> replace(text, "    hard: {hits: 5}\n", "    hard: {at: [[93.78, 296]]}\n"),
                "only enemies have easy/hard placements");
        assertProblem(
                text -> replace(text, "    hard: {hits: 5}\n", "    hard: {hits: 6, at: [[93.78, 296]]}\n"),
                "a trigger's easy/hard gives its hits");
        assertProblem(
                text -> replace(text, "    at: []\n    hard: {at:", "    at: []\n    hard: {hits: 2, at:"),
                "an easy/hard change gives at (an enemy nest's placements) or hits (a trigger's)");
        assertProblem(text -> replace(text, "    hard: {hits: 5}\n", "    hard: {hits: 0}\n"), "hits must be > 0");
        assertProblem(
                text -> replace(
                        text,
                        "    hits: 3\n    hard: {hits: 5}\n",
                        "    hits: 3\n    group: containers\n    hard: {hits: 5}\n"),
                "only enemies and destructibles have a group");
        assertProblem(
                text -> replace(
                        text,
                        "    hp: 3\n    drop: small salvage\n",
                        "    hp: 3\n    drop: small salvage\n    hard: {hits: 2}\n"),
                "only enemies have easy/hard placements; a trigger's easy/hard gives its hits");
    }
}
