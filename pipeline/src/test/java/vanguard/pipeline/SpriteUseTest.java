package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    void aLevelsTowsClaimTheLifeboatSprites() {
        Map<String, String> atlases =
                SpriteUse.atlases(content, List.of("lifeboat", "lifeboat-pod", "lifeboat-cable", "brood-carrier-hull"));

        assertEquals("level-07", atlases.get("lifeboat"));
        assertEquals("level-07", atlases.get("lifeboat-pod"));
        assertEquals("level-07", atlases.get("lifeboat-cable"));
        assertEquals("level-07", atlases.get("brood-carrier-hull"));
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

    /**
     * An enemy no level uses yet goes in its first level's unit atlas, as one only its first level
     * uses does (the Creeper, Level 08's, the newest unit since M5 part B): the same atlas either way.
     */
    @Test
    void anEnemyNoLevelUsesYetGoesInItsFirstLevelsUnitAtlas() {
        Set<String> used = new TreeSet<>();
        content.levels().forEach((key, level) -> {
            for (String root : SpriteUse.roots(content, level)) {
                if (!content.enemies().containsKey(root)
                        || content.enemy(root).firstLevel() != Content.levelNumber(key)) {
                    used.add(root);
                }
            }
        });
        String unused = content.enemies().keySet().stream()
                .filter(slug -> !used.contains(slug))
                .filter(slug -> content.enemies().keySet().stream()
                        .noneMatch(other -> !other.equals(slug) && other.startsWith(slug + "-")))
                .findFirst()
                .orElseThrow();

        Map<String, String> atlases = SpriteUse.atlases(content, List.of(unused, unused + "-husk"));

        String atlas = SpriteUse.atlasOf(content.enemy(unused).firstLevel());
        assertEquals(Map.of(unused, atlas, unused + "-husk", atlas), atlases);
    }

    @Test
    void aSpriteNothingUsesFailsTheBuild() {
        var failure = assertThrows(
                IllegalStateException.class, () -> SpriteUse.atlases(content, List.of("ship", "mystery-blob")));

        assertTrue(failure.getMessage().contains("[mystery-blob]"), failure.getMessage());
    }
}
