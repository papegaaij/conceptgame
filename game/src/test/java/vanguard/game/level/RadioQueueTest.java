package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
