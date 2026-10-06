package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.level;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The simulation's side of the M5 utility modules (design/player/systems): the Salvage scanner adds
 * its share to salvage pickups and hidden crates before the one rounding, and nothing else; an
 * enemy counts the steps since its last hit for the Targeting computer's HP bar, outside the state
 * hash. Neither allocates per step.
 */
class UtilityEffectsTest {
    /** A magnet that takes every pickup on the field at once, so the crates are collected. */
    private static final Magnet EVERYWHERE = new Magnet(2000, 20_000);

    private static Sortie sortie(LevelScript level, double salvageBonus) {
        return new Sortie(
                1,
                TestSpecs.LOADOUT.withMagnet(EVERYWHERE).withSalvage(salvageBonus),
                level,
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
    }

    @Test
    void theScannerAddsItsShareToSalvagePickups() {
        // Small 10 and medium 50 credits (TestSpecs.RULES) at a credit factor of 1, each rounded on its own.
        int[] expected = {60, 66, 72};
        double[] bonus = {0, 0.1, 0.2};
        for (int k = 0; k < bonus.length; k++) {
            Sortie sortie = sortie(level(20, List.of()), bonus[k]);
            sortie.step(Command.NONE);
            Ship ship = sortie.ship();
            sortie.drop(PickupType.SMALL_SALVAGE, ship.x(), ship.y() + 50);
            sortie.drop(PickupType.MEDIUM_SALVAGE, ship.x(), ship.y() + 50);
            sortie.drop(PickupType.SHIELD_CELL, ship.x(), ship.y() + 50);
            for (int i = 0; i < 10; i++) {
                sortie.step(Command.NONE);
            }
            assertEquals(0, sortie.pickupCount());
            assertEquals(expected[k], sortie.credits(), "bonus " + bonus[k]);
        }
    }

    @Test
    void theScannerAddsItsShareToASecretsHiddenCrateButNotToBounties() {
        var beacon = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(12, 12), 0, 0, Optional.empty(), 3, 80, "beacon cache", false);
        int[] expected = {80, 88, 96};
        double[] bonus = {0, 0.1, 0.2};
        long[] scores = new long[bonus.length];
        for (int k = 0; k < bonus.length; k++) {
            Sortie sortie = sortie(level(20, List.of(), List.of(beacon), List.of()), bonus[k]);
            for (int i = 0; i < 3 * SimStep.PER_SECOND; i++) {
                sortie.step(Command.FIRE.bit());
            }
            assertTrue(sortie.groundObject(0).spent());
            assertEquals(0, sortie.pickupCount(), "the crate was collected");
            assertEquals(expected[k], sortie.credits(), "bonus " + bonus[k]);
            scores[k] = sortie.score();
        }
        assertEquals(scores[0], scores[2], "the score counts the crate's plain value");
    }

    @Test
    void anEnemyCountsTheStepsSinceItsLastHit() {
        var sortie = new Sortie(1, TestSpecs.LOADOUT, SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        int hitThisStep = 0;
        for (int i = 0; i < 1800; i++) {
            sortie.step(SortieTest.Pilot.commands(i));
            for (int e = 0; e < sortie.enemyCount(); e++) {
                Enemy enemy = sortie.enemy(e);
                int since = enemy.ticksSinceHit();
                boolean damaged = enemy.hp() < enemy.spec().hp();
                assertEquals(damaged, since != Integer.MAX_VALUE, "hit once it lost HP: step " + i);
                if (since == 1) {
                    hitThisStep++;
                }
            }
        }
        assertTrue(hitThisStep > 0, "some unit survived a hit");
    }

    @Test
    void steppingWithAScannerDoesNotAllocate() {
        long allocated = Allocations.least(
                () -> {
                    var sortie = new Sortie(
                            1,
                            TestSpecs.LOADOUT.withSalvage(0.2),
                            SortieTest.mixedLevel(),
                            TestSpecs.RULES,
                            TestSpecs.FULL_ARMOUR);
                    for (int i = 0; i < 600; i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                    return sortie;
                },
                sortie -> {
                    for (int i = 600; i < 600 + 3600; i++) {
                        if (i % 30 == 0) {
                            Ship ship = sortie.ship();
                            sortie.drop(PickupType.SMALL_SALVAGE, ship.x(), ship.y() + 20);
                        }
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "3600 steps allocated " + allocated + " bytes");
    }

    @Test
    void withoutAScannerTheSortieHashesAsBefore() {
        Sortie plain =
                new Sortie(1, TestSpecs.LOADOUT, SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        Sortie zero = new Sortie(
                1, TestSpecs.LOADOUT.withSalvage(0), SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        for (int i = 0; i < 1800; i++) {
            plain.step(SortieTest.Pilot.commands(i));
            zero.step(SortieTest.Pilot.commands(i));
        }
        assertEquals(plain.stateHash(), zero.stateHash());
        assertEquals(plain.credits(), zero.credits());
    }

    /**
     * Boss bounties follow the economy's rule (design/systems/economy: data holds Act 1 terms): the
     * act factor applies to every payout, a boss part's included, with no exemption. The Harbour
     * Kraken's 300 credits in Act 2 are written 188 and pay 301.
     */
    @Test
    void aBossPartsBountyIsActScaledLikeEveryOther() {
        ScoringRules act2 = new ScoringRules(
                TestSpecs.RULES.scoring().killScore(),
                TestSpecs.RULES.scoring().pickupScore(),
                TestSpecs.RULES.scoring().chain(),
                1,
                1.6,
                TestSpecs.RULES.scoring().weights(),
                TestSpecs.RULES.scoring().bonuses(),
                TestSpecs.RULES.scoring().grades(),
                1);
        Tally tally = new Tally(act2);
        assertEquals(301, tally.partKill(188));
        assertEquals(301, tally.credits(CreditSource.KILLS));
        assertEquals(8, tally.kill(5), "a Skitter's 5 × 1.6");
    }
}
