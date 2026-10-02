package vanguard.sim;

/**
 * The rules of a sortie that do not depend on the level, at one difficulty.
 *
 * @param bulletBudget the most enemy bullets on screen (design/systems/difficulty); a volley beyond
 *     it loses the shots that do not fit
 */
public record Rules(int bulletBudget, PickupRules pickups, ScoringRules scoring) {}
