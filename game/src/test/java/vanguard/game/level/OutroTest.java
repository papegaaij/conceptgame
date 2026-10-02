package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.LevelData;

/** The debrief waits for the radio after a won level, at most {@link LevelData#OUTRO_SECONDS}. */
class OutroTest {
    private static final float FRAME = 1 / 60f;
    /** Level 01's level-end line: 3 lines, 60 characters with the line breaks, 2 s at 30 per second. */
    private static final String LEVEL_END = "Good work, Aegis. That was the scouts. The rest are coming.";

    private static final String SECONDARY = "Clean sweep. I'll make sure High Command hears about it.";

    private final RadioQueue radio = new RadioQueue();
    private final Outro outro = new Outro();

    @Test
    void nothingHappensWhileTheLevelIsFlown() {
        assertFalse(outro.update(FRAME, radio));
    }

    @Test
    void theDebriefWaitsUntilTheLevelEndLineHasBeenShown() {
        outro.start();
        radio.add("Okafor", "neutral", LEVEL_END, false);

        assertEquals(2 + RadioQueue.LAST_PAGE_SECONDS, secondsUntilDebrief(), 2 * FRAME);
    }

    @Test
    void aQueuedLineIsShownToo() {
        radio.add("Okafor", "neutral", SECONDARY, false);
        radio.update(FRAME);
        run(5);
        outro.start();
        radio.add("Okafor", "neutral", LEVEL_END, false);

        // What is left of the secondary-objective line (57 characters typed in 1.9 s, then held),
        // the gap and the level-end line.
        double left = 1.9 + RadioQueue.LAST_PAGE_SECONDS - 5;
        assertEquals(left + RadioQueue.GAP_SECONDS + 2 + RadioQueue.LAST_PAGE_SECONDS, secondsUntilDebrief(), 0.1);
    }

    @Test
    void bothEndOfLevelLinesFitInTheCap() {
        outro.start();
        radio.add("Okafor", "neutral", LEVEL_END, false);
        radio.add("Okafor", "neutral", SECONDARY, false);

        assertTrue(secondsUntilDebrief() < LevelData.OUTRO_SECONDS, "the cap leaves room for both lines");
    }

    @Test
    void theRadioHoldsTheDebriefAtMostTheCap() {
        outro.start();
        radio.add("Okafor", "neutral", LEVEL_END, false);
        radio.add("Okafor", "neutral", SECONDARY, false);
        radio.add("Okafor", "neutral", LEVEL_END, false);

        assertEquals(LevelData.OUTRO_SECONDS, secondsUntilDebrief(), 2 * FRAME);
    }

    @Test
    void withAQuietRadioTheDebriefFollowsAtOnce() {
        outro.start();

        assertTrue(outro.update(FRAME, radio));
    }

    @Test
    void aRestartStopsIt() {
        outro.start();
        outro.stop();

        assertFalse(outro.update(FRAME, radio));
    }

    /** Runs frames in the level screen's order (the outro, then the radio) until the debrief takes over. */
    private double secondsUntilDebrief() {
        double seconds = 0;
        while (!outro.update(FRAME, radio)) {
            radio.update(FRAME);
            seconds += FRAME;
        }
        return seconds;
    }

    private void run(double seconds) {
        for (double t = 0; t < seconds; t += FRAME) {
            radio.update(FRAME);
        }
    }
}
