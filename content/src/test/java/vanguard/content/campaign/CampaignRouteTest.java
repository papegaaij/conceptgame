package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void afterLevel01TheCampaignWaitsInTheHangarUntilLevel02IsBuilt() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        assertInstanceOf(CampaignRoute.Step.Hangar.class, CampaignRoute.beforeNextLevel(content, campaign));
        assertEquals(Optional.empty(), CampaignRoute.launch(content, campaign));
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
