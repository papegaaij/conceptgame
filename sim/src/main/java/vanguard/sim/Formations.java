package vanguard.sim;

import java.util.List;
import java.util.Optional;
import vanguard.sim.WaveSpec.Edge;
import vanguard.sim.WaveSpec.Entry;

/**
 * Places the units of a wave by formation and entry edge (design/enemies, formation vocabulary)
 * and plans how each one flies. Runs once when a level is loaded; the paths it builds are shared
 * by every attempt, so stepping the level never allocates.
 *
 * <p>Positions are play-field pixels, y up from the bottom edge. The layout numbers here (where
 * a V hovers, the circle's centre, how far apart units fly) are not in the design yet: they are
 * first values, to be tuned after playing.
 */
final class Formations {
    private static final double WIDTH = PlayField.WIDTH;
    private static final double HEIGHT = PlayField.HEIGHT;
    /** Units enter this far outside the edge, so they fly in rather than appear. */
    private static final double OUTSIDE = 40;

    /** Enters top left, curls towards the centre and leaves through the bottom. */
    private static final FlightPath CURL =
            FlightPath.through(100, 580, 100, 420, 170, 300, 300, 260, 360, 180, 330, 60, 300, -40);
    /** Dives from the top right into the lower third, hooks back and leaves through the left edge. */
    private static final FlightPath HOOK =
            FlightPath.through(330, 580, 330, 350, 280, 200, 180, 160, 90, 220, 40, 340, -40, 420);
    /** Enters from the left edge and sweeps down across the player's lane to the right edge. */
    private static final FlightPath SWEEP = FlightPath.through(-40, 470, 100, 420, 240, 260, 380, 200, 520, 230);
    /** A front stream unit from the left: down and across to the right. */
    private static final FlightPath FRONT_STREAM = FlightPath.through(90, HEIGHT + OUTSIDE, 330, -OUTSIDE);
    /** A side stream unit from the left: across and down to the right edge. */
    private static final FlightPath SIDE_STREAM = FlightPath.through(-OUTSIDE, 430, WIDTH + OUTSIDE, 170);

    /** Horizontal gap between the ranks of a V. */
    private static final double V_SPACING = 50;
    /** The largest height step between the ranks of a V. */
    private static final double V_RANK_STEP = 35;
    /** How far from its edge a pincer unit holds, and the height of the first pair. */
    private static final double PINCER_X = 60;

    private static final double PINCER_Y = 360;
    private static final double PINCER_RANK_STEP = 70;
    /** The circle orbits a point above the play field's centre. */
    private static final double CIRCLE_Y = 360;
    /** The gap between two break groups of a circle ("one by one"). */
    static final double BREAK_INTERVAL_SECONDS = 0.5;

    private Formations() {}

    /** Adds the units of {@code wave} to {@code out}; {@code kind} is the wave's enemy kind. */
    static void plan(WaveSpec wave, int kind, SplitMix64 rng, List<Spawn> out) {
        Planner planner = new Planner(wave, kind, rng, out);
        switch (wave.formation()) {
            case SNAKE -> planner.snake();
            case V_WING -> planner.vWing();
            case LINE_ABREAST -> planner.lineAbreast();
            case STREAM -> planner.stream();
            case PINCER -> planner.pincer();
            case CIRCLE -> planner.circle();
        }
    }

    /** The planning of one wave. */
    private static final class Planner {
        private final WaveSpec wave;
        private final int kind;
        private final SplitMix64 rng;
        private final List<Spawn> out;

        Planner(WaveSpec wave, int kind, SplitMix64 rng, List<Spawn> out) {
            this.wave = wave;
            this.kind = kind;
            this.rng = rng;
            this.out = out;
        }

        private EnemySpec enemy() {
            return wave.enemy();
        }

        private double speed(double own) {
            return wave.speed().orElse(own);
        }

        void snake() {
            FlightPath path =
                    switch (wave.entry()) {
                        case FRONT ->
                            switch (wave.edge()) {
                                case LEFT -> CURL;
                                case RIGHT -> CURL.mirrored();
                                default -> HOOK;
                            };
                        case SIDES ->
                            switch (wave.edge()) {
                                case LEFT -> SWEEP;
                                case RIGHT -> SWEEP.mirrored();
                                default -> throw unsupported("a snake enters from one side");
                            };
                        case REAR -> throw unsupported("snakes do not enter from the rear");
                    };
            double spacing = required(enemy().snake(), "snake").spacingSeconds();
            for (int i = 0; i < wave.count(); i++) {
                add(i, wave.t() + i * spacing, path, speed(enemy().speed()), 0, Optional.empty(), Spawn.Exit.DOWN);
            }
        }

        void vWing() {
            requireFront();
            EnemySpec.Hover hover = required(enemy().hover(), "hover");
            double centre =
                    switch (wave.edge()) {
                        case LEFT -> WIDTH * 0.3;
                        case RIGHT -> WIDTH * 0.7;
                        default -> WIDTH / 2;
                    };
            int ranks = wave.count() / 2;
            double rankStep =
                    Math.min(V_RANK_STEP, (hover.depth().max() - hover.depth().min()) / Math.max(1, ranks));
            for (int i = 0; i < wave.count(); i++) {
                int rank = (i + 1) / 2;
                int side = i == 0 ? 0 : (i % 2 == 1 ? -1 : 1);
                double x = centre + side * rank * V_SPACING;
                double y = HEIGHT - (hover.depth().max() - rank * rankStep);
                FlightPath path = FlightPath.through(x + side * 40, HEIGHT + OUTSIDE, x + side * 20, y + 90, x, y);
                hoverAt(i, path, hover, Spawn.Exit.awayFromCentre(x));
            }
        }

        void lineAbreast() {
            int count = wave.count();
            if (enemy().hover().isPresent() && wave.entry() == Entry.FRONT) {
                EnemySpec.Hover hover = enemy().hover().orElseThrow();
                double y = HEIGHT - hover.depth().at(0.5);
                for (int i = 0; i < count; i++) {
                    double x = (i + 1) * WIDTH / (count + 1);
                    hoverAt(i, FlightPath.through(x, HEIGHT + OUTSIDE, x, y), hover, Spawn.Exit.awayFromCentre(x));
                }
                return;
            }
            boolean rear =
                    switch (wave.entry()) {
                        case FRONT -> false;
                        case REAR -> true;
                        case SIDES -> throw unsupported("a line abreast enters from the front or the rear");
                    };
            for (int i = 0; i < count; i++) {
                double x = (i + 0.5) * WIDTH / count;
                FlightPath path = rear
                        ? FlightPath.through(x, -OUTSIDE, x, HEIGHT + OUTSIDE)
                        : FlightPath.through(x, HEIGHT + OUTSIDE, x, -OUTSIDE);
                add(i, wave.t(), path, speed(enemy().speed()), 0, Optional.empty(), Spawn.Exit.DOWN);
            }
        }

        void stream() {
            FlightPath fromLeft =
                    switch (wave.entry()) {
                        case FRONT -> FRONT_STREAM;
                        case SIDES -> SIDE_STREAM;
                        case REAR -> throw unsupported("streams do not enter from the rear");
                    };
            FlightPath fromRight = fromLeft.mirrored();
            double interval = required(wave.intervalSeconds(), "interval");
            double speed = speed(enemy().streamSpeed().orElse(enemy().speed()));
            for (int i = 0; i < wave.count(); i++) {
                boolean left =
                        switch (wave.edge()) {
                            case LEFT -> true;
                            case RIGHT -> false;
                            case NONE, ALTERNATING -> i % 2 == 0;
                        };
                add(
                        i,
                        wave.t() + i * interval,
                        left ? fromLeft : fromRight,
                        speed,
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN);
            }
        }

        void pincer() {
            if (wave.entry() != Entry.SIDES || wave.edge() != Edge.NONE) {
                throw unsupported("a pincer enters from both sides");
            }
            double hold = required(wave.holdSeconds(), "hold");
            for (int i = 0; i < wave.count(); i++) {
                int rank = i / 2;
                double y = PINCER_Y - rank * PINCER_RANK_STEP;
                FlightPath fromLeft = FlightPath.through(-OUTSIDE, y + 30, PINCER_X, y);
                FlightPath path = i % 2 == 0 ? fromLeft : fromLeft.mirrored();
                add(i, wave.t(), path, speed(enemy().speed()), hold, Optional.empty(), Spawn.Exit.DOWN);
            }
        }

        void circle() {
            requireFront();
            EnemySpec.Orbit orbit = required(enemy().orbit(), "orbit");
            double hold = required(wave.holdSeconds(), "hold");
            double radius = orbit.radius();
            double rate = StrictMath.toRadians(orbit.degreesPerSecond());
            double centreX = WIDTH / 2;
            // Every unit flies the same distance straight down to its place, so the circle arrives whole.
            double drop = HEIGHT + OUTSIDE - (CIRCLE_Y - radius);
            for (int i = 0; i < wave.count(); i++) {
                double angle = StrictMath.PI / 2 + 2 * StrictMath.PI * i / wave.count();
                double x = centreX + radius * StrictMath.cos(angle);
                double y = CIRCLE_Y + radius * StrictMath.sin(angle);
                double breakDelay = (double) (i / wave.breakGroup()) * BREAK_INTERVAL_SECONDS;
                var place = Optional.of(new Spawn.Orbit(centreX, CIRCLE_Y, radius, angle, rate));
                add(
                        i,
                        wave.t(),
                        FlightPath.through(x, y + drop, x, y),
                        speed(enemy().speed()),
                        hold + breakDelay,
                        place,
                        Spawn.Exit.TOWARD_SHIP);
            }
        }

        private void hoverAt(int unit, FlightPath path, EnemySpec.Hover hover, Spawn.Exit exit) {
            add(unit, wave.t(), path, speed(enemy().speed()), hover.seconds().pick(rng), Optional.empty(), exit);
        }

        private void add(
                int unit,
                double t,
                FlightPath path,
                double speed,
                double hold,
                Optional<Spawn.Orbit> orbit,
                Spawn.Exit exit) {
            boolean leads = wave.formation() == WaveSpec.Formation.CIRCLE
                    && unit % 2 == 1
                    && enemy().gun().map(EnemyGun::leadsTargetInCircle).orElse(false);
            out.add(new Spawn(SimStep.ticks(t), kind, enemy(), path, speed, hold, orbit, exit, leads, carried(unit)));
        }

        private Optional<PickupType> carried(int unit) {
            for (WaveSpec.Carried carried : wave.carried()) {
                if (unit == (carried.lastUnit() ? wave.count() - 1 : 0)) {
                    return Optional.of(carried.pickup());
                }
            }
            return Optional.empty();
        }

        private void requireFront() {
            if (wave.entry() != Entry.FRONT) {
                throw unsupported("a " + wave.formation() + " enters from the front");
            }
        }

        private <T> T required(Optional<T> value, String what) {
            return value.orElseThrow(() -> unsupported("needs " + what));
        }

        private IllegalArgumentException unsupported(String message) {
            return new IllegalArgumentException(
                    "wave at t=" + wave.t() + " (" + wave.formation() + " of " + enemy().slug() + "): " + message);
        }
    }
}
