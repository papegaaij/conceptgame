package vanguard.sim;

/**
 * An enemy's {@code aimed} attack (design/enemies, attack vocabulary), with the difficulty levers
 * already applied: a single shot or a {@code burst} at the player, starting a while after the
 * enemy stops and repeating at the interval.
 *
 * @param intervalSeconds time between two volleys
 * @param firstShotDelay time from stopping to the first volley
 * @param burst shots per volley, {@link #BURST_GAP_SECONDS} apart
 * @param bulletSpeed px/s
 * @param damage damage of a hit, by the bullet class (design/enemies, balancing basis)
 * @param leadsTargetInCircle whether selected units in a {@code circle} formation aim where the
 *     player is going rather than where it is (a hard-mode hook)
 */
public record EnemyGun(
        double intervalSeconds,
        double firstShotDelay,
        int burst,
        double bulletSpeed,
        double damage,
        boolean leadsTargetInCircle) {
    /**
     * The gap between the shots of a burst. "Quick succession" in the attack vocabulary has no
     * number yet; this is a first value to tune.
     */
    public static final double BURST_GAP_SECONDS = 0.15;

    /** The hit box of a {@code small} enemy bullet (the 9 px orb or the 5x13 needle; no number in the design yet). */
    public static final Hitbox BULLET = new Hitbox(6, 6);
}
