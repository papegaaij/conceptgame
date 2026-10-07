package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;
import vanguard.sim.WingmanSpec;

/**
 * A convoy line names its unit, a line with {@code {side}} Rook's side, and a timed line that
 * requires a special only counts with one fitted.
 */
class RadioScheduleTest {
    @Test
    void aConvoyLineNamesItsUnit() {
        assertEquals("Crawler Three is hit!", RadioSchedule.line("Crawler {ally} is hit!", 2));
        assertEquals("We lost One.", RadioSchedule.line("We lost {ally}.", 0));
        assertEquals("No unit {ally}.", RadioSchedule.line("No unit {ally}.", -1));
    }

    @Test
    void aSideLineNamesRooksSide() {
        // M5 part B: {side} becomes the escort's side setting, so the line finds that side's voiced take.
        assertEquals(
                "Lancer, I'm on your left.",
                RadioSchedule.line("Lancer, I'm on your {side}.", -1, WingmanSpec.Side.LEFT));
        assertEquals(
                "Lancer, I'm on your right.",
                RadioSchedule.line("Lancer, I'm on your {side}.", -1, WingmanSpec.Side.RIGHT));
        assertEquals("Crawler Two is hit!", RadioSchedule.line("Crawler {ally} is hit!", 1, WingmanSpec.Side.RIGHT));
    }

    @Test
    void aTimedLineThatRequiresASpecialIsDueOnlyWithOne() {
        var hammer = new LevelScript.RadioCue(
                LevelScript.CueTrigger.TIME,
                60,
                "",
                "Okafor",
                "Hammer flight is on station.",
                false,
                "neutral",
                "Okafor",
                true,
                0,
                Integer.MAX_VALUE);
        var script = new LevelScript(
                4,
                1,
                0,
                List.of(new LevelScript.Section(100, 120)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(hammer),
                new LevelScript.Secondary(0.8, 50),
                List.of());

        assertEquals(10, new RadioSchedule(script, true).untilTimed(50), 1e-4);
        assertEquals(Float.POSITIVE_INFINITY, new RadioSchedule(script, false).untilTimed(50));
    }
}
