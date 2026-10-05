package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.sim.LevelResult;

class ActSummaryTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theSummaryListsEveryLevelOfTheActWithItsCreditsAndKillsAndTheTotals() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 7; i++) {
            campaign.complete(CampaignTest.won(i == 2 ? "A+" : "A", 80, 1000), 60);
        }

        ActSummary summary = ActSummary.of(
                content, campaign, CampaignRoute.actEnd(content, campaign).orElseThrow());

        assertEquals("ACT I", summary.act());
        assertEquals("FIRST CONTACT", summary.name());
        assertEquals(7, summary.missions().size());
        ActSummary.Mission first = summary.missions().getFirst();
        assertEquals(1, first.number());
        assertEquals("BREAK AT DAWN", first.name());
        assertEquals(Optional.of(new SaveGame.LevelStats(480, 80)), first.stats());
        assertEquals(Optional.of("A+"), summary.missions().get(2).grade());
        assertEquals(7 * 80, summary.kills());
        assertEquals(7 * 480, summary.credits());
        assertEquals(List.of(), summary.dataCores());
    }

    @Test
    void theDataCoresFoundInTheActShowWithTheirLevel() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        for (int i = 0; i < 7; i++) {
            LevelResult won = CampaignTest.won("A", 80, 1000);
            if (i == 5) {
                won = won.withDataCores(List.of(new LevelResult.DataCore("settlement log", "Targeting computer")));
            }
            campaign.complete(won, 60);
        }

        ActSummary summary = ActSummary.of(
                content, campaign, CampaignRoute.actEnd(content, campaign).orElseThrow());

        assertEquals(List.of(new ActSummary.DataCore("SETTLEMENT LOG", 6, "FARSIDE")), summary.dataCores());
    }

    /** A save of format version 1 recorded nothing per level: those levels show no stats and add nothing. */
    @Test
    void levelsWonBeforeTheStatsWereRecordedShowNone() {
        Campaign start = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 7);
        Campaign campaign = Campaign.load(CampaignTest.RULES, start.save(Instant.EPOCH));
        campaign.complete(CampaignTest.won("A", 80, 1000), 60);

        ActSummary summary = ActSummary.of(
                content, campaign, CampaignRoute.actEnd(content, campaign).orElseThrow());

        assertEquals(Optional.empty(), summary.missions().getFirst().stats());
        assertEquals(
                Optional.of(new SaveGame.LevelStats(480, 80)),
                summary.missions().getLast().stats());
        assertEquals(80, summary.kills());
        assertEquals(480, summary.credits());
    }
}
