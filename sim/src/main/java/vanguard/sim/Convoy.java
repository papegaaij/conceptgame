package vanguard.sim;

/**
 * The convoy of an {@code escort} primary objective (design/allies, civilian crawler; design/
 * campaign, Level 04): its units roll in from the bottom edge one after another to their stations
 * in a column and follow the level's {@link Road} there at the scroll speed, so they hold their
 * height on the screen and drift sideways as the road winds. A destroyed unit stops and lies on the
 * road as a wreck. The units are allocated up front, so stepping does not allocate.
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

    Convoy(LevelScript.Escort escort, Road road) {
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
        reset();
    }

    /** Back to the level start: every unit waiting below the screen with full HP. */
    void reset() {
        for (Ally ally : allies) {
            ally.reset(startY, escort.ally().hp());
        }
        lost = 0;
        firstHit = -1;
        firstLost = -1;
    }

    /** One step: the units due roll in, the others roll on or hold their stations, the wrecks scroll. */
    void update(int levelTick, double groundScroll, double scrollStep) {
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
        return true;
    }

    /** The units still alive. */
    int alive() {
        return allies.length - lost;
    }

    /** Whether every unit is lost: the primary objective failed. */
    boolean allLost() {
        return lost == allies.length;
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
        for (Ally ally : allies) {
            ally.addTo(hash);
        }
    }
}
