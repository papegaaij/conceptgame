package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import vanguard.sim.Layer;

/**
 * Placeholder art generated at start-up from a fixed seed: one sprite sheet (so all small
 * sprites batch into one draw call) and one tiling texture per background layer. Grey shapes
 * are tinted per layer when drawn.
 */
public final class ProceduralArt implements Disposable {
    private static final int SHEET = 256;
    private static final int TILE = 256;

    private final Texture sheet;
    private final Map<Layer, Texture> layerTiles = new EnumMap<>(Layer.class);
    public final TextureRegion pixel;
    public final TextureRegion[] enemies = new TextureRegion[3];
    public final TextureRegion player;
    public final TextureRegion bulletCore;
    public final TextureRegion glow;
    public final TextureRegion spark;

    public ProceduralArt() {
        Pixmap pixmap = new Pixmap(SHEET, SHEET, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(0, 0, 4, 4);
        int[] sizes = {20, 32, 48};
        int x = 8;
        for (int size : sizes) {
            drawOrb(pixmap, x, 8, size);
            x += size + 8;
        }
        drawShip(pixmap, 140, 8, 32);
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(180, 8, 4, 12);
        drawRadial(pixmap, 192, 8, 32, 2.0f);
        drawRadial(pixmap, 232, 8, 12, 1.5f);
        sheet = new Texture(pixmap);
        sheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pixmap.dispose();

        pixel = new TextureRegion(sheet, 1, 1, 2, 2);
        x = 8;
        for (int i = 0; i < sizes.length; i++) {
            enemies[i] = new TextureRegion(sheet, x, 8, sizes[i], sizes[i]);
            x += sizes[i] + 8;
        }
        player = new TextureRegion(sheet, 140, 8, 32, 32);
        bulletCore = new TextureRegion(sheet, 180, 8, 4, 12);
        glow = new TextureRegion(sheet, 192, 8, 32, 32);
        spark = new TextureRegion(sheet, 232, 8, 12, 12);

        RandomXS128 random = new RandomXS128(2185);
        for (Layer layer : Layer.values()) {
            if (layer != Layer.AIR) {
                layerTiles.put(layer, layerTile(layer, random));
            }
        }
    }

    /** The tiling texture of a background layer; none for the play plane. */
    public Texture tile(Layer layer) {
        return layerTiles.get(layer);
    }

    private static void drawOrb(Pixmap pixmap, int x, int y, int size) {
        int r = size / 2;
        pixmap.setColor(0.35f, 0.35f, 0.35f, 1);
        pixmap.fillCircle(x + r, y + r, r - 1);
        pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
        pixmap.fillCircle(x + r, y + r, r * 2 / 3);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(x + r - r / 3, y + r - r / 3, Math.max(1, r / 4));
    }

    private static void drawShip(Pixmap pixmap, int x, int y, int size) {
        pixmap.setColor(0.85f, 0.9f, 1, 1);
        pixmap.fillTriangle(x + size / 2, y, x, y + size - 4, x + size, y + size - 4);
        pixmap.setColor(0.3f, 0.6f, 1, 1);
        pixmap.fillRectangle(x + size / 2 - 3, y + size / 3, 6, size / 2);
    }

    /** A soft radial falloff in white, for additive glows. */
    private static void drawRadial(Pixmap pixmap, int x, int y, int size, float falloff) {
        float r = size / 2f;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float d = (float) Math.hypot(px + 0.5f - r, py + 0.5f - r) / r;
                float a = d >= 1 ? 0 : (float) Math.pow(1 - d, falloff);
                pixmap.drawPixel(x + px, y + py, Color.rgba8888(1, 1, 1, a));
            }
        }
    }

    private static Texture layerTile(Layer layer, RandomXS128 random) {
        Pixmap pixmap = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        switch (layer) {
            case DEEP -> {
                pixmap.setColor(0.03f, 0.04f, 0.09f, 1);
                pixmap.fill();
                scatter(pixmap, random, 160, 1, 2, 0.5f, 0.55f, 0.7f, 1);
            }
            case FAR -> scatter(pixmap, random, 14, 12, 40, 0.12f, 0.13f, 0.2f, 0.85f);
            case GROUND -> scatter(pixmap, random, 22, 24, 70, 0.22f, 0.2f, 0.26f, 1);
            case SUB -> scatter(pixmap, random, 30, 6, 30, 0.1f, 0.4f, 0.45f, 0.25f);
            case LOW_AIR -> scatter(pixmap, random, 10, 20, 60, 0.6f, 0.6f, 0.62f, 0.4f);
            case HIGH_AIR -> scatter(pixmap, random, 8, 4, 90, 0.85f, 0.85f, 0.9f, 0.3f);
            case AIR -> throw new IllegalArgumentException("the play plane has no tile");
        }
        Texture texture = new Texture(pixmap);
        texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static void scatter(Pixmap pixmap, RandomXS128 random, int count, int minSize, int maxSize,
                                float r, float g, float b, float a) {
        for (int i = 0; i < count; i++) {
            float shade = 0.75f + random.nextFloat() * 0.5f;
            pixmap.setColor(Math.min(1, r * shade), Math.min(1, g * shade), Math.min(1, b * shade), a);
            int w = minSize + random.nextInt(maxSize - minSize + 1);
            int h = minSize + random.nextInt(maxSize - minSize + 1);
            int x = random.nextInt(TILE - w + 1);
            int y = random.nextInt(TILE - h + 1);
            pixmap.fillRectangle(x, y, w, h);
        }
    }

    @Override
    public void dispose() {
        sheet.dispose();
        layerTiles.values().forEach(Texture::dispose);
    }
}
