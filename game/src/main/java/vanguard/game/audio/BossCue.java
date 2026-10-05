package vanguard.game.audio;

import java.util.Arrays;

/**
 * An act boss's music as one stream (design/audio/music, Loops and transitions): the boss warning
 * (track 22) played once, and the boss track (track 18) with its loop points coming in on the
 * downbeat after the warning's bars, sample-exact, while the warning's tail rings out under it. Either
 * part may be missing: the warning alone then ends in silence, the boss track alone starts at once.
 */
final class BossCue implements PcmStream {
    private final PcmSource warning;
    private final PcmStream track;
    /** The output frame at which the boss track starts. */
    private final long trackFrom;

    private final int channels;
    private final int sampleRate;
    private long position;
    private boolean warningDone;
    /** The boss track's samples of the last read; reallocated only when the chunk size changes. */
    private short[] trackChunk = new short[0];

    /**
     * @param warning the warning, played once; null for none
     * @param track the boss track, endless; null for none
     * @param trackFrom the output frame the boss track starts at (0 without a warning)
     */
    BossCue(PcmSource warning, PcmStream track, long trackFrom) {
        if (warning == null && track == null) {
            throw new IllegalArgumentException("a boss cue has a warning or a boss track");
        }
        if (warning != null
                && track != null
                && (warning.channels() != track.channels() || warning.sampleRate() != track.sampleRate())) {
            throw new IllegalArgumentException("the warning and the boss track differ in format");
        }
        this.warning = warning;
        this.track = track;
        this.trackFrom = warning == null ? 0 : Math.max(0, trackFrom);
        channels = warning != null ? warning.channels() : track.channels();
        sampleRate = warning != null ? warning.sampleRate() : track.sampleRate();
        warningDone = warning == null;
    }

    @Override
    public void read(short[] out) {
        int frames = out.length / channels;
        Arrays.fill(out, (short) 0);
        int filled = 0;
        while (!warningDone && filled < frames) {
            int decoded = warning.read(out, filled * channels, frames - filled);
            if (decoded <= 0) {
                warningDone = true;
            } else {
                filled += decoded;
            }
        }
        if (track != null && position + frames > trackFrom) {
            int start = (int) Math.max(0, trackFrom - position);
            int length = (frames - start) * channels;
            if (trackChunk.length != length) {
                // Only the chunk where the track comes in is shorter than the others.
                trackChunk = new short[length];
            }
            track.read(trackChunk);
            for (int i = 0; i < length; i++) {
                int at = start * channels + i;
                out[at] = (short) Math.clamp(out[at] + trackChunk[i], Short.MIN_VALUE, Short.MAX_VALUE);
            }
        }
        position += frames;
    }

    @Override
    public int channels() {
        return channels;
    }

    @Override
    public int sampleRate() {
        return sampleRate;
    }

    @Override
    public void close() {
        if (warning != null) {
            warning.close();
        }
        if (track != null) {
            track.close();
        }
    }
}
