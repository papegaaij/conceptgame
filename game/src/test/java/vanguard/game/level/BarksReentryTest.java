package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.content.WingmenData;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;
import vanguard.sim.WaveSpec;

/**
 * M5 part D (design/player/wingmen, Formations and Radio barks): a re-entry at the bottom edge (a
 * Wraith's rear ambush, a swarm's or a chain's loop-back) is a rear wave from the re-entry, so Rook's
 * rear bark fires 1.5 s before it, at the end of the edge's 3 s warning; a rear ambush wave's own time
 * (its entry at the top) barks nothing.
 */
class BarksReentryTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final WingmenData.Barks DATA = CONTENT.wingmen().barks();

    private final RadioQueue radio = new RadioQueue();
    private final List<Barks.Line> queued = new ArrayList<>();

    private RadioQueue.Message queue(Barks.Line line) {
        queued.add(line);
        return radio.add(
                line.speaker(),
                line.speaker(),
                line.expression(),
                line.text(),
                false,
                line.priority(),
                Optional.empty(),
                0);
    }

    @Test
    void aReentrysBarkFiresHalfwayIntoItsWarning() {
        Barks barks = new Barks(DATA, 10, new double[0], new double[0], new double[0], radio, this::queue);
        int warning = SimStep.ticks(52);
        barks.step(warning - 1, 1, 0);
        assertTrue(barks.reentry(warning));
        int due = warning + SimStep.ticks(Barks.REENTRY_WARNING_SECONDS - 1.5);
        barks.step(due - 1, 1, 0);
        assertEquals(0, queued.size(), "not before 1.5 s ahead of the re-entry");
        barks.step(due, 1, 0);
        assertEquals(1, queued.size());
        assertEquals(Barks.Trigger.REAR_WAVE, queued.getFirst().trigger());
        barks.step(due + 600, 1, 0);
        assertEquals(1, queued.size(), "once");
    }

    @Test
    void aRearWavesOwnWarningIsNotBarkedTwice() {
        double rearWave = 60;
        Barks barks = new Barks(DATA, 10, new double[0], new double[] {rearWave}, new double[0], radio, this::queue);
        assertFalse(barks.reentry(SimStep.ticks(rearWave - 3)), "the wave's own 3 s warning");
        assertTrue(barks.reentry(SimStep.ticks(rearWave + 20)), "a later re-entry");
    }

    @Test
    void aRestartForgetsTheWaitingReentries() {
        Barks barks = new Barks(DATA, 10, new double[0], new double[0], new double[0], radio, this::queue);
        assertTrue(barks.reentry(100));
        barks.reset(0);
        barks.step(1000, 1, 0);
        assertEquals(0, queued.size());
    }

    @Test
    void levelTensRearAmbushesBarkOnlyFromTheirReentry() {
        String key = CONTENT.levelKey(10).orElseThrow();
        LevelScript script = SimSpecs.level(CONTENT, key, Difficulty.MEDIUM);
        double first = script.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.REAR_AMBUSH)
                .mapToDouble(WaveSpec::t)
                .min()
                .orElseThrow();
        Barks barks = Barks.of(DATA, script, 0, radio, this::queue);
        // Its wave time is its cloaked entry at the top: no bark ahead of it.
        int start = SimStep.ticks(first - 3);
        for (int tick = 0; tick <= start; tick++) {
            barks.step(tick, tick * SimStep.SECONDS, 1, 0);
        }
        long before = queued.stream()
                .filter(line -> line.trigger() == Barks.Trigger.REAR_WAVE)
                .count();
        for (int tick = start + 1; tick <= SimStep.ticks(first + 1); tick++) {
            barks.step(tick, tick * SimStep.SECONDS, 1, 0);
        }
        assertEquals(
                before,
                queued.stream()
                        .filter(line -> line.trigger() == Barks.Trigger.REAR_WAVE)
                        .count(),
                "no rear bark for the swoop at " + first + " s");
    }
}
