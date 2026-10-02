package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.NEEDLER;
import static vanguard.sim.TestSpecs.SKITTER;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.ALTERNATING;
import static vanguard.sim.WaveSpec.Edge.LEFT;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;
import static vanguard.sim.WaveSpec.Entry.REAR;
import static vanguard.sim.WaveSpec.Entry.SIDES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FormationsTest {
    private static List<Spawn> plan(WaveSpec wave) {
        List<Spawn> spawns = new ArrayList<>();
        Formations.plan(wave, 0, new SplitMix64(1), spawns);
        return spawns;
    }

    private static WaveSpec held(
            WaveSpec.Formation formation,
            EnemySpec enemy,
            int count,
            WaveSpec.Entry entry,
            double hold,
            int breakGroup) {
        return new WaveSpec(
                10,
                formation,
                enemy,
                count,
                entry,
                NONE,
                Optional.of(hold),
                Optional.empty(),
                breakGroup,
                Optional.empty(),
                Optional.empty(),
                List.of());
    }

    private static double endX(Spawn spawn) {
        FlightPath path = spawn.path();
        return path.x(path.segmentAt(path.length(), 0), path.length());
    }

    private static double endY(Spawn spawn) {
        FlightPath path = spawn.path();
        return path.y(path.segmentAt(path.length(), 0), path.length());
    }

    private static double startY(Spawn spawn) {
        return spawn.path().y(0, 0);
    }

    @Test
    void aSnakeFollowsOnePathWithItsUnitsTheSpacingApart() {
        List<Spawn> snake = plan(wave(10, WaveSpec.Formation.SNAKE, SKITTER, 6, FRONT, LEFT));

        assertEquals(6, snake.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(SimStep.ticks(10 + 0.25 * i), snake.get(i).tick());
            assertSame(snake.get(0).path(), snake.get(i).path());
            assertEquals(190, snake.get(i).speed());
            assertEquals(0, snake.get(i).holdSeconds(), "gone at the end of the path");
        }
        assertTrue(startY(snake.getFirst()) > PlayField.HEIGHT, "enters from off-screen");
    }

    @Test
    void aWaveSpeedReplacesTheEnemysOwn() {
        var slow = new WaveSpec(
                75,
                WaveSpec.Formation.SNAKE,
                SKITTER,
                6,
                SIDES,
                LEFT,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.of(120.0),
                Optional.empty(),
                List.of());

        Spawn first = plan(slow).getFirst();

        assertEquals(120, first.speed());
        assertTrue(first.path().x(0, 0) < 0, "a side snake enters through the left edge");
    }

    @Test
    void aVWingHoversInTheHoverBandLedByItsTip() {
        List<Spawn> v = plan(wave(10, WaveSpec.Formation.V_WING, NEEDLER, 5, FRONT, NONE));

        assertEquals(240, endX(v.get(0)));
        for (Spawn unit : v) {
            double depth = PlayField.HEIGHT - endY(unit);
            assertTrue(depth >= 80 && depth <= 220, "hovers 80-220 px below the top, was " + depth);
            assertTrue(unit.holdSeconds() >= 2 && unit.holdSeconds() <= 4);
            assertTrue(endY(unit) >= endY(v.get(0)), "the tip leads");
        }
        assertTrue(endX(v.get(1)) < 240 && endX(v.get(2)) > 240, "wings on both sides");
        assertTrue(v.get(1).exit().dx() < 0 && v.get(2).exit().dx() > 0, "wings leave to their side");
    }

    @Test
    void aSkitterLineAbreastSweepsDownAndARearLineFliesUp() {
        List<Spawn> front = plan(wave(10, WaveSpec.Formation.LINE_ABREAST, SKITTER, 8, FRONT, NONE));
        List<Spawn> rear = plan(wave(10, WaveSpec.Formation.LINE_ABREAST, SKITTER, 6, REAR, NONE));

        assertEquals(30, endX(front.get(0)));
        assertEquals(450, endX(front.get(7)));
        assertTrue(endY(front.get(0)) < 0);
        assertTrue(startY(rear.get(0)) < 0 && endY(rear.get(0)) > PlayField.HEIGHT);
    }

    @Test
    void aNeedlerLineAbreastHoversThenLeaves() {
        List<Spawn> line = plan(wave(10, WaveSpec.Formation.LINE_ABREAST, NEEDLER, 3, FRONT, NONE));

        assertEquals(
                List.of(120.0, 240.0, 360.0),
                line.stream().map(FormationsTest::endX).toList());
        assertTrue(line.stream().allMatch(unit -> unit.holdSeconds() >= 2));
    }

    @Test
    void aStreamTricklesInFromAlternatingEdges() {
        var stream = new WaveSpec(
                10,
                WaveSpec.Formation.STREAM,
                SKITTER,
                4,
                FRONT,
                ALTERNATING,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.of(0.5),
                List.of());

        List<Spawn> units = plan(stream);

        assertEquals(SimStep.ticks(11.5), units.get(3).tick());
        assertEquals(160, units.get(0).speed(), "the stream speed");
        assertTrue(units.get(0).path().x(0, 0) < 240 && units.get(1).path().x(0, 0) > 240);
    }

    @Test
    void aPincerHoldsAtBothEdges() {
        List<Spawn> pincer = plan(held(WaveSpec.Formation.PINCER, NEEDLER, 4, SIDES, 4, 1));

        assertEquals(
                List.of(60.0, 420.0, 60.0, 420.0),
                pincer.stream().map(FormationsTest::endX).toList());
        assertTrue(pincer.stream().allMatch(unit -> unit.holdSeconds() == 4));
    }

    @Test
    void aCircleArrivesWholeOrbitsAndBreaksOffGroupByGroup() {
        List<Spawn> circle = plan(held(WaveSpec.Formation.CIRCLE, NEEDLER, 8, FRONT, 6, 2));

        for (int i = 0; i < 8; i++) {
            Spawn unit = circle.get(i);
            assertEquals(circle.get(0).path().length(), unit.path().length(), 1e-9);
            Spawn.Orbit orbit = unit.orbit().orElseThrow();
            assertEquals(90, Math.hypot(endX(unit) - orbit.centreX(), endY(unit) - orbit.centreY()), 1e-6);
            assertEquals(6 + (i / 2) * Formations.BREAK_INTERVAL_SECONDS, unit.holdSeconds(), 1e-9);
            assertTrue(unit.exit().towardShip());
        }
    }

    @Test
    void selectedCircleUnitsLeadTheTargetWhenTheGunSaysSo() {
        EnemySpec leading = TestSpecs.needler(EnemyGun.aimed(2.5, 0.8, 1, 150, 4, true));

        List<Spawn> circle = plan(held(WaveSpec.Formation.CIRCLE, leading, 4, FRONT, 6, 1));

        assertEquals(
                List.of(false, true, false, true),
                circle.stream().map(Spawn::leadsTarget).toList());
    }

    @Test
    void theCarriedPickupGoesToTheLastUnit() {
        var wave = new WaveSpec(
                10,
                WaveSpec.Formation.PINCER,
                NEEDLER,
                4,
                SIDES,
                NONE,
                Optional.of(4.0),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(new WaveSpec.Carried(PickupType.ARMOUR_PATCH, true)));

        List<Spawn> pincer = plan(wave);

        assertEquals(Optional.of(PickupType.ARMOUR_PATCH), pincer.get(3).carried());
        assertTrue(pincer.subList(0, 3).stream().allMatch(unit -> unit.carried().isEmpty()));
    }

    @Test
    void aFormationWithoutTheMovementItNeedsIsRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> plan(held(WaveSpec.Formation.CIRCLE, SKITTER, 8, FRONT, 6, 1)));
    }
}
