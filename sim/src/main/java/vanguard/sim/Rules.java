package vanguard.sim;

/**
 * The rules of a sortie that do not depend on the level, at one difficulty.
 *
 * @param bulletBudget the most enemy bullets on screen (design/systems/difficulty); a volley beyond
 *     it loses the shots that do not fit
 * @param aimedSpread aimed shots leave within ± this many radians of the aim, uniformly at random
 * @param invulnerableShip a debug rule for testing (the {@code --invulnerable} launch option):
 *     enemy bullets and rammers pass through the ship; never set in play
 */
public record Rules(
        int bulletBudget, double aimedSpread, PickupRules pickups, ScoringRules scoring, boolean invulnerableShip) {
    public Rules(int bulletBudget, double aimedSpread, PickupRules pickups, ScoringRules scoring) {
        this(bulletBudget, aimedSpread, pickups, scoring, false);
    }

    /** These rules with the debug rule that nothing hits the ship. */
    public Rules withInvulnerableShip() {
        return new Rules(bulletBudget, aimedSpread, pickups, scoring, true);
    }
}
