package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartGSpecs.count;
import static vanguard.sim.PartGSpecs.lanes;
import static vanguard.sim.PartGSpecs.runTo;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * M5 part G's Skimmer (design/enemies/naval/skimmer, user decision G4 = a): a lane walker that follows
 * its wave's lane in ground space at 180 px/s with its 40 px / 1.2 s weave across it, faces its
 * velocity over the ground, enters from any edge (the sides and the rear warned), fires its aimed shot
 * on the screen and flips and sinks when destroyed.
 */
class SkimmerTest {
    private static final double HALF_STEP = 1e-6;

    private static List<WaveSpec.At> lane(double... xDepth) {
        List<WaveSpec.At> points = new java.util.ArrayList<>();
        for (int i = 0; i < xDepth.length; i += 2) {
            points.add(new WaveSpec.At(xDepth[i], xDepth[i + 1]));
        }
        return points;
    }

    private static Sortie sortie(WaveSpec wave) {
        return PartGSpecs.untouchable(PartGSpecs.level(Map.of(), List.of(wave), List.of()));
    }

    @Test
    void itFollowsItsLaneAtItsSpeedFacingAlongIt() {
        WaveSpec wave = lanes(
                50, WaveSpec.Formation.SINGLE, PartGSpecs.straightSkimmer(), 1, List.of(lane(-40, 300, 520, 300)));
        Sortie sortie = sortie(wave);
        runTo(sortie, 50);
        assertEquals(1, sortie.enemyCount());
        Enemy skimmer = sortie.enemy(0);
        assertTrue(skimmer.laneWalking());
        PartGSpecs.run(sortie, 1);
        // 61 steps: the step it entered in moved it too.
        assertEquals(-40 + 183, skimmer.x(), 0.01, "180 px/s along the lane");
        assertEquals(PlayField.HEIGHT - 300, skimmer.y(), HALF_STEP);
        assertEquals(-Math.PI / 2, skimmer.facing(), 1e-9, "facing right, along its velocity");
    }

    @Test
    void itWeavesAcrossItsLaneAndFacesItsVelocity() {
        WaveSpec wave = lanes(50, WaveSpec.Formation.SINGLE, PartGSpecs.SKIMMER, 1, List.of(lane(-40, 300, 1000, 300)));
        Sortie sortie = sortie(wave);
        runTo(sortie, 50);
        Enemy skimmer = sortie.enemy(0);
        double most = 0;
        double turned = 0;
        for (int i = 0; i < SimStep.ticks(2.4) && sortie.enemyCount() > 0; i++) {
            sortie.step(Command.NONE);
            double across = (PlayField.HEIGHT - 300) - skimmer.y();
            most = Math.max(most, Math.abs(across));
            turned = Math.max(turned, Math.abs(skimmer.facing() + Math.PI / 2));
        }
        assertEquals(40, most, 0.5, "40 px either side of its lane");
        assertTrue(turned > Math.toRadians(30), "its heading swings with the weave: " + Math.toDegrees(turned));
    }

    @Test
    void itsLaneScrollsWithTheGroundInTheApproach() {
        WaveSpec wave =
                lanes(5, WaveSpec.Formation.SINGLE, PartGSpecs.straightSkimmer(), 1, List.of(lane(-40, 100, 520, 100)));
        Sortie sortie = sortie(wave);
        runTo(sortie, 5);
        Enemy skimmer = sortie.enemy(0);
        double y = skimmer.y();
        PartGSpecs.run(sortie, 1);
        assertEquals(y - PartGSpecs.APPROACH, skimmer.y(), 0.01, "the lane moves down with the ground");
        assertEquals(-40 + 183, skimmer.x(), 0.01);
        assertEquals(-Math.PI / 2, skimmer.facing(), 1e-9, "its heading is over the ground, not the screen");
    }

    @Test
    void itEntersFromTheRearWarnedAndLeavesAtTheOtherEnd() {
        WaveSpec rear = TestSpecs.wave(
                        60,
                        WaveSpec.Formation.SINGLE,
                        PartGSpecs.straightSkimmer(),
                        1,
                        WaveSpec.Entry.REAR,
                        WaveSpec.Edge.NONE)
                .withLanes(List.of(lane(200, PlayField.HEIGHT + 40, 200, -100)));
        Sortie sortie = sortie(rear);
        runTo(sortie, 58);
        assertTrue(WarningEdge.BOTTOM.in(sortie.edgeWarnings()), "the rear edge is warned ahead");
        runTo(sortie, 60.02);
        assertEquals(1, sortie.enemyCount(), "below the bottom edge it is on its way in");
        assertEquals(WaveSpec.Entry.REAR, sortie.enemy(0).entry());
        assertTrue(sortie.enemy(0).y() < 0);
        runTo(sortie, 64);
        assertEquals(0, sortie.enemyCount(), "gone off the top edge");
    }

    @Test
    void itFiresItsAimedShotOnTheScreenEveryIntervalAndSinksWhenDestroyed() {
        WaveSpec wave = lanes(
                50, WaveSpec.Formation.SINGLE, PartGSpecs.straightSkimmer(), 1, List.of(lane(-40, 300, 520, 300)));
        Sortie sortie = sortie(wave);
        runTo(sortie, 50);
        int fired = 0;
        while (sortie.levelSeconds() < 52.7) {
            sortie.step(Command.NONE);
            fired += count(sortie, SimEvents.Type.ENEMY_FIRED);
        }
        // Its first shot 0.5 s after it came onto the screen, then one every 1.8 s.
        assertEquals(2, fired);
        double facing = sortie.enemy(0).facing();
        sortie.destroyEnemy(0);
        assertEquals(1, count(sortie, SimEvents.Type.LANE_WALKER_DOWN));
        int value = sortie.events().value(0);
        for (int i = 0; i < sortie.events().size(); i++) {
            if (sortie.events().type(i) == SimEvents.Type.LANE_WALKER_DOWN) {
                value = sortie.events().value(i);
            }
        }
        assertEquals(sortie.enemyKinds().indexOf(PartGSpecs.straightSkimmer()), SimEvents.walkerKind(value));
        double turn = 2 * Math.PI;
        assertEquals(((facing % turn) + turn) % turn, SimEvents.walkerFacing(value), 0.002);
    }

    @Test
    void onlyAGroundUnitThatIsNotAWalkerFollowsLanes() {
        WaveSpec air = lanes(5, WaveSpec.Formation.SINGLE, TestSpecs.NEEDLER, 1, List.of(lane(-40, 100, 520, 100)));
        assertThrows(IllegalArgumentException.class, () -> sortie(air));
        WaveSpec walker =
                lanes(5, WaveSpec.Formation.SINGLE, PartGSpecs.SCUTTLER, 1, List.of(lane(-40, 100, 520, 100)));
        assertThrows(IllegalArgumentException.class, () -> sortie(walker));
    }

    @Test
    void laneWalkersDoNotAllocate() {
        WaveSpec convoy = lanes(
                42, WaveSpec.Formation.CONVOY, PartGSpecs.SKIMMER, 4, List.of(lane(-40, 300, 260, 200, 520, 300)));
        LevelScript level =
                PartGSpecs.level(Map.of("skimmer", LevelScript.HookMode.ALTERNATE), List.of(convoy), List.of());
        long allocated = Allocations.least(() -> PartGSpecs.untouchable(level), sortie -> {
            for (int i = 0; i < SimStep.ticks(50); i++) {
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }
}
