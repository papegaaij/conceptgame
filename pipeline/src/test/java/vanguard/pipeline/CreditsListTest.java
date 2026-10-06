package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The credits roll (design/ui/credits) lists every shipped asset that asks for attribution and
 * leaves out rejected and unshipped ones; the committed roll is the one CREDITS.md gives today.
 */
class CreditsListTest {
    private static final Path ROOT = Path.of(System.getProperty("vanguard.rootDir", ".."));

    private static final String HEADER = """
            # Credits

            | File | Title | Author | Source | Licence | Changes |
            |---|---|---|---|---|---|
            """;

    @TempDir
    Path dir;

    private static String row(String file, String title, String author, String licence) {
        String url =
                licence.startsWith("CC-BY") ? "https://creativecommons.org/licenses/by/4.0/" : "https://cc0.example/";
        return "| " + file + " | " + title + " | " + author + " | [freesound](https://freesound.org/people/" + author
                + "/sounds/1/) | [" + licence + "](" + url + ") | cut |\n";
    }

    private void touch(String path) throws IOException {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1});
    }

    private String roll(String markdown) throws IOException {
        Files.writeString(dir.resolve("CREDITS.md"), markdown, StandardCharsets.UTF_8);
        return CreditsList.render(dir);
    }

    @Test
    void aShippedCcByRowIsAttributedWithTitleAuthorLicenceAndSource() throws IOException {
        touch("assets/sfx/zap-r01-a.ogg");
        String roll = roll(HEADER + row("assets/sfx/zap-r01-a.ogg", "Big Zap", "zapper", "CC-BY 4.0"));
        assertTrue(roll.contains("item|“Big Zap” by zapper\n"), roll);
        assertTrue(roll.contains("detail|CC-BY 4.0 · freesound.org/people/zapper/sounds/1\n"), roll);
        assertTrue(roll.contains("title|SOUND EFFECTS\n"), roll);
    }

    @Test
    void aRejectedOrUnshippedRowIsLeftOut() throws IOException {
        touch("design/audio/sfx/concept/rejected/zap-r01-b.ogg");
        touch("design/audio/sfx/concept/zap-r01-c.ogg");
        String roll = roll(HEADER
                + row("design/audio/sfx/concept/rejected/zap-r01-b.ogg", "Rejected Zap", "nobody", "CC-BY 4.0")
                + row("design/audio/sfx/concept/zap-r01-c.ogg", "Concept Zap", "someone", "CC-BY 4.0")
                + row("assets/sfx/gone-r01-a.ogg", "Gone", "ghost", "CC-BY 4.0"));
        assertFalse(roll.contains("Rejected Zap"), roll);
        assertFalse(roll.contains("Concept Zap"), roll);
        assertFalse(roll.contains("Gone"), roll);
        assertFalse(roll.contains("SOUND EFFECTS"), "no group without shipped rows");
    }

    @Test
    void aConceptRowCountsWhenItsCopyIsInTheAssets() throws IOException {
        touch("assets/sfx/zap-r01-c.ogg");
        String roll =
                roll(HEADER + row("design/audio/sfx/concept/zap-r01-c.ogg", "Copied Zap", "someone", "CC-BY 3.0"));
        assertTrue(roll.contains("“Copied Zap” by someone"), roll);
    }

    @Test
    void cc0AuthorsAreThankedOnceAndAttributionsListedOnce() throws IOException {
        touch("assets/sfx/a.ogg");
        touch("assets/sfx/b.ogg");
        touch("assets/sfx/c.ogg");
        String roll = roll(HEADER
                + row("design/audio/sfx/concept/a.ogg", "Twice", "twin", "CC-BY 4.0")
                + row("assets/sfx/a.ogg", "Twice", "twin", "CC-BY 4.0")
                + row("assets/sfx/b.ogg", "Free", "zed", "CC0 1.0")
                + row("assets/sfx/c.ogg", "Free too", "Amy", "CC0 1.0"));
        assertEquals(1, count(roll, "Twice"));
        assertTrue(roll.contains("role|WITH THANKS TO (CC0)\ntext|Amy, zed\n"), roll);
        assertFalse(roll.contains("Free too"), "CC0 titles are not listed");
    }

    @Test
    void aVoiceReferenceCountsWhenItsSpeakerHasLines() throws IOException {
        touch("assets/voice/okafor/line.ogg");
        String roll = roll(HEADER
                + row("design/audio/voice/refs/ref-okafor.wav", "Book", "Ruth Reader (LibriVox reader)", "CC0 1.0")
                + row("design/audio/voice/refs/ref-unused.wav", "Book", "Una Used (LibriVox reader)", "CC0 1.0"));
        assertTrue(roll.contains("title|VOICES\n"), roll);
        assertTrue(roll.contains("text|Ruth Reader\n"), roll);
        assertFalse(roll.contains("Una Used"), roll);
    }

    @Test
    void fontsAreAttributedWithTheirLicence() throws IOException {
        touch("assets/fonts/body.png");
        String roll = roll(HEADER
                + "| assets/fonts/body.png (and design/ui/concept/kit.png) | Sans Bold | Font team"
                + " | [fonts.example](https://fonts.example/) | [Vera licence](https://fonts.example/l) (permissive)"
                + " | rasterised |\n");
        assertTrue(roll.contains("title|FONTS\nitem|“Sans Bold” by Font team\n"), roll);
        assertTrue(roll.contains("detail|Vera licence · fonts.example\n"), roll);
    }

    @Test
    void aMalformedRowFailsTheBuild() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CreditsList.parse(HEADER + "| assets/sfx/a.ogg | Title | Author | no link |\n"));
    }

    /** A Windows checkout gives CREDITS.md {@code \r\n} line ends; the roll is the same. */
    @Test
    void aCreditsMdWithWindowsLineEndsGivesTheSameRoll() throws IOException {
        touch("assets/sfx/zap-r01-a.ogg");
        touch("assets/sfx/b.ogg");
        String markdown = HEADER
                + row("assets/sfx/zap-r01-a.ogg", "Big Zap", "zapper", "CC-BY 4.0")
                + row("assets/sfx/b.ogg", "Free", "zed", "CC0 1.0");
        String unix = roll(markdown);
        assertEquals(unix, roll(markdown.replace("\n", "\r\n")));
        assertFalse(unix.contains("\r"), "the roll is written with \\n line ends");
        assertTrue(
                unix.contains("item|“Big Zap” by zapper\ndetail|CC-BY 4.0 · freesound.org/people/zapper/sounds/1\n"));
        assertEquals(
                CreditsList.parse(markdown),
                CreditsList.parse(markdown.replace("\n", "\r\n")),
                "the same rows, line numbers and cells");
    }

    /** The committed roll and CREDITS.md are read alike whatever line ends the checkout gave them. */
    @Test
    void textFilesAreReadWithUnixLineEnds() throws IOException {
        Path windows = dir.resolve("windows.txt");
        Files.writeString(windows, "logo|\r\ngap|\r\nitem|a\r\n", StandardCharsets.UTF_8);
        Path mac = dir.resolve("mac.txt");
        Files.writeString(mac, "logo|\rgap|\ritem|a\r", StandardCharsets.UTF_8);
        assertEquals("logo|\ngap|\nitem|a\n", CreditsList.readText(windows));
        assertEquals("logo|\ngap|\nitem|a\n", CreditsList.readText(mac));
    }

    @Test
    void theCommittedRollIsUpToDate() throws IOException {
        assertRollUpToDate(ROOT);
    }

    /** Checked from the other side: every CC-BY row whose file is in assets/ is in the committed roll. */
    @Test
    void everyShippedCcByFileOfCreditsMdIsInTheRoll() throws IOException {
        assertTrue(assertShippedCcByRowsInRoll(ROOT) > 0, "the assets ship CC-BY sounds");
        assertTrue(
                CreditsList.readText(ROOT.resolve(CreditsList.OUTPUT)).contains("title|FONTS\n"),
                "the fonts are credited");
    }

    /**
     * The checks of the committed roll pass on a Windows checkout, where Git ({@code core.autocrlf})
     * gives CREDITS.md and the committed roll {@code \r\n} line ends (CI's windows-latest runner).
     */
    @Test
    void theCommittedRollChecksPassOnAWindowsCheckout() throws IOException {
        touch("assets/sfx/zap-r01-a.ogg");
        touch("assets/sfx/b.ogg");
        String markdown = HEADER
                + row("assets/sfx/zap-r01-a.ogg", "Big Zap", "zapper", "CC-BY 4.0")
                + row("assets/sfx/b.ogg", "Free", "zed", "CC0 1.0");
        String roll = roll(markdown);
        Files.writeString(dir.resolve("CREDITS.md"), markdown.replace("\n", "\r\n"), StandardCharsets.UTF_8);
        Path committed = dir.resolve(CreditsList.OUTPUT);
        Files.createDirectories(committed.getParent());
        Files.writeString(committed, roll.replace("\n", "\r\n"), StandardCharsets.UTF_8);
        assertRollUpToDate(dir);
        assertEquals(1, assertShippedCcByRowsInRoll(dir));
    }

    private static void assertRollUpToDate(Path root) throws IOException {
        String committed = CreditsList.readText(root.resolve(CreditsList.OUTPUT));
        assertEquals(CreditsList.render(root), committed, "run ./gradlew :pipeline:credits and commit the roll");
    }

    /** Asserts that every CC-BY row with a file in {@code root}'s assets is in its roll; the number of rows. */
    private static int assertShippedCcByRowsInRoll(Path root) throws IOException {
        String roll = CreditsList.readText(root.resolve(CreditsList.OUTPUT));
        List<CreditsList.Row> rows = CreditsList.parse(CreditsList.readText(root.resolve("CREDITS.md")));
        int shipped = 0;
        for (CreditsList.Row row : rows) {
            boolean inAssets = row.paths().stream()
                    .anyMatch(path -> path.startsWith("assets/") && Files.isRegularFile(root.resolve(path)));
            if (inAssets && row.licence().toUpperCase(Locale.ROOT).startsWith("CC-BY")) {
                shipped++;
                assertTrue(
                        roll.contains("item|“" + row.title() + "” by " + row.author() + "\n"),
                        "CREDITS.md line " + row.line() + " (" + row.title() + ") is not in the roll");
                assertTrue(roll.contains("detail|" + row.licence() + " · " + row.source() + "\n"));
            }
        }
        return shipped;
    }

    /** Every recorded sound effect in assets/sfx that CREDITS.md names under CC-BY is credited. */
    @Test
    void everyCcBySoundInTheAssetsIsCredited() throws IOException {
        String roll = CreditsList.readText(ROOT.resolve(CreditsList.OUTPUT));
        List<CreditsList.Row> rows = CreditsList.parse(CreditsList.readText(ROOT.resolve("CREDITS.md")));
        try (Stream<Path> sounds = Files.list(ROOT.resolve("assets/sfx"))) {
            for (Path sound : sounds.toList()) {
                String name = sound.getFileName().toString();
                for (CreditsList.Row row : rows) {
                    boolean names = row.paths().stream()
                            .anyMatch(path -> !path.contains("/rejected/") && path.endsWith("/" + name));
                    if (names && row.needsAttribution()) {
                        assertTrue(
                                roll.contains("“" + row.title() + "” by " + row.author()),
                                name + " (CREDITS.md line " + row.line() + ") is not credited");
                    }
                }
            }
        }
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }
}
