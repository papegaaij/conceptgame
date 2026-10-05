package vanguard.content;

import java.util.List;
import java.util.Optional;

/**
 * design/systems/scoring/data.yaml.
 *
 * @param killScore score per kill = bounty × killScore × chain multiplier
 * @param pickupScore pickups and bonuses score pickupScore × their credit value
 * @param rating the grade rating's weights and the chain that earns the chain part in full
 */
public record ScoringData(
        double killScore, double pickupScore, Chain chain, Rating rating, List<Bonus> bonuses, List<Grade> grades) {
    public ScoringData {
        Check.notEmpty("grades", grades);
        for (int i = 0; i < grades.size() - 1; i++) {
            Check.that(grades.get(i).rating().isPresent(), "grades[" + i + "]: only the last grade has no rating");
        }
        Check.that(grades.getLast().rating().isEmpty(), "grades: the last grade has no rating");
    }

    /**
     * Kills within {@code window} seconds keep the chain; every {@code step} kills add
     * {@code increment} to the multiplier, up to {@code max}.
     */
    public record Chain(double window, int step, double increment, double max) {
        public Chain {
            Check.positive("window", window);
            Check.positive("step", step);
            Check.positive("increment", increment);
            Check.positive("max", max);
        }
    }

    /**
     * The grade rating's weights (they add up to 1) and {@code fullChain}, the longest chain that
     * earns the {@code maxChain} part in full.
     */
    public record Rating(double killRatio, double armourDamage, double secrets, double maxChain, int fullChain) {
        public Rating {
            Check.that(
                    Math.abs(killRatio + armourDamage + secrets + maxChain - 1) < 1e-9,
                    "the rating weights add up to 1");
            Check.positive("full_chain", fullChain);
        }
    }

    /** {@code points} × (the kill % when {@code perKillPercent}) × the level number or the act. */
    public record Bonus(String name, double points, Optional<Boolean> perKillPercent, Scale scale) {}

    public enum Scale {
        LEVEL,
        ACT
    }

    /** The lowest rating for the grade (none for the last) and its credit bonus share. */
    public record Grade(String grade, Optional<Integer> rating, double creditBonus) {
        public Grade {
            Check.notNegative("credit_bonus", creditBonus);
        }
    }
}
