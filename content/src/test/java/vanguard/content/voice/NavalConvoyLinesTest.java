package vanguard.content.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

/** M5 part E: a naval convoy's {@code {ally}} lines get one take per ship name, not per number word. */
class NavalConvoyLinesTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String LEVEL_11 = "act-2-homefront/level-11-atlantic-convoy";

    @Test
    void everyCargoShipGetsItsOwnTakeAndTheFrigateNone() {
        List<String> spoken = VoiceLines.radio(
                        CONTENT.voices(), CONTENT.levels().get(LEVEL_11), LEVEL_11)
                .stream()
                .map(VoiceLines.VoiceLine::text)
                .toList();
        for (String ship : List.of("Halvorsen", "Mbeki", "Saint-Laurent")) {
            assertTrue(spoken.contains("The " + ship + " is hit! Taking on water, but holding!"), ship + " hit");
            assertTrue(
                    spoken.contains("We've lost the " + ship + ". Crew in the water. Ruyter is picking them up."),
                    ship + " lost");
        }
        assertEquals(0, spoken.stream().filter(text -> text.contains("{ally}")).count());
        assertEquals(
                0,
                spoken.stream()
                        .filter(text -> text.contains("The Ruyter is hit"))
                        .count());
    }

    /** Atlas Control was cast in concept round 33 (b, MaryAnn): her ten takes are in the line list. */
    @Test
    void castAtlasControlHasHerTenLines() {
        assertEquals(
                10,
                VoiceLines.radio(CONTENT.voices(), CONTENT.levels().get(LEVEL_11), LEVEL_11).stream()
                        .filter(line -> line.speaker().equals("Atlas Control"))
                        .count());
    }
}
