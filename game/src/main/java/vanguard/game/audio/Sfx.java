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
     * The warning klaxon (round 08 a, a 4.76 s loop played once): under an act boss's warning banner
     * and the boss warning track (design/campaign, Level 07 music).
     */
    KLAXON("sfx/ui-klaxon-r08-a.ogg", 1, Bus.EFFECTS),
    /**
     * The Leviathan's whale-song cry under its death's explosion (round 16 a, synthesized): a
     * placeholder until the recorded enemy sounds replace it.
     */
    LEVIATHAN_CRY("sfx/enemy-leviathan-cry-r16-a.ogg", 1, Bus.EFFECTS),
    /** The Polyp Mortar's lob (round 21 a, a real mortar thump). */
    MORTAR_LOB("sfx/enemy-mortar-lob-r21-a.ogg", 2, Bus.EFFECTS),
    /** The Polyp Mortar's blob landing on its marker (round 21 a, a wet splat). */
    MORTAR_IMPACT("sfx/enemy-mortar-impact-r21-a.ogg", 3, Bus.EFFECTS),
    /** The mass-driver rail's charge hum while the lights chase before a sled (round 21 a, a 0.78 s loop). */
    SLED_WHINE("sfx/hazard-sled-whine-r21-a.ogg", 2, Bus.EFFECTS),
    /** A sled racing up the rail (round 21 b, a rushing flyby). */
    SLED_PASS("sfx/hazard-sled-pass-r21-b.ogg", 1, Bus.EFFECTS),
    /** The Mantis's 0.6 s telegraph before a sweep (round 23 a, a laser charge-up). */
    MANTIS_TELEGRAPH("sfx/enemy-mantis-telegraph-r23-a.ogg", 2, Bus.EFFECTS),
    /** The Mantis's beam sweep (round 23 b, a death ray with crackle, CC-BY). */
    MANTIS_SWEEP("sfx/enemy-mantis-sweep-r23-b.ogg", 2, Bus.EFFECTS),
    /** The perimeter beacon firing a flare shell (round 23 a, a flare-gun shot, CC-BY). */
    FLARE_LAUNCH("sfx/hazard-flare-launch-r23-a.ogg", 1, Bus.EFFECTS),
    /** A flare burning as it falls (round 23 a, a road flare, a seamless 3 s loop played back to back). */
    FLARE_BURN("sfx/hazard-flare-burn-r23-a.ogg", 2, Bus.EFFECTS),
    /** A cut Coilwyrm's rear part growing its new head (round 23 b, an insect growl and chitter). */
    COILWYRM_REGROW("sfx/enemy-coilwyrm-regrow-r23-b.ogg", 2, Bus.EFFECTS),
    /** A Brood Pod bursting into its Skitters, shot or on its own (round 08 b, the fleshy burst). */
    BROOD_BURST("sfx/enemy-spawn-r08-b.ogg", 2, Bus.EFFECTS),
    /**
     * A Coilwyrm segment or tail bursting, shot or in the chained death's ripple (round 24 b, the
     * fleshy burst). Six instances, for segments shot together by a spread.
     */
    COILWYRM_BURST("sfx/enemy-coilwyrm-burst-r24-b.ogg", 6, Bus.EFFECTS),
    /** A Coilwyrm head's (or regrown head's) burst that starts the chained death (round 24 c, the slowed splatter). */
    COILWYRM_HEAD_BURST("sfx/enemy-coilwyrm-head-burst-r24-c.ogg", 2, Bus.EFFECTS),
    /**
     * The Brood Carrier's roar as it arrives and, lower, as it turns broadside (round 25 a, a deep
     * roar with its echo; provisional until the round closes).
     */
    CARRIER_ROAR("sfx/enemy-carrier-roar-r25-a.ogg", 1, Bus.EFFECTS),
    /** A bay sac's membrane parting as its window opens (round 25 a, provisional). */
    CARRIER_SAC_OPEN("sfx/enemy-carrier-sac-open-r25-a.ogg", 2, Bus.EFFECTS),
    /** A bay sac sucking shut as its window closes (round 25 a, provisional). */
    CARRIER_SAC_CLOSE("sfx/enemy-carrier-sac-close-r25-a.ogg", 2, Bus.EFFECTS),
    /** A unit spat out of an open sac (round 25 a, provisional). */
    CARRIER_LAUNCH("sfx/enemy-carrier-launch-r25-a.ogg", 3, Bus.EFFECTS),
    /** A bay sac bursting, shot or in the death chain (round 25 a, provisional). */
    CARRIER_SAC_BURST("sfx/enemy-carrier-sac-burst-r25-a.ogg", 3, Bus.EFFECTS),
    /** The plate iris opening over the core (round 25 a, provisional). */
    CARRIER_IRIS("sfx/enemy-carrier-iris-r25-a.ogg", 1, Bus.EFFECTS),
    /** A tow's cable snapping as its pod falls free: Level 07's lifeboat (round 25 a, provisional). */
    CABLE_SNAP("sfx/secret-cable-snap-r25-a.ogg", 1, Bus.EFFECTS),
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
    /** Track 21, the mini-boss sting (round 08 a "Contact Heavy"). */
    MINIBOSS_STING("music/miniboss-sting.ogg", 1, Bus.MUSIC),
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
