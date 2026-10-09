package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.game.render.EnemyLooks;
import vanguard.game.settings.AudioSettings;
import vanguard.sim.BossSpec;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part E's sounds (design/audio/sfx and music, Level 11): the Kraken's churn, slam (its impact on
 * the slam), surfacing and death, the ships' hit and sinking, the frigate's flak, the Driftjelly's
 * ring, the torpedo's launch and the water explosions, and the level's music data.
 */
class Level11SoundsTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(11).orElseThrow();
    private static final long NANOS_PER_STEP = 1_000_000_000L / SimStep.PER_SECOND;

    @Test
    void everySoundHasItsFileAndRoundThirtyThreesNames() {
        List<Sfx> all = List.of(
                Sfx.KRAKEN_SLAM,
                Sfx.KRAKEN_CHURN,
                Sfx.KRAKEN_SURFACE,
                Sfx.KRAKEN_DEATH,
                Sfx.SHIP_HIT,
                Sfx.SHIP_SINK,
                Sfx.FRIGATE_FLAK,
                Sfx.DRIFTJELLY_PULSE,
                Sfx.SHOT_TORPEDO,
                Sfx.EXPLOSION_WATER,
                Sfx.EXPLOSION_UNDERWATER);
        for (Sfx sfx : all) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx.path());
        }
        // Round 33's sounds, all accepted (2026-10-09): seven made from their originals by
        // tools/art/sfx_originals.py, the synthesized pulse copied, all under their concept names.
        assertEquals("sfx/kraken-slam-r33-a.ogg", Sfx.KRAKEN_SLAM.path(), "round 33's picks");
        assertEquals("sfx/kraken-churn-r33-a.ogg", Sfx.KRAKEN_CHURN.path());
        assertEquals("sfx/kraken-surface-r33-a.ogg", Sfx.KRAKEN_SURFACE.path());
        assertEquals("sfx/kraken-death-r33-a.ogg", Sfx.KRAKEN_DEATH.path());
        assertEquals("sfx/ship-hit-r33-a.ogg", Sfx.SHIP_HIT.path());
        assertEquals("sfx/ship-sink-r33-a.ogg", Sfx.SHIP_SINK.path());
        assertEquals("sfx/frigate-flak-r33-a.ogg", Sfx.FRIGATE_FLAK.path());
        assertEquals("sfx/driftjelly-pulse-r33-a.ogg", Sfx.DRIFTJELLY_PULSE.path());
        assertEquals("sfx/shot-torpedo-r03-a.ogg", Sfx.SHOT_TORPEDO.path());
    }

    @Test
    void theSlamFilesImpactSitsWhereTheStartAssumesIt() throws IOException {
        assertEquals(0.5, FlightSounds.SLAM_IMPACT_SECONDS, 1e-9);
        // The impact's thud at 0.5 s and the splash after it hold the file's loudest 50 ms (0.85 s in
        // round 33 a), well after the rush that rises to it.
        double loudest = Level10SoundsTest.loudest(Sfx.KRAKEN_SLAM);
        assertTrue(loudest >= FlightSounds.SLAM_IMPACT_SECONDS && loudest < 1.2, "the loudest moment: " + loudest);
    }

    @Test
    void theTorpedoPodPlaysTheTorpedoLaunch() {
        var pod = CONTENT.weapon("torpedo-pod");
        assertEquals("torpedo", pod.sfx());
    }

    @Test
    void theLevelsMusicHasItsAmbienceSectionsAndSting() {
        LevelData.Music music = CONTENT.level(KEY).music();
        assertEquals(Sfx.AMBIENCE_OCEAN, Sfx.ambience(music.ambience()));
        assertEquals(2, music.startSection());
        assertTrue(music.full(4) && !music.full(3), "the full mix from section 4");
        assertTrue(music.bossSting().isPresent(), "the mini-boss sting at the arena halt");
        assertEquals("miniboss-sting", music.bossSting().orElseThrow());
        assertTrue(music.ambienceChangeList().isEmpty(), "the ocean from t=0 throughout");
    }

    @Test
    void theKrakensSoundsFollowTheirEventsAtMediumAndHard() {
        for (Difficulty difficulty : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            Set<SimEvents.Type> heard = EnumSet.noneOf(SimEvents.Type.class);
            Sortie sortie = new Sortie(
                    1,
                    SimSpecs.starterLoadout(CONTENT, difficulty),
                    SimSpecs.level(CONTENT, KEY, difficulty),
                    SimSpecs.rules(CONTENT, KEY, difficulty).withInvulnerableShip(),
                    60);
            BossSpec.Slam slam = sortie.script().setPieces().stream()
                    .flatMap(piece -> piece.boss().stream())
                    .flatMap(boss -> boss.arena().stream())
                    .map(BossSpec.Arena::slam)
                    .findFirst()
                    .orElseThrow();
            long[] now = {0};
            var bank =
                    new SfxBank(new MockAudio(), new Lwjgl3Files(), new Mixer(AudioSettings.defaults()), () -> now[0]);
            var sounds = new FlightSounds(
                    bank,
                    new EnemyLooks[0],
                    sortie.armament(),
                    sortie.script().setPieces().stream()
                            .map(vanguard.sim.LevelScript.SetPieceSpec::slug)
                            .toList());
            sounds.naval(sortie.navalConvoy());
            sounds.slamCycle(slam.telegraphSeconds(), slam.riseSeconds(), slam.secondSeconds());
            long telegraphStep = -1;
            long slamSoundStep = -1;
            long slamStep = -1;
            for (long t = 0; t < SimStep.ticks(185) && slamStep < 0; t++) {
                now[0] += NANOS_PER_STEP;
                sortie.step(0);
                sounds.step();
                SimEvents events = sortie.events();
                try {
                    sounds.play(events);
                } catch (ArrayIndexOutOfBoundsException lookless) {
                    // a kill of a lookless test fixture
                }
                sounds.watch(sortie);
                if (events.count(SimEvents.Type.TELEGRAPH) > 0) {
                    if (telegraphStep < 0) {
                        telegraphStep = t;
                        assertTrue(bank.playing(Sfx.KRAKEN_CHURN) > 0, "the churn at the telegraph");
                    }
                    heard.add(SimEvents.Type.TELEGRAPH);
                }
                if (slamSoundStep < 0 && bank.playing(Sfx.KRAKEN_SLAM) > 0) {
                    slamSoundStep = t;
                }
                if (events.count(SimEvents.Type.SURFACE) > 0) {
                    assertTrue(bank.playing(Sfx.KRAKEN_SURFACE) > 0, "the surfacing swell");
                    heard.add(SimEvents.Type.SURFACE);
                }
                if (events.count(SimEvents.Type.ALLY_FLAK) > 0) {
                    assertTrue(bank.playing(Sfx.FRIGATE_FLAK) > 0, "the frigate's flak");
                    heard.add(SimEvents.Type.ALLY_FLAK);
                }
                if (events.count(SimEvents.Type.ALLY_HIT) > 0) {
                    assertTrue(bank.playing(Sfx.SHIP_HIT) > 0, "the ship's hit");
                    heard.add(SimEvents.Type.ALLY_HIT);
                }
                if (events.count(SimEvents.Type.SLAM) > 0) {
                    slamStep = t;
                }
            }
            assertTrue(telegraphStep > 0 && slamStep > telegraphStep, difficulty + ": a slam came");
            assertEquals(
                    SimStep.ticks(slam.telegraphSeconds() + slam.riseSeconds()),
                    slamStep - telegraphStep,
                    1,
                    difficulty + ": telegraph and rise");
            // The sound starts 0.5 s before the first impact, so its own impact lands on it.
            assertEquals(
                    SimStep.ticks(FlightSounds.SLAM_IMPACT_SECONDS),
                    slamStep - slamSoundStep,
                    1,
                    difficulty + ": the slam sound's start");
            assertTrue(heard.contains(SimEvents.Type.ALLY_FLAK), difficulty + ": the flak was heard");
            bank.dispose();
        }
    }
}
