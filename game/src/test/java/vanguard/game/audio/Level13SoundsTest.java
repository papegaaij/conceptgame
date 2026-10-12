package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import vanguard.game.render.AtmosphereRamp;
import vanguard.game.render.DroneLooks;
import vanguard.game.render.EnemyLooks;
import vanguard.game.settings.AudioSettings;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part G's sounds (design/audio/sfx and music, Level 13): the arctic ambience under the whole level
 * (the key {@code earth-arctic}, the blocking bug of step G4), 3 dB louder through the whiteout when the
 * peak says so; "Homefront" from section 2, the full mix from section 4, the mission-complete jingle at
 * t=220.6 after the theme's 2.6 s fade from t=218 (grid online's chord before it) and none at the debrief; round 35's sounds (variant a until the round
 * closes): the relay's hit, its integrity alarm at 50 % and 25 %, its destruction, grid online at the
 * synchronisation's end and the supply drone's launch as its arc starts.
 */
class Level13SoundsTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(13).orElseThrow();
    private static final LevelData LEVEL = CONTENT.level(KEY);
    private static final long NANOS_PER_STEP = 1_000_000_000L / SimStep.PER_SECOND;

    @Test
    void everySoundHasItsFileAndRoundThirtyFivesVariantA() {
        for (Sfx sfx :
                List.of(Sfx.RELAY_HIT, Sfx.INTEGRITY_ALARM, Sfx.RELAY_DESTROYED, Sfx.DRONE_LAUNCH, Sfx.GRID_ONLINE)) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx.path());
            assertTrue(sfx.path().endsWith("-r35-a.ogg"), sfx + ": round 35's a, pending the user's pick");
        }
        assertTrue(Files.isRegularFile(ASSETS.resolve(Sfx.AMBIENCE_ARCTIC.path())));
        assertEquals("sfx/ambience-arctic-r08-a.ogg", Sfx.AMBIENCE_ARCTIC.path(), "round 08's chosen a");
    }

    @Test
    void theLevelsAmbienceIsTheArcticWind() {
        // The blocking bug G4 handed over: the level could not launch without the key.
        assertEquals(Sfx.AMBIENCE_ARCTIC, Sfx.ambience(LEVEL.music().ambience()));
        assertEquals(Sfx.Priority.AMBIENCE, Sfx.AMBIENCE_ARCTIC.priority());
    }

    @Test
    void homefrontFromTheHaltTheFullMixInTheWhiteoutAndTheJingleInTheLevel() throws IOException {
        LevelData.Music music = LEVEL.music();
        assertEquals(6, music.track(), "Act 2 A, Homefront");
        assertEquals(2, music.startSection(), "the ambience alone in the approach");
        assertFalse(music.full(2));
        assertFalse(music.full(3));
        assertTrue(music.full(4), "the full mix in the whiteout");
        LevelData.Music.Jingle jingle = music.jingle().orElseThrow();
        assertEquals(218, jingle.t(), 1e-9, "under Nansen's \"Grid sync complete\"");
        LevelData.Defend defend = LEVEL.objectives().defend().orElseThrow();
        assertEquals(defend.from() + defend.duration(), jingle.t(), 1e-9, "the theme fades with grid online");
        // Step G8: grid online's chord (DEFEND_COMPLETE, as loud as the jingle) rang over the jingle's first
        // second; the theme's fade now lasts the chord, so the jingle starts as it ends.
        double chordEnd = jingle.t() + seconds(Sfx.GRID_ONLINE);
        assertEquals(chordEnd, jingle.t() + jingle.fadeSeconds(), 0.1, "the jingle after the chord");
        assertTrue(jingle.t() + jingle.fadeSeconds() >= chordEnd - 0.05);
        assertFalse(music.debriefJingle(), "the debrief starts without it");
        // Every other level keeps its debrief jingle.
        CONTENT.levels().forEach((key, level) -> {
            if (!key.equals(KEY)) {
                assertTrue(level.music().debriefJingle(), key);
            }
        });
    }

    @Test
    void theThemeFadesOutAtTheJinglesTimeAndTheJinglePlaysOnceAfterIt() {
        MockAudio audio = new MockAudio();
        Lwjgl3Files files = new Lwjgl3Files();
        Mixer mixer = new Mixer(AudioSettings.defaults());
        long[] now = {0};
        SfxBank bank = new SfxBank(audio, files, mixer, () -> now[0]);
        LevelMusic music = music(audio, files, mixer, bank).jingle(218, 2.6);
        music.update(4, 217.5, 0.5f, false);
        assertFalse(music.jinglePlayed());
        assertEquals(0, bank.playing(Sfx.MISSION_COMPLETE));
        music.update(4, 218, 0.5f, false);
        assertFalse(music.jinglePlayed(), "the theme fades out first");
        music.update(4, 218.5, 0.5f, false);
        assertFalse(music.jinglePlayed());
        music.update(4, 219, 0.5f, false);
        assertFalse(music.jinglePlayed(), "not over grid online's chord");
        music.update(4, 220.5, 0.5f, false);
        assertFalse(music.jinglePlayed());
        music.update(4, 220.6, 0.1f, false);
        assertTrue(music.jinglePlayed(), "after the 2.6 s fade, as the chord ends");
        assertEquals(1, bank.playing(Sfx.MISSION_COMPLETE));
        for (double t = 221; t < 235; t += 0.5) {
            music.update(4, t, 0.5f, false);
        }
        assertEquals(1, bank.playing(Sfx.MISSION_COMPLETE), "once");
        music.restart();
        assertFalse(music.jinglePlayed(), "a new attempt plays it again");
        music.cut();
        music.update(4, 220, 0.5f, false);
        assertFalse(music.jinglePlayed(), "never over a lost sortie's cut music");
        music.dispose();
        bank.dispose();
    }

    @Test
    void theWindRisesThreeDecibelsThroughAPeakThatAsksForIt() {
        // The peak's ambience_db (the data line for section 4 is the main agent's merge, step G6's report):
        // a synthetic level stands in so the rule is pinned whatever the data says today.
        LevelData.Peak peak =
                new LevelData.Peak(LevelData.Atmosphere.HEAVY, 176, 188, Optional.of(6.0), Optional.of(3.0));
        List<LevelData.Section> level = withPeak(peak);
        double ramp = LEVEL.backdrop().ramp();
        assertEquals(0, AtmosphereRamp.ambienceDb(level, ramp, false, 170), 1e-9, "before the whiteout");
        assertEquals(1.5, AtmosphereRamp.ambienceDb(level, ramp, false, 176), 1e-9, "half way at its start");
        assertEquals(3, AtmosphereRamp.ambienceDb(level, ramp, false, 176 + ramp / 2), 1e-9, "full once ramped in");
        assertEquals(3, AtmosphereRamp.ambienceDb(level, ramp, false, 182), 1e-9);
        assertEquals(0, AtmosphereRamp.ambienceDb(level, ramp, false, 188 + ramp / 2), 1e-9, "ramped out");
        // Easy's 6 s whiteout: centred on the authored peak, 179-185.
        assertEquals(1.5, AtmosphereRamp.ambienceDb(level, ramp, true, 179), 1e-9);
        assertEquals(1.5, AtmosphereRamp.ambienceDb(level, ramp, true, 185), 1e-9);
        assertEquals(0, AtmosphereRamp.ambienceDb(level, ramp, true, 176), 1e-9);
        MockAudio audio = new MockAudio();
        Lwjgl3Files files = new Lwjgl3Files();
        Mixer mixer = new Mixer(AudioSettings.defaults());
        SfxBank bank = new SfxBank(audio, files, mixer);
        LevelMusic music =
                music(audio, files, mixer, bank).ambienceLevels(t -> AtmosphereRamp.ambienceDb(level, ramp, false, t));
        music.update(4, 170, 0.1f, false);
        assertEquals(1, music.ambienceGain(), 1e-6);
        music.update(4, 182, 0.1f, false);
        assertEquals((float) Math.pow(10, 3 / 20.0), music.ambienceGain(), 1e-5, "+3 dB in the whiteout");
        assertTrue(bank.looping(Sfx.AMBIENCE_ARCTIC));
        music.restart();
        assertEquals(1, music.ambienceGain(), 1e-6, "a restart starts at the ambience's own level");
        music.dispose();
        bank.dispose();
    }

    @Test
    void aPeakWithoutTheKeysLeavesTheAmbienceAndTheEasyPeakAlone() {
        for (LevelData.Section section : LEVEL.sections()) {
            section.peak().ifPresent(peak -> {
                if (peak.ambienceDb().isEmpty()) {
                    assertEquals(0, peak.ambienceRise(), 1e-9);
                }
                if (peak.easy().isEmpty()) {
                    assertEquals(peak, peak.onEasy());
                }
            });
        }
        LevelData level02 = CONTENT.level(CONTENT.levelKey(2).orElseThrow());
        for (double t = 0; t < 250; t += 5) {
            assertEquals(0, AtmosphereRamp.ambienceDb(level02, false, t), 1e-9, "Level 02 at " + t);
        }
        assertEquals(level02.atmosphereStretches(), level02.atmosphereStretches(true), "no easy peak there");
    }

    @Test
    void theRelaysSoundsSitWithTheWarningsAndTheExplosions() {
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.ALARM_LEVEL, 1e-6, "the alarm is a warning");
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.GRID_ONLINE_LEVEL, 1e-6);
        assertEquals(FlightSounds.WARNING_LEVEL, FlightSounds.RELAY_DESTROYED_LEVEL, 1e-6, "a big loss");
        assertTrue(FlightSounds.RELAY_HIT_LEVEL < FlightSounds.WARNING_LEVEL, "the hit plays often: under them");
        assertTrue(FlightSounds.DRONE_LEVEL > FlightSounds.SONDE_LEVEL, "not a quiet pickup cue");
    }

    @Test
    void theRelaysHitsAlarmAndLossFollowItsEvents() {
        // Medium, the ship holding still at its start: nothing guards the relay, so it is hit, drops below
        // half and a quarter and falls.
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        long[] now = {0};
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()), () -> now[0]);
        var sounds = new FlightSounds(bank, new EnemyLooks[0], sortie.armament(), List.of());
        sounds.defend(sortie.defending(), new double[] {162, 202}, DroneLooks.ARC_SECONDS);
        assertTrue(sortie.defending());
        boolean hit = false;
        int alarms = 0;
        boolean lost = false;
        for (long t = 0; t < SimStep.ticks(230) && !lost; t++) {
            now[0] += NANOS_PER_STEP;
            sortie.step(0);
            sounds.step();
            SimEvents events = sortie.events();
            try {
                sounds.play(events);
            } catch (ArrayIndexOutOfBoundsException lookless) {
                // a kill without the test's looks
            }
            sounds.watch(sortie);
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case ALLY_HIT -> {
                        assertTrue(bank.playing(Sfx.RELAY_HIT) > 0, "the relay's hit, not a crawler's");
                        hit = true;
                    }
                    case ALLY_BELOW -> {
                        assertTrue(bank.playing(Sfx.INTEGRITY_ALARM) > 0, "the alarm at " + events.value(i) + " %");
                        alarms++;
                    }
                    case ALLY_LOST -> {
                        assertTrue(bank.playing(Sfx.RELAY_DESTROYED) > 0, "the power-down");
                        lost = true;
                    }
                    default -> {}
                }
            }
        }
        assertTrue(hit, "the relay was hit");
        assertEquals(2, alarms, "once at 50 %, once at 25 %");
        assertTrue(lost, "unguarded, the relay falls");
        bank.dispose();
    }

    @Test
    void aSupplyDronesLaunchPlaysAsItsArcStartsFourSecondsBeforeItsDrop() {
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()));
        var sounds = new FlightSounds(bank, new EnemyLooks[0], new vanguard.sim.Armament(List.of()), List.of());
        double[] drops =
                LEVEL.released().stream().mapToDouble(LevelData.Drop::t).toArray();
        assertArrayEquals(new double[] {162, 202}, drops, 1e-9, "the drones' drops (G6 = a)");
        sounds.defend(true, drops, DroneLooks.ARC_SECONDS);
        assertArrayEquals(new long[] {SimStep.ticks(158), SimStep.ticks(198)}, sounds.launchTicks());
        long launch = SimStep.ticks(158);
        assertFalse(FlightSounds.launched(-1, launch, sounds.launchTicks()), "not before the first watch");
        assertFalse(FlightSounds.launched(launch - 2, launch - 1, sounds.launchTicks()));
        assertTrue(FlightSounds.launched(launch - 1, launch, sounds.launchTicks()), "as the arc starts");
        assertFalse(FlightSounds.launched(launch, launch + 1, sounds.launchTicks()), "once");
        assertEquals(4, DroneLooks.ARC_SECONDS, 1e-9, "the arc's 4 s (G6 = a)");
        bank.dispose();
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

    /** A sound file's length, seconds. */
    static double seconds(Sfx sfx) throws IOException {
        try (var file = new VorbisFile(Files.readAllBytes(ASSETS.resolve(sfx.path())))) {
            return file.frameCount() / (double) file.sampleRate();
        }
    }

    /** Level 13's sections with the whiteout's peak (the heavy one) replaced by {@code peak}. */
    static List<LevelData.Section> withPeak(LevelData.Peak peak) {
        return LEVEL.sections().stream()
                .map(section -> section.peak()
                                .filter(p -> p.atmosphere() == LevelData.Atmosphere.HEAVY)
                                .isPresent()
                        ? new LevelData.Section(
                                section.name(),
                                section.end(),
                                section.speed(),
                                section.atmosphere(),
                                Optional.of(peak),
                                section.tiles(),
                                section.arena(),
                                section.ramp())
                        : section)
                .toList();
    }
}
