package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vanguard.sim.LevelResult.Rating;
import vanguard.sim.ScoringRules;

class DebriefRatingTest {
    private static final ScoringRules.Grade A_PLUS = new ScoringRules.Grade("A+", 85, 0.3);

    @Test
    void eachPartShowsItsPointsOfItsWeight() {
        assertEquals("19 / 30", DebriefScreen.ratingPart(new Rating.Part(18.6, 30)));
        assertEquals("40 / 40", DebriefScreen.ratingPart(new Rating.Part(40, 40)));
    }

    @Test
    void theRatingIsRoundedDownSoItNeverShowsAThresholdItMissed() {
        Rating almost = new Rating(
                new Rating.Part(40, 40),
                new Rating.Part(22.9, 30),
                new Rating.Part(15, 15),
                new Rating.Part(7.1, 15),
                A_PLUS);

        assertEquals("85", DebriefScreen.ratingTotal(almost));
        Rating missed = new Rating(
                new Rating.Part(40, 40),
                new Rating.Part(22.5, 30),
                new Rating.Part(15, 15),
                new Rating.Part(7.1, 15),
                A_PLUS);
        assertEquals("84", DebriefScreen.ratingTotal(missed));
        assertEquals("A+ AT 85", DebriefScreen.ratingTarget(missed));
    }
}
