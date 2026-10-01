package vanguard.tools;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Builds a 768-frame angle set of the Halo Platform ring and packs it into texture atlases.
 *
 * <p>The round-07 concept sheet holds one assembled frame (the 768 frames of the concept were
 * rendered from a 3D model and only shown in the GIF). This cuts that frame out, keys the flat
 * panel background to transparency and rotates it in 768 steps of 0.47 degrees with bicubic
 * filtering: a stand-in with the same frame count and size as the production set.
 *
 * <p>Usage: {@code AngleSetGenerator <sheet.png> <outputDir>}
 */
public final class AngleSetGenerator {
    static final int FRAMES = 768;
    static final int FRAME_SIZE = 224;
    /** Centre and radius of the assembled ring in the sheet (from its gold trim bounds). */
    private static final double CENTRE_X = 161.0;
    private static final double CENTRE_Y = 195.5;
    private static final double RADIUS = 112;
    private static final int PANEL_BACKGROUND = 0x161822;
    private static final int DROP_SHADOW = 0x08090D;
    private static final int SHADOW_ARGB = 0xA0000000;

    private AngleSetGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AngleSetGenerator <sheet.png> <outputDir>");
        }
        BufferedImage cutout = keyBackground(ImageIO.read(Path.of(args[0]).toFile()));
        Path outputDir = Path.of(args[1]);
        Path framesDir = Files.createTempDirectory("halo-frames");
        try {
            for (int i = 0; i < FRAMES; i++) {
                double angle = -2 * Math.PI * i / FRAMES;
                ImageIO.write(rotate(cutout, angle), "png", framesDir.resolve("halo_" + i + ".png").toFile());
            }
            TexturePacker.process(packerSettings(), framesDir.toString(), outputDir.toString(), "halo");
        } finally {
            deleteRecursively(framesDir);
        }
    }

    /** Copies the sheet with the panel background transparent and the drop shadow translucent. */
    private static BufferedImage keyBackground(BufferedImage sheet) {
        var result = new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                int rgb = sheet.getRGB(x, y) & 0xFFFFFF;
                int argb;
                if (colourDistance(rgb, PANEL_BACKGROUND) <= 6) {
                    argb = 0;
                } else if (colourDistance(rgb, DROP_SHADOW) <= 6) {
                    argb = SHADOW_ARGB;
                } else {
                    argb = 0xFF000000 | rgb;
                }
                result.setRGB(x, y, argb);
            }
        }
        return result;
    }

    private static int colourDistance(int a, int b) {
        return Math.abs((a >> 16) - (b >> 16)) + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }

    /** One frame: the ring rotated about its centre, clipped to its circle. */
    private static BufferedImage rotate(BufferedImage cutout, double angle) {
        var frame = new BufferedImage(FRAME_SIZE, FRAME_SIZE, BufferedImage.TYPE_INT_ARGB);
        var g = frame.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setClip(new Ellipse2D.Double(0, 0, 2 * RADIUS, 2 * RADIUS));
        var transform = new AffineTransform();
        transform.translate(FRAME_SIZE / 2.0, FRAME_SIZE / 2.0);
        transform.rotate(angle);
        transform.translate(-CENTRE_X, -CENTRE_Y);
        g.drawImage(cutout, transform, null);
        g.dispose();
        return frame;
    }

    private static TexturePacker.Settings packerSettings() {
        var settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.grid = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = true;
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;
        settings.filterMin = TextureFilter.Nearest;
        settings.filterMag = TextureFilter.Nearest;
        settings.silent = true;
        return settings;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
