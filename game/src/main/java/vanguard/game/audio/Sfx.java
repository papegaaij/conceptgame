package vanguard.game.audio;

/**
 * The sound effects of the first flight: the chosen concept sounds of design/audio/sfx, imported
 * into {@code assets/} by {@code :pipeline:importPlaceholders}, each with its instance limit
 * (design/audio/sfx, Mixing rules): beyond it the oldest instance is stopped.
 */
public enum Sfx {
    PULSE_SHOT("sfx/player-shot-r02-a.ogg", 2),
    HIT_ORGANIC_A("sfx/hit-organic-r08-a.ogg", 4),
    HIT_ORGANIC_B("sfx/hit-organic-r08-b.ogg", 4),
    EXPLOSION_TINY_A("sfx/explosion-tiny-r03-a.ogg", 6),
    EXPLOSION_TINY_B("sfx/explosion-tiny-r03-b.ogg", 6),
    SHIELD_HIT("sfx/player-shield-hit-r08-a.ogg", 2),
    SHIELD_BREAK("sfx/player-shield-break-r08-a.ogg", 1),
    ARMOUR_HIT("sfx/player-armour-hit-r08-a.ogg", 2),
    SHIP_DESTROYED("sfx/player-destroyed-r08-a.ogg", 1),
    /** The music's failure sting (design/audio/music, track 25), played over the cut music. */
    MISSION_FAILED("music/mission-failed.ogg", 1);

    private final String path;
    private final int instanceLimit;

    Sfx(String path, int instanceLimit) {
        this.path = path;
        this.instanceLimit = instanceLimit;
    }

    public String path() {
        return path;
    }

    public int instanceLimit() {
        return instanceLimit;
    }
}
