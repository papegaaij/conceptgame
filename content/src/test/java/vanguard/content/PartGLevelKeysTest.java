package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * M5 part G's level keys (design/tech/architecture, Data file schemas), on Level 13's own data: the
 * {@code defend} primary (its ally, place, start and duration, {@code targeted_by} with the easy/hard
 * changes), a timed halt (a section of speed 0 without an arena, its {@code ramp}), a wave's {@code lanes},
 * the {@code cross} formation from {@code all} edges, a circle's centre {@code at}, a column's {@code
 * pairs}, a nest's {@code side} entry, the {@code integrity} secondary, the {@code drops}, the radio's
 * {@code ally-below}, the music's {@code jingle} and the {@code water_splash}; loaded strictly, checked
 * and mapped for the simulation (javadoc "M5 part G": M4's part G is {@code LevelRulesTest}).
 */
class PartGLevelKeysTest {
    private static final String KEY = Level13Test.LEVEL;
    private static final String PATH = "campaign/" + KEY + "/data.yaml";
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 13's data has " + what);
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
    void theTimedHaltIsMappedWithItsRamps() {
        LevelData data = CONTENT.level(KEY);
        assertTrue(data.sections().get(1).timedHalt());
        assertTrue(data.sections().get(2).timedHalt());
        assertTrue(data.sections().get(3).timedHalt());
        assertFalse(data.sections().get(0).timedHalt());
        assertEquals(2, data.sections().get(1).rampSeconds());
        List<LevelScript.Section> sections =
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).sections();
        assertTrue(sections.get(1).timedHalt());
        assertFalse(sections.get(1).arena());
        assertEquals(2, sections.get(3).rampSeconds());
        // The data's scroll (the backdrop's places) eases as the simulation's, within a pixel.
        LevelScript level = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);
        for (double t : new double[] {10, 38, 38.5, 39, 39.5, 40, 100, 220, 220.5, 221, 222, 230}) {
            assertEquals(Sortie.scrollAt(level, t), data.scrollAt(t), 1.5, "t=" + t);
        }
    }

    @Test
    void theDefendIsMapped() {
        LevelData.Defend defend = CONTENT.level(KEY).objectives().defend().orElseThrow();
        assertEquals("defend", CONTENT.level(KEY).objectives().primary());
        assertEquals(LevelData.HookMode.IN_ARC, defend.targetedBy().get("scuttler"));
        assertEquals(LevelData.HookMode.NONE, defend.modes(Difficulty.EASY).get("stinger"));
        assertEquals(LevelData.HookMode.ALWAYS, defend.modes(Difficulty.HARD).get("stinger"));
        LevelScript.Defend easy =
                SimSpecs.level(CONTENT, KEY, Difficulty.EASY).defend().orElseThrow();
        assertFalse(easy.targetedBy().containsKey("stinger"), "none is left out");
        assertTrue(CONTENT.allies().allies().get("nansen-relay").standing());
    }

    @Test
    void theWaveKeysAreMapped() {
        LevelScript hard = SimSpecs.level(CONTENT, KEY, Difficulty.HARD);
        WaveSpec cross = hard.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.CROSS && wave.t() == 118)
                .findFirst()
                .orElseThrow();
        assertEquals(8, cross.count());
        assertEquals(8, cross.lanes().size());
        assertEquals(new WaveSpec.At(240, -40), cross.lanes().getFirst().getFirst());
        WaveSpec convoy = hard.waves().stream()
                .filter(wave -> wave.enemy().slug().equals("skimmer") && wave.t() == 84)
                .findFirst()
                .orElseThrow();
        assertEquals(WaveSpec.Entry.REAR, convoy.entry());
        assertEquals(1, convoy.lanes().size());
        assertTrue(CONTENT.level(KEY).navalSplashOnly());
        assertEquals(218, CONTENT.level(KEY).music().jingle().orElseThrow().t());
        // Step G8: the theme fades over grid online's 2.6 s chord, the jingle after it (t=220.6).
        assertEquals(2.6, CONTENT.level(KEY).music().jingle().orElseThrow().fadeSeconds());
    }

    @Test
    void aTimedHaltNeedsADefendPrimary() {
        assertProblem(
                text -> replace(
                                replace(text, "  primary: defend\n", "  primary: reach-end\n"),
                                "  secondary: {integrity: 0.5, label: INTEGRITY, credits: 95, name: Keep the relay at 50 % or more}",
                                "  secondary: {kill_ratio: 0.5, credits: 95}")
                        .replaceAll("(?s)  defend:   # M5 part G.*?hard: \\{hp: 520\\}\n", ""),
                "a timed halt (speed 0 without an arena) needs a defend primary");
    }

    @Test
    void nothingEntersAtTheTopEdgeInTheHalt() {
        assertProblem(
                text -> replace(
                        text,
                        "  - {pickup: overdrive, dropped_by: {wave: 124, unit: first}}",
                        "  - {pickup: overdrive, dropped_by: {wave: 124, unit: first}}\n  - {pickup: shield cell, at: [60, 200]}"),
                "enters at the top edge in a timed halt");
        assertProblem(
                text -> replace(text, "    at: [[12, 380]]\n", "    at: [[62, 380]]\n"),
                "enters at the top edge in a timed halt");
    }

    @Test
    void theRampFitsTheSectionBeside() {
        assertProblem(
                text -> replace(
                        text,
                        "    ramp: 2           # s the scroll eases down",
                        "    ramp: 50          # s the scroll eases down"),
                "the ease into the halt (50.0 s) fits the section before it");
    }

    @Test
    void theDefendLiesInTheHalt() {
        assertProblem(text -> replace(text, "    duration: 178", "    duration: 190"), "lies inside a timed halt");
    }

    @Test
    void aCrossComesFromAllEdgesWithALanePerUnit() {
        assertProblem(
                text -> replace(text, "    count: 8\n    from: all\n", "    count: 8\n    from: front\n"),
                "a cross enters from all edges (from: all)");
        assertProblem(
                text -> replace(text, "    hard: {count: 8}\n", "    hard: {count: 9}\n"),
                "a cross has a lane per unit: 9 on hard");
    }

    @Test
    void onlyAColumnEntersInPairs() {
        assertProblem(
                text -> replace(
                        text,
                        "{t: 132, formation: line abreast, enemy: needler, count: 3, from: front}",
                        "{t: 132, formation: line abreast, enemy: needler, count: 3, from: front, hard: {pairs: 0.4}}"),
                "only a column enters in pairs (pairs)");
    }

    @Test
    void lanesAreForLaneWalkers() {
        assertProblem(
                text -> replace(
                        text,
                        "{t: 132, formation: line abreast, enemy: needler, count: 3, from: front}",
                        "{t: 132, formation: line abreast, enemy: needler, count: 3, from: front, lanes: [[[0, 0], [10, 10]]]}"),
                "only a ground unit that does not walk follows lanes");
    }

    @Test
    void hookModesNameTheLevelsEnemies() {
        assertProblem(
                text -> replace(text, "skimmer: alternate, scuttler", "skimmer: alternate, ravager: always, scuttler"),
                "no wave or nest of 'ravager' in this level");
        assertProblem(text -> replace(text, "skimmer: alternate", "skimmer: sideways"), "sideways");
    }

    @Test
    void theIntegritySecondaryNeedsItsLabel() {
        assertProblem(
                text -> replace(text, "{integrity: 0.5, label: INTEGRITY, credits", "{integrity: 0.5, credits"),
                "has its tracker label");
    }

    @Test
    void aSideNestCrossesTheEdgeInTheField() {
        assertProblem(text -> replace(text, "[[66, 440], [66, 400]]", "[[66, 600], [66, 400]]"), "y=600");
    }

    @Test
    void aDropLiesInTheField() {
        assertProblem(
                text -> replace(text, "{t: 202, x: 240, y: 380,", "{t: 202, x: 240, y: 600,"), "inside the play field");
    }

    @Test
    void theWaterSplashIsAllOrNavalOnly() {
        assertProblem(text -> replace(text, "water_splash: naval-only", "water_splash: some"), "water_splash");
    }
}
