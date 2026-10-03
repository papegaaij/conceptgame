package vanguard.sim;

import java.util.Optional;

/**
 * One unit as {@link Formations} planned it when the level was loaded: when it enters, what it
 * is and how it flies. It flies its path; at the end it is gone, or, with a hold time, it hovers
 * (or orbits) while its gun fires, then leaves.
 *
 * @param tick the level step it enters at
 * @param kind its enemy's index in {@link Sortie#enemyKinds()}
 * @param speed px/s along the path and when leaving
 * @param holdSeconds how long it stays at the path's end; 0 = gone there
 * @param orbit the circle it flies while holding, instead of hovering
 * @param exit how it leaves after holding
 * @param leadsTarget whether its gun aims where the ship is going
 * @param carried the pickup it drops when destroyed
 * @param release where a whirl cluster's unit spirals out from, instead of flying its path
 * @param escort how an escort circles its carrier (the unit of a spawner planned just before it)
 * @param walk a walker's ground path, instead of its flight path
 */
record Spawn(
        int tick,
        int kind,
        EnemySpec enemy,
        FlightPath path,
        double speed,
        double holdSeconds,
        Optional<Orbit> orbit,
        Exit exit,
        boolean leadsTarget,
        Optional<PickupType> carried,
        Optional<Release> release,
        Optional<Escort> escort,
        Optional<WalkPath> walk) {

    Spawn(
            int tick,
            int kind,
            EnemySpec enemy,
            FlightPath path,
            double speed,
            double holdSeconds,
            Optional<Orbit> orbit,
            Exit exit,
            boolean leadsTarget,
            Optional<PickupType> carried,
            Optional<Release> release) {
        this(
                tick,
                kind,
                enemy,
                path,
                speed,
                holdSeconds,
                orbit,
                exit,
                leadsTarget,
                carried,
                release,
                Optional.empty(),
                Optional.empty());
    }

    Spawn(
            int tick,
            int kind,
            EnemySpec enemy,
            FlightPath path,
            double speed,
            double holdSeconds,
            Optional<Orbit> orbit,
            Exit exit,
            boolean leadsTarget,
            Optional<PickupType> carried) {
        this(tick, kind, enemy, path, speed, holdSeconds, orbit, exit, leadsTarget, carried, Optional.empty());
    }

    /**
     * An escort's circle around its moving carrier: {@code radius} px, starting at {@code angle}
     * (radians), {@code radiansPerSecond}; when the carrier ends it breaks off toward the ship
     * after {@code breakSeconds}.
     */
    record Escort(double radius, double angle, double radiansPerSecond, double breakSeconds) {}

    /** A whirl cluster's release point and this unit's starting angle on the spiral (radians). */
    record Release(double x, double y, double angle) {}

    /** Circling {@code (centreX, centreY)} at {@code radius}, starting at {@code angle} (radians). */
    record Orbit(double centreX, double centreY, double radius, double angle, double radiansPerSecond) {}

    /** Leaving along the unit vector {@code (dx, dy)}, or straight at the ship's position at that moment. */
    record Exit(boolean towardShip, double dx, double dy) {
        static final Exit DOWN = new Exit(false, 0, -1);
        static final Exit TOWARD_SHIP = new Exit(true, 0, 0);

        /** Down and out to the side of the play field the unit is on. */
        static Exit awayFromCentre(double x) {
            double side = Math.signum(x - PlayField.WIDTH / 2.0);
            return side == 0 ? DOWN : new Exit(false, side * 0.6, -0.8);
        }
    }
}
