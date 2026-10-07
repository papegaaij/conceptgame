package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.LevelClockSpecs.tagged;
import static vanguard.sim.PartCSpecs.count;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A secondary {@code escapes} objective scoped to a wave tag (design/campaign Level 09, "Hold the
 * bridge"; M5 part C user decision D6 = a): only the units of the tagged waves count, met when all
 * of them are destroyed, failed when one leaves the screen alive; the enemy's other units may get
 * away.
 */
class ScopedSecondaryTest {
    private static WaveSpec column(double t) {
        return TestSpecs.wave(
                t, WaveSpec.Formation.COLUMN, TestSpecs.SKITTER, 2, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
    }

    private static Sortie sortie() {
        LevelScript level = LevelClockSpecs.level(
                30,
                150,
                List.of(tagged(column(1), "bridge"), column(3), tagged(column(5), "bridge")),
                List.of(),
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "skitter", List.of(), "", "", List.of(), -1, "bridge"),
                List.of(),
                List.of(),
                Optional.empty());
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    @Test
    void itCountsOnlyTheTaggedWavesUnits() {
        Sortie sortie = sortie();

        assertTrue(sortie.secondaryByEscapes());
        assertEquals(4, sortie.escapesTotal(), "two tagged columns of two, not the untagged one");
    }

    @Test
    void itIsMetWhenEveryTaggedUnitIsDestroyedThoughOthersGetAway() {
        Sortie sortie = sortie();
        int met = 0;
        for (int step = 0; step < SimStep.ticks(25); step++) {
            sortie.step(Command.NONE);
            for (int i = sortie.enemyCount() - 1; i >= 0; i--) {
                Enemy enemy = sortie.enemy(i);
                if (enemy.tag().equals("bridge") && PlayField.overlaps(enemy.x(), enemy.y(), enemy.hitbox())) {
                    sortie.destroyEnemy(i);
                }
            }
            // The step's events and the kills' after it, until the next step clears them.
            met += count(sortie, SimEvents.Type.OBJECTIVE_MET);
        }

        assertEquals(4, sortie.escapesDestroyed());
        assertTrue(sortie.secondaryMet());
        assertFalse(sortie.secondaryFailed(), "the untagged column got away");
        assertEquals(1, met, "met once");
    }

    @Test
    void itFailsWhenATaggedUnitGetsAway() {
        Sortie sortie = sortie();
        int failed = 0;
        for (int step = 0; step < SimStep.ticks(25); step++) {
            sortie.step(Command.NONE);
            failed += count(sortie, SimEvents.Type.OBJECTIVE_FAILED);
        }

        assertTrue(sortie.secondaryFailed());
        assertFalse(sortie.secondaryMet());
        assertEquals(1, failed);
    }
}
