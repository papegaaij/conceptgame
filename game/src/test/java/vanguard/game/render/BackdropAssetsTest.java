package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vanguard.content.BackdropData;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;
import vanguard.sim.PlayField;

/** Every level's backdrop finds its images in assets/backdrop, at the sizes its data file gives. */
class BackdropAssetsTest {
    private static final Path BACKDROP = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "backdrop");

    @Test
    void everyTileSetAndSetPieceHasItsImagesAtTheirSize() throws IOException {
        Content content = ContentLoader.fromClasspath();
        for (Map.Entry<String, LevelData> level : content.levels().entrySet()) {
            Path folder = BACKDROP.resolve(Backdrop.folder(level.getKey()));
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

    private static void assertSize(Path file, int width, int height) throws IOException {
        assertTrue(Files.isRegularFile(file), file + " exists");
        BufferedImage image = ImageIO.read(file.toFile());
        assertEquals(width + "x" + height, image.getWidth() + "x" + image.getHeight(), file.toString());
    }
}
