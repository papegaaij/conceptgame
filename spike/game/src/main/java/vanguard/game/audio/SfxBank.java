package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;

/**
 * All {@link Sfx} loaded as OpenAL buffers. Rapid repeats of one effect are throttled, and every
 * play that finds no free OpenAL source is counted as a dropout.
 */
public final class SfxBank implements Disposable {
    private final Map<Sfx, Sound> sounds = new EnumMap<>(Sfx.class);
    private final long[] lastPlayed = new long[Sfx.values().length];
    private int plays;
    private int dropouts;

    public SfxBank(Audio audio, Files files) {
        for (Sfx sfx : Sfx.values()) {
            sounds.put(sfx, audio.newSound(files.internal(sfx.path())));
        }
    }

    /** Plays unless the same effect started less than {@code minIntervalMillis} ago. */
    public void playThrottled(Sfx sfx, float volume, long minIntervalMillis) {
        long now = System.nanoTime();
        if (now - lastPlayed[sfx.ordinal()] >= minIntervalMillis * 1_000_000L) {
            lastPlayed[sfx.ordinal()] = now;
            play(sfx, volume, 1f);
        }
    }

    public void play(Sfx sfx, float volume, float pitch) {
        plays++;
        if (sounds.get(sfx).play(volume, pitch, 0f) == -1) {
            dropouts++;
        }
    }

    public int plays() {
        return plays;
    }

    public int dropouts() {
        return dropouts;
    }

    @Override
    public void dispose() {
        sounds.values().forEach(Sound::dispose);
    }
}
