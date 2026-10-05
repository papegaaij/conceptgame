package vanguard.game.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An act boss's chained death (design/enemies/bosses: {@code huge}, chained explosions from tail to
 * head over 3 s): a burst at every part and a row of bursts along the hull between them, each started
 * by its place along the hull's long axis, the tail at once and the head at the chain's end. Offsets
 * are px from the boss's centre on the screen (x right, y up) in the pose it died in; the pose's angle
 * turns the nose-down axis (the head at the bottom). Pure presentation.
 */
public final class DeathChain {
    /** The bursts along the hull between the parts, so the chain reads as one ripple. */
    static final int HULL_BURSTS = 12;
    /** The hull bursts alternate this share of the hull's width either side of its axis. */
    private static final double HULL_SPREAD = 0.22;

    /**
     * A burst of the chain: at (dx, dy) from the centre, {@code at} steps after the death; {@code part}
     * is the part's index, or -1 for a hull burst.
     */
    public record Burst(double dx, double dy, int at, int part) {}

    private DeathChain() {}

    /**
     * The chain's bursts, earliest first.
     *
     * @param partX the parts' offsets at the death, px right
     * @param partY the parts' offsets at the death, px up
     * @param angle the pose, radians counter-clockwise (y up): 0 nose-down, pi/2 broadside head right
     * @param length the hull's length (its nose-down height), px
     * @param width the hull's width, px
     * @param ticks the chain's length, steps
     */
    static List<Burst> plan(double[] partX, double[] partY, double angle, double length, double width, int ticks) {
        // The head's direction: straight down when nose-down, turned with the pose.
        double headX = Math.sin(angle);
        double headY = -Math.cos(angle);
        // Across the hull.
        double sideX = Math.cos(angle);
        double sideY = Math.sin(angle);
        List<Burst> bursts = new ArrayList<>();
        for (int p = 0; p < partX.length; p++) {
            double along = partX[p] * headX + partY[p] * headY;
            bursts.add(new Burst(partX[p], partY[p], at(along, length, ticks), p));
        }
        for (int i = 0; i < HULL_BURSTS; i++) {
            double along = -length / 2 + length * (i + 0.5) / HULL_BURSTS;
            double across = (i % 2 == 0 ? 1 : -1) * width * HULL_SPREAD;
            bursts.add(new Burst(
                    headX * along + sideX * across, headY * along + sideY * across, at(along, length, ticks), -1));
        }
        bursts.sort(Comparator.comparingInt(Burst::at));
        return bursts;
    }

    /** The step a burst starts: by its place from the tail ({@code -length / 2}) to the head ({@code length / 2}). */
    private static int at(double along, double length, int ticks) {
        double share = Math.clamp((along + length / 2) / length, 0, 1);
        return (int) Math.round(share * ticks);
    }

    /** The head end of the hull, px from the centre: x right. */
    static double headX(double angle, double length) {
        return Math.sin(angle) * length / 2;
    }

    /** The head end of the hull, px from the centre: y up. */
    static double headY(double angle, double length) {
        return -Math.cos(angle) * length / 2;
    }
}
