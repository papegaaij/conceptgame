package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.game.render.EnemyLooks;
import vanguard.game.settings.AudioSettings;
import vanguard.sim.Command;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part F's sounds (design/audio/sfx and music, Level 12): round 34's sounds (variant a until the
 * round closes) from their files, the rain-only storm bed with the ocean in the eye, the thunder
 * 0.3 to 1.5 s after each flash, the gust with its telegraph, the latch and the drain loop while a
 * Lamprey drains, the sonde's chime; the latch, shake-off and gust at the warnings' level with a short
 * duck of the theme; the theme's base stem at -6 dB in the eye.
 */
class Level12SoundsTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(12).orElseThrow();
    private static final long NANOS_PER_STEP = 1_000_000_000L / SimStep.PER_SECOND;

    @Test
    void everySoundHasItsFileAndRoundThirtyFoursVariantA() {
        List<Sfx> all = List.of(
                Sfx.AMBIENCE_STORM,
                Sfx.THUNDER_1,
                Sfx.THUNDER_2,
                Sfx.THUNDER_3,
                Sfx.GUST,
                Sfx.LAMPREY_LATCH,
                Sfx.LAMPREY_DRAIN,
                Sfx.LAMPREY_SHAKE,
                Sfx.SONDE_CHIME);
        for (Sfx sfx : all) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx.path());
            assertTrue(sfx.path().endsWith("-r34-a.ogg"), sfx + ": round 34's a, pending the user's pick");
        }
        assertEquals("sfx/ambience-storm-rain-r34-a.ogg", Sfx.AMBIENCE_STORM.path(), "the rain-only bed (F6 = a)");
        // Round 08's chosen storm ambience (rain and its own thunder) stays in the assets.
        assertTrue(Files.isRegularFile(ASSETS.resolve("sfx/ambience-storm-r08-a.ogg")));
    }

    @Test
    void theLevelsMusicHasTheStormBedTheOceanInTheEyeAndTheEyesLevel() {
        LevelData.Music music = CONTENT.level(KEY).music();
        assertEquals(Sfx.AMBIENCE_STORM, Sfx.ambience(music.ambience()));
        List<LevelData.Music.AmbienceChange> changes = music.ambienceChangeList();
        assertEquals(2, changes.size());
        assertEquals(5, changes.get(0).section());
        assertEquals(Sfx.AMBIENCE_OCEAN, Sfx.ambience(changes.get(0).ambience()));
        assertEquals(6, changes.get(1).section());
        assertEquals(Sfx.AMBIENCE_STORM, Sfx.ambience(changes.get(1).ambience()));
        assertEquals(2, music.startSection(), "the storm bed alone in the squall");
        assertFalse(music.full(3), "the base stem in the rain wall and the lightning field");
        assertTrue(music.full(4), "the full mix in the storm wall");
        assertFalse(music.full(5), "the base stem in the eye");
        assertTrue(music.full(6), "the full mix at the breakout");
        for (int section = 1; section <= 6; section++) {
            assertEquals(section == 5 ? -6 : 0, music.sectionLevel(section), 1e-9, "section " + section);
        }
    }

    @Test
    void theThemeDropsSixDecibelsOverASecondInTheEyeAndComesBack() {
        MockAudio audio = new MockAudio();
        Lwjgl3Files files = new Lwjgl3Files();
        Mixer mixer = new Mixer(AudioSettings.defaults());
        SfxBank bank = new SfxBank(audio, files, mixer);
        LevelData.Music data = CONTENT.level(KEY).music();
        LevelMusic music = new LevelMusic(
                        audio,
                        mixer,
                        files.internal(Tracks.path(data.track())),
                        files.internal(Tracks.path(data.track())),
                        bank,
                        Sfx.ambience(data.ambience()),
                        data.startSection(),
                        0,
                        data::full,
                        Double.POSITIVE_INFINITY,
                        Optional.empty())
                .sectionLevels(data::sectionLevel);
        music.update(4, 140, 0.5f, false);
        assertEquals(1, music.sectionGain(), 1e-6, "full in the wall");
        music.update(5, 150, 0.5f, false);
        float eye = (float) Math.pow(10, -6 / 20.0);
        assertEquals((1 + eye) / 2, music.sectionGain(), 1e-4, "half way after half a second");
        music.update(5, 151, 0.5f, false);
        assertEquals(eye, music.sectionGain(), 1e-6, "-6 dB after a second");
        music.update(5, 160, 3, false);
        assertEquals(eye, music.sectionGain(), 1e-6);
        music.update(6, 176, 1, false);
        assertEquals(1, music.sectionGain(), 1e-6, "full again at the breakout");
        music.update(5, 160, 1, false);
        music.restart();
        assertEquals(1, music.sectionGain(), 1e-6, "a restart starts at the first section's level");
        music.dispose();
        bank.dispose();
    }

    @Test
    void theShakeOffCutsThroughTheStorm() throws IOException {
        // Step G8: at the warnings' level (the game's ceiling) the shake-off was still marginal in the busy
        // mix, so its file is 3.5 dB louder (sfx_r34.py, -13.5 dB): its loudest moment now sits clearly over
        // the latch's (it was level with it), the latch and the gust unchanged.
        double shake = loudestDb(Sfx.LAMPREY_SHAKE);
        double latch = loudestDb(Sfx.LAMPREY_LATCH);
        assertTrue(shake >= latch + 1.5, "shake-off " + shake + " dBFS, latch " + latch);
        assertTrue(shake < -3, "under the clipping: " + shake);
        assertEquals(-15.8, latch, 0.5, "the latch stays");
        assertEquals(-18.7, loudestDb(Sfx.GUST), 0.5, "the gust stays");
    }

    /** The RMS of a file's loudest 100 ms (in steps of 50 ms), dBFS. */
    private static double loudestDb(Sfx sfx) throws IOException {
        try (var file = new VorbisFile(Files.readAllBytes(ASSETS.resolve(sfx.path())))) {
            int channels = file.channels();
            int half = file.sampleRate() / 20;
            short[] block = new short[half * channels];
            double previous = -1;
            double best = 0;
            int read;
            while ((read = file.read(block, 0, half)) > 0) {
                double energy = 0;
                for (int i = 0; i < read * channels; i++) {
                    double sample = block[i] / 32768.0;
                    energy += sample * sample;
                }
                if (previous >= 0) {
                    best = Math.max(best, (previous + energy) / (2.0 * half * channels));
                }
                previous = energy;
            }
            return 10 * Math.log10(Math.max(best, 1e-12));
        }
    }

    @Test
    void theLatchTheShakeOffAndTheGustSitWithTheWarnings() {
        // The round 34 capture could not hear them (the gust at -4 dB, the shake-off at -2 dB): at the
        // warnings' level now, as the edge warning and the shield hit play.
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.LAMPREY_LATCH_LEVEL, 1e-6);
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.LAMPREY_SHAKE_LEVEL, 1e-6);
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.GUST_LEVEL, 1e-6);
        assertEquals(1, FlightSounds.WARNING_LEVEL, 1e-6, "the loudest group");
        assertTrue(FlightSounds.DRAIN_LEVEL < FlightSounds.WARNING_LEVEL, "the drain's loop stays under them");
        assertTrue(FlightSounds.SONDE_LEVEL < FlightSounds.WARNING_LEVEL, "the chime, a pickup, too");
        // A latch and a shake-off duck the theme a moment, as the scripted loss's duck (-6 dB).
        assertEquals(-6, FlightSounds.CUE_DUCK_DB, 1e-9);
        assertTrue(FlightSounds.CUE_DUCK_SECONDS > 0.5 && FlightSounds.CUE_DUCK_SECONDS <= 1.5);
        assertEquals(
                (float) Math.pow(10, -6 / 20.0),
                LevelMusic.duckTarget(false, (float) Math.pow(10, FlightSounds.CUE_DUCK_DB / 20)),
                1e-6);
    }

    @Test
    void theThunderFollowsEachFlashAfterAThirdToOneAndAHalfSecondsInTurn() {
        for (int flash = 0; flash < 40; flash++) {
            int steps = FlightSounds.thunderSteps(flash);
            assertTrue(
                    steps >= SimStep.ticks(FlightSounds.THUNDER_MIN_SECONDS)
                            && steps <= SimStep.ticks(FlightSounds.THUNDER_MAX_SECONDS),
                    "flash " + flash + ": " + steps);
            assertNotEquals(FlightSounds.thunder(flash), FlightSounds.thunder(flash + 1), "no roll twice in a row");
        }
        assertNotEquals(FlightSounds.thunderSteps(0), FlightSounds.thunderSteps(1), "the delays differ");
        assertTrue(FlightSounds.thunderPan(0) < 0 && FlightSounds.thunderPan(1) > 0, "from the bolt's side");
    }

    @Test
    void theStormsSoundsFollowItsEvents() {
        // Medium, the ship holding still at its start: its armour carries it to the first latch.
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        long[] now = {0};
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()), () -> now[0]);
        var sounds = new FlightSounds(bank, new EnemyLooks[0], sortie.armament(), List.of());
        boolean gust = false;
        boolean latched = false;
        boolean drained = false;
        long latchStep = -1;
        for (long t = 0; t < SimStep.ticks(60) && !(gust && latched && drained); t++) {
            now[0] += NANOS_PER_STEP;
            int before = sortie.wind().telegraph(SimStep.ticks(sortie.levelSeconds()));
            sortie.step(0);
            sounds.step();
            SimEvents events = sortie.events();
            try {
                sounds.play(events);
            } catch (ArrayIndexOutOfBoundsException lookless) {
                // a kill without the test's looks
            }
            sounds.watch(sortie);
            int telegraph = sortie.wind().telegraph(SimStep.ticks(sortie.levelSeconds()));
            if (before == 0 && telegraph != 0) {
                assertTrue(bank.playing(Sfx.GUST) > 0, "the gust with its telegraph at " + sortie.levelSeconds());
                gust = true;
            }
            if (events.count(SimEvents.Type.LAMPREY_LATCHED) > 0) {
                assertTrue(bank.playing(Sfx.LAMPREY_LATCH) > 0, "the latch's bite");
                latched = true;
                latchStep = t;
            }
            assertEquals(
                    sortie.draining() && sortie.flying() && !sortie.complete(),
                    bank.looping(Sfx.LAMPREY_DRAIN),
                    "the drain loop while a Lamprey drains, at " + sortie.levelSeconds());
            drained |= bank.looping(Sfx.LAMPREY_DRAIN);
        }
        assertTrue(gust, "the demonstration gust at t=16");
        assertTrue(latched && latchStep > 0, "a Lamprey of the introduction latched");
        assertTrue(drained, "and drained");
        sounds.stopLoops();
        assertFalse(bank.looping(Sfx.LAMPREY_DRAIN));
        bank.dispose();
    }

    @Test
    void aFlashsThunderFollowsItAndASondesChimePlaysAsItIsCollected() {
        // Invulnerable, the ship flying under the first sonde (t=60) and holding there past the first flash.
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        long[] now = {0};
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()), () -> now[0]);
        var sounds = new FlightSounds(bank, new EnemyLooks[0], sortie.armament(), List.of());
        double sondeX = sortie.sondes().x(0);
        long flashStep = -1;
        int flash = -1;
        boolean chimed = false;
        for (long t = 0; t < SimStep.ticks(85) && !(chimed && flashStep >= 0 && t > flashStep + 120); t++) {
            now[0] += NANOS_PER_STEP;
            double dx = sondeX - sortie.ship().x();
            int commands = Math.abs(dx) < 2 ? 0 : (dx > 0 ? Command.RIGHT : Command.LEFT).bit();
            sortie.step(commands);
            sounds.step();
            SimEvents events = sortie.events();
            try {
                sounds.play(events);
            } catch (ArrayIndexOutOfBoundsException lookless) {
                // a kill without the test's looks
            }
            sounds.watch(sortie);
            if (events.count(SimEvents.Type.SONDE_COLLECTED) > 0) {
                assertTrue(bank.playing(Sfx.SONDE_CHIME) > 0, "the chime");
                chimed = true;
            }
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.LIGHTNING && flashStep < 0) {
                    flashStep = t;
                    flash = events.value(i);
                    assertEquals(0, bank.playing(FlightSounds.thunder(flash)), "no thunder with the flash");
                }
            }
            if (flashStep >= 0 && t == flashStep + FlightSounds.thunderSteps(flash) - 1) {
                assertEquals(0, bank.playing(FlightSounds.thunder(flash)), "not yet");
            }
            if (flashStep >= 0 && t == flashStep + FlightSounds.thunderSteps(flash)) {
                assertTrue(bank.playing(FlightSounds.thunder(flash)) > 0, "the thunder after its delay");
            }
        }
        assertTrue(chimed, "the first sonde was collected");
        assertEquals(0, flash, "the level's first flash (t=78)");
        assertEquals(SimStep.ticks(78), flashStep + 1, 1);
        bank.dispose();
    }
}
