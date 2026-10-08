package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * All {@link Sfx} loaded as OpenAL buffers, carried over from the tech spike. Every play goes
 * through the {@link VoiceLimit}: at most 32 effects at once, each within its instance limit, the
 * lowest-priority oldest voice stolen first (design/audio/sfx, Mixing rules). Every play is scaled
 * by the {@link Mixer}'s gain for the effect's bus; loops follow a change. The desktop launcher
 * raises the OpenAL source count to 64, so the music stream and many effects play at once.
 *
 * <p>Play only from the render thread: libGDX's OpenAL source pool, which the music streamer
 * shares, is not thread-safe (spike gate 5).
 */
public final class SfxBank implements Disposable, Mixer.Listener {
    /** A file's length when it cannot be read (a test's mock files): long enough to count as playing. */
    static final long UNKNOWN_NANOS = 1_000_000_000L;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Map<Sfx, Sound> sounds = new EnumMap<>(Sfx.class);
    /** Each effect's length at its recorded pitch. */
    private final long[] lengths = new long[Sfx.values().length];
    /** Each running loop's instance. */
    private final long[] loopIds = new long[Sfx.values().length];
    /** The volume of each running loop before the mixer's gain; negative when it is not looping. */
    private final float[] loopVolumes = new float[Sfx.values().length];

    private final Mixer mixer;
    private final LongSupplier clock;
    private final VoiceLimit voices;

    public SfxBank(Audio audio, Files files, Mixer mixer) {
        this(audio, files, mixer, System::nanoTime);
    }

    /** @param clock the time in nanoseconds, for the voices' ends */
    SfxBank(Audio audio, Files files, Mixer mixer, LongSupplier clock) {
        this.mixer = mixer;
        this.clock = clock;
        voices = new VoiceLimit(
                VoiceLimit.MAX_VOICES, (sfx, id) -> sounds.get(sfx).stop(id));
        Arrays.fill(loopVolumes, -1);
        mixer.listen(this);
        for (Sfx sfx : Sfx.values()) {
            FileHandle file = files.internal(sfx.path());
            sounds.put(sfx, audio.newSound(file));
            lengths[sfx.ordinal()] = file.exists() ? oggNanos(file.readBytes()) : UNKNOWN_NANOS;
        }
    }

    /**
     * Plays {@code sfx} once, unless the voice limit drops it.
     *
     * @param volume 0..1
     * @param pitch 1 for the recorded pitch
     * @param pan -1 (left) to 1 (right)
     */
    public void play(Sfx sfx, float volume, float pitch, float pan) {
        long now = clock.getAsLong();
        if (!voices.admit(sfx, now, false)) {
            return;
        }
        long id = sounds.get(sfx).play(volume * mixer.gain(sfx.bus()), pitch, pan);
        if (id != -1) {
            voices.started(sfx, id, now + (long) (lengths[sfx.ordinal()] / Math.max(0.01f, pitch)));
        }
    }

    /** Loops {@code sfx} until {@link #stop(Sfx)}; a loop always gets a voice and keeps it. */
    public void loop(Sfx sfx, float volume) {
        stop(sfx);
        voices.admit(sfx, clock.getAsLong(), true);
        long id = sounds.get(sfx).loop(volume * mixer.gain(sfx.bus()));
        if (id != -1) {
            voices.started(sfx, id, VoiceLimit.LOOPING);
        }
        loopIds[sfx.ordinal()] = id;
        loopVolumes[sfx.ordinal()] = volume;
    }

    /** Sets a running loop's volume (before the mixer's gain); nothing when it is not looping. */
    public void loopVolume(Sfx sfx, float volume) {
        if (loopVolumes[sfx.ordinal()] < 0) {
            return;
        }
        loopVolumes[sfx.ordinal()] = volume;
        sounds.get(sfx).setVolume(loopIds[sfx.ordinal()], volume * mixer.gain(sfx.bus()));
    }

    /** A running loop's volume before the mixer's gain; negative when it is not looping. */
    float loopVolume(Sfx sfx) {
        return loopVolumes[sfx.ordinal()];
    }

    /** Whether {@code sfx} is looping. */
    public boolean looping(Sfx sfx) {
        return loopVolumes[sfx.ordinal()] >= 0;
    }

    /** Stops every playing instance of {@code sfx}. */
    public void stop(Sfx sfx) {
        sounds.get(sfx).stop();
        voices.stopped(sfx);
        loopVolumes[sfx.ordinal()] = -1;
    }

    /** How many effects play now (for tests and the voice limit's checks). */
    int playing() {
        return voices.playing(clock.getAsLong());
    }

    /** How many instances of {@code sfx} play now (for tests). */
    int playing(Sfx sfx) {
        return voices.playing(sfx, clock.getAsLong());
    }

    @Override
    public void gainsChanged() {
        for (Sfx sfx : Sfx.values()) {
            float volume = loopVolumes[sfx.ordinal()];
            if (volume >= 0) {
                sounds.get(sfx).setVolume(loopIds[sfx.ordinal()], volume * mixer.gain(sfx.bus()));
            }
        }
    }

    @Override
    public void dispose() {
        mixer.forget(this);
        sounds.values().forEach(Sound::dispose);
    }

    /**
     * An Ogg Vorbis file's length in nanoseconds: the last page's granule position (its last
     * sample) over the sample rate of the identification header; {@link #UNKNOWN_NANOS} when the
     * bytes are not Ogg Vorbis.
     */
    static long oggNanos(byte[] ogg) {
        if (ogg.length < 28 || !page(ogg, 0)) {
            return UNKNOWN_NANOS;
        }
        int packet = 27 + (ogg[26] & 0xFF);
        if (packet + 16 > ogg.length || ogg[packet] != 1 || ogg[packet + 1] != 'v') {
            return UNKNOWN_NANOS;
        }
        long rate = littleEndian(ogg, packet + 12, 4);
        if (rate <= 0) {
            return UNKNOWN_NANOS;
        }
        for (int i = ogg.length - 27; i >= 0; i--) {
            if (page(ogg, i)) {
                long granule = littleEndian(ogg, i + 6, 8);
                if (granule > 0) {
                    return granule * NANOS_PER_SECOND / rate;
                }
            }
        }
        return UNKNOWN_NANOS;
    }

    private static boolean page(byte[] bytes, int at) {
        return bytes[at] == 'O' && bytes[at + 1] == 'g' && bytes[at + 2] == 'g' && bytes[at + 3] == 'S';
    }

    private static long littleEndian(byte[] bytes, int at, int length) {
        long value = 0;
        for (int k = length - 1; k >= 0; k--) {
            value = value << 8 | bytes[at + k] & 0xFF;
        }
        return value;
    }
}
