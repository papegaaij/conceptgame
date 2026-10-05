package vanguard.game.audio;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import vanguard.game.render.EnemyLooks;
import vanguard.sim.Armament;
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
 * twice in a row, and a subtle pan by the position in the play field. Level 03's events reuse the
 * sounds there are until they get their own: a spore drops with a soft low organic plop, bursts or
 * is shot with the tiny explosion, debris rings like metal and breaks with a low tiny explosion, a
 * set piece's part blows with a lower small explosion and the whole unit with both small
 * explosions pitched down.
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

    /**
     * @param looks the explosions of the level's enemy kinds
     * @param armament the fitted weapons, whose sound families the shots play
     * @param setPieces the slugs of the level's set pieces, whose death cries they play
     */
    public FlightSounds(SfxBank bank, EnemyLooks[] looks, Armament armament, List<String> setPieces) {
        this.bank = bank;
        this.looks = looks;
        cries = setPieces.stream().map(FlightSounds::cry).toArray(Sfx[]::new);
        bossSounds = setPieces.stream().map(FlightSounds::bossSounds).toArray(BossSounds[]::new);
        ownPhaseSounds = Arrays.stream(bossSounds).anyMatch(Objects::nonNull);
        shots = new Sfx[armament.size()];
        shotPitch = new float[armament.size()];
        for (int m = 0; m < armament.size(); m++) {
            shots[m] = shot(armament.mount(m).weapon().sfx());
            shotPitch[m] = armament.mount(m).slot() == Armament.Slot.REAR ? REAR_PITCH : 1;
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
     * A set piece's break-up after its death cry (design/enemies/space/leviathan): deep, slowed
     * explosions layered under the blasts, one as the cluster builds, two together at the swap
     * where the body comes apart, and one under the trailing blasts.
     *
     * @param swap the step after the death at which the body is replaced by its chunks
     */
    public void breakUp(double x, int swap) {
        float pan = pan(x);
        later(Sfx.EXPLOSION_SMALL_A, EXPLOSIONS, 0.6f, pan, swap - 24);
        later(Sfx.EXPLOSION_SMALL_B, PLAYER_DAMAGE, 0.5f, pan, swap);
        later(Sfx.EXPLOSION_SMALL_A, EXPLOSIONS, 0.55f, -pan, swap + 3);
        later(Sfx.EXPLOSION_SMALL_B, EXPLOSIONS, 0.65f, pan, swap + 40);
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

    /** Plays the sounds of one step's events. */
    public void play(SimEvents events) {
        int launches = 0;
        float launchPan = 0;
        for (int i = 0; i < events.size(); i++) {
            float pan = pan(events.x(i));
            switch (events.type(i)) {
                case SHOT_FIRED -> {
                    int mount = events.value(i);
                    bank.play(shots[mount], PLAYER_FIRE, shotPitch[mount] * pitch(0.05), pan);
                }
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
                case ENEMY_FIRED ->
                    bank.play(alternate(Sfx.ENEMY_SHOT_A, Sfx.ENEMY_SHOT_B), ENEMY_FIRE, pitch(0.05), pan);
                case GROUND_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, pitch(0.05), pan);
                case GROUND_DESTROYED -> bank.play(Sfx.EXPLOSION_SMALL_A, EXPLOSIONS, pitch(0.04), pan);
                case MINE_DROPPED ->
                    bank.play(alternate(Sfx.HIT_ORGANIC_A, Sfx.HIT_ORGANIC_B), ENEMY_FIRE, 0.6f * pitch(0.05), pan);
                case MINE_BURST, MINE_DESTROYED ->
                    bank.play(alternate(Sfx.EXPLOSION_TINY_A, Sfx.EXPLOSION_TINY_B), EXPLOSIONS, pitch(0.06), pan);
                case DEBRIS_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, pitch(0.05), pan);
                case DEBRIS_DESTROYED ->
                    bank.play(
                            alternate(Sfx.EXPLOSION_TINY_A, Sfx.EXPLOSION_TINY_B), EXPLOSIONS, 0.8f * pitch(0.04), pan);
                case PART_DESTROYED ->
                    bank.play(
                            alternate(Sfx.EXPLOSION_SMALL_A, Sfx.EXPLOSION_SMALL_B),
                            EXPLOSIONS,
                            0.85f * pitch(0.04),
                            pan);
                case SET_PIECE_DESTROYED -> {
                    bank.play(Sfx.EXPLOSION_SMALL_A, PLAYER_DAMAGE, 0.6f, pan);
                    bank.play(Sfx.EXPLOSION_SMALL_B, EXPLOSIONS, 0.7f, pan);
                    // Levelled like the enemy sounds, so it sits under the burst (design/audio/sfx).
                    Sfx cry = cries[events.value(i)];
                    if (cry != null) {
                        bank.play(cry, EXPLOSIONS, 1, pan);
                    }
                }
                case PICKUP_COLLECTED -> bank.play(pickupSound(PICKUP_TYPES[events.value(i)]), PICKUPS, 1, pan);
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
                case ALLY_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, 0.9f * pitch(0.05), pan);
                case ALLY_LOST ->
                    bank.play(alternate(Sfx.EXPLOSION_SMALL_A, Sfx.EXPLOSION_SMALL_B), EXPLOSIONS, 1, pan);
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
                        bank.play(Sfx.EXPLOSION_SMALL_B, EXPLOSIONS, 0.8f, pan);
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
                // Level 06: a cut chain's wet tear (a placeholder from the existing sounds), its
                // regrowth, the chained pops, the Mantis's telegraph and beam, the Smart Bomb's huge
                // blast with a whoosh (placeholders), the flare's launch and burn.
                case CHAIN_CUT -> bank.play(Sfx.BROOD_BURST, EXPLOSIONS, 0.8f * pitch(0.04), pan);
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
                case SWEEP_FIRED -> bank.play(Sfx.MANTIS_SWEEP, HITS, pitch(0.03), pan);
                case SMART_BOMB -> {
                    bank.play(Sfx.EXPLOSION_SMALL_A, PLAYER_DAMAGE, 0.55f, pan);
                    bank.play(Sfx.EXPLOSION_SMALL_B, EXPLOSIONS, 0.7f, pan);
                    bank.play(Sfx.AIRSTRIKE_JETS, EXPLOSIONS, 1.5f, 0);
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

    /**
     * Once a simulation step, after {@link #play}: the sounds that follow a state rather than an
     * event. A boss with its own sounds (round 25, the Brood Carrier): its sacs opening (one sound
     * for the sacs that open together) and shutting with their windows, a sac shot off bursting (not
     * at its death, whose chain plays them), its iris opening as the core is exposed, its roar as its
     * turn starts. And a tow's cable snapping as its pod falls free.
     */
    public void watch(Sortie sortie) {
        int pieces = Math.min(bossSounds.length, sortie.setPieceCount());
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

    private static Sfx pickupSound(PickupType type) {
        return switch (type) {
            // A special charge plays the small salvage until its own sound is imported.
            case SMALL_SALVAGE, MEDIUM_SALVAGE, SPECIAL_CHARGE -> Sfx.SALVAGE_SMALL;
            case OVERDRIVE -> Sfx.OVERDRIVE_START;
            case HIDDEN_CRATE, LARGE_SALVAGE, DATA_CORE -> Sfx.SALVAGE_LARGE;
            case SHIELD_CELL -> Sfx.SHIELD_CELL;
            case ARMOUR_PATCH -> Sfx.ARMOUR_PATCH;
        };
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
