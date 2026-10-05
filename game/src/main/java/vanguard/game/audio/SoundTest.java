package vanguard.game.audio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The sound test of the Options Audio tab (design/ui/options): the music tracks and the sound
 * effects the game has, picked with left and right and played on confirm through their buses, so
 * the volume sliders apply. A track plays solo, so the music of the screen below is silent while
 * it plays: a theme loops until confirm stops it or another track starts, a jingle plays once.
 */
public final class SoundTest implements AutoCloseable {
    /** The test's two lists. */
    public enum Kind {
        MUSIC,
        EFFECTS
    }

    /**
     * A music track.
     *
     * @param where where the game plays it
     * @param volume its level before the music volume, as the screen that plays it has it
     */
    record Track(String title, String where, String path, boolean loops, float volume) {}

    /**
     * The screens' music level (the level's boss cue too), and the debrief's, game over's and the
     * act outro's for their cues.
     */
    private static final float THEME_VOLUME = 0.6f;

    private static final float CUE_VOLUME = 0.8f;
    /** The menu sounds' level. */
    private static final float EFFECT_VOLUME = 0.5f;

    static final List<Track> TRACKS = List.of(
            new Track(
                    "TERRAN VANGUARD",
                    "TITLE THEME: TITLE SCREEN AND MAIN MENU",
                    "music/title-theme.ogg",
                    true,
                    THEME_VOLUME),
            new Track("SITUATION ROOM", "BRIEFING THEME", "music/briefing-theme.ogg", true, THEME_VOLUME),
            new Track("DRY DOCK", "HANGAR THEME", "music/hangar-theme.ogg", true, THEME_VOLUME),
            new Track("COALITION RISING", "LEVEL THEME: ACT 1", "music/coalition-rising.ogg", true, THEME_VOLUME),
            new Track("RED ALERT", "CUE: BOSS WARNING", "music/boss-warning.ogg", false, THEME_VOLUME),
            new Track(
                    "THE CHOIR DESCENDS",
                    "BOSS THEME: THE VRELL ACT BOSSES",
                    "music/choir-descends.ogg",
                    true,
                    THEME_VOLUME),
            new Track("MISSION COMPLETE", "JINGLE: DEBRIEF", "music/mission-complete.ogg", false, CUE_VOLUME),
            new Track("MISSION FAILED", "STING: MISSION FAILED", "music/mission-failed.ogg", false, CUE_VOLUME),
            new Track("CONTACT HEAVY", "STING: MINI-BOSS", "music/miniboss-sting.ogg", false, CUE_VOLUME),
            new Track("ACT COMPLETE", "FANFARE: ACT OUTRO", "music/act-complete.ogg", false, CUE_VOLUME),
            new Track("GAME OVER", "CUE: GAME OVER", "music/game-over.ogg", false, CUE_VOLUME));

    /** Every sound effect but the music cues, which are tracks here. */
    static final List<Sfx> EFFECTS =
            Arrays.stream(Sfx.values()).filter(sfx -> sfx.bus() != Bus.MUSIC).toList();

    private static final List<String> TITLES = TRACKS.stream().map(Track::title).toList();
    private static final List<String> EFFECT_NAMES =
            EFFECTS.stream().map(sfx -> sfx.name().replace('_', ' ')).toList();

    private final Audio audio;
    private final Files files;
    private final Mixer mixer;
    private final SfxBank sfx;
    private final int[] picked = new int[Kind.values().length];
    /** The track playing, or -1. */
    private int playing = -1;

    private Optional<MusicStreamer> streamer = Optional.empty();
    private Optional<OnceStream> once = Optional.empty();

    public SoundTest(Audio audio, Files files, Mixer mixer, SfxBank sfx) {
        this.audio = audio;
        this.files = files;
        this.mixer = mixer;
        this.sfx = sfx;
    }

    /** The names in a list. */
    public List<String> names(Kind kind) {
        return switch (kind) {
            case MUSIC -> TITLES;
            case EFFECTS -> EFFECT_NAMES;
        };
    }

    /** The index of the entry picked in a list. */
    public int picked(Kind kind) {
        return picked[kind.ordinal()];
    }

    /** What the picked entry is: where the game plays the track, or the volume the effect follows. */
    public String note(Kind kind) {
        return switch (kind) {
            case MUSIC -> TRACKS.get(picked(kind)).where();
            case EFFECTS ->
                EFFECTS.get(picked(kind)).bus() == Bus.RADIO
                        ? "FOLLOWS THE RADIO BLIPS VOLUME"
                        : "FOLLOWS THE EFFECTS VOLUME";
        };
    }

    /** Whether the picked track is the one playing. */
    public boolean playing() {
        return playing == picked(Kind.MUSIC);
    }

    /** Picks the next (+1) or previous (-1) entry of a list, wrapping round; returns whether it moved. */
    public boolean pick(Kind kind, int direction) {
        if (direction == 0) {
            return false;
        }
        picked[kind.ordinal()] =
                Math.floorMod(picked(kind) + direction, names(kind).size());
        return true;
    }

    /**
     * Plays the picked entry: an effect once, a track solo (stopping the one playing), or stops the
     * picked track when it is the one playing. An effect that loops already (the ambience under a
     * paused level) is heard as it is.
     */
    public void play(Kind kind) {
        switch (kind) {
            case EFFECTS -> {
                Sfx effect = EFFECTS.get(picked(kind));
                if (!sfx.looping(effect)) {
                    sfx.play(effect, EFFECT_VOLUME, 1, 0);
                }
            }
            case MUSIC -> {
                boolean again = playing();
                stop();
                if (!again) {
                    start(picked(kind));
                }
            }
        }
    }

    /** Ends a jingle's turn once it has played out; call it every frame. */
    public void update() {
        if (once.filter(OnceStream::ended).isPresent()) {
            stop();
        }
    }

    private void start(int track) {
        playing = track;
        if (!MusicStreamer.available(audio)) {
            return;
        }
        Track chosen = TRACKS.get(track);
        VorbisFile file = new VorbisFile(files.internal(chosen.path()).readBytes());
        PcmStream stream;
        if (chosen.loops()) {
            stream = LoopingStream.of(file);
        } else {
            OnceStream cue = new OnceStream(file);
            once = Optional.of(cue);
            stream = cue;
        }
        streamer = Optional.of(MusicStreamer.solo(audio, stream, chosen.volume(), mixer));
    }

    private void stop() {
        streamer.ifPresent(MusicStreamer::close);
        streamer = Optional.empty();
        once = Optional.empty();
        playing = -1;
    }

    @Override
    public void close() {
        stop();
    }
}
