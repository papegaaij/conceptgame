package vanguard.content.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Expression;
import vanguard.content.LevelData;

/**
 * Every spoken line has its rendered file in assets/voice (design/audio/voice, Build and CI): a
 * changed line without a render fails with the list of missing lines. The check is switched on per
 * level ({@link #RENDERED}); the lines of the other levels are only listed. Render with
 * {@code python3 tools/art/voice.py}.
 */
class VoiceFilesTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    /** The levels whose lines must have their files, and the acts whose briefings must. */
    private static final Set<String> RENDERED = Set.of(
            "act-1-first-contact/level-01-break-at-dawn",
            "act-1-first-contact/level-02-shipyard-burning",
            "act-1-first-contact/level-03-spore-drift",
            "act-1-first-contact/level-04-tranquility-run",
            "act-1-first-contact/level-05-crater-nest",
            "act-1-first-contact briefing",
            "specials");

    private static boolean rendered(VoiceLines.VoiceLine line) {
        return RENDERED.stream().anyMatch(where -> line.source().startsWith(where));
    }

    @Test
    void everyLineOfTheRenderedLevelsHasItsVoiceFile() {
        List<VoiceLines.VoiceLine> lines = VoiceLines.all(CONTENT);
        String missing = lines.stream()
                .filter(VoiceFilesTest::rendered)
                .filter(line -> !Files.isRegularFile(ASSETS.resolve(line.path())))
                .map(line -> line.path() + " (" + line.source() + "): " + line.spoken())
                .collect(Collectors.joining("\n"));
        assertEquals("", missing, "lines without their voice file; run python3 tools/art/voice.py");
        lines.stream()
                .filter(line -> !rendered(line) && !Files.isRegularFile(ASSETS.resolve(line.path())))
                .forEach(line -> System.out.println("not rendered yet: " + line.source() + ": " + line.spoken()));
    }

    @Test
    void everyVoiceFileBelongsToALine() throws IOException {
        Set<String> used =
                VoiceLines.all(CONTENT).stream().map(VoiceLines.VoiceLine::path).collect(Collectors.toSet());
        Path voice = ASSETS.resolve("voice");
        if (!Files.isDirectory(voice)) {
            return;
        }
        try (Stream<Path> files = Files.walk(voice)) {
            List<String> unused = files.filter(Files::isRegularFile)
                    .map(file -> ASSETS.relativize(file).toString().replace('\\', '/'))
                    .filter(path -> !used.contains(path))
                    .toList();
            assertEquals(List.of(), unused, "voice files no line uses; tools/art/voice.py deletes them");
        }
    }

    /** Every speaker has a voice; only a speaker marked {@code uncast} in the speaker table may go without. */
    @Test
    void everySpeakerOfALevelOrBriefingHasAVoice() {
        for (LevelData level : CONTENT.levels().values()) {
            for (LevelData.RadioCue cue : level.radio()) {
                assertTrue(
                        CONTENT.voices().voiceOf(cue.speaker()).isPresent()
                                || CONTENT.voices().uncast(cue.speaker()),
                        "no voice for " + cue.speaker());
            }
            level.briefing()
                    .pages()
                    .forEach(page -> assertTrue(
                            CONTENT.voices().voiceOf(page.speaker()).isPresent(), "no voice for " + page.speaker()));
        }
    }

    @Test
    void stageDirectionsAndMarkupAreNotSpoken() {
        assertEquals("", VoiceLines.spoken("[the Choir sings]"));
        assertEquals("The Kestrel's lifeboat stores.", VoiceLines.spoken("The *Kestrel*'s lifeboat stores."));
        assertEquals("on the hull, burn it off us!", VoiceLines.spoken("on the hull — burn it off us!"));
        assertTrue(VoiceLines.line(
                        CONTENT.voices(),
                        "The Choir",
                        "[the Choir sings]",
                        Expression.NEUTRAL,
                        false,
                        VoiceLines.Filter.DISTORTED,
                        "test")
                .isEmpty());
    }

    @Test
    void aShoutedLineUsesTheShoutRowAndAnotherKey() {
        var calm = VoiceLines.line(
                        CONTENT.voices(), "Rook", "Both edges!", Expression.FIERCE, false, VoiceLines.Filter.RADIO, "t")
                .orElseThrow();
        var shout = VoiceLines.line(
                        CONTENT.voices(), "Rook", "Both edges!", Expression.FIERCE, true, VoiceLines.Filter.RADIO, "t")
                .orElseThrow();
        assertEquals(1.2, shout.settings().exaggeration(), 1e-9);
        assertTrue(!calm.key().equals(shout.key()));
        assertEquals(12, shout.key().length());
    }
}
