package vanguard.game.audio;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import vanguard.game.level.LowArmour;
import vanguard.game.render.BulletLooks;
import vanguard.game.render.EnemyLooks;
import vanguard.game.render.LevelRenderer;
import vanguard.game.render.TriggerBreak;
import vanguard.sim.Armament;
import vanguard.sim.Defences;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.SplitMix64;
import vanguard.sim.Tow;
import vanguard.sim.WarningEdge;

/**
 * Turns the simulation's events into sound effects, mixed per design/audio/sfx (Mixing rules):
 * relative levels, a few percent of random pitch, two variants alternating so no file repeats
 * twice in a row, and a subtle pan by the position in the play field. Explosions follow the size
 * ladder: a set piece's part blows with the {@code medium} rung, a mid-boss and a boss's phase end
 * with the {@code large} one, an act boss or a huge set piece with the {@code huge} one. Level
 * 03's events reuse the sounds there are until they get their own: a spore drops with a soft low
 * organic plop, bursts or is shot with the tiny explosion, debris rings like metal and breaks with
 * a low tiny explosion. Besides the events it follows the ship's state ({@link #watch}): the
 * low-armour beeps and the shield's restore chime; and the large Vrell units entering the screen,
 * which screech.
 */
public final class FlightSounds {
    /** The levels relative to player damage, the loudest group (+2 dB in the mixing rules). */
    private static final float PLAYER_DAMAGE = 1f;

    private static final float EXPLOSIONS = decibels(-2);
    /** Enemy hits sit 8 dB below player damage. */
    private static final float HITS = decibels(-8);

    private static final float ENEMY_FIRE = decibels(-11);
    private static final float PICKUPS = decibels(-8);
    private static final float PLAYER_FIRE = decibels(-14);
    /** A proximity mine's arming beep: a quiet tick above the shots' level, below the hits'. */
    private static final float MINE_ARMING = decibels(-10);

    private static final float MAX_PAN = 0.4f;
    private static final PickupType[] PICKUP_TYPES = PickupType.values();

    /** Rear guns play their family's sound about 10 % lower (design/audio/sfx, Weapon sound families). */
    private static final float REAR_PITCH = 0.9f;

    private static final int MAX_PENDING = 8;
    /** The sled whine's loop (0.78 s) in simulation steps: its second play over the lights' 1.5 s chase. */
    private static final int SLED_WHINE_LOOP_STEPS = 47;
    /** The flare burn's seamless loop (round 23 a), played back to back while a flare burns. */
    private static final double FLARE_BURN_LOOP_SECONDS = 3;
    /** How long a flare burns (the level's darkness, by difficulty); 0 without flares. */
    private double flareSeconds;

    private final SfxBank bank;
    private final EnemyLooks[] looks;
    private final Sfx[] shots;
    private final float[] shotPitch;
    /** Each set piece's death cry, by index in the script; null for none. */
    private final Sfx[] cries;

    /** The sounds' own generator (the pitch variation, the pounce's pick), apart from the simulation's. */
    private final SplitMix64 random = new SplitMix64(0x5F3);
    /** Sounds waiting for their step (a set piece's break-up): what, how loud, pitch, pan, steps left. */
    private final Sfx[] pending = new Sfx[MAX_PENDING];

    private final float[] pendingVolume = new float[MAX_PENDING];
    private final float[] pendingPitch = new float[MAX_PENDING];
    private final float[] pendingPan = new float[MAX_PENDING];
    private final int[] pendingSteps = new int[MAX_PENDING];
    private int pendingCount;
    private boolean variantB;

    /**
     * A chained death's bursts (design/enemies/air/coilwyrm) follow the chain's taper: the pitch of
     * a member's burst rises as its hit box narrows, 0.94 at the first segment's 37.8 px (54 px x
     * 0.7) up to at most 1.2 at the last one's 18.9 px (the tail, 28 px, in between), with ±2 %
     * random on top; one burst every 0.25 s, so they do not overlap and play at the explosion level.
     */
    private static final double RIPPLE_REFERENCE_WIDTH = 37.8;

    private static final double RIPPLE_PITCH_START = 0.94;
    private static final double RIPPLE_PITCH_EXPONENT = 0.35;
    private static final double RIPPLE_PITCH_MAX = 1.2;

    /**
     * A boss's own sounds (design/audio/sfx, round 25: the Brood Carrier): its roar as it arrives
     * and as it turns, its sacs opening and shutting with their windows, a sac bursting, its iris
     * opening over the core. A boss without them keeps the generic sounds. The units its windows
     * launch spit out with {@link Sfx#CARRIER_LAUNCH} (only the carrier has windows).
     */
    record BossSounds(Sfx roar, Sfx sacOpen, Sfx sacClose, Sfx sacBurst, Sfx iris) {}

    /** The roar starts this many steps after the klaxon, so the klaxon's first blast is heard on its own. */
    private static final int ARRIVAL_ROAR_STEPS = 45;
    /** The roar at the turn is a little lower than the arrival's. */
    private static final float TURN_ROAR_PITCH = 0.85f;
    /** At most this many launch sounds for one opening, this many steps apart after it opens. */
    private static final int LAUNCH_SOUNDS = 3;

    private static final int LAUNCH_STEPS = 6;

    /**
     * The low-armour warning (design/player/armor): a beep every {@value #SLOW_BEEP_SECONDS} s at or
     * below 30 % armour, every {@value #FAST_BEEP_SECONDS} s at or below 15 %, while the ship flies:
     * the {@link LowArmour} stages, the same as the HUD's flashing readout. It repeats for as long as
     * the armour stays low (armour does not regenerate), so it plays 6 dB under the warnings' level.
     */
    static final double SLOW_BEEP_SECONDS = 1.2;

    static final double FAST_BEEP_SECONDS = 0.6;
    private static final float LOW_ARMOUR_LEVEL = decibels(-6);
    /** Steps until the next low-armour beep; 0 while the armour is not low. */
    private int beepIn;
    /** Whether the shield broke and has not been full since: its restore chime is due. */
    private boolean shieldDown;
    /** A destroyed ground target at least this large (px²) crumbles like a structure, a smaller one bursts into rubble. */
    private static final double LARGE_GROUND_AREA = 1000;
    /** Which ground objects of the script are large, from the first {@link #watch}; null before. */
    private boolean[] largeGround;
    /** Which ground objects of the script are triggers that break as they are spent ({@link TriggerBreak}); null before. */
    private boolean[] breakingTrigger;
    /** Whether each set piece's death is huge: an act boss, or a set piece that is no boss. */
    private boolean[] hugeDeath;
    /** Whether each set piece's death is an act boss's tail-to-head chain. */
    private boolean[] finale;

    /** Each set piece's own sounds, by index in the script; null for the generic ones. */
    private final BossSounds[] bossSounds;
    /** Whether a boss of the level brings its own phase sounds (its turn and its iris, not the generic phase blast). */
    private final boolean ownPhaseSounds;
    /** What {@link #watch} saw at the last step: each own-sound boss's parts open and wrecked, its turn, the tows holding. */
    private boolean[][] wasOpen;

    private boolean[][] wasWrecked;
    private boolean[] wasTurning;
    private boolean[] wasHolding;
    /** The next {@link #watch} only notes the state (after a restart or a boss checkpoint's restore). */
    private boolean resync = true;

    /** The Vrell screech as a large unit enters the screen, at most one every 3 s. */
    private final ScreechCue screech = new ScreechCue(SimStep.ticks(ScreechCue.THROTTLE_SECONDS));
    /** M5 part D: a Mote Swarm's rush of wings as it enters the screen. */
    private final SwarmCue swarms = new SwarmCue();

    /**
     * M5 part D: where the impact sits in {@link Sfx#LANCE_STRIKE}, seconds from its start: round 32's
     * lance a lands it 1.2 s in (tools/concept/audio/sfx_r32.py), so the sound starts this long
     * before the scripted loss's hit (Level 10: at 116.8 for the hit at 118), inside the glow.
     */
    static final double LANCE_IMPACT_SECONDS = 1.2;
    /** The level clock at the last {@link #watch}, for the lance's start; NaN before the first. */
    private double lastSeconds = Double.NaN;
    /** The scripted loss's hit on the level clock, from the first {@link #watch}; infinite without one, NaN before. */
    private double lanceHit = Double.NaN;

    /**
     * @param looks the explosions of the level's enemy kinds
     * @param armament the fitted weapons, whose sound families the shots play
     * @param setPieces the slugs of the level's set pieces, whose death cries they play
     */
    public FlightSounds(SfxBank bank, EnemyLooks[] looks, Armament armament, List<String> setPieces) {
        this(bank, looks, armament, setPieces, java.util.Optional.empty());
    }

    /**
     * With a wingman's gun too (M5 part A): its shots carry the mount index past the armament's and
     * play its family.
     */
    public FlightSounds(
            SfxBank bank,
            EnemyLooks[] looks,
            Armament armament,
            List<String> setPieces,
            java.util.Optional<vanguard.sim.WeaponSpec> wingman) {
        this.bank = bank;
        this.looks = looks;
        cries = setPieces.stream().map(FlightSounds::cry).toArray(Sfx[]::new);
        bossSounds = setPieces.stream().map(FlightSounds::bossSounds).toArray(BossSounds[]::new);
        ownPhaseSounds = Arrays.stream(bossSounds).anyMatch(Objects::nonNull);
        int mounts = armament.size() + (wingman.isPresent() ? 1 : 0);
        shots = new Sfx[mounts];
        shotPitch = new float[mounts];
        wingman.ifPresent(gun -> {
            shots[armament.size()] = shot(gun.sfx());
            shotPitch[armament.size()] = 1;
        });
        for (int m = 0; m < armament.size(); m++) {
            shots[m] = shot(armament.mount(m).weapon().sfx());
            // Rear guns play their family lower; a mine's drop is not a gun's report.
            shotPitch[m] = armament.mount(m).slot() == Armament.Slot.REAR
                            && !armament.mount(m).weapon().sfx().equals("mine")
                    ? REAR_PITCH
                    : 1;
        }
    }

    /** How long the level's flares burn (its darkness at the difficulty): the burn loop's length. */
    public void flareSeconds(double seconds) {
        flareSeconds = seconds;
    }

    /** A set piece's cry at its death, or null for none: the Leviathan's whale song. */
    private static Sfx cry(String slug) {
        return switch (slug) {
            case "leviathan" -> Sfx.LEVIATHAN_CRY;
            // M5 part E: the Kraken's death groan, under the huge water burst (round 33 a).
            case "harbour-kraken" -> Sfx.KRAKEN_DEATH;
            default -> null;
        };
    }

    /** A boss's own sounds, or null for the generic ones: the Brood Carrier's (round 25 a, provisional). */
    private static BossSounds bossSounds(String slug) {
        return switch (slug) {
            case "brood-carrier" ->
                new BossSounds(
                        Sfx.CARRIER_ROAR,
                        Sfx.CARRIER_SAC_OPEN,
                        Sfx.CARRIER_SAC_CLOSE,
                        Sfx.CARRIER_SAC_BURST,
                        Sfx.CARRIER_IRIS);
            default -> null;
        };
    }

    /** Set piece {@code k}'s sac burst for its chained death's bursts at its sacs, or null for the generic explosions. */
    public Sfx sacBurst(int k) {
        return k < bossSounds.length && bossSounds[k] != null ? bossSounds[k].sacBurst() : null;
    }

    /** The sound of a weapon sound family; the families of later weapons play the pulse until they have theirs. */
    private static Sfx shot(String family) {
        return switch (family) {
            case "vulcan" -> Sfx.VULCAN_SHOT;
            case "ballistic" -> Sfx.BALLISTIC_SHOT;
            case "laser" -> Sfx.LASER_SHOT;
            case "micromissile" -> Sfx.MICROMISSILE_SHOT;
            case "mortar" -> Sfx.MORTAR_SHOT;
            case "bomb" -> Sfx.BOMB_SHOT;
            case "missile" -> Sfx.MISSILE_SHOT;
            case "mine" -> Sfx.MINE_DROP;
            case "torpedo" -> Sfx.SHOT_TORPEDO;
            default -> Sfx.PULSE_SHOT;
        };
    }

    /**
     * The launch rail at the start of an attempt's launch: its buffer clunk, the release, lands 3.6 s
     * into the 5 s launch. The file is levelled like the interface sounds, so it plays as it is.
     */
    public void launch() {
        bank.play(Sfx.LAUNCH_RAIL, 1, 1, 0);
    }

    /**
     * The warning tone for edge warnings that started (design/audio/sfx: warnings at the player-damage
     * level), panned towards the warned edge; one tone at a time, so overlapping warnings do not stack.
     *
     * @param edges the edges whose warning started, as {@link WarningEdge} bits
     */
    public void edgeWarnings(int edges) {
        if (edges == 0) {
            return;
        }
        float pan = WarningEdge.LEFT.in(edges) == WarningEdge.RIGHT.in(edges)
                ? 0
                : WarningEdge.LEFT.in(edges) ? -MAX_PAN : MAX_PAN;
        bank.play(Sfx.EDGE_WARNING, PLAYER_DAMAGE, 1, pan);
    }

    /**
     * A set piece's break-up after its death cry (design/enemies/space/leviathan): the {@code
     * large} rung layered under the blasts, one as the cluster builds, two together at the swap
     * where the body comes apart, and one under the trailing blasts.
     *
     * @param swap the step after the death at which the body is replaced by its chunks
     */
    public void breakUp(double x, int swap) {
        float pan = pan(x);
        later(Sfx.EXPLOSION_MEDIUM_A, EXPLOSIONS, 0.8f, pan, swap - 24);
        later(Sfx.EXPLOSION_LARGE_A, PLAYER_DAMAGE, 0.85f, pan, swap);
        later(Sfx.EXPLOSION_LARGE_B, EXPLOSIONS, 0.8f, -pan, swap + 3);
        later(Sfx.EXPLOSION_MEDIUM_B, EXPLOSIONS, 0.75f, pan, swap + 40);
    }

    /**
     * Set piece {@code k}'s death blast on the explosion ladder: the {@code huge} rung for an act
     * boss and for a set piece that is no boss (a huge regular enemy, design/enemies: Size tiers),
     * the {@code large} one for a mid-boss. An act boss's chained death plays it at the chain's end.
     */
    public Sfx deathBlast(int k) {
        return hugeDeath == null || k >= hugeDeath.length || hugeDeath[k]
                ? Sfx.EXPLOSION_HUGE_A
                : Sfx.EXPLOSION_LARGE_B;
    }

    /** Whether set piece {@code k}'s death is an act boss's tail-to-head chain, which ends in its {@link #deathBlast}. */
    private boolean finale(int k) {
        return finale != null && k < finale.length && finale[k];
    }

    private void later(Sfx sfx, float volume, float pitch, float pan, int steps) {
        if (pendingCount < MAX_PENDING) {
            pending[pendingCount] = sfx;
            pendingVolume[pendingCount] = volume;
            pendingPitch[pendingCount] = pitch;
            pendingPan[pendingCount] = pan;
            pendingSteps[pendingCount] = steps;
            pendingCount++;
        }
    }

    /** Advances the waiting sounds by one simulation step and plays the due ones. */
    public void step() {
        for (int i = pendingCount - 1; i >= 0; i--) {
            if (--pendingSteps[i] <= 0) {
                bank.play(pending[i], pendingVolume[i], pendingPitch[i], pendingPan[i]);
                pendingCount--;
                pending[i] = pending[pendingCount];
                pendingVolume[i] = pendingVolume[pendingCount];
                pendingPitch[i] = pendingPitch[pendingCount];
                pendingPan[i] = pendingPan[pendingCount];
                pendingSteps[i] = pendingSteps[pendingCount];
            }
        }
    }

    /**
     * M5 part C: whether the arcology's collapse sound starts with its warning (round 31 a: a rumble
     * that swells under the lean and the drop into the crash) rather than with the drop (a sound that
     * crashes from its first moment, as the placeholder did).
     */
    static final boolean COLLAPSE_AT_WARNING = true;

    /**
     * Where the crash sits in {@link Sfx#ARCOLOGY_COLLAPSE}, in seconds from its start: Level 09's
     * collapse impact (the 1.5 s lean and the 1.5 s drop, round 31's look c), so a sound started with
     * the warning crashes as the tower hits the ground (the file cut by tools/concept/audio/
     * sfx_r31.py's PRODUCTION at its IMPACT, built by tools/art/sfx_originals.py). The test {@code
     * SfxFilesTest} ties it to the level's impact.
     */
    static final double COLLAPSE_CRASH_SECONDS = 3.0;

    /**
     * Where the impact sits in {@link Sfx#KRAKEN_SLAM}, seconds from its start (round 33 a,
     * tools/concept/audio/sfx_r33.py): the sound starts this long before the slam.
     */
    static final double SLAM_IMPACT_SECONDS = 0.5;

    /** The steps from a lane's telegraph to the slam sound's start, and the second lane's lag; 0 without arena. */
    private int slamSteps;

    private int secondSteps;
    /** Whether the level's convoy is naval (M5 part E): ships hit and sink, not trucks. */
    private boolean naval;
    /** The level's ring-firing enemies' source (M5 part E), from the last {@link #watch}; null before. */
    private Sortie ringSource;

    /**
     * M5 part E: the Harbour Kraken's slam cycle (design/enemies/bosses/harbour-kraken): its slam
     * sound starts {@code telegraph + rise - }{@link #SLAM_IMPACT_SECONDS} after the lane's telegraph,
     * so its impact lands on the slam, and the second lane of a volley {@code second} s later.
     */
    public void slamCycle(double telegraph, double rise, double second) {
        slamSteps = Math.max(1, SimStep.ticks(telegraph + rise - SLAM_IMPACT_SECONDS));
        secondSteps = SimStep.ticks(second);
    }

    /** M5 part E: the level's convoy sails on the water: its hits and losses play the ships' sounds. */
    public void naval(boolean naval) {
        this.naval = naval;
    }

    /**
     * M5 part E: a kill, a torpedo's impact or a landing blast on the water at {@code x}: the surface
     * burst, or the under-water one ({@code under}), played over the size rung.
     */
    public void waterExplosion(boolean under, double x) {
        bank.play(under ? Sfx.EXPLOSION_UNDERWATER : Sfx.EXPLOSION_WATER, EXPLOSIONS, pitch(0.04), pan(x));
    }

    /** Whether a unit with a proximity ring stands at (x, y), where its ring just fired. */
    private boolean ringAt(double x, double y) {
        if (ringSource == null) {
            return false;
        }
        for (int e = 0; e < ringSource.enemyCount(); e++) {
            var enemy = ringSource.enemy(e);
            if (Math.abs(enemy.renderX(1) - x) < 0.5
                    && Math.abs(enemy.renderY(1) - y) < 0.5
                    && enemy.spec().ring().isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** Plays the sounds of one step's events. */
    public void play(SimEvents events) {
        int launches = 0;
        float launchPan = 0;
        int telegraphs = 0;
        for (int i = 0; i < events.size(); i++) {
            float pan = pan(events.x(i));
            switch (events.type(i)) {
                case SHOT_FIRED -> {
                    int mount = events.value(i);
                    bank.play(shots[mount], PLAYER_FIRE, shotPitch[mount] * pitch(0.05), pan);
                }
                case PROXIMITY_MINE_ARMED -> bank.play(Sfx.MINE_ARM, MINE_ARMING, pitch(0.03), pan);
                case SHOT_GLANCED ->
                    bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, 1.3f * pitch(0.05), pan);
                case BLAST ->
                    bank.play(alternate(Sfx.EXPLOSION_SMALL_A, Sfx.EXPLOSION_SMALL_B), EXPLOSIONS, pitch(0.06), pan);
                case OVERDRIVE_ENDED -> bank.play(Sfx.OVERDRIVE_END, PICKUPS, 1, 0);
                case CLAMP_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, 0.8f * pitch(0.05), pan);
                case ENEMY_HIT -> bank.play(alternate(Sfx.HIT_ORGANIC_A, Sfx.HIT_ORGANIC_B), HITS, pitch(0.05), pan);
                case ENEMY_DESTROYED -> {
                    EnemyLooks kind = looks[events.value(i)];
                    bank.play(alternate(kind.explosionA(), kind.explosionB()), EXPLOSIONS, pitch(0.04), pan);
                }
                // A heavy shot for a medium bullet: the event's value is the bullet's damage, the same
                // class boundary as its large orb.
                case ENEMY_FIRED -> {
                    if (ringAt(events.x(i), events.y(i))) {
                        // M5 part E: a Driftjelly's proximity ring (round 33 a), not a gun's report.
                        bank.play(Sfx.DRIFTJELLY_PULSE, 1, pitch(0.04), pan);
                    } else {
                        bank.play(
                                heavyShot(events.value(i))
                                        ? alternate(Sfx.ENEMY_HEAVY_SHOT_A, Sfx.ENEMY_HEAVY_SHOT_B)
                                        : alternate(Sfx.ENEMY_SHOT_A, Sfx.ENEMY_SHOT_B),
                                ENEMY_FIRE,
                                pitch(0.05),
                                pan);
                    }
                }
                case GROUND_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, pitch(0.05), pan);
                // A destroyed ground target: its blast with its crumble.
                case GROUND_DESTROYED -> groundBreak(events.value(i), pan);
                // A trigger whose spent frame is a wreck (Level 08's billboard topples) breaks the
                // same way (TriggerBreak); the others are spent without a sound of their own.
                case TRIGGER_SPENT -> {
                    int index = events.value(i);
                    if (breakingTrigger != null && index < breakingTrigger.length && breakingTrigger[index]) {
                        groundBreak(index, pan);
                    }
                }
                case MINE_DROPPED ->
                    bank.play(alternate(Sfx.HIT_ORGANIC_A, Sfx.HIT_ORGANIC_B), ENEMY_FIRE, 0.6f * pitch(0.05), pan);
                case MINE_BURST, MINE_DESTROYED ->
                    bank.play(alternate(Sfx.EXPLOSION_TINY_A, Sfx.EXPLOSION_TINY_B), EXPLOSIONS, pitch(0.06), pan);
                case DEBRIS_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, pitch(0.05), pan);
                case DEBRIS_DESTROYED ->
                    bank.play(
                            alternate(Sfx.EXPLOSION_TINY_A, Sfx.EXPLOSION_TINY_B), EXPLOSIONS, 0.8f * pitch(0.04), pan);
                case PART_DESTROYED ->
                    bank.play(alternate(Sfx.EXPLOSION_MEDIUM_A, Sfx.EXPLOSION_MEDIUM_B), EXPLOSIONS, pitch(0.04), pan);
                // The whole unit: its rung at once, or, for an act boss's tail-to-head chain, the large
                // rung as the chain starts (its huge blast ends the chain, deathBlast).
                case SET_PIECE_DESTROYED -> {
                    int k = events.value(i);
                    bank.play(finale(k) ? Sfx.EXPLOSION_LARGE_A : deathBlast(k), PLAYER_DAMAGE, pitch(0.03), pan);
                    // Levelled like the enemy sounds, so it sits under the burst (design/audio/sfx).
                    Sfx cry = cries[k];
                    if (cry != null) {
                        bank.play(cry, EXPLOSIONS, 1, pan);
                    }
                }
                case PICKUP_COLLECTED -> {
                    PickupType type = PICKUP_TYPES[events.value(i)];
                    bank.play(pickupSound(type), PICKUPS, 1, pan);
                    if (type == PickupType.OVERDRIVE) {
                        bank.play(Sfx.OVERDRIVE_START, PICKUPS, 1, 0);
                    }
                }
                case SHIELD_HIT -> bank.play(Sfx.SHIELD_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIELD_BROKEN -> bank.play(Sfx.SHIELD_BREAK, PLAYER_DAMAGE, 1, pan);
                case ARMOUR_HIT -> bank.play(Sfx.ARMOUR_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIP_DESTROYED -> {
                    bank.play(Sfx.SHIP_DESTROYED, PLAYER_DAMAGE, 1, pan);
                    bank.play(Sfx.MISSION_FAILED, PLAYER_DAMAGE, 1, 0);
                }
                case SORTIE_RESTARTED, BOSS_RETRY -> {
                    pendingCount = 0;
                    resync = true;
                }
                // The Airstrike (design/audio/sfx, Specials): the jets' flyby as the bombers enter,
                // the bomb carpet from the first blast; its single blasts play no sound of their own.
                case AIRSTRIKE_INBOUND -> bank.play(Sfx.AIRSTRIKE_JETS, EXPLOSIONS, 1, pan);
                case AIRSTRIKE_BLAST -> {
                    if (events.value(i) == 0) {
                        bank.play(Sfx.AIRSTRIKE_BOMBS, EXPLOSIONS, 1, pan);
                    }
                }
                case SPECIAL_DENIED -> bank.play(Sfx.SPECIAL_DENIED, PICKUPS, 1, 0);
                // A convoy unit: metal hit and a small explosion; the convoy lost plays the sting as a wreck does.
                // M5 part E: a naval convoy's ship hit by a slam, and a ship sinking from its loss (round 33 a).
                case ALLY_HIT ->
                    bank.play(
                            naval ? Sfx.SHIP_HIT : alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B),
                            naval ? EXPLOSIONS : HITS,
                            0.9f * pitch(0.05),
                            pan);
                case ALLY_LOST ->
                    bank.play(
                            naval ? Sfx.SHIP_SINK : alternate(Sfx.EXPLOSION_SMALL_A, Sfx.EXPLOSION_SMALL_B),
                            EXPLOSIONS,
                            1,
                            pan);
                // M5 part E: the frigate's distant flak, quiet (the file is levelled low).
                case ALLY_FLAK -> bank.play(Sfx.FRIGATE_FLAK, 1, pitch(0.05), pan);
                // The Harbour Kraken: a lane's churn now and its slam's rush so the impact lands on the
                // slam; the second lane of a volley 0.5 s after the first; the head's surfacing swell.
                case TELEGRAPH -> {
                    bank.play(Sfx.KRAKEN_CHURN, 1, 1, pan);
                    later(Sfx.KRAKEN_SLAM, 1, 1, pan, slamSteps + telegraphs * secondSteps);
                    telegraphs++;
                }
                case SURFACE -> bank.play(Sfx.KRAKEN_SURFACE, 1, 1, pan);
                // Rook (M5 part A): a metal hit on his hull, quieter than the ship's; his craft's
                // medium explosion as he ejects.
                case WINGMAN_HIT ->
                    bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), 0.7f * HITS, 0.9f * pitch(0.05), pan);
                case WINGMAN_EJECTED ->
                    bank.play(alternate(Sfx.EXPLOSION_MEDIUM_A, Sfx.EXPLOSION_MEDIUM_B), PLAYER_DAMAGE, 1, pan);
                case PRIMARY_FAILED -> bank.play(Sfx.MISSION_FAILED, PLAYER_DAMAGE, 1, 0);
                // The boss's arrival warns like an edge warning; its death pays out in a shower.
                // A boss with its own sounds roars as it comes in (after the klaxon's first blast); its
                // phases sound in watch() (the turn's roar, the iris), not as the generic blast.
                case BOSS_ARRIVED -> {
                    bank.play(Sfx.EDGE_WARNING, PICKUPS, 1, 0);
                    BossSounds own = events.value(i) < bossSounds.length ? bossSounds[events.value(i)] : null;
                    if (own != null) {
                        later(own.roar(), EXPLOSIONS, 1, pan, ARRIVAL_ROAR_STEPS);
                    }
                }
                case BOSS_PHASE -> {
                    if (!ownPhaseSounds) {
                        bank.play(alternate(Sfx.EXPLOSION_LARGE_A, Sfx.EXPLOSION_LARGE_B), EXPLOSIONS, 0.9f, pan);
                    }
                }
                // A unit leaving an open window (the carrier's sacs): counted, played after the loop.
                case BOSS_LAUNCHED -> {
                    launches++;
                    launchPan += pan;
                }
                case BOSS_DESTROYED -> bank.play(Sfx.SALVAGE_LARGE, PICKUPS, 1, pan);
                // Level 05's sounds (round 21): the Polyp Mortar's lob and impact; the rail's charge
                // hum, its loop played twice over the lights' 1.5 s chase, and the sled's pass.
                case MORTAR_LOBBED -> bank.play(Sfx.MORTAR_LOB, 0.6f * EXPLOSIONS, pitch(0.05), pan);
                case MORTAR_IMPACT -> bank.play(Sfx.MORTAR_IMPACT, 0.7f * EXPLOSIONS, pitch(0.05), pan);
                case SLED_LIGHTS -> {
                    bank.play(Sfx.SLED_WHINE, 0.7f, 1f, pan);
                    later(Sfx.SLED_WHINE, 0.7f, 1f, pan, SLED_WHINE_LOOP_STEPS);
                }
                case SLED_LAUNCHED -> bank.play(Sfx.SLED_PASS, 0.8f, 1f, pan);
                // A Brood Pod's fleshy burst, shot or on its own (round 08 b), as its Skitters fly out.
                case BROOD_HATCHED -> bank.play(Sfx.BROOD_BURST, EXPLOSIONS, pitch(0.04), pan);
                // Level 06: a cut chain's wet tear (round 27 a, provisional), its regrowth, the
                // chained pops, the Mantis's telegraph and beam, the Smart Bomb, the flare's launch
                // and burn.
                case CHAIN_CUT -> bank.play(Sfx.COILWYRM_CUT, EXPLOSIONS, pitch(0.04), pan);
                // The new head growing (round 23 b, an insect growl and chitter).
                case CHAIN_REGROWN -> bank.play(Sfx.COILWYRM_REGROW, EXPLOSIONS, pitch(0.04), pan);
                // A chained death's burst (round 24): the member's own burst, in the step its look bursts.
                case CHAIN_POP -> {
                    int value = events.value(i);
                    EnemyLooks kind = looks[SimEvents.chainPopKind(value)];
                    bank.play(
                            alternate(kind.explosionA(), kind.explosionB()),
                            EXPLOSIONS,
                            ripplePitch(SimEvents.chainPopWidth(value)) * pitch(0.02),
                            pan);
                }
                // The Mantis (round 23): its telegraph a warning (the player-damage level), its beam a
                // little above the enemy fire, one long sound.
                case SWEEP_TELEGRAPH -> bank.play(Sfx.MANTIS_TELEGRAPH, PLAYER_DAMAGE, pitch(0.03), pan);
                // M5 part C: the Hive Node's iris and release, the Ravager's take-off, the arcology's collapse.
                case SPAWN_TELEGRAPH -> bank.play(Sfx.HIVE_IRIS, HITS, pitch(0.04), pan);
                case SPAWN_RELEASED -> bank.play(Sfx.HIVE_SPAWN, EXPLOSIONS, pitch(0.04), pan);
                case POUNCE -> bank.play(pounce(random), HITS, pitch(0.06), pan);
                case COLLAPSE_WARNING -> {
                    if (COLLAPSE_AT_WARNING) {
                        bank.play(Sfx.ARCOLOGY_COLLAPSE, EXPLOSIONS, 1, 0);
                    }
                }
                case COLLAPSE_FALL -> {
                    if (!COLLAPSE_AT_WARNING) {
                        bank.play(Sfx.ARCOLOGY_COLLAPSE, EXPLOSIONS, 1, 0);
                    }
                }
                case SWEEP_FIRED -> bank.play(Sfx.MANTIS_SWEEP, HITS, pitch(0.03), pan);
                // M5 part D: a Wraith's decloak and a Mote Swarm's loop-back, levelled like the Vrell
                // spawns (round 32: decloak b, swarm b).
                case DECLOAK, LOOP_BACK -> bank.play(eventSound(events.type(i)), EXPLOSIONS, pitch(0.03), pan);
                // The Smart Bomb (round 08 a): its energy blast, swelling over 0.5 s, on the huge rung's
                // sub-heavy boom, which gives the instant flash its punch.
                case SMART_BOMB -> {
                    bank.play(Sfx.SMART_BOMB, PLAYER_DAMAGE, 1, 0);
                    bank.play(Sfx.EXPLOSION_HUGE_B, 0.7f * EXPLOSIONS, 1, pan);
                }
                // The perimeter beacon's flare (round 23 a): the flare-gun shot, then the road flare's
                // 3 s loop back to back while it burns, the last play quieter as the pool fades.
                case FLARE_FIRED -> {
                    bank.play(Sfx.FLARE_LAUNCH, 0.6f * EXPLOSIONS, pitch(0.03), pan);
                    bank.play(Sfx.FLARE_BURN, ENEMY_FIRE, 1, pan);
                    for (int k = 1; k * FLARE_BURN_LOOP_SECONDS < flareSeconds; k++) {
                        boolean last = (k + 1) * FLARE_BURN_LOOP_SECONDS >= flareSeconds;
                        later(
                                Sfx.FLARE_BURN,
                                (last ? 0.5f : 1) * ENEMY_FIRE,
                                1,
                                pan,
                                SimStep.ticks(k * FLARE_BURN_LOOP_SECONDS));
                    }
                }
                case BROOD_BURST,
                        WALKER_DOWN,
                        SWEEP_HIT,
                        SECRET_FOUND,
                        CREDITS_PICKED_UP,
                        RADIO,
                        OBJECTIVE_MET,
                        LEVEL_COMPLETE,
                        GROUP_CLEARED,
                        GROUP_LOST,
                        OBJECTIVE_FAILED,
                        SET_PIECE_DESCENDED,
                        SET_PIECE_ESCAPED,
                        SLED_HIT,
                        ROCK_THROWN,
                        SPECIAL_CALLED -> {}
            }
        }
        // An opening's units all leave in one step: a few spits just after the sac opens, not one each.
        for (int n = 0; n < Math.min(launches, LAUNCH_SOUNDS); n++) {
            later(Sfx.CARRIER_LAUNCH, EXPLOSIONS, pitch(0.06), launchPan / launches, (n + 1) * LAUNCH_STEPS);
        }
    }

    /** M5 part D: the sound of a Level 10 event: the Wraith's decloak, the swarm's loop-back; null for another. */
    static Sfx eventSound(SimEvents.Type type) {
        return switch (type) {
            case DECLOAK -> Sfx.WRAITH_DECLOAK;
            case LOOP_BACK -> Sfx.MOTE_SWARM;
            default -> null;
        };
    }

    /**
     * M5 part D: whether the lance's sound starts between the level clock {@code before} and {@code
     * now}: its start {@link #LANCE_IMPACT_SECONDS} before the hit at {@code hitSeconds} lies after
     * the one and at or before the other.
     */
    static boolean lanceDue(double before, double now, double hitSeconds) {
        double start = hitSeconds - LANCE_IMPACT_SECONDS;
        return before < start && start <= now;
    }

    /**
     * A ground target's blast with its crumble (round 08): a structure's collapse or a small
     * target's rubble burst, under the blast.
     */
    private void groundBreak(int index, float pan) {
        bank.play(alternate(Sfx.EXPLOSION_SMALL_C, Sfx.EXPLOSION_SMALL_A), EXPLOSIONS, pitch(0.04), pan);
        boolean large = largeGround != null && index < largeGround.length && largeGround[index];
        bank.play(large ? Sfx.CRUMBLE_LARGE : Sfx.CRUMBLE_SMALL, 0.7f * EXPLOSIONS, pitch(0.05), pan);
    }

    /**
     * Once a simulation step, after {@link #play}: the sounds that follow a state rather than an
     * event. A boss with its own sounds (round 25, the Brood Carrier): its sacs opening (one sound
     * for the sacs that open together) and shutting with their windows, a sac shot off bursting (not
     * at its death, whose chain plays them), its iris opening as the core is exposed, its roar as its
     * turn starts. A tow's cable snapping as its pod falls free. And the Vrell screech as a large
     * unit enters the screen ({@link ScreechCue}).
     */
    public void watch(Sortie sortie) {
        int pieces = Math.min(bossSounds.length, sortie.setPieceCount());
        if (largeGround == null) {
            var objects = sortie.script().groundObjects();
            largeGround = new boolean[objects.size()];
            breakingTrigger = new boolean[objects.size()];
            for (int g = 0; g < largeGround.length; g++) {
                var size = objects.get(g).size();
                largeGround[g] = size.width() * size.height() >= LARGE_GROUND_AREA;
                breakingTrigger[g] = TriggerBreak.breaks(objects.get(g));
            }
            hugeDeath = new boolean[sortie.setPieceCount()];
            finale = new boolean[sortie.setPieceCount()];
            for (int k = 0; k < hugeDeath.length; k++) {
                SetPiece piece = sortie.setPiece(k);
                boolean boss = piece.boss().isPresent();
                hugeDeath[k] = !boss || LevelRenderer.flashesAtDeath(piece);
                finale[k] = boss && LevelRenderer.tailToHead(piece);
            }
        }
        watchShip(sortie);
        // Levelled like the Vrell spawns (design/audio/sfx), so it sits under the explosions.
        Sfx screeched = screech.watch(sortie, resync);
        if (screeched != null) {
            bank.play(screeched, EXPLOSIONS, pitch(0.03), pan(screech.x()));
        }
        // M5 part D: a swarm's entry; the scripted loss's lance, timed so its impact lands on the hit.
        if (swarms.watch(sortie, resync)) {
            bank.play(Sfx.MOTE_SWARM, EXPLOSIONS, pitch(0.03), pan(swarms.x()));
        }
        watchLance(sortie);
        ringSource = sortie;
        if (wasOpen == null) {
            wasOpen = new boolean[pieces][];
            wasWrecked = new boolean[pieces][];
            wasTurning = new boolean[pieces];
            wasHolding = new boolean[sortie.towCount()];
            for (int k = 0; k < pieces; k++) {
                int parts = sortie.setPiece(k).partCount();
                wasOpen[k] = new boolean[parts];
                wasWrecked[k] = new boolean[parts];
            }
            resync = true;
        }
        for (int k = 0; k < pieces; k++) {
            if (bossSounds[k] != null) {
                watchBoss(bossSounds[k], sortie.setPiece(k), k);
            }
        }
        for (int i = 0; i < wasHolding.length; i++) {
            Tow tow = sortie.tow(i);
            boolean holding = tow.holding();
            if (!resync && wasHolding[i] && !holding) {
                bank.play(Sfx.CABLE_SNAP, EXPLOSIONS, pitch(0.03), pan(tow.podX()));
            }
            wasHolding[i] = holding;
        }
        resync = false;
    }

    /**
     * The scripted loss's lance (M5 part D, Level 10): its sound starts {@link #LANCE_IMPACT_SECONDS}
     * before the hit, panned to its unit; not after a restart that skipped past it.
     */
    private void watchLance(Sortie sortie) {
        if (Double.isNaN(lanceHit)) {
            lanceHit = sortie.script()
                    .escort()
                    .flatMap(vanguard.sim.LevelScript.Escort::air)
                    .flatMap(vanguard.sim.LevelScript.Air::scriptedLoss)
                    .map(vanguard.sim.LevelScript.ScriptedLoss::t)
                    .orElse(Double.POSITIVE_INFINITY);
        }
        double now = sortie.levelSeconds();
        double before = resync ? now : lastSeconds;
        lastSeconds = now;
        int unit = sortie.scriptedAlly();
        if (unit >= 0 && !Double.isNaN(before) && lanceDue(before, now, lanceHit)) {
            bank.play(Sfx.LANCE_STRIKE, PLAYER_DAMAGE, 1, pan(sortie.ally(unit).x()));
        }
    }

    /**
     * The ship's warnings: the low-armour beeps while it flies, and the shield's restore chime once
     * it is full again after a break (not after every hit: it regenerates after each).
     */
    private void watchShip(Sortie sortie) {
        Defences defences = sortie.ship().defences();
        boolean flying = sortie.flying() && !sortie.complete();
        int period = flying ? beepPeriod(defences.armour(), defences.maxArmour()) : 0;
        if (period == 0) {
            beepIn = 0;
        } else if (--beepIn <= 0) {
            bank.play(Sfx.LOW_ARMOUR, LOW_ARMOUR_LEVEL, 1, 0);
            beepIn = period;
        } else {
            beepIn = Math.min(beepIn, period);
        }
        if (resync || !flying) {
            shieldDown = false;
        } else if (defences.broken()) {
            shieldDown = true;
        } else if (shieldDown && defences.shield() >= defences.maxShield()) {
            bank.play(Sfx.SHIELD_RESTORE, PICKUPS, 1, 0);
            shieldDown = false;
        }
    }

    /** Whether an enemy shot of that damage ({@code ENEMY_FIRED}'s value) fires a medium bullet: a heavy shot. */
    static boolean heavyShot(int damage) {
        return damage >= BulletLooks.MEDIUM_DAMAGE;
    }

    /** The steps between low-armour beeps at {@code armour} of {@code maxArmour}; 0 for none (a wreck neither). */
    static int beepPeriod(double armour, double maxArmour) {
        if (armour <= 0) {
            return 0;
        }
        return switch (LowArmour.of(armour, maxArmour)) {
            case NONE -> 0;
            case LOW -> SimStep.ticks(SLOW_BEEP_SECONDS);
            case CRITICAL -> SimStep.ticks(FAST_BEEP_SECONDS);
        };
    }

    private void watchBoss(BossSounds own, SetPiece piece, int k) {
        boolean turning = piece.motion() == SetPiece.Motion.TURN;
        if (!resync && turning && !wasTurning[k]) {
            bank.play(own.roar(), EXPLOSIONS, TURN_ROAR_PITCH, pan(piece.renderX(1)));
        }
        wasTurning[k] = turning;
        boolean dying = false;
        for (int p = 0; p < piece.partCount(); p++) {
            dying |= vital(piece, p) && piece.partWrecked(p) && !wasWrecked[k][p];
        }
        int opened = 0;
        int shut = 0;
        double openedX = 0;
        double shutX = 0;
        for (int p = 0; p < piece.partCount(); p++) {
            boolean wrecked = piece.partWrecked(p);
            // As the renderer reads it: a sac open in its window, the core once it takes damage.
            boolean open = !wrecked && (piece.partWindowed(p) ? piece.partOpen(p) : !piece.partShielded(p));
            if (!resync && !piece.partArmoured(p)) {
                if (vital(piece, p)) {
                    if (open && !wasOpen[k][p]) {
                        bank.play(own.iris(), EXPLOSIONS, 1, pan(piece.partX(p)));
                    }
                } else if (wrecked && !wasWrecked[k][p]) {
                    if (!dying) {
                        bank.play(own.sacBurst(), EXPLOSIONS, pitch(0.04), pan(piece.partX(p)));
                    }
                } else if (open && !wasOpen[k][p]) {
                    opened++;
                    openedX += piece.partX(p);
                } else if (!open && wasOpen[k][p] && !wrecked) {
                    shut++;
                    shutX += piece.partX(p);
                }
            }
            wasOpen[k][p] = open;
            wasWrecked[k][p] = wrecked;
        }
        if (opened > 0) {
            bank.play(own.sacOpen(), EXPLOSIONS, pitch(0.04), pan(openedX / opened));
        }
        if (shut > 0) {
            bank.play(own.sacClose(), EXPLOSIONS, pitch(0.04), pan(shutX / shut));
        }
    }

    private static boolean vital(SetPiece piece, int p) {
        return piece.spec().parts().get(p).vital();
    }

    /** A chained burst's pitch by the member's hit box width (see {@link #RIPPLE_REFERENCE_WIDTH}). */
    static float ripplePitch(double width) {
        double ratio = RIPPLE_REFERENCE_WIDTH / Math.max(1, width);
        return (float) Math.min(RIPPLE_PITCH_MAX, RIPPLE_PITCH_START * Math.pow(ratio, RIPPLE_PITCH_EXPONENT));
    }

    /** A pickup's sound (design/audio/sfx, Pickups); the overdrive's start cue plays with its pickup. */
    static Sfx pickupSound(PickupType type) {
        return switch (type) {
            case SMALL_SALVAGE -> Sfx.SALVAGE_SMALL;
            case MEDIUM_SALVAGE -> Sfx.SALVAGE_MEDIUM;
            case HIDDEN_CRATE, LARGE_SALVAGE -> Sfx.SALVAGE_LARGE;
            case SPECIAL_CHARGE -> Sfx.SPECIAL_CHARGE;
            case OVERDRIVE -> Sfx.POWER_UP;
            case DATA_CORE -> Sfx.DATA_CORE;
            case SHIELD_CELL -> Sfx.SHIELD_CELL;
            case ARMOUR_PATCH -> Sfx.ARMOUR_PATCH;
        };
    }

    /**
     * The Ravager's pounce, a or b at random each time (round 31: both kept): drawn from the sound's
     * own generator, never the simulation's, which stays deterministic.
     */
    static Sfx pounce(SplitMix64 random) {
        return random.nextInt(2) == 0 ? Sfx.RAVAGER_POUNCE_A : Sfx.RAVAGER_POUNCE_B;
    }

    /** Two variants in turn, so no file plays twice in a row. */
    private Sfx alternate(Sfx a, Sfx b) {
        variantB = !variantB;
        return variantB ? b : a;
    }

    private float pitch(double variation) {
        return (float) random.range(1 - variation, 1 + variation);
    }

    private static float pan(double x) {
        return (float) (x / PlayField.WIDTH * 2 - 1) * MAX_PAN;
    }

    static float decibels(double db) {
        return (float) Math.pow(10, db / 20);
    }
}
