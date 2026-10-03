package vanguard.content;

import java.util.List;
import java.util.Optional;

/**
 * design/player/specials/data.yaml: the charge-based specials and the Airstrike's numbers.
 *
 * @param inputBuffer seconds a press of the special button waits while the special is busy
 */
public record SpecialsData(double inputBuffer, List<Special> specials, Airstrike airstrike) {
    public SpecialsData {
        Check.notNegative("input_buffer", inputBuffer);
        specials = List.copyOf(specials);
    }

    /**
     * A special bought by the charge; available from level {@code unlock}.
     *
     * @param freeCharges charges given once, at the hangar visit before the unlock level
     */
    public record Special(String name, int chargePrice, int maxCharges, int unlock, Optional<Integer> freeCharges) {
        public Special {
            Check.notNegative("charge_price", chargePrice);
            Check.positive("max_charges", maxCharges);
            Check.positive("unlock", unlock);
            freeCharges.ifPresent(free -> Check.notNegative("free_charges", free));
        }

        /** The charges given once at the unlock; 0 for none. */
        public int free() {
            return freeCharges.orElse(0);
        }
    }

    /**
     * The Airstrike (design/player/specials, Airstrike (L04)).
     *
     * @param delay seconds from the call to the bombers entering at the bottom edge
     * @param offset px left and right of the ship's x at the call
     * @param speed the bombers' px/s up the screen
     * @param bomberSize the bomber sprite, px, facing up
     * @param bombSpacing px of travel between two bombs of a bomber
     * @param fall seconds from a bomb's release to its blast
     * @param blastRadius px
     */
    public record Airstrike(
            double delay,
            double offset,
            double speed,
            Size bomberSize,
            double bombSpacing,
            double fall,
            double blastRadius,
            Damage damage,
            Cap cap,
            Radio radio) {
        public Airstrike {
            Check.notNegative("delay", delay);
            Check.notNegative("offset", offset);
            Check.positive("speed", speed);
            Check.positive("bomb_spacing", bombSpacing);
            Check.positive("fall", fall);
            Check.positive("blast_radius", blastRadius);
        }
    }

    /** Damage per blast to ground and low-air targets, and to air targets. */
    public record Damage(double ground, double air) {
        public Damage {
            Check.notNegative("ground", ground);
            Check.notNegative("air", air);
        }
    }

    /** The most damage one strike deals to one ground or low-air target, air target and boss part. */
    public record Cap(double ground, double air, double bossPart) {
        public Cap {
            Check.notNegative("ground", ground);
            Check.notNegative("air", air);
            Check.notNegative("boss_part", bossPart);
        }
    }

    /** The call's radio line. */
    public record Radio(String speaker, String portrait, String line) {}
}
