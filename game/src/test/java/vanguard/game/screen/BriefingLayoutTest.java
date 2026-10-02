package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

/** Every briefing page of the content fits the briefing screen, and its image is in the assets. */
class BriefingLayoutTest {
    private static final Path IMAGES = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "ui", "briefing");

    private final Content content = ContentLoader.fromClasspath();

    @Test
    void aPageWithoutAnImageFitsOneScreen() {
        for (BriefingPage page : pages()) {
            if (page.image().isEmpty()) {
                int lines = BriefingScreen.lines(page).size();
                assertTrue(lines <= BriefingScreen.maxLines(false), lines + " lines: " + page.line());
            }
        }
    }

    @Test
    void aPageWithAnImageGoesOnOverTheNextScreens() {
        BriefingPage page = new BriefingPage("Okafor", "word ".repeat(200), Optional.empty(), Optional.of("map"));
        List<List<String>> screens = BriefingScreen.screens(page);
        assertTrue(screens.size() > 1);
        screens.forEach(lines -> assertTrue(lines.size() <= BriefingScreen.maxLines(true)));
        assertEquals(
                BriefingScreen.lines(page),
                screens.stream().flatMap(List::stream).toList());
    }

    /** tools/art/briefing_images.py renders every image a page names, at the screen's image size. */
    @Test
    void everyPageImageIsInTheAssetsAtItsSize() throws IOException {
        for (BriefingPage page : pages()) {
            if (page.image().isPresent()) {
                Path file = IMAGES.resolve(page.image().get() + ".png");
                assertTrue(Files.isRegularFile(file), file.toString());
                try (InputStream in = Files.newInputStream(file);
                        DataInputStream png = new DataInputStream(in)) {
                    png.skipNBytes(16);
                    assertEquals(BriefingScreen.IMAGE_WIDTH, png.readInt(), file.toString());
                    assertEquals(BriefingScreen.IMAGE_HEIGHT, png.readInt(), file.toString());
                }
            }
        }
    }

    private List<BriefingPage> pages() {
        List<BriefingPage> pages = new ArrayList<>();
        content.acts().values().forEach(act -> pages.addAll(act.briefing()));
        content.levels().values().forEach(level -> pages.addAll(level.briefing().pages()));
        return pages;
    }
}
