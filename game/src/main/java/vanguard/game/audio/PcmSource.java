package vanguard.game.audio;

/** A seekable source of interleaved 16-bit PCM frames. */
public interface PcmSource extends AutoCloseable {
    int channels();

    int sampleRate();

    /** Total length in frames (one frame = one sample per channel). */
    long frameCount();

    /**
     * Decodes up to {@code frames} frames into {@code out} starting at sample index {@code offset}.
     *
     * @return the number of frames decoded; 0 at the end of the source
     */
    int read(short[] out, int offset, int frames);

    /** Moves to an exact frame; the next {@link #read} starts there. */
    void seek(long frame);

    @Override
    void close();
}
