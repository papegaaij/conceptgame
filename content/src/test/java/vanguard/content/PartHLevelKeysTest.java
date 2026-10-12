package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;

/**
 * M5 part H's level keys (design/tech/architecture, Data file schemas), on Level 14's own data: the
 * {@code root_bursts} block (its telegraph, first-shot delay with hard's, the growths' spacing and group, each
 * burst's time, point and growths with easy's), the arena of speed 0 after which a daylight section scrolls,
 * and a {@code groups} secondary with its tracker {@code label}; loaded strictly, checked ({@link PartHChecks})
 * and mapped ({@link PartHRules}) for the simulation (javadoc "M5 part H": M4's part H is the intel and the act
 * summary).
 */
class PartHLevelKeysTest {
    private static final String KEY = Level14Test.LEVEL;
    private static final String PATH = "campaign/" + KEY + "/data.yaml";
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 14's data has " + what);
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
    void theRootBurstsAreReadAndMapped() {
        LevelData.RootBursts block = CONTENT.level(KEY).rootBursts().orElseThrow();
        assertEquals(1.0, block.telegraph());
        assertEquals(0.75, block.firstShotDelay(Difficulty.MEDIUM));
        assertEquals(0.75, block.firstShotDelay(Difficulty.EASY));
        assertEquals(0.5, block.firstShotDelay(Difficulty.HARD));
        assertEquals("roots", block.group());
        assertEquals(4, block.bursts().size());
        LevelData.RootBurst first = block.bursts().getFirst();
        assertEquals(List.of("spine-turret", "polyp-mortar"), first.growthsOn(Difficulty.MEDIUM));
        assertEquals(List.of("spine-turret"), first.growthsOn(Difficulty.EASY));
        List<LevelScript.GroundUnit> growths = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).groundUnits().stream()
                .filter(unit -> unit.burst().isPresent())
                .toList();
        LevelScript.GroundUnit turret = growths.getFirst();
        LevelScript.GroundUnit mortar = growths.get(1);
        assertEquals(first.x() - 26, turret.x(), 1e-9, "the growths 52 px apart, centred on the burst");
        assertEquals(first.x() + 26, mortar.x(), 1e-9);
        assertEquals("polyp-mortar", mortar.enemy().slug());
        LevelScript.RootBurst burst = turret.burst().orElseThrow();
        assertEquals(0, burst.index());
        assertEquals(first.y(), burst.depth());
        assertEquals(3, growths.getLast().burst().orElseThrow().index());
    }

    @Test
    void theRootBurstsAreChecked() {
        assertProblem(
                text -> replace(text, "{t: 63, x: 150, y: 120,", "{t: 63, x: 150, y: 60,"),
                "root_bursts.bursts[0].y: a burst lies 100.0–400.0 px below the top edge");
        assertProblem(
                text -> replace(text, "{t: 63, x: 150, y: 120,", "{t: 63, x: 470, y: 120,"),
                "root_bursts.bursts[0].x: its growths leave the play field");
        assertProblem(
                text -> replace(text, "group: roots           # the secondary's group", "group: trunks"),
                "root_bursts.group: no group 'trunks' in the objectives' groups");
        assertProblem(
                text -> replace(
                        text,
                        "growths: [spine-turret, polyp-mortar], easy: {growths: [spine-turret]}, notes",
                        "growths: [skitter, polyp-mortar], easy: {growths: [spine-turret]}, notes"),
                "'skitter' does not stand on the ground");
        assertProblem(
                text -> replace(text, "{t: 92, x: 320,", "{t: 107.5, x: 320,"),
                "the growths appear before the arena starts");
        assertProblem(text -> replace(text, "{t: 74, x: 330,", "{t: 54, x: 330,"), "listed in time order");
    }

    @Test
    void aGroupsSecondaryMayNameItsTracker() {
        LevelData.Secondary secondary =
                CONTENT.level(KEY).objectives().secondary().orElseThrow();
        assertEquals(List.of("roots"), secondary.groups().orElseThrow());
        assertEquals("ROOTS", secondary.label().orElseThrow());
        assertEquals(
                "ROOTS",
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).secondary().label());
    }

    @Test
    void theDaylightScrollsAfterTheArena() {
        List<LevelData.Section> sections = CONTENT.level(KEY).sections();
        assertTrue(sections.get(4).isArena());
        assertEquals(12, sections.get(5).end() - sections.get(4).end(), "12 s of daylight");
        assertEquals(25, sections.get(5).speed().orElseThrow(), "a 25 px/s drift (steps H7b, H9)");
        assertEquals(80, sections.get(2).speed().orElseThrow(), "the root field's slower scroll");
    }
}
