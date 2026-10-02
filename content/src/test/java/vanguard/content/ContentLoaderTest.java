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
                List.of("needler", "skitter"),
                content.enemies().keySet().stream().sorted().toList());
        assertEquals(190, content.levels().get("level-01-break-at-dawn").seconds());
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
    void anUnknownTierIsRejected() {
        assertProblem(
                SKITTER,
                text -> text.replace("tier: tiny", "tier: teeny"),
                "design/enemies/air/skitter/data.yaml:",
                "tier: ",
                "\"teeny\"");
    }

    @Test
    void aWaveOfAnUnknownEnemyIsRejected() {
        assertProblem(
                LEVEL_01,
                text -> text.replaceFirst("enemy: needler", "enemy: neddler"),
                "design/" + LEVEL_01 + ": waves[2].enemy: unknown enemy 'neddler' (known: needler, skitter)");
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
                "design/" + LEVEL_01 + ": waves[13].t: t=196.0 is after the level end at 190.0 s");
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
