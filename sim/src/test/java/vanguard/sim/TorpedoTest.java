package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.loadout;
import static vanguard.sim.TestSpecs.muzzle;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2a (design/player/weapons/torpedo-pod): the torpedo delivery with the data's
 * numbers. It runs only over water, speeds up from 300 to 420 px/s over 0.5 s, picks the nearest
 * {@code sub} or {@code ground} unit in its 60° cone as it is fired (and again only when that one is
 * gone), steers at most 60°/s towards it, strikes the first {@code sub} or {@code ground} target it
 * touches at full damage and passes under flyers; it is gone after its range. Stepping allocates
 * nothing.
 */
class TorpedoTest {
    private static final int FIRE = Command.FIRE.bit();
    /** The left pod's muzzle: 16 px left of the ship's centre. */
    private static final double POD_X = Ship.START_X - 16;

    /** After the data's L1, from the left pod; {@code range} px. */
    private static WeaponSpec torpedo(double range) {
        return new WeaponSpec(
                "torpedo-pod",
                "torpedo",
                "torpedo",
                WeaponSpec.Delivery.TORPEDO,
                false,
                0.8,
                12.5,
                300,
                new Hitbox(5, 16),
                range,
                Double.POSITIVE_INFINITY,
                1,
                0,
                Math.toRadians(60),
                Math.toRadians(30),
                0,
                0,
                List.of(muzzle(-16, 0, 0)),
                420,
                0.5,
                WeaponSpec.Mines.NONE);
    }

    static final WeaponSpec TORPEDO = torpedo(500);

    static Sortie sortie(LevelScript level, WeaponSpec weapon) {
        return new Sortie(
                1,
                loadout(new Armament.Mount(Armament.Slot.LEFT_WING, weapon, weapon)),
                level,
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
    }

    /** Units coming down with the scroll (130 px/s) from the top edge, over water or over land. */
    static LevelScript units(boolean water, LevelScript.GroundUnit... units) {
        return new LevelScript(
                        1,
                        1,
                        0,
                        List.of(new LevelScript.Section(60, 130)),
                        List.of(),
                        List.of(),
                        List.of(units),
                        0,
                        List.of(),
                        new LevelScript.Secondary(0.8, 50),
                        List.of())
                .withWater(water);
    }

    static EnemySpec dummy(Layer layer, double hp) {
        return new EnemySpec(
                "dummy-" + layer,
                hp,
                new Hitbox(20, 20),
                layer,
                0,
                false,
                0,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
    }

    static int count(Sortie sortie, int steps, int commands, SimEvents.Type type) {
        int counted = 0;
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
            counted += sortie.events().count(type);
        }
        return counted;
    }

    static void run(Sortie sortie, int steps, int commands) {
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
        }
    }

    @Test
    void overLandThePodFiresNothingButOverWaterItFires() {
        var land = sortie(units(false), TORPEDO);
        assertTrue(land.mountIdle(0));
        assertEquals(0, count(land, SimStep.ticks(3), FIRE, SimEvents.Type.SHOT_FIRED));
        assertEquals(0, land.shotCount());
        assertEquals(Integer.MAX_VALUE, land.ticksSinceShot(0), "no muzzle flash either");

        var water = sortie(units(true), TORPEDO);
        assertFalse(water.mountIdle(0));
        // 0.8 volleys/s: at 0, 1.25 and 2.5 s.
        assertEquals(3, count(water, SimStep.ticks(3), FIRE, SimEvents.Type.SHOT_FIRED));
    }

    @Test
    void itLeavesThePodAndSpeedsUpFromItsStartToItsEndSpeedOverHalfASecond() {
        var sortie = sortie(units(true), TORPEDO);
        sortie.step(FIRE);
        Shot torpedo = sortie.shot(0);
        assertEquals(WeaponSpec.Delivery.TORPEDO, torpedo.weapon().delivery());
        assertEquals(POD_X, torpedo.renderX(0), 1e-9);
        assertEquals(300, torpedo.vy(), 1e-9);

        run(sortie, SimStep.ticks(0.25), Command.NONE);
        assertEquals(360, torpedo.vy(), 1e-9);
        run(sortie, SimStep.ticks(0.25), Command.NONE);
        assertEquals(420, torpedo.vy(), 1e-9);
        run(sortie, 10, Command.NONE);
        assertEquals(420, torpedo.vy(), 1e-9);
        assertEquals(0, torpedo.heading(), 1e-12, "without a target it runs straight");
    }

    @Test
    void itIsGoneAfterItsRange() {
        // A shorter range than the data's 500 px, which runs past the top edge first.
        var sortie = sortie(units(true), torpedo(300));
        sortie.step(FIRE);
        double startY = sortie.shot(0).renderY(1);
        double lastY = startY;
        int steps = 0;
        while (sortie.shotCount() > 0) {
            lastY = sortie.shot(0).renderY(1);
            sortie.step(Command.NONE);
            steps++;
        }
        assertEquals(300, lastY - startY, 2 * 420 * SimStep.SECONDS);
        // 180 px in the first 0.5 s, the other 120 px at 420 px/s.
        assertEquals(0.5 + 120 / 420.0, steps * SimStep.SECONDS, 2 * SimStep.SECONDS);
    }

    @Test
    void itStrikesSubAndGroundUnitsAtFullDamageButPassesUnderFlyers() {
        for (Layer layer : List.of(Layer.SUB, Layer.GROUND)) {
            // 20 HP: one torpedo of 12.5 leaves it standing (no ×2 on the ground), the second sinks it.
            var sortie = sortie(units(true, new LevelScript.GroundUnit(0, POD_X, dummy(layer, 20), -1)), TORPEDO);
            run(sortie, SimStep.ticks(1), Command.NONE);
            sortie.step(FIRE);
            assertEquals(1, count(sortie, SimStep.ticks(1.2), Command.NONE, SimEvents.Type.ENEMY_HIT), layer.name());
            assertEquals(7.5, sortie.enemy(0).hp(), 1e-9, layer.name());
            assertEquals(0, sortie.shotCount(), layer + ": it is spent on the first hit");
            run(sortie, SimStep.ticks(0.1), Command.NONE);
            sortie.step(FIRE);
            run(sortie, SimStep.ticks(1.2), Command.NONE);
            assertEquals(1, sortie.kills(), layer.name());
        }
        for (Layer layer : List.of(Layer.LOW_AIR, Layer.AIR, Layer.HIGH_AIR)) {
            var sortie = sortie(units(true, new LevelScript.GroundUnit(0, POD_X, dummy(layer, 20), -1)), TORPEDO);
            run(sortie, SimStep.ticks(1), Command.NONE);
            sortie.step(FIRE);
            assertEquals(0, count(sortie, SimStep.ticks(1.5), Command.NONE, SimEvents.Type.ENEMY_HIT), layer.name());
            assertEquals(20, sortie.enemy(0).hp(), layer.name());
        }
    }

    @Test
    void itSteersAtMostItsTurnRateTowardsASubUnitInItsCone() {
        // 80 px right of the pod's line, about 450 px ahead: 10 degrees off, inside the 30 either side.
        var sortie = sortie(units(true, new LevelScript.GroundUnit(0, POD_X + 80, dummy(Layer.SUB, 10), -1)), TORPEDO);
        run(sortie, SimStep.ticks(0.5), Command.NONE);
        sortie.step(FIRE);
        Shot torpedo = sortie.shot(0);
        double previous = torpedo.heading();
        double most = 0;
        boolean hit = false;
        for (int i = 0; i < SimStep.ticks(1.5) && !hit; i++) {
            sortie.step(Command.NONE);
            hit = sortie.events().count(SimEvents.Type.ENEMY_HIT) > 0;
            if (!hit) {
                double turned = Math.abs(torpedo.heading() - previous);
                assertTrue(turned <= Math.toRadians(1) + 1e-9, "at most 60 degrees/s: " + Math.toDegrees(turned));
                previous = torpedo.heading();
                most = Math.max(most, torpedo.heading());
            }
        }
        assertTrue(most > Math.toRadians(5), "it turned to the right: " + Math.toDegrees(most));
        assertTrue(hit);
        assertEquals(1, sortie.kills());
    }

    @Test
    void aUnitOutsideItsConeOrAFlyerIsNoTarget() {
        // 250 px to the right about 400 px ahead: 32 degrees off, outside the cone.
        var outside =
                sortie(units(true, new LevelScript.GroundUnit(0, POD_X + 250, dummy(Layer.SUB, 10), -1)), TORPEDO);
        run(outside, SimStep.ticks(1), Command.NONE);
        outside.step(FIRE);
        Shot torpedo = outside.shot(0);
        assertEquals(-1, torpedo.target());
        run(outside, SimStep.ticks(0.5), Command.NONE);
        assertEquals(0, torpedo.heading(), 1e-12);

        var flyer =
                sortie(units(true, new LevelScript.GroundUnit(0, POD_X + 60, dummy(Layer.LOW_AIR, 10), -1)), TORPEDO);
        run(flyer, SimStep.ticks(0.5), Command.NONE);
        flyer.step(FIRE);
        assertEquals(-1, flyer.shot(0).target());
        run(flyer, SimStep.ticks(0.5), Command.NONE);
        assertEquals(0, flyer.shot(0).heading(), 1e-12);
    }

    @Test
    void withoutATargetAsItIsFiredItRunsStraightPastOneThatComesLater() {
        // Fired before the unit is on the field: it does not look again.
        var sortie =
                sortie(units(true, new LevelScript.GroundUnit(0.3, POD_X + 80, dummy(Layer.SUB, 10), -1)), TORPEDO);
        sortie.step(FIRE);
        Shot torpedo = sortie.shot(0);
        run(sortie, SimStep.ticks(0.8), Command.NONE);
        assertEquals(-1, torpedo.target());
        assertEquals(0, torpedo.heading(), 1e-12);
    }

    @Test
    void steppingWithTorpedoesOverWaterDoesNotAllocate() {
        Supplier<Sortie> prepare = () -> new Sortie(
                3,
                loadout(
                        new Armament.Mount(Armament.Slot.LEFT_WING, TORPEDO, TORPEDO),
                        new Armament.Mount(Armament.Slot.RIGHT_WING, TORPEDO, TORPEDO)),
                units(
                        true,
                        new LevelScript.GroundUnit(2, POD_X, dummy(Layer.SUB, 200), -1),
                        new LevelScript.GroundUnit(4, POD_X + 60, dummy(Layer.GROUND, 200), -1),
                        new LevelScript.GroundUnit(6, POD_X - 40, dummy(Layer.SUB, 200), -1),
                        new LevelScript.GroundUnit(8, POD_X + 20, dummy(Layer.AIR, 200), -1),
                        new LevelScript.GroundUnit(10, POD_X, dummy(Layer.SUB, 200), -1)),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);

        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = prepare.get();
                    run(sortie, 300, FIRE);
                    return sortie;
                },
                sortie -> run(sortie, 1200, FIRE));

        assertEquals(0, allocated, "1200 steps allocated " + allocated + " bytes");
    }
}
