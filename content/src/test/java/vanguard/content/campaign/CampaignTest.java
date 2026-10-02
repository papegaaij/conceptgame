package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.sim.LevelResult;
import vanguard.sim.ScoringRules;

class CampaignTest {
    static final CampaignRules RULES = CampaignRules.of(ContentLoader.fromClasspath());

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
                80,
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
        campaign.retry();

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

        assertEquals(30, campaign.retry(), "50 % of 60");
        assertEquals(30, campaign.armour());
    }

    @Test
    void aRetryKeepsAHigherLevelStartArmour() {
        Campaign campaign = Campaign.start(RULES, Difficulty.EASY);
        campaign.complete(won("B", 40, 1000), 50);
        campaign.fail();

        assertEquals(50, campaign.retry());
    }

    @Test
    void hardEndsTheCampaignAtTheFailureAfterThreeRetries() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);
        assertEquals(Optional.of(3), campaign.retriesLeft());

        for (int retry = 1; retry <= 3; retry++) {
            assertEquals(Campaign.Failure.MISSION_FAILED, campaign.fail());
            campaign.retry();
            assertEquals(Optional.of(3 - retry), campaign.retriesLeft());
        }

        assertFalse(campaign.canRetry());
        assertEquals(Campaign.Failure.GAME_OVER, campaign.fail());
        assertThrows(IllegalStateException.class, campaign::retry);
    }

    @Test
    void hardRenewsTheRetriesWithTheNextLevel() {
        Campaign campaign = Campaign.start(RULES, Difficulty.HARD);
        campaign.fail();
        campaign.retry();

        campaign.complete(won("B", 40, 1000), 60);

        assertEquals(Optional.of(3), campaign.retriesLeft());
    }

    @Test
    void easyAndMediumRetryWithoutLimit() {
        for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.MEDIUM)) {
            Campaign campaign = Campaign.start(RULES, difficulty);
            for (int i = 0; i < 20; i++) {
                assertEquals(Campaign.Failure.MISSION_FAILED, campaign.fail());
                campaign.retry();
            }
        }
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
        assertTrue(third.complete(won("S", 120, 1000), 60));
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
        campaign.retry();
        campaign.play(125.5);
        Instant now = Instant.parse("2026-10-02T12:34:56Z");

        SaveGame save = campaign.save(now);
        Campaign loaded = Campaign.load(RULES, save);

        assertEquals(save, loaded.save(now));
        assertEquals(Optional.of(2), loaded.retriesLeft());
        assertEquals(125.5, loaded.playtime());
    }
}
