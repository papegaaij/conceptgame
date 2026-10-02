package vanguard.game.audio;

/** An endless stream of interleaved 16-bit PCM samples, read by the {@link MusicStreamer}'s thread. */
public interface PcmStream extends AutoCloseable {
    /** Fills {@code out} completely with interleaved samples; never allocates. */
    void read(short[] out);

    int channels();

    int sampleRate();

    @Override
    void close();
}
