package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
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
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part H's sounds (design/audio/sfx and music, Level 14): the canopy's hum as the level's ambience
 * (the key {@code earth-canopy}), dying with the Siege Spire over 2 s while the megacity's own ambience
 * rises over 7 s; "Firestorm" from section 2, the full mix from section 4, the boss warning and the boss
 * theme for the Spire, the debrief's jingle; round 36's sounds (variant a until the round closes): a root
 * burst's stone giving way from its telegraph and its growths growing in as they appear.
 */
class Level14SoundsTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(14).orElseThrow();
    private static final LevelData LEVEL = CONTENT.level(KEY);
    private static final long NANOS_PER_STEP = 1_000_000_000L / SimStep.PER_SECOND;

    @Test
    void everySoundHasItsFileAndRoundThirtySixsVariantA() {
        for (Sfx sfx : List.of(
                Sfx.ROOT_BURST, Sfx.ROOT_GROWTH, Sfx.TEAR_FREE_ROAR, Sfx.CANOPY_WITHERING, Sfx.AMBIENCE_CANOPY)) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx.path());
            assertTrue(sfx.path().endsWith("-r36-a.ogg"), sfx + ": round 36's a, pending the user's pick");
        }
        assertEquals(Sfx.Priority.AMBIENCE, Sfx.AMBIENCE_CANOPY.priority());
    }

    @Test
    void theLevelsAmbienceIsTheCanopysHumWhichHandsOverToTheCityAtTheKill() {
        assertEquals(Sfx.AMBIENCE_CANOPY, Sfx.ambience(LEVEL.music().ambience()));
        assertEquals(Sfx.AMBIENCE_CITY, Sfx.afterBoss(Sfx.AMBIENCE_CANOPY));
        assertNull(Sfx.afterBoss(Sfx.AMBIENCE_CITY), "every other ambience plays on");
        assertNull(Sfx.afterBoss(Sfx.AMBIENCE_ARCTIC));
        CONTENT.levels().forEach((key, level) -> {
            if (!key.equals(KEY)) {
                assertNull(Sfx.afterBoss(Sfx.ambience(level.music().ambience())), key + " keeps its ambience");
            }
        });
    }

    @Test
    void firestormFromTheOldTownTheFullMixFromTheApproachAndTheSpiresOwnMusic() {
        LevelData.Music music = LEVEL.music();
        assertEquals(7, music.track(), "Act 2 B, Firestorm");
        assertEquals(2, music.startSection(), "the ambience alone on the lakefront");
        assertFalse(music.full(2));
        assertFalse(music.full(3));
        assertTrue(music.full(4), "the full mix from the Concord approach");
        assertEquals(Optional.of("boss-warning"), music.bossWarning(), "track 22 at the arena's start");
        assertEquals(Optional.of("choir-descends"), music.bossTrack(), "track 18 through the fight");
        assertTrue(music.debriefJingle(), "the debrief's jingle as every level's (H6 = a)");
        assertTrue(music.jingle().isEmpty(), "no jingle inside the level");
    }

    @Test
    void theHumFadesOutOverTwoSecondsAtTheKillAsTheCityRisesOverSeven() {
        MockAudio audio = new MockAudio();
        Lwjgl3Files files = new Lwjgl3Files();
        Mixer mixer = new Mixer(AudioSettings.defaults());
        SfxBank bank = new SfxBank(audio, files, mixer);
        LevelMusic music = music(audio, files, mixer, bank);
        music.update(5, 120, 0.1f, false);
        assertTrue(bank.looping(Sfx.AMBIENCE_CANOPY));
        assertFalse(bank.looping(Sfx.AMBIENCE_CITY));
        float full = bank.loopVolume(Sfx.AMBIENCE_CANOPY);
        music.bossDown();
        assertEquals(Sfx.AMBIENCE_CITY, music.ambience());
        assertEquals(Sfx.AMBIENCE_CANOPY, music.fadingAmbience());
        music.update(5, 250, 1f, false);
        assertEquals(full / 2, bank.loopVolume(Sfx.AMBIENCE_CANOPY), 1e-4, "half way out after 1 s");
        assertEquals(full / 7, bank.loopVolume(Sfx.AMBIENCE_CITY), 1e-4, "a seventh in");
        music.update(6, 251, 1f, false);
        assertEquals(0, bank.loopVolume(Sfx.AMBIENCE_CANOPY), 1e-6, "the hum gone by 2 s");
        for (int s = 0; s < 5; s++) {
            music.update(6, 252 + s, 1f, false);
        }
        assertEquals(full, bank.loopVolume(Sfx.AMBIENCE_CITY), 1e-4, "the city at full by 7 s");
        assertFalse(bank.looping(Sfx.AMBIENCE_CANOPY), "the hum's loop stopped");
        assertEquals(null, music.fadingAmbience());
        music.restart();
        assertTrue(bank.looping(Sfx.AMBIENCE_CANOPY), "a new attempt starts under the canopy again");
        assertFalse(bank.looping(Sfx.AMBIENCE_CITY));
        music.dispose();
        bank.dispose();
    }

    @Test
    void aRootBurstsStoneGivesWayAtItsTelegraphAndItsGrowthsGrowAsTheyAppear() {
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        long[] now = {0};
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()), () -> now[0]);
        var sounds = new FlightSounds(bank, new EnemyLooks[0], sortie.armament(), List.of());
        int telegraphs = 0;
        int grown = 0;
        for (long t = 0; t < SimStep.ticks(70); t++) {
            now[0] += NANOS_PER_STEP;
            sortie.step(0);
            sounds.step();
            SimEvents events = sortie.events();
            try {
                sounds.play(events);
            } catch (ArrayIndexOutOfBoundsException lookless) {
                // a kill without the test's looks
            }
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case ROOT_BURST_TELEGRAPH -> {
                        assertTrue(bank.playing(Sfx.ROOT_BURST) > 0, "the paving under stress");
                        telegraphs++;
                    }
                    case ROOT_BURST_GREW -> {
                        assertTrue(bank.playing(Sfx.ROOT_GROWTH) > 0, "the growths growing in");
                        grown++;
                    }
                    default -> {}
                }
            }
        }
        assertEquals(1, telegraphs, "the first burst's telegraph at t=63");
        assertEquals(1, grown, "its growths 1 s later");
        bank.dispose();
    }

    @Test
    void theCanopyWithersAsItTears() {
        assertEquals(3.0, FlightSounds.WITHERING_DELAY_SECONDS, 1e-9, "the end of the 3 s death chain (step H7b)");
    }

    private static LevelMusic music(MockAudio audio, Lwjgl3Files files, Mixer mixer, SfxBank bank) {
        LevelData.Music data = LEVEL.music();
        return new LevelMusic(
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
                Optional.empty());
    }
}
