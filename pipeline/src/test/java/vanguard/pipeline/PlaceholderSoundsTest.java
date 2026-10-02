package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaceholderSoundsTest {
    @TempDir
    Path dir;

    @Test
    void finalSoundsAreKeptAndPlaceholdersCopied() throws IOException {
        Path concept = Files.createDirectories(dir.resolve("concept"));
        Path sfx = Files.createDirectories(dir.resolve("sfx"));
        for (String name : List.of("hit-metal-r08-a.ogg", "ui-menu-move-r08-a.ogg", "launch-rail-r11-b.ogg")) {
            FinalArtTest.writeOgg(concept.resolve(name), "Lavf", "TITLE=concept");
        }
        FinalArtTest.writeOgg(sfx.resolve("hit-metal-r08-a.ogg"), "Lavf", "SOURCE=tools/art/sfx_originals.py");
        byte[] finalSound = Files.readAllBytes(sfx.resolve("hit-metal-r08-a.ogg"));
        FinalArtTest.writeOgg(sfx.resolve("ui-menu-move-r08-a.ogg"), "Lavf", "TITLE=old placeholder");

        List<String> copied = PlaceholderSounds.copy(
                concept, sfx, List.of("hit-metal-r08-a.ogg", "ui-menu-move-r08-a.ogg", "launch-rail-r11-b.ogg"));

        assertEquals(List.of("ui-menu-move-r08-a.ogg", "launch-rail-r11-b.ogg"), copied);
        assertArrayEquals(finalSound, Files.readAllBytes(sfx.resolve("hit-metal-r08-a.ogg")));
        assertArrayEquals(
                Files.readAllBytes(concept.resolve("ui-menu-move-r08-a.ogg")),
                Files.readAllBytes(sfx.resolve("ui-menu-move-r08-a.ogg")));
        assertArrayEquals(
                Files.readAllBytes(concept.resolve("launch-rail-r11-b.ogg")),
                Files.readAllBytes(sfx.resolve("launch-rail-r11-b.ogg")));
    }
}
