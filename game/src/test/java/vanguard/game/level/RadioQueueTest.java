package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RadioQueueTest {
    private final RadioQueue radio = new RadioQueue();

    @Test
    void linesWrapAtWordsIntoTheSubtitleWidth() {
        List<String> lines = RadioQueue.wrap(
                "Lancer, Rook. Aegis Two's got the north arm, you've got the south. Try not to have all the fun.");

        assertEquals(
                List.of(
                        "Lancer, Rook. Aegis",
                        "Two's got the north",
                        "arm, you've got the",
                        "south. Try not to have",
                        "all the fun."),
                lines);
        assertTrue(lines.stream().allMatch(line -> line.length() <= RadioQueue.LINE_CHARS));
    }

    @Test
    void anUrgentLineInterruptsAndTheInterruptedLinePlaysAgainAfterIt() {
        radio.add("Okafor", "neutral", "Five crawlers, four hundred civilians. Get them to the terminal.", false);
        radio.add("Rook", "neutral", "Aegis Two over Shackleton.", false);
        radio.update(0.01f);
        run(2);
        assertEquals("Okafor", radio.current().orElseThrow().speaker());

        radio.add(
                "Hammer Lead", "generic-cdf", "neutral", "Hammer flight, inbound!", false, RadioQueue.Priority.URGENT);

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f), "at once, without a gap");
        assertEquals("Hammer Lead", radio.current().orElseThrow().speaker());
        run(0.3);
        assertTrue(radio.visibleLines().getFirst().startsWith("Hammer"));
        assertEquals(RadioQueue.Change.CLOSED, run(5.6));
        assertEquals(RadioQueue.Change.OPENED, run(0.6));
        assertEquals("Okafor", radio.current().orElseThrow().speaker(), "the interrupted line plays again");
        assertEquals(1, radio.visibleLines().size(), "from its start");
        run(12);
        assertEquals("Rook", radio.current().orElseThrow().speaker(), "then the queue goes on");
    }

    @Test
    void aMessageOpensTypesPagesAndCloses() {
        radio.add("Rook", "neutral", "Lancer, Rook. Aegis Two's got the north arm, you've got the south.", false);

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        assertEquals(RadioQueue.Change.TYPED, radio.update(0.2f));
        assertEquals(List.of("Lancer"), radio.visibleLines());
        run(5.2);
        assertEquals("Rook", radio.current().orElseThrow().speaker());
        assertEquals(List.of("south."), radio.visibleLines(), "the second page");
        assertEquals(RadioQueue.Change.CLOSED, run(6));
        assertTrue(radio.current().isEmpty());
    }

    @Test
    void theRadioKnowsWhenItsMessageOpenedAndWhenItWillClose() {
        radio.add("Okafor", "neutral", "Weapons free.", false);
        radio.update(0.01f);
        run(0.2);

        assertEquals(0.2f, radio.sinceOpened(), 0.011f);
        assertEquals(Float.POSITIVE_INFINITY, radio.untilClosed(), "still typing");
        run(2);
        float typedOut = 14 / 30f; // the line and its break at 30 characters a second
        assertEquals(RadioQueue.LAST_PAGE_SECONDS - (2.2f - typedOut), radio.untilClosed(), 0.02f);
    }

    @Test
    void queuedMessagesPlayOneAfterTheOther() {
        radio.add("Okafor", "neutral", "Weapons free.", false);
        radio.add("Varga", "neutral", "Keep moving.", false);
        radio.update(0.01f);

        assertEquals(RadioQueue.Change.CLOSED, run(5.6));
        assertEquals(RadioQueue.Change.OPENED, run(1));

        assertEquals("Varga", radio.current().orElseThrow().speaker());
    }

    @Test
    void aTimedLinePlaysBeforeAnEventLineQueuedEarlier() {
        radio.add("Rook", "Rook", "neutral", "They pop like bugs.", false, RadioQueue.Priority.EVENT);
        radio.add("Okafor", "Okafor", "neutral", "Weapons free.", false, RadioQueue.Priority.TIMED);

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        assertEquals("Okafor", radio.current().orElseThrow().speaker());
        run(5.6);
        assertEquals(RadioQueue.Change.OPENED, run(1));
        assertEquals("Rook", radio.current().orElseThrow().speaker(), "the event line in the gap after it");
    }

    @Test
    void anEventLineWaitsForAGapThatEndsBeforeTheNextTimedLine() {
        radio.add("Rook", "Rook", "neutral", "They pop like bugs.", false, RadioQueue.Priority.EVENT);

        assertEquals(RadioQueue.Change.NONE, radio.update(0.01f, 4), "it takes about 5.7 s");
        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f, 7));
    }

    @Test
    void anEventLineThatWaitedTooLongIsDroppedButAClosingLineIsNot() {
        radio.add("Rook", "Rook", "neutral", "They pop like bugs.", false, RadioQueue.Priority.EVENT);
        radio.add("Okafor", "Okafor", "neutral", "Come home, Aegis.", false, RadioQueue.Priority.CLOSING);
        for (int i = 0; i < (RadioQueue.STALE_SECONDS + 1) * 100; i++) {
            radio.update(0.01f, 1);
        }

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        assertEquals("Okafor", radio.current().orElseThrow().speaker(), "the stale event line is gone");
    }

    @Test
    void aClosingLineQueuedOnAFreeRadioGoesAheadOfTheNextTimedLine() {
        // M5 part H, step H9: Level 14's root-grid line 2.5 s before the Choir's timed line.
        radio.add("Okafor", "Okafor", "neutral", "Root grid destroyed.", false, RadioQueue.Priority.CLOSING);
        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f, 2.5f), "it opens at once");
        radio.add("The Choir", "neutral", "[the Choir sings]", true);
        run(2);
        assertEquals("Okafor", radio.current().orElseThrow().speaker(), "the timed line waits for it");
        assertEquals(RadioQueue.Change.CLOSED, run(4), "Okafor's line closes about 5.7 s after it opened");
        assertEquals(RadioQueue.Change.OPENED, run(1));
        assertEquals("The Choir", radio.current().orElseThrow().speaker());

        // Queued while a line plays, it waits for a gap as before.
        radio.clear();
        radio.add("Varga", "neutral", "It's singing.", false);
        radio.update(0.01f, 2.5f);
        radio.add("Okafor", "Okafor", "neutral", "Root grid destroyed.", false, RadioQueue.Priority.CLOSING);
        radio.add("The Choir", "neutral", "[the Choir sings]", true);
        RadioQueue.Change change = RadioQueue.Change.NONE;
        for (int i = 0; i < 700 && change != RadioQueue.Change.OPENED; i++) {
            change = radio.update(0.01f, 1);
        }
        assertEquals("The Choir", radio.current().orElseThrow().speaker(), "no gap: the timed line goes first");
    }

    @Test
    void aBossPhasesSecondLineOutlastsTheFirstButNotTheLongerStaleTime() {
        // M5 part H, step H7b: Okafor's and Rook's lines as the Siege Spire tears loose queue together.
        radio.add(
                "Okafor",
                "Okafor",
                "fierce",
                "It's tearing loose! Stay on it, Lancer!",
                false,
                RadioQueue.Priority.PHASE);
        radio.add(
                "Rook",
                "Rook",
                "neutral",
                "Roots and all. Somebody call a gardener.",
                false,
                RadioQueue.Priority.PHASE);
        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        // Okafor's line holds the radio about 6.4 s, the gap 0.4 s: Rook's waits longer than an event line may.
        assertEquals(RadioQueue.Change.OPENED, run(RadioQueue.STALE_SECONDS + 2));
        assertEquals("Rook", radio.current().orElseThrow().speaker(), "the reply waited out the first line");
        assertTrue(radio.sinceOpened() < 2, "it opened after an event line would have gone stale");

        radio.clear();
        radio.add("Rook", "Rook", "neutral", "Roots and all.", false, RadioQueue.Priority.PHASE);
        for (int i = 0; i < (RadioQueue.PHASE_STALE_SECONDS + 1) * 100; i++) {
            radio.update(0.01f, 1);
        }
        assertTrue(radio.idle(), "a phase line that never found a gap is dropped at last");
    }

    @Test
    void aDroppedLineLeavesTheQueueOrTheRadioIfItHasJustOpened() {
        RadioQueue.Message secondary = radio.add(
                "Okafor",
                "Okafor",
                "neutral",
                "Root grid in the old town destroyed.",
                false,
                RadioQueue.Priority.CLOSING,
                java.util.Optional.empty(),
                0);
        assertFalse(radio.drop(secondary, 1.5f));
        assertTrue(radio.idle(), "dropped while it waits");

        secondary = radio.add(
                "Okafor",
                "Okafor",
                "neutral",
                "Root grid in the old town destroyed.",
                false,
                RadioQueue.Priority.CLOSING,
                java.util.Optional.empty(),
                0);
        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        run(1);
        assertTrue(radio.drop(secondary, 1.5f), "cut on the radio a second after it opened");
        assertTrue(radio.idle());

        secondary = radio.add(
                "Okafor",
                "Okafor",
                "neutral",
                "Root grid in the old town destroyed.",
                false,
                RadioQueue.Priority.CLOSING,
                java.util.Optional.empty(),
                0);
        radio.update(0.01f);
        run(2);
        assertFalse(radio.drop(secondary, 1.5f), "a line well under way plays on");
        assertTrue(radio.current().isPresent());
    }

    /** Updates in 10 ms frames for {@code seconds}; returns the last change that was not NONE. */
    private RadioQueue.Change run(double seconds) {
        RadioQueue.Change last = RadioQueue.Change.NONE;
        for (int i = 0; i < seconds * 100; i++) {
            RadioQueue.Change change = radio.update(0.01f);
            if (change != RadioQueue.Change.NONE && change != RadioQueue.Change.TYPED) {
                last = change;
            }
        }
        return last;
    }
}
