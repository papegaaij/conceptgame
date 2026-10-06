package vanguard.content.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
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
    /**
     * The levels whose lines must have their files, the acts whose briefings must, and the other
     * sources by their prefix (the specials' calls, the low-armour line, Rook's barks).
     */
    private static final Set<String> RENDERED = Set.of(
            "act-1-first-contact/level-01-break-at-dawn",
            "act-1-first-contact/level-02-shipyard-burning",
            "act-1-first-contact/level-03-spore-drift",
            "act-1-first-contact/level-04-tranquility-run",
            "act-1-first-contact/level-05-crater-nest",
            "act-1-first-contact/level-06-farside",
            "act-1-first-contact/level-07-brood-carrier",
            "act-1-first-contact briefing",
            "act-1-first-contact outro",
            "specials",
            "armour",
            VoiceLines.BARKS);

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

    /**
     * The rendered levels have no uncast speaker left: Level 07's Lifeboat Seven was cast in round 25,
     * so its line has a voice (and its file, above) instead of playing as text.
     */
    @Test
    void everySpeakerOfARenderedLevelIsCast() {
        CONTENT.levels().forEach((id, level) -> {
            if (RENDERED.contains(id)) {
                level.radio()
                        .forEach(cue -> assertTrue(
                                CONTENT.voices().voiceOf(cue.speaker()).isPresent(),
                                id + ": " + cue.speaker() + " is not cast"));
            }
        });
        assertEquals(
                "lifeboat-seven",
                CONTENT.voices().voiceOf("Lifeboat Seven").orElse(null),
                "Lifeboat Seven speaks with its own voice");
    }

    /** The acts' briefings and outros are spoken too: every page's speaker has a voice and a line. */
    @Test
    void everyPageOfAnActBriefingOrOutroIsALine() {
        for (var act : CONTENT.acts().entrySet()) {
            List<BriefingPage> pages = new ArrayList<>(act.getValue().briefing());
            act.getValue().outro().ifPresent(outro -> pages.addAll(outro.pages()));
            for (var page : pages) {
                assertTrue(CONTENT.voices().voiceOf(page.speaker()).isPresent(), "no voice for " + page.speaker());
            }
            act.getValue().outro().ifPresent(outro -> {
                List<String> sources = VoiceLines.all(CONTENT).stream()
                        .map(VoiceLines.VoiceLine::source)
                        .filter(source -> source.startsWith(act.getKey() + " outro page "))
                        .toList();
                assertEquals(outro.pages().size(), sources.size(), "the outro's pages are lines: " + sources);
            });
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

    /** Level 06's perimeter beacon (round 23): its radio lines go through the public-address filter. */
    @Test
    void thePerimeterBeaconSpeaksThroughThePublicAddressFilter() {
        var radio = VoiceLines.line(
                        CONTENT.voices(),
                        "Perimeter beacon",
                        "All residents report to shelter.",
                        Expression.NEUTRAL,
                        false,
                        VoiceLines.Filter.RADIO,
                        "t")
                .orElseThrow();
        assertEquals(VoiceLines.Filter.PA, radio.filter());
        var rook = VoiceLines.line(
                        CONTENT.voices(),
                        "Rook",
                        "All residents report to shelter.",
                        Expression.NEUTRAL,
                        false,
                        VoiceLines.Filter.RADIO,
                        "t")
                .orElseThrow();
        assertEquals(VoiceLines.Filter.RADIO, rook.filter());
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
