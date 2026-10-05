package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.campaign.ActSummary;
import vanguard.content.campaign.SaveGame;
import vanguard.sim.LevelResult;

/** The debrief's BOSS TIME row and the act summary's page (design/ui/debrief). */
class DebriefActEndTest {
    @Test
    void theBossTimeShowsTheKillTimeAgainstThePar() {
        assertEquals("1:42 (PAR 2:30)", DebriefScreen.bossTime(new LevelResult.BossTime(150, 102.9)));
        assertEquals("0:59 (PAR 1:00)", DebriefScreen.bossTime(new LevelResult.BossTime(60, 59.99)));
        assertEquals("NOT KILLED (PAR 1:00)", DebriefScreen.bossTime(new LevelResult.BossTime(60, -1)));
    }

    @Test
    void theActSummaryListsTheLevelsTheTotalsAndTheDataCores() {
        ActSummary summary = new ActSummary(
                "ACT I",
                "FIRST CONTACT",
                List.of(
                        new ActSummary.Mission(1, "BREAK AT DAWN", Optional.of("A"), Optional.empty()),
                        new ActSummary.Mission(
                                6, "FARSIDE", Optional.of("A+"), Optional.of(new SaveGame.LevelStats(1051, 212)))),
                212,
                1051,
                List.of(new ActSummary.DataCore("SETTLEMENT LOG", 6, "FARSIDE")));

        List<DebriefScreen.Row> rows = DebriefScreen.summaryRows(summary);

        assertEquals(
                List.of(
                        "MISSIONS",
                        "01 BREAK AT DAWN",
                        "06 FARSIDE",
                        "ACT TOTAL",
                        "1 OF 2 MISSIONS RECORDED",
                        "DATA CORES",
                        "SETTLEMENT LOG"),
                rows.stream().map(DebriefScreen.Row::label).toList());
        assertEquals("--  A", rows.get(1).middle());
        assertEquals("", rows.get(1).right(1));
        assertEquals("212 KILLS  A+", rows.get(2).middle());
        assertEquals("1 051 CR", rows.get(2).right(1));
        assertEquals("1 051 CR", rows.get(3).right(1));
        assertEquals("L06 FARSIDE", rows.get(6).middle());
    }

    @Test
    void anActWithoutDataCoresSaysSo() {
        ActSummary summary = new ActSummary("ACT I", "FIRST CONTACT", List.of(), 0, 0, List.of());

        assertEquals("NONE FOUND", DebriefScreen.summaryRows(summary).getLast().label());
    }
}
