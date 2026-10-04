package vanguard.game.audio;

import java.util.List;
import vanguard.game.render.EnemyLooks;
import vanguard.sim.Armament;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.SplitMix64;
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
     * @param looks the explosions of the level's enemy kinds
     * @param armament the fitted weapons, whose sound families the shots play
     * @param setPieces the slugs of the level's set pieces, whose death cries they play
     */
    public FlightSounds(SfxBank bank, EnemyLooks[] looks, Armament armament, List<String> setPieces) {
        this.bank = bank;
        this.looks = looks;
        cries = setPieces.stream().map(FlightSounds::cry).toArray(Sfx[]::new);
        shots = new Sfx[armament.size()];
        shotPitch = new float[armament.size()];
        for (int m = 0; m < armament.size(); m++) {
            shots[m] = shot(armament.mount(m).weapon().sfx());
            shotPitch[m] = armament.mount(m).slot() == Armament.Slot.REAR ? REAR_PITCH : 1;
        }
    }

    /** A set piece's cry at its death, or null for none: the Leviathan's whale song. */
    private static Sfx cry(String slug) {
        return switch (slug) {
            case "leviathan" -> Sfx.LEVIATHAN_CRY;
            default -> null;
        };
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
                case SORTIE_RESTARTED -> pendingCount = 0;
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
                case BOSS_ARRIVED -> bank.play(Sfx.EDGE_WARNING, PICKUPS, 1, 0);
                case BOSS_PHASE -> bank.play(Sfx.EXPLOSION_SMALL_B, EXPLOSIONS, 0.8f, pan);
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
                case BROOD_BURST,
                        WALKER_DOWN,
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
    }

    private static Sfx pickupSound(PickupType type) {
        return switch (type) {
            // A special charge plays the small salvage until its own sound is imported.
            case SMALL_SALVAGE, MEDIUM_SALVAGE, SPECIAL_CHARGE -> Sfx.SALVAGE_SMALL;
            case OVERDRIVE -> Sfx.OVERDRIVE_START;
            case HIDDEN_CRATE, LARGE_SALVAGE -> Sfx.SALVAGE_LARGE;
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
