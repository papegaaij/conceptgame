package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.files.FileHandle;
import java.util.Optional;

/**
 * A music track played once through on the music bus, then silence: the hook for track 24 "Act
 * Complete" under the first page of an act outro (design/campaign/act-1-first-contact, Act intro and
 * outro: {@code Fanfare.play(audio, files.internal(Tracks.path(Tracks.ACT_COMPLETE)), 0.6f, mixer)}).
 * It streams on its own thread like the themes, so it never stalls a frame; nothing plays without an
 * audio device or when the file is missing.
 */
public final class Fanfare implements AutoCloseable {
    private final MusicStreamer streamer;
    private final OnceStream stream;

    private Fanfare(MusicStreamer streamer, OnceStream stream) {
        this.streamer = streamer;
        this.stream = stream;
    }

    /** Starts {@code file} once at {@code volume} (0..1 before the mixer's music gain); empty without a device or file. */
    public static Optional<Fanfare> play(Audio audio, FileHandle file, float volume, Mixer mixer) {
        if (!MusicStreamer.available(audio) || !file.exists()) {
            return Optional.empty();
        }
        OnceStream stream = new OnceStream(new VorbisFile(file.readBytes()));
        return Optional.of(new Fanfare(MusicStreamer.play(audio, stream, volume, mixer), stream));
    }

    /** Sets the volume, 0..1 before the mixer's music gain, for fades and the duck under a voice. */
    public void setVolume(float volume) {
        streamer.setVolume(volume);
    }

    /** Whether it has played to its end. */
    public boolean ended() {
        return stream.ended();
    }

    @Override
    public void close() {
        streamer.close();
    }
}
