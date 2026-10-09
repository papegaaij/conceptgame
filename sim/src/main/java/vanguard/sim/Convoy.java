package vanguard.sim;

import java.util.List;

/**
 * The convoy of an {@code escort} primary objective (design/allies, civilian crawler; design/
 * campaign, Level 04): its units roll in from the bottom edge one after another to their stations
 * in a column and follow the level's {@link Road} there at the scroll speed, so they hold their
 * height on the screen and drift sideways as the road winds. A destroyed unit stops and lies on the
 * road as a wreck. The units are allocated up front, so stepping does not allocate.
 *
 * <p>M5 part D, an air escort (design/allies, evacuation shuttle; Level 10; {@link
 * LevelScript.Air}): the units stand on their pads until the liftoff, lift off to their stations,
 * fly them with the lane sway on the level clock, and climb out off the top edge; the scripted
 * loss's unit is lost at its time without counting as a loss the player could have prevented.
 *
 * <p>M5 part E, a naval convoy outside the objectives (design/allies, convoy cargo ship and escort
 * frigate; Level 11; {@link LevelScript.Naval}): its units hold screen-space stations from the
 * start; when the scroll halts in the boss's arena each glides (smoothstep, on the real steps) to its
 * lane or off the bottom edge; when the halt ends they hold clear (a unit with a hold point glides
 * there) until the sea has scrolled the convoy's hold, Level 11's Platform Tiamat passing them, and
 * glide back to their stations. Only the boss's slams hurt
 * a unit ({@link #slammable(int)}, {@link #lane(int)}); a sunk unit is a wreck at once. The frigate's
 * flak bursts are timed here on the real steps ({@link SimEvents.Type#ALLY_FLAK}), hitting nothing.
 */
final class Convoy {
    private final LevelScript.Escort escort;
    private final Road road;
    private final Ally[] allies;
    private final int[] enterTicks;
    private final double startY;
    private int lost;
    private int firstHit = -1;
    private int firstLost = -1;
    /** M5 part D: the air escort; null for a ground convoy. */
    private final LevelScript.Air air;
    /** M5 part D: the scripted loss's unit; -1 for none. */
    private final int scripted;
    /** M5 part D: the units the player can save (every unit but the scripted loss's). */
    private final int saveable;

    private final int liftTick;
    private final int liftTicks;
    private final int climbTick;
    private final int climbTicks;
    private final int glowTick;
    private final int lossTick;
    /** The ground scrolled by the liftoff (from the level's sections), so a pad is at its point then. */
    private final double liftScroll;
    /** Above the top edge: where a unit is home. */
    private final double homeY;
    /** M5 part D: the units lost that the player could have saved (all but the scripted loss). */
    private int lostSaveable;
    /** M5 part D: the unit lost last in this attempt (the {@code ally-lost} cue's); -1 before. */
    private int lastLost = -1;

    private boolean glowStarted;
    private boolean scriptedLost;

    /** M5 part E: the naval convoy; null for an escort's convoy. */
    private final LevelScript.Naval naval;
    /** M5 part E: per naval unit, where its glide started (y up). */
    private final double[] fromX;

    private final double[] fromY;
    /** M5 part E: per naval unit, the steps into its glide. */
    private final int[] glideTicks;
    /** M5 part E: per naval unit, steps to its next flak burst (a unit with flak on its station). */
    private final int[] flakWait;
    /** M5 part E: where the arena is for the naval convoy: {@link #BEFORE}, {@link #OUT} or {@link #BACK}. */
    private int arena;
    /** M5 part E: the flak bursts so far in this attempt, which place the next one. */
    private int flakBursts;
    /** M5 part E: the unit hit last in this attempt (the {@code ally-hit} cue's); -1 before. */
    private int lastHit = -1;
    /** M5 part E: px the sea has scrolled since the halt ended, while the units hold clear. */
    private double heldScroll;

    /** M5 part E: before the halt, the naval units at their stations. */
    static final int BEFORE = 0;
    /** M5 part E: the scroll halted: the naval units glide to their lanes or away. */
    static final int OUT = 1;
    /** M5 part E: the halt ended (the boss is down): the naval units glide back to their stations. */
    static final int BACK = 2;
    /**
     * M5 part E: the halt ended, the naval units hold clear (in their lanes, away or at their hold
     * points) until the sea has scrolled the convoy's {@link LevelScript.Naval#holdClear()}.
     */
    static final int HOLD = 3;
    /** M5 part E: a flak burst is this far above its frigate at the least, px. */
    private static final double FLAK_ABOVE = 80;
    /** M5 part E: ... and at most this much farther. */
    private static final double FLAK_RANGE = 160;
    /** M5 part E: ... and this far to either side of it at the most, px. */
    private static final double FLAK_SIDE = 100;

    Convoy(LevelScript.Escort escort, Road road) {
        this(escort, road, List.of());
    }

    /** M5 part E: a naval convoy (Level 11), every unit at its station. */
    Convoy(LevelScript.Naval naval) {
        this.naval = naval;
        escort = null;
        road = null;
        int units = naval.units().size();
        allies = new Ally[units];
        for (int k = 0; k < units; k++) {
            allies[k] = new Ally();
        }
        enterTicks = new int[units];
        startY = 0;
        air = null;
        scripted = -1;
        saveable = naval.damageable();
        liftTick = 0;
        liftTicks = 1;
        climbTick = Integer.MAX_VALUE;
        climbTicks = 1;
        lossTick = Integer.MAX_VALUE;
        glowTick = Integer.MAX_VALUE;
        liftScroll = 0;
        homeY = 0;
        fromX = new double[units];
        fromY = new double[units];
        glideTicks = new int[units];
        flakWait = new int[units];
        reset();
    }

    /**
     * @param sections the level's sections: an air escort's pads scroll with the ground at their
     *     speeds until the liftoff
     */
    Convoy(LevelScript.Escort escort, Road road, List<LevelScript.Section> sections) {
        this.escort = escort;
        naval = null;
        fromX = new double[0];
        fromY = new double[0];
        glideTicks = new int[0];
        flakWait = new int[0];
        this.road = road;
        int units = escort.stations().size();
        allies = new Ally[units];
        enterTicks = new int[units];
        for (int k = 0; k < units; k++) {
            allies[k] = new Ally();
            enterTicks[k] = SimStep.ticks(escort.enterSeconds() + k * escort.enterInterval());
        }
        startY = -escort.ally().size().height() / 2;
        air = escort.air().orElse(null);
        scripted = escort.scriptedUnit();
        saveable = escort.saveable();
        liftTick =
                air == null ? 0 : air.liftoff().map(l -> SimStep.ticks(l.t())).orElse(0);
        liftTicks = air == null
                ? 1
                : air.liftoff().map(l -> SimStep.ticks(l.seconds())).orElse(1);
        climbTick = air == null
                ? Integer.MAX_VALUE
                : air.climb().map(c -> SimStep.ticks(c.t())).orElse(Integer.MAX_VALUE);
        climbTicks = air == null
                ? 1
                : air.climb().map(c -> SimStep.ticks(c.seconds())).orElse(1);
        int loss = Integer.MAX_VALUE;
        int glow = Integer.MAX_VALUE;
        if (air != null && air.scriptedLoss().isPresent()) {
            LevelScript.ScriptedLoss scriptedLoss = air.scriptedLoss().get();
            loss = SimStep.ticks(scriptedLoss.t());
            glow = SimStep.ticks(scriptedLoss.t() - scriptedLoss.glow());
        }
        lossTick = loss;
        glowTick = glow;
        liftScroll = scrolled(sections, liftTick);
        homeY = PlayField.HEIGHT + escort.ally().size().height();
        reset();
    }

    /**
     * The ground scrolled in the first {@code ticks} steps at the sections' speeds (as {@link
     * Sortie} scrolls outside a hold or an arena).
     */
    private static double scrolled(List<LevelScript.Section> sections, int ticks) {
        double distance = 0;
        int section = 0;
        for (int tick = 0; tick < ticks && !sections.isEmpty(); tick++) {
            while (section < sections.size() - 1
                    && tick >= SimStep.ticks(sections.get(section).end())) {
                section++;
            }
            distance += sections.get(section).speed() * SimStep.SECONDS;
        }
        return distance;
    }

    /** Back to the level start: every unit waiting below the screen (an air unit on its pad or at its station) with full HP. */
    void reset() {
        if (naval != null) {
            resetNaval();
            return;
        }
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            if (air == null) {
                ally.reset(startY, escort.ally().hp());
            } else if (air.liftoff().isPresent()) {
                LevelScript.Pad pad = air.liftoff().get().pads().get(k);
                ally.resetAir(
                        Ally.State.PAD,
                        pad.x(),
                        pad.y() + liftScroll,
                        escort.ally().hp(),
                        0);
            } else {
                LevelScript.Station station = air.stations().get(k);
                ally.resetAir(
                        Ally.State.FLYING,
                        station.xAt(0),
                        station.yAt(0),
                        escort.ally().hp(),
                        1);
            }
        }
        lost = 0;
        firstHit = -1;
        firstLost = -1;
        lostSaveable = 0;
        lastLost = -1;
        glowStarted = false;
        scriptedLost = false;
        if (air != null) {
            fly(0, 0);
        }
    }

    /** One step: the units due roll in, the others roll on or hold their stations, the wrecks scroll. */
    void update(int levelTick, double groundScroll, double scrollStep) {
        if (air != null) {
            for (Ally ally : allies) {
                ally.remember();
            }
            fly(levelTick, groundScroll);
            return;
        }
        double climb = escort.enterSpeed() * SimStep.SECONDS;
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            ally.remember();
            double station = escort.stations().get(k);
            switch (ally.state()) {
                case WAITING -> {
                    if (levelTick >= enterTicks[k]) {
                        ally.state(Ally.State.ROLLING);
                        follow(ally, startY, groundScroll, 0);
                        ally.settle();
                    }
                }
                case ROLLING -> {
                    double y = Math.min(ally.y() + climb, station);
                    follow(ally, y, groundScroll, y - ally.y() + scrollStep);
                    if (y >= station) {
                        ally.state(Ally.State.STATION);
                    }
                }
                case STATION -> follow(ally, station, groundScroll, scrollStep);
                case WRECK -> ally.scroll(scrollStep);
            }
        }
    }

    /** M5 part E: back to the level start, every naval unit at its station, afloat and unhit. */
    private void resetNaval() {
        for (int k = 0; k < allies.length; k++) {
            LevelScript.NavalUnit unit = naval.units().get(k);
            allies[k].resetNaval(unit.x(), unit.y(), unit.ally().hp());
            glideTicks[k] = 0;
            fromX[k] = unit.x();
            fromY[k] = unit.y();
            flakWait[k] = flakTicks(unit.ally());
        }
        arena = BEFORE;
        heldScroll = 0;
        flakBursts = 0;
        lost = 0;
        firstHit = -1;
        firstLost = -1;
        lostSaveable = 0;
        lastLost = -1;
        lastHit = -1;
    }

    /** A unit's steps between its flak bursts; 0 without flak. */
    private static int flakTicks(AllySpec ally) {
        return ally.flakSeconds() > 0 ? Math.max(1, SimStep.ticks(ally.flakSeconds())) : 0;
    }

    /**
     * M5 part E: one step of the naval convoy. When the scroll {@code halted} for the first time the
     * units afloat glide to their lanes (or off the bottom edge); when the halt ends they hold clear
     * (a unit with a hold point glides there) until the sea has scrolled the convoy's hold, then glide
     * back to their stations. The glides run on the real steps; wrecks scroll with the ground by
     * {@code scrollStep}. A unit with flak on its station fires a burst at its interval into {@code
     * events}.
     */
    void sail(boolean halted, double scrollStep, SimEvents events) {
        for (Ally ally : allies) {
            ally.remember();
        }
        if (arena == BEFORE && halted) {
            arena = OUT;
            startGlides();
        } else if (arena == OUT && !halted) {
            if (naval.holdClear() > 0) {
                arena = HOLD;
                heldScroll = 0;
                startHolds();
            } else {
                arena = BACK;
                startGlides();
            }
        } else if (arena == HOLD) {
            heldScroll += scrollStep;
            if (heldScroll >= naval.holdClear()) {
                arena = BACK;
                startGlides();
            }
        }
        int glide = Math.max(1, SimStep.ticks(naval.glideSeconds()));
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            LevelScript.NavalUnit unit = naval.units().get(k);
            switch (ally.state()) {
                case GLIDING -> {
                    double s = Math.min(1, (double) ++glideTicks[k] / glide);
                    double e = s * s * (3 - 2 * s);
                    ally.sail(fromX[k] + (targetX(k) - fromX[k]) * e, fromY[k] + (targetY(k) - fromY[k]) * e);
                    if (s >= 1) {
                        ally.state(
                                arena == OUT || arena == HOLD
                                        ? (unit.leaves() ? Ally.State.AWAY : Ally.State.LANE)
                                        : Ally.State.STATION);
                    }
                }
                case WRECK -> ally.scroll(scrollStep);
                case STATION -> {
                    if (flakWait[k] > 0 && --flakWait[k] == 0) {
                        flakWait[k] = flakTicks(unit.ally());
                        flak(k, ally, events);
                    }
                }
                default -> {
                    // In its lane or away: it holds there until the halt ends.
                }
            }
        }
    }

    /** Every unit afloat starts gliding from where it is toward the arena phase's target. */
    private void startGlides() {
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            if (ally.lost()) {
                continue;
            }
            fromX[k] = ally.x();
            fromY[k] = ally.y();
            glideTicks[k] = 0;
            ally.state(Ally.State.GLIDING);
        }
    }

    /** The units afloat with a hold point start gliding there from where they are; the others hold where they are. */
    private void startHolds() {
        for (int k = 0; k < allies.length; k++) {
            if (!allies[k].lost() && naval.units().get(k).holds()) {
                fromX[k] = allies[k].x();
                fromY[k] = allies[k].y();
                glideTicks[k] = 0;
                allies[k].state(Ally.State.GLIDING);
            }
        }
    }

    /** Where unit {@code k}'s glide ends: its lane, below the bottom edge, its hold point or (back) its station. */
    private double targetX(int k) {
        LevelScript.NavalUnit unit = naval.units().get(k);
        if (arena == HOLD) {
            return unit.holdX();
        }
        return arena == OUT && !unit.leaves() ? naval.laneX(unit.lane()) : unit.x();
    }

    private double targetY(int k) {
        LevelScript.NavalUnit unit = naval.units().get(k);
        if (arena == HOLD) {
            return unit.holdY();
        }
        if (arena != OUT) {
            return unit.y();
        }
        return unit.leaves() ? -unit.ally().size().height() : naval.laneY();
    }

    /**
     * A flak burst of unit {@code k} (presentation, hitting nothing): above it over the convoy, at a
     * place that follows from the bursts so far (no random draw, so the level's random stream stays).
     */
    private void flak(int k, Ally ally, SimEvents events) {
        flakBursts++;
        long z = flakBursts * 0x9E3779B97F4A7C15L + k;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        double across = ((z >>> 11) & 0xFFFF) / 65535.0;
        double up = ((z >>> 32) & 0xFFFF) / 65535.0;
        double x = Math.clamp(ally.x() + (2 * across - 1) * FLAK_SIDE, 0, PlayField.WIDTH);
        double y = Math.min(PlayField.HEIGHT, ally.y() + FLAK_ABOVE + up * FLAK_RANGE);
        events.add(SimEvents.Type.ALLY_FLAK, x, y, k);
    }

    /** M5 part E: whether this is a naval convoy (Level 11), not an escort's. */
    boolean naval() {
        return naval != null;
    }

    /** M5 part E: the naval convoy's spec; null for an escort's convoy. */
    LevelScript.Naval navalSpec() {
        return naval;
    }

    /** M5 part E: where the arena is for the naval convoy: {@link #BEFORE}, {@link #OUT} or {@link #BACK}. */
    int arenaPhase() {
        return arena;
    }

    /** Unit {@code k}'s spec: the escort's ally, or (M5 part E) the naval unit's own. */
    AllySpec spec(int k) {
        return naval != null ? naval.units().get(k).ally() : escort.ally();
    }

    /**
     * M5 part E: the arena lane (1-based) naval unit {@code k}'s centre is in now, by its x; 0 when it
     * is off the play field or the arena has no lanes.
     */
    int lane(int k) {
        Ally ally = allies[k];
        if (naval == null || naval.lanes() == 0 || ally.y() < 0 || ally.y() > PlayField.HEIGHT) {
            return 0;
        }
        int lane = (int) Math.floor(ally.x() / naval.laneWidth()) + 1;
        return lane >= 1 && lane <= naval.lanes() ? lane : 0;
    }

    /** M5 part E: whether naval unit {@code k} is afloat and a slam hurts it. */
    boolean slammable(int k) {
        return naval != null && !allies[k].lost() && naval.units().get(k).ally().slams() > 0;
    }

    /** M5 part E: the naval units that can be damaged still afloat (the afloat objective's and the level-end cues' count). */
    int afloat() {
        return saveable() - lostSaveable;
    }

    /** M5 part E: the unit hit last in this attempt (the {@code ally-hit} line's); -1 before. */
    int lastHit() {
        return lastHit;
    }

    /** M5 part E: takes over {@code other}'s state (a boss checkpoint); both are of the same naval convoy. */
    void copyFrom(Convoy other) {
        for (int k = 0; k < allies.length; k++) {
            allies[k].copyFrom(other.allies[k]);
        }
        System.arraycopy(other.fromX, 0, fromX, 0, fromX.length);
        System.arraycopy(other.fromY, 0, fromY, 0, fromY.length);
        System.arraycopy(other.glideTicks, 0, glideTicks, 0, glideTicks.length);
        System.arraycopy(other.flakWait, 0, flakWait, 0, flakWait.length);
        arena = other.arena;
        heldScroll = other.heldScroll;
        flakBursts = other.flakBursts;
        lost = other.lost;
        firstHit = other.firstHit;
        firstLost = other.firstLost;
        lostSaveable = other.lostSaveable;
        lastLost = other.lastLost;
        lastHit = other.lastHit;
    }

    /**
     * M5 part D: places the air units at {@code levelTick}: on their pads (scrolled by {@code
     * groundScroll}), lifting off, at their stations, climbing out or home; a lost unit stays where
     * it was lost.
     */
    private void fly(int levelTick, double groundScroll) {
        double seconds = levelTick * SimStep.SECONDS;
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            LevelScript.Station station = air.stations().get(k);
            boolean held = k == scripted && !scriptedLost;
            switch (ally.state()) {
                case PAD -> {
                    LevelScript.Pad pad = air.liftoff().orElseThrow().pads().get(k);
                    if (levelTick < liftTick) {
                        ally.fly(pad.x(), pad.y() + liftScroll - groundScroll, 0, true);
                        break;
                    }
                    ally.state(Ally.State.LIFTING);
                    lift(ally, pad, station, levelTick, seconds, held);
                }
                case LIFTING ->
                    lift(ally, air.liftoff().orElseThrow().pads().get(k), station, levelTick, seconds, held);
                case FLYING -> {
                    if (levelTick >= climbTick) {
                        ally.state(Ally.State.CLIMBING);
                        climb(ally, station, levelTick, seconds);
                    } else {
                        ally.fly(station.xAt(seconds), station.yAt(seconds), 1, held);
                    }
                }
                case CLIMBING -> climb(ally, station, levelTick, seconds);
                case HOME -> ally.fly(station.xAt(seconds), homeY, 1, true);
                default -> {
                    // A lost unit stays where it was lost: its glide is drawn by the game.
                }
            }
        }
    }

    /**
     * A unit lifting off: from its pad's point to its station (smoothstep), its scale rising with it;
     * untouchable until it is there, and after that while {@code held} (the scripted loss's unit).
     */
    private void lift(
            Ally ally, LevelScript.Pad pad, LevelScript.Station station, int levelTick, double seconds, boolean held) {
        double s = Math.min(1, (double) (levelTick - liftTick) / liftTicks);
        double e = s * s * (3 - 2 * s);
        double x = pad.x() + (station.xAt(seconds) - pad.x()) * e;
        double y = pad.y() + (station.yAt(seconds) - pad.y()) * e;
        if (s >= 1) {
            ally.state(Ally.State.FLYING);
        }
        ally.fly(x, y, e, s < 1 || held);
    }

    /** A unit climbing out: from its station up off the top edge, speeding up (s²); home at the end. */
    private void climb(Ally ally, LevelScript.Station station, int levelTick, double seconds) {
        double s = Math.min(1, (double) (levelTick - climbTick) / climbTicks);
        double from = station.yAt(seconds);
        if (s >= 1) {
            ally.state(Ally.State.HOME);
        }
        ally.fly(station.xAt(seconds), from + (homeY - from) * s * s, 1, true);
    }

    /** M5 part D: whether the scripted loss's glow starts at {@code levelTick} (once). */
    boolean glowDue(int levelTick) {
        if (glowStarted || levelTick < glowTick || scripted < 0 || !allies[scripted].alive()) {
            return false;
        }
        glowStarted = true;
        return true;
    }

    /** M5 part D: whether the scripted loss's lance takes its unit at {@code levelTick}: it is lost then (once). */
    boolean lossDue(int levelTick) {
        if (scriptedLost || levelTick < lossTick || scripted < 0 || !allies[scripted].alive()) {
            return false;
        }
        scriptedLost = true;
        allies[scripted].lose();
        lost++;
        return true;
    }

    /** M5 part D: the scripted loss's unit, from 0; -1 for none. */
    int scripted() {
        return scripted;
    }

    /** M5 part D: whether this is an air escort. */
    boolean air() {
        return air != null;
    }

    /** M5 part D: whether air unit {@code k} can be hit now and its hit box overlaps {@code box} around (x, y). */
    boolean touchesInAir(int k, double x, double y, Hitbox box) {
        Ally ally = allies[k];
        return ally.touchable() && escort.ally().hitbox().overlaps(ally.x(), ally.y(), box, x, y);
    }

    /** M5 part D: the first air unit that can be hit now whose hit box overlaps {@code box} around (x, y); -1 for none. */
    int touchingInAir(double x, double y, Hitbox box) {
        for (int k = 0; k < allies.length; k++) {
            if (touchesInAir(k, x, y, box)) {
                return k;
            }
        }
        return -1;
    }

    /** Puts a unit on the road at height {@code y}, facing along it. */
    private void follow(Ally ally, double y, double groundScroll, double distance) {
        double along = groundScroll + y - PlayField.HEIGHT / 2.0;
        ally.place(road.x(along), y, road.headingDegrees(along), distance);
    }

    /**
     * The unit an objective-aimed attack from (x, y) goes for in mode {@code nearest}: the closest
     * living unit on the screen, if it is closer than the ship (centre to centre); -1 for the ship.
     */
    int nearest(double x, double y, double shipX, double shipY) {
        double best = (shipX - x) * (shipX - x) + (shipY - y) * (shipY - y);
        int found = -1;
        double half = escort.ally().size().height() / 2;
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            if (!ally.rolling() || ally.y() + half <= 0) {
                continue;
            }
            double distance = (ally.x() - x) * (ally.x() - x) + (ally.y() - y) * (ally.y() - y);
            if (distance < best) {
                best = distance;
                found = k;
            }
        }
        return found;
    }

    /** The first living unit on the road whose hit box overlaps {@code box} around (x, y); -1 for none. */
    int touching(double x, double y, Hitbox box) {
        Hitbox hitbox = escort.ally().hitbox();
        for (int k = 0; k < allies.length; k++) {
            Ally ally = allies[k];
            if (ally.rolling() && hitbox.overlaps(ally.x(), ally.y(), box, x, y)) {
                return k;
            }
        }
        return -1;
    }

    /** Whether unit {@code k} is on the road and its hit box overlaps {@code box} around (x, y). */
    boolean touches(int k, double x, double y, Hitbox box) {
        Ally ally = allies[k];
        return ally.rolling() && escort.ally().hitbox().overlaps(ally.x(), ally.y(), box, x, y);
    }

    /** Unit {@code k} takes damage; returns whether that destroyed it. */
    boolean damage(int k, double amount) {
        if (firstHit < 0) {
            firstHit = k;
        }
        lastHit = k;
        if (!allies[k].damage(amount)) {
            return false;
        }
        if (firstLost < 0) {
            firstLost = k;
        }
        lost++;
        lostSaveable++;
        lastLost = k;
        return true;
    }

    /** The units still alive. */
    int alive() {
        return allies.length - lost;
    }

    /** M5 part D: the units the player can save, every unit but the scripted loss's. */
    int saveable() {
        return saveable;
    }

    /** M5 part D: the saveable units still alive: at the level end, the units home. */
    int saveableAlive() {
        return saveable() - lostSaveable;
    }

    /** M5 part D: the saveable units lost so far. */
    int lostSaveable() {
        return lostSaveable;
    }

    /** M5 part D: the unit lost last in this attempt (not the scripted loss); -1 before. */
    int lastLost() {
        return lastLost;
    }

    /** Whether every saveable unit is lost (every unit without a scripted loss): the primary objective failed. */
    boolean allLost() {
        return lostSaveable == saveable();
    }

    int size() {
        return allies.length;
    }

    Ally get(int k) {
        return allies[k];
    }

    /** The unit hit first in this attempt; -1 before. */
    int firstHit() {
        return firstHit;
    }

    /** The unit lost first in this attempt; -1 before. */
    int firstLost() {
        return firstLost;
    }

    /** The escort objective's spec; null for a naval convoy (M5 part E). */
    LevelScript.Escort escort() {
        return escort;
    }

    void addTo(StateHash hash) {
        hash.add(lost).add(firstHit).add(firstLost);
        if (naval != null) {
            // M5 part E: only a naval convoy's, so the escorts hash as before.
            hash.add(lostSaveable)
                    .add(lastLost)
                    .add(lastHit)
                    .add(arena)
                    .add(flakBursts)
                    .add(heldScroll);
            for (int k = 0; k < allies.length; k++) {
                hash.add(fromX[k]).add(fromY[k]).add(glideTicks[k]).add(flakWait[k]);
            }
        }
        if (air != null) {
            // M5 part D: only an air escort's, so Level 04's convoy hashes as before.
            hash.add(lostSaveable).add(lastLost).add(glowStarted ? 1 : 0).add(scriptedLost ? 1 : 0);
        }
        for (Ally ally : allies) {
            ally.addTo(hash);
        }
    }
}
