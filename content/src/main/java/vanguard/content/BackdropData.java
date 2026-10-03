package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A level's backdrop ({@code backdrop} in its data.yaml): presentation only, the simulation never
 * reads it. Every section names a tile set per layer (in {@link LevelData.Section#tiles()}); set
 * pieces are placed along the scroll; each section's atmosphere intensity picks its cloud banks,
 * wisps and haze.
 *
 * @param scrollFactors every layer's scroll speed relative to the ground layer ({@code deep} optional:
 *     a surface level without one has opaque ground tiles, see {@link #base()})
 * @param ramp seconds an atmosphere change takes, centred on the section boundary
 * @param hazeColour the setting's haze colour, {@code rrggbb}
 * @param atmosphere what each atmosphere intensity draws
 * @param tileSets the repeating textures, by id
 * @param pieces the set pieces, by id
 * @param placed the set pieces along the scroll; on a layer, later ones are drawn over earlier ones
 */
public record BackdropData(
        Map<BackdropLayer, Double> scrollFactors,
        double ramp,
        String hazeColour,
        Atmospheres atmosphere,
        Map<String, TileSet> tileSets,
        Map<String, Piece> pieces,
        List<PlacedPiece> placed) {
    public BackdropData {
        Check.that(
                scrollFactors.keySet().containsAll(EnumSet.complementOf(EnumSet.of(BackdropLayer.DEEP))),
                "scroll_factors: give every layer (deep, far, ground, low-air, high-air; deep may be left"
                        + " out on a surface whose ground tiles cover the screen)");
        scrollFactors.forEach((layer, factor) -> Check.positive("scroll_factors." + layer.key(), factor));
        Check.that(
                scrollFactors.get(BackdropLayer.GROUND) == 1.0,
                "scroll_factors.ground: the ground layer scrolls at the level's speed (1.0)");
        Check.positive("ramp", ramp);
        Check.that(hazeColour.matches("[0-9a-fA-F]{6}"), "haze_colour: a colour as rrggbb");
    }

    /** The layer's scroll factor; 0 for a {@code deep} layer the level does not have. */
    public double factor(BackdropLayer layer) {
        return scrollFactors.getOrDefault(layer, 0.0);
    }

    /** Whether the level has a {@code deep} layer (a top-down surface such as Luna's has none). */
    public boolean hasDeep() {
        return scrollFactors.containsKey(BackdropLayer.DEEP);
    }

    /**
     * The layer that has to cover the whole screen, which nothing is drawn behind
     * (design/art-direction, Parallax layer model): {@code deep}, or {@code ground} on a surface
     * without one (Level 04's Luna), where the far layer shows only through the ground's openings.
     */
    public BackdropLayer base() {
        return hasDeep() ? BackdropLayer.DEEP : BackdropLayer.GROUND;
    }

    /**
     * A texture that repeats along its layer: as wide as the play field and {@code height} px
     * tall.
     *
     * @param drift px/s it drifts sideways (positive to the right), wrapping around
     */
    public record TileSet(BackdropLayer layer, int height, Optional<Double> drift) {
        public TileSet {
            Check.positive("height", height);
        }

        public boolean drifts() {
            return drift.orElse(0.0) != 0;
        }
    }

    /**
     * A set piece, pre-rendered at its layer's scale.
     *
     * @param frames an animation loop of this many frames, played at {@code fps}
     * @param headings an angle set of this many frames (frame i heads i × 360° / headings
     *     clockwise from up the screen), turned along a path
     * @param midSize whether it counts towards the art direction's mid-size set pieces per screen
     */
    public record Piece(
            BackdropLayer layer,
            Size size,
            Optional<Integer> frames,
            Optional<Double> fps,
            Optional<Integer> headings,
            Optional<Boolean> midSize) {
        public Piece {
            Check.that(frames.isPresent() == fps.isPresent(), "an animation gives frames and fps");
            Check.that(frames.isEmpty() || headings.isEmpty(), "give frames (an animation) or headings, not both");
            frames.ifPresent(n -> Check.that(n >= 2, "frames must be >= 2, was " + n));
            fps.ifPresent(f -> Check.positive("fps", f));
            headings.ifPresent(n -> Check.that(n >= 2, "headings must be >= 2, was " + n));
        }

        /** The number of images: animation frames, headings or 1. */
        public int imageCount() {
            return frames.or(() -> headings).orElse(1);
        }

        public boolean animated() {
            return frames.isPresent();
        }

        public boolean countsAsMidSize() {
            return midSize.orElse(false);
        }
    }

    /**
     * A set piece along the scroll: its centre passes the middle of the screen at {@code t}
     * seconds, {@code x} px from the play field's left edge.
     *
     * @param mirror drawn flipped left to right
     * @param path how it moves within its layer; before the first waypoint it rests at that
     *     waypoint's offset, after the last at the last one's
     * @param overhead a ground piece's part above the road (a bridge's arches, a gate's roof): drawn
     *     over the convoy and the ground objects and under the ground units, which may stand on it
     */
    public record PlacedPiece(
            String piece,
            double t,
            double x,
            Optional<Boolean> mirror,
            Optional<List<Waypoint>> path,
            Optional<Boolean> overhead) {
        public PlacedPiece {
            path.ifPresent(points -> {
                Check.that(points.size() >= 2, "path: give at least two waypoints");
                for (int i = 1; i < points.size(); i++) {
                    Check.that(points.get(i).t() > points.get(i - 1).t(), "path: waypoints in time order");
                }
            });
        }

        public boolean mirrored() {
            return mirror.orElse(false);
        }

        public boolean isOverhead() {
            return overhead.orElse(false);
        }

        /** The path's sideways offset at {@code time}, px. */
        public double offsetX(double time) {
            return path.isPresent() ? along(path.get(), time, true) : 0;
        }

        /** The path's offset up the screen at {@code time}, px. */
        public double offsetY(double time) {
            return path.isPresent() ? along(path.get(), time, false) : 0;
        }

        /** The direction of travel at {@code time}: degrees clockwise from up the screen, 0 without a path. */
        public double heading(double time) {
            if (path.isEmpty()) {
                return 0;
            }
            List<Waypoint> points = path.get();
            int i = 1;
            while (i < points.size() - 1 && time >= points.get(i).t()) {
                i++;
            }
            Waypoint a = points.get(i - 1);
            Waypoint b = points.get(i);
            double degrees = Math.toDegrees(Math.atan2(b.dx() - a.dx(), b.dy() - a.dy()));
            return degrees < 0 ? degrees + 360 : degrees;
        }

        private static double along(List<Waypoint> points, double time, boolean x) {
            Waypoint first = points.getFirst();
            if (time <= first.t()) {
                return x ? first.dx() : first.dy();
            }
            for (int i = 1; i < points.size(); i++) {
                Waypoint b = points.get(i);
                if (time < b.t()) {
                    Waypoint a = points.get(i - 1);
                    double f = (time - a.t()) / (b.t() - a.t());
                    return x ? a.dx() + (b.dx() - a.dx()) * f : a.dy() + (b.dy() - a.dy()) * f;
                }
            }
            Waypoint last = points.getLast();
            return x ? last.dx() : last.dy();
        }
    }

    /** At {@code t} seconds the piece is {@code dx} px to the right and {@code dy} px up from its place. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Waypoint(double t, double dx, double dy) {}

    /** What each atmosphere intensity draws; only the intensities the level uses need one. */
    public record Atmospheres(Optional<Look> clear, Optional<Look> light, Optional<Look> medium, Optional<Look> heavy) {
        public Optional<Look> of(LevelData.Atmosphere atmosphere) {
            return switch (atmosphere) {
                case CLEAR -> clear;
                case LIGHT -> light;
                case MEDIUM -> medium;
                case HEAVY -> heavy;
            };
        }
    }

    /**
     * An atmosphere intensity's look.
     *
     * @param banks the cloud-bank tile set on low-air
     * @param wisps the wisp tile set on high-air
     * @param haze how strongly the haze colour veils the deep and far layers, 0..1
     */
    public record Look(Optional<String> banks, Optional<String> wisps, double haze) {
        public Look {
            Check.share("haze", haze);
        }
    }
}
