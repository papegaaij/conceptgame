package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.sim.LevelResult;
import vanguard.sim.ScoringRules;

class CampaignTest {
    static final CampaignRules RULES = CampaignRules.of(ContentLoader.fromClasspath());

    private static final LevelResult.Rating RATING = new LevelResult.Rating(
            new LevelResult.Rating.Part(34, 40),
            new LevelResult.Rating.Part(25, 30),
            new LevelResult.Rating.Part(15, 15),
            new LevelResult.Rating.Part(6, 15),
            new ScoringRules.Grade("A+", 85, 0.3));

    /** A won level: 400 credits earned, the grade's bonus and score as given. */
    static LevelResult won(String grade, int gradeBonus, long score) {
        return new LevelResult(
                80,
                95,
                10,
                1,
                1,
                30,
                2,
                true,
                new LevelResult.Credits(300, 50, 0, 0, 50),
                List.of(),
                score,
                RATING,
                new ScoringRules.Grade(grade, 0, 0),
                gradeBonus);
    }

    @Test
    void aNewCampaignStartsAtLevel01WithTheStartingCreditsAndTheStarterLoadout() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);

        assertEquals(1, campaign.nextLevel());
        assertEquals(300, campaign.credits());
        assertEquals(60, campaign.armour());
        assertEquals(60, campaign.maxArmour());
        assertEquals(new Fitted("pulse-cannon", 1), campaign.loadout().get(LoadoutSlot.FRONT));
        assertEquals(new Fitted("Standard", 1), campaign.loadout().get(LoadoutSlot.ARMOUR));
        assertEquals(Optional.empty(), campaign.retriesLeft());
    }

    @Test
    void aWonLevelBanksItsCreditsWithTheGradeBonusAndMovesOn() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);

        boolean best = campaign.complete(won("A", 80, 12_000), 41);

        assertTrue(best);
        assertEquals(300 + 400 + 80, campaign.credits());
        assertEquals(12_000, campaign.score());
        assertEquals(41, campaign.armour(), "armour is not repaired by itself");
        assertEquals(2, campaign.nextLevel());
        assertEquals(Optional.of("A"), campaign.grade(1));
        assertEquals(80, campaign.kills());
    }

    @Test
    void aFailedAttemptLosesWhatItEarnedAndKeepsTheLevelStartState() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);

        assertEquals(Campaign.Failure.MISSION_FAILED, campaign.fail());

        assertEquals(300, campaign.credits(), "nothing is banked from the failed attempt");
        assertEquals(0, campaign.score());
        assertEquals(1, campaign.nextLevel());
        assertEquals(1, campaign.deaths());
    }

    @Test
    void aRetryStartsWithTheLevelStartArmourButAtLeastHalf() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);
        campaign.complete(won("C", 0, 1000), 12);
        assertEquals(12, campaign.armour(), "the next level starts on what was left");

        campaign.fail();

        assertEquals(30, campaign.armour(), "50 % of 60");
    }

    @Test
    void aRetryKeepsAHigherLevelStartArmour() {
        Campaign campaign = Campaign.start(RULES, Difficulty.EASY);
        campaign.complete(won("B", 40, 1000), 50);
        campaign.fail();

        assertEquals(50, campaign.armour());
    }

    @Test
    void hardEndsTheCampaignAtTheFailureAfterThreeRetries() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);
        assertEquals(Optional.of(3), campaign.retriesLeft());

        for (int retry = 1; retry <= 3; retry++) {
            assertEquals(Campaign.Failure.MISSION_FAILED, campaign.fail());
            assertEquals(Optional.of(3 - retry), campaign.retriesLeft(), "the failure uses the retry at once");
        }

        assertFalse(campaign.canRetry());
        assertThrows(IllegalStateException.class, campaign::retry);
        assertEquals(Campaign.Failure.GAME_OVER, campaign.fail());
    }

    @Test
    void aGameOverAutosavesTheHangarBeforeTheLastLaunchWithTheRetriesRenewed(@TempDir Path directory)
            throws IOException {
        Campaign campaign = HangarTest.campaign(Difficulty.HARD, 2, 1000, 12);
        Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);
        hangar.apply(
                LoadoutSlot.FRONT,
                HangarTest.row(hangar, LoadoutSlot.FRONT, "Pulse Cannon", Hangar.State.FITTED),
                Hangar.Action.UPGRADE);
        campaign.launch();
        campaign.fail();
        assertEquals(30, campaign.armour(), "the retry's armour floor");
        // Back to the hangar between attempts: a repair, then the level is launched again.
        Hangar between = new Hangar(HangarTest.CATALOGUE, campaign);
        between.repair(5);
        campaign.launch();
        SaveGame launched = campaign.save(Instant.EPOCH);
        campaign.fail();
        campaign.fail();
        assertEquals(Optional.of(0), campaign.retriesLeft());

        SaveSlots saves = new SaveSlots(directory);
        assertEquals(Campaign.Failure.GAME_OVER, campaign.fail());
        saves.write(SaveSlots.Slot.AUTOSAVE, campaign.save(Instant.parse("2026-10-02T12:00:00Z")));

        Campaign continued = Campaign.load(RULES, saves.mostRecent().orElseThrow());
        assertEquals(2, continued.nextLevel(), "the hangar before the failed level");
        assertEquals(Optional.of(3), continued.retriesLeft(), "a fresh set of retries");
        assertEquals(launched.credits(), continued.credits());
        assertEquals(1000 - 300 - 5 * 10, continued.credits());
        assertEquals(launched.loadout(), continued.loadout());
        assertEquals(35, continued.armour(), "as launched: the floor plus the repair");
        assertEquals(4, continued.deaths(), "the campaign's statistics go on");
    }

    @Test
    void hardRenewsTheRetriesWithTheNextLevel() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);
        campaign.fail();

        campaign.complete(won("B", 40, 1000), 60);

        assertEquals(Optional.of(3), campaign.retriesLeft());
    }

    @Test
    void easyAndMediumRetryWithoutLimit() {
        for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.MEDIUM)) {
            Campaign campaign = Campaign.start(RULES, difficulty);
            for (int i = 0; i < 20; i++) {
                assertEquals(Campaign.Failure.MISSION_FAILED, campaign.fail());
            }
        }
    }

    @Test
    void aFailureUsesItsRetryBeforeThePlayerChooses() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);

        campaign.fail();

        assertEquals(
                Optional.of(2),
                campaign.save(Instant.EPOCH).retriesLeft(),
                "an autosave written now keeps the used retry, so quitting cannot give it back");
    }

    @Test
    void aRestartOrAnAbortUsesARetryOnHard() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);

        assertEquals(60, campaign.retry());

        assertEquals(Optional.of(2), campaign.retriesLeft());
    }

    @Test
    void aWonLevelRecordsItsBankedCreditsAndKillsAndAReplayDoesNot() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);
        campaign.complete(won("A", 80, 1000), 60);

        assertEquals(Optional.of(new SaveGame.LevelStats(480, 80)), campaign.levelStats(1));
        assertEquals(Optional.empty(), campaign.levelStats(2));
        SaveGame save = campaign.save(Instant.EPOCH);
        assertEquals(
                Optional.of(new SaveGame.LevelStats(480, 80)),
                Campaign.load(RULES, save).levelStats(1));

        Campaign replay = Campaign.replay(RULES, SaveSlots.Slot.AUTOSAVE, save, 1);
        replay.complete(won("A+", 120, 1000), 60);
        assertEquals(Optional.of(new SaveGame.LevelStats(480, 80)), replay.levelStats(1));
    }

    @Test
    void onlyABetterGradeIsANewBest() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);
        campaign.complete(won("A", 80, 1000), 60);
        SaveGame save = campaign.save(Instant.EPOCH);
        // Replay level 01 from a save that had it next, with the A already recorded.
        Campaign again = Campaign.load(RULES, withNextLevel(save, 1));

        assertFalse(again.complete(won("B", 40, 1000), 60));
        assertEquals(Optional.of("A"), again.grade(1));
        Campaign third = Campaign.load(RULES, withNextLevel(save, 1));
        assertTrue(third.complete(won("A+", 120, 1000), 60));
    }

    private static SaveGame withNextLevel(SaveGame save, int level) {
        return new SaveGame(
                save.version(),
                save.created(),
                save.playtime(),
                save.difficulty(),
                level,
                save.credits(),
                save.score(),
                save.loadout(),
                save.inventory(),
                save.unlocks(),
                save.specials(),
                save.armour(),
                save.retriesLeft(),
                save.grades(),
                save.dataCores(),
                save.storyFlags(),
                save.stats());
    }

    @Test
    void theStateSurvivesASaveAndALoad() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);
        campaign.fail();
        campaign.play(125.5);
        Instant now = Instant.parse("2026-10-02T12:34:56Z");

        SaveGame save = campaign.save(now);
        Campaign loaded = Campaign.load(RULES, save);

        assertEquals(save, loaded.save(now));
        assertEquals(Optional.of(2), loaded.retriesLeft());
        assertEquals(125.5, loaded.playtime());
    }

    private static final vanguard.content.Content CONTENT = ContentLoader.fromClasspath();

    /** A campaign whose next level is {@code level}. */
    private static Campaign at(int level) {
        return DebugFit.startAt(RULES, Difficulty.MEDIUM, level);
    }

    @Test
    void theFirstAirstrikeChargeIsGivenOnceAtItsUnlockAndFittedIntoAnEmptySlot() {
        assertEquals(List.of(), at(3).giveFreeCharges(CONTENT.specials()));
        Campaign campaign = at(4);

        List<Campaign.FreeCharges> given = campaign.giveFreeCharges(CONTENT.specials());

        assertEquals(List.of(new Campaign.FreeCharges("Airstrike", 1, true)), given);
        assertEquals(1, campaign.gear().charges("Airstrike"));
        assertEquals(new Fitted("Airstrike", 1), campaign.loadout().get(LoadoutSlot.SPECIAL));
        assertEquals(List.of(), campaign.giveFreeCharges(CONTENT.specials()));
        assertEquals(1, campaign.gear().charges("Airstrike"));
        Campaign loaded = Campaign.load(RULES, campaign.save(Instant.EPOCH));
        assertEquals(List.of(), loaded.giveFreeCharges(CONTENT.specials()));
        assertEquals(1, loaded.gear().charges("Airstrike"));
    }

    @Test
    void theFreeChargeLeavesAFittedSpecialInPlace() {
        Campaign campaign = at(7);
        Gear gear = campaign.gear();
        var loadout = new java.util.EnumMap<LoadoutSlot, Fitted>(LoadoutSlot.class);
        loadout.putAll(gear.loadout());
        loadout.put(LoadoutSlot.SPECIAL, new Fitted("Smart Bomb", 1));
        campaign.gear(new Gear(
                gear.credits(),
                loadout,
                gear.inventory(),
                java.util.Map.of("Smart Bomb", 2),
                gear.armour(),
                gear.escort()));

        List<Campaign.FreeCharges> given = campaign.giveFreeCharges(CONTENT.specials());

        assertEquals(
                List.of(
                        new Campaign.FreeCharges("Airstrike", 1, false),
                        new Campaign.FreeCharges("Smart Bomb", 1, false)),
                given);
        assertEquals(new Fitted("Smart Bomb", 1), campaign.loadout().get(LoadoutSlot.SPECIAL));
        assertEquals(1, campaign.gear().charges("Airstrike"));
        assertEquals(3, campaign.gear().charges("Smart Bomb"));
    }

    @Test
    void theFreeSmartBombChargeIsGivenAtItsUnlockAndFittedOnlyIntoAnEmptySlot() {
        Campaign six = at(6);
        List<Campaign.FreeCharges> given = six.giveFreeCharges(CONTENT.specials());

        // The Airstrike's free charge fills the empty slot first; the Smart Bomb's waits in the inventory.
        assertEquals(
                List.of(
                        new Campaign.FreeCharges("Airstrike", 1, true),
                        new Campaign.FreeCharges("Smart Bomb", 1, false)),
                given);
        assertEquals(new Fitted("Airstrike", 1), six.loadout().get(LoadoutSlot.SPECIAL));
        assertEquals(1, six.gear().charges("Smart Bomb"));
        assertEquals(List.of(), six.giveFreeCharges(CONTENT.specials()));
    }

    @Test
    void aDataCoreCollectedInAWonLevelRecordsTheCoreAndItsUnlock() {
        Campaign campaign = at(6);
        campaign.launch();
        var core = new LevelResult.DataCore("settlement-log", "Targeting computer");

        campaign.fail();
        campaign.retry();
        campaign.complete(won("B", 0, 1000).withDataCores(List.of(core)), 40);

        SaveGame save = campaign.save(Instant.EPOCH);
        assertEquals(List.of("settlement-log"), save.dataCores());
        assertTrue(save.unlocks().contains("Targeting computer"));
    }

    @Test
    void aWonLevelAppliesTheChargesUsedAndFoundAFailedOneNone() {
        Campaign campaign = at(4);
        campaign.giveFreeCharges(CONTENT.specials());
        var charges = new java.util.HashMap<>(campaign.gear().specials());
        charges.put("Airstrike", 3);
        Gear gear = campaign.gear();
        campaign.gear(
                new Gear(gear.credits(), gear.loadout(), gear.inventory(), charges, gear.armour(), gear.escort()));
        campaign.launch();

        campaign.fail();
        campaign.retry();
        assertEquals(3, campaign.gear().charges("Airstrike"));
        campaign.complete(won("B", 0, 1000), 40, 2, 1);

        assertEquals(2, campaign.gear().charges("Airstrike"));
    }

    @Test
    void theFlightCarriesTheAirstrikeWithItsCharges() {
        Campaign campaign = at(4);
        campaign.giveFreeCharges(CONTENT.specials());

        Flight flight = Flight.of(CONTENT, Catalogue.of(CONTENT), campaign);

        var special = flight.loadout().special().orElseThrow();
        assertEquals("Airstrike", special.name());
        assertEquals(1, special.charges());
        assertEquals(4, special.maxCharges());
        assertEquals(36, special.airstrike().bombSpacing());
        assertEquals(List.of(), flight.notFlown());
    }
}
