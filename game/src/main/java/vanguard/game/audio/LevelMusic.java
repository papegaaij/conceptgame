package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * A level's music cues (design/audio/music): the setting's ambience from the start, the level
 * theme from its start section (at the level's start level through that section, rising to full in
 * two seconds at the next one), a cut when the ship is destroyed and a one-second fade when the
 * level is won. The theme plays as two sample-aligned stems: the base stem alone until the level's
 * full section, where it crossfades in a second to the full mix (Intensity layers); a section can
 * override that either way (Level 03 drops to the base stem under the Leviathan's first pass and
 * forces the full mix for its second), crossfading the same way. While a radio
 * message is shown the music ducks by 4 dB (design/audio, Mix groups). A level may end on its
 * ambience alone: from its {@code ambience_from} time the theme fades out as at a won level
 * (Level 06), and loop a radio line faintly under one section (Level 06's perimeter beacon).
 */
public final class LevelMusic implements Disposable {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final float AMBIENCE_VOLUME = 0.35f;
    private static final float FADE_SECONDS = 1;
    /** The intensity layer fades in over a second (design/audio/music, Intensity layers). */
    private static final double CROSSFADE_SECONDS = 1;
    /** The start level rises to full over this long. */
    private static final float RISE_SECONDS = 2;
    /** −4 dB. */
    public static final float DUCKED = (float) Math.pow(10, -4 / 20.0);
    /** The duck moves this much of the way per second, so it does not click. */
    public static final float DUCK_RATE = 4;

    private final Audio audio;
    private final Mixer mixer;
    private final FileHandle base;
    private final FileHandle full;
    private final SfxBank sfx;
    private final Sfx ambience;
    private final int startSection;
    private final float startLevel;
    private final IntPredicate fullMix;
    private Optional<MusicStreamer> music = Optional.empty();
    private Optional<StemMix> stems = Optional.empty();
    private boolean cut;
    private float fade = -1;
    private float duck = 1;
    private float rise;
    /** Seconds left of a sting over the theme; negative without one. */
    private float sting = -1;
    /** A sting's length, its 0.5 s crossfades included (track 21 is 4.6 s). */
    private static final float STING_SECONDS = 4.6f;

    private static final float STING_FADE = 0.5f;

    /** A radio line looping under a section: its file, the section and its gain below full. */
    public record VoiceLoop(FileHandle file, int section, float gain) {}

    private final double ambienceFrom;
    private final Optional<VoiceLoop> voiceLoop;
    private Sound loopSound;
    private long loopId = -1;

    /**
     * @param base the theme's base stem
     * @param full the theme's full mix, sample-aligned with the base stem
     * @param startSection the section the theme starts in
     * @param startDb the theme's level through its start section, dB (0 for full)
     * @param fullMix whether a section (1-based) plays the full mix rather than the base stem
     */
    public LevelMusic(
            Audio audio,
            Mixer mixer,
            FileHandle base,
            FileHandle full,
            SfxBank sfx,
            Sfx ambience,
            int startSection,
            double startDb,
            IntPredicate fullMix,
            double ambienceFrom,
            Optional<VoiceLoop> voiceLoop) {
        this.audio = audio;
        this.mixer = mixer;
        this.base = base;
        this.full = full;
        this.sfx = sfx;
        this.ambience = ambience;
        this.startSection = startSection;
        startLevel = (float) Math.pow(10, startDb / 20);
        rise = startLevel;
        this.fullMix = fullMix;
        this.ambienceFrom = ambienceFrom;
        this.voiceLoop = voiceLoop;
        sfx.loop(ambience, AMBIENCE_VOLUME);
    }

    /**
     * Starts the theme once the scroll reaches its section, raises it to full after that section,
     * crossfades between the base stem and the full mix as the sections ask, and runs the fade and
     * the duck.
     *
     * @param levelSeconds the level time: from {@code ambience_from} on only the ambience plays
     * @param radio whether a radio message is shown
     */
    public void update(int section, double levelSeconds, float seconds, boolean radio) {
        if (levelSeconds >= ambienceFrom && fade < 0 && music.isPresent()) {
            fadeOut();
        }
        loop(section);
        if (music.isEmpty()
                && !cut
                && fade < 0
                && levelSeconds < ambienceFrom
                && section >= startSection
                && MusicStreamer.available(audio)) {
            StemMix mix = StemMix.of(
                    new VorbisFile(base.readBytes()),
                    new VorbisFile(full.readBytes()),
                    CROSSFADE_SECONDS,
                    fullMix.test(section) ? 1 : 0);
            stems = Optional.of(mix);
            music = Optional.of(MusicStreamer.play(audio, mix, MUSIC_VOLUME * rise, mixer));
        }
        if (stems.isPresent()) {
            stems.get().fadeTo(fullMix.test(section) ? 1 : 0);
        }
        if (section > startSection) {
            rise = Math.min(1, rise + seconds * (1 - startLevel) / RISE_SECONDS);
        }
        float target = radio ? DUCKED : 1;
        duck += (target - duck) * Math.min(1, DUCK_RATE * seconds);
        float level = rise * (fade >= 0 ? fade / FADE_SECONDS : 1) * stingDip();
        if (sting >= 0) {
            sting -= seconds;
        }
        music.ifPresent(streamer -> streamer.setVolume(MUSIC_VOLUME * duck * level));
        if (fade >= 0) {
            fade = Math.max(0, fade - seconds);
            if (fade == 0) {
                stopTheme();
            }
        }
    }

    /**
     * A boss arrived (design/audio/music, Level 05's mini-boss): {@code sting} cuts in over a 0.5 s
     * crossfade from the theme, which comes back after it.
     */
    public void sting(Sfx stingSound) {
        sfx.play(stingSound, 1, 1, 0);
        sting = STING_SECONDS;
    }

    /** The theme's level under a sting: down over the first half second, back over the last. */
    private float stingDip() {
        if (sting < 0) {
            return 1;
        }
        float in = STING_SECONDS - sting;
        return Math.clamp(Math.max(1 - in / STING_FADE, 1 - sting / STING_FADE), 0, 1);
    }

    /** The voice loop plays while its section does (and the music has not been cut). */
    private void loop(int section) {
        if (voiceLoop.isEmpty()) {
            return;
        }
        VoiceLoop line = voiceLoop.get();
        boolean wanted = section == line.section() && !cut && line.file().exists();
        if (wanted && loopSound == null) {
            try {
                loopSound = audio.newSound(line.file());
                loopId = loopSound.loop(line.gain() * mixer.gain(Bus.VOICE));
            } catch (RuntimeException e) {
                loopSound = null;
            }
        } else if (!wanted && loopSound != null) {
            stopLoop();
        }
    }

    private void stopLoop() {
        if (loopSound != null) {
            loopSound.stop(loopId);
            loopSound.dispose();
            loopSound = null;
            loopId = -1;
        }
    }

    /** The ship was destroyed: the music cuts (the failure sting plays as a sound effect). */
    public void cut() {
        cut = true;
        stopTheme();
        stopLoop();
    }

    /** The level restarts: the theme waits for its section again, at its start level. */
    public void restart() {
        cut = false;
        sting = -1;
        rise = startLevel;
        fade = -1;
        stopTheme();
        stopLoop();
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
        stopLoop();
        sfx.stop(ambience);
    }
}
