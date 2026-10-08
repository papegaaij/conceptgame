package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import vanguard.content.AlliesData;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

class SpriteUseTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void aSpriteOnlyOneLevelUsesGoesInItsUnitAtlas() {
        Map<String, Set<String>> atlases = SpriteUse.atlases(
                content,
                List.of(
                        "leviathan-fin-left",
                        "whirl-seed-husk",
                        "debris-large-a",
                        "crane-four-arm",
                        "civilian-crawler-pip",
                        "gorgon-frigate-head",
                        "ore-canister"));

        assertEquals(Set.of("level-03"), atlases.get("leviathan-fin-left"));
        assertEquals(Set.of("level-03"), atlases.get("whirl-seed-husk"));
        assertEquals(Set.of("level-03"), atlases.get("debris-large-a"));
        assertEquals(Set.of("level-02"), atlases.get("crane-four-arm"));
        assertEquals(Set.of("level-04"), atlases.get("civilian-crawler-pip"));
        assertEquals(Set.of("level-05"), atlases.get("gorgon-frigate-head"));
        assertEquals(Set.of("level-05"), atlases.get("ore-canister"));
    }

    /** D10 = a (M5 part C): a unit several levels use goes in each of their unit atlases, never the shared pages. */
    @Test
    void aSpriteOfSeveralLevelsGoesInEachOfTheirUnitAtlases() {
        Map<String, Set<String>> atlases = SpriteUse.atlases(content, List.of("mortar-blob", "skitter"));

        assertEquals(levelsUsing("mortar"), atlases.get("mortar-blob"));
        assertTrue(
                atlases.get("mortar-blob").containsAll(Set.of("level-05", "level-06")),
                "Levels 05 and 06 have Polyp Mortars: " + atlases.get("mortar-blob"));
        assertEquals(levelsUsing("skitter"), atlases.get("skitter"));
        assertTrue(atlases.get("skitter").size() > 1, atlases.toString());
    }

    @Test
    void aLevelsTowsClaimTheLifeboatSprites() {
        Map<String, Set<String>> atlases =
                SpriteUse.atlases(content, List.of("lifeboat", "lifeboat-pod", "lifeboat-cable", "brood-carrier-hull"));

        assertEquals(Set.of("level-07"), atlases.get("lifeboat"));
        assertEquals(Set.of("level-07"), atlases.get("lifeboat-pod"));
        assertEquals(Set.of("level-07"), atlases.get("lifeboat-cable"));
        assertEquals(Set.of("level-07"), atlases.get("brood-carrier-hull"));
    }

    @Test
    void aLevelsCollapseClaimsItsDustPuffs() {
        Map<String, Set<String>> atlases = SpriteUse.atlases(content, List.of("collapse-puff"));

        assertEquals(Set.of("level-09"), atlases.get("collapse-puff"), "Level 09's arcology");
    }

    @Test
    void aScriptedLossClaimsTheGlowAndTheLance() {
        Map<String, Set<String>> atlases =
                SpriteUse.atlases(content, List.of("cloud-glow", "lance", "lance-flash", "ferry-hatch-break"));

        assertEquals(Set.of("level-10"), atlases.get("cloud-glow"), "Level 10's Lifeline Three");
        assertEquals(Set.of("level-10"), atlases.get("lance"));
        assertEquals(Set.of("level-10"), atlases.get("lance-flash"));
        assertEquals(Set.of("level-10"), atlases.get("ferry-hatch-break"), "the ferry hatch's look");
    }

    @Test
    void sharedSpritesAreTheGameWideOnesOnly() {
        List<String> gameWide = List.of(
                "ship",
                "engine-flame",
                "rook",
                "pickup-crate",
                "explosion-large",
                "scatter-vulcan-shot",
                "airstrike-bomber");
        List<String> units = List.of("skitter", "stinger-flare", "brood-pod", "supply-drop", "cargo-container");
        List<String> sprites = new ArrayList<>(gameWide);
        sprites.addAll(units);

        Map<String, Set<String>> atlases = SpriteUse.atlases(content, sprites);

        gameWide.forEach(sprite -> assertEquals(Set.of(SpriteUse.SHARED), atlases.get(sprite), sprite));
        units.forEach(sprite -> {
            assertFalse(atlases.get(sprite).contains(SpriteUse.SHARED), sprite + ": " + atlases.get(sprite));
            assertTrue(atlases.get(sprite).size() > 1, sprite + ": " + atlases.get(sprite));
        });
    }

    /** Every level finds all the sprites its data names in its own unit atlas, or in the shared pages. */
    @Test
    void everyLevelsUnitsAreInItsOwnAtlas() {
        Set<String> roots = new TreeSet<>();
        content.levels().values().forEach(level -> roots.addAll(SpriteUse.roots(content, level)));

        Map<String, Set<String>> atlases = SpriteUse.atlases(content, roots);

        content.levels().forEach((key, level) -> {
            for (String root : SpriteUse.roots(content, level)) {
                Set<String> in = atlases.get(root);
                assertTrue(
                        in.contains(SpriteUse.atlasOf(key)) || in.equals(Set.of(SpriteUse.SHARED)), root + ": " + in);
            }
        });
    }

    private Set<String> levelsUsing(String root) {
        Set<String> levels = new TreeSet<>();
        content.levels().forEach((key, level) -> {
            if (SpriteUse.roots(content, level).contains(root)) {
                levels.add(SpriteUse.atlasOf(key));
            }
        });
        return levels;
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

        Map<String, Set<String>> atlases = SpriteUse.atlases(content, List.of(unused, unused + "-husk"));

        Set<String> atlas = Set.of(SpriteUse.atlasOf(content.enemy(unused).firstLevel()));
        assertEquals(Map.of(unused, atlas, unused + "-husk", atlas), atlases);
    }

    /**
     * An ally no level escorts yet goes in the unit atlas of its {@code first_level}, as one only its
     * first level escorts does (the evacuation shuttle, Level 10's, whose art lands before the
     * level's data, M5 part D): the same atlas either way.
     */
    @Test
    void anAllyNoLevelUsesYetGoesInItsFirstLevelsUnitAtlas() {
        Set<String> used = new TreeSet<>();
        content.levels().forEach((key, level) -> {
            for (String root : SpriteUse.roots(content, level)) {
                AlliesData.Ally ally = content.allies().allies().get(root);
                if (ally == null || !ally.firstLevel().equals(Optional.of(Content.levelNumber(key)))) {
                    used.add(root);
                }
            }
        });
        Map.Entry<String, AlliesData.Ally> unused = content.allies().allies().entrySet().stream()
                .filter(ally -> !used.contains(ally.getKey()))
                .filter(ally -> ally.getValue().firstLevel().isPresent())
                .findFirst()
                .orElseThrow();
        String slug = unused.getKey();

        Map<String, Set<String>> atlases = SpriteUse.atlases(content, List.of(slug, slug + "-wreck_0"));

        Set<String> atlas =
                Set.of(SpriteUse.atlasOf(unused.getValue().firstLevel().orElseThrow()));
        assertEquals(Map.of(slug, atlas, slug + "-wreck_0", atlas), atlases);
    }

    @Test
    void aSpriteNothingUsesFailsTheBuild() {
        var failure = assertThrows(
                IllegalStateException.class, () -> SpriteUse.atlases(content, List.of("ship", "mystery-blob")));

        assertTrue(failure.getMessage().contains("[mystery-blob]"), failure.getMessage());
    }
}
