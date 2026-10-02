package vanguard.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The atlas budgets of the production art plan (design/art-direction/production, Budgets), checked
 * on the packed atlases at RGBA8: the {@code sprites} atlas is always loaded and counts as the
 * shared pages (Level 01's enemies and loot targets are in it too until levels get their own sprite
 * atlas, which is stricter); a level's pages are the {@code backdrop} pages that hold one of its
 * regions ({@code level-NN/...}); one sprite (all frames of one region name) must fit a page.
 */
public final class AtlasBudget {
    static final long PAGE_BYTES = 2048L * 2048 * 4;
    static final int SHARED_PAGES = 2;
    static final int LEVEL_PAGES = 6;

    /** A page of a packed atlas: its size and the names of the regions on it, with their bytes. */
    record Page(String file, int width, int height, Map<String, Long> regionBytes) {
        long bytes() {
            return (long) width * height * 4;
        }
    }

    private AtlasBudget() {}

    /** Fails with every exceeded budget listed. */
    static void enforce(Path atlasDir) throws IOException {
        List<String> violations = violations(atlasDir);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "atlas budget exceeded (design/art-direction/production):\n  " + String.join("\n  ", violations));
        }
    }

    static List<String> violations(Path atlasDir) throws IOException {
        List<String> violations = new ArrayList<>();
        List<Page> sprites = read(atlasDir.resolve("sprites.atlas"));
        check(violations, "shared pages (sprites atlas)", sprites, SHARED_PAGES);
        Map<String, Long> spriteBytes = new TreeMap<>();
        for (Page page : sprites) {
            page.regionBytes().forEach((name, bytes) -> spriteBytes.merge(name, bytes, Long::sum));
        }
        spriteBytes.forEach((name, bytes) -> {
            if (bytes > PAGE_BYTES) {
                violations.add("sprite '" + name + "': " + mib(bytes) + " MiB of frames, more than one page ("
                        + mib(PAGE_BYTES) + " MiB)");
            }
        });
        List<Page> backdrop = read(atlasDir.resolve("backdrop.atlas"));
        Map<String, List<Page>> levels = new TreeMap<>();
        for (Page page : backdrop) {
            for (String level : levelsOn(page)) {
                levels.computeIfAbsent(level, key -> new ArrayList<>()).add(page);
            }
        }
        levels.forEach((level, pages) -> check(violations, "level " + level, pages, LEVEL_PAGES));
        return violations;
    }

    private static void check(List<String> violations, String what, List<Page> pages, int maxPages) {
        long bytes = pages.stream().mapToLong(Page::bytes).sum();
        if (pages.size() > maxPages || bytes > maxPages * PAGE_BYTES) {
            violations.add(what + ": " + pages.size() + " pages, " + mib(bytes) + " MiB (budget " + maxPages
                    + " pages, " + mib(maxPages * PAGE_BYTES) + " MiB)");
        }
    }

    private static TreeSet<String> levelsOn(Page page) {
        TreeSet<String> levels = new TreeSet<>();
        for (String name : page.regionBytes().keySet()) {
            int slash = name.indexOf('/');
            if (slash > 0) {
                levels.add(name.substring(0, slash));
            }
        }
        return levels;
    }

    private static long mib(long bytes) {
        return Math.round(bytes / (1024.0 * 1024.0));
    }

    /** The pages of a libGDX atlas file: a page starts after a blank line, regions are indented. */
    static List<Page> read(Path atlas) throws IOException {
        List<Page> pages = new ArrayList<>();
        String file = null;
        int width = 0;
        int height = 0;
        Map<String, Long> regions = new LinkedHashMap<>();
        String region = null;
        boolean pageStart = true;
        for (String line : Files.readAllLines(atlas)) {
            if (line.isBlank()) {
                pageStart = true;
            } else if (pageStart) {
                if (file != null) {
                    pages.add(new Page(file, width, height, regions));
                }
                file = line.strip();
                regions = new LinkedHashMap<>();
                region = null;
                pageStart = false;
            } else if (Character.isWhitespace(line.charAt(0))) {
                int[] size = size(line);
                if (size != null && region != null) {
                    regions.merge(region, (long) size[0] * size[1] * 4, Long::sum);
                }
            } else if (line.contains(":")) {
                int[] size = size(line);
                if (size != null) {
                    width = size[0];
                    height = size[1];
                }
            } else {
                region = line.strip();
            }
        }
        if (file != null) {
            pages.add(new Page(file, width, height, regions));
        }
        return pages;
    }

    private static int[] size(String line) {
        String stripped = line.strip();
        if (!stripped.startsWith("size:")) {
            return null;
        }
        String[] parts = stripped.substring("size:".length()).split(",");
        return new int[] {Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip())};
    }
}
