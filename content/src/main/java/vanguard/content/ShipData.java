package vanguard.content;

import java.util.List;

/**
 * design/player/ship/data.yaml: the AF-12 Stormhawk's flight numbers. Its top speed is the fitted
 * engine's ({@link SystemsData}).
 *
 * @param size the edge of the square hull sprite in px
 * @param edgeGap the gap the hull keeps to every play field edge in px
 * @param bankChangeSteps game frames from level flight to the hard bank frame
 * @param mounts mount points in px from the sprite's top left
 */
public record ShipData(
        double accelerationSeconds,
        double stopSeconds,
        double precisionFactor,
        double size,
        double edgeGap,
        Size hitbox,
        double collectionRadius,
        double mercySeconds,
        int bankChangeSteps,
        Mounts mounts) {
    public ShipData {
        Check.positive("acceleration_seconds", accelerationSeconds);
        Check.positive("stop_seconds", stopSeconds);
        Check.share("precision_factor", precisionFactor);
        Check.positive("size", size);
        Check.notNegative("edge_gap", edgeGap);
        Check.positive("collection_radius", collectionRadius);
        Check.notNegative("mercy_seconds", mercySeconds);
        Check.positive("bank_change_steps", bankChangeSteps);
    }

    public record Mounts(Point front, List<Point> wings, Point rear, List<Point> engines) {
        public Mounts {
            Check.that(wings.size() == 2, "wings: two mount points, left and right");
        }
    }
}
