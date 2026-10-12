package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
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

    /**
     * M5 part H (H6 = a): Level 14 ends Act 2: the summary page {@code ACT II COMPLETE} / {@code HOMEFRONT}
     * lists Levels 08–14, seven rows as Act 1's, the totals and no data cores (none of the act's levels
     * hides one).
     */
    @Test
    void theAct2SummaryListsItsSevenMissionsAndNoDataCores() {
        Content content = ContentLoader.fromClasspath();
        List<ActSummary.Mission> missions = new ArrayList<>();
        for (int number = 8; number <= 14; number++) {
            String name =
                    Content.levelName(content.levelKey(number).orElseThrow()).toUpperCase(Locale.ROOT);
            missions.add(new ActSummary.Mission(
                    number, name, Optional.of("A"), Optional.of(new SaveGame.LevelStats(1000, 100))));
        }
        ActSummary summary = new ActSummary("ACT II", "HOMEFRONT", missions, 700, 7000, List.of());

        List<DebriefScreen.Row> rows = DebriefScreen.summaryRows(summary);

        List<String> labels = rows.stream().map(DebriefScreen.Row::label).toList();
        assertEquals("MISSIONS", labels.getFirst());
        assertEquals("08 NEON SKYLINE", labels.get(1));
        assertEquals("14 SIEGE SPIRE", labels.get(7));
        assertEquals("ACT TOTAL", labels.get(8));
        assertEquals("NONE FOUND", labels.getLast());
        assertEquals(
                labels.size(),
                DebriefScreen.summaryRows(new ActSummary("ACT I", "FIRST CONTACT", missions, 700, 7000, List.of()))
                        .size(),
                "as many rows as Act 1's seven, which fit the page");
        assertEquals("7 000 CR", rows.get(8).right(1));
    }

    @Test
    void anActWithoutDataCoresSaysSo() {
        ActSummary summary = new ActSummary("ACT I", "FIRST CONTACT", List.of(), 0, 0, List.of());

        assertEquals("NONE FOUND", DebriefScreen.summaryRows(summary).getLast().label());
    }
}
