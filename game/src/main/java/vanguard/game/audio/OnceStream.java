package vanguard.game.audio;

import java.util.Arrays;

/**
 * Plays a source once, then silence: the sound test's jingles and cues, which have no loop.
 * {@link #ended} tells the render thread when the source and half a second of silence after it
 * have been written, so the device's queued buffers have played out too.
 */
final class OnceStream implements PcmStream {
    private static final double TAIL_SECONDS = 0.5;

    private final PcmSource source;
    private boolean drained;
    private long silence;
    private volatile boolean ended;

    OnceStream(PcmSource source) {
        this.source = source;
    }

    @Override
    public void read(short[] out) {
        int channels = source.channels();
        int frames = out.length / channels;
        int filled = 0;
        while (!drained && filled < frames) {
            int decoded = source.read(out, filled * channels, frames - filled);
            if (decoded <= 0) {
                drained = true;
            } else {
                filled += decoded;
            }
        }
        Arrays.fill(out, filled * channels, out.length, (short) 0);
        if (drained) {
            silence += frames - filled;
            ended = silence >= TAIL_SECONDS * source.sampleRate();
        }
    }

    /** Whether the source has played to its end. */
    boolean ended() {
        return ended;
    }

    @Override
    public int channels() {
        return source.channels();
    }

    @Override
    public int sampleRate() {
        return source.sampleRate();
    }

    @Override
    public void close() {
        source.close();
    }
}
