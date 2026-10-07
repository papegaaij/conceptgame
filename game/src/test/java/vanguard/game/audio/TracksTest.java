package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TracksTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));

    @Test
    void everyTrackWithAFileHasItInTheAssets() {
        for (int track = 1; track <= 30; track++) {
            int number = track;
            Tracks.name(track)
                    .ifPresent(name -> assertTrue(
                            Files.exists(ASSETS.resolve(Tracks.path(name))), "track " + number + ": " + name));
        }
    }

    @Test
    void theLevelThemesAndTheActBossCuesHaveTheirFiles() {
        assertEquals("music/coalition-rising.ogg", Tracks.path(5));
        assertEquals("music/afterburner-base.ogg", Tracks.basePath("afterburner"));
        // Track 6, Level 08's "Homefront" (M5 part B), with its base stem.
        assertEquals("music/homefront.ogg", Tracks.path(6));
        assertEquals("music/homefront-base.ogg", Tracks.basePath(Tracks.name(6).orElseThrow()));
        assertTrue(Files.exists(ASSETS.resolve(Tracks.basePath("homefront"))), "the Homefront base stem");
        assertEquals("music/choir-descends.ogg", Tracks.path(Tracks.BOSS_VRELL));
        assertEquals("music/boss-warning.ogg", Tracks.path(Tracks.BOSS_WARNING));
        assertEquals("music/act-complete.ogg", Tracks.path(Tracks.ACT_COMPLETE));
    }

    @Test
    void theBossTrackLoopsAndTheWarningLastsItsThreeBars() throws IOException {
        try (var track = new VorbisFile(Files.readAllBytes(ASSETS.resolve(Tracks.path(Tracks.BOSS_VRELL))));
                var warning = new VorbisFile(Files.readAllBytes(ASSETS.resolve(Tracks.path(Tracks.BOSS_WARNING))))) {
            assertTrue(track.loopPoints().start() > 0, "the boss track has an intro and loop points");
            assertEquals(track.sampleRate(), warning.sampleRate());
            assertEquals(track.channels(), warning.channels());
            double seconds = warning.frameCount() / (double) warning.sampleRate();
            assertTrue(
                    seconds >= Tracks.BOSS_WARNING_BARS_SECONDS,
                    "the warning plays its bars before the boss track: " + seconds);
        }
    }
}
