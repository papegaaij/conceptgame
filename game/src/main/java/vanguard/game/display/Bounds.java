package vanguard.game.display;

/** A rectangle in desktop (screen) coordinates: a window or a monitor's work area. */
public record Bounds(int x, int y, int width, int height) {
    /** Whether the centre of {@code other} lies inside this rectangle. */
    public boolean containsCentreOf(Bounds other) {
        int centreX = other.x + other.width / 2;
        int centreY = other.y + other.height / 2;
        return centreX >= x && centreX < x + width && centreY >= y && centreY < y + height;
    }
}
