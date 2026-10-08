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

    Convoy(LevelScript.Escort escort, Road road) {
        this(escort, road, List.of());
    }

    /**
     * @param sections the level's sections: an air escort's pads scroll with the ground at their
     *     speeds until the liftoff
     */
    Convoy(LevelScript.Escort escort, Road road, List<LevelScript.Section> sections) {
        this.escort = escort;
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

    LevelScript.Escort escort() {
        return escort;
    }

    void addTo(StateHash hash) {
        hash.add(lost).add(firstHit).add(firstLost);
        if (air != null) {
            // M5 part D: only an air escort's, so Level 04's convoy hashes as before.
            hash.add(lostSaveable).add(lastLost).add(glowStarted ? 1 : 0).add(scriptedLost ? 1 : 0);
        }
        for (Ally ally : allies) {
            ally.addTo(hash);
        }
    }
}
