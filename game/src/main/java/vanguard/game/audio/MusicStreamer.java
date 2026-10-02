package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import com.badlogic.gdx.files.FileHandle;
import java.util.Optional;

/**
 * Plays a {@link LoopingStream} on its own thread through a libGDX {@link AudioDevice} (an
 * OpenAL streaming source). libGDX {@code Music} has no loop points and is refilled from the
 * render loop; this thread blocks inside {@link AudioDevice#writeSamples} instead, so a long frame
 * cannot starve the music. Its volume is scaled by the {@link Mixer}'s music gain and follows a
 * change of it.
 */
public final class MusicStreamer implements AutoCloseable, Mixer.Listener {
    private static final int FRAMES_PER_CHUNK = 2048;

    private final LoopingStream stream;
    private final AudioDevice device;
    private final short[] chunk;
    private final Thread thread;
    private final Mixer mixer;
    private float volume;
    private volatile boolean running = true;

    private MusicStreamer(Audio audio, LoopingStream stream, float volume, Mixer mixer) {
        this.stream = stream;
        this.mixer = mixer;
        this.device = audio.newAudioDevice(stream.sampleRate(), stream.channels() == 1);
        setVolume(volume);
        mixer.listen(this);
        this.chunk = new short[FRAMES_PER_CHUNK * stream.channels()];
        // The first write claims an OpenAL source from libGDX's pool, which is not thread-safe:
        // do it here on the render thread before the streaming thread starts.
        writeNextChunk();
        this.thread = Thread.ofPlatform().name("music").daemon().unstarted(this::run);
        this.thread.start();
    }

    private void run() {
        while (running) {
            writeNextChunk();
        }
    }

    private void writeNextChunk() {
        stream.read(chunk);
        device.writeSamples(chunk, 0, chunk.length);
    }

    /** Sets the volume, 0..1 before the mixer's music gain, for fades. */
    public void setVolume(float volume) {
        this.volume = volume;
        device.setVolume(volume * mixer.gain(Bus.MUSIC));
    }

    @Override
    public void gainsChanged() {
        setVolume(volume);
    }

    @Override
    public void close() {
        mixer.forget(this);
        running = false;
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        device.dispose();
        stream.close();
    }

    /**
     * Starts a track with its loop points: the intro once, then the loop section forever. Without an
     * audio device libGDX substitutes a mock whose writes return at once, so no music is started then.
     */
    public static Optional<MusicStreamer> play(Audio audio, FileHandle track, float volume, Mixer mixer) {
        if (audio instanceof MockAudio) {
            return Optional.empty();
        }
        VorbisFile file = new VorbisFile(track.readBytes());
        return Optional.of(new MusicStreamer(audio, new LoopingStream(file, file.loopPoints()), volume, mixer));
    }
}
