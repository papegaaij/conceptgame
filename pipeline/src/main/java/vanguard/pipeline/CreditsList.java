package vanguard.pipeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes the credits roll the game scrolls (design/ui/credits) from {@code CREDITS.md}: the game's
 * own credits around the third-party assets that the build ships, grouped by kind. Every shipped
 * asset whose licence asks for attribution (CC-BY, and the fonts' permissive licences) is listed
 * with its title, author, licence and source; the authors of shipped CC0 and public-domain assets
 * are thanked by name. A row counts as shipped by its path: a file under {@code assets/} that
 * exists, a concept file whose copy (same name) is in {@code assets/}, or a voice reference clip
 * whose speaker has rendered lines in {@code assets/voice/}; a path in a {@code rejected/}
 * directory never counts.
 *
 * <p>The roll is a line file, one {@code kind|text} entry per line ({@code #} starts a comment),
 * read by {@code vanguard.game.ui.CreditsRoll}; the screen wraps the texts to its column.
 *
 * <p>Usage: {@code CreditsList <repository root> <output file>}.
 */
public final class CreditsList {
    /** Where the roll goes, relative to the repository root; the game loads {@code ui/credits.txt}. */
    public static final String OUTPUT = "assets/ui/credits.txt";

    private static final Pattern PATH = Pattern.compile("(?:assets|design)/[^\\s,()`]+");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)]*)\\)");
    private static final Pattern VOICE_REF = Pattern.compile("design/audio/voice/refs/ref-([a-z0-9-]+)\\.wav");

    /** The groups of the roll, in their order. */
    enum Group {
        SOUND("SOUND EFFECTS"),
        VOICE("VOICES"),
        MUSIC("MUSIC"),
        FONT("FONTS"),
        OTHER("OTHER ASSETS");

        final String heading;

        Group(String heading) {
            this.heading = heading;
        }
    }

    /** A row of CREDITS.md's table: the paths of its File cell and the link texts and targets. */
    record Row(int line, List<String> paths, String title, String author, String source, String licence) {
        /** CC0 and public-domain assets need no attribution; everything else does (CC-BY, font licences). */
        boolean needsAttribution() {
            String lower = licence.toLowerCase(Locale.ROOT);
            return !(lower.startsWith("cc0") || lower.startsWith("public domain"));
        }
    }

    /** One attribution of the roll. */
    record Credit(Group group, String title, String author, String licence, String source) {}

    /** What the roll lists: the attributions in CREDITS.md order and the thanked authors per group. */
    record Listing(List<Credit> credits, Map<Group, Set<String>> thanks) {}

    private static final String HEAD = """
            logo|
            gap|
            role|A GAME BY
            name|Emond Papegaaij
            gap|
            role|ART
            text|Pre-rendered sprites, scenes and the logo made by the game's own generators
            gap|
            role|MUSIC
            text|Original score, synthesised by the game's own generators
            gap|
            """;

    private static final Map<Group, String> INTRO = Map.of(
            Group.VOICE,
            "Spoken by the Chatterbox text-to-speech engine (Resemble AI, MIT licence) in voices cloned from"
                    + " LibriVox recordings");

    private static final String TAIL = """
            title|TOOLS
            text|libGDX and LWJGL, Java 21, Gradle and Construo
            text|Python with NumPy and Pillow, FFmpeg
            text|Chatterbox text-to-speech by Resemble AI
            text|Written with Claude Code by Anthropic
            gap|
            title|LICENCE
            text|Terran Vanguard is open source under the Apache License 2.0
            text|Third-party assets keep their own licences, listed in CREDITS.md
            gap|
            role|MADE WITH LIBGDX
            gap|
            gap|
            name|THANK YOU FOR PLAYING
            """;

    private CreditsList() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: CreditsList <repository root> <output file>");
        }
        Path root = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Listing listing = listing(parse(readText(root.resolve("CREDITS.md"))), root);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, render(listing), StandardCharsets.UTF_8);
        System.out.println(listing.credits().size() + " attributions, "
                + listing.thanks().values().stream().mapToInt(Set::size).sum() + " thanked authors -> " + out);
    }

    /** The roll for the repository at {@code root}, as {@link #main} writes it. */
    static String render(Path root) throws IOException {
        return render(listing(parse(readText(root.resolve("CREDITS.md"))), root));
    }

    /**
     * A text file with its line ends as {@code \n}: a Windows checkout (Git's {@code core.autocrlf})
     * gives CREDITS.md and the committed roll {@code \r\n}, while the roll is always written with
     * {@code \n}.
     */
    static String readText(Path file) throws IOException {
        return normalise(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** {@code text} with every {@code \r\n} and lone {@code \r} line end as {@code \n}. */
    static String normalise(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** The rows of the asset table; a row that is not six cells or misses a link fails loudly. */
    static List<Row> parse(String markdown) {
        List<Row> rows = new ArrayList<>();
        String[] lines = normalise(markdown).split("\n", -1);
        boolean table = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (!line.startsWith("|")) {
                table = false;
                continue;
            }
            if (line.startsWith("| File |")) {
                table = true;
                continue;
            }
            if (!table || line.startsWith("|---")) {
                continue;
            }
            List<String> cells = cells(line);
            if (cells.size() != 6) {
                throw new IllegalArgumentException(
                        "CREDITS.md line " + (i + 1) + ": " + cells.size() + " cells instead of 6: " + line);
            }
            List<String> paths = new ArrayList<>();
            Matcher path = PATH.matcher(cells.get(0));
            while (path.find()) {
                paths.add(path.group());
            }
            if (paths.isEmpty()) {
                throw new IllegalArgumentException("CREDITS.md line " + (i + 1) + ": no file path: " + line);
            }
            rows.add(new Row(
                    i + 1,
                    List.copyOf(paths),
                    cells.get(1).strip(),
                    cells.get(2).strip(),
                    link(cells.get(3), true, i + 1),
                    link(cells.get(4), false, i + 1)));
        }
        return rows;
    }

    private static List<String> cells(String line) {
        String inner = line.substring(1, line.endsWith("|") ? line.length() - 1 : line.length());
        List<String> cells = new ArrayList<>();
        for (String cell : inner.split("\\|", -1)) {
            cells.add(cell.strip());
        }
        return cells;
    }

    /** A cell's first link: its target without the scheme and trailing slash, or its text. */
    private static String link(String cell, boolean target, int line) {
        Matcher link = LINK.matcher(cell);
        if (!link.find()) {
            throw new IllegalArgumentException("CREDITS.md line " + line + ": no link in '" + cell + "'");
        }
        if (!target) {
            return link.group(1).strip();
        }
        return link.group(2).strip().replaceFirst("^https?://", "").replaceFirst("/$", "");
    }

    /** The attributions and thanks for the rows that the assets under {@code root} ship. */
    static Listing listing(List<Row> rows, Path root) throws IOException {
        Set<String> assetNames = assetNames(root.resolve("assets"));
        Set<Credit> credits = new LinkedHashSet<>();
        Map<Group, Set<String>> thanks = new EnumMap<>(Group.class);
        for (Row row : rows) {
            List<String> shipped = row.paths().stream()
                    .filter(path -> shipped(path, root, assetNames))
                    .toList();
            if (shipped.isEmpty()) {
                continue;
            }
            Group group = group(shipped.getFirst());
            if (row.needsAttribution()) {
                credits.add(new Credit(group, row.title(), row.author(), row.licence(), row.source()));
            } else {
                thanks.computeIfAbsent(
                                group,
                                g -> new TreeSet<>(Comparator.comparing((String name) -> name.toLowerCase(Locale.ROOT))
                                        .thenComparing(Comparator.naturalOrder())))
                        .add(name(row.author()));
            }
        }
        return new Listing(List.copyOf(credits), thanks);
    }

    /** Whether the build ships the file at {@code path} (see the class comment). */
    static boolean shipped(String path, Path root, Set<String> assetNames) {
        if (path.contains("/rejected/")) {
            return false;
        }
        if (path.startsWith("assets/")) {
            return Files.isRegularFile(root.resolve(path));
        }
        Matcher ref = VOICE_REF.matcher(path);
        if (ref.matches()) {
            Path lines = root.resolve("assets/voice").resolve(ref.group(1));
            if (!Files.isDirectory(lines)) {
                return false;
            }
            try (Stream<Path> files = Files.list(lines)) {
                return files.findAny().isPresent();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return assetNames.contains(path.substring(path.lastIndexOf('/') + 1));
    }

    private static Set<String> assetNames(Path assets) throws IOException {
        if (!Files.isDirectory(assets)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(assets)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }

    static Group group(String path) {
        if (path.startsWith("assets/sfx/") || path.startsWith("design/audio/sfx/")) {
            return Group.SOUND;
        }
        if (path.startsWith("assets/voice/") || path.startsWith("design/audio/voice/")) {
            return Group.VOICE;
        }
        if (path.startsWith("assets/music/") || path.startsWith("design/audio/music/")) {
            return Group.MUSIC;
        }
        if (path.startsWith("assets/fonts/")) {
            return Group.FONT;
        }
        return Group.OTHER;
    }

    /** An author's name for the thanks: without the parenthesised note ("(LibriVox reader)"). */
    private static String name(String author) {
        return author.replaceAll("\\s*\\([^)]*\\)", "").strip();
    }

    /** The roll file: the head, each group with its attributions and thanks, the tail. */
    static String render(Listing listing) {
        StringBuilder roll = new StringBuilder();
        roll.append("# The credits roll (design/ui/credits), generated from CREDITS.md by\n")
                .append("# ./gradlew :pipeline:credits (vanguard.pipeline.CreditsList); do not edit.\n")
                .append("# One entry per line: kind|text. Kinds: logo, gap, title, role, name, item, detail, text.\n")
                .append(HEAD);
        for (Group group : Group.values()) {
            List<Credit> credits = listing.credits().stream()
                    .filter(credit -> credit.group() == group)
                    .toList();
            Set<String> thanked = listing.thanks().getOrDefault(group, Set.of());
            if (credits.isEmpty() && thanked.isEmpty()) {
                continue;
            }
            roll.append("title|").append(group.heading).append('\n');
            if (INTRO.containsKey(group)) {
                roll.append("text|").append(INTRO.get(group)).append('\n');
                roll.append("gap|\n");
            }
            for (Credit credit : credits) {
                roll.append("item|“")
                        .append(credit.title())
                        .append("” by ")
                        .append(credit.author())
                        .append('\n');
                roll.append("detail|")
                        .append(credit.licence())
                        .append(" · ")
                        .append(credit.source())
                        .append('\n');
            }
            if (!thanked.isEmpty()) {
                if (!credits.isEmpty()) {
                    roll.append("gap|\n");
                }
                roll.append(group == Group.VOICE ? "role|READ BY (PUBLIC DOMAIN AND CC0)" : "role|WITH THANKS TO (CC0)")
                        .append('\n');
                roll.append("text|").append(String.join(", ", thanked)).append('\n');
            }
            roll.append("gap|\n");
        }
        return roll.append(TAIL).toString();
    }
}
