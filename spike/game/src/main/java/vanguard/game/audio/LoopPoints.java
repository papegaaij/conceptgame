package vanguard.game.audio;

import java.util.Map;

/**
 * The loop section of a track: frames {@code [start, end)} repeat forever after the intro
 * {@code [0, start)} has played once. Read from the {@code LOOPSTART} / {@code LOOPLENGTH} Vorbis
 * comments (in frames) that design/audio/music prescribes.
 */
public record LoopPoints(long start, long end) {
    public LoopPoints {
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("invalid loop [" + start + ", " + end + ")");
        }
    }

    public long length() {
        return end - start;
    }

    /** Loop points from the comments, or the whole track when they are absent. */
    public static LoopPoints fromComments(Map<String, String> comments, long frameCount) {
        String start = comments.get("LOOPSTART");
        String length = comments.get("LOOPLENGTH");
        if (start == null || length == null) {
            return new LoopPoints(0, frameCount);
        }
        long loopStart = Long.parseLong(start.trim());
        LoopPoints points = new LoopPoints(loopStart, loopStart + Long.parseLong(length.trim()));
        if (points.end > frameCount) {
            throw new IllegalArgumentException("loop end " + points.end + " beyond track length " + frameCount);
        }
        return points;
    }
}
