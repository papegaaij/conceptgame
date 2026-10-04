package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtlasBudgetTest {
    @TempDir
    Path dir;

    @Test
    void level01AsMeasuredInM2FitsItsBudget() throws IOException {
        write("sprites.atlas", page("sprites.png", 2048, 256, region("ship", 48, 48), region("ship", 48, 48)));
        write(
                "backdrop.atlas",
                page("backdrop.png", 2048, 2048, region("level-01/earth", 480, 1120))
                        + page("backdrop2.png", 2048, 1024, region("level-01/moon", 96, 96)));

        assertEquals(List.of(), AtlasBudget.violations(dir));
        AtlasBudget.enforce(dir);
    }

    @Test
    void readsEveryPageWithItsRegionBytes() throws IOException {
        write("sprites.atlas", page("sprites.png", 2048, 256, region("orb", 15, 15), region("orb", 15, 15)));

        List<AtlasBudget.Page> pages = AtlasBudget.read(dir.resolve("sprites.atlas"));

        assertEquals(1, pages.size());
        assertEquals(2048L * 256 * 4, pages.getFirst().bytes());
        assertEquals(2L * 15 * 15 * 4, pages.getFirst().regionBytes().get("orb"));
    }

    @Test
    void moreThanTwoSharedPagesFailTheBuild() throws IOException {
        write(
                "sprites.atlas",
                page("sprites.png", 2048, 2048, region("a", 8, 8))
                        + page("sprites2.png", 2048, 2048, region("b", 8, 8))
                        + page("sprites3.png", 64, 64, region("c", 8, 8)));
        write("backdrop.atlas", page("backdrop.png", 2048, 2048, region("level-01/earth", 8, 8)));

        List<String> violations = AtlasBudget.violations(dir);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().startsWith("shared pages (sprites atlas): 3 pages"), violations.getFirst());
        assertThrows(IllegalStateException.class, () -> AtlasBudget.enforce(dir));
    }

    @Test
    void aLevelCountsOnlyThePagesThatHoldItsRegions() throws IOException {
        write("sprites.atlas", page("sprites.png", 64, 64, region("a", 8, 8)));
        StringBuilder backdrop = new StringBuilder();
        for (int i = 1; i <= 7; i++) {
            backdrop.append(page("backdrop" + i + ".png", 2048, 2048, region("level-02/piece" + i, 8, 8)));
        }
        backdrop.append(page("backdrop8.png", 2048, 2048, region("level-01/earth", 8, 8)));
        write("backdrop.atlas", backdrop.toString());

        List<String> violations = AtlasBudget.violations(dir);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().startsWith("level level-02: 7 pages, 112 MiB"), violations.getFirst());
    }

    @Test
    void aLevelsUnitAtlasCountsWithItsBackdrop() throws IOException {
        write("sprites.atlas", page("sprites.png", 2048, 2048, region("ship", 8, 8)));
        StringBuilder units = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            units.append(page("level-03" + i + ".png", 2048, 2048, region("leviathan-" + i, 8, 8)));
        }
        write("level-03.atlas", units.toString());
        StringBuilder backdrop = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            backdrop.append(page("backdrop" + i + ".png", 2048, 2048, region("level-03/piece" + i, 8, 8)));
        }
        write("backdrop.atlas", backdrop.toString());

        List<String> violations = AtlasBudget.violations(dir);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().startsWith("level level-03: 7 pages"), violations.getFirst());
        assertTrue(
                AtlasBudget.report(dir).contains("level level-03 (units + backdrop): 7/6 pages"),
                AtlasBudget.report(dir).toString());
    }

    @Test
    void aSpriteInAUnitAtlasMustFitAPage() throws IOException {
        write("sprites.atlas", page("sprites.png", 64, 64, region("ship", 8, 8)));
        String halo = region("halo", 1024, 1024).repeat(5);
        write("level-02.atlas", page("level-02.png", 2048, 2048, halo));
        write("backdrop.atlas", page("backdrop.png", 64, 64, region("level-02/earth", 8, 8)));

        assertEquals(
                List.of("sprite 'halo': 20 MiB of frames, more than one page (16 MiB)"), AtlasBudget.violations(dir));
    }

    @Test
    void oneSpriteLargerThanAPageFails() throws IOException {
        String halo = region("halo", 1024, 1024).repeat(5);
        write("sprites.atlas", page("sprites.png", 2048, 2048, halo) + page("sprites2.png", 2048, 1024, halo));
        write("backdrop.atlas", page("backdrop.png", 64, 64, region("level-01/earth", 8, 8)));

        List<String> violations = AtlasBudget.violations(dir);

        assertEquals(List.of("sprite 'halo': 40 MiB of frames, more than one page (16 MiB)"), violations);
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content);
    }

    private static String page(String file, int width, int height, String... regions) {
        return "\n" + file + "\nsize: " + width + ", " + height
                + "\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n" + String.join("", regions);
    }

    private static String region(String name, int width, int height) {
        return name + "\n  rotate: false\n  xy: 0, 0\n  size: " + width + ", " + height + "\n  orig: " + width + ", "
                + height + "\n  offset: 0, 0\n  index: -1\n";
    }
}
