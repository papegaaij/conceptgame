package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vanguard.content.BackdropData;
import vanguard.content.BackdropLayer;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;

/**
 * A level without a deep layer (Luna) shows its terrain on the ground and far layers: there a tile
 * set repeats without a seam, and where a section's tile set changes the terrain runs on. Level 04's
 * mare showed a 1 px line where its tiles repeated (a noise octave whose lattice did not divide the
 * tile's height). Over open space (a level with a deep layer, Level 07's L1 point) every tile set
 * repeats without a line too, the translucent banks and streaks included, Level 07's set pieces keep
 * clear of their image borders on every layer, and nothing but the deep layer is on screen in the
 * boss arena, where an early kill makes the clock (and the scroll) jump to the arena's end. Level 08's
 * city has a deep layer under the harbour's water, but its ground covers the screen elsewhere, so
 * there too the ground runs on where a section's tile set changes (under a cross highway), and its
 * set pieces keep clear of their image borders as Level 07's do.
 */
class BackdropSeamsTest {
    private static final String LEVEL_07 = "act-1-first-contact/level-07-brood-carrier";
    private static final String LEVEL_08 = "act-2-homefront/level-08-neon-skyline";
    private static final Path BACKDROP = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "backdrop");
    private static final BackdropLayer[] TERRAIN = {BackdropLayer.GROUND, BackdropLayer.FAR};
    /** How much more the rows around the wrap may change than the rows around them (the old line: 3.2 to 3.7). */
    private static final double WRAP_LIMIT = 1.5;
    /** Rows above and below the wrap whose changes are the reference. */
    private static final int REFERENCE = 40;
    /** A pixel differs at a border when its luminance differs by more than this, or its alpha. */
    private static final double PIXEL_LIMIT = 16;

    private final Map<Path, BufferedImage> images = new HashMap<>();

    @Test
    void terrainTileSetsWrapTopToBottom() throws IOException {
        List<String> seams = new ArrayList<>();
        for (Map.Entry<String, LevelData> entry : terrainLevels().entrySet()) {
            Path folder = BACKDROP.resolve(Backdrop.folder(entry.getKey(), entry.getValue()));
            for (Map.Entry<String, BackdropData.TileSet> tileSet :
                    entry.getValue().backdrop().tileSets().entrySet()) {
                if (!terrain(tileSet.getValue().layer())) {
                    continue;
                }
                double score = wrapScore(image(folder.resolve(tileSet.getKey() + ".png")));
                if (score > WRAP_LIMIT) {
                    seams.add(String.format(Locale.ROOT, "%s/%s: %.2f", folder.getFileName(), tileSet.getKey(), score));
                }
            }
        }
        assertEquals(
                List.of(), seams, "tile sets with a seam where they repeat (rows changing x times their neighbours)");
    }

    @Test
    void theTerrainRunsOnWhereASectionsTileSetChanges() throws IOException {
        List<String> seams = new ArrayList<>();
        for (Map.Entry<String, LevelData> entry : terrainLevels().entrySet()) {
            LevelData level = entry.getValue();
            Path folder = BACKDROP.resolve(Backdrop.folder(entry.getKey(), level));
            for (BackdropLayer layer : TERRAIN) {
                for (int s = 1; s < level.sections().size(); s++) {
                    String below = tileOn(level, s - 1, layer);
                    String above = tileOn(level, s, layer);
                    if (below == null || above == null || below.equals(above)) {
                        continue;
                    }
                    long seam = Math.round(level.seam(layer, s));
                    // Both tile sets over the rows next to the border, the set pieces on top: they
                    // match, or a feature crosses the border without a piece cut from one render
                    // over it.
                    int[][] lower = composite(level, layer, folder, below, seam - 2, seam + 2);
                    int[][] upper = composite(level, layer, folder, above, seam - 2, seam + 2);
                    double share = differing(lower, upper);
                    if (share > 0.002) {
                        seams.add(String.format(
                                Locale.ROOT,
                                "%s%s -> %s at %d: %.1f %% of the pixels",
                                folder,
                                below,
                                above,
                                seam,
                                share * 100));
                    }
                }
            }
        }
        assertEquals(List.of(), seams, "set borders where the terrain does not run on");
    }

    @Test
    void tileSetsOverSpaceWrapWithoutALine() throws IOException {
        List<String> seams = new ArrayList<>();
        for (Map.Entry<String, LevelData> entry :
                ContentLoader.fromClasspath().levels().entrySet()) {
            LevelData level = entry.getValue();
            if (!level.backdrop().hasDeep()) {
                continue;
            }
            Path folder = BACKDROP.resolve(Backdrop.folder(entry.getKey(), level));
            for (Map.Entry<String, BackdropData.TileSet> tileSet :
                    level.backdrop().tileSets().entrySet()) {
                BufferedImage image = image(folder.resolve(tileSet.getKey() + ".png"));
                double rows = translucentWrapScore(image, false);
                double columns = tileSet.getValue().drifts() ? translucentWrapScore(image, true) : 0;
                if (Math.max(rows, columns) > WRAP_LIMIT) {
                    seams.add(String.format(
                            Locale.ROOT,
                            "%s/%s: rows %.2f, columns %.2f",
                            folder.getFileName(),
                            tileSet.getKey(),
                            rows,
                            columns));
                }
            }
        }
        assertEquals(List.of(), seams, "tile sets with a line where they repeat (a drifting one also sideways)");
    }

    @Test
    void levelSevensSetPiecesShowNoEdgeOnAnyLayer() throws IOException {
        assertEquals(
                List.of(), cutPieces(LEVEL_07, "level-07/"), "pieces cut at an image border that crosses the screen");
    }

    @Test
    void levelEightsSetPiecesShowNoEdgeOnAnyLayer() throws IOException {
        assertEquals(
                List.of(), cutPieces(LEVEL_08, "level-08/"), "pieces cut at an image border that crosses the screen");
    }

    @Test
    void levelEightsGroundRunsOnWhereItsTileSetChanges() throws IOException {
        LevelData level = ContentLoader.fromClasspath().level(LEVEL_08);
        Path folder = BACKDROP.resolve(Backdrop.folder(LEVEL_08, level));
        assumeTrue(folder.endsWith("level-08"), "Level 08 still takes another level's backdrop images");
        List<String> seams = new ArrayList<>();
        for (int s = 1; s < level.sections().size(); s++) {
            String below = tileOn(level, s - 1, BackdropLayer.GROUND);
            String above = tileOn(level, s, BackdropLayer.GROUND);
            if (below == null || above == null || below.equals(above)) {
                continue;
            }
            long seam = Math.round(level.seam(BackdropLayer.GROUND, s));
            double share = differing(
                    composite(level, BackdropLayer.GROUND, folder, below, seam - 2, seam + 2),
                    composite(level, BackdropLayer.GROUND, folder, above, seam - 2, seam + 2));
            if (share > 0.002) {
                seams.add(String.format(
                        Locale.ROOT, "%s -> %s at %d: %.1f %% of the pixels", below, above, seam, share * 100));
            }
        }
        assertEquals(List.of(), seams, "ground tile set changes that no piece covers");
    }

    /** A level's flat set pieces (towers aside: a roof lies over its own walls) cut at an image border on screen. */
    private List<String> cutPieces(String key, String own) throws IOException {
        LevelData level = ContentLoader.fromClasspath().level(key);
        String folder = Backdrop.folder(key, level);
        assumeTrue(folder.equals(own), key + " still takes another level's backdrop images");
        BackdropData backdrop = level.backdrop();
        List<String> cut = new ArrayList<>();
        for (BackdropData.PlacedPiece placed : backdrop.placements()) {
            BackdropData.Piece spec = backdrop.pieces().get(placed.piece());
            if (spec.isTower()) {
                continue;
            }
            int count = spec.imageCount();
            double minDx = 0;
            double maxDx = 0;
            for (BackdropData.Waypoint point : placed.path().orElse(List.of())) {
                minDx = Math.min(minDx, point.dx());
                maxDx = Math.max(maxDx, point.dx());
            }
            for (int i = 0; i < count; i++) {
                BufferedImage image = image(
                        BACKDROP.resolve(folder + (count == 1 ? placed.piece() : placed.piece() + "_" + i) + ".png"));
                int w = image.getWidth();
                long left = Math.round(placed.x() + minDx - w / 2.0);
                long right = Math.round(placed.x() + maxDx + w / 2.0);
                boolean topOrBottom = false;
                for (int x = 0; x < w; x++) {
                    // the column sweeps [left + x, right - w + x] along the path
                    boolean onScreen = left + x < PlayField.WIDTH && right - w + x >= 0;
                    topOrBottom |= onScreen && (alpha(image, x, 0) || alpha(image, x, image.getHeight() - 1));
                }
                boolean sides = false;
                for (int y = 0; y < image.getHeight(); y++) {
                    sides |= left > 0 && left < PlayField.WIDTH && alpha(image, 0, y);
                    sides |= right > 0 && right < PlayField.WIDTH && alpha(image, w - 1, y);
                }
                if (topOrBottom || sides) {
                    cut.add(placed.piece() + " (image " + i + ") at t " + placed.t());
                }
            }
        }
        return cut;
    }

    @Test
    void overOpenSpaceOnlyTheDeepLayerShowsAnythingInTheArena() {
        List<String> seen = new ArrayList<>();
        for (Map.Entry<String, LevelData> entry :
                ContentLoader.fromClasspath().levels().entrySet()) {
            LevelData level = entry.getValue();
            BackdropData backdrop = level.backdrop();
            if (!backdrop.hasDeep()) {
                continue;
            }
            for (int s = 0; s < level.sections().size(); s++) {
                if (!level.sections().get(s).isArena()) {
                    continue;
                }
                if (level.sections().get(s).speed().filter(speed -> speed == 0).isPresent()) {
                    // M5 part E: an arena of speed 0 (Level 11's) does not scroll, so the jump to its
                    // end moves nothing; its anchored boss's platform lies there on purpose.
                    continue;
                }
                double start = level.sectionStart(s);
                double end = level.sections().get(s).end();
                for (BackdropData.PlacedPiece placed : backdrop.placements()) {
                    BackdropData.Piece spec = backdrop.pieces().get(placed.piece());
                    if (spec.layer() == BackdropLayer.DEEP) {
                        continue;
                    }
                    for (double t = start; t <= end; t += SimStep.SECONDS) {
                        if (onScreen(level, placed, spec, t)) {
                            seen.add(String.format(Locale.ROOT, "%s: %s at t=%.2f", entry.getKey(), placed.piece(), t));
                            break;
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), seen, "pieces that an early kill's jump to the arena's end would pop in or out");
    }

    private static boolean onScreen(
            LevelData level, BackdropData.PlacedPiece placed, BackdropData.Piece spec, double t) {
        double y = level.pieceCentre(placed)
                - level.scrollAt(t) * level.backdrop().factor(spec.layer())
                + placed.offsetY(t);
        double x = placed.x() + placed.offsetX(t);
        double halfWidth = spec.size().width() / 2;
        double halfHeight = spec.size().height() / 2;
        return x + halfWidth > 0
                && x - halfWidth < PlayField.WIDTH
                && y + halfHeight > 0
                && y - halfHeight < PlayField.HEIGHT;
    }

    private static boolean alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24 != 0;
    }

    /**
     * {@link #wrapScore} for a tile set that may be translucent (banks, streaks): the luminance
     * premultiplied by alpha over every pixel, and each wrap pair against the pairs of the same
     * 4-row phase within 16 rows of it (an ordered dither's rows change by different amounts, and a
     * bank's texture varies over a few dozen rows); with {@code columns} the left-right wrap.
     */
    static double translucentWrapScore(BufferedImage image, boolean columns) {
        int n = columns ? image.getWidth() : image.getHeight();
        int across = columns ? image.getHeight() : image.getWidth();
        double[][] lum = new double[n][across];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < across; j++) {
                int argb = columns ? image.getRGB(i, j) : image.getRGB(j, i);
                lum[i][j] = luminance(argb) * (argb >>> 24) / 255.0;
            }
        }
        double worst = 0;
        for (int k = n - 2; k <= n; k++) {
            List<Double> reference = new ArrayList<>();
            for (int r = k - 16; r <= k + 16; r += 4) {
                if (r != k) {
                    reference.add(trimmedChange(lum, Math.floorMod(r, n), Math.floorMod(r + 1, n)));
                }
            }
            reference.sort(null);
            double median = reference.get(reference.size() / 2);
            worst = Math.max(
                    worst, trimmedChange(lum, Math.floorMod(k, n), Math.floorMod(k + 1, n)) / Math.max(median, 0.5));
        }
        return worst;
    }

    private static double trimmedChange(double[][] lum, int a, int b) {
        double[] diffs = new double[lum[a].length];
        for (int j = 0; j < diffs.length; j++) {
            diffs[j] = Math.abs(lum[a][j] - lum[b][j]);
        }
        Arrays.sort(diffs);
        int kept = (int) (diffs.length * 0.9);
        double sum = 0;
        for (int j = 0; j < kept; j++) {
            sum += diffs[j];
        }
        return kept == 0 ? 0 : sum / kept;
    }

    @Test
    void aLineWhereTheTilesRepeatIsFound() {
        BufferedImage tile = new BufferedImage(64, 120, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < tile.getHeight(); y++) {
            for (int x = 0; x < tile.getWidth(); x++) {
                int grey = 96
                        + (int) Math.round(16 * Math.sin(2 * Math.PI * y / tile.getHeight()) + ((x * 7 + y * 13) % 5));
                tile.setRGB(x, y, 0xff000000 | grey << 16 | grey << 8 | grey);
            }
        }
        assertTrue(wrapScore(tile) < WRAP_LIMIT, "a periodic tile passes");
        for (int x = 0; x < tile.getWidth(); x++) {
            tile.setRGB(x, tile.getHeight() - 1, 0xff808898);
        }
        assertTrue(wrapScore(tile) > WRAP_LIMIT, "a line in its bottom row fails");
        assertTrue(translucentWrapScore(tile, false) > WRAP_LIMIT, "and fails the translucent score");
    }

    /**
     * How much the row pairs at the wrap (the last two rows, last to first, first two) change
     * against the median change of the row pairs around them; a row pair's change is the mean
     * luminance difference of its columns without the 10 % that change most (a rail, roots).
     */
    static double wrapScore(BufferedImage image) {
        int h = image.getHeight();
        double near = Math.max(
                pairChange(image, h - 2, h - 1), Math.max(pairChange(image, h - 1, 0), pairChange(image, 0, 1)));
        List<Double> reference = new ArrayList<>();
        for (int k = h - REFERENCE; k < h + REFERENCE; k++) {
            if (k < h - 4 || k > h + 2) {
                double change = pairChange(image, Math.floorMod(k, h), Math.floorMod(k + 1, h));
                if (!Double.isNaN(change)) {
                    reference.add(change);
                }
            }
        }
        reference.sort(null);
        double median = reference.get(reference.size() / 2);
        return near / Math.max(median, 0.5);
    }

    private static double pairChange(BufferedImage image, int a, int b) {
        double[] diffs = new double[image.getWidth()];
        int n = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            int p = image.getRGB(x, a);
            int q = image.getRGB(x, b);
            if (p >>> 24 != 0 && q >>> 24 != 0) {
                diffs[n++] = Math.abs(luminance(p) - luminance(q));
            }
        }
        if (n < image.getWidth() / 4) {
            return Double.NaN;
        }
        Arrays.sort(diffs, 0, n);
        int kept = (int) (n * 0.9);
        double sum = 0;
        for (int i = 0; i < kept; i++) {
            sum += diffs[i];
        }
        return kept == 0 ? 0 : sum / kept;
    }

    /** The layer's rows [from, to) as one tile set and the layer's still set pieces draw them (ARGB, bottom row first). */
    private int[][] composite(LevelData level, BackdropLayer layer, Path folder, String tileSet, long from, long to)
            throws IOException {
        BufferedImage tile = image(folder.resolve(tileSet + ".png"));
        int width = tile.getWidth();
        int height = tile.getHeight();
        int[][] rows = new int[(int) (to - from)][width];
        for (int r = 0; r < rows.length; r++) {
            int y = height - 1 - (int) Math.floorMod(from + r, (long) height);
            for (int x = 0; x < width; x++) {
                rows[r][x] = tile.getRGB(x, y);
            }
        }
        BackdropData backdrop = level.backdrop();
        for (BackdropData.PlacedPiece placed : backdrop.placements()) {
            BackdropData.Piece spec = backdrop.pieces().get(placed.piece());
            if (spec.layer() != layer || placed.path().isPresent() || spec.isTower()) {
                continue;
            }
            BufferedImage piece =
                    image(folder.resolve((spec.imageCount() == 1 ? placed.piece() : placed.piece() + "_0") + ".png"));
            int w = piece.getWidth();
            int h = piece.getHeight();
            long bottom = Math.round(level.pieceCentre(placed) - h / 2.0);
            long left = Math.round(placed.x() - w / 2.0);
            for (int r = 0; r < rows.length; r++) {
                long y = h - 1 - (from + r - bottom);
                if (y < 0 || y >= h) {
                    continue;
                }
                for (int x = 0; x < width; x++) {
                    long column = x - left;
                    if (column >= 0 && column < w) {
                        int c = (int) (placed.mirrored() ? w - 1 - column : column);
                        rows[r][x] = over(piece.getRGB(c, (int) y), rows[r][x]);
                    }
                }
            }
        }
        return rows;
    }

    private static int over(int top, int below) {
        int a = top >>> 24;
        if (a == 0xff || a == 0) {
            return a == 0 ? below : top;
        }
        double t = a / 255.0;
        int out = Math.max(below >>> 24, a) << 24;
        for (int shift = 0; shift < 24; shift += 8) {
            double v = ((top >> shift) & 0xff) * t + ((below >> shift) & 0xff) * (1 - t);
            out |= (int) Math.round(v) << shift;
        }
        return out;
    }

    private static double differing(int[][] a, int[][] b) {
        int count = 0;
        int total = 0;
        for (int r = 0; r < a.length; r++) {
            for (int x = 0; x < a[r].length; x++) {
                total++;
                boolean opaqueA = a[r][x] >>> 24 != 0;
                boolean opaqueB = b[r][x] >>> 24 != 0;
                if (opaqueA != opaqueB
                        || (opaqueA && Math.abs(luminance(a[r][x]) - luminance(b[r][x])) > PIXEL_LIMIT)) {
                    count++;
                }
            }
        }
        return (double) count / total;
    }

    private static double luminance(int argb) {
        return 0.299 * ((argb >> 16) & 0xff) + 0.587 * ((argb >> 8) & 0xff) + 0.114 * (argb & 0xff);
    }

    private static boolean terrain(BackdropLayer layer) {
        return Arrays.asList(TERRAIN).contains(layer);
    }

    private static String tileOn(LevelData level, int section, BackdropLayer layer) {
        for (String id : level.sections().get(section).tiles()) {
            if (level.backdrop().tileSets().get(id).layer() == layer) {
                return id;
            }
        }
        return null;
    }

    /** The levels whose ground is terrain: no deep layer under it. */
    private static Map<String, LevelData> terrainLevels() {
        Content content = ContentLoader.fromClasspath();
        Map<String, LevelData> out = new HashMap<>();
        content.levels().forEach((key, level) -> {
            if (!level.backdrop().hasDeep()) {
                out.put(key, level);
            }
        });
        assertTrue(!out.isEmpty(), "Luna's levels have no deep layer");
        return out;
    }

    private BufferedImage image(Path file) throws IOException {
        BufferedImage image = images.get(file);
        if (image == null) {
            image = ImageIO.read(file.toFile());
            images.put(file, image);
        }
        return image;
    }
}
