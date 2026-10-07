package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vanguard.sim.PlayField;

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
 * @param images the folder of another level whose images it uses (e.g. {@code level-04}, for a level
 *     whose own art does not exist yet); its own folder when not given
 */
public record BackdropData(
        Map<BackdropLayer, Double> scrollFactors,
        double ramp,
        String hazeColour,
        Atmospheres atmosphere,
        Map<String, TileSet> tileSets,
        Map<String, Piece> pieces,
        List<PlacedPiece> placed,
        Optional<String> images) {
    public BackdropData {
        images.ifPresent(folder -> Check.that(folder.matches("level-\\d{2}"), "images: a level folder, level-NN"));
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

    /**
     * Every placement along the scroll with the repeats written out, in the order of {@link
     * #placed} (a repeat's copies follow it): what the renderer draws and the checks sample.
     */
    public List<PlacedPiece> placements() {
        List<PlacedPiece> out = new ArrayList<>();
        for (PlacedPiece p : placed) {
            for (int n = 0; n < p.count(); n++) {
                out.add(p.copy(n));
            }
        }
        return out;
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
     * @param size its image's size; a tower's footprint on the ground
     * @param frames an animation loop of this many frames, played at {@code fps}
     * @param headings an angle set of this many frames (frame i heads i × 360° / headings
     *     clockwise from up the screen), turned along a path
     * @param midSize whether it counts towards the art direction's mid-size set pieces per screen
     * @param tower a ground piece drawn in true perspective as a tower (design/art-direction,
     *     Perspective towers are scenery): walls up from its footprint and its image as the roof
     */
    public record Piece(
            BackdropLayer layer,
            Size size,
            Optional<Integer> frames,
            Optional<Double> fps,
            Optional<Integer> headings,
            Optional<Boolean> midSize,
            Optional<Tower> tower) {
        public Piece {
            Check.that(frames.isPresent() == fps.isPresent(), "an animation gives frames and fps");
            Check.that(frames.isEmpty() || headings.isEmpty(), "give frames (an animation) or headings, not both");
            frames.ifPresent(n -> Check.that(n >= 2, "frames must be >= 2, was " + n));
            fps.ifPresent(f -> Check.positive("fps", f));
            headings.ifPresent(n -> Check.that(n >= 2, "headings must be >= 2, was " + n));
            Check.that(
                    tower.isEmpty() || layer == BackdropLayer.GROUND,
                    "tower: only a ground piece is a tower, this one is on " + layer.key());
            Check.that(
                    tower.isEmpty() || (frames.isEmpty() && headings.isEmpty()),
                    "tower: a tower's roof is one image (no frames or headings)");
        }

        /** Whether it is drawn in true perspective as a tower. */
        public boolean isTower() {
            return tower.isPresent();
        }

        /**
         * The size of its image: the frame's {@link #size}, or a tower's roof as drawn, the
         * footprint at the roof's scale rounded to whole pixels.
         */
        public int imageWidth() {
            return (int) Math.round(size.width() * tower.map(Tower::scale).orElse(1.0));
        }

        /** See {@link #imageWidth}. */
        public int imageHeight() {
            return (int) Math.round(size.height() * tower.map(Tower::scale).orElse(1.0));
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
     * A tower: a ground piece drawn in true perspective (design/art-direction, Perspective towers
     * are scenery; user decision D1 of M5 part B: scenery only, nothing the simulation knows stands
     * on it). Its piece's {@code size} is the footprint on the ground; its roof, the piece's image,
     * is drawn at {@link #scale} round the {@link #CENTRE_X projection centre}, and the walls run from
     * the footprint's edges up to the roof's.
     *
     * @param height the roof's height in camera units, 0 < h <= {@value #MAX_HEIGHT}
     * @param wall the wall texture's image id ({@code <wall>.png} beside the level's other backdrop
     *     images): its columns run along a wall, seen from outside left to right, and repeat along a
     *     long one; its rows run from the foot (the image's bottom row) up to the roof's edge (its top
     *     row), window rows included
     * @param shade the brightness of the walls that face right and down the screen, away from the
     *     key light (0..1, default {@value #DEFAULT_SHADE}); those facing up and left are drawn as the
     *     texture
     */
    public record Tower(double height, String wall, Optional<Double> shade) {
        /** The camera's height over the ground, in the units of {@link #height} (parallax B's camera model). */
        public static final double CAMERA = 6;
        /** The tallest roof: at most a third more than the ground's scroll (k = 1.33). */
        public static final double MAX_HEIGHT = 1.5;

        public static final double DEFAULT_SHADE = 0.5;
        /** The projection centre, x px from the play field's left edge. */
        public static final double CENTRE_X = PlayField.WIDTH / 2.0;
        /** The projection centre, y px up from the play field's bottom edge: 297 px below its top (55 % down). */
        public static final double CENTRE_Y = PlayField.HEIGHT - 297;

        public Tower {
            Check.that(
                    height > 0 && height <= MAX_HEIGHT,
                    "tower.height must be > 0 and <= " + MAX_HEIGHT + ", was " + height);
            Check.that(
                    wall.matches("[a-z0-9]+(-[a-z0-9]+)*"),
                    "tower.wall: an image id in kebab-case, was '" + wall + "'");
            shade.ifPresent(s -> Check.share("tower.shade", s));
        }

        /** The scale the roof is drawn at round the projection centre: k = 6 / (6 − h). */
        public double scale() {
            return scaleAt(height);
        }

        /** The scale of a point on a wall {@code z} camera units above the ground. */
        public static double scaleAt(double z) {
            return CAMERA / (CAMERA - z);
        }

        public double shadeOrDefault() {
            return shade.orElse(DEFAULT_SHADE);
        }

        /**
         * How fast the roof moves on its own, px/s, while the ground scrolls at {@code groundSpeed}:
         * it is drawn at (k − 1) × the ground's distance from the projection centre, so it slides
         * past the ground below it at (k − 1) × the scroll (the motion budget's own speed).
         */
        public double ownSpeed(double groundSpeed) {
            return (scale() - 1) * Math.abs(groundSpeed);
        }

        /** Where a point of the ground at {@code coordinate} is drawn on a layer of scale {@code k}, round {@code centre}. */
        public static double project(double coordinate, double centre, double k) {
            return centre + (coordinate - centre) * k;
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
     * @param repeat the placement repeated along the scroll (a stream of traffic): {@code count}
     *     placements in all, each {@code every} seconds after the one before, its path shifted in
     *     time with it; see {@link BackdropData#placements()}
     */
    public record PlacedPiece(
            String piece,
            double t,
            double x,
            Optional<Boolean> mirror,
            Optional<List<Waypoint>> path,
            Optional<Boolean> overhead,
            Optional<Repeat> repeat) {
        public PlacedPiece {
            path.ifPresent(points -> {
                Check.that(points.size() >= 2, "path: give at least two waypoints");
                for (int i = 1; i < points.size(); i++) {
                    Check.that(points.get(i).t() > points.get(i - 1).t(), "path: waypoints in time order");
                }
            });
        }

        /** The {@code n}-th placement of its repeat (0: itself), {@code n} × {@code every} seconds later, without the repeat. */
        public PlacedPiece copy(int n) {
            double shift = repeat.map(r -> r.every() * n).orElse(0.0);
            return new PlacedPiece(
                    piece,
                    t + shift,
                    x,
                    mirror,
                    path.map(points -> points.stream()
                            .map(p -> new Waypoint(p.t() + shift, p.dx(), p.dy()))
                            .toList()),
                    overhead,
                    Optional.empty());
        }

        /** How many placements it stands for: its repeat's count, else 1. */
        public int count() {
            return repeat.map(Repeat::count).orElse(1);
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

    /**
     * A placement repeated along the scroll.
     *
     * @param count the placements in all, the first included (at least 2)
     * @param every seconds from one placement to the next
     */
    public record Repeat(int count, double every) {
        public Repeat {
            Check.that(count >= 2, "repeat.count must be >= 2, was " + count);
            Check.positive("repeat.every", every);
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
