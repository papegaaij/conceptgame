package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;
import vanguard.sim.WaveSpec;

/**
 * M5 part C's level keys (design/tech/architecture, Data file schemas; Level 09's hold zones,
 * collapse, wave tag, scoped secondary, radio events and {@code requires: escort}, music
 * {@code full_on} and the threat profile's {@code required}), loaded strictly and mapped for the
 * simulation. Level 09's data is written later, so the keys are tried on Level 08's file turned
 * into a destroy-targets level (its two turret nests as the groups {@code Pad} and {@code Roof}).
 */
class PartCLevelKeysTest {
    private static final String KEY = "act-2-homefront/level-08-neon-skyline";
    private static final String PATH = "campaign/" + KEY + "/data.yaml";

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 08's data has " + what);
        return text.replace(what, with);
    }

    /** Level 08 with every part C key. */
    private static String withPartC(String text) {
        text = replace(
                text,
                "  primary: reach-end\n  secondary: {escapes: creeper, credits: 56}",
                "  primary: destroy-targets\n  targets: [Pad, Roof]\n  secondary: {escapes: creeper, credits: 56, tag: roofs}");
        text = replace(text, "    at: [[44, 300]", "    group: Pad\n    at: [[44, 300]");
        text = replace(text, "    at: [[88, 60]", "    group: Roof\n    at: [[88, 60]");
        text = replace(
                text,
                "    paths: [[[330, -40], [330, -150], [260, -240], [260, -800]]]",
                "    tag: roofs\n    paths: [[[330, -40], [330, -150], [260, -240], [260, -800]]]");
        text = replace(text, "{t: 8.5, speaker: Rook,", "{t: 8.5, requires: escort, speaker: Rook,");
        text = replace(
                text,
                "  - {t: 1, speaker: Okafor,",
                "  - {event: hold-start, speaker: Okafor, line: \"Slowing you down.\"}\n"
                        + "  - {event: collapse, requires: escort, speaker: Rook, line: \"Timber.\"}\n"
                        + "  - {t: 1, speaker: Okafor,");
        text = replace(
                text, "  end_jingle: mission-complete", "  end_jingle: mission-complete\n  full_on: [hold, collapse]");
        text = replace(
                text,
                "  traits: [anti-ground, forward]",
                "  traits: [anti-ground, forward]\n  required: [anti-ground]");
        return text
                + "\nholds:\n"
                + "  - {groups: [Pad], y: 200, speed: 30, easy: {speed: 20}, hard: {speed: 40}, ramp: 1}\n"
                + "  - {groups: [Roof], y: 200, speed: 30, ramp: 1}\n"
                + COLLAPSE;
    }

    private static final String COLLAPSE = "collapse: {groups: [Roof], warning: 1.5, drop: 1.5,"
            + " blast: {seconds: 1, from: 100, to: 390}, tower: arcology,"
            + " dust: {atmosphere: heavy, seconds: 6}, rubble: cross-highway}\n";

    private static List<DataFile> files(UnaryOperator<String> edit) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(PATH) ? new DataFile(PATH, edit.apply(f.text())) : f)
                .toList();
    }

    @Test
    void thePartCKeysLoadAndMapForTheSimulation() {
        Content content = ContentLoader.load(files(PartCLevelKeysTest::withPartC));
        LevelData level = content.level(KEY);
        LevelScript medium = SimSpecs.level(content, KEY, Difficulty.MEDIUM);

        assertEquals(
                List.of(new LevelScript.Hold(List.of(0), 200, 30, 1), new LevelScript.Hold(List.of(1), 200, 30, 1)),
                medium.holds());
        assertEquals(
                20,
                SimSpecs.level(content, KEY, Difficulty.EASY).holds().getFirst().speed());
        assertEquals(
                40,
                SimSpecs.level(content, KEY, Difficulty.HARD).holds().getFirst().speed());
        assertEquals(
                30,
                SimSpecs.level(content, KEY, Difficulty.HARD).holds().getLast().speed());
        // The band: the footprint of Level 08's arcology (placed once, 180 px deep) on the ground.
        BackdropData.PlacedPiece arcology = level.backdrop().placements().stream()
                .filter(p -> p.piece().equals("arcology"))
                .findFirst()
                .orElseThrow();
        double centre = level.pieceCentre(arcology);
        double half = level.backdrop().pieces().get("arcology").size().height() / 2;
        assertEquals(
                Optional.of(new LevelScript.Collapse(
                        List.of(1), 1.5, 1.5, 1, 100, 390, 6, arcology.x(), centre - half, centre + half)),
                medium.collapse());
        LevelData.Collapse collapse = level.collapse().orElseThrow();
        assertEquals(new LevelData.Dust(LevelData.Atmosphere.HEAVY, 6), collapse.dust());
        assertEquals("cross-highway", collapse.rubble());

        assertEquals("roofs", medium.secondary().tag());
        List<WaveSpec> tagged =
                medium.waves().stream().filter(w -> w.tag().equals("roofs")).toList();
        assertEquals(1, tagged.size());
        assertEquals("creeper", tagged.getFirst().enemy().slug());

        assertTrue(medium.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.HOLD_START && cue.requires() == 0));
        assertTrue(medium.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.COLLAPSE
                        && cue.requires() == LevelScript.RadioCue.FITTED_ESCORT));
        assertTrue(medium.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.TIME
                        && cue.t() == 8.5
                        && cue.requires() == LevelScript.RadioCue.FITTED_ESCORT));

        assertTrue(level.music().fullOn(LevelData.Music.FullOn.HOLD));
        assertTrue(level.music().fullOn(LevelData.Music.FullOn.COLLAPSE));
        assertEquals(List.of("anti-ground"), level.threatProfile().requiredTraits());
    }

    @Test
    void theRealLevelsHaveNoneOfThem() {
        Content content = ContentLoader.load(DesignTree.dataFiles());
        LevelData level = content.level(KEY);

        assertEquals(List.of(), SimSpecs.level(content, KEY, Difficulty.MEDIUM).holds());
        assertTrue(level.collapse().isEmpty());
        assertTrue(!level.music().fullOn(LevelData.Music.FullOn.HOLD));
        assertEquals(List.of(), level.threatProfile().requiredTraits());
    }

    @Test
    void anUnknownHoldFieldIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("speed: 30, ramp: 1}", "speed: 30, ramp: 1, timeout: 15}"),
                "holds",
                "unknown field");
    }

    @Test
    void aHoldOfAnUnknownGroupIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("  - {groups: [Roof], y: 200", "  - {groups: [Rooof], y: 200"),
                "holds[1].groups",
                "no group 'Rooof'");
    }

    @Test
    void aGroupInTwoHoldsIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("  - {groups: [Roof], y: 200", "  - {groups: [Pad, Roof], y: 200"),
                "holds[1].groups",
                "in another hold");
    }

    @Test
    void aHoldNoSlowerThanItsSectionsIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("hard: {speed: 40}", "hard: {speed: 400}"),
                "holds[0].speed",
                "on hard");
    }

    @Test
    void aCollapseLeavingAnUnknownPieceIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("rubble: cross-highway", "rubble: rubble-heap"),
                "collapse.rubble",
                "rubble-heap");
    }

    @Test
    void theOldSweepKeyAndAShrinkingBlastAreRejected() {
        assertProblem(text -> withPartC(text).replace("drop: 1.5,", "drop: 1.5, sweep: 3,"), "sweep");
        assertProblem(text -> withPartC(text).replace("from: 100, to: 390", "from: 390, to: 100"), "collapse", "grows");
    }

    @Test
    void aCollapseWithoutATowerPlacedOnceIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("tower: arcology,", "tower: cross-highway,"),
                "collapse.tower",
                "no backdrop tower piece 'cross-highway'");
        assertProblem(
                text -> withPartC(text).replace("tower: arcology,", "tower: tower-56x56-h060-a,"),
                "collapse.tower",
                "not once");
    }

    @Test
    void aScopedSecondaryWithoutItsTaggedWavesIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("credits: 56, tag: roofs}", "credits: 56, tag: bridge}"),
                "objectives.secondary.tag",
                "tagged 'bridge'");
    }

    @Test
    void aRequiredTraitThatIsNotRecommendedIsRejected() {
        assertProblem(
                text -> withPartC(text).replace("required: [anti-ground]", "required: [area]"), "required", "'area'");
    }

    @Test
    void anUnknownRequirementIsRejected() {
        assertProblem(
                text -> withPartC(text)
                        .replace(
                                "requires: escort, speaker: Rook, line: \"Timber",
                                "requires: rook, speaker: Rook, line: \"Timber"),
                "requires",
                "'escort'");
    }

    @Test
    void aFirstPounceCueWithoutAnEnemyThatPouncesIsRejected() {
        assertProblem(
                text -> withPartC(text)
                        .replace("{event: hold-start, speaker: Okafor", "{event: first-pounce, speaker: Okafor"),
                "radio[0].event",
                "first-pounce");
    }

    @Test
    void aFullMixOnAnEventTheLevelLacksIsRejected() {
        assertProblem(
                text -> withPartC(text)
                        .replace(COLLAPSE, "")
                        .replace("  - {event: collapse, requires: escort, speaker: Rook, line: \"Timber.\"}\n", ""),
                "music.full_on",
                "collapse");
    }

    @Test
    void aPackWithoutAPathPerUnitIsRejected() {
        assertProblem(
                text -> withPartC(text)
                        .replace(
                                "    formation: single\n    enemy: creeper\n    count: 1\n    from: front\n    paths: [[[150, -40]",
                                "    formation: pack\n    enemy: creeper\n    count: 2\n    from: front\n    paths: [[[150, -40]"),
                "paths",
                "a path per unit");
    }

    /** Loads the design tree with Level 08 edited; exactly one problem is reported, containing every part. */
    private static void assertProblem(UnaryOperator<String> edit, String... parts) {
        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files(edit)));

        assertEquals(1, e.problems().size(), e.getMessage());
        for (String part : parts) {
            assertTrue(
                    e.problems().getFirst().contains(part),
                    "'" + part + "' in " + e.problems().getFirst());
        }
    }
}
