package vanguard.pipeline;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

/**
 * Packs the sprite frames in {@code assets/} into the game's texture atlases (build output, never
 * committed): {@code sprites} with nearest filtering for the pixel-exact sprites, {@code backdrop}
 * with linear filtering for the parallax layers, which the M1 placeholders draw enlarged.
 *
 * <p>Usage: {@code AtlasPacker <assetsDir> <outputDir>}
 */
public final class AtlasPacker {
    private AtlasPacker() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AtlasPacker <assetsDir> <outputDir>");
        }
        pack(args[0] + "/sprites", args[1], "sprites", TextureFilter.Nearest);
        pack(args[0] + "/backdrop", args[1], "backdrop", TextureFilter.Linear);
    }

    private static void pack(String input, String output, String name, TextureFilter filter) {
        var settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = true;
        settings.filterMin = filter;
        settings.filterMag = filter;
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;
        TexturePacker.process(settings, input, output, name);
    }
}
