package vanguard.content;

import java.util.Optional;

/**
 * design/systems/difficulty/data.yaml: the global difficulty levers. Factors multiply the medium
 * value; changes are shares of the base value (−0.25 = −25 %).
 *
 * @param patternBullets the change in bullets per fan, ring and burst (at least one bullet)
 * @param aimedShots how aimed shots aim, in words
 * @param bulletBudget the most enemy bullets on screen
 * @param repairCost credits per armour point repaired in the hangar
 * @param retries retries per level; unlimited where absent
 * @param sensorBonus extra sensor levels for the hangar intel
 */
public record DifficultyData(
        PerDifficulty<Double> enemyHp,
        PerDifficulty<Double> enemyBulletSpeed,
        PerDifficulty<Double> enemyFireRate,
        PerDifficulty<Double> patternBullets,
        PerDifficulty<String> aimedShots,
        PerDifficulty<Double> formationSize,
        PerDifficulty<Integer> bulletBudget,
        PerDifficulty<Double> shieldRegen,
        PerDifficulty<Double> creditIncome,
        PerDifficulty<Double> score,
        PerDifficulty<Integer> repairCost,
        Retries retries,
        PerDifficulty<Boolean> bossCheckpoint,
        PerDifficulty<Integer> sensorBonus) {

    /** An enemy's HP on {@code difficulty}: the medium HP times the lever, rounded half to even, at least 1. */
    public double enemyHp(double mediumHp, Difficulty difficulty) {
        return Math.max(1, Math.rint(mediumHp * enemyHp.of(difficulty)));
    }

    /** A wave's unit count on {@code difficulty}, by the formation size lever: rounded half to even, at least 1. */
    public int formationSize(int mediumCount, Difficulty difficulty) {
        return (int) Math.max(1, Math.rint(mediumCount * (1 + formationSize.of(difficulty))));
    }

    /** Retries per level on each difficulty; absent = unlimited. */
    public record Retries(Optional<Integer> easy, Optional<Integer> medium, Optional<Integer> hard) {}
}
