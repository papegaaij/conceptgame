package vanguard.pipeline;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Packs the sprite frames in {@code assets/} into the game's texture atlases (build output, never
 * committed): {@code sprites} and {@code backdrop}, both with nearest filtering, since everything is
 * drawn at its native size on whole pixels. Subfolders become region names such as {@code
 * level-01/earth} or {@code hud/well} (a {@code .9.png} is a nine-patch with its splits); the
 * sprites' subfolders (the HUD's parts) share the sprite pages, while each backdrop subfolder (one
 * per level) gets pages of its own. The packed pages are then checked against the production
 * plan's budgets ({@link AtlasBudget}); an exceeded budget fails the build.
 *
 * <p>Usage: {@code AtlasPacker <assetsDir> <outputDir>}
 */
public final class AtlasPacker {
    private AtlasPacker() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AtlasPacker <assetsDir> <outputDir>");
        }
        pack(args[0] + "/sprites", args[1], "sprites", true);
        pack(args[0] + "/backdrop", args[1], "backdrop", false);
        AtlasBudget.enforce(Path.of(args[1]));
    }

    private static void pack(String input, String output, String name, boolean combineSubdirectories) {
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
        settings.combineSubdirectories = combineSubdirectories;
        TexturePacker.process(settings, input, output, name);
    }
}
