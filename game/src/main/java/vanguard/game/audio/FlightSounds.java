package vanguard.game.audio;

import vanguard.game.render.EnemyLooks;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.SplitMix64;

/**
 * Turns the simulation's events into sound effects, mixed per design/audio/sfx (Mixing rules):
 * relative levels, a few percent of random pitch, two variants alternating so no file repeats
 * twice in a row, and a subtle pan by the position in the play field.
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

    private final SfxBank bank;
    private final EnemyLooks[] looks;
    private final SplitMix64 random = new SplitMix64(0x5F3);
    private boolean variantB;

    /** @param looks the explosions of the level's enemy kinds */
    public FlightSounds(SfxBank bank, EnemyLooks[] looks) {
        this.bank = bank;
        this.looks = looks;
    }

    /** Plays the sounds of one step's events. */
    public void play(SimEvents events) {
        for (int i = 0; i < events.size(); i++) {
            float pan = pan(events.x(i));
            switch (events.type(i)) {
                case SHOT_FIRED -> bank.play(Sfx.PULSE_SHOT, PLAYER_FIRE, pitch(0.05), pan);
                case ENEMY_HIT -> bank.play(alternate(Sfx.HIT_ORGANIC_A, Sfx.HIT_ORGANIC_B), HITS, pitch(0.05), pan);
                case ENEMY_DESTROYED -> {
                    EnemyLooks kind = looks[events.value(i)];
                    bank.play(alternate(kind.explosionA(), kind.explosionB()), EXPLOSIONS, pitch(0.04), pan);
                }
                case ENEMY_FIRED ->
                    bank.play(alternate(Sfx.ENEMY_SHOT_A, Sfx.ENEMY_SHOT_B), ENEMY_FIRE, pitch(0.05), pan);
                case GROUND_HIT -> bank.play(alternate(Sfx.HIT_METAL_A, Sfx.HIT_METAL_B), HITS, pitch(0.05), pan);
                case GROUND_DESTROYED -> bank.play(Sfx.EXPLOSION_SMALL_A, EXPLOSIONS, pitch(0.04), pan);
                case PICKUP_COLLECTED -> bank.play(pickupSound(PICKUP_TYPES[events.value(i)]), PICKUPS, 1, pan);
                case SHIELD_HIT -> bank.play(Sfx.SHIELD_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIELD_BROKEN -> bank.play(Sfx.SHIELD_BREAK, PLAYER_DAMAGE, 1, pan);
                case ARMOUR_HIT -> bank.play(Sfx.ARMOUR_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIP_DESTROYED -> {
                    bank.play(Sfx.SHIP_DESTROYED, PLAYER_DAMAGE, 1, pan);
                    bank.play(Sfx.MISSION_FAILED, PLAYER_DAMAGE, 1, 0);
                }
                case SECRET_FOUND, CREDITS_PICKED_UP, RADIO, OBJECTIVE_MET, LEVEL_COMPLETE, SORTIE_RESTARTED -> {}
            }
        }
    }

    private static Sfx pickupSound(PickupType type) {
        return switch (type) {
            case SMALL_SALVAGE -> Sfx.SALVAGE_SMALL;
            case HIDDEN_CRATE -> Sfx.SALVAGE_LARGE;
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
