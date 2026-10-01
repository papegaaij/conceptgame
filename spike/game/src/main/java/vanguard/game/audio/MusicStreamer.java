package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.AudioDevice;

/**
 * Plays a {@link LoopingStream} on its own thread through a libGDX {@link AudioDevice} (an
 * OpenAL streaming source). libGDX {@code Music} has no loop points and is refilled from the
 * render loop; this thread blocks inside {@link AudioDevice#writeSamples} instead, so a long frame
 * cannot starve the music.
 */
public final class MusicStreamer implements AutoCloseable {
    private static final int FRAMES_PER_CHUNK = 2048;

    private final LoopingStream stream;
    private final AudioDevice device;
    private final short[] chunk;
    private final Thread thread;
    private volatile boolean running = true;

    public MusicStreamer(Audio audio, LoopingStream stream, float volume) {
        this.stream = stream;
        this.device = audio.newAudioDevice(stream.sampleRate(), stream.channels() == 1);
        this.device.setVolume(volume);
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

    @Override
    public void close() {
        running = false;
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        device.dispose();
        stream.close();
    }

    /** Starts the round-08 "Coalition Rising" track: intro once, then its loop section forever. */
    public static MusicStreamer coalitionRising(Audio audio, Files files) {
        VorbisFile track = new VorbisFile(files.internal("music/coalition-rising-full-r08-a.ogg").readBytes());
        return new MusicStreamer(audio, new LoopingStream(track, track.loopPoints()), 0.6f);
    }
}
