package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Every level's backdrop finds its images in assets/backdrop, at the sizes its data file gives, and
 * on the layers that cover the screen no set piece shows an edge until the debrief.
 */
class BackdropAssetsTest {
    private static final Path BACKDROP = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "backdrop");

    @Test
    void everyTileSetAndSetPieceHasItsImagesAtTheirSize() throws IOException {
        Content content = ContentLoader.fromClasspath();
        for (Map.Entry<String, LevelData> level : content.levels().entrySet()) {
            Path folder = BACKDROP.resolve(Backdrop.folder(level.getKey(), level.getValue()));
            BackdropData backdrop = level.getValue().backdrop();
            for (Map.Entry<String, BackdropData.TileSet> tileSet :
                    backdrop.tileSets().entrySet()) {
                assertSize(
                        folder.resolve(tileSet.getKey() + ".png"),
                        PlayField.WIDTH,
                        tileSet.getValue().height());
            }
            for (Map.Entry<String, BackdropData.Piece> piece : backdrop.pieces().entrySet()) {
                BackdropData.Piece spec = piece.getValue();
                int width = (int) spec.size().width();
                int height = (int) spec.size().height();
                int count = spec.imageCount();
                for (int i = 0; i < count; i++) {
                    String file = count == 1 ? piece.getKey() : piece.getKey() + "_" + i;
                    assertSize(folder.resolve(file + ".png"), width, height);
                }
            }
        }
    }

    /**
     * A set piece on an opaque layer lies over the layer's tile set, so wherever one of its edges
     * crosses the screen the piece has to be transparent along it; otherwise the tiles show
     * through as a gap beyond the edge (Level 01's Earth limb in the outro).
     */
    @Test
    void opaqueLayersShowNoSetPieceEdgeUntilTheDebrief() throws IOException {
        Content content = ContentLoader.fromClasspath();
        for (Map.Entry<String, LevelData> entry : content.levels().entrySet()) {
            LevelData level = entry.getValue();
            BackdropData backdrop = level.backdrop();
            Path folder = BACKDROP.resolve(Backdrop.folder(entry.getKey(), entry.getValue()));
            for (BackdropData.PlacedPiece placed : backdrop.placed()) {
                BackdropData.Piece spec = backdrop.pieces().get(placed.piece());
                if (spec.layer() == backdrop.base()) {
                    for (BufferedImage image : images(folder, placed.piece(), spec)) {
                        assertNoEdgeOnScreen(level, placed, spec, image);
                    }
                }
            }
        }
    }

    /**
     * A level without a deep layer (Level 04's Luna, a top-down surface) has nothing behind its
     * ground: each section's ground tile set is opaque, or where it is not (the rille's channel) the
     * section's far tile set, opaque everywhere, shows through. The set pieces on the ground then
     * follow the edge rule above, as the deep layer's do.
     */
    @Test
    void withoutADeepLayerTheGroundTilesCoverTheScreen() throws IOException {
        Content content = ContentLoader.fromClasspath();
        for (Map.Entry<String, LevelData> entry : content.levels().entrySet()) {
            BackdropData backdrop = entry.getValue().backdrop();
            if (backdrop.hasDeep()) {
                continue;
            }
            Path folder = BACKDROP.resolve(Backdrop.folder(entry.getKey(), entry.getValue()));
            for (LevelData.Section section : entry.getValue().sections()) {
                String ground = tileOn(backdrop, section, BackdropLayer.GROUND);
                String far = tileOn(backdrop, section, BackdropLayer.FAR);
                assertTrue(ground != null, section.name() + ": no ground tile set");
                if (opaque(ImageIO.read(folder.resolve(ground + ".png").toFile()))) {
                    continue;
                }
                assertTrue(
                        far != null
                                && opaque(ImageIO.read(
                                        folder.resolve(far + ".png").toFile())),
                        section.name() + ": " + ground + " has openings but no opaque far tile set lies under them");
            }
        }
    }

    private static String tileOn(BackdropData backdrop, LevelData.Section section, BackdropLayer layer) {
        for (String id : section.tiles()) {
            if (backdrop.tileSets().get(id).layer() == layer) {
                return id;
            }
        }
        return null;
    }

    private static boolean opaque(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 != 0xff) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void assertNoEdgeOnScreen(
            LevelData level, BackdropData.PlacedPiece placed, BackdropData.Piece spec, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long bottom = Math.round(level.pieceCentre(placed) - height / 2.0);
        double factor = level.backdrop().factor(spec.layer());
        for (int step = 0; step <= SimStep.ticks(level.outroEnd()); step++) {
            double t = step * SimStep.SECONDS;
            // As Backdrop draws it: whole pixels, y up from the play field's bottom edge.
            long y = bottom - Math.round(level.scrollAt(t) * factor) + Math.round(placed.offsetY(t));
            long x = Math.round(placed.x() + placed.offsetX(t) - width / 2.0);
            List<String> edges = new ArrayList<>();
            // The image columns on screen, reflected for a mirrored piece.
            long from = Math.max(0, x) - x;
            long to = Math.min(PlayField.WIDTH, x + width) - x;
            if (placed.mirrored()) {
                long reflected = width - to;
                to = width - from;
                from = reflected;
            }
            if (inside(y, PlayField.HEIGHT) && opaqueRow(image, height - 1, from, to)) {
                edges.add("bottom");
            }
            if (inside(y + height, PlayField.HEIGHT) && opaqueRow(image, 0, from, to)) {
                edges.add("top");
            }
            int low = (int) Math.max(0, y);
            int high = (int) Math.min(PlayField.HEIGHT, y + height);
            int first = placed.mirrored() ? width - 1 : 0;
            if (inside(x, PlayField.WIDTH) && opaqueColumn(image, first, y + height - high, y + height - low)) {
                edges.add("left");
            }
            if (inside(x + width, PlayField.WIDTH)
                    && opaqueColumn(image, width - 1 - first, y + height - high, y + height - low)) {
                edges.add("right");
            }
            if (!edges.isEmpty()) {
                fail(String.format(
                        Locale.ROOT,
                        "%s (t=%s): its %s edge is not transparent but on screen at t=%.2f",
                        placed.piece(),
                        placed.t(),
                        String.join(" and ", edges),
                        t));
            }
        }
    }

    private static boolean inside(long edge, int size) {
        return edge > 0 && edge < size;
    }

    private static boolean opaqueRow(BufferedImage image, int row, long from, long to) {
        for (long column = from; column < to; column++) {
            if (alpha(image, (int) column, row) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean opaqueColumn(BufferedImage image, int column, long from, long to) {
        for (long row = from; row < to; row++) {
            if (alpha(image, column, (int) row) > 0) {
                return true;
            }
        }
        return false;
    }

    private static int alpha(BufferedImage image, int column, int row) {
        return image.getRGB(column, row) >>> 24;
    }

    private static List<BufferedImage> images(Path folder, String id, BackdropData.Piece spec) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        int count = spec.imageCount();
        for (int i = 0; i < count; i++) {
            images.add(ImageIO.read(
                    folder.resolve((count == 1 ? id : id + "_" + i) + ".png").toFile()));
        }
        return images;
    }

    private static void assertSize(Path file, int width, int height) throws IOException {
        assertTrue(Files.isRegularFile(file), file + " exists");
        BufferedImage image = ImageIO.read(file.toFile());
        assertEquals(width + "x" + height, image.getWidth() + "x" + image.getHeight(), file.toString());
    }
}
