package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ContentLoaderTest {
    private static final String SKITTER = "enemies/air/skitter/data.yaml";
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";

    @Test
    void everyDataFileInTheDesignTreeLoadsAndValidates() {
        Content content = ContentLoader.load(DesignTree.dataFiles());

        assertEquals(14, content.weapons().size());
        assertEquals(
                List.of(
                        "brood-carrier",
                        "brood-pod",
                        "coilwyrm",
                        "creeper",
                        "driftjelly",
                        "gorgon-frigate",
                        "harbour-kraken",
                        "hive-node",
                        "leviathan",
                        "mantis",
                        "mote-swarm",
                        "needler",
                        "polyp-mortar",
                        "ravager",
                        "reef-spitter",
                        "scuttler",
                        "skitter",
                        "spine-turret",
                        "spore-bomber",
                        "stinger",
                        "whirl-seed",
                        "wraith"),
                content.enemies().keySet().stream().sorted().toList());
        assertEquals(
                180, content.level("act-1-first-contact/level-01-break-at-dawn").seconds());
        assertEquals(
                185, content.level("act-1-first-contact/level-03-spore-drift").seconds());
    }

    @Test
    void theClasspathCarriesEveryDataFile() throws IOException {
        String index;
        try (InputStream in = getClass().getResourceAsStream(ContentLoader.INDEX)) {
            index = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(
                DesignTree.dataFiles().stream().map(DataFile::path).toList(),
                index.lines().toList());
        assertEquals(ContentLoader.load(DesignTree.dataFiles()), ContentLoader.fromClasspath());
    }

    @Test
    void anUnknownFieldIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("\nbounty: 5", "\nbounty: 5\nbonus: 5"),
                "design/enemies/air/skitter/data.yaml",
                "bonus: unknown field (known: ");
    }

    @Test
    void aMissingFieldIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("\nbounty: 5", ""),
                "design/enemies/air/skitter/data.yaml:",
                "bounty: required field is missing");
    }

    @Test
    void aValueOutOfRangeIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("\nhp: 1", "\nhp: -1"),
                "design/enemies/air/skitter/data.yaml:",
                "hp must be > 0, was -1.0");
    }

    @Test
    void theOrientationGivesTheHeadingsOfTheAngleSet() {
        Content content = ContentLoader.load(DesignTree.dataFiles());

        assertEquals(Orientation.ANGLES_16, content.enemy("skitter").orientation());
        assertEquals(16, content.enemy("skitter").orientation().headings());
        assertEquals(Orientation.FIXED, content.enemy("needler").orientation());
        assertEquals(1, content.enemy("needler").orientation().headings());
    }

    @Test
    void anUnknownOrientationIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("orientation: 16 angles", "orientation: 12 angles"),
                "design/enemies/air/skitter/data.yaml:",
                "orientation must be fixed, ±30° tilt, 16 angles, 32 angles or radial, was '12 angles'");
    }

    @Test
    void anUnknownTierIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("tier: tiny", "tier: teeny"),
                "design/enemies/air/skitter/data.yaml:",
                "tier: ",
                "\"teeny\"");
    }

    @Test
    void thePartDDataIsRead() {
        Content content = ContentLoader.load(DesignTree.dataFiles());

        AlliesData.Ally crawler = content.allies().allies().get("civilian-crawler");
        assertEquals(60, crawler.hp());
        assertEquals(10, crawler.damagedBy().claws());
        assertEquals(7, crawler.headings().count());
        EnemyData.Attack spawn = content.enemy("brood-pod").attacks().getFirst();
        assertEquals("skitter", spawn.spawn().orElseThrow().enemy());
        assertEquals(6, spawn.spawn().orElseThrow().count());
        assertEquals(
                40, content.enemy("brood-pod").movement().drift().orElseThrow().speed());
        EnemyData scuttler = content.enemy("scuttler");
        assertEquals(45, scuttler.armour().frontArc().orElseThrow());
        assertEquals("facing", scuttler.attacks().getFirst().aim().orElseThrow());
        assertEquals(90, scuttler.attacks().get(1).away().orElseThrow());
        assertEquals(24, scuttler.movement().walk().orElseThrow().stride());
        assertEquals("none", content.enemy("needler").armour().text().orElseThrow());
        assertEquals(1, content.specials().specials().getFirst().free());
        assertEquals(36, content.specials().airstrike().bombSpacing());
    }

    @Test
    void aWaveOfAnUnknownEnemyIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replaceFirst("enemy: needler", "enemy: neddler"),
                "design/" + LEVEL_01
                        + ": waves[2].enemy: unknown enemy 'neddler' (known: brood-carrier, brood-pod, coilwyrm, creeper, driftjelly, gorgon-frigate, harbour-kraken, hive-node, leviathan, mantis, mote-swarm, needler, polyp-mortar, ravager, reef-spitter, scuttler, skitter, spine-turret, spore-bomber, stinger, whirl-seed, wraith)");
    }

    @Test
    void aMixedWaveNamesTheGroupOfAnUnknownFormation() {
        assertProblem(
                LEVEL_01,
                text -> text.replace(
                        "{formation: V-wing, enemy: needler, count: 3}",
                        "{formation: W-wing, enemy: needler, count: 3}"),
                "design/" + LEVEL_01 + ": waves[8].groups[1].formation: unknown formation 'W-wing'");
    }

    @Test
    void aWaveAfterTheLevelEndIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("{t: 166,", "{t: 196,"),
                "design/" + LEVEL_01 + ": waves[13].t: t=196.0 is after the level end at 180.0 s");
    }

    @Test
    void aWaveWithoutEnemiesIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace(
                        "formation: snake, enemy: skitter, count: 6, from: front, edge: left",
                        "from: front, edge: left"),
                "design/" + LEVEL_01 + ":",
                "waves[0]: give either formation, enemy and count, or the groups");
    }

    @Test
    void anUnknownSetPieceIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("{piece: moon,", "{piece: mooon,"),
                "design/" + LEVEL_01 + ": backdrop.placed[5].piece: unknown set piece 'mooon' (known: ");
    }

    @Test
    void twoTileSetsOnOneLayerAreRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("tiles: [earth, gantry-rails]", "tiles: [earth, gantry-rails, dock-frames]"),
                "design/" + LEVEL_01 + ": sections[2].tiles[2]: a second tile set on ground");
    }

    @Test
    void aSectionWithoutADeepTileSetIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("tiles: [earth, dock-frames]", "tiles: [dock-frames]"),
                "design/" + LEVEL_01 + ": sections[1].tiles: no tile set on deep, which has to cover the whole screen");
    }

    @Test
    void anAtmosphereWithoutALookIsRejected() {
        assertProblem(
                LEVEL_01,
                // Level 01 defines no look for heavy weather; one section asking for it is an error.
                text -> text.replaceFirst("atmosphere: clear", "atmosphere: heavy"),
                "design/" + LEVEL_01 + ": sections[0].atmosphere: no backdrop.atmosphere.heavy");
    }

    @Test
    void moreThanThreeMidSizeSetPiecesOnScreenAreRejected() {
        String hull = "    - {piece: cruiser-hull, t: 82, x: 240}\n";
        assertProblem(
                LEVEL_01,
                text -> text.replace(hull, hull.repeat(4)),
                "design/" + LEVEL_01 + ": backdrop.placed: t=",
                ": 4 mid-size set pieces on screen (at most 3): cruiser-hull, cruiser-hull");
    }

    @Test
    void moreThanTwoStronglyAnimatedElementsOnScreenAreRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace(
                        "perimeter: {layer: ground, height: 608}", "perimeter: {layer: ground, height: 608, drift: 4}"),
                "design/" + LEVEL_01 + ": backdrop: t=",
                ": 3 strongly animated elements on screen (at most 2): atmosphere banks, perimeter, platform-burning");
    }

    @Test
    void aSetPieceMovingMoreThanTwoPixelsPerFrameIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("[6, 150, 400]", "[6, 400, 400]"),
                "design/" + LEVEL_01 + ": backdrop.placed[3].path[3]: moves 256 px/s, more than 120");
    }

    @Test
    void aSetPieceThatIsNeverOnScreenIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replace("{piece: moon, t: 21.2, x: 44}", "{piece: moon, t: 21.2, x: 900}"),
                "design/" + LEVEL_01 + ": backdrop.placed[5]: 'moon' is never on screen");
    }

    @Test
    void brokenYamlIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("traits: [spread, forward]", "traits: [spread, forward"),
                "design/enemies/air/skitter/data.yaml:");
    }

    @Test
    void aDataFileWithoutASchemaIsRejected() {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        files.add(new DataFile("world/earth-orbit/data.yaml", "speed: 1\n"));

        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));

        assertEquals(
                List.of("design/world/earth-orbit/data.yaml: no schema for a data file at this path"), e.problems());
    }

    /** Loads the design tree with one file edited; exactly one problem is reported, containing every part. */
    private static void assertProblem(String path, UnaryOperator<String> edit, String... parts) {
        List<DataFile> files = DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(path) ? new DataFile(path, edit.apply(f.text())) : f)
                .toList();
        assertTrue(files.stream().anyMatch(f -> f.path().equals(path)), path);

        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));

        assertEquals(1, e.problems().size(), e.getMessage());
        for (String part : parts) {
            assertTrue(
                    e.problems().getFirst().contains(part),
                    "'" + part + "' in " + e.problems().getFirst());
        }
    }
}
