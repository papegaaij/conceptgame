package vanguard.sim;

import java.util.Optional;

/**
 * An enemy's attack (design/enemies, attack vocabulary), with the difficulty levers already
 * applied: an {@code aimed} shot, a {@code burst} of them, or an n-way {@code fan} centred on the
 * player. A hovering unit fires a while after it stops and then at the interval; a diver fires
 * once in its dive; a turret fires along its barrel, which turns towards the player at its turn
 * rate while the player is inside its arc.
 *
 * @param intervalSeconds time between two volleys; infinite for a one-shot attack
 * @param firstShotDelay time from stopping (a turret: entering the screen) to the first volley
 * @param burst shots per volley, {@code burstGapSeconds} apart
 * @param bulletSpeed px/s
 * @param damage damage of a hit, by the bullet class (design/enemies, balancing basis)
 * @param leadsTargetInCircle whether selected units in a {@code circle} formation aim where the
 *     player is going rather than where it is (a hard-mode hook)
 * @param fan bullets of a fan; 1 for aimed shots
 * @param spreadRadians a fan's angle from its first to its last bullet
 * @param turnRate a turret's barrel turn rate in radians per second; infinite for units that aim at once
 * @param arcRadians a turret fires while the player is within this angle of its facing (straight
 *     down); infinite for units that fire every way
 * @param mine a mine layer's spore: it drops one at the interval instead of firing
 * @param mortar a mortar's lob (design/enemies/ground/polyp-mortar): it lobs one at the interval at
 *     the player's position instead of firing; {@code damage} is the direct hit's, {@code bulletSpeed}
 *     the ring's
 * @param burstGapSeconds M5 part D: the gap between the shots of a burst (the stat block's {@code
 *     burst_gap}; {@link #BURST_GAP_SECONDS} when it has none)
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
        double arcRadians,
        Optional<MineSpec> mine,
        Optional<MortarSpec> mortar,
        double burstGapSeconds,
        boolean up) {
    public EnemyGun {
        if (!(burstGapSeconds > 0)) {
            throw new IllegalArgumentException("a burst's shots are apart in time: " + burstGapSeconds);
        }
        if (up && (fan != 1 || mine.isPresent() || mortar.isPresent() || Double.isFinite(turnRate))) {
            throw new IllegalArgumentException("only an aimed burst goes straight up");
        }
    }

    /** An attack that is not fired straight up (every unit but the Wraith). */
    public EnemyGun(
            double intervalSeconds,
            double firstShotDelay,
            int burst,
            double bulletSpeed,
            double damage,
            boolean leadsTargetInCircle,
            int fan,
            double spreadRadians,
            double turnRate,
            double arcRadians,
            Optional<MineSpec> mine,
            Optional<MortarSpec> mortar,
            double burstGapSeconds) {
        this(
                intervalSeconds,
                firstShotDelay,
                burst,
                bulletSpeed,
                damage,
                leadsTargetInCircle,
                fan,
                spreadRadians,
                turnRate,
                arcRadians,
                mine,
                mortar,
                burstGapSeconds,
                false);
    }

    /** An attack with the default burst gap, {@link #BURST_GAP_SECONDS} (every unit before M5 part D). */
    public EnemyGun(
            double intervalSeconds,
            double firstShotDelay,
            int burst,
            double bulletSpeed,
            double damage,
            boolean leadsTargetInCircle,
            int fan,
            double spreadRadians,
            double turnRate,
            double arcRadians,
            Optional<MineSpec> mine,
            Optional<MortarSpec> mortar) {
        this(
                intervalSeconds,
                firstShotDelay,
                burst,
                bulletSpeed,
                damage,
                leadsTargetInCircle,
                fan,
                spreadRadians,
                turnRate,
                arcRadians,
                mine,
                mortar,
                BURST_GAP_SECONDS);
    }

    /**
     * The gap between the shots of a burst. "Quick succession" in the attack vocabulary has no
     * number yet; this is a first value to tune.
     */
    public static final double BURST_GAP_SECONDS = 0.15;

    /** The hit box of a {@code small} enemy bullet (the 9 px orb or the 5x13 needle; no number in the design yet). */
    public static final Hitbox BULLET = new Hitbox(6, 6);

    /**
     * A spore mine: it drifts at {@code drift} px/s in a random direction, arms (rises to the player
     * plane, where it can be hit and burst on contact) after {@code armSeconds}, and after
     * {@code lifeSeconds} bursts into a ring of {@code ring} bullets at the attack's bullet speed,
     * or just fades when it never {@code bursts}; destroyed it pays {@code credits}.
     *
     * @param ringDamage the ring bullets' damage; a contact burst deals the attack's damage
     */
    public record MineSpec(
            double armSeconds,
            double lifeSeconds,
            double drift,
            double hp,
            int ring,
            double ringDamage,
            boolean bursts,
            int credits) {
        /** A spore's hit box. */
        public static final Hitbox BOX = new Hitbox(10, 10);
    }

    /**
     * A mortar's lob: a blob (not shootable) flies {@code flightSeconds} to where the player was at
     * launch, its lime marker showing there all the while; it lands and bursts into a ring of
     * {@code ring} bullets of {@code ringDamage}, and a ship within {@code impactRadius} px of the
     * point takes the attack's damage (the direct hit).
     */
    public record MortarSpec(double flightSeconds, double impactRadius, int ring, double ringDamage) {}

    /** An attack without a mortar. */
    public EnemyGun(
            double intervalSeconds,
            double firstShotDelay,
            int burst,
            double bulletSpeed,
            double damage,
            boolean leadsTargetInCircle,
            int fan,
            double spreadRadians,
            double turnRate,
            double arcRadians,
            Optional<MineSpec> mine) {
        this(
                intervalSeconds,
                firstShotDelay,
                burst,
                bulletSpeed,
                damage,
                leadsTargetInCircle,
                fan,
                spreadRadians,
                turnRate,
                arcRadians,
                mine,
                Optional.empty());
    }

    /** An attack without a mine (aimed, a burst, a fan or a turret's). */
    public EnemyGun(
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
        this(
                intervalSeconds,
                firstShotDelay,
                burst,
                bulletSpeed,
                damage,
                leadsTargetInCircle,
                fan,
                spreadRadians,
                turnRate,
                arcRadians,
                Optional.empty());
    }

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
