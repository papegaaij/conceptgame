package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

class SpriteUseTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void aSpriteOnlyOneLevelUsesGoesInItsUnitAtlas() {
        Map<String, String> atlases = SpriteUse.atlases(
                content,
                List.of(
                        "leviathan-fin-left",
                        "whirl-seed-husk",
                        "debris-large-a",
                        "crane-four-arm",
                        "civilian-crawler-pip",
                        "gorgon-frigate-head",
                        "mortar-blob",
                        "ore-canister"));

        assertEquals("level-03", atlases.get("leviathan-fin-left"));
        assertEquals("level-03", atlases.get("whirl-seed-husk"));
        assertEquals("level-03", atlases.get("debris-large-a"));
        assertEquals("level-02", atlases.get("crane-four-arm"));
        assertEquals("level-04", atlases.get("civilian-crawler-pip"));
        assertEquals("level-05", atlases.get("gorgon-frigate-head"));
        assertEquals(SpriteUse.SHARED, atlases.get("mortar-blob"), "Levels 05 and 06 both have Polyp Mortars");
        assertEquals("level-05", atlases.get("ore-canister"));
    }

    @Test
    void sharedSpritesAreTheGameWideOnesAndThoseOfSeveralLevels() {
        Map<String, String> atlases = SpriteUse.atlases(
                content,
                List.of(
                        "ship",
                        "skitter",
                        "stinger-flare",
                        "brood-pod",
                        "supply-drop",
                        "cargo-container",
                        "scatter-vulcan-shot",
                        "airstrike-bomber"));

        assertTrue(atlases.values().stream().allMatch(SpriteUse.SHARED::equals), atlases.toString());
    }

    @Test
    void aSpriteNothingUsesFailsTheBuild() {
        var failure = assertThrows(
                IllegalStateException.class, () -> SpriteUse.atlases(content, List.of("ship", "mystery-blob")));

        assertTrue(failure.getMessage().contains("[mystery-blob]"), failure.getMessage());
    }
}
