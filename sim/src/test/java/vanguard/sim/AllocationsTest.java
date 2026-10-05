package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The allocation measurement the "does not allocate" tests share: it still catches what the work
 * itself allocates, per step or once per run, and ignores what happens in one run only (the JVM's
 * one-offs, such as the strings it resolves when it queues a method for the optimising compiler).
 */
class AllocationsTest {
    /** Where the deliberate allocations go, so that escape analysis cannot remove them. */
    static volatile Object sink;

    private static final int STEPS = 600;

    private static Sortie sortie() {
        return new Sortie(3, TestSpecs.LOADOUT, SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    @Test
    void anAllocationInEveryStepIsCaught() {
        long allocated = Allocations.least(AllocationsTest::sortie, sortie -> {
            for (int i = 0; i < STEPS; i++) {
                sortie.step(SortieTest.Pilot.commands(i));
                sink = new int[1];
            }
        });

        assertTrue(allocated >= 16L * STEPS, "a step's allocation shows in every run: " + allocated);
    }

    @Test
    void aSingleSmallAllocationPerRunIsCaught() {
        // As small as the list iterator a for-each once allocated when a group was cleared.
        long allocated = Allocations.least(AllocationsTest::sortie, sortie -> {
            for (int i = 0; i < STEPS; i++) {
                sortie.step(SortieTest.Pilot.commands(i));
                if (i == STEPS / 2) {
                    sink = new Object();
                }
            }
        });

        assertTrue(allocated > 0, "one allocation per run shows: " + allocated);
    }

    @Test
    void aOneOffInASingleRunIsIgnored() {
        // Like the JVM resolving a class's strings in the second run, which broke CI on macOS.
        int[] runs = {0};
        long allocated = Allocations.least(
                () -> {
                    runs[0]++;
                    return sortie();
                },
                sortie -> {
                    for (int i = 0; i < STEPS; i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                    if (runs[0] == 2) {
                        sink = new byte[4096];
                    }
                });

        assertEquals(0, allocated);
        assertTrue(runs[0] >= 3, "measured past the one-off: " + runs[0] + " runs");
    }
}
