package vanguard.pipeline;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import vanguard.content.ContentLoader;

/**
 * Packs the sprite frames in {@code assets/} into the game's texture atlases (build output, never
 * committed), all with nearest filtering, since everything is drawn at its native size on whole
 * pixels: the shared {@code sprites} atlas with the game-wide sprites, one unit atlas {@code
 * level-NN} per level with every sprite that level uses (a unit of several levels is copied into each
 * of their atlases; {@link SpriteUse} decides from the levels' data) and {@code backdrop}.
 * Subfolders become region names such as {@code level-01/earth} or {@code hud/well} (a {@code
 * .9.png} is a nine-patch with its splits); the sprites' subfolders (the HUD, the UI kit, the
 * hangar's icons and intel portraits, the speakers' portraits) are shared and share the sprite
 * pages, while each backdrop subfolder (one per level) gets pages of its own. The packed pages are
 * then checked against the production plan's budgets ({@link AtlasBudget}); an exceeded budget
 * fails the build.
 *
 * <p>Usage: {@code AtlasPacker <assetsDir> <outputDir>}
 */
public final class AtlasPacker {
    /** A frame's sprite name: {@code skitter_3.png} and {@code skitter.png} are {@code skitter}. */
    private static final Pattern FRAME = Pattern.compile("(_\\d+)?(\\.9)?\\.png$");

    private AtlasPacker() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AtlasPacker <assetsDir> <outputDir>");
        }
        Path sprites = Path.of(args[0], "sprites");
        Path output = Path.of(args[1]);
        Path staging = output.resolveSibling(output.getFileName() + "-input");
        clear(output);
        clear(staging);
        stage(sprites, staging);
        try (Stream<Path> atlases = Files.list(staging)) {
            for (Path atlas : atlases.sorted().toList()) {
                pack(atlas.toString(), args[1], atlas.getFileName().toString(), true);
            }
        }
        pack(args[0] + "/backdrop", args[1], "backdrop", false);
        AtlasBudget.report(output).forEach(System.out::println);
        AtlasBudget.enforce(output);
    }

    /**
     * Copies the sprite frames into one input folder per atlas: the subfolders and every sprite
     * {@link SpriteUse} shares into {@code sprites}, the sprites levels use into the {@code
     * level-NN} of each of those levels.
     */
    private static void stage(Path sprites, Path staging) throws IOException {
        List<Path> frames;
        try (Stream<Path> files = Files.list(sprites)) {
            frames = files.sorted().toList();
        }
        List<String> names = frames.stream()
                .filter(Files::isRegularFile)
                .map(frame -> FRAME.matcher(frame.getFileName().toString()).replaceFirst(""))
                .distinct()
                .toList();
        Map<String, Set<String>> atlases = SpriteUse.atlases(ContentLoader.fromClasspath(), names);
        for (Path frame : frames) {
            String file = frame.getFileName().toString();
            if (Files.isDirectory(frame)) {
                copyTree(frame, staging.resolve(SpriteUse.SHARED).resolve(file));
            } else if (file.endsWith(".png")) {
                for (String atlas : atlases.get(FRAME.matcher(file).replaceFirst(""))) {
                    Path target = staging.resolve(atlas);
                    Files.createDirectories(target);
                    Files.copy(frame, target.resolve(file));
                }
            }
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path file : files.toList()) {
                Path target = to.resolve(from.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
    }

    /** Deletes a folder's contents, so no atlas of a former split stays behind. */
    private static void clear(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder())
                    .filter(file -> !file.equals(dir))
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
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
