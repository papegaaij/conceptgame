package vanguard.game.audio;

/**
 * The voice limit of design/audio/sfx (Mixing rules): at most {@link #MAX_VOICES} sound effects at
 * once, and per effect at most its {@link Sfx#instanceLimit()}. A play beyond an effect's own limit
 * stops that effect's oldest instance; a play beyond the global limit steals the oldest voice of
 * the lowest {@link Sfx.Priority} at or below its own, or is dropped when every voice ranks above
 * it. A loop is never stolen (it would not come back) and always gets a voice. Libgdx cannot say
 * when a sound has ended, so a voice counts until its file's length (at its pitch) has passed.
 *
 * <p>Fixed arrays: admitting a sound never allocates. Not thread-safe, like the bank that owns it.
 */
final class VoiceLimit {
    /** The most effects playing at once (the desktop launcher opens 64 OpenAL sources; music streams use some). */
    static final int MAX_VOICES = 32;
    /** The end of a loop's voice: never. */
    static final long LOOPING = Long.MAX_VALUE;

    /** Stops a playing instance whose voice was stolen. */
    @FunctionalInterface
    interface Stopper {
        void stop(Sfx sfx, long id);
    }

    private final int capacity;
    private final Stopper stopper;
    private final Sfx[] sounds;
    private final long[] ids;
    /** When each voice ends, in the clock's nanoseconds; {@link #LOOPING} for a loop. */
    private final long[] ends;
    /** The order the voices started in: lower is older. */
    private final long[] orders;

    private int count;
    private long order;

    VoiceLimit(int capacity, Stopper stopper) {
        this.capacity = capacity;
        this.stopper = stopper;
        sounds = new Sfx[capacity];
        ids = new long[capacity];
        ends = new long[capacity];
        orders = new long[capacity];
    }

    /**
     * Makes room for a play of {@code sfx} at {@code now}, stopping what it steals; false when it must
     * not play. On true the caller plays it and reports it with {@link #started}.
     */
    boolean admit(Sfx sfx, long now, boolean loop) {
        expire(now);
        int same = 0;
        int oldestSame = -1;
        for (int i = 0; i < count; i++) {
            if (sounds[i] == sfx && ends[i] != LOOPING) {
                same++;
                if (oldestSame < 0 || orders[i] < orders[oldestSame]) {
                    oldestSame = i;
                }
            }
        }
        if (same >= sfx.instanceLimit()) {
            steal(oldestSame);
        }
        if (count < capacity) {
            return true;
        }
        int victim = -1;
        for (int i = 0; i < count; i++) {
            if (ends[i] != LOOPING && (victim < 0 || lower(i, victim))) {
                victim = i;
            }
        }
        if (victim < 0 || !loop && sounds[victim].priority().compareTo(sfx.priority()) > 0) {
            return false;
        }
        steal(victim);
        return true;
    }

    /** Whether voice {@code i} gives way before voice {@code j}: a lower priority, or the same and older. */
    private boolean lower(int i, int j) {
        int compared = sounds[i].priority().compareTo(sounds[j].priority());
        return compared < 0 || compared == 0 && orders[i] < orders[j];
    }

    /** Records an admitted play as a voice until {@code end} ({@link #LOOPING} for a loop). */
    void started(Sfx sfx, long id, long end) {
        if (count == capacity) {
            return;
        }
        sounds[count] = sfx;
        ids[count] = id;
        ends[count] = end;
        orders[count] = order++;
        count++;
    }

    /** Forgets every voice of {@code sfx}: the bank stopped it. */
    void stopped(Sfx sfx) {
        for (int i = count - 1; i >= 0; i--) {
            if (sounds[i] == sfx) {
                remove(i);
            }
        }
    }

    /** How many voices play at {@code now}. */
    int playing(long now) {
        expire(now);
        return count;
    }

    /** How many voices of {@code sfx} play at {@code now}. */
    int playing(Sfx sfx, long now) {
        expire(now);
        int found = 0;
        for (int i = 0; i < count; i++) {
            if (sounds[i] == sfx) {
                found++;
            }
        }
        return found;
    }

    private void expire(long now) {
        for (int i = count - 1; i >= 0; i--) {
            if (ends[i] != LOOPING && ends[i] <= now) {
                remove(i);
            }
        }
    }

    private void steal(int i) {
        stopper.stop(sounds[i], ids[i]);
        remove(i);
    }

    private void remove(int i) {
        count--;
        sounds[i] = sounds[count];
        ids[i] = ids[count];
        ends[i] = ends[count];
        orders[i] = orders[count];
        sounds[count] = null;
    }
}
