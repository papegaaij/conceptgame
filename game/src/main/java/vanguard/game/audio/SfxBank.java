package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * All {@link Sfx} loaded as OpenAL buffers, carried over from the tech spike. Each effect keeps
 * the ids of its playing instances; a play beyond its instance limit stops the oldest one first.
 * The desktop launcher raises the OpenAL source count to 64, so the music stream and many effects
 * play at once.
 *
 * <p>Play only from the render thread: libGDX's OpenAL source pool, which the music streamer
 * shares, is not thread-safe (spike gate 5).
 */
public final class SfxBank implements Disposable {
    private final Map<Sfx, Sound> sounds = new EnumMap<>(Sfx.class);
    private final long[][] instances = new long[Sfx.values().length][];
    private final int[] next = new int[Sfx.values().length];

    public SfxBank(Audio audio, Files files) {
        for (Sfx sfx : Sfx.values()) {
            sounds.put(sfx, audio.newSound(files.internal(sfx.path())));
            instances[sfx.ordinal()] = new long[sfx.instanceLimit()];
            Arrays.fill(instances[sfx.ordinal()], -1);
        }
    }

    /**
     * @param volume 0..1
     * @param pitch 1 for the recorded pitch
     * @param pan -1 (left) to 1 (right)
     */
    public void play(Sfx sfx, float volume, float pitch, float pan) {
        Sound sound = sounds.get(sfx);
        long[] playing = instances[sfx.ordinal()];
        int slot = next[sfx.ordinal()];
        if (playing[slot] != -1) {
            sound.stop(playing[slot]);
        }
        playing[slot] = sound.play(volume, pitch, pan);
        next[sfx.ordinal()] = (slot + 1) % playing.length;
    }

    /** Loops {@code sfx} until {@link #stop(Sfx)}; it uses the effect's first instance slot. */
    public void loop(Sfx sfx, float volume) {
        stop(sfx);
        instances[sfx.ordinal()][0] = sounds.get(sfx).loop(volume);
    }

    /** Stops every playing instance of {@code sfx}. */
    public void stop(Sfx sfx) {
        sounds.get(sfx).stop();
        Arrays.fill(instances[sfx.ordinal()], -1);
    }

    @Override
    public void dispose() {
        sounds.values().forEach(Sound::dispose);
    }
}
