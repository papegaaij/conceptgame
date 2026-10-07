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
    /** Divers of a V go one after another this far apart (design/enemies/air/stinger), and the units of a column enter so. */
    static final double DIVE_STAGGER_SECONDS = 0.4;
    /** A convoy's units follow one another this far apart, and come down this far from their side edge. */
    static final double CONVOY_INTERVAL_SECONDS = 1.5;

    /** M5 part C: a pack's walkers enter this far apart, each on its own path (design/enemies, formation vocabulary). */
    static final double PACK_INTERVAL_SECONDS = 0.25;

    private static final double CONVOY_EDGE_GAP = 70;
    private static final double CONVOY_TURN = 60;
    /** A whirl cluster releases its units within this time. */
    static final double CLUSTER_RELEASE_SECONDS = 0.3;
    /** The gap between the stops of a column entering from a side edge, inwards from the edge. */
    private static final double COLUMN_STOP_SPACING = 70;

    private Formations() {}

    /**
     * A boss stream's unit (design/enemies/bosses: Skitter streams from the side edges), entering
     * from the left or the right edge on a side stream's path; its tick is not read.
     */
    static Spawn streamUnit(EnemySpec enemy, int kind, boolean left) {
        return new Spawn(
                0,
                kind,
                enemy,
                left ? SIDE_STREAM : SIDE_STREAM.mirrored(),
                enemy.streamSpeed().orElse(enemy.speed()),
                0,
                Optional.empty(),
                Spawn.Exit.DOWN,
                false,
                Optional.empty());
    }

    /** Adds the units of {@code wave} to {@code out}; {@code kind} is the wave's enemy kind. */
    static void plan(WaveSpec wave, int kind, SplitMix64 rng, List<Spawn> out) {
        Planner planner = new Planner(wave, kind, rng, out);
        if (wave.enemy().walker().isPresent()) {
            planner.walkers();
            return;
        }
        if (wave.enemy().chain().isPresent()) {
            planner.chains();
            return;
        }
        if (wave.enemy().sideHover().isPresent() && wave.entry() == Entry.SIDES) {
            planner.sideHovers();
            return;
        }
        switch (wave.formation()) {
            case SINGLE -> planner.single();
            case CONVOY -> planner.convoy();
            case WHIRL_CLUSTER -> planner.whirlCluster();
            case COLUMN -> planner.column();
            case SNAKE -> planner.snake();
            case V_WING -> planner.vWing();
            case LINE_ABREAST -> planner.lineAbreast();
            case STREAM -> planner.stream();
            case PINCER -> planner.pincer();
            case CIRCLE -> planner.circle();
            case CARRIER_ESCORTS -> planner.carrierEscorts();
            case PACK ->
                throw new IllegalArgumentException("a pack is a formation of walkers, not of "
                        + wave.enemy().slug());
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

        void single() {
            requireFront();
            double x = frontColumn();
            if (!stops()) {
                add(
                        0,
                        wave.t(),
                        FlightPath.through(x, HEIGHT + OUTSIDE, x, -OUTSIDE),
                        speed(enemy().speed()),
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN);
                return;
            }
            stopAt(
                    0,
                    wave.t(),
                    FlightPath.through(
                            x, HEIGHT + OUTSIDE, x, HEIGHT - stopDepth().at(0.5)),
                    x);
        }

        /**
         * A column from the top near one side edge that comes down to its strafe height and turns
         * across the screen to leave through the other side, {@link #CONVOY_INTERVAL_SECONDS} apart.
         */
        void convoy() {
            requireFront();
            Range strafe = required(enemy().strafe(), "strafe");
            boolean fromLeft = wave.edge() != Edge.RIGHT;
            double x = fromLeft ? CONVOY_EDGE_GAP : WIDTH - CONVOY_EDGE_GAP;
            double y = HEIGHT - strafe.at(0.5);
            double out = fromLeft ? WIDTH + OUTSIDE * 2 : -OUTSIDE * 2;
            double turn = fromLeft ? CONVOY_TURN : -CONVOY_TURN;
            // The turn starts and ends this far from the corner, so the curve hardly dips below the strafe height.
            FlightPath path = FlightPath.through(x, HEIGHT + OUTSIDE * 2, x, y + CONVOY_TURN, x + turn, y, out, y);
            for (int i = 0; i < wave.count(); i++) {
                add(
                        i,
                        wave.t() + i * CONVOY_INTERVAL_SECONDS,
                        path,
                        speed(enemy().speed()),
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN);
            }
        }

        /**
         * The units of a whirl cluster released from its point within {@link #CLUSTER_RELEASE_SECONDS}
         * at 360/n degrees apart, so the spiral reads as one shape (design/enemies/air/whirl-seed).
         */
        void whirlCluster() {
            WaveSpec.At at = required(wave.at(), "a release point (at)");
            double x = at.x();
            double y = HEIGHT - at.depth();
            for (int i = 0; i < wave.count(); i++) {
                double angle = 2 * StrictMath.PI * i / wave.count();
                double t = wave.t() + CLUSTER_RELEASE_SECONDS * i / wave.count();
                out.add(new Spawn(
                        SimStep.ticks(t),
                        kind,
                        enemy(),
                        FlightPath.through(x, y, x, y - 1),
                        speed(enemy().speed()),
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN,
                        false,
                        carried(i),
                        Optional.of(new Spawn.Release(x, y, angle))));
            }
        }

        /**
         * One behind the other on the same path, {@link #DIVE_STAGGER_SECONDS} apart: from the front
         * down to where the units stop (or through the screen for units that do not), or from a side
         * edge along the row they stop at, each stopping further in than the one after it.
         */
        void column() {
            double gap = DIVE_STAGGER_SECONDS;
            if (wave.entry() == Entry.SIDES) {
                boolean left =
                        switch (wave.edge()) {
                            case LEFT -> true;
                            case RIGHT -> false;
                            default -> throw unsupported("a column enters from one side");
                        };
                double y = HEIGHT - stopDepth().at(0.5);
                for (int i = 0; i < wave.count(); i++) {
                    double in = COLUMN_STOP_SPACING * (wave.count() - i);
                    double x = left ? in : WIDTH - in;
                    double edge = left ? -OUTSIDE : WIDTH + OUTSIDE;
                    stopAt(i, wave.t() + i * gap, FlightPath.through(edge, y, x, y), x);
                }
                return;
            }
            requireFront();
            double x = frontColumn();
            for (int i = 0; i < wave.count(); i++) {
                if (stops()) {
                    stopAt(
                            i,
                            wave.t() + i * gap,
                            FlightPath.through(
                                    x, HEIGHT + OUTSIDE, x, HEIGHT - stopDepth().at(0.5)),
                            x);
                } else {
                    add(
                            i,
                            wave.t() + i * gap,
                            FlightPath.through(x, HEIGHT + OUTSIDE, x, -OUTSIDE),
                            speed(enemy().speed()),
                            0,
                            Optional.empty(),
                            Spawn.Exit.DOWN);
                }
            }
        }

        private double frontColumn() {
            return switch (wave.edge()) {
                case LEFT -> WIDTH * 0.3;
                case RIGHT -> WIDTH * 0.7;
                default -> WIDTH / 2;
            };
        }

        void vWing() {
            requireFront();
            Range depth = stopDepth();
            double centre = frontColumn();
            int ranks = wave.count() / 2;
            double rankStep = Math.min(V_RANK_STEP, (depth.max() - depth.min()) / Math.max(1, ranks));
            for (int i = 0; i < wave.count(); i++) {
                int rank = (i + 1) / 2;
                int side = i == 0 ? 0 : (i % 2 == 1 ? -1 : 1);
                double x = centre + side * rank * V_SPACING;
                double y = HEIGHT - (depth.max() - rank * rankStep);
                FlightPath path = FlightPath.through(x + side * 40, HEIGHT + OUTSIDE, x + side * 20, y + 90, x, y);
                stopAt(i, wave.t(), path, x);
            }
        }

        /** Whether its units stop on the screen: they hover, or pause before a dive. */
        private boolean stops() {
            return enemy().hover().isPresent() || enemy().dive().isPresent();
        }

        /** How far below the top edge its units stop: where they hover, or pause before a dive. */
        private Range stopDepth() {
            if (enemy().dive().isPresent()) {
                return enemy().dive().get().depth();
            }
            return required(enemy().hover(), "hover or dive").depth();
        }

        /**
         * A unit that flies {@code path} and stops at its end: a hover for its hover time, then away
         * from the centre; or a diver's pause (the units of a V one after another), then the dive.
         */
        private void stopAt(int unit, double t, FlightPath path, double x) {
            if (enemy().dive().isPresent()) {
                EnemySpec.Dive dive = enemy().dive().get();
                double stagger = wave.formation() == WaveSpec.Formation.V_WING ? unit * DIVE_STAGGER_SECONDS : 0;
                add(
                        unit,
                        t,
                        path,
                        speed(enemy().speed()),
                        dive.pauseSeconds() + stagger,
                        Optional.empty(),
                        Spawn.Exit.TOWARD_SHIP);
            } else {
                EnemySpec.Hover hover = required(enemy().hover(), "hover or dive");
                add(
                        unit,
                        t,
                        path,
                        speed(enemy().speed()),
                        hover.seconds().pick(rng),
                        Optional.empty(),
                        Spawn.Exit.awayFromCentre(x));
            }
        }

        void lineAbreast() {
            int count = wave.count();
            if (enemy().hover().isPresent() && wave.entry() == Entry.FRONT) {
                EnemySpec.Hover hover = enemy().hover().orElseThrow();
                double y = HEIGHT - hover.depth().at(0.5);
                for (int i = 0; i < count; i++) {
                    double x = (i + 1) * WIDTH / (count + 1);
                    stopAt(i, wave.t(), FlightPath.through(x, HEIGHT + OUTSIDE, x, y), x);
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

        /**
         * A {@code carrier + escorts} group (design/enemies/air/brood-pod): the carriers (spawners)
         * drift straight down, side by side; the escorts circle the carrier that entered last
         * (the wave lists the carrier's group first) at their orbit's radius and rate, evenly
         * spaced, and break off toward the ship {@link #BREAK_INTERVAL_SECONDS} apart when it ends.
         */
        void carrierEscorts() {
            requireFront();
            if (enemy().brood().isPresent()) {
                for (int i = 0; i < wave.count(); i++) {
                    double x = (i + 1) * WIDTH / (wave.count() + 1);
                    add(
                            i,
                            wave.t(),
                            FlightPath.through(x, HEIGHT + OUTSIDE, x, -OUTSIDE * 2),
                            speed(enemy().speed()),
                            0,
                            Optional.empty(),
                            Spawn.Exit.DOWN);
                }
                return;
            }
            EnemySpec.Orbit orbit = required(enemy().orbit(), "orbit");
            double rate = StrictMath.toRadians(orbit.degreesPerSecond());
            double x = WIDTH / 2;
            for (int i = 0; i < wave.count(); i++) {
                double angle = StrictMath.PI / 2 + 2 * StrictMath.PI * i / wave.count();
                out.add(new Spawn(
                        SimStep.ticks(wave.t()),
                        kind,
                        enemy(),
                        FlightPath.through(x, HEIGHT + OUTSIDE, x, HEIGHT),
                        speed(enemy().speed()),
                        0,
                        Optional.empty(),
                        Spawn.Exit.TOWARD_SHIP,
                        false,
                        carried(i),
                        Optional.empty(),
                        Optional.of(new Spawn.Escort(orbit.radius(), angle, rate, i * BREAK_INTERVAL_SECONDS)),
                        Optional.empty()));
            }
        }

        /**
         * Walkers (design/enemies/ground/scuttler) on the wave's ground paths: unit i walks path
         * i (wrapping); a pincer with a single path mirrors it about the centre line for every
         * second unit and sends each further pair {@link #CONVOY_INTERVAL_SECONDS} later; a pack (M5
         * part C) needs a path per unit and sends them {@link #PACK_INTERVAL_SECONDS} apart; any other
         * formation (a convoy, a single) sends its units one after another {@link
         * #CONVOY_INTERVAL_SECONDS} apart. These planners are separate from the air formations of the
         * same names.
         */
        void walkers() {
            if (wave.paths().isEmpty()) {
                throw unsupported("a walker wave needs its paths");
            }
            if (wave.formation() == WaveSpec.Formation.PACK && wave.paths().size() < wave.count()) {
                throw unsupported(
                        "a pack needs a path per unit: " + wave.paths().size() + " for " + wave.count());
            }
            for (int i = 0; i < wave.count(); i++) {
                WalkPath path = walkPath(wave.paths().get(i % wave.paths().size()));
                double delay;
                if (wave.formation() == WaveSpec.Formation.PINCER) {
                    if (wave.paths().size() == 1 && i % 2 == 1) {
                        path = path.mirrored();
                    }
                    delay = (i / 2) * CONVOY_INTERVAL_SECONDS;
                } else if (wave.formation() == WaveSpec.Formation.PACK) {
                    delay = i * PACK_INTERVAL_SECONDS;
                } else {
                    delay = i * CONVOY_INTERVAL_SECONDS;
                }
                double x = path.x(0);
                double y = path.y(0);
                out.add(new Spawn(
                        SimStep.ticks(wave.t() + delay),
                        kind,
                        enemy(),
                        FlightPath.through(x, y, x, y - 1),
                        enemy().walker().orElseThrow().speed(),
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN,
                        false,
                        carried(i),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(path)));
            }
        }

        /**
         * Units that enter from a side edge and hover there (design/enemies/air/mantis): a
         * {@code single} from its edge (left when none is given), a {@code pincer} one per edge in
         * turn; each flies in to its edge distance at a height picked within its hover range,
         * hovers its hover time and leaves through its edge (or down and out, without exit back).
         */
        void sideHovers() {
            EnemySpec.SideHover side = enemy().sideHover().orElseThrow();
            EnemySpec.Hover hover = required(enemy().hover(), "hover");
            boolean pincer = wave.formation() == WaveSpec.Formation.PINCER;
            if (!pincer && wave.formation() != WaveSpec.Formation.SINGLE) {
                throw unsupported("a side hover enters as a single or a pincer");
            }
            for (int i = 0; i < wave.count(); i++) {
                boolean left = pincer ? i % 2 == 0 : wave.edge() != Edge.RIGHT;
                double y = HEIGHT - hover.depth().pick(rng);
                double x = left ? side.edgeX() : WIDTH - side.edgeX();
                double outside = left ? -OUTSIDE : WIDTH + OUTSIDE;
                Spawn.Exit exit =
                        side.exitBack() ? new Spawn.Exit(false, left ? -1 : 1, 0) : Spawn.Exit.awayFromCentre(x);
                add(
                        i,
                        wave.t(),
                        FlightPath.through(outside, y, x, y),
                        speed(enemy().speed()),
                        wave.holdSeconds().orElseGet(() -> hover.seconds().pick(rng)),
                        Optional.empty(),
                        exit);
            }
        }

        /**
         * Segment chains (design/enemies/air/coilwyrm): unit i's head flies the wave's path i
         * (wrapping; with a single path every second unit flies it mirrored, so a pair crosses),
         * in (x, depth below the top edge) points, from the front, the rear or a side as the path
         * starts; units after the first follow {@link #DIVE_STAGGER_SECONDS} apart. A loop-back
         * re-enters its gap after the path's end on its own path shifted to start at the head's x
         * (straight up without one), the bottom edge warned ahead.
         */
        void chains() {
            if (wave.paths().isEmpty()) {
                throw unsupported("a segment chain flies its wave's paths");
            }
            for (int i = 0; i < wave.count(); i++) {
                List<WaveSpec.At> points = wave.paths().get(i % wave.paths().size());
                boolean mirror = wave.paths().size() == 1 && i % 2 == 1;
                double[] xy = new double[Math.max(2, points.size()) * 2];
                for (int k = 0; k < points.size(); k++) {
                    double x = points.get(k).x();
                    xy[2 * k] = mirror ? WIDTH - x : x;
                    xy[2 * k + 1] = HEIGHT - points.get(k).depth();
                }
                if (points.size() == 1) {
                    // One point: straight down through it.
                    xy[2] = xy[0];
                    xy[3] = -OUTSIDE * 4;
                }
                FlightPath path = FlightPath.through(xy);
                Optional<Spawn.Loop> loop = wave.loopBack().map(back -> loop(back, xy[xy.length - 2], mirror));
                out.add(new Spawn(
                        SimStep.ticks(wave.t() + i * DIVE_STAGGER_SECONDS),
                        kind,
                        enemy(),
                        path,
                        speed(enemy().speed()),
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN,
                        false,
                        carried(i),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        loop));
            }
        }

        /** A loop-back's path, shifted so it starts at {@code headX} (clamped into the field). */
        private Spawn.Loop loop(WaveSpec.LoopBack back, double headX, boolean mirror) {
            double x0 = Math.clamp(headX, OUTSIDE, WIDTH - OUTSIDE);
            double[] xy;
            if (back.path().isEmpty()) {
                xy = new double[] {x0, -OUTSIDE * 2, x0, HEIGHT + OUTSIDE * 4};
            } else {
                List<WaveSpec.At> points = back.path();
                xy = new double[Math.max(2, points.size()) * 2];
                double first = mirror
                        ? WIDTH - points.getFirst().x()
                        : points.getFirst().x();
                for (int k = 0; k < points.size(); k++) {
                    double x =
                            mirror ? WIDTH - points.get(k).x() : points.get(k).x();
                    xy[2 * k] = x - first + x0;
                    xy[2 * k + 1] = HEIGHT - points.get(k).depth();
                }
                if (points.size() == 1) {
                    xy[2] = xy[0];
                    xy[3] = HEIGHT + OUTSIDE * 4;
                }
            }
            return new Spawn.Loop(
                    FlightPath.through(xy),
                    back.afterSeconds(),
                    Math.max(
                            WaveSchedule.EDGE_WARNING_SECONDS,
                            wave.warningSeconds().orElse(0.0)));
        }

        private static WalkPath walkPath(List<WaveSpec.At> points) {
            double[] xy = new double[points.size() * 2];
            for (int i = 0; i < points.size(); i++) {
                xy[2 * i] = points.get(i).x();
                xy[2 * i + 1] = HEIGHT - points.get(i).depth();
            }
            return WalkPath.through(xy);
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
                if (unit == (carried.lastUnit() ? wave.count() - 1 : Math.min(carried.unit(), wave.count() - 1))) {
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
