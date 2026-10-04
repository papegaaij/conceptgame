package vanguard.sim;

import java.util.List;

/**
 * A hit shape made of boxes around its owner's position, for a hull that a single box fits badly
 * (design/player/ship, Hitbox: the Stormhawk's nose, body and swept wings).
 *
 * @param parts the boxes; together they cover the visible hull
 */
public record Hull(List<Part> parts) {
    public Hull {
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("a hull needs at least one box");
        }
    }

    /**
     * One box of the hull.
     *
     * @param dx the box centre right of the owner's position
     * @param dy the box centre above the owner's position
     * @param box the box
     */
    public record Part(double dx, double dy, Hitbox box) {}

    /** Whether this hull around (x, y) overlaps {@code other} around (otherX, otherY). */
    boolean overlaps(double x, double y, Hitbox other, double otherX, double otherY) {
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            if (part.box().overlaps(x + part.dx(), y + part.dy(), other, otherX, otherY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this hull around (x, y) touches the segment from (ax, ay) to (bx, by) thickened by
     * {@code halfWidth} on each side (a beam): a slab test against each box grown by it.
     */
    boolean touchesSegment(double x, double y, double ax, double ay, double bx, double by, double halfWidth) {
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            double cx = x + part.dx();
            double cy = y + part.dy();
            double hw = part.box().width() / 2 + halfWidth;
            double hh = part.box().height() / 2 + halfWidth;
            if (slab(ax - cx, bx - ax, hw, ay - cy, by - ay, hh)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the segment from (px, py) along (dx, dy), t in [0, 1], enters the box |x| < hw, |y| < hh. */
    private static boolean slab(double px, double dx, double hw, double py, double dy, double hh) {
        double enter = 0;
        double leave = 1;
        if (dx == 0) {
            if (Math.abs(px) >= hw) {
                return false;
            }
        } else {
            double t1 = (-hw - px) / dx;
            double t2 = (hw - px) / dx;
            enter = Math.max(enter, Math.min(t1, t2));
            leave = Math.min(leave, Math.max(t1, t2));
        }
        if (dy == 0) {
            if (Math.abs(py) >= hh) {
                return false;
            }
        } else {
            double t1 = (-hh - py) / dy;
            double t2 = (hh - py) / dy;
            enter = Math.max(enter, Math.min(t1, t2));
            leave = Math.min(leave, Math.max(t1, t2));
        }
        return enter < leave;
    }
}
