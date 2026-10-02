package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LevelResultTest {
    private static final LevelScript.GroundObjectSpec BEACON =
            new LevelScript.GroundObjectSpec(0, 240, new Hitbox(12, 12), 0, 0, Optional.empty(), 3, 80, "beacon cache");
    private static final LevelScript LEVEL = TestSpecs.level(60, List.of(), List.of(BEACON), List.of());

    private static Tally kills(int count) {
        var tally = new Tally(TestSpecs.SCORING);
        for (int i = 0; i < count; i++) {
            tally.kill(5);
        }
        return tally;
    }

    @Test
    void aPerfectLevelGetsAnSWithEveryBonus() {
        LevelResult result = LevelResult.of(TestSpecs.SCORING, LEVEL, kills(80), 80, 0, 60, 1, true);

        assertEquals(100, result.rating(), 1e-9);
        assertEquals("S", result.grade().letter());
        assertEquals(
                List.of(
                        new LevelResult.BonusScore("Destruction", 10_000),
                        new LevelResult.BonusScore("Untouched", 5000),
                        new LevelResult.BonusScore("Explorer", 3000)),
                result.bonuses());
        assertEquals(400, result.credits().kills());
        assertEquals(120, result.gradeBonus(), "30 % of the credits earned");
    }

    @Test
    void theRatingWeighsKillsArmourSecretsAndTheLongestChain() {
        // Half the enemies, a third of the armour lost, the secret missed, a chain of 40 of 80.
        LevelResult result = LevelResult.of(TestSpecs.SCORING, LEVEL, kills(40), 80, 20, 60, 0, false);

        assertEquals(100 * (0.4 * 0.5 + 0.3 * (2.0 / 3) + 0.15 * 0 + 0.15 * 0.5), result.rating(), 1e-9);
        assertEquals("C", result.grade().letter());
        assertEquals(0, result.gradeBonus());
        assertEquals(50, result.killPercent());
        assertEquals(List.of(new LevelResult.BonusScore("Destruction", 5000)), result.bonuses());
    }
}
