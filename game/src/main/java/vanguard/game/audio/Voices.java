package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.voice.VoiceLines;

/**
 * The spoken radio lines and briefing pages (design/audio/voice): finds a line's rendered file in
 * assets/voice by its key and plays one line at a time on the {@link Bus#VOICE} bus. A line
 * without a file is not an error: it shows as text only.
 */
public final class Voices implements Mixer.Listener {
    /** A line's voice file and its length. */
    public record Voice(String path, float seconds) {}

    private final Audio audio;
    private final Files files;
    private final Mixer mixer;
    private final Content content;
    private final Map<String, VoiceLines.VoiceLine> radio;
    private final Map<String, Optional<Voice>> found = new HashMap<>();

    private Sound sound;
    private long id = -1;
    private float left;
    private boolean paused;

    public Voices(Audio audio, Files files, Mixer mixer, Content content) {
        this.audio = audio;
        this.files = files;
        this.mixer = mixer;
        this.content = content;
        this.radio = VoiceLines.radioIndex(content);
        mixer.listen(this);
    }

    /**
     * The voice of a radio line as shown (with {@code {ally}} filled in), if it has a file: its
     * rendered line, or for a stage direction the speaker's stage sound (the Choir sings).
     */
    public Optional<Voice> radio(String speaker, String line, String expression) {
        return VoiceLines.radioVoice(radio, content.voices(), speaker, line, expression)
                .flatMap(this::file);
    }

    /** The voice of a briefing page, if it has a file. */
    public Optional<Voice> briefing(BriefingPage page) {
        return VoiceLines.briefingPage(content.voices(), page, "briefing").flatMap(voice -> file(voice.path()));
    }

    private Optional<Voice> file(String path) {
        return found.computeIfAbsent(path, p -> {
            FileHandle file = files.internal(p);
            if (!file.exists()) {
                return Optional.empty();
            }
            try (VorbisFile vorbis = new VorbisFile(file.readBytes())) {
                return Optional.of(new Voice(p, (float) vorbis.frameCount() / vorbis.sampleRate()));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
    }

    /** Plays {@code voice}, cutting the one that plays. */
    public void play(Voice voice) {
        stop();
        try {
            sound = audio.newSound(files.internal(voice.path()));
        } catch (RuntimeException e) {
            return;
        }
        id = sound.play(mixer.gain(Bus.VOICE));
        left = voice.seconds();
        paused = false;
    }

    /** Counts the playing line down; it ends by itself after its length. */
    public void update(float seconds) {
        if (sound != null && !paused) {
            left -= seconds;
            if (left <= 0) {
                stop();
            }
        }
    }

    /** Whether a line is playing (or paused). */
    public boolean playing() {
        return sound != null;
    }

    public void pause() {
        if (sound != null && !paused) {
            sound.pause(id);
            paused = true;
        }
    }

    public void resume() {
        if (sound != null && paused) {
            sound.resume(id);
            paused = false;
        }
    }

    /** Cuts the playing line. */
    public void stop() {
        if (sound != null) {
            sound.stop(id);
            sound.dispose();
            sound = null;
            id = -1;
            paused = false;
        }
    }

    @Override
    public void gainsChanged() {
        if (sound != null) {
            sound.setVolume(id, mixer.gain(Bus.VOICE));
        }
    }
}
