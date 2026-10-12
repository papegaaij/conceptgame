package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M5 part H, the boss {@code sweep} (design/enemies, Boss attacks and root bursts; decision H5 = a): the
 * Mantis's laser sweep from the airborne Siege Spire's core: a 120° arc (hard 180°) centred on the ship's
 * bearing when its 0.6 s telegraph starts, then the beam sweeps it over 1 s, its direction alternating;
 * {@code laser} = 8 once per sweep on the ship and on Rook; every 5 s in turn with the rings of 14.
 */
class BossSweepTest {
    private static Sortie freeSpire(LevelScript.SetPieceSpec spire, Loadout loadout, Rules rules) {
        Sortie sortie = new Sortie(1, loadout, PartHSpecs.level(spire), rules, TestSpecs.FULL_ARMOUR);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        PartHSpecs.killRoots(sortie);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0);
        sortie.destroyPart(0, PartHSpecs.MAW);
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_LIFTED, PartHSpecs.LIFT_SECONDS + 0.2, 0);
        return sortie;
    }

    private static Sortie freeSpire(double arc, Rules rules) {
        // No Wraiths from the maw: only the core's attacks.
        return freeSpire(PartHSpecs.spire(0, 10, arc), TestSpecs.LOADOUT, rules);
    }

    @Test
    void theTelegraphCentresTheArcOnTheShipThenTheBeamSweepsIt() {
        Sortie sortie = freeSpire(120, TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = sortie.setPiece(0);
        assertEquals(
                List.of(PartHSpecs.SWEEP),
                PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 1.1, 0),
                "the sweep's turn first");
        int a = PartHSpecs.SWEEP;
        assertTrue(boss.sweepTelegraphing(a));
        assertFalse(boss.sweepBeaming(a));
        double ox = boss.sweepOriginX(a);
        double oy = boss.sweepOriginY(a);
        assertEquals(boss.partY(PartHSpecs.CORE) - 20, oy, 1e-9, "from below the core");
        double bearing = Math.atan2(-(sortie.ship().x() - ox), -(sortie.ship().y() - oy));
        assertEquals(bearing, (boss.sweepFrom(a) + boss.sweepTo(a)) / 2, 1e-9, "centred on the ship");
        assertEquals(Math.toRadians(120), Math.abs(boss.sweepTo(a) - boss.sweepFrom(a)), 1e-9);
        int steps = 0;
        List<Integer> beam = List.of();
        while (beam.isEmpty()) {
            sortie.step(0);
            steps++;
            beam = PartHSpecs.values(sortie, SimEvents.Type.SPIRE_SWEEP);
        }
        assertEquals(SimStep.ticks(0.6), steps, "a 0.6 s telegraph");
        assertTrue(boss.sweepBeaming(a));
        assertEquals(boss.sweepFrom(a), boss.sweepBeam(a, 0), 1e-9, "from its first edge");
        PartHSpecs.steps(sortie, 0.5);
        assertEquals(bearing, boss.sweepBeam(a, 0), 1e-9, "half-way: on the ship's bearing at the telegraph");
        PartHSpecs.steps(sortie, 0.5);
        assertFalse(boss.sweepBeaming(a), "over after 1 s");
    }

    @Test
    void theBeamHurtsTheShipOncePerSweepAndTheNextSweepsTheOtherWay() {
        Sortie sortie = freeSpire(120, TestSpecs.RULES);
        SetPiece boss = sortie.setPiece(0);
        int a = PartHSpecs.SWEEP;
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 1.1, 0);
        double direction = Math.signum(boss.sweepTo(a) - boss.sweepFrom(a));
        Defences defences = sortie.ship().defences();
        double before = defences.shield() + defences.armour();
        int hits = 0;
        for (int i = 0; i < SimStep.ticks(1.6); i++) {
            sortie.step(0);
            hits += PartHSpecs.values(sortie, SimEvents.Type.SWEEP_HIT).size();
        }
        assertEquals(1, hits, "once per sweep");
        assertEquals(before - 8, defences.shield() + defences.armour(), 1e-9, "`laser` = 8");
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 5, 0);
        assertEquals(-direction, Math.signum(boss.sweepTo(a) - boss.sweepFrom(a)), "the other way round");
    }

    @Test
    void theRingsOfFourteenComeInTurnEveryFiveSeconds() {
        Sortie sortie = freeSpire(120, TestSpecs.RULES.withInvulnerableShip());
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 1.1, 0);
        double sweep = sortie.realSeconds();
        PartHSpecs.until(sortie, SimEvents.Type.ENEMY_FIRED, 3, 0);
        assertEquals(2.5, sortie.realSeconds() - sweep, 1e-9, "the ring's turn after the sweep's");
        int ring = 0;
        SetPiece boss = sortie.setPiece(0);
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            // Fired this step, they moved on one step's flight (2 px at 120 px/s).
            if (Math.hypot(bullet.x() - boss.partX(PartHSpecs.CORE), bullet.y() - boss.partY(PartHSpecs.CORE)) < 3) {
                ring++;
            }
        }
        assertEquals(14, ring, "a ring of 14");
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 3, 0);
        assertEquals(5, sortie.realSeconds() - sweep, 1e-9, "a sweep every 5 s");
    }

    @Test
    void hardSweepsOneHundredAndEightyDegrees() {
        Sortie sortie = freeSpire(180, TestSpecs.RULES.withInvulnerableShip());
        SetPiece boss = sortie.setPiece(0);
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 1.1, 0);
        assertEquals(Math.PI, Math.abs(boss.sweepTo(PartHSpecs.SWEEP) - boss.sweepFrom(PartHSpecs.SWEEP)), 1e-9);
    }

    @Test
    void theBeamHurtsRookToo() {
        Sortie sortie = freeSpire(
                PartHSpecs.spire(0, 10, 120),
                TestSpecs.LOADOUT.withWingman(WingmanTest.rook(WingmanSpec.Side.LEFT)),
                TestSpecs.RULES);
        Wingman rook = sortie.wingman().orElseThrow();
        PartHSpecs.until(sortie, SimEvents.Type.SPIRE_SWEEP_TELEGRAPH, 1.1, 0);
        double armour = rook.armour();
        for (int i = 0; i < SimStep.ticks(1.6); i++) {
            sortie.step(0);
        }
        assertEquals(armour - 8, rook.armour(), 1e-9, "Rook beside the ship is swept once");
    }
}
