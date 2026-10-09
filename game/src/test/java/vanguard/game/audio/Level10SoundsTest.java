package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.game.settings.AudioSettings;
import vanguard.sim.Enemy;
import vanguard.sim.Flock;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;

/**
 * M5 part D's sounds (design/audio/sfx and music, Level 10): the Wraith's decloak and the swarm's
 * loop-back on their events, a swarm's rush once as it enters the screen, the lance started so its
 * impact lands on the hit, the theme's duck on the scripted loss (the lower of it and the radio's),
 * and the ambience crossfading to the ocean at section 4.
 */
class Level10SoundsTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(10).orElseThrow();

    @Test
    void theDecloakAndTheLoopBackPlayTheirSounds() {
        assertEquals(Sfx.WRAITH_DECLOAK, FlightSounds.eventSound(SimEvents.Type.DECLOAK));
        assertEquals(Sfx.MOTE_SWARM, FlightSounds.eventSound(SimEvents.Type.LOOP_BACK));
        assertNull(FlightSounds.eventSound(SimEvents.Type.LOSS_GLOW), "the glow is silent");
        assertNull(FlightSounds.eventSound(SimEvents.Type.SCRIPTED_LOSS), "the lance is timed ahead of it");
        assertEquals("sfx/wraith-decloak-r32-b.ogg", Sfx.WRAITH_DECLOAK.path(), "round 32's picks");
        assertEquals("sfx/mote-swarm-r32-b.ogg", Sfx.MOTE_SWARM.path());
        assertEquals("sfx/lance-r32-a.ogg", Sfx.LANCE_STRIKE.path());
        for (Sfx sfx : List.of(Sfx.WRAITH_DECLOAK, Sfx.MOTE_SWARM, Sfx.LANCE_STRIKE)) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx.path());
            assertTrue(sfx.priority().compareTo(Sfx.Priority.PLAYER_FIRE) > 0, sfx + " is heard over the guns");
        }
    }

    @Test
    void theLanceStartsSoItsImpactLandsOnTheHit() throws IOException {
        LevelScript.ScriptedLoss loss = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM)
                .escort()
                .flatMap(LevelScript.Escort::air)
                .flatMap(LevelScript.Air::scriptedLoss)
                .orElseThrow();
        assertEquals(118, loss.t(), 1e-9);
        assertEquals(1.2, FlightSounds.LANCE_IMPACT_SECONDS, 1e-9, "round 32's lance a: the impact 1.2 s in");
        double start = loss.t() - FlightSounds.LANCE_IMPACT_SECONDS;
        assertTrue(start > loss.t() - loss.glow(), "inside the glow");
        assertTrue(FlightSounds.lanceDue(start - 0.01, start + 0.01, loss.t()));
        assertTrue(FlightSounds.lanceDue(start - 1.0 / 60, start, loss.t()), "the step that reaches it");
        assertFalse(FlightSounds.lanceDue(start, start + 1.0 / 60, loss.t()), "once");
        assertFalse(FlightSounds.lanceDue(0, start - 0.01, loss.t()));
        assertFalse(FlightSounds.lanceDue(loss.t(), loss.t() + 1, loss.t()));
        // The file's loudest moment (lance a's thunder crack, round 32) sits at its impact.
        assertEquals(FlightSounds.LANCE_IMPACT_SECONDS, loudest(Sfx.LANCE_STRIKE), 0.25);
    }

    /** The start of a file's loudest 50 ms, seconds. */
    static double loudest(Sfx sfx) throws IOException {
        try (var file = new VorbisFile(Files.readAllBytes(ASSETS.resolve(sfx.path())))) {
            int window = file.sampleRate() / 20;
            short[] block = new short[window * file.channels()];
            double best = -1;
            long at = 0;
            long bestAt = 0;
            int read;
            while ((read = file.read(block, 0, window)) > 0) {
                double energy = 0;
                for (int i = 0; i < read * file.channels(); i++) {
                    energy += (double) block[i] * block[i];
                }
                if (energy > best) {
                    best = energy;
                    bestAt = at;
                }
                at += read;
            }
            return bestAt / (double) file.sampleRate();
        }
    }

    @Test
    void aSwarmMemberSeenOnceIsNotHeardAgain() {
        var cue = new SwarmCue();
        assertFalse(cue.seen(7));
        cue.note(7);
        cue.note(7);
        assertTrue(cue.seen(7));
        assertFalse(cue.seen(8));
        cue.reset();
        assertFalse(cue.seen(7), "a restart numbers its units afresh");
        for (int serial = 1; serial <= SwarmCue.REMEMBERED + 1; serial++) {
            cue.note(serial);
        }
        assertFalse(cue.seen(1), "the ring forgets the oldest");
        assertTrue(cue.seen(SwarmCue.REMEMBERED + 1));
    }

    /**
     * Level 10 flown without input: the swarm's rush plays exactly once for each swarm, in the step
     * its first member comes onto the screen, at that member.
     */
    @Test
    void eachSwarmIsHeardOnceAsItEnters() {
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        var cue = new SwarmCue();
        Set<Integer> seenMembers = new HashSet<>();
        int entries = 0;
        for (int t = 0; t < 60 * 100 && !sortie.complete(); t++) {
            sortie.step(0);
            int entering = 0;
            for (int f = 0; f < sortie.flockCount(); f++) {
                Flock flock = sortie.flock(f);
                boolean known = false;
                boolean shown = false;
                for (int i = 0; i < flock.size(); i++) {
                    Enemy member = flock.member(i);
                    if (member == null) {
                        continue;
                    }
                    known |= seenMembers.contains(member.serial());
                    shown |= PlayField.overlaps(member.renderX(1), member.renderY(1), member.hitbox());
                }
                if (shown && !known) {
                    entering++;
                    for (int i = 0; i < flock.size(); i++) {
                        if (flock.member(i) != null) {
                            seenMembers.add(flock.member(i).serial());
                        }
                    }
                }
            }
            boolean heard = cue.watch(sortie, t == 0);
            assertEquals(entering > 0, heard, "step " + t);
            entries += entering;
        }
        assertTrue(entries >= 2, "the swarms by 100 s: " + entries);
    }

    @Test
    void theScriptedDuckAndTheRadiosDuckDoNotAddUp() {
        float sixDb = (float) Math.pow(10, -6 / 20.0);
        assertEquals(1, LevelMusic.duckTarget(false, 1), 1e-6);
        assertEquals(LevelMusic.DUCKED, LevelMusic.duckTarget(true, 1), 1e-6, "the radio's −4 dB");
        assertEquals(sixDb, LevelMusic.duckTarget(false, sixDb), 1e-6, "the lance's −6 dB");
        assertEquals(sixDb, LevelMusic.duckTarget(true, sixDb), 1e-6, "the lower of the two");
        LevelData.Music.Duck duck = CONTENT.level(KEY).music().duck().orElseThrow();
        assertEquals(LevelData.Music.DuckOn.SCRIPTED_LOSS, duck.on());
        assertEquals(-6, duck.db(), 1e-9);
        assertEquals(3, duck.seconds(), 1e-9);
    }

    @Test
    void theAmbienceCrossfadesToTheOceanAtSectionFourAndBackOnARestart() {
        MockAudio audio = new MockAudio();
        Lwjgl3Files files = new Lwjgl3Files();
        Mixer mixer = new Mixer(AudioSettings.defaults());
        SfxBank bank = new SfxBank(audio, files, mixer);
        LevelData.Music data = CONTENT.level(KEY).music();
        assertEquals(Sfx.AMBIENCE_CITY, Sfx.ambience(data.ambience()));
        LevelData.Music.AmbienceChange change = data.ambienceChangeList().getFirst();
        assertEquals(4, change.section());
        assertEquals(Sfx.AMBIENCE_OCEAN, Sfx.ambience(change.ambience()));
        LevelMusic music = new LevelMusic(
                        audio,
                        mixer,
                        files.internal(Tracks.basePath("coalition-rising")),
                        files.internal(Tracks.path(5)),
                        bank,
                        Sfx.AMBIENCE_CITY,
                        2,
                        0,
                        section -> section >= 3,
                        Double.POSITIVE_INFINITY,
                        Optional.empty())
                .ambienceChanges(List.of(new LevelMusic.AmbienceChange(4, Sfx.AMBIENCE_OCEAN, change.crossfade())));
        music.update(3, 100, 0.5f, false);
        assertEquals(Sfx.AMBIENCE_CITY, music.ambience());
        assertFalse(bank.looping(Sfx.AMBIENCE_OCEAN));
        float full = bank.loopVolume(Sfx.AMBIENCE_CITY);
        music.update(4, 131, 1, false);
        assertEquals(Sfx.AMBIENCE_OCEAN, music.ambience());
        assertTrue(bank.looping(Sfx.AMBIENCE_CITY) && bank.looping(Sfx.AMBIENCE_OCEAN), "both under the crossfade");
        assertEquals(full / 4, bank.loopVolume(Sfx.AMBIENCE_OCEAN), 1e-4, "a quarter in after 1 s of 4");
        assertEquals(full * 3 / 4, bank.loopVolume(Sfx.AMBIENCE_CITY), 1e-4);
        music.update(4, 134, 3, false);
        assertFalse(bank.looping(Sfx.AMBIENCE_CITY), "the megacity stopped at the end");
        assertEquals(full, bank.loopVolume(Sfx.AMBIENCE_OCEAN), 1e-4);
        music.restart();
        assertEquals(Sfx.AMBIENCE_CITY, music.ambience());
        assertTrue(bank.looping(Sfx.AMBIENCE_CITY));
        assertFalse(bank.looping(Sfx.AMBIENCE_OCEAN));
        music.dispose();
        assertFalse(bank.looping(Sfx.AMBIENCE_CITY));
        bank.dispose();
    }
}
