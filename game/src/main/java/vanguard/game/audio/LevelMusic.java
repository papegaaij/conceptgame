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
 * (Level 06), and loop a radio line faintly under one section (Level 06's perimeter beacon). M5 part
 * C: a run-time hook ({@code full_on}) plays the full mix while the sortie asks for it whatever the
 * section (Level 09: while a hold zone runs and from the collapse on), fading in over a second as it
 * starts and out over {@value #RUN_TIME_OUT_SECONDS} s after it ends.
 *
 * <p>An act boss (Level 07) has its own music: at its arrival the theme crossfades out over 0.5 s
 * while the boss warning (track 22) starts, and the boss track (track 18) comes in on the downbeat
 * after the warning's bars ({@link BossCue}); at the kill it fades out in a second, leaving the
 * ambience, and the theme does not come back. A Retry from boss holds the theme until the boss
 * arrives again. A level without boss music keeps the mid-boss sting ({@link #sting}).
 */
public final class LevelMusic implements Disposable {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final float AMBIENCE_VOLUME = 0.35f;
    private static final float FADE_SECONDS = 1;
    /** The intensity layer fades in over a second (design/audio/music, Intensity layers). */
    private static final double CROSSFADE_SECONDS = 1;
    /** M5 part C: the run-time hook's full mix fades back to the base stem over this long (design/audio/music). */
    static final double RUN_TIME_OUT_SECONDS = 4;
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
    /** The full mix's share the stems were last sent to. */
    private float stemTarget;
    /** Whether the run-time hook asked for the full mix at the last update. */
    private boolean intense;

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

    /**
     * An act boss's music: the warning played once at its arrival (track 22), the boss track looping
     * from {@code trackFromSeconds} into it (track 18); either may be missing.
     */
    public record Boss(Optional<FileHandle> warning, Optional<FileHandle> track, double trackFromSeconds) {
        public Boss {
            if (warning.isEmpty() && track.isEmpty()) {
                throw new IllegalArgumentException("boss music has a warning or a track");
            }
        }
    }

    /** Where the boss music stands. */
    enum BossState {
        /** Before the boss: the theme plays as its sections ask. */
        WAITING,
        /** A Retry from boss restarted the level just before the boss: the theme waits for it. */
        HELD,
        /** The boss's warning and track play; the theme is out. */
        FIGHT,
        /** The boss is down: its track has faded, only the ambience plays. */
        DOWN
    }

    /** The theme crossfades out under the boss warning over this long (design/audio/music). */
    private static final float BOSS_CROSSFADE = 0.5f;

    private final Optional<Boss> boss;
    private BossState bossState = BossState.WAITING;
    private Optional<MusicStreamer> bossMusic = Optional.empty();
    /** Seconds left of the theme's crossfade out under the warning; negative when not fading. */
    private float themeOut = -1;
    /** Seconds left of the boss music's fade at the kill; negative when not fading. */
    private float bossOut = -1;

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
        this(
                audio,
                mixer,
                base,
                full,
                sfx,
                ambience,
                startSection,
                startDb,
                fullMix,
                ambienceFrom,
                voiceLoop,
                Optional.empty());
    }

    /** @param boss an act boss's music, empty for none (a mid-boss's sting plays through {@link #sting}) */
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
            Optional<VoiceLoop> voiceLoop,
            Optional<Boss> boss) {
        this.boss = boss;
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
        update(section, levelSeconds, seconds, radio, false);
    }

    /**
     * As {@link #update(int, double, float, boolean)}, with the run-time hook (M5 part C, {@code
     * full_on}).
     *
     * @param runTimeFull whether the sortie's state asks for the full mix now (a hold zone runs, the
     *     collapse has started), whatever the section
     */
    public void update(int section, double levelSeconds, float seconds, boolean radio, boolean runTimeFull) {
        boolean wasIntense = intense;
        intense = runTimeFull;
        boolean wanted = fullMix.test(section) || runTimeFull;
        if (levelSeconds >= ambienceFrom && fade < 0 && music.isPresent()) {
            fadeOut();
        }
        loop(section);
        if (music.isEmpty()
                && !cut
                && fade < 0
                && bossState == BossState.WAITING
                && levelSeconds < ambienceFrom
                && section >= startSection
                && MusicStreamer.available(audio)) {
            StemMix mix = StemMix.of(
                    new VorbisFile(base.readBytes()),
                    new VorbisFile(full.readBytes()),
                    CROSSFADE_SECONDS,
                    wanted ? 1 : 0);
            stemTarget = wanted ? 1 : 0;
            stems = Optional.of(mix);
            music = Optional.of(MusicStreamer.play(audio, mix, MUSIC_VOLUME * rise, mixer));
        }
        float share = wanted ? 1 : 0;
        if (stems.isPresent() && share != stemTarget) {
            stems.get().fadeTo(share, fadeSeconds(share, wasIntense && !runTimeFull));
            stemTarget = share;
        }
        if (section > startSection) {
            rise = Math.min(1, rise + seconds * (1 - startLevel) / RISE_SECONDS);
        }
        float target = radio ? DUCKED : 1;
        duck += (target - duck) * Math.min(1, DUCK_RATE * seconds);
        float faded = fade >= 0 ? fade / FADE_SECONDS : 1;
        float level = rise * faded * stingDip() * (themeOut >= 0 ? themeOut / BOSS_CROSSFADE : 1);
        if (sting >= 0) {
            sting -= seconds;
        }
        music.ifPresent(streamer -> streamer.setVolume(MUSIC_VOLUME * duck * level));
        float bossLevel = faded * (bossOut >= 0 ? bossOut / FADE_SECONDS : 1);
        bossMusic.ifPresent(streamer -> streamer.setVolume(MUSIC_VOLUME * duck * bossLevel));
        if (themeOut >= 0) {
            themeOut = Math.max(0, themeOut - seconds);
            if (themeOut == 0) {
                themeOut = -1;
                stopTheme();
            }
        }
        if (bossOut >= 0) {
            bossOut = Math.max(0, bossOut - seconds);
            if (bossOut == 0) {
                bossOut = -1;
                stopBoss();
            }
        }
        if (fade >= 0) {
            fade = Math.max(0, fade - seconds);
            if (fade == 0) {
                stopTheme();
                stopBoss();
            }
        }
    }

    /**
     * How long a stem change takes (design/audio/music, Intensity layers): a second, but the run-time
     * hook's full mix fades back to the base stem over {@value #RUN_TIME_OUT_SECONDS} s.
     *
     * @param target the full mix's share it fades to
     * @param runTimeEnded whether the run-time hook just stopped asking for the full mix
     */
    static double fadeSeconds(float target, boolean runTimeEnded) {
        return target < 1 && runTimeEnded ? RUN_TIME_OUT_SECONDS : CROSSFADE_SECONDS;
    }

    /**
     * An act boss arrived: the theme crossfades out over 0.5 s while the boss warning starts, the
     * boss track coming in after the warning's bars. Returns false, doing nothing, for a level without
     * boss music (its mid-boss plays the sting instead).
     */
    public boolean bossArrived() {
        if (boss.isEmpty()) {
            return false;
        }
        if (bossState == BossState.FIGHT || cut) {
            return true;
        }
        bossState = BossState.FIGHT;
        if (music.isPresent()) {
            themeOut = BOSS_CROSSFADE;
        }
        sting = -1;
        stopBoss();
        bossOut = -1;
        if (MusicStreamer.available(audio)) {
            BossCue cue = cue(boss.get());
            if (cue != null) {
                bossMusic = Optional.of(MusicStreamer.play(audio, cue, MUSIC_VOLUME * duck, mixer));
            }
        }
        return true;
    }

    /** The boss's warning and track as one stream, from the files that exist; null when none does. */
    private static BossCue cue(Boss boss) {
        VorbisFile warning = boss.warning()
                .filter(FileHandle::exists)
                .map(file -> new VorbisFile(file.readBytes()))
                .orElse(null);
        LoopingStream track = boss.track()
                .filter(FileHandle::exists)
                .map(file -> LoopingStream.of(new VorbisFile(file.readBytes())))
                .orElse(null);
        if (warning == null && track == null) {
            return null;
        }
        int rate = warning != null ? warning.sampleRate() : track.sampleRate();
        return new BossCue(warning, track, Math.round(boss.trackFromSeconds() * rate));
    }

    /** The act boss is down: its music fades out in a second; the ambience plays on and the theme stays out. */
    public void bossDown() {
        if (boss.isEmpty() || bossState != BossState.FIGHT) {
            return;
        }
        bossState = BossState.DOWN;
        if (bossMusic.isPresent()) {
            bossOut = FADE_SECONDS;
        }
    }

    /** A Retry from boss restarts the level at the boss checkpoint: the theme waits for the boss's arrival. */
    public void bossRetry() {
        if (boss.isPresent()) {
            bossState = BossState.HELD;
            stopTheme();
        }
    }

    /** Where the boss music stands, for the tests. */
    BossState bossState() {
        return bossState;
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
        stopBoss();
        stopLoop();
    }

    /** The level restarts: the theme waits for its section again, at its start level. */
    public void restart() {
        cut = false;
        sting = -1;
        rise = startLevel;
        fade = -1;
        themeOut = -1;
        bossOut = -1;
        bossState = BossState.WAITING;
        stopTheme();
        stopBoss();
        stopLoop();
    }

    /** The level is won: the theme (or the boss music) fades out. */
    public void fadeOut() {
        fade = FADE_SECONDS;
    }

    private void stopTheme() {
        music.ifPresent(MusicStreamer::close);
        music = Optional.empty();
        stems = Optional.empty();
        intense = false;
    }

    private void stopBoss() {
        bossMusic.ifPresent(MusicStreamer::close);
        bossMusic = Optional.empty();
    }

    @Override
    public void dispose() {
        stopTheme();
        stopBoss();
        stopLoop();
        sfx.stop(ambience);
    }
}
