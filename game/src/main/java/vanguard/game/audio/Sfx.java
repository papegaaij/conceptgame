package vanguard.game.audio;

import static vanguard.game.audio.Sfx.Priority.AMBIENCE;
import static vanguard.game.audio.Sfx.Priority.BOSS;
import static vanguard.game.audio.Sfx.Priority.ENEMY_FIRE;
import static vanguard.game.audio.Sfx.Priority.EXPLOSION;
import static vanguard.game.audio.Sfx.Priority.PICKUP;
import static vanguard.game.audio.Sfx.Priority.PLAYER_FIRE;
import static vanguard.game.audio.Sfx.Priority.WARNING;

import vanguard.content.Tier;
import vanguard.content.campaign.Hangar;

/**
 * The sound effects: the chosen concept sounds of design/audio/sfx, imported
 * into {@code assets/} by {@code :pipeline:importPlaceholders}, each with its instance limit
 * (design/audio/sfx, Mixing rules): beyond it the oldest instance is stopped, the {@link Bus}
 * whose volume it follows, and its {@link Priority} for the global voice limit ({@link SfxBank}).
 */
public enum Sfx {
    PULSE_SHOT("sfx/player-shot-r02-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    /** The weapon sound families of the Act 1 arsenal (design/audio/sfx, Weapon sound families). */
    VULCAN_SHOT("sfx/shot-vulcan-r03-b.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),

    BALLISTIC_SHOT("sfx/player-shot-r02-d.ogg", 3, Bus.EFFECTS, PLAYER_FIRE),
    LASER_SHOT("sfx/shot-laser-r03-b.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    MICROMISSILE_SHOT("sfx/shot-micromissile-r03-a.ogg", 3, Bus.EFFECTS, PLAYER_FIRE),
    MORTAR_SHOT("sfx/shot-mortar-r03-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    BOMB_SHOT("sfx/shot-bomb-r03-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    /** The Act 2 families: the Hornet Launcher's rocket launch and the proximity mine's drop-and-bounce clunk. */
    MISSILE_SHOT("sfx/shot-missile-r03-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),

    MINE_DROP("sfx/shot-mine-r03-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    /**
     * A proximity mine arming (round 28 a, provisional until the round closes: a soft rising
     * two-blip chirp); two at a time, so mines arming together do not stack.
     */
    MINE_ARM("sfx/weapon-mine-arm-r28-a.ogg", 2, Bus.EFFECTS, PLAYER_FIRE),
    OVERDRIVE_START("sfx/overdrive-start-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    OVERDRIVE_END("sfx/overdrive-end-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    /** Hits are the player's fire landing: they share its priority. */
    HIT_ORGANIC_A("sfx/hit-organic-r08-a.ogg", 4, Bus.EFFECTS, PLAYER_FIRE),
    HIT_ORGANIC_B("sfx/hit-organic-r08-b.ogg", 4, Bus.EFFECTS, PLAYER_FIRE),
    EXPLOSION_TINY_A("sfx/explosion-tiny-r03-a.ogg", 6, Bus.EFFECTS, EXPLOSION),
    EXPLOSION_TINY_B("sfx/explosion-tiny-r03-b.ogg", 6, Bus.EFFECTS, EXPLOSION),
    SHIELD_HIT("sfx/player-shield-hit-r08-a.ogg", 2, Bus.EFFECTS, WARNING),
    SHIELD_BREAK("sfx/player-shield-break-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The shield full again after a break (round 08 a, a rising charge). */
    SHIELD_RESTORE("sfx/player-shield-restore-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    ARMOUR_HIT("sfx/player-armour-hit-r08-a.ogg", 2, Bus.EFFECTS, WARNING),
    /** One beep of the low-armour warning (round 08 a), repeated slowly at 30 % armour and fast at 15 %. */
    LOW_ARMOUR("sfx/player-low-armour-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    SHIP_DESTROYED("sfx/player-destroyed-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    ENEMY_SHOT_A("sfx/enemy-shot-small-r08-a.ogg", 3, Bus.EFFECTS, ENEMY_FIRE),
    ENEMY_SHOT_B("sfx/enemy-shot-small-r08-b.ogg", 3, Bus.EFFECTS, ENEMY_FIRE),
    /**
     * A heavy (medium-bullet) enemy shot (round 08 a and b): played for an {@code ENEMY_FIRED} event
     * whose value, the bullet's damage, is a medium bullet's (FlightSounds).
     */
    ENEMY_HEAVY_SHOT_A("sfx/enemy-shot-heavy-r08-a.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),

    ENEMY_HEAVY_SHOT_B("sfx/enemy-shot-heavy-r08-b.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),
    /** The {@code small} explosion family (Needler and other small units). */
    EXPLOSION_SMALL_A("sfx/explosion-r02-a.ogg", 6, Bus.EFFECTS, EXPLOSION),
    EXPLOSION_SMALL_B("sfx/explosion-small-r03-a.ogg", 6, Bus.EFFECTS, EXPLOSION),
    /** The small ladder's third file, a heavier pop: destroyed ground targets. */
    EXPLOSION_SMALL_C("sfx/explosion-r02-b.ogg", 4, Bus.EFFECTS, EXPLOSION),
    /** The {@code medium} rung: medium units (Mantis, Scuttler, Spore Bomber, Brood Pod) and set-piece parts. */
    EXPLOSION_MEDIUM_A("sfx/explosion-r02-c.ogg", 4, Bus.EFFECTS, EXPLOSION),
    EXPLOSION_MEDIUM_B("sfx/explosion-medium-r03-b.ogg", 4, Bus.EFFECTS, EXPLOSION),
    /** The {@code large} rung: a mid-boss's death, a boss's phase ends, a set piece's break-up. */
    EXPLOSION_LARGE_A("sfx/explosion-r02-d.ogg", 3, Bus.EFFECTS, EXPLOSION),
    EXPLOSION_LARGE_B("sfx/explosion-large-r03-a.ogg", 3, Bus.EFFECTS, EXPLOSION),
    /** The large rung's boss blast ("boss destroyed / capital ship"): under an act boss's final blast. */
    EXPLOSION_LARGE_C("sfx/explosion-r02-e.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /** The {@code huge} rung: an act boss's or a huge set piece's death (a recorded TNT blast with debris). */
    EXPLOSION_HUGE_A("sfx/explosion-huge-r04-a.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /** The huge rung's sub-heavy boom: under the Smart Bomb's blast. */
    EXPLOSION_HUGE_B("sfx/explosion-huge-r03-b.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /** A large destroyed ground target crumbling (round 08 a, a building collapse). */
    CRUMBLE_LARGE("sfx/hit-crumble-r08-a.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /** A small destroyed ground target's rubble burst (round 08 b). */
    CRUMBLE_SMALL("sfx/hit-crumble-r08-b.ogg", 3, Bus.EFFECTS, EXPLOSION),
    /** Hits on machines and ground objects such as cargo containers. */
    HIT_METAL_A("sfx/hit-metal-r08-a.ogg", 4, Bus.EFFECTS, PLAYER_FIRE),
    HIT_METAL_B("sfx/hit-metal-r08-b.ogg", 4, Bus.EFFECTS, PLAYER_FIRE),
    SALVAGE_SMALL("sfx/pickup-salvage-small-r08-a.ogg", 2, Bus.EFFECTS, PICKUP),
    /** Salvage medium (round 01 c, the rising sweep with sparkles). */
    SALVAGE_MEDIUM("sfx/pickup-r01-c.ogg", 2, Bus.EFFECTS, PICKUP),
    /** Salvage large: a set piece's drop, the hidden crate, the boss's credit shower. */
    SALVAGE_LARGE("sfx/pickup-salvage-large-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    SHIELD_CELL("sfx/pickup-shield-cell-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    ARMOUR_PATCH("sfx/pickup-armour-patch-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    /** A special charge picked up (round 08 a, three rising notes and a bell). */
    SPECIAL_CHARGE("sfx/pickup-special-charge-r08-a.ogg", 1, Bus.EFFECTS, PICKUP),
    /** The overdrive pickup (round 01 a, the standard power-up arpeggio), with the overdrive's start cue. */
    POWER_UP("sfx/pickup-r01-a.ogg", 1, Bus.EFFECTS, PICKUP),
    /** A data core picked up (round 01 b, the two-bell chime for rare upgrades). */
    DATA_CORE("sfx/pickup-r01-b.ogg", 1, Bus.EFFECTS, PICKUP),
    RADIO_OPEN("sfx/ui-radio-open-r08-a.ogg", 1, Bus.RADIO, WARNING),
    RADIO_CLOSE("sfx/ui-radio-close-r08-a.ogg", 1, Bus.RADIO, WARNING),
    TYPEWRITER("sfx/ui-typewriter-r08-a.ogg", 2, Bus.RADIO, WARNING),
    TALLY_TICK("sfx/ui-tally-tick-r08-a.ogg", 2, Bus.EFFECTS, WARNING),
    TALLY_TOTAL("sfx/ui-tally-total-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    GRADE_STAMP("sfx/ui-grade-stamp-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The catapult run of a level's launch (round 11 b): its buffer clunk, the release, is 3.6 s in. */
    LAUNCH_RAIL("sfx/launch-rail-r11-b.ogg", 1, Bus.EFFECTS, WARNING),
    /** The contact ping with an edge warning (round 11 b); one at a time, so overlapping warnings do not stack. */
    EDGE_WARNING("sfx/ui-edge-warning-r11-b.ogg", 1, Bus.EFFECTS, WARNING),
    /**
     * The warning klaxon (round 08 a, a 4.76 s loop played once): under an act boss's warning banner
     * and the boss warning track (design/campaign, Level 07 music).
     */
    KLAXON("sfx/ui-klaxon-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /**
     * The Leviathan's whale-song cry under its death's explosion (round 16 a, synthesized): a
     * placeholder until the recorded enemy sounds replace it.
     */
    LEVIATHAN_CRY("sfx/enemy-leviathan-cry-r16-a.ogg", 1, Bus.EFFECTS, BOSS),
    /** The Polyp Mortar's lob (round 21 a, a real mortar thump). */
    MORTAR_LOB("sfx/enemy-mortar-lob-r21-a.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),
    /** The Polyp Mortar's blob landing on its marker (round 21 a, a wet splat). */
    MORTAR_IMPACT("sfx/enemy-mortar-impact-r21-a.ogg", 3, Bus.EFFECTS, ENEMY_FIRE),
    /** The mass-driver rail's charge hum while the lights chase before a sled (round 21 a, a 0.78 s loop). */
    SLED_WHINE("sfx/hazard-sled-whine-r21-a.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),
    /** A sled racing up the rail (round 21 b, a rushing flyby). */
    SLED_PASS("sfx/hazard-sled-pass-r21-b.ogg", 1, Bus.EFFECTS, ENEMY_FIRE),
    /** The Mantis's 0.6 s telegraph before a sweep (round 23 a, a laser charge-up): a warning. */
    MANTIS_TELEGRAPH("sfx/enemy-mantis-telegraph-r23-a.ogg", 2, Bus.EFFECTS, WARNING),
    /** The Mantis's beam sweep (round 23 b, a death ray with crackle, CC-BY). */
    MANTIS_SWEEP("sfx/enemy-mantis-sweep-r23-b.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),
    /** The perimeter beacon firing a flare shell (round 23 a, a flare-gun shot, CC-BY). */
    FLARE_LAUNCH("sfx/hazard-flare-launch-r23-a.ogg", 1, Bus.EFFECTS, ENEMY_FIRE),
    /** A flare burning as it falls (round 23 a, a road flare, a seamless 3 s loop played back to back). */
    FLARE_BURN("sfx/hazard-flare-burn-r23-a.ogg", 2, Bus.EFFECTS, ENEMY_FIRE),
    /** A cut Coilwyrm's rear part growing its new head (round 23 b, an insect growl and chitter). */
    COILWYRM_REGROW("sfx/enemy-coilwyrm-regrow-r23-b.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /**
     * The Vrell screech (round 08 c and d, in turn) as a large Vrell unit enters the screen: the
     * Mantis, the Coilwyrm's head, the Spore Bomber, the Scuttler; at most one every 3 s
     * ({@link ScreechCue}).
     */
    ENEMY_SCREECH_C("sfx/enemy-screech-r08-c.ogg", 1, Bus.EFFECTS, ENEMY_FIRE),

    ENEMY_SCREECH_D("sfx/enemy-screech-r08-d.ogg", 1, Bus.EFFECTS, ENEMY_FIRE),
    /** A Coilwyrm's chain cut through: the body torn in two (round 27 a, a wet flesh rip). */
    COILWYRM_CUT("sfx/enemy-coilwyrm-cut-r27-a.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /** A Brood Pod bursting into its Skitters, shot or on its own (round 08 b, the fleshy burst). */
    BROOD_BURST("sfx/enemy-spawn-r08-b.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /**
     * A Coilwyrm segment or tail bursting, shot or in the chained death's ripple (round 24 b, the
     * fleshy burst). Six instances, for segments shot together by a spread.
     */
    COILWYRM_BURST("sfx/enemy-coilwyrm-burst-r24-b.ogg", 6, Bus.EFFECTS, EXPLOSION),
    /** A Coilwyrm head's (or regrown head's) burst that starts the chained death (round 24 c, the slowed splatter). */
    COILWYRM_HEAD_BURST("sfx/enemy-coilwyrm-head-burst-r24-c.ogg", 2, Bus.EFFECTS, EXPLOSION),
    /**
     * The Brood Carrier's roar as it arrives and, lower, as it turns broadside (round 25 b, a bear's
     * roar over a didgeridoo drone).
     */
    CARRIER_ROAR("sfx/enemy-carrier-roar-r25-b.ogg", 1, Bus.EFFECTS, BOSS),
    /** A bay sac's membrane parting as its window opens (round 25 b, a tear out of sucking mud). */
    CARRIER_SAC_OPEN("sfx/enemy-carrier-sac-open-r25-b.ogg", 2, Bus.EFFECTS, BOSS),
    /** A bay sac sucking shut as its window closes (round 25 b, the opening's tear reversed). */
    CARRIER_SAC_CLOSE("sfx/enemy-carrier-sac-close-r25-b.ogg", 2, Bus.EFFECTS, BOSS),
    /** A unit spat out of an open sac (round 25 b, a slime lunge). */
    CARRIER_LAUNCH("sfx/enemy-carrier-launch-r25-b.ogg", 3, Bus.EFFECTS, BOSS),
    /** A bay sac bursting, shot or in the death chain (round 25 a, a very wet, fleshy explosion). */
    CARRIER_SAC_BURST("sfx/enemy-carrier-sac-burst-r25-a.ogg", 3, Bus.EFFECTS, BOSS),
    /** The plate iris opening over the core (round 25 b, a grinding organic morph). */
    CARRIER_IRIS("sfx/enemy-carrier-iris-r25-b.ogg", 1, Bus.EFFECTS, BOSS),
    /** A tow's cable snapping as its pod falls free: Level 07's lifeboat (round 25 a, a chain snap). */
    CABLE_SNAP("sfx/secret-cable-snap-r25-a.ogg", 1, Bus.EFFECTS, EXPLOSION),
    /** The Airstrike's jets flyby (round 08 a), as the bombers enter. */
    AIRSTRIKE_JETS("sfx/special-airstrike-jets-r08-a.ogg", 1, Bus.EFFECTS, BOSS),
    /** The Airstrike's bomb carpet (round 08 a), from its first blast. */
    AIRSTRIKE_BOMBS("sfx/special-airstrike-bombs-r08-a.ogg", 1, Bus.EFFECTS, BOSS),
    /** The Smart Bomb's charge-up and white-out boom (round 08 a). */
    SMART_BOMB("sfx/special-smartbomb-r08-a.ogg", 1, Bus.EFFECTS, BOSS),
    /** The special button with no charge, no special or a strike still flying (round 08 a). */
    SPECIAL_DENIED("sfx/special-denied-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The Earth-orbit ambience, looped (design/audio/sfx, Ambience per setting). */
    AMBIENCE_ORBIT("sfx/ambience-orbit-r08-a.ogg", 1, Bus.EFFECTS, AMBIENCE),
    /** The Luna ambience, looped (design/audio/sfx, Ambience per setting): Level 04. */
    AMBIENCE_LUNA("sfx/ambience-luna-r08-a.ogg", 1, Bus.EFFECTS, AMBIENCE),
    /** The Earth megacity ambience (night city, distant sirens), looped: Level 08 (M5 part B). */
    AMBIENCE_CITY("sfx/ambience-city-r08-a.ogg", 1, Bus.EFFECTS, AMBIENCE),
    /** The mission complete jingle (design/audio/music, track 23), played as a one-shot. */
    MISSION_COMPLETE("music/mission-complete.ogg", 1, Bus.MUSIC, WARNING),
    /** The music's failure sting (design/audio/music, track 25), played over the cut music. */
    MISSION_FAILED("music/mission-failed.ogg", 1, Bus.MUSIC, WARNING),
    /** Track 21, the mini-boss sting (round 08 a "Contact Heavy"). */
    MINIBOSS_STING("music/miniboss-sting.ogg", 1, Bus.MUSIC, WARNING),
    /** The game over cue (design/audio/music, track 26), played as a one-shot. */
    GAME_OVER("music/game-over.ogg", 1, Bus.MUSIC, WARNING),
    MENU_MOVE("sfx/ui-menu-move-r08-a.ogg", 2, Bus.EFFECTS, WARNING),
    MENU_CONFIRM("sfx/ui-menu-confirm-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    MENU_BACK("sfx/ui-menu-back-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The hangar's purchase: an item or a special charge bought, a repair paid ({@link #shop}). */
    SHOP_BUY("sfx/ui-shop-buy-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The hangar's sale, and an undo's refund. */
    SHOP_SELL("sfx/ui-shop-sell-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** The hangar refusing a choice: can't afford, won't fit (power), at its most. */
    SHOP_DENIED("sfx/ui-shop-denied-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** An item fitted to a slot or moved to the inventory. */
    EQUIP("sfx/ui-equip-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** A weapon upgraded a level. */
    UPGRADE("sfx/ui-upgrade-r08-a.ogg", 1, Bus.EFFECTS, WARNING),
    /** A save written: the hangar's autosave as it opens, a save to a slot (round 27 b, the calm chime). */
    SAVE_DONE("sfx/ui-save-r27-b.ogg", 1, Bus.EFFECTS, WARNING);

    /**
     * Which voices give way when more than {@link VoiceLimit#MAX_VOICES} effects play (design/audio/sfx,
     * Mixing rules), lowest first: a new sound steals the oldest voice of the lowest priority at or
     * below its own, and is dropped when every voice ranks above it.
     */
    public enum Priority {
        /** Setting loops; a running loop is never stolen (it would not come back). */
        AMBIENCE,
        PICKUP,
        /** The player's shots and their hits. */
        PLAYER_FIRE,
        /** Enemy shots, beams and hazards. */
        ENEMY_FIRE,
        /** Explosions, bursts and crumbling targets. */
        EXPLOSION,
        /** A boss's own sounds and the specials' (rare, long and called by the player). */
        BOSS,
        /** Warnings and player damage, and the interface: menus, radio, the hangar, the music's cues. */
        WARNING
    }

    private final String path;
    private final int instanceLimit;
    private final Bus bus;
    private final Priority priority;

    Sfx(String path, int instanceLimit, Bus bus, Priority priority) {
        this.path = path;
        this.instanceLimit = instanceLimit;
        this.bus = bus;
        this.priority = priority;
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

    public Priority priority() {
        return priority;
    }

    /**
     * A setting's ambience loop by its key, a level's {@code music.ambience} (design/audio/sfx,
     * Ambience per setting).
     */
    public static Sfx ambience(String setting) {
        return switch (setting) {
            case "earth-orbit" -> AMBIENCE_ORBIT;
            case "luna" -> AMBIENCE_LUNA;
            case "earth-megacity" -> AMBIENCE_CITY;
            default -> throw new IllegalArgumentException("no ambience for " + setting + " yet");
        };
    }

    /**
     * An enemy's explosion by its size tier (design/audio/sfx, Explosion ladder), variant a or b: two
     * files a rung, alternated so no file plays twice in a row.
     */
    public static Sfx explosion(Tier tier, boolean variantB) {
        return switch (tier) {
            case TINY -> variantB ? EXPLOSION_TINY_B : EXPLOSION_TINY_A;
            case SMALL -> variantB ? EXPLOSION_SMALL_B : EXPLOSION_SMALL_A;
            case MEDIUM -> variantB ? EXPLOSION_MEDIUM_B : EXPLOSION_MEDIUM_A;
            case LARGE -> variantB ? EXPLOSION_LARGE_B : EXPLOSION_LARGE_A;
            case HUGE -> variantB ? EXPLOSION_HUGE_B : EXPLOSION_HUGE_A;
        };
    }

    /**
     * The hangar's sound for a shop action that was made (design/audio/sfx, UI and radio): buying an
     * item or a charge, selling, fitting or unfitting, upgrading. A refused choice plays
     * {@link #SHOP_DENIED}, a paid repair {@link #SHOP_BUY} and an undo's refund {@link #SHOP_SELL}.
     */
    public static Sfx shop(Hangar.Action action) {
        return switch (action) {
            case BUY, BUY_CHARGE -> SHOP_BUY;
            case SELL -> SHOP_SELL;
            case FIT, UNFIT -> EQUIP;
            case UPGRADE -> UPGRADE;
        };
    }
}
