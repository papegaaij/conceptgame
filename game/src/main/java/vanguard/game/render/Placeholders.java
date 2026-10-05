package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder frames for units whose art is not made yet (M4 part F made them for the Mantis and the
 * Coilwyrm's head, segments, tail and regrown head, which now have production sprites, so these
 * only stand in when a sprite is missing): simple shapes in the concept's colours, drawn once per
 * run and kept, an angle set with a nose mark where the unit turns.
 */
final class Placeholders {
    private static final Map<String, Array<AtlasRegion>> MADE = new HashMap<>();

    private Placeholders() {}

    /** The frames for {@code slug}: {@code headings} turned copies (heading 0 facing down the screen). */
    static synchronized Array<AtlasRegion> frames(String slug, int headings) {
        return MADE.computeIfAbsent(slug + "/" + headings, key -> make(slug, headings));
    }

    private static Array<AtlasRegion> make(String slug, int headings) {
        Look look = look(slug);
        Array<AtlasRegion> frames = new Array<>(headings);
        for (int h = 0; h < headings; h++) {
            double facing = 2 * Math.PI * h / headings;
            Pixmap pixmap = new Pixmap(look.size, look.size, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            int c = look.size / 2;
            pixmap.setColor(look.rim);
            fillEllipse(pixmap, c, c, look.width / 2, look.height / 2);
            pixmap.setColor(look.body);
            fillEllipse(pixmap, c, c, look.width / 2 - 2, look.height / 2 - 2);
            if (look.nose != null) {
                // Facing down the screen: the nose at the bottom of the frame (pixmap rows run down).
                int nx = c - (int) Math.round(Math.sin(facing) * look.width * 0.3);
                int ny = c + (int) Math.round(Math.cos(facing) * look.height * 0.3);
                pixmap.setColor(look.nose);
                pixmap.fillCircle(nx, ny, Math.max(2, look.width / 7));
            }
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            frames.add(new AtlasRegion(texture, 0, 0, look.size, look.size));
        }
        return frames;
    }

    private static void fillEllipse(Pixmap pixmap, int cx, int cy, int rx, int ry) {
        for (int y = -ry; y <= ry; y++) {
            for (int x = -rx; x <= rx; x++) {
                if ((double) x * x / (rx * rx) + (double) y * y / (ry * ry) <= 1) {
                    pixmap.drawPixel(cx + x, cy + y);
                }
            }
        }
    }

    private record Look(int size, int width, int height, Color rim, Color body, Color nose) {}

    private static Look look(String slug) {
        Color rust = Color.valueOf("8A4A2A");
        Color dark = Color.valueOf("3A2016");
        Color teal = Color.valueOf("3FE0C8");
        return switch (slug) {
            case "mantis" ->
                new Look(80, 40, 60, Color.valueOf("8C8778"), Color.valueOf("E8E2CF"), Color.valueOf("D0203A"));
            case "coilwyrm" -> new Look(58, 46, 46, dark, rust, teal);
            case "coilwyrm-regrown" -> new Look(58, 44, 44, dark, Color.valueOf("B0623A"), teal);
            case "coilwyrm-segment" -> new Look(54, 44, 40, dark, rust, null);
            case "coilwyrm-tail" -> new Look(40, 26, 32, dark, Color.valueOf("6E3A22"), Color.valueOf("2A9C8C"));
            default -> new Look(32, 28, 28, Color.DARK_GRAY, Color.MAGENTA, Color.WHITE);
        };
    }
}
