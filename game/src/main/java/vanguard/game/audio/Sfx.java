package vanguard.game.audio;

/**
 * The sound effects: the chosen concept sounds of design/audio/sfx, imported
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
    ENEMY_SHOT_A("sfx/enemy-shot-small-r08-a.ogg", 3),
    ENEMY_SHOT_B("sfx/enemy-shot-small-r08-b.ogg", 3),
    /** The {@code small} explosion family (Needler and other small units). */
    EXPLOSION_SMALL_A("sfx/explosion-r02-a.ogg", 6),
    EXPLOSION_SMALL_B("sfx/explosion-small-r03-a.ogg", 6),
    /** Hits on machines and ground objects such as cargo containers. */
    HIT_METAL_A("sfx/hit-metal-r08-a.ogg", 4),
    HIT_METAL_B("sfx/hit-metal-r08-b.ogg", 4),
    SALVAGE_SMALL("sfx/pickup-salvage-small-r08-a.ogg", 2),
    /** Salvage large, standing in for the hidden crate. */
    SALVAGE_LARGE("sfx/pickup-salvage-large-r08-a.ogg", 1),
    SHIELD_CELL("sfx/pickup-shield-cell-r08-a.ogg", 1),
    ARMOUR_PATCH("sfx/pickup-armour-patch-r08-a.ogg", 1),
    RADIO_OPEN("sfx/ui-radio-open-r08-a.ogg", 1),
    RADIO_CLOSE("sfx/ui-radio-close-r08-a.ogg", 1),
    TYPEWRITER("sfx/ui-typewriter-r08-a.ogg", 2),
    TALLY_TICK("sfx/ui-tally-tick-r08-a.ogg", 2),
    TALLY_TOTAL("sfx/ui-tally-total-r08-a.ogg", 1),
    GRADE_STAMP("sfx/ui-grade-stamp-r08-a.ogg", 1),
    /** The Earth-orbit ambience, looped (design/audio/sfx, Ambience per setting). */
    AMBIENCE_ORBIT("sfx/ambience-orbit-r08-a.ogg", 1),
    /** The mission complete jingle (design/audio/music, track 23), played as a one-shot. */
    MISSION_COMPLETE("music/mission-complete.ogg", 1),
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
