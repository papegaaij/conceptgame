package vanguard.pipeline;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

/**
 * Packs the sprite frames in {@code assets/} into the game's texture atlases (build output, never
 * committed): {@code sprites} and {@code backdrop}, both with nearest filtering, since everything is
 * drawn at its native size on whole pixels. The backdrop's subfolders (one per level) become region
 * names such as {@code level-01/earth}.
 *
 * <p>Usage: {@code AtlasPacker <assetsDir> <outputDir>}
 */
public final class AtlasPacker {
    private AtlasPacker() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AtlasPacker <assetsDir> <outputDir>");
        }
        pack(args[0] + "/sprites", args[1], "sprites");
        pack(args[0] + "/backdrop", args[1], "backdrop");
    }

    private static void pack(String input, String output, String name) {
        var settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = true;
        settings.filterMin = TextureFilter.Nearest;
        settings.filterMag = TextureFilter.Nearest;
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;
        TexturePacker.process(settings, input, output, name);
    }
}
