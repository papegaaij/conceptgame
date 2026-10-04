package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/**
 * A level without a deep layer (Luna) shows its terrain on the ground and far layers: there a tile
 * set repeats without a seam, and where a section's tile set changes the terrain runs on. Level 04's
 * mare showed a 1 px line where its tiles repeated (a noise octave whose lattice did not divide the
 * tile's height).
 */
class BackdropSeamsTest {
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
        for (BackdropData.PlacedPiece placed : backdrop.placed()) {
            BackdropData.Piece spec = backdrop.pieces().get(placed.piece());
            if (spec.layer() != layer || placed.path().isPresent()) {
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
