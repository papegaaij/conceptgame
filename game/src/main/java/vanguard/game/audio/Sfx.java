package vanguard.game.audio;

/**
 * The sound effects: the chosen concept sounds of design/audio/sfx, imported
 * into {@code assets/} by {@code :pipeline:importPlaceholders}, each with its instance limit
 * (design/audio/sfx, Mixing rules): beyond it the oldest instance is stopped, and the
 * {@link Bus} whose volume it follows.
 */
public enum Sfx {
    PULSE_SHOT("sfx/player-shot-r02-a.ogg", 2, Bus.EFFECTS),
    /** The weapon sound families of the Act 1 arsenal (design/audio/sfx, Weapon sound families). */
    VULCAN_SHOT("sfx/shot-vulcan-r03-b.ogg", 2, Bus.EFFECTS),

    BALLISTIC_SHOT("sfx/player-shot-r02-d.ogg", 3, Bus.EFFECTS),
    LASER_SHOT("sfx/shot-laser-r03-b.ogg", 2, Bus.EFFECTS),
    MICROMISSILE_SHOT("sfx/shot-micromissile-r03-a.ogg", 3, Bus.EFFECTS),
    MORTAR_SHOT("sfx/shot-mortar-r03-a.ogg", 2, Bus.EFFECTS),
    BOMB_SHOT("sfx/shot-bomb-r03-a.ogg", 2, Bus.EFFECTS),
    OVERDRIVE_START("sfx/overdrive-start-r08-a.ogg", 1, Bus.EFFECTS),
    OVERDRIVE_END("sfx/overdrive-end-r08-a.ogg", 1, Bus.EFFECTS),
    HIT_ORGANIC_A("sfx/hit-organic-r08-a.ogg", 4, Bus.EFFECTS),
    HIT_ORGANIC_B("sfx/hit-organic-r08-b.ogg", 4, Bus.EFFECTS),
    EXPLOSION_TINY_A("sfx/explosion-tiny-r03-a.ogg", 6, Bus.EFFECTS),
    EXPLOSION_TINY_B("sfx/explosion-tiny-r03-b.ogg", 6, Bus.EFFECTS),
    SHIELD_HIT("sfx/player-shield-hit-r08-a.ogg", 2, Bus.EFFECTS),
    SHIELD_BREAK("sfx/player-shield-break-r08-a.ogg", 1, Bus.EFFECTS),
    ARMOUR_HIT("sfx/player-armour-hit-r08-a.ogg", 2, Bus.EFFECTS),
    SHIP_DESTROYED("sfx/player-destroyed-r08-a.ogg", 1, Bus.EFFECTS),
    ENEMY_SHOT_A("sfx/enemy-shot-small-r08-a.ogg", 3, Bus.EFFECTS),
    ENEMY_SHOT_B("sfx/enemy-shot-small-r08-b.ogg", 3, Bus.EFFECTS),
    /** The {@code small} explosion family (Needler and other small units). */
    EXPLOSION_SMALL_A("sfx/explosion-r02-a.ogg", 6, Bus.EFFECTS),
    EXPLOSION_SMALL_B("sfx/explosion-small-r03-a.ogg", 6, Bus.EFFECTS),
    /** Hits on machines and ground objects such as cargo containers. */
    HIT_METAL_A("sfx/hit-metal-r08-a.ogg", 4, Bus.EFFECTS),
    HIT_METAL_B("sfx/hit-metal-r08-b.ogg", 4, Bus.EFFECTS),
    SALVAGE_SMALL("sfx/pickup-salvage-small-r08-a.ogg", 2, Bus.EFFECTS),
    /** Salvage large, standing in for the hidden crate. */
    SALVAGE_LARGE("sfx/pickup-salvage-large-r08-a.ogg", 1, Bus.EFFECTS),
    SHIELD_CELL("sfx/pickup-shield-cell-r08-a.ogg", 1, Bus.EFFECTS),
    ARMOUR_PATCH("sfx/pickup-armour-patch-r08-a.ogg", 1, Bus.EFFECTS),
    RADIO_OPEN("sfx/ui-radio-open-r08-a.ogg", 1, Bus.RADIO),
    RADIO_CLOSE("sfx/ui-radio-close-r08-a.ogg", 1, Bus.RADIO),
    TYPEWRITER("sfx/ui-typewriter-r08-a.ogg", 2, Bus.RADIO),
    TALLY_TICK("sfx/ui-tally-tick-r08-a.ogg", 2, Bus.EFFECTS),
    TALLY_TOTAL("sfx/ui-tally-total-r08-a.ogg", 1, Bus.EFFECTS),
    GRADE_STAMP("sfx/ui-grade-stamp-r08-a.ogg", 1, Bus.EFFECTS),
    /** The catapult run of a level's launch (round 11 b): its buffer clunk, the release, is 3.6 s in. */
    LAUNCH_RAIL("sfx/launch-rail-r11-b.ogg", 1, Bus.EFFECTS),
    /** The contact ping with an edge warning (round 11 b); one at a time, so overlapping warnings do not stack. */
    EDGE_WARNING("sfx/ui-edge-warning-r11-b.ogg", 1, Bus.EFFECTS),
    /**
     * The Leviathan's whale-song cry under its death's explosion (round 16 a, synthesized): a
     * placeholder until the recorded enemy sounds replace it.
     */
    LEVIATHAN_CRY("sfx/enemy-leviathan-cry-r16-a.ogg", 1, Bus.EFFECTS),
    /** A Brood Pod bursting into its Skitters, shot or on its own (round 08 b, the fleshy burst). */
    BROOD_BURST("sfx/enemy-spawn-r08-b.ogg", 2, Bus.EFFECTS),
    /** The Airstrike's jets flyby (round 08 a), as the bombers enter. */
    AIRSTRIKE_JETS("sfx/special-airstrike-jets-r08-a.ogg", 1, Bus.EFFECTS),
    /** The Airstrike's bomb carpet (round 08 a), from its first blast. */
    AIRSTRIKE_BOMBS("sfx/special-airstrike-bombs-r08-a.ogg", 1, Bus.EFFECTS),
    /** The special button with no charge, no special or a strike still flying (round 08 a). */
    SPECIAL_DENIED("sfx/special-denied-r08-a.ogg", 1, Bus.EFFECTS),
    /** The Earth-orbit ambience, looped (design/audio/sfx, Ambience per setting). */
    AMBIENCE_ORBIT("sfx/ambience-orbit-r08-a.ogg", 1, Bus.EFFECTS),
    /** The Luna ambience, looped (design/audio/sfx, Ambience per setting): Level 04. */
    AMBIENCE_LUNA("sfx/ambience-luna-r08-a.ogg", 1, Bus.EFFECTS),
    /** The mission complete jingle (design/audio/music, track 23), played as a one-shot. */
    MISSION_COMPLETE("music/mission-complete.ogg", 1, Bus.MUSIC),
    /** The music's failure sting (design/audio/music, track 25), played over the cut music. */
    MISSION_FAILED("music/mission-failed.ogg", 1, Bus.MUSIC),
    /** The game over cue (design/audio/music, track 26), played as a one-shot. */
    GAME_OVER("music/game-over.ogg", 1, Bus.MUSIC),
    MENU_MOVE("sfx/ui-menu-move-r08-a.ogg", 2, Bus.EFFECTS),
    MENU_CONFIRM("sfx/ui-menu-confirm-r08-a.ogg", 1, Bus.EFFECTS),
    MENU_BACK("sfx/ui-menu-back-r08-a.ogg", 1, Bus.EFFECTS);

    private final String path;
    private final int instanceLimit;
    private final Bus bus;

    Sfx(String path, int instanceLimit, Bus bus) {
        this.path = path;
        this.instanceLimit = instanceLimit;
        this.bus = bus;
    }

    public String path() {
        return path;
    }

    public int instanceLimit() {
        return instanceLimit;
    }

    public Bus bus() {
        return bus;
    }
}
