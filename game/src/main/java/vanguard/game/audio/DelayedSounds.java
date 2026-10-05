package vanguard.game.audio;

import java.util.Arrays;

/**
 * Sound effects due some simulation steps from now: an act boss's chained death plays a burst with
 * each explosion of the chain, from tail to head over its 3 s. A fixed queue (no allocation); a sound
 * past its capacity is dropped. Advanced with the simulation steps; cleared when the level restarts.
 */
public final class DelayedSounds {
    /** The most sounds waiting at once. */
    static final int CAPACITY = 32;

    private final SfxBank bank;
    private final Sfx[] sounds = new Sfx[CAPACITY];
    private final float[] volumes = new float[CAPACITY];
    private final float[] pitches = new float[CAPACITY];
    private final float[] pans = new float[CAPACITY];
    private final int[] steps = new int[CAPACITY];
    private int count;

    public DelayedSounds(SfxBank bank) {
        this.bank = bank;
    }

    /** Plays {@code sfx} {@code after} steps from now (0: at the next step). */
    public void play(Sfx sfx, float volume, float pitch, float pan, int after) {
        if (count == CAPACITY) {
            return;
        }
        sounds[count] = sfx;
        volumes[count] = volume;
        pitches[count] = pitch;
        pans[count] = pan;
        steps[count] = after;
        count++;
    }

    /** Advances one simulation step and plays the sounds that are due. */
    public void step() {
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (steps[i] <= 0) {
                if (bank != null) {
                    bank.play(sounds[i], volumes[i], pitches[i], pans[i]);
                }
                continue;
            }
            sounds[kept] = sounds[i];
            volumes[kept] = volumes[i];
            pitches[kept] = pitches[i];
            pans[kept] = pans[i];
            steps[kept] = steps[i] - 1;
            kept++;
        }
        Arrays.fill(sounds, kept, count, null);
        count = kept;
    }

    /** How many sounds wait. */
    int waiting() {
        return count;
    }

    /** Forgets every waiting sound. */
    public void clear() {
        Arrays.fill(sounds, null);
        count = 0;
    }
}
