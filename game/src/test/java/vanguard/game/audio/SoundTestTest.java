package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vanguard.game.settings.AudioSettings;

class SoundTestTest {
    private final MockAudio audio = new MockAudio();
    private final Lwjgl3Files files = new Lwjgl3Files();
    private final Mixer mixer = new Mixer(AudioSettings.defaults());
    private final SoundTest test = new SoundTest(audio, files, mixer, new SfxBank(audio, files, mixer));

    @Test
    void theTracksAreTheMusicFilesTheGameHasAndTheEffectsTheRest() {
        for (SoundTest.Track track : SoundTest.TRACKS) {
            assertTrue(Files.exists(Path.of("../assets").resolve(track.path())), track.path());
        }
        assertTrue(SoundTest.EFFECTS.stream().noneMatch(sfx -> sfx.bus() == Bus.MUSIC), "the cues are tracks");
        assertEquals(
                Sfx.values().length,
                SoundTest.EFFECTS.size()
                        + SoundTest.TRACKS.stream()
                                .filter(track -> !track.loops())
                                .count());
    }

    @Test
    void confirmStartsTheTrackAndStopsItAgain() {
        test.play(SoundTest.Kind.MUSIC);
        assertTrue(test.playing());

        test.pick(SoundTest.Kind.MUSIC, 1);
        assertFalse(test.playing(), "the picked track is not the one playing");
        test.play(SoundTest.Kind.MUSIC);
        assertTrue(test.playing(), "it replaces the one playing");

        test.play(SoundTest.Kind.MUSIC);
        assertFalse(test.playing());
    }

    @Test
    void aSoloStreamSilencesTheOtherMusic() {
        mixer.solo(true);

        assertEquals(0, mixer.musicGain(false));
        assertEquals(1, mixer.musicGain(true));

        mixer.solo(false);
        assertEquals(1, mixer.musicGain(false));
    }

    @Test
    void theNoteSaysWhereATrackPlaysAndWhichVolumeAnEffectFollows() {
        assertEquals("TITLE THEME: TITLE SCREEN AND MAIN MENU", test.note(SoundTest.Kind.MUSIC));
        test.pick(SoundTest.Kind.EFFECTS, SoundTest.EFFECTS.indexOf(Sfx.TYPEWRITER));
        assertEquals("FOLLOWS THE RADIO BLIPS VOLUME", test.note(SoundTest.Kind.EFFECTS));
    }
}
