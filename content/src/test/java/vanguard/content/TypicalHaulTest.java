package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Every level's typical haul lands within 5 % of its budget, and a perfect run earns well above it
 * (design/systems/economy, per-level budget; user decision 2026-10-04).
 */
class TypicalHaulTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theTypicalHaulLandsOnTheBudget() {
        Map<Integer, TypicalHaul> hauls = hauls();
        StringBuilder off = new StringBuilder();
        hauls.forEach((number, haul) -> {
            System.out.printf(
                    "Level %02d: typical %.0f, budget %.0f (%+.1f %%), perfect %d (%.2f × budget)%n",
                    number,
                    haul.typical(),
                    haul.budget(),
                    100 * haul.offBudget(),
                    haul.perfect(),
                    haul.perfect() / haul.budget());
            if (Math.abs(haul.offBudget()) > 0.05) {
                off.append(String.format(" Level %02d: typical %.0f vs %.0f;", number, haul.typical(), haul.budget()));
            }
        });
        assertTrue(off.isEmpty(), "off budget:" + off);
    }

    @Test
    void aPerfectRunEarnsWellAboveTheBudget() {
        hauls().forEach((number, haul) ->
                assertTrue(haul.perfect() >= 1.4 * haul.budget(), "Level " + number + ": perfect " + haul.perfect()));
    }

    @Test
    void thePerfectRunsAreTheLevelTestsTotals() {
        // The level tests count each source by hand; the README tables show the same totals.
        Map<Integer, Integer> perfect = new TreeMap<>();
        hauls().forEach((number, haul) -> perfect.put(number, haul.perfect()));
        assertEquals(
                Map.ofEntries(
                        Map.entry(1, 1_175),
                        Map.entry(2, 1_184),
                        Map.entry(3, 1_272),
                        Map.entry(4, 1_257),
                        Map.entry(5, 1_334),
                        Map.entry(6, 1_615),
                        Map.entry(7, 1_542),
                        Map.entry(8, 1_738),
                        Map.entry(9, 1_789),
                        Map.entry(10, 2_113),
                        Map.entry(11, 2_118)),
                perfect);
    }

    private Map<Integer, TypicalHaul> hauls() {
        Map<Integer, TypicalHaul> hauls = new TreeMap<>();
        for (String key : content.levels().keySet()) {
            hauls.put(Content.levelNumber(key), TypicalHaul.of(content, key));
        }
        return hauls;
    }
}
