package vanguard.sim;

/**
 * The rules of a sortie that do not depend on the level, at one difficulty.
 *
 * @param bulletBudget the most enemy bullets on screen (design/systems/difficulty); a volley beyond
 *     it loses the shots that do not fit
 * @param aimedSpread aimed shots leave within ± this many radians of the aim, uniformly at random
 */
public record Rules(int bulletBudget, double aimedSpread, PickupRules pickups, ScoringRules scoring) {}
