package vanguard.content;

import java.util.List;

/**
 * design/player/weapons/data.yaml: the rules all weapons share.
 *
 * @param upgradeCostFactors the L2–L5 upgrade costs as factors of a weapon's upgrade base
 * @param drawRound the step the draw between L1 and L5 is rounded to, in MW
 * @param singleTarget the target that single-target DPS is counted against
 */
public record WeaponRulesData(List<Double> upgradeCostFactors, double drawRound, SingleTarget singleTarget) {
    public WeaponRulesData {
        Check.that(upgradeCostFactors.size() == 4, "upgrade_cost_factors: one factor for each of L2–L5");
        Check.positive("draw_round", drawRound);
    }

    /** A target {@code width} px wide, {@code distance} px straight ahead of the muzzle. */
    public record SingleTarget(double distance, double width) {}
}
