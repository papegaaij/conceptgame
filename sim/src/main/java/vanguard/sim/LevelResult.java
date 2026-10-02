package vanguard.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The totals of a finished level for the debrief (design/ui/debrief): the tally, the credits by
 * source, the level-end bonuses, the grade and its credit bonus (design/systems/scoring).
 *
 * @param armourDamage armour points lost in the level
 * @param maxMultiplier the chain multiplier at the longest chain
 * @param bonuses the level-end bonuses that were paid
 * @param score the level's score including the bonuses
 * @param rating the grade rating, 0–100
 * @param gradeBonus the grade's credit bonus on the credits earned
 */
public record LevelResult(
        int kills,
        int enemies,
        double armourDamage,
        int secretsFound,
        int secrets,
        int maxChain,
        double maxMultiplier,
        boolean secondaryMet,
        Credits credits,
        List<BonusScore> bonuses,
        long score,
        double rating,
        ScoringRules.Grade grade,
        int gradeBonus) {
    public LevelResult {
        bonuses = List.copyOf(bonuses);
    }

    /** The credits earned in the level by source, before the grade bonus. */
    public record Credits(int kills, int groundTargets, int salvage, int secrets, int objectives) {
        public int total() {
            return kills + groundTargets + salvage + secrets + objectives;
        }
    }

    /** A level-end bonus and the score it paid. */
    public record BonusScore(String name, long score) {}

    /** The kill ratio in whole percent, rounded half to even. */
    public int killPercent() {
        return enemies == 0 ? 100 : (int) Math.rint(100.0 * kills / enemies);
    }

    /**
     * Rates and grades a level. The design gives the rating's weights but not how each part maps
     * to 0–1; here: the kill ratio, 1 − armour lost ÷ the plating's maximum, secrets found ÷
     * secrets (1 without secrets) and the longest chain ÷ the chain that reaches the top multiplier.
     */
    static LevelResult of(
            ScoringRules rules,
            LevelScript script,
            Tally tally,
            int enemies,
            double armourDamage,
            double maxArmour,
            int secretsFound,
            boolean secondaryMet) {
        Credits credits = new Credits(
                tally.credits(CreditSource.KILLS),
                tally.credits(CreditSource.GROUND_TARGETS),
                tally.credits(CreditSource.SALVAGE),
                tally.credits(CreditSource.SECRETS),
                tally.credits(CreditSource.OBJECTIVES));
        int kills = tally.kills();
        double killRatio = enemies == 0 ? 1 : (double) kills / enemies;
        int killPercent = (int) Math.rint(100 * killRatio);
        int secrets = script.secrets();
        List<BonusScore> bonuses = new ArrayList<>();
        for (ScoringRules.Bonus bonus : rules.bonuses()) {
            boolean paid =
                    switch (bonus.kind()) {
                        case DESTRUCTION -> kills > 0;
                        case UNTOUCHED -> armourDamage == 0;
                        case EXPLORER -> secrets > 0 && secretsFound == secrets;
                        case BOSS_RUSH -> false;
                    };
            if (paid) {
                double points = bonus.points() * (bonus.perLevel() ? script.number() : script.act());
                if (bonus.perKillPercent()) {
                    points *= killPercent;
                }
                bonuses.add(new BonusScore(bonus.name(), Math.round(points * rules.scoreFactor())));
            }
        }
        long score =
                tally.score() + bonuses.stream().mapToLong(BonusScore::score).sum();
        ScoringRules.Weights weights = rules.weights();
        double rating = 100
                * (weights.killRatio() * killRatio
                        + weights.armourDamage() * Math.max(0, 1 - armourDamage / maxArmour)
                        + weights.secrets() * (secrets == 0 ? 1 : (double) secretsFound / secrets)
                        + weights.maxChain()
                                * Math.min(
                                        1,
                                        (double) tally.maxChain()
                                                / rules.chain().countAtMax()));
        ScoringRules.Grade grade = rules.grades().stream()
                .filter(g -> rating >= g.minRating())
                .findFirst()
                .orElse(rules.grades().getLast());
        int gradeBonus = (int) Math.rint(grade.creditBonus() * credits.total());
        return new LevelResult(
                kills,
                enemies,
                armourDamage,
                secretsFound,
                secrets,
                tally.maxChain(),
                rules.chain().multiplier(tally.maxChain()),
                secondaryMet,
                credits,
                bonuses,
                score,
                rating,
                grade,
                gradeBonus);
    }
}
