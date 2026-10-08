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
 * @param loop a segment chain's loop-back: the head re-enters on this second path after a gap
 * @param ambush M5 part D: a {@code rear ambush} unit's way back up after its swoop down {@code path}
 * @param swarm M5 part D: a {@code swarm} member: its flock's route and its place in the flock
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
        Optional<WalkPath> walk,
        Optional<Loop> loop,
        Optional<Ambush> ambush,
        Optional<Swarm> swarm) {

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
            Optional<Release> release,
            Optional<Escort> escort,
            Optional<WalkPath> walk,
            Optional<Loop> loop) {
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
                escort,
                walk,
                loop,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * M5 part D, a {@code rear ambush} unit's way back (design/enemies/air/wraith): after its swoop
     * down its spawn's path (at the spawn's speed, off the bottom edge) it waits {@code gapSeconds}
     * below the edge, flies {@code rise} up to its hold point at the same speed, decloaks there over
     * {@code flashSeconds}, holds {@code holdSeconds} after the flash and leaves along {@code exit}
     * at {@code exitSpeed} px/s; the bottom edge is warned {@code warningSeconds} ahead of its
     * re-entry.
     */
    record Ambush(
            FlightPath rise,
            double gapSeconds,
            double flashSeconds,
            double holdSeconds,
            FlightPath exit,
            double exitSpeed,
            double warningSeconds) {}

    /**
     * M5 part D, a swarm's route (design/enemies/air/mote-swarm): its leader point flies {@code path}
     * at {@code speed} px/s and on past its end; {@code gapSeconds} after the end of a path it
     * re-enters on the next of the {@code loops} at {@code diveSpeed} px/s (the bottom edge warned
     * {@code warningSeconds} ahead), until the loops are flown. Shared by the members of a wave.
     */
    record Route(
            FlightPath path,
            java.util.List<FlightPath> loops,
            double speed,
            double diveSpeed,
            double gapSeconds,
            double warningSeconds) {
        Route {
            loops = java.util.List.copyOf(loops);
        }

        /** The steps from the wave's start to loop-back {@code k} (from 0): its re-entry. */
        int reentryAfter(int k) {
            double seconds = path.length() / speed + gapSeconds;
            for (int j = 0; j < k; j++) {
                seconds += loops.get(j).length() / diveSpeed + gapSeconds;
            }
            return SimStep.ticks(seconds);
        }
    }

    /**
     * M5 part D, a swarm's member {@code member} (from 0, in entry order) of a flock flying {@code
     * route}, starting at ({@code x}, {@code y}) in the seeded cloud round the route's first point.
     */
    record Swarm(Route route, int member, double x, double y) {}

    /** M5 part D: the level step an ambush unit re-enters at the bottom edge; -1 for any other unit. */
    int ambushReentryTick() {
        if (ambush.isEmpty()) {
            return -1;
        }
        return tick + SimStep.ticks(path.length() / speed + ambush.get().gapSeconds());
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
            Optional<PickupType> carried,
            Optional<Release> release,
            Optional<Escort> escort,
            Optional<WalkPath> walk) {
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
                escort,
                walk,
                Optional.empty());
    }

    /**
     * A chain's loop-back (design/enemies/air/coilwyrm): {@code gapSeconds} after its head left the
     * first path's end it re-enters on {@code path} (from below the bottom edge at the head's x),
     * the bottom edge warned {@code warningSeconds} ahead.
     */
    record Loop(FlightPath path, double gapSeconds, double warningSeconds) {}

    /** The level step a loop-back re-enters at: the first path flown, then the gap; -1 without one. */
    int reentryTick() {
        if (loop.isEmpty()) {
            return -1;
        }
        return tick + SimStep.ticks(path.length() / speed + loop.get().gapSeconds());
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
