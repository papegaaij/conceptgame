package vanguard.sim;

import java.util.List;

/**
 * Score, chain, credits and grades (design/systems/scoring and design/systems/economy) at one
 * difficulty, built by {@code vanguard.content.SimSpecs}.
 *
 * @param killScore score per kill = bounty × killScore × chain multiplier
 * @param pickupScore pickups and bonuses score pickupScore × their credit value
 * @param chain the chain multiplier's rule
 * @param scoreFactor the difficulty's score multiplier, on every score
 * @param creditFactor the difficulty's credit income times the act factor, on every credit payout
 * @param weights the grade rating's weights
 * @param bonuses the level-end score bonuses
 * @param grades from the best grade down; the last has no minimum
 */
public record ScoringRules(
        double killScore,
        double pickupScore,
        Chain chain,
        double scoreFactor,
        double creditFactor,
        Weights weights,
        List<Bonus> bonuses,
        List<Grade> grades) {
    public ScoringRules {
        bonuses = List.copyOf(bonuses);
        grades = List.copyOf(grades);
    }

    /**
     * Kills within {@code windowSeconds} of each other keep the chain; every {@code step} kills add
     * {@code increment} to the multiplier, up to {@code max}.
     */
    public record Chain(double windowSeconds, int step, double increment, double max) {
        /** The multiplier at a chain of {@code count} kills. */
        public double multiplier(int count) {
            return Math.min(max, 1 + increment * (count / step));
        }

        /** The shortest chain that reaches the highest multiplier. */
        public int countAtMax() {
            return (int) Math.round((max - 1) / increment) * step;
        }
    }

    /** The grade rating's weights; they add up to 1. */
    public record Weights(double killRatio, double armourDamage, double secrets, double maxChain) {}

    /**
     * A level-end bonus: {@code points} × (the kill % when {@code perKillPercent}) × the level
     * number or the act.
     */
    public record Bonus(BonusKind kind, String name, double points, boolean perKillPercent, boolean perLevel) {}

    /** When a bonus is paid (design/systems/scoring, Level-end bonuses). */
    public enum BonusKind {
        /** Always, scaled by the kill ratio. */
        DESTRUCTION,
        /** No armour damage in the level. */
        UNTOUCHED,
        /** Every secret found, in a level that has secrets. */
        EXPLORER,
        /** A boss killed under par time; no level has a boss yet. */
        BOSS_RUSH
    }

    /** A grade from {@code minRating} (0–100) up, paying {@code creditBonus} of the level's credits. */
    public record Grade(String letter, double minRating, double creditBonus) {}
}
