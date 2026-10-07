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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The atlas budgets of the production art plan (design/art-direction/production, Budgets), checked
 * on the packed atlases at RGBA8: the {@code sprites} atlas is always loaded and counts as the
 * shared pages; a level's pages are its unit atlas ({@code level-NN.atlas}, loaded while the level
 * runs) and the {@code backdrop} pages that hold one of its regions ({@code level-NN/...}); one
 * sprite (all frames of one region name in one atlas; a unit several levels use is in each of
 * their atlases) must fit a page.
 */
public final class AtlasBudget {
    static final long PAGE_BYTES = 2048L * 2048 * 4;
    static final int SHARED_PAGES = 2;
    static final int LEVEL_PAGES = 6;
    /** A level's unit atlas: {@code level-NN.atlas}. */
    private static final Pattern UNITS = Pattern.compile("(level-\\d{2})\\.atlas");

    /**
     * A page of a packed atlas: its size, the names of the regions on it, with their bytes, and the
     * bytes its packed rectangles cover (identical frames share one).
     */
    record Page(String file, int width, int height, Map<String, Long> regionBytes, long usedBytes) {
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
        Map<String, List<Page>> units = units(atlasDir);
        // A sprite's frames within one atlas; a unit copied into several levels' atlases counts once.
        Map<String, Long> spriteBytes = new TreeMap<>();
        Stream.concat(Stream.of(sprites), units.values().stream()).forEach(atlas -> {
            Map<String, Long> inAtlas = new TreeMap<>();
            atlas.forEach(page -> page.regionBytes().forEach((name, bytes) -> inAtlas.merge(name, bytes, Long::sum)));
            inAtlas.forEach((name, bytes) -> spriteBytes.merge(name, bytes, Math::max));
        });
        spriteBytes.forEach((name, bytes) -> {
            if (bytes > PAGE_BYTES) {
                violations.add("sprite '" + name + "': " + mib(bytes) + " MiB of frames, more than one page ("
                        + mib(PAGE_BYTES) + " MiB)");
            }
        });
        levels(atlasDir, units).forEach((level, pages) -> check(violations, "level " + level, pages, LEVEL_PAGES));
        return violations;
    }

    /** One line per atlas and level: its pages, their size and how full they are. */
    static List<String> report(Path atlasDir) throws IOException {
        List<String> lines = new ArrayList<>();
        Map<String, List<Page>> atlases = new TreeMap<>();
        atlases.put("sprites (shared)", read(atlasDir.resolve("sprites.atlas")));
        Map<String, List<Page>> units = units(atlasDir);
        units.forEach((level, pages) -> atlases.put(level + " units", pages));
        atlases.put("backdrop", read(atlasDir.resolve("backdrop.atlas")));
        atlases.forEach((name, pages) -> lines.add(name + ": " + describe(pages)));
        levels(atlasDir, units)
                .forEach((level, pages) -> lines.add(
                        "level " + level + " (units + backdrop): " + pages.size() + "/" + LEVEL_PAGES + " pages"));
        return lines;
    }

    private static String describe(List<Page> pages) {
        List<String> fills = new ArrayList<>();
        for (Page page : pages) {
            fills.add(page.file() + " " + page.width() + "x" + page.height() + " "
                    + Math.round(100.0 * page.usedBytes() / page.bytes()) + " % full");
        }
        return pages.size() + " pages (" + String.join(", ", fills) + ")";
    }

    /** The unit atlases by level ({@code level-NN}). */
    private static Map<String, List<Page>> units(Path atlasDir) throws IOException {
        Map<String, List<Page>> units = new TreeMap<>();
        try (Stream<Path> files = Files.list(atlasDir)) {
            for (Path file : files.toList()) {
                Matcher unit = UNITS.matcher(file.getFileName().toString());
                if (unit.matches()) {
                    units.put(unit.group(1), read(file));
                }
            }
        }
        return units;
    }

    /** A level's pages: its unit atlas and the backdrop pages that hold one of its regions. */
    private static Map<String, List<Page>> levels(Path atlasDir, Map<String, List<Page>> units) throws IOException {
        Map<String, List<Page>> levels = new TreeMap<>();
        units.forEach((level, pages) ->
                levels.computeIfAbsent(level, key -> new ArrayList<>()).addAll(pages));
        for (Page page : read(atlasDir.resolve("backdrop.atlas"))) {
            for (String level : levelsOn(page)) {
                levels.computeIfAbsent(level, key -> new ArrayList<>()).add(page);
            }
        }
        return levels;
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
        Map<String, Long> rectangles = new LinkedHashMap<>();
        String region = null;
        String xy = null;
        boolean pageStart = true;
        for (String line : Files.readAllLines(atlas)) {
            if (line.isBlank()) {
                pageStart = true;
            } else if (pageStart) {
                if (file != null) {
                    pages.add(new Page(file, width, height, regions, used(rectangles)));
                }
                file = line.strip();
                regions = new LinkedHashMap<>();
                rectangles = new LinkedHashMap<>();
                region = null;
                pageStart = false;
            } else if (Character.isWhitespace(line.charAt(0))) {
                int[] size = size(line);
                if (line.strip().startsWith("xy:")) {
                    xy = line.strip();
                }
                if (size != null && region != null) {
                    regions.merge(region, (long) size[0] * size[1] * 4, Long::sum);
                    rectangles.put(xy, (long) size[0] * size[1] * 4);
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
            pages.add(new Page(file, width, height, regions, used(rectangles)));
        }
        return pages;
    }

    private static long used(Map<String, Long> rectangles) {
        return rectangles.values().stream().mapToLong(Long::longValue).sum();
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
