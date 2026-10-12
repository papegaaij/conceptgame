package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M5 part H, the anchored boss without lanes and its lift (design/enemies/bosses/siege-spire; decisions
 * H1 and H4 = a): all six roots on the field from the halt, the phases on the roots and the maw, then the
 * lift: invulnerable and holding its fire for its 3 s, eased off the anchor to its station in the upper
 * half, the core onto {@code air} at its end, the stump left at the anchor, an armour patch at its start;
 * then the sway of ±100 px at up to 50 px/s.
 */
class SpireLiftTest {
    private static SetPiece boss(Sortie sortie) {
        return sortie.setPiece(0);
    }

    @Test
    void allSixRootsAreOnTheFieldFromTheHaltAndTheCoreAndTheMawWaitForTheirPhases() {
        Sortie sortie = PartHSpecs.sortie(PartHSpecs.level(PartHSpecs.spire()), TestSpecs.RULES);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        SetPiece boss = boss(sortie);
        assertEquals(PartHSpecs.HOVER_Y, boss.y(), 1e-6, "anchored 320 px above the bottom edge");
        assertTrue(boss.arena().isPresent());
        assertEquals(0, boss.arena().get().laneCount(), "no slam lanes");
        for (int p : PartHSpecs.ROOTS) {
            assertTrue(boss.partOnField(p), "root " + p + " on the field");
            assertFalse(boss.partShielded(p));
            assertEquals(Layer.GROUND, boss.partLayer(p));
        }
        assertTrue(boss.partShielded(PartHSpecs.MAW), "the maw from phase 2");
        assertTrue(boss.partShielded(PartHSpecs.CORE), "the core from phase 3");
        assertTrue(boss.partArmoured(PartHSpecs.TRUNK));
        assertFalse(boss.onPlane(), "on the ground: no contact");
        PartHSpecs.killRoots(sortie);
        assertEquals(List.of(1), PartHSpecs.until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0));
        assertFalse(boss.partShielded(PartHSpecs.MAW), "the maw exposed from the phase's start");
    }

    @Test
    void theLiftIsInvulnerableEasesOffTheAnchorAndFlipsTheCoreToAirAtItsEnd() {
        Sortie sortie = PartHSpecs.atLift(TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = boss(sortie);
        assertEquals(List.of(2), PartHSpecs.values(sortie, SimEvents.Type.SPIRE_LIFT), "the tear-free shockwave");
        assertTrue(boss.lifted());
        assertTrue(boss.lifting());
        boolean patch = false;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            patch |= sortie.pickup(i).type() == PickupType.ARMOUR_PATCH;
        }
        assertTrue(patch, "the shockwave throws up an armour patch");
        double y = boss.y();
        int steps = 0;
        while (boss.lifting()) {
            assertTrue(boss.partShielded(PartHSpecs.CORE), "invulnerable while it lifts");
            assertEquals(Layer.GROUND, boss.partLayer(PartHSpecs.CORE));
            assertFalse(boss.onPlane(), "no contact while it lifts");
            assertTrue(PartHSpecs.values(sortie, SimEvents.Type.ENEMY_FIRED).isEmpty(), "it holds its fire");
            assertTrue(boss.y() >= y - 1e-9, "it rises");
            y = boss.y();
            sortie.step(0);
            steps++;
        }
        assertEquals(SimStep.ticks(PartHSpecs.LIFT_SECONDS), steps, "a 3 s lift");
        assertEquals(List.of(2), PartHSpecs.values(sortie, SimEvents.Type.SPIRE_LIFTED));
        assertEquals(PartHSpecs.FREE_X, boss.x(), 1e-6);
        assertEquals(PartHSpecs.FREE_Y, boss.y(), 1e-6, "its station in the upper half");
        assertEquals(1, boss.liftShare(1), 1e-9);
        assertEquals(Layer.AIR, boss.layer());
        assertEquals(Layer.AIR, boss.partLayer(PartHSpecs.CORE), "the core on air");
        assertTrue(boss.onPlane(), "a ship touching it takes contact damage");
        assertEquals("free", boss.poseName());
        assertFalse(boss.partShielded(PartHSpecs.CORE));
        assertEquals(boss.x(), boss.partX(PartHSpecs.CORE), 1e-9, "the core lifted with it");
    }

    @Test
    void theStumpStaysAtTheAnchorAndShotsFlyOverIt() {
        Sortie sortie = PartHSpecs.atLift(TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = boss(sortie);
        PartHSpecs.steps(sortie, PartHSpecs.LIFT_SECONDS + 1);
        assertEquals(PlayField.WIDTH / 2.0, boss.stumpX(), 1e-9);
        assertEquals(PartHSpecs.HOVER_Y, boss.stumpY(), 1e-6, "the ground is halted");
        for (int p : List.of(PartHSpecs.TRUNK, PartHSpecs.MAW, PartHSpecs.POD_1, PartHSpecs.MORTAR_RIGHT)) {
            assertTrue(boss.partStays(p), "part " + p + " stays with the stump");
        }
        assertFalse(boss.partStays(PartHSpecs.CORE));
        assertEquals(boss.stumpX(), boss.partX(PartHSpecs.TRUNK), 1e-9);
        assertEquals(boss.stumpY() + 90, boss.partY(PartHSpecs.TRUNK), 1e-9, "the trunk where it stood");
        assertEquals(Layer.GROUND, boss.partLayer(PartHSpecs.TRUNK));
        assertFalse(
                boss.partTouches(
                        PartHSpecs.TRUNK,
                        new Hitbox(4, 12),
                        boss.partX(PartHSpecs.TRUNK),
                        boss.partY(PartHSpecs.TRUNK)),
                "the stump is scenery");
    }

    @Test
    void theFreeSpireSwaysAHundredPixelsEachSideAtFiftyPixelsASecondAtMost() {
        Sortie sortie = PartHSpecs.atLift(TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = boss(sortie);
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_LIFTED, PartHSpecs.LIFT_SECONDS + 0.1, 0);
        double least = boss.x();
        double most = boss.x();
        double fastest = 0;
        double x = boss.x();
        for (int i = 0; i < SimStep.ticks(PartHSpecs.SWAY_PERIOD + 0.5); i++) {
            sortie.step(0);
            least = Math.min(least, boss.x());
            most = Math.max(most, boss.x());
            fastest = Math.max(fastest, Math.abs(boss.x() - x) / SimStep.SECONDS);
            x = boss.x();
            assertEquals(PartHSpecs.FREE_Y, boss.y(), 1e-9);
        }
        assertEquals(PartHSpecs.FREE_X - 100, least, 0.5);
        assertEquals(PartHSpecs.FREE_X + 100, most, 0.5);
        assertTrue(fastest <= 50 + 1e-6, "50 px/s at most: " + fastest);
    }

    @Test
    void theStumpScrollsAwayWithTheGroundAfterTheKill() {
        Sortie sortie = PartHSpecs.atLift(TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = boss(sortie);
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_LIFTED, PartHSpecs.LIFT_SECONDS + 0.1, 0);
        sortie.destroyPart(0, PartHSpecs.CORE);
        assertTrue(boss.destroyed());
        PartHSpecs.steps(sortie, 2);
        assertTrue(sortie.groundSpeed() > 0, "the scroll runs on after the arena's jump");
        assertTrue(boss.stumpY() < PartHSpecs.HOVER_Y - 50, "the stump scrolls with the ground: " + boss.stumpY());
    }

    @Test
    void retryFromTheBossBringsItBackRooted() {
        Sortie sortie = PartHSpecs.atLift(TestSpecs.RULES.withInvulnerableShip());
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_LIFTED, PartHSpecs.LIFT_SECONDS + 0.1, 0);
        sortie.retryFromBoss();
        sortie.step(0);
        SetPiece boss = boss(sortie);
        assertFalse(boss.lifted());
        assertEquals(0, boss.phase());
        assertEquals(Layer.GROUND, boss.partLayer(PartHSpecs.CORE));
        assertFalse(boss.partStays(PartHSpecs.TRUNK));
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 2, 0);
        assertEquals(PartHSpecs.HOVER_Y, boss.y(), 1e-6, "rooted at the same place");
    }

    @Test
    void theWholeFightIsDeterministicAndAllocatesNothing() {
        Sortie a = PartHSpecs.sortie(PartHSpecs.level(PartHSpecs.spire(3, 12, 180)), TestSpecs.RULES);
        Sortie b = PartHSpecs.sortie(PartHSpecs.level(PartHSpecs.spire(3, 12, 180)), TestSpecs.RULES);
        for (Sortie sortie : List.of(a, b)) {
            fight(sortie);
        }
        assertEquals(a.stateHash(), b.stateHash());
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = PartHSpecs.sortie(
                            PartHSpecs.level(PartHSpecs.spire(3, 12, 180)), TestSpecs.RULES.withInvulnerableShip());
                    PartHSpecs.steps(sortie, 4);
                    return sortie;
                },
                SpireLiftTest::fight);
        assertEquals(0, allocated, "the lobs, the launches, the lift and the sweeps allocate nothing per step");
    }

    /** 40 s of the fight: the roots for 8 s, the maw (two launches) for 12 s, the lift and the free spire. */
    private static void fight(Sortie sortie) {
        for (int i = 0; i < SimStep.ticks(40); i++) {
            if (i == SimStep.ticks(8)) {
                PartHSpecs.killRoots(sortie);
            }
            if (i == SimStep.ticks(20) && !boss(sortie).partWrecked(PartHSpecs.MAW)) {
                sortie.destroyPart(0, PartHSpecs.MAW);
            }
            sortie.step(Command.FIRE.bit() | ((i / 40) % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
        }
    }
}
