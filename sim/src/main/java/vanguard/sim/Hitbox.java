package vanguard.sim;

/** An axis-aligned hit box of {@code width} x {@code height} pixels, centred on its owner's position. */
public record Hitbox(double width, double height) {
    /** Whether this box around (x, y) overlaps {@code other} around (otherX, otherY); touching edges do not count. */
    boolean overlaps(double x, double y, Hitbox other, double otherX, double otherY) {
        return Math.abs(x - otherX) * 2 < width + other.width && Math.abs(y - otherY) * 2 < height + other.height;
    }
}
