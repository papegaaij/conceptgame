package vanguard.game.audio;

import java.util.Map;
import java.util.Optional;

/**
 * The music files the game plays, by their number in the track list (design/audio/music): each
 * one's file in {@code assets/music/} (imported by {@code :pipeline:importPlaceholders} until its
 * production render by tools/art/themes.py replaces it). A level names its theme by number and its
 * boss cues by file name ({@code boss_sting: miniboss-sting}); both resolve here.
 */
public final class Tracks {
    /** Track 18, the Vrell boss theme "The Choir Descends": loops under an act boss fight (Level 07). */
    public static final int BOSS_VRELL = 18;
    /** Track 22, the boss warning "Red Alert": three bars at 150 BPM that bridge into the boss track. */
    public static final int BOSS_WARNING = 22;
    /** Track 24, "Act Complete": the fanfare under the first page of an act outro (Level 07's Act 1 outro). */
    public static final int ACT_COMPLETE = 24;

    /**
     * Where the boss track starts under the warning: track 22 is three bars at 150 BPM (4.8 s); its
     * last beat is silent, so the boss track comes in on the downbeat after it, the warning's tail
     * ringing out under it (design/audio/music, Loops and transitions).
     */
    public static final double BOSS_WARNING_BARS_SECONDS = 4.8;

    /** The file names (without {@code .ogg}) of the tracks that have one. */
    private static final Map<Integer, String> FILES = Map.ofEntries(
            Map.entry(1, "title-theme"),
            Map.entry(2, "hangar-theme"),
            Map.entry(3, "briefing-theme"),
            Map.entry(4, "afterburner"),
            Map.entry(5, "coalition-rising"),
            Map.entry(BOSS_VRELL, "choir-descends"),
            Map.entry(21, "miniboss-sting"),
            Map.entry(BOSS_WARNING, "boss-warning"),
            Map.entry(23, "mission-complete"),
            Map.entry(ACT_COMPLETE, "act-complete"),
            Map.entry(25, "mission-failed"),
            Map.entry(26, "game-over"));

    private Tracks() {}

    /** A track's file name without the extension ({@code coalition-rising}), or empty while it has none. */
    public static Optional<String> name(int track) {
        return Optional.ofNullable(FILES.get(track));
    }

    /** A music file's path in the assets by its name: {@code music/<name>.ogg}. */
    public static String path(String name) {
        return "music/" + name + ".ogg";
    }

    /** A track's path in the assets ({@code music/act-complete.ogg}); it must have a file. */
    public static String path(int track) {
        return path(name(track).orElseThrow(() -> new IllegalArgumentException("no file for track " + track + " yet")));
    }

    /** A level theme's base stem ({@code music/coalition-rising-base.ogg}): the theme's own file when it has no stems. */
    public static String basePath(String name) {
        return "music/" + name + "-base.ogg";
    }
}
