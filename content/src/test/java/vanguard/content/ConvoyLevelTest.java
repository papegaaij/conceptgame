package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Intel;
import vanguard.sim.Command;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * A test level with a convoy (Level 04's own data comes with its waves): Level 01's script with an
 * {@code escort} primary, a road, the convoy's radio events, the per-outcome level-end lines and a
 * cue that requires a special, loaded at Level 04's place. It checks the schema, the validator, the
 * specs the simulation gets and the failed-primary-objective flow through the campaign
 * (design/systems/retry).
 */
class ConvoyLevelTest {
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String PATH = "campaign/act-1-first-contact/level-04-tranquility-run/data.yaml";
    private static final String KEY = "act-1-first-contact/level-04-tranquility-run";

    private static final String ESCORT = """
            objectives:
              primary: escort
              escort:
                ally: civilian-crawler
                y: [150, 234, 318, 402, 486]
                credits: 30
                enter: {t: 4, interval: 1, speed: 84}
                hook: {mode: nearest, enemies: [needler]}
                easy: {hp: 90}
                hard: {hp: 1}
            """;
    private static final String ROAD = """

            road:
              width: 56
              texture: road
              points: [[-3, 240], [40, 240], [60, 300], [100, 180], [200, 180]]
            """;
    private static final String RADIO = """
              - {event: level-end, allies: [5, 5], speaker: Okafor, line: "All five. That's what we're for."}
              - {event: level-end, allies: [1, 4], speaker: Okafor, line: "We got most of them home."}
              - {event: first-ally-hit, speaker: Okafor, line: "Crawler {ally} is hit!"}
              - {event: first-ally-lost, speaker: Okafor, line: "We lost {ally}."}
              - {event: mission-failed, speaker: Okafor, expression: grim, line: "The convoy is gone, Lancer. Pull back."}
              - {t: 30, requires: special, speaker: Okafor, line: "Hammer flight is on station."}
            """;

    /** The test level's data: Level 01's with a convoy. */
    static String convoyLevel(String level01) {
        String text = level01.replace("objectives:\n  primary: reach-end\n", ESCORT)
                .replaceFirst("  boss: none\n", "  boss: none\n  objective: ESCORT 5 CRAWLERS\n")
                .replaceFirst("(?m)^  - \\{event: level-end, .*\\n", RADIO.replace("$", "\\$"));
        return text + ROAD;
    }

    private static List<DataFile> files(UnaryOperator<String> edit) {
        List<DataFile> files = new java.util.ArrayList<>(DesignTree.dataFiles());
        String level01 = files.stream()
                .filter(file -> file.path().equals(LEVEL_01))
                .findFirst()
                .orElseThrow()
                .text();
        files.add(new DataFile(PATH, edit.apply(convoyLevel(level01))));
        return files;
    }

    private static Content content() {
        return ContentLoader.load(files(UnaryOperator.identity()));
    }

    @Test
    void theConvoyLevelGivesTheSimulationItsEscortRoadAndConditionalCues() {
        Content content = content();

        LevelScript medium = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        LevelScript.Escort escort = medium.escort().orElseThrow();
        assertEquals(List.of(390.0, 306.0, 222.0, 138.0, 54.0), escort.stations());
        assertEquals(60, escort.ally().hp());
        assertEquals(30, escort.ally().maxHeadingDegrees());
        assertEquals(30, escort.credits());
        assertEquals(List.of("needler"), escort.targetedBy());
        assertEquals(
                90,
                SimSpecs.level(content, KEY, Difficulty.EASY)
                        .escort()
                        .orElseThrow()
                        .ally()
                        .hp());
        assertEquals(
                1,
                SimSpecs.level(content, KEY, Difficulty.HARD)
                        .escort()
                        .orElseThrow()
                        .ally()
                        .hp());
        assertEquals(240, medium.road().orElseThrow().x(0));
        assertEquals(300, medium.road().orElseThrow().x(content.level(KEY).scrollAt(60)), 1e-9);

        List<LevelScript.RadioCue> radio = medium.radio();
        LevelScript.RadioCue allFive = radio.stream()
                .filter(cue -> cue.line().startsWith("All five"))
                .findFirst()
                .orElseThrow();
        assertEquals(LevelScript.CueTrigger.LEVEL_END, allFive.trigger());
        assertEquals(5, allFive.alliesMin());
        assertEquals(5, allFive.alliesMax());
        assertTrue(radio.stream().anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.MISSION_FAILED));
        assertTrue(radio.stream().anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.FIRST_ALLY_LOST));
        assertTrue(radio.stream().anyMatch(LevelScript.RadioCue::requiresSpecial));

        Intel intel = Intel.of(content, KEY, 1);
        assertEquals(Optional.of("ESCORT 5 CRAWLERS"), intel.profile().objective());
        assertTrue(intel.shows(Intel.Field.OBJECTIVE));
        assertFalse(Intel.of(content, KEY, 0).shows(Intel.Field.OBJECTIVE));
    }

    @Test
    void theValidatorChecksTheConvoyAndItsRoad() {
        assertProblem(text -> text.replace("ally: civilian-crawler", "ally: hover-bus"), "unknown ally 'hover-bus'");
        assertProblem(
                text -> text.replace("[[-3, 240], [40, 240]", "[[-3, 460], [40, 460]"),
                "road.points: the ribbon leaves the play field");
        assertProblem(
                text -> text.replace("[[-3, 240], [40, 240], [60, 300]", "[[-3, 240], [40, 240], [40.3, 300]"),
                "road.points: bends");
        assertProblem(text -> text.replace("y: [150, 234,", "y: [150, 200,"), "y[1]: units overlap");
        assertProblem(text -> text.replace("enemies: [needler]", "enemies: [stinger]"), "no 'stinger' in this level");
        assertProblem(text -> text.replace("allies: [1, 4]", "allies: [1, 6]"), "the convoy has 5 units");
        assertProblem(text -> text.replace("requires: special", "requires: wings"), "only 'special' is known");
        assertProblem(text -> text.replace("mode: nearest", "mode: always"), "only 'nearest' is implemented");
        assertProblem(text -> text.replace("  escort:\n", "  convoy:\n"), "an escort primary has its escort block");
    }

    @Test
    void convoyEventsNeedAnEscortObjective() {
        List<DataFile> files = DesignTree.dataFiles().stream()
                .map(file -> file.path().equals(LEVEL_01)
                        ? new DataFile(
                                LEVEL_01,
                                file.text()
                                        .replace(
                                                "radio:  #",
                                                "radio:\n  - {event: mission-failed, speaker: Okafor, line: Gone.}\n  #"))
                        : file)
                .toList();

        ContentException problem = assertThrows(ContentException.class, () -> ContentLoader.load(files));

        assertTrue(
                problem.getMessage().contains("convoy events and allies ranges need an escort objective"),
                problem.getMessage());
    }

    private static void assertProblem(UnaryOperator<String> edit, String expected) {
        ContentException problem = assertThrows(ContentException.class, () -> ContentLoader.load(files(edit)));
        assertTrue(problem.getMessage().contains(expected), problem.getMessage());
    }

    /**
     * The last crawler lost fails the level as a wreck does (design/systems/retry, on a failed
     * primary objective): a retry used on hard, the attempt's earnings lost, the level-start armour
     * back, and a fresh convoy in the next attempt.
     */
    @Test
    void aLostConvoyFailsTheLevelThroughTheCampaignLikeAWreck() {
        Content content = content();
        Campaign campaign = Campaign.start(CampaignRules.of(content), Difficulty.HARD);
        int creditsBefore = campaign.credits();
        double armourBefore = campaign.armour();
        Loadout loadout = SimSpecs.starterLoadout(content, Difficulty.HARD);
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, KEY, Difficulty.HARD),
                // The ship cannot be wrecked, so only the convoy can fail the level here.
                SimSpecs.rules(content, KEY, Difficulty.HARD).withInvulnerableShip(),
                armourBefore);
        campaign.launch();

        Optional<Campaign.Failure> failure = Optional.empty();
        for (int i = 0; i < SimStep.ticks(185) && failure.isEmpty(); i++) {
            sortie.step(Autopilot.commands(sortie));
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.PRIMARY_FAILED) {
                    failure = Optional.of(campaign.fail());
                }
            }
        }

        assertEquals(Optional.of(Campaign.Failure.MISSION_FAILED), failure, "the convoy was lost before the end");
        assertTrue(sortie.flying(), "the ship was not destroyed");
        assertTrue(sortie.credits() > 0, "the attempt had earned credits");
        assertEquals(Optional.of(2), campaign.retriesLeft(), "the failure used a retry on hard");
        assertEquals(creditsBefore, campaign.credits(), "the attempt's earnings are not banked");
        assertEquals(armourBefore, campaign.armour());

        sortie.retry(campaign.armour());
        sortie.step(Command.NONE);

        assertEquals(2, sortie.attempt());
        assertEquals(0, sortie.credits());
        assertEquals(5, sortie.alliesAlive());
        assertFalse(sortie.primaryFailed());
    }
}
