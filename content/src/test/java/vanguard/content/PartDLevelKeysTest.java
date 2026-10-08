package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.AllySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;

/**
 * M5 part D's level keys (design/tech/architecture, Data file schemas; Level 10's air escort with its
 * stations, liftoff, climb-out and scripted loss, the radio events {@code ally-lost}, {@code
 * scripted-loss}, {@code first-decloak} and {@code first-loop-back}, the music's {@code duck} and
 * {@code ambience_changes}, and {@code required: [rear]}), loaded strictly and mapped for the
 * simulation, and the evacuation shuttle's ally keys. Level 10's data is written later, so the keys
 * are tried on Level 08's file turned into an air escort level with a Wraith ambush and a looping
 * Mote Swarm in place of its last wave.
 */
class PartDLevelKeysTest {
    private static final String KEY = "act-2-homefront/level-08-neon-skyline";
    private static final String PATH = "campaign/" + KEY + "/data.yaml";

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 08's data has " + what);
        return text.replace(what, with);
    }

    private static final String ESCORT = String.join(
            "\n",
            "  primary: escort",
            "  escort:",
            "    ally: evacuation-shuttle",
            "    credits: 25",
            "    stations:",
            "      - {at: [240, 165], sway: [16, 4], period: 9, phase: 0}",
            "      - {at: [168, 215], sway: [16, 4], period: 11, phase: 0.25}",
            "      - {at: [312, 215], sway: [16, 4], period: 10, phase: 0.5}",
            "      - {at: [168, 270], sway: [16, 4], period: 12, phase: 0.75}",
            "      - {at: [312, 270], sway: [16, 4], period: 8, phase: 0.1}",
            "    liftoff: {t: 1, seconds: 6, pads: [[240, 400], [176, 440], [304, 440], [176, 490], [304, 490]]}",
            "    climb: {t: 196, seconds: 2}",
            "    scripted_loss: {unit: 3, t: 118, glow: 2}",
            "    easy: {hp: 180}",
            "    hard: {hp: 90}",
            "");

    private static final String WAVES = String.join(
            "\n",
            "  - {t: 176.5, formation: rear ambush, enemy: wraith, count: 2, from: rear}",
            "  - t: 179",
            "    formation: swarm",
            "    enemy: mote-swarm",
            "    count: 20",
            "    from: front",
            "    paths: [[[80, -60], [120, 120], [300, 200], [360, 330], [240, 420], [520, 600]]]",
            "    loop_back: {after: 1.5, count: 1}",
            "    hard: {loops: 2}");

    private static final String RADIO = String.join(
            "\n",
            "  - {event: ally-lost, speaker: Okafor, expression: grim, line: \"We lost Lifeline {ally}. Stay on the others.\"}",
            "  - {event: scripted-loss, speaker: Okafor, expression: grim, line: \"Lifeline Three is down.\"}",
            "  - {event: first-decloak, speaker: Varga, line: \"It hid in plain sight!\"}",
            "  - {event: first-loop-back, requires: escort, speaker: Rook, line: \"The flock's turned around.\"}",
            "  - {event: mission-failed, speaker: Okafor, line: \"We've lost the corridor, Lancer. Pull back.\"}",
            "  - {event: level-end, allies: [4, 4], speaker: Okafor, line: \"Four of five made orbit.\"}",
            "  - {event: level-end, allies: [3, 3], speaker: Okafor, line: \"Three shuttles made orbit.\"}",
            "  - {event: level-end, allies: [2, 2], speaker: Okafor, line: \"Two shuttles made orbit.\"}",
            "  - {event: level-end, allies: [1, 1], speaker: Okafor, line: \"One shuttle made orbit.\"}");

    /** Level 08 with every part D level key. */
    private static String withPartD(String text) {
        text = replace(
                text, "  primary: reach-end\n  secondary: {escapes: creeper, credits: 56}", ESCORT.stripTrailing());
        text = replace(
                text,
                "  - {t: 181, formation: snake, enemy: skitter, count: 6, from: front, edge: left, notes: Past the arcology district; the last wave}",
                WAVES);
        text = replace(
                text,
                "  - {event: level-end, speaker: Okafor, line: \"Good flying, both of you. Get some rest. The arcologies are next.\", notes: {trigger: Level end}}\n"
                        + "  - {event: secondary-objective, speaker: Civilian, portrait: generic-civilian, line: \"Shelter nine here. The roofs are quiet. Thank you, Aegis.\", notes: {trigger: Secondary met}}",
                RADIO);
        text = replace(
                text,
                "  end_jingle: mission-complete",
                "  end_jingle: mission-complete\n"
                        + "  duck: {on: scripted-loss, db: -6, seconds: 3}\n"
                        + "  ambience_changes:\n"
                        + "    - {section: 4, ambience: earth-ocean, crossfade: 4}");
        return replace(
                text, "  traits: [anti-ground, forward]", "  traits: [anti-ground, forward, rear]\n  required: [rear]");
    }

    private static final String LOSS = "    scripted_loss: {unit: 3, t: 118, glow: 2}\n";
    private static final String LOSS_LINE =
            "  - {event: scripted-loss, speaker: Okafor, expression: grim, line: \"Lifeline Three is down.\"}\n";
    private static final String DUCK = "  duck: {on: scripted-loss, db: -6, seconds: 3}\n";

    /** Level 08 with every part D level key but the scripted loss (and its radio line and duck). */
    private static String withoutLoss(String text) {
        return replace(replace(replace(withPartD(text), LOSS, ""), LOSS_LINE, ""), DUCK, "");
    }

    private static List<DataFile> files(UnaryOperator<String> edit) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(PATH) ? new DataFile(PATH, edit.apply(f.text())) : f)
                .toList();
    }

    private static Content partD() {
        return ContentLoader.load(files(PartDLevelKeysTest::withPartD));
    }

    @Test
    void theAirEscortLoadsAndMapsForTheSimulation() {
        Content content = partD();
        LevelData level = content.level(KEY);
        LevelData.Escort data = level.objectives().escort().orElseThrow();
        assertTrue(data.air());
        assertEquals(5, data.units());
        assertEquals(4, data.saveable());
        assertEquals(
                new LevelData.Station(new Point(312, 215), new Point(16, 4), 10, 0.5),
                data.stations().orElseThrow().get(2));

        LevelScript medium = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        LevelScript.Escort escort = medium.escort().orElseThrow();
        LevelScript.Air air = escort.air().orElseThrow();
        assertEquals(5, escort.units());
        assertEquals(4, escort.saveable());
        assertEquals(2, escort.scriptedUnit(), "Lifeline Three, from 0");
        assertEquals(25, escort.credits());
        assertEquals(List.of(), escort.targetedBy());
        assertEquals(
                new LevelScript.Station(312, PlayField.HEIGHT - 215, 16, 4, 10, 0.5),
                air.stations().get(2));
        assertEquals(PlayField.HEIGHT - 215.0, escort.stations().get(2), "the stations' heights, y up");
        LevelScript.Liftoff liftoff = air.liftoff().orElseThrow();
        assertEquals(1, liftoff.t());
        assertEquals(6, liftoff.seconds());
        assertEquals(
                new LevelScript.Pad(176, PlayField.HEIGHT - 490), liftoff.pads().get(3));
        assertEquals(Optional.of(new LevelScript.Climb(196, 2)), air.climb());
        assertEquals(Optional.of(new LevelScript.ScriptedLoss(2, 118, 2)), air.scriptedLoss());
        assertTrue(medium.road().isEmpty(), "no road");

        AllySpec shuttle = escort.ally();
        assertEquals("evacuation-shuttle", shuttle.slug());
        assertTrue(shuttle.bullets());
        assertTrue(shuttle.contact());
        assertFalse(shuttle.objectiveAimed());
        assertEquals(0, shuttle.clawsPerSecond());
        assertEquals(5, shuttle.bankFrames());
        assertEquals(20, shuttle.bankFull());
        assertEquals(3, shuttle.glideSeconds());
        assertEquals(48, shuttle.hitbox().width());
        assertEquals(120, shuttle.hp());
        assertEquals(
                180,
                SimSpecs.level(content, KEY, Difficulty.EASY)
                        .escort()
                        .orElseThrow()
                        .ally()
                        .hp());
        assertEquals(
                90,
                SimSpecs.level(content, KEY, Difficulty.HARD)
                        .escort()
                        .orElseThrow()
                        .ally()
                        .hp());
    }

    @Test
    void theRadioEventsMusicKeysAndRequiredRearLoad() {
        Content content = partD();
        LevelData level = content.level(KEY);
        LevelScript medium = SimSpecs.level(content, KEY, Difficulty.MEDIUM);

        for (LevelScript.CueTrigger trigger : List.of(
                LevelScript.CueTrigger.ALLY_LOST,
                LevelScript.CueTrigger.SCRIPTED_LOSS,
                LevelScript.CueTrigger.FIRST_DECLOAK,
                LevelScript.CueTrigger.FIRST_LOOP_BACK)) {
            assertEquals(
                    1,
                    medium.radio().stream()
                            .filter(cue -> cue.trigger() == trigger)
                            .count(),
                    trigger.name());
        }
        assertTrue(LevelScript.CueTrigger.ALLY_LOST.repeats());
        assertFalse(LevelScript.CueTrigger.FIRST_ALLY_LOST.repeats());
        assertTrue(medium.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.FIRST_LOOP_BACK
                        && cue.requires() == LevelScript.RadioCue.FITTED_ESCORT));
        assertTrue(medium.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.LEVEL_END
                        && cue.alliesMin() == 4
                        && cue.alliesMax() == 4));

        LevelData.Music music = level.music();
        assertEquals(Optional.of(new LevelData.Music.Duck(LevelData.Music.DuckOn.SCRIPTED_LOSS, -6, 3)), music.duck());
        assertEquals(List.of(new LevelData.Music.AmbienceChange(4, "earth-ocean", 4)), music.ambienceChangeList());
        assertEquals(List.of("rear"), level.threatProfile().requiredTraits());
    }

    @Test
    void theTypicalHaulPaysTheSaveableShuttlesOnly() {
        TypicalHaul four = TypicalHaul.of(partD(), KEY);
        TypicalHaul five = TypicalHaul.of(ContentLoader.load(files(PartDLevelKeysTest::withoutLoss)), KEY);

        // 25 in Act 1 terms × the credit factor 1.6 = 40 for the fifth shuttle, at the primary's rate 1.0.
        assertEquals(40, five.perfect() - four.perfect());
        assertEquals(40, five.typical() - four.typical(), 1e-9);
    }

    @Test
    void theRealLevelsKeepTheirKeysAndTheShuttlesAllyKeysLoad() {
        Content content = ContentLoader.load(DesignTree.dataFiles());
        AlliesData.Ally shuttle = content.allies().allies().get("evacuation-shuttle");
        assertTrue(shuttle.air());
        assertTrue(shuttle.damagedBy().hitByBullets());
        assertTrue(shuttle.damagedBy().hitByContact());
        assertEquals(Optional.of(new AlliesData.Banks(5, 20)), shuttle.banks());
        assertEquals(Optional.of(3.0), shuttle.glide());
        AlliesData.Ally crawler = content.allies().allies().get("civilian-crawler");
        assertFalse(crawler.air());
        assertFalse(crawler.damagedBy().hitByBullets());

        LevelData level04 = content.level("act-1-first-contact/level-04-tranquility-run");
        LevelData.Escort convoy = level04.objectives().escort().orElseThrow();
        assertFalse(convoy.air());
        assertEquals(5, convoy.units());
        assertEquals(5, convoy.saveable());
        LevelScript.Escort escort = SimSpecs.level(
                        content, "act-1-first-contact/level-04-tranquility-run", Difficulty.MEDIUM)
                .escort()
                .orElseThrow();
        assertTrue(escort.air().isEmpty());
        assertFalse(escort.ally().bullets());
        assertTrue(content.level(KEY).music().duck().isEmpty());
        assertEquals(List.of(), content.level(KEY).music().ambienceChangeList());
    }

    @Test
    void anAirEscortGivesStationsAndNoGroundKeys() {
        assertProblem(
                text -> withPartD(text).replace("    credits: 25\n", "    credits: 25\n    y: [150, 234]\n"),
                "y",
                "stations");
        assertProblem(
                text -> withPartD(text)
                        .replace(
                                "    credits: 25\n",
                                "    credits: 25\n    hook: {mode: nearest, enemies: [needler]}\n"),
                "hook");
        assertProblem(
                text -> withPartD(text)
                        .replace("    credits: 25\n", "    credits: 25\n    enter: {t: 4, interval: 1, speed: 84}\n"),
                "enter");
        assertProblem(
                text -> withPartD(text).replace("    ally: evacuation-shuttle", "    ally: civilian-crawler"),
                "follows the road");
        assertProblem(text -> withPartD(text).replace("phase: 0.1}", "phase: 0.1, drift: 3}"), "unknown");
        assertProblem(text -> withPartD(text).replace("[304, 490]]}", "[304, 490], [240, 520]]}"), "pads");
        assertProblem(text -> withPartD(text).replace("scripted_loss: {unit: 3", "scripted_loss: {unit: 6"), "unit");
    }

    @Test
    void theStationsKeepTheirHitBoxesApartAndOnTheScreen() {
        assertProblem(
                text -> withPartD(text).replace("{at: [168, 270], sway: [16, 4]", "{at: [168, 230], sway: [16, 4]"),
                "stations[3]",
                "overlaps station 1");
        assertProblem(
                text -> withPartD(text).replace("{at: [240, 165], sway: [16, 4]", "{at: [10, 165], sway: [16, 4]"),
                "stations[0]",
                "leaves the play field");
    }

    @Test
    void theTimesFollowEachOther() {
        assertProblem(
                text -> withPartD(text).replace("scripted_loss: {unit: 3, t: 118", "scripted_loss: {unit: 3, t: 6"),
                "glow starts before the liftoff ends");
        assertProblem(
                text -> withPartD(text).replace("climb: {t: 196", "climb: {t: 100"), "climb.t", "after the liftoff");
        assertProblem(text -> withPartD(text).replace("climb: {t: 196", "climb: {t: 199"), "climb.t", "level end");
    }

    @Test
    void theEventsAndTheDuckNeedWhatTheyName() {
        assertProblem(
                text -> withoutLoss(text).replace("  - {event: first-decloak", LOSS_LINE + "  - {event: first-decloak"),
                "radio",
                "scripted-loss in a level without a scripted loss");
        assertProblem(
                text -> withoutLoss(text).replace("  ambience_changes:", DUCK + "  ambience_changes:"), "music.duck");
        assertProblem(
                text -> withPartD(text)
                        .replace("  - {t: 176.5, formation: rear ambush, enemy: wraith, count: 2, from: rear}\n", ""),
                "first-decloak");
        assertProblem(
                text -> withPartD(text).replace("    loop_back: {after: 1.5, count: 1}\n    hard: {loops: 2}", ""),
                "first-loop-back");
        assertProblem(text -> withPartD(text).replace("{section: 4, ambience", "{section: 9, ambience"), "sections");
        assertProblem(text -> withPartD(text).replace("db: -6", "db: 2"), "db");
        assertProblem(text -> withPartD(text).replace("allies: [4, 4]", "allies: [5, 5]"), "4 saveable units");
    }

    @Test
    void requiredRearIsOneOfTheTraits() {
        assertProblem(
                text -> withPartD(text)
                        .replace("  traits: [anti-ground, forward, rear]", "  traits: [anti-ground, forward]"),
                "required",
                "rear");
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
