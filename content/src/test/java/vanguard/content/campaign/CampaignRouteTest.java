package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;

class CampaignRouteTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void aNewGameBriefsThenLaunchesLevel01FromTheHangar() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);

        var step =
                assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));

        assertEquals(1, step.script().mission());
        assertEquals(
                Optional.of("act-1-first-contact/level-01-break-at-dawn"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel01TheBriefingOfLevel02Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(
                Optional.of("act-1-first-contact/level-02-shipyard-burning"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel02TheBriefingOfLevel03Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(Optional.of("act-1-first-contact/level-03-spore-drift"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel03TheBriefingOfLevel04Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(
                Optional.of("act-1-first-contact/level-04-tranquility-run"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel04TheBriefingOfLevel05Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 4; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(Optional.of("act-1-first-contact/level-05-crater-nest"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel05TheBriefingOfLevel06Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 5; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(Optional.of("act-1-first-contact/level-06-farside"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel06TheBriefingOfLevel07Comes() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 6; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(
                Optional.of("act-1-first-contact/level-07-brood-carrier"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel07TheActOutroComesThenTheAct2IntroAndTheBriefingOfLevel08() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 7; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        var outro = assertInstanceOf(CampaignRoute.Step.Outro.class, CampaignRoute.afterLevel(content, campaign));
        assertEquals("act-1-first-contact", outro.script().actDirectory());
        assertEquals(7, outro.script().last());
        assertEquals(8, outro.script().nextLevel());
        assertEquals(4, outro.script().pages().size());
        // After the outro: Act 2's title card and act briefing, then Level 08's pages, then its hangar.
        var briefing =
                assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(8, briefing.script().mission());
        assertEquals("HOMEFRONT", briefing.script().titleCard().orElseThrow().name());
        assertEquals(Optional.of("act-2-homefront/level-08-neon-skyline"), CampaignRoute.launch(content, campaign));
    }

    @Test
    void afterLevel08TheHangarBeforeLevel09WaitsForItsData() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 8; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        // Level 09 has no data yet: the hangar before it, which cannot launch.
        assertInstanceOf(CampaignRoute.Step.Hangar.class, CampaignRoute.afterLevel(content, campaign));
        assertInstanceOf(CampaignRoute.Step.Hangar.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(Optional.empty(), CampaignRoute.launch(content, campaign));
    }

    @Test
    void aLevelInsideTheActEndsNoAct() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 6; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }

        assertEquals(Optional.empty(), CampaignRoute.actEnd(content, campaign));
        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.afterLevel(content, campaign), "Level 07's");
    }

    @Test
    void aReplayOfTheActsLastLevelHasNoOutro() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 7; i++) {
            campaign.complete(CampaignTest.won("A", 80, 1000), 60);
        }
        Campaign replay =
                Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, campaign.save(java.time.Instant.EPOCH), 7);
        replay.complete(CampaignTest.won("A+", 80, 1000), 60);

        assertEquals(Optional.empty(), CampaignRoute.actEnd(content, replay));
        assertFalse(CampaignRoute.afterLevel(content, replay) instanceof CampaignRoute.Step.Outro);
    }

    @Test
    void theActEndDebugOptionEndsTheActAfterTheLevelStart() {
        Campaign campaign = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 6);
        campaign.debugActEnd();
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        var outro = assertInstanceOf(CampaignRoute.Step.Outro.class, CampaignRoute.afterLevel(content, campaign));
        assertEquals(6, outro.script().last());
        assertEquals(7, outro.script().nextLevel());
    }

    @Test
    void aFailedLevelIsLaunchedAgainFromTheHangar() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.HARD);
        campaign.fail();
        campaign.retry();

        assertEquals(
                Optional.of("act-1-first-contact/level-01-break-at-dawn"), CampaignRoute.launch(content, campaign));
    }
}
