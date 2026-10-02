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
}
