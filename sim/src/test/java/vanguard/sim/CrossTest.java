package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartGSpecs.runTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part G's formations and entries (design/enemies, formation vocabulary; design/campaign Level 13):
 * the {@code cross} from all four edges (each unit on its own lane, every edge with a unit warned), a
 * circle's centre {@code at}, a column's pairs (hard's t=210 Stingers) and a ground unit entering at a
 * side edge with a drift (the mortar floe).
 */
class CrossTest {
    /** A lane from each edge toward the middle: the top, the right, the bottom and the left. */
    private static final List<List<WaveSpec.At>> FOUR = List.of(
            List.of(new WaveSpec.At(120, -40), new WaveSpec.At(120, 600)),
            List.of(new WaveSpec.At(520, 200), new WaveSpec.At(-60, 200)),
            List.of(new WaveSpec.At(360, PlayField.HEIGHT + 40), new WaveSpec.At(360, -100)),
            List.of(new WaveSpec.At(-40, 350), new WaveSpec.At(540, 350)));

    private static Sortie sortie(List<WaveSpec> waves, List<LevelScript.GroundUnit> units) {
        return PartGSpecs.untouchable(PartGSpecs.level(Map.of(), waves, units));
    }

    private static WaveSpec cross(double t, EnemySpec enemy, int count) {
        return TestSpecs.wave(t, WaveSpec.Formation.CROSS, enemy, count, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
    }

    @Test
    void aCrossSendsAUnitFromEachEdgeTogetherEachOnItsLaneWithEveryEdgeWarned() {
        Sortie sortie = sortie(List.of(cross(60, PartGSpecs.SKIMMER, 4).withLanes(FOUR)), List.of());
        runTo(sortie, 57.5);
        int warned = sortie.edgeWarnings();
        assertTrue(WarningEdge.LEFT.in(warned) && WarningEdge.RIGHT.in(warned) && WarningEdge.BOTTOM.in(warned));
        runTo(sortie, 60);
        assertEquals(4, sortie.enemyCount(), "together");
        List<WaveSpec.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            assertTrue(sortie.enemy(i).laneWalking());
            entries.add(sortie.enemy(i).entry());
        }
        assertEquals(1, entries.stream().filter(e -> e == WaveSpec.Entry.FRONT).count());
        assertEquals(2, entries.stream().filter(e -> e == WaveSpec.Entry.SIDES).count());
        assertEquals(1, entries.stream().filter(e -> e == WaveSpec.Entry.REAR).count());
        assertEquals(0, sortie.edgeWarnings());
    }

    @Test
    void aCrossWithoutLanesFliesStraightAcrossFromEveryEdge() {
        Sortie sortie = sortie(List.of(cross(60, TestSpecs.SKITTER, 8)), List.of());
        runTo(sortie, 59);
        assertEquals(
                WarningEdge.LEFT.bit() | WarningEdge.RIGHT.bit() | WarningEdge.BOTTOM.bit(), sortie.edgeWarnings());
        runTo(sortie, 60);
        assertEquals(8, sortie.enemyCount());
        int above = 0;
        int below = 0;
        int left = 0;
        int right = 0;
        for (int i = 0; i < 8; i++) {
            Enemy unit = sortie.enemy(i);
            above += unit.y() > PlayField.HEIGHT ? 1 : 0;
            below += unit.y() < 0 ? 1 : 0;
            left += unit.x() < 0 ? 1 : 0;
            right += unit.x() > PlayField.WIDTH ? 1 : 0;
        }
        assertEquals(List.of(2, 2, 2, 2), List.of(above, right, below, left), "2 per edge");
    }

    @Test
    void aCrossNeedsAsManyUnitsPerEdgeAndALanePerUnit() {
        assertThrows(IllegalArgumentException.class, () -> sortie(List.of(cross(60, TestSpecs.SKITTER, 6)), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> sortie(List.of(cross(60, PartGSpecs.SKIMMER, 4).withLanes(FOUR.subList(0, 3))), List.of()));
    }

    @Test
    void aCircleOrbitsItsCentreAt() {
        WaveSpec circle = new WaveSpec(
                        60,
                        WaveSpec.Formation.CIRCLE,
                        TestSpecs.NEEDLER,
                        6,
                        WaveSpec.Entry.FRONT,
                        WaveSpec.Edge.NONE,
                        Optional.of(6.0),
                        Optional.empty(),
                        6,
                        Optional.empty(),
                        Optional.empty(),
                        List.of())
                .withAt(new WaveSpec.At(240, 150));
        Sortie sortie = sortie(List.of(circle), List.of());
        runTo(sortie, 64);
        double centreY = PlayField.HEIGHT - 150;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy unit = sortie.enemy(i);
            assertEquals(90, Math.hypot(unit.x() - 240, unit.y() - centreY), 0.5, "on the orbit round (240, 390)");
        }
        assertEquals(6, sortie.enemyCount());
    }

    @Test
    void aColumnInPairsEntersTwoSideBySideThePairsApart() {
        WaveSpec column = TestSpecs.wave(
                        60,
                        WaveSpec.Formation.COLUMN,
                        GroundAndDiveTest.STINGER,
                        4,
                        WaveSpec.Entry.FRONT,
                        WaveSpec.Edge.NONE)
                .withPairs(0.4);
        Sortie sortie = sortie(List.of(column), List.of());
        runTo(sortie, 60);
        assertEquals(2, sortie.enemyCount(), "the first pair");
        assertEquals(50, Math.abs(sortie.enemy(0).x() - sortie.enemy(1).x()), 1e-9);
        assertEquals(240, (sortie.enemy(0).x() + sortie.enemy(1).x()) / 2, 1e-9);
        runTo(sortie, 60.38);
        assertEquals(2, sortie.enemyCount());
        runTo(sortie, 60.41);
        assertEquals(4, sortie.enemyCount(), "the second pair 0.4 s later");
    }

    @Test
    void aGroundUnitFromASideDriftsAcrossWarnedAhead() {
        LevelScript.GroundUnit floe = LevelScript.GroundUnit.fromSide(
                66, CraterNestTest.MORTAR, -1, new LevelScript.SideEntry(true, 400, 10));
        Sortie sortie = sortie(List.of(), List.of(floe));
        runTo(sortie, 63.5);
        assertEquals(WarningEdge.LEFT.bit(), sortie.edgeWarnings());
        runTo(sortie, 66);
        assertEquals(1, sortie.enemyCount());
        Enemy mortar = sortie.enemy(0);
        assertTrue(mortar.grounded());
        assertEquals(WaveSpec.Entry.SIDES, mortar.entry());
        assertEquals(400, mortar.y(), 1e-9);
        double x = mortar.x();
        PartGSpecs.run(sortie, 2);
        assertEquals(x + 20, mortar.x(), 1e-6, "10 px/s across, in the halt nothing else");
        assertEquals(400, mortar.y(), 1e-9);
        assertFalse(WarningEdge.LEFT.in(sortie.edgeWarnings()));
    }

    @Test
    void aGroundUnitShotOnItsFloeHandsItsDriftToItsRemains() {
        // Step G8: the mortars' wreck stayed at the left edge while their floe drifted on.
        LevelScript.GroundUnit floe = LevelScript.GroundUnit.fromSide(
                66, CraterNestTest.MORTAR, -1, new LevelScript.SideEntry(true, 400, 10));
        Sortie sortie = sortie(List.of(), List.of(floe));
        runTo(sortie, 68);
        double x = sortie.enemy(0).x();
        sortie.destroyEnemy(0);

        SimEvents events = sortie.events();
        int destroyed = -1;
        for (int i = 0; i < events.size(); i++) {
            destroyed = events.type(i) == SimEvents.Type.ENEMY_DESTROYED ? i : destroyed;
        }
        assertTrue(destroyed >= 0 && destroyed + 1 < events.size());
        assertEquals(SimEvents.Type.WRECK_DRIFT, events.type(destroyed + 1), "right after its death");
        assertEquals(x, events.x(destroyed + 1), 1e-9);
        assertEquals(10, SimEvents.driftX(events.value(destroyed + 1)), 1e-9);
        assertEquals(0, SimEvents.driftY(events.value(destroyed + 1)), 1e-9);
    }

    @Test
    void aDriftValueKeepsBothDirectionsToATenth() {
        int value = SimEvents.driftValue(-10, 3.25);
        assertEquals(-10, SimEvents.driftX(value), 1e-9);
        assertEquals(3.3, SimEvents.driftY(value), 1e-9);
        assertEquals(0, SimEvents.driftX(SimEvents.driftValue(0, -7)), 1e-9);
        assertEquals(-7, SimEvents.driftY(SimEvents.driftValue(0, -7)), 1e-9);
    }
}
