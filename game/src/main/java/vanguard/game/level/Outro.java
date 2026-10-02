package vanguard.game.level;

import vanguard.content.LevelData;

/**
 * The outro after a won level (design/ui/hud, Radio): the scroll runs on under the radio, and the
 * debrief takes over once the radio has shown its last message (the level-end line and whatever
 * is still queued), at the latest {@link LevelData#OUTRO_SECONDS} after the level's end.
 */
public final class Outro {
    /** Seconds since the level's end; negative while the level is flown. */
    private double elapsed = -1;

    /** The level is won. */
    public void start() {
        elapsed = 0;
    }

    /** The level starts over. */
    public void stop() {
        elapsed = -1;
    }

    /**
     * Advances the outro by a frame.
     *
     * @return whether the debrief takes over now
     */
    public boolean update(float seconds, RadioQueue radio) {
        if (elapsed < 0) {
            return false;
        }
        elapsed += seconds;
        return radio.idle() || elapsed >= LevelData.OUTRO_SECONDS;
    }
}
