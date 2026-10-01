package vanguard.game.audio;

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
    /** Hits have no level of their own in the rules; they sit between explosions and player fire. */
    private static final float HITS = decibels(-8);

    private static final float PLAYER_FIRE = decibels(-14);
    private static final float MAX_PAN = 0.4f;

    private final SfxBank bank;
    private final SplitMix64 random = new SplitMix64(0x5F3);
    private boolean hitVariantB;
    private boolean explosionVariantB;

    public FlightSounds(SfxBank bank) {
        this.bank = bank;
    }

    /** Plays the sounds of one step's events. */
    public void play(SimEvents events) {
        for (int i = 0; i < events.size(); i++) {
            float pan = pan(events.x(i));
            switch (events.type(i)) {
                case SHOT_FIRED -> bank.play(Sfx.PULSE_SHOT, PLAYER_FIRE, pitch(0.05), pan);
                case ENEMY_HIT -> {
                    hitVariantB = !hitVariantB;
                    bank.play(hitVariantB ? Sfx.HIT_ORGANIC_B : Sfx.HIT_ORGANIC_A, HITS, pitch(0.05), pan);
                }
                case ENEMY_DESTROYED -> {
                    explosionVariantB = !explosionVariantB;
                    Sfx explosion = explosionVariantB ? Sfx.EXPLOSION_TINY_B : Sfx.EXPLOSION_TINY_A;
                    bank.play(explosion, EXPLOSIONS, pitch(0.04), pan);
                }
                case SHIELD_HIT -> bank.play(Sfx.SHIELD_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIELD_BROKEN -> bank.play(Sfx.SHIELD_BREAK, PLAYER_DAMAGE, 1, pan);
                case ARMOUR_HIT -> bank.play(Sfx.ARMOUR_HIT, PLAYER_DAMAGE, pitch(0.05), pan);
                case SHIP_DESTROYED -> {
                    bank.play(Sfx.SHIP_DESTROYED, PLAYER_DAMAGE, 1, pan);
                    bank.play(Sfx.MISSION_FAILED, PLAYER_DAMAGE, 1, 0);
                }
                case SORTIE_RESTARTED -> {}
            }
        }
    }

    private float pitch(double variation) {
        return (float) random.range(1 - variation, 1 + variation);
    }

    private static float pan(double x) {
        return (float) (x / PlayField.WIDTH * 2 - 1) * MAX_PAN;
    }

    private static float decibels(double db) {
        return (float) Math.pow(10, db / 20);
    }
}
