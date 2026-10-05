package vanguard.game.level;

import vanguard.sim.Defences;

/**
 * The low-armour warning stages (design/player/armor: warnings at 30 % and 15 % of the max armour;
 * design/ui/hud: the armour readout flashes red at both). The HUD flashes the armour gauge, slowly
 * at {@link #LOW} and fast at {@link #CRITICAL}; the low-armour beeps ({@code FlightSounds}) use the
 * same thresholds.
 */
public enum LowArmour {
    /** Above 30 %. */
    NONE,
    /** At or below 30 %: smoke, beeping, the readout flashing. */
    LOW,
    /** At or below 15 %: sparks, a faster beep, the radio line, the readout flashing fast. */
    CRITICAL;

    /** The share of the max armour at or below which the warnings start. */
    public static final double LOW_SHARE = 0.30;

    /** The share at or below which they turn critical: the simulation's, which sets off the radio line there. */
    public static final double CRITICAL_SHARE = Defences.CRITICAL_SHARE;

    /** The stage at {@code armour} of {@code maxArmour}. */
    public static LowArmour of(double armour, double maxArmour) {
        double share = maxArmour > 0 ? armour / maxArmour : 1;
        if (share <= CRITICAL_SHARE) {
            return CRITICAL;
        }
        return share <= LOW_SHARE ? LOW : NONE;
    }

    /** The ship's stage now. */
    public static LowArmour of(Defences defences) {
        return of(defences.armour(), defences.maxArmour());
    }
}
