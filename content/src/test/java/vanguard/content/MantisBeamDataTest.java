package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.EnemySpec;
import vanguard.sim.WaveSpec;

/**
 * The Mantis's beam origin at its eye (design/enemies/air/mantis/data.yaml, {@code sweep.origin}):
 * read into the sweep on every difficulty, and rejected outside the sprite.
 */
class MantisBeamDataTest {
    private static final String MANTIS = "enemies/air/mantis/data.yaml";

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theSweepStartsAtTheEyeOnEveryDifficulty(Difficulty difficulty) {
        Content content = ContentLoader.load(DesignTree.dataFiles());
        EnemySpec.Sweep sweep = SimSpecs.level(content, Level06Test.LEVEL, difficulty).waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("mantis"))
                .findFirst()
                .orElseThrow()
                .sweep()
                .orElseThrow();

        assertEquals(14, sweep.originIn(), 1e-9, "toward the field");
        assertEquals(25, sweep.originDown(), 1e-9, "below its centre");
    }

    @Test
    void anEyeOutsideTheSpriteIsRejected() {
        assertTrue(DesignTree.dataFiles().stream().anyMatch(f -> f.path().equals(MANTIS)), MANTIS);
        List<DataFile> files = DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(MANTIS)
                        ? new DataFile(MANTIS, f.text().replace("origin: [14, 25]", "origin: [14, 45]"))
                        : f)
                .toList();

        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));

        assertEquals(1, e.problems().size(), e.getMessage());
        assertTrue(e.problems().getFirst().contains(MANTIS), e.problems().getFirst());
        assertTrue(
                e.problems().getFirst().contains("attacks[0].sweep.origin"),
                e.problems().getFirst());
    }
}
