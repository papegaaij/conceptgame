package vanguard.game.audio;

/**
 * Two sample-aligned stems of a level theme played in lockstep (design/audio/music, Intensity
 * layers): the base stem and the full mix, which is the intensity layer's "on" state. Both loop
 * at the same points, so they stay aligned forever; the output crossfades linearly between them
 * (the base is part of the full mix, so a linear fade keeps it at a constant level). The mix moves
 * towards its target by a fixed step per frame, so a change never clicks. The target is set from
 * the render thread, the samples are read on the music thread.
 */
public final class StemMix implements PcmStream {
    private final PcmStream base;
    private final PcmStream full;
    /** The mix moves by this much per frame: a whole crossfade takes the fade time. */
    private final float stepPerFrame;
    /** The step of the change under way: {@link #stepPerFrame}, or a fade time of its own ({@link #fadeTo(float, double)}). */
    private volatile float step;

    private volatile float target;
    private float mix;
    /** The stems' samples of the last read; reallocated only when the chunk size changes. */
    private short[] baseChunk = new short[0];

    private short[] fullChunk = new short[0];

    /** @param fadeSeconds how long a whole crossfade takes */
    StemMix(PcmStream base, PcmStream full, double fadeSeconds) {
        if (base.channels() != full.channels() || base.sampleRate() != full.sampleRate()) {
            throw new IllegalArgumentException("the stems differ in format");
        }
        this.base = base;
        this.full = full;
        stepPerFrame = (float) (1 / (fadeSeconds * base.sampleRate()));
        step = stepPerFrame;
    }

    /**
     * The stems of a theme; they must have the same length and loop points, so they stay sample-aligned.
     *
     * @param fullShare where the mix starts: 0 = the base stem alone, 1 = the full mix
     */
    static StemMix of(VorbisFile base, VorbisFile full, double fadeSeconds, float fullShare) {
        if (base.frameCount() != full.frameCount() || !base.loopPoints().equals(full.loopPoints())) {
            throw new IllegalArgumentException("the stems are not sample-aligned");
        }
        StemMix stems = new StemMix(LoopingStream.of(base), LoopingStream.of(full), fadeSeconds);
        stems.target = fullShare;
        stems.mix = fullShare;
        return stems;
    }

    /** Crossfades towards {@code share} of the full mix: 0 = the base stem alone, 1 = the full mix. */
    public void fadeTo(float share) {
        step = stepPerFrame;
        target = Math.clamp(share, 0, 1);
    }

    /**
     * Crossfades towards {@code share} with a whole crossfade taking {@code fadeSeconds} (M5 part C:
     * a hold zone's full mix fades out over 4 s, design/audio/music); it keeps that pace until the
     * next change.
     */
    public void fadeTo(float share, double fadeSeconds) {
        step = (float) (1 / (fadeSeconds * base.sampleRate()));
        target = Math.clamp(share, 0, 1);
    }

    @Override
    public void read(short[] out) {
        if (baseChunk.length != out.length) {
            baseChunk = new short[out.length];
            fullChunk = new short[out.length];
        }
        short[] b = baseChunk;
        short[] f = fullChunk;
        base.read(b);
        full.read(f);
        int channels = channels();
        float goal = target;
        float pace = step;
        for (int frame = 0; frame < out.length / channels; frame++) {
            if (mix < goal) {
                mix = Math.min(goal, mix + pace);
            } else if (mix > goal) {
                mix = Math.max(goal, mix - pace);
            }
            for (int c = 0; c < channels; c++) {
                int i = frame * channels + c;
                float sample = b[i] + (f[i] - b[i]) * mix;
                out[i] = (short) Math.clamp(Math.round(sample), Short.MIN_VALUE, Short.MAX_VALUE);
            }
        }
    }

    @Override
    public int channels() {
        return base.channels();
    }

    @Override
    public int sampleRate() {
        return base.sampleRate();
    }

    @Override
    public void close() {
        base.close();
        full.close();
    }
}
