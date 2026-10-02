package vanguard.sim;

/**
 * An enemy's attack (design/enemies, attack vocabulary), with the difficulty levers already
 * applied: an {@code aimed} shot, a {@code burst} of them, or an n-way {@code fan} centred on the
 * player. A hovering unit fires a while after it stops and then at the interval; a diver fires
 * once in its dive; a turret fires along its barrel, which turns towards the player at its turn
 * rate while the player is inside its arc.
 *
 * @param intervalSeconds time between two volleys; infinite for a one-shot attack
 * @param firstShotDelay time from stopping (a turret: entering the screen) to the first volley
 * @param burst shots per volley, {@link #BURST_GAP_SECONDS} apart
 * @param bulletSpeed px/s
 * @param damage damage of a hit, by the bullet class (design/enemies, balancing basis)
 * @param leadsTargetInCircle whether selected units in a {@code circle} formation aim where the
 *     player is going rather than where it is (a hard-mode hook)
 * @param fan bullets of a fan; 1 for aimed shots
 * @param spreadRadians a fan's angle from its first to its last bullet
 * @param turnRate a turret's barrel turn rate in radians per second; infinite for units that aim at once
 * @param arcRadians a turret fires while the player is within this angle of its facing (straight
 *     down); infinite for units that fire every way
 */
public record EnemyGun(
        double intervalSeconds,
        double firstShotDelay,
        int burst,
        double bulletSpeed,
        double damage,
        boolean leadsTargetInCircle,
        int fan,
        double spreadRadians,
        double turnRate,
        double arcRadians) {
    /**
     * The gap between the shots of a burst. "Quick succession" in the attack vocabulary has no
     * number yet; this is a first value to tune.
     */
    public static final double BURST_GAP_SECONDS = 0.15;

    /** The hit box of a {@code small} enemy bullet (the 9 px orb or the 5x13 needle; no number in the design yet). */
    public static final Hitbox BULLET = new Hitbox(6, 6);

    /** An aimed attack that aims at once in every direction. */
    public static EnemyGun aimed(
            double intervalSeconds,
            double firstShotDelay,
            int burst,
            double bulletSpeed,
            double damage,
            boolean leadsTargetInCircle) {
        return new EnemyGun(
                intervalSeconds,
                firstShotDelay,
                burst,
                bulletSpeed,
                damage,
                leadsTargetInCircle,
                1,
                0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
    }
}
