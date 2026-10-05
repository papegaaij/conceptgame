package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.SKITTER;
import static vanguard.sim.TestSpecs.level;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;
import static vanguard.sim.WaveSpec.Formation.LINE_ABREAST;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Okafor's low-armour radio line (design/player/armor, Low-armour warnings) in a sortie: set off once
 * per attempt by the first armour hit to 15 % or below, and again in the next attempt.
 */
class LowArmourLineTest {
    /** Skitters ramming the ship (3 armour each through the shield's half) until it is a wreck. */
    private static Sortie rammed(double armour) {
        List<WaveSpec> waves = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            waves.add(wave(0.3 * i, LINE_ABREAST, SKITTER, 1, FRONT, NONE));
        }
        return new Sortie(1, TestSpecs.LOADOUT, level(30, waves), TestSpecs.RULES, armour);
    }

    /** Steps until the ship is a wreck; returns the line's events and the armour each one left. */
    private static List<Double> flyToTheWreck(Sortie sortie) {
        List<Double> lines = new ArrayList<>();
        int steps = 0;
        do {
            sortie.step(Command.NONE);
            for (int i = 0; i < sortie.events().size(); i++) {
                if (sortie.events().type(i) == SimEvents.Type.ARMOUR_CRITICAL) {
                    lines.add(sortie.ship().defences().armour());
                }
            }
            assertTrue(++steps < 30 * SimStep.PER_SECOND, "the Skitters should wear the ship down");
        } while (sortie.flying());
        return lines;
    }

    @Test
    void theLineComesOncePerAttemptAtFifteenPercent() {
        Sortie sortie = rammed(20);

        List<Double> first = flyToTheWreck(sortie);

        assertEquals(1, first.size(), "once in the attempt: " + first);
        assertTrue(first.getFirst() <= 0.15 * 60 && first.getFirst() > 9 - 3, "the first hit to 9 or below");

        for (int i = 0; i < 10 * SimStep.PER_SECOND && !sortie.flying(); i++) {
            sortie.step(Command.NONE);
        }
        sortie.retry(20);
        List<Double> second = flyToTheWreck(sortie);

        assertEquals(2, sortie.attempt());
        assertEquals(1, second.size(), "the retry is a new attempt: " + second);
    }

    @Test
    void aShipWithItsArmourAboveFifteenPercentHearsNothing() {
        Sortie sortie = rammed(60);
        int lines = 0;
        for (int i = 0; i < 2 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.NONE);
            lines += sortie.events().count(SimEvents.Type.ARMOUR_CRITICAL);
        }

        assertTrue(sortie.ship().defences().armour() > 9, "still above 15 %");
        assertEquals(0, lines);
    }
}
