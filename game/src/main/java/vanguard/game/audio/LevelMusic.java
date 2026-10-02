package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.Optional;

/**
 * A level's music cues (design/audio/music): the setting's ambience from the start, the level
 * theme from its start section, a cut when the ship is destroyed and a one-second fade when the
 * level is won. The themes have no separate stems yet, so the full mix plays from the start
 * section on rather than the base stem alone. While a radio message is shown the music ducks by
 * 4 dB (design/audio, Mix groups).
 */
public final class LevelMusic implements Disposable {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final float AMBIENCE_VOLUME = 0.35f;
    private static final float FADE_SECONDS = 1;
    /** −4 dB. */
    private static final float DUCKED = (float) Math.pow(10, -4 / 20.0);
    /** The duck moves this much of the way per second, so it does not click. */
    private static final float DUCK_RATE = 4;

    private final Audio audio;
    private final Mixer mixer;
    private final FileHandle theme;
    private final SfxBank sfx;
    private final Sfx ambience;
    private final int startSection;
    private Optional<MusicStreamer> music = Optional.empty();
    private boolean cut;
    private float fade = -1;
    private float duck = 1;

    public LevelMusic(Audio audio, Mixer mixer, FileHandle theme, SfxBank sfx, Sfx ambience, int startSection) {
        this.audio = audio;
        this.mixer = mixer;
        this.theme = theme;
        this.sfx = sfx;
        this.ambience = ambience;
        this.startSection = startSection;
        sfx.loop(ambience, AMBIENCE_VOLUME);
    }

    /**
     * Starts the theme once the scroll reaches its section, and runs the fade and the duck.
     *
     * @param radio whether a radio message is shown
     */
    public void update(int section, float seconds, boolean radio) {
        if (music.isEmpty() && !cut && fade < 0 && section >= startSection) {
            music = MusicStreamer.play(audio, theme, MUSIC_VOLUME, mixer);
        }
        float target = radio ? DUCKED : 1;
        duck += (target - duck) * Math.min(1, DUCK_RATE * seconds);
        float level = fade >= 0 ? fade / FADE_SECONDS : 1;
        music.ifPresent(streamer -> streamer.setVolume(MUSIC_VOLUME * duck * level));
        if (fade >= 0) {
            fade = Math.max(0, fade - seconds);
            if (fade == 0) {
                stopTheme();
            }
        }
    }

    /** The ship was destroyed: the music cuts (the failure sting plays as a sound effect). */
    public void cut() {
        cut = true;
        stopTheme();
    }

    /** The level restarts: the theme waits for its section again. */
    public void restart() {
        cut = false;
        stopTheme();
    }

    /** The level is won: the theme fades out. */
    public void fadeOut() {
        fade = FADE_SECONDS;
    }

    private void stopTheme() {
        music.ifPresent(MusicStreamer::close);
        music = Optional.empty();
    }

    @Override
    public void dispose() {
        stopTheme();
        sfx.stop(ambience);
    }
}
