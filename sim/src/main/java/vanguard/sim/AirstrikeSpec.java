package vanguard.sim;

/**
 * The Airstrike's numbers (design/player/specials, Airstrike (L04)): two bombers enter at the
 * bottom edge {@code offset} px left and right of the ship's x at the call, {@code delaySeconds}
 * after it, and fly straight up at {@code speed}; each drops a bomb every {@code bombSpacing} px,
 * which bursts {@code fallSeconds} later where the ground has carried its release point. A blast
 * deals its damage per layer, and one strike at most its cap to one target.
 *
 * @param bomber the bomber's sprite box, facing up
 * @param groundDamage per blast to {@code ground} and {@code low-air} targets (hardened included)
 * @param airDamage per blast to {@code air} targets
 * @param bossPartCap the most one strike deals to a boss part (applied once bosses exist)
 */
public record AirstrikeSpec(
        double delaySeconds,
        double offset,
        double speed,
        Hitbox bomber,
        double bombSpacing,
        double fallSeconds,
        double blastRadius,
        double groundDamage,
        double groundCap,
        double airDamage,
        double airCap,
        double bossPartCap) {
    public AirstrikeSpec {
        if (!(speed > 0) || !(bombSpacing > 0) || !(fallSeconds > 0)) {
            throw new IllegalArgumentException("an Airstrike needs a speed, a bomb spacing and a fall time");
        }
    }
}
