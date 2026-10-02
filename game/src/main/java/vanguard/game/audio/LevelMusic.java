package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.Optional;

/**
 * A level's music cues (design/audio/music): the setting's ambience from the start, the level
 * theme from its start section, a cut when the ship is destroyed and a one-second fade when the
 * level is won. The theme plays as two sample-aligned stems: the base stem alone until the level's
 * full section, where it crossfades in a second to the full mix (Intensity layers). While a radio
 * message is shown the music ducks by 4 dB (design/audio, Mix groups).
 */
public final class LevelMusic implements Disposable {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final float AMBIENCE_VOLUME = 0.35f;
    private static final float FADE_SECONDS = 1;
    /** The intensity layer fades in over a second (design/audio/music, Intensity layers). */
    private static final double CROSSFADE_SECONDS = 1;
    /** −4 dB. */
    private static final float DUCKED = (float) Math.pow(10, -4 / 20.0);
    /** The duck moves this much of the way per second, so it does not click. */
    private static final float DUCK_RATE = 4;

    private final Audio audio;
    private final Mixer mixer;
    private final FileHandle base;
    private final FileHandle full;
    private final SfxBank sfx;
    private final Sfx ambience;
    private final int startSection;
    private final int fullSection;
    private Optional<MusicStreamer> music = Optional.empty();
    private Optional<StemMix> stems = Optional.empty();
    private boolean cut;
    private float fade = -1;
    private float duck = 1;

    /**
     * @param base the theme's base stem
     * @param full the theme's full mix, sample-aligned with the base stem
     * @param startSection the section the theme starts in
     * @param fullSection the section from which the full mix plays
     */
    public LevelMusic(
            Audio audio,
            Mixer mixer,
            FileHandle base,
            FileHandle full,
            SfxBank sfx,
            Sfx ambience,
            int startSection,
            int fullSection) {
        this.audio = audio;
        this.mixer = mixer;
        this.base = base;
        this.full = full;
        this.sfx = sfx;
        this.ambience = ambience;
        this.startSection = startSection;
        this.fullSection = fullSection;
        sfx.loop(ambience, AMBIENCE_VOLUME);
    }

    /**
     * Starts the theme once the scroll reaches its section, crossfades to the full mix at its
     * section, and runs the fade and the duck.
     *
     * @param radio whether a radio message is shown
     */
    public void update(int section, float seconds, boolean radio) {
        if (music.isEmpty() && !cut && fade < 0 && section >= startSection && MusicStreamer.available(audio)) {
            StemMix mix = StemMix.of(
                    new VorbisFile(base.readBytes()),
                    new VorbisFile(full.readBytes()),
                    CROSSFADE_SECONDS,
                    section >= fullSection ? 1 : 0);
            stems = Optional.of(mix);
            music = Optional.of(MusicStreamer.play(audio, mix, MUSIC_VOLUME, mixer));
        }
        if (section >= fullSection) {
            stems.ifPresent(mix -> mix.fadeTo(1));
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
        stems = Optional.empty();
    }

    @Override
    public void dispose() {
        stopTheme();
        sfx.stop(ambience);
    }
}
