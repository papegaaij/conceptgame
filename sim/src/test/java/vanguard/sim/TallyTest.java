package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TallyTest {
    private final Tally tally = new Tally(TestSpecs.SCORING);

    @Test
    void killsWithinTheWindowBuildTheChainAndItsMultiplier() {
        for (int i = 0; i < 9; i++) {
            tally.kill(5);
        }
        assertEquals(1.0, tally.multiplier());

        tally.kill(5);

        assertEquals(10, tally.chain());
        assertEquals(1.5, tally.multiplier());
        assertEquals(9 * 50 + 75, tally.score());
    }

    @Test
    void theMultiplierStopsAtItsMaximum() {
        assertEquals(5.0, TestSpecs.SCORING.chain().multiplier(200));
        assertEquals(80, TestSpecs.SCORING.chain().countAtMax());
    }

    @Test
    void theChainEndsWhenTheWindowRunsOut() {
        tally.kill(5);
        for (int i = 0; i < SimStep.ticks(2) - 1; i++) {
            tally.step();
        }
        assertEquals(1, tally.chain());

        tally.step();

        assertEquals(0, tally.chain());
        assertEquals(1, tally.maxChain());
    }

    @Test
    void creditsAreTheBaseValueTimesTheFactorRoundedHalfToEven() {
        var easy = new ScoringRules(
                10,
                10,
                TestSpecs.SCORING.chain(),
                0.75,
                1.25,
                TestSpecs.SCORING.weights(),
                List.of(),
                TestSpecs.SCORING.grades());
        var tally = new Tally(easy);

        assertEquals(12, tally.earn(CreditSource.SALVAGE, 10), "12.5 rounds to 12");
        assertEquals(15, tally.kill(12));
        assertEquals(90, tally.score(), "120 × 0.75");
        assertEquals(27, tally.credits());
    }

    @Test
    void pickupsScoreTenTimesTheirCreditValue() {
        tally.scoreValue(80);

        assertEquals(800, tally.score());
        assertEquals(0, tally.chain(), "no chain for pickups");
    }

    @Test
    void theBountyScaleMultipliesBountiesOnlyAndRoundsOncePerPayout() {
        var scaled = new ScoringRules(
                10,
                10,
                TestSpecs.SCORING.chain(),
                1,
                1.6,
                TestSpecs.SCORING.weights(),
                List.of(),
                TestSpecs.SCORING.grades(),
                1.5);
        var tally = new Tally(scaled);

        assertEquals(12, tally.kill(5), "5 × 1.6 × 1.5");
        assertEquals(29, tally.partKill(12), "28.8: one rounding after the scale");
        assertEquals(16, tally.earn(CreditSource.SALVAGE, 10), "salvage is not a bounty");
        assertEquals(170, tally.score(), "5 × 10 + 12 × 10: the score uses the bounty without the scale");
    }
}
