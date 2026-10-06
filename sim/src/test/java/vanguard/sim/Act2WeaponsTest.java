package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.loadout;
import static vanguard.sim.TestSpecs.muzzle;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;
import static vanguard.sim.WaveSpec.Formation.LINE_ABREAST;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * The Act 2 deliveries (design/player/weapons): the Hornet's accelerating missiles, the Swivel Gun's
 * turret and the Proximity Mines, with the data's numbers.
 */
class Act2WeaponsTest {
    private static final int FIRE = Command.FIRE.bit();
    /** The rear muzzle: 24 px behind the ship's centre. */
    private static final double REAR = -24;

    private static final WeaponSpec HORNET = new WeaponSpec(
            "hornet-launcher",
            "missile",
            "missile",
            WeaponSpec.Delivery.HOMING,
            false,
            1.25,
            6,
            450,
            new Hitbox(6, 14),
            420,
            1.6,
            1,
            0,
            Math.toRadians(180),
            Math.toRadians(60),
            0,
            0,
            List.of(muzzle(-10, 21, 0)),
            650,
            0.3,
            WeaponSpec.Mines.NONE);
    private static final WeaponSpec SWIVEL = new WeaponSpec(
            "swivel-gun",
            "ballistic",
            "ballistic",
            WeaponSpec.Delivery.TURRET,
            false,
            4,
            2,
            700,
            new Hitbox(5, 5),
            300,
            INFINITE,
            1,
            0,
            Math.toRadians(360),
            Math.toRadians(30),
            0,
            0,
            List.of(muzzle(16, -3, 0)));

    /** Proximity mines after the data's L1, living long enough for a unit to come down to them. */
    private static WeaponSpec mines(double lifetime, int maxLive) {
        return new WeaponSpec(
                "proximity-mines",
                "mine",
                "mine",
                WeaponSpec.Delivery.MINE,
                false,
                1,
                20,
                60,
                new Hitbox(10, 10),
                INFINITE,
                lifetime,
                1,
                48,
                0,
                Math.PI,
                0,
                0,
                List.of(muzzle(0, REAR, 180)),
                60,
                0,
                new WeaponSpec.Mines(0.5, 0.4, 40, maxLive));
    }

    private static Sortie sortie(LevelScript level, Armament.Mount... mounts) {
        return new Sortie(1, loadout(mounts), level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static Armament.Mount mount(Armament.Slot slot, WeaponSpec weapon) {
        return new Armament.Mount(slot, weapon, weapon);
    }

    /** A level of units moving down with the scroll (130 px/s) from the top edge, as the test fire's dummies. */
    private static LevelScript units(LevelScript.GroundUnit... units) {
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
                List.of());
    }

    private static EnemySpec dummy(Layer layer, double hp) {
        return new EnemySpec(
                "dummy",
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

    private static int count(Sortie sortie, int steps, int commands, SimEvents.Type type) {
        int counted = 0;
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
            counted += sortie.events().count(type);
        }
        return counted;
    }

    private static void run(Sortie sortie, int steps, int commands) {
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
        }
    }

    @Test
    void aHornetSpeedsUpFromItsStartToItsEndSpeedOverItsAccelerationTime() {
        var sortie = sortie(TestSpecs.level(10, List.of()), mount(Armament.Slot.FRONT, HORNET));
        sortie.step(FIRE);
        Shot missile = sortie.shot(0);
        assertEquals(450, missile.vy(), 1e-9);

        run(sortie, SimStep.ticks(0.15), Command.NONE);
        assertEquals(550, missile.vy(), 1e-9);
        run(sortie, SimStep.ticks(0.15), Command.NONE);
        assertEquals(650, missile.vy(), 1e-9);
        run(sortie, 10, Command.NONE);
        assertEquals(650, missile.vy(), 1e-9);
    }

    @Test
    void aTurretWithoutATargetFacesForwardAndHoldsFire() {
        var sortie = sortie(TestSpecs.level(10, List.of()), mount(Armament.Slot.RIGHT_WING, SWIVEL));

        assertEquals(0, count(sortie, SimStep.ticks(2), FIRE, SimEvents.Type.SHOT_FIRED));
        assertEquals(0, sortie.turretHeading(0), 1e-12);
    }

    @Test
    void aTurretSlewsAtItsRateAfterAUnitPassingBesideTheShipAndHitsItBehind() {
        var sortie = sortie(
                units(new LevelScript.GroundUnit(0, Ship.START_X + 70, dummy(Layer.GROUND, 1000), -1)),
                mount(Armament.Slot.RIGHT_WING, SWIVEL));
        double most = 0;
        double previous = sortie.turretHeading(0);
        boolean hitBehind = false;
        for (int i = 0; i < SimStep.ticks(6); i++) {
            sortie.step(FIRE);
            double heading = sortie.turretHeading(0);
            double turned = Math.abs(Math.IEEEremainder(heading - previous, 2 * Math.PI));
            assertTrue(turned <= Math.toRadians(6) + 1e-9, "at most 360 degrees/s: " + Math.toDegrees(turned));
            previous = heading;
            most = Math.max(most, Math.abs(heading));
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.ENEMY_HIT
                        && sortie.events().y(e) < sortie.ship().y() - 24) {
                    hitBehind = true;
                }
            }
        }
        assertTrue(most > Math.toRadians(120), "it swung round behind the wing: " + Math.toDegrees(most));
        assertTrue(hitBehind, "its shots hit the unit behind the ship");
    }

    @Test
    void aTurretPrefersAFlankTargetNearlyAsCloseAsTheNearestAhead() {
        // A unit straight ahead of the pod and one 150 px out to the side, coming down together.
        // The side one is outside the forward cone below 260 px ahead and within 20 % of the
        // nearest's distance above 226 px: in between the turret points to the side.
        double podX = Ship.START_X + 16;
        var sortie = sortie(
                units(
                        new LevelScript.GroundUnit(0, podX, dummy(Layer.GROUND, 1e6), -1),
                        new LevelScript.GroundUnit(0, podX + 150, dummy(Layer.GROUND, 1e6), -1)),
                mount(Armament.Slot.RIGHT_WING, SWIVEL));
        boolean flank = false;
        for (int i = 0; i < SimStep.ticks(4); i++) {
            sortie.step(Command.NONE);
            double ahead = sortie.enemy(0).renderY(1) - sortie.ship().y();
            if (ahead > 230 && ahead < 255 && sortie.turretHeading(0) > Math.toRadians(30)) {
                flank = true;
            }
            if (ahead > 0 && ahead < 200) {
                // Nearer than that the unit ahead wins.
                assertTrue(sortie.turretHeading(0) < Math.toRadians(35), "ahead at " + ahead);
            }
        }
        assertTrue(flank, "the turret turned to the flank");
    }

    @Test
    void aMineDriftsBackThenHoldsItsScreenPositionAndArmsAfterItsDelay() {
        var sortie = sortie(TestSpecs.level(10, List.of()), mount(Armament.Slot.REAR, mines(10, 3)));
        sortie.step(FIRE);
        Shot mine = sortie.shot(0);
        double dropY = sortie.ship().y() + REAR;
        assertFalse(mine.armed());

        run(sortie, SimStep.ticks(0.4) - 2, Command.NONE);
        assertFalse(mine.armed());
        run(sortie, 2, Command.NONE);
        assertTrue(mine.armed());
        run(sortie, SimStep.ticks(0.2), Command.NONE);
        // 60 px/s decaying to nothing over 0.5 s: 15 px back.
        double held = mine.renderY(1);
        assertEquals(dropY - 15, held, 0.6);
        run(sortie, SimStep.ticks(2), Command.NONE);
        assertEquals(held, mine.renderY(1), 1e-12);
        assertEquals(Ship.START_X, mine.renderX(1), 1e-9);
    }

    @Test
    void aMineSendsOneArmedEventAtTheStepItArms() {
        var sortie = sortie(TestSpecs.level(10, List.of()), mount(Armament.Slot.REAR, mines(10, 3)));
        sortie.step(FIRE);
        Shot mine = sortie.shot(0);
        int armed = 0;
        for (int i = 0; i < SimStep.ticks(4); i++) {
            boolean before = mine.armed();
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.PROXIMITY_MINE_ARMED) {
                    armed++;
                    assertTrue(!before && mine.armed(), "in the step it arms");
                    assertEquals(0, sortie.events().value(e), "the mount that dropped it");
                    assertEquals(mine.x(), sortie.events().x(e), 1e-9);
                    assertEquals(mine.y(), sortie.events().y(e), 1e-9);
                }
            }
        }
        assertEquals(1, armed, "one beep per mine");
    }

    @Test
    void atItsCapAMountDropsNoMoreMinesUntilOneFizzles() {
        var sortie = sortie(TestSpecs.level(20, List.of()), mount(Armament.Slot.REAR, mines(4, 3)));
        int dropped = count(sortie, SimStep.ticks(3.9), FIRE, SimEvents.Type.SHOT_FIRED);
        assertEquals(3, dropped);
        assertEquals(3, sortie.shotCount());

        // The first fizzles at 4 s and the next drops at once.
        assertEquals(1, count(sortie, SimStep.ticks(0.2), FIRE, SimEvents.Type.SHOT_FIRED));
        assertEquals(3, sortie.shotCount());
    }

    @Test
    void aLowFlyerSetsAMineOffAndItsBlastHitsTheGroundTooButAGroundUnitDoesNot() {
        // Both pass the mine's column; only the low flyer sets it off, and the blast reaches the
        // ground unit 30 px beside it.
        var ground = sortie(
                units(new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.GROUND, 5), -1)),
                mount(Armament.Slot.REAR, mines(10, 1)));
        assertEquals(0, count(ground, SimStep.ticks(6), FIRE, SimEvents.Type.BLAST));

        var both = sortie(
                units(
                        new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.LOW_AIR, 5), -1),
                        new LevelScript.GroundUnit(0, Ship.START_X + 30, dummy(Layer.GROUND, 5), -1)),
                mount(Armament.Slot.REAR, mines(10, 1)));
        int blasts = count(both, SimStep.ticks(6), FIRE, SimEvents.Type.BLAST);
        assertEquals(1, blasts);
        assertEquals(2, both.kills());
    }

    @Test
    void aMineIgnoresAFlyerUntilItArms() {
        // A flyer hovering right behind the drop point: the mine waits for its arming delay.
        var sortie = sortie(
                units(new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.LOW_AIR, 1000), -1)),
                mount(Armament.Slot.REAR, mines(10, 1)));
        // The unit is at the drop point after about (540 - 72) / 130 = 3.6 s; drop just before.
        run(sortie, SimStep.ticks(3.5), Command.NONE);
        sortie.step(FIRE);
        int armTicks = SimStep.ticks(0.4);
        int first = -1;
        for (int i = 1; i < SimStep.ticks(2) && first < 0; i++) {
            sortie.step(Command.NONE);
            if (sortie.events().count(SimEvents.Type.BLAST) > 0) {
                first = i;
            }
        }
        assertTrue(first >= armTicks - 1, "burst after " + first + " steps, arms after " + armTicks);
    }

    @Test
    void steppingWithTheAct2WeaponsDoesNotAllocate() {
        Supplier<Sortie> prepare = () -> new Sortie(
                3,
                loadout(
                        mount(Armament.Slot.FRONT, HORNET),
                        mount(Armament.Slot.REAR, mines(4, 3)),
                        mount(Armament.Slot.LEFT_WING, SWIVEL),
                        mount(Armament.Slot.RIGHT_WING, SWIVEL)),
                TestSpecs.level(
                        80,
                        List.of(
                                wave(1, WaveSpec.Formation.SNAKE, TestSpecs.SKITTER, 6, FRONT, WaveSpec.Edge.LEFT),
                                wave(3, LINE_ABREAST, TestSpecs.NEEDLER, 5, FRONT, NONE))),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);

        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = prepare.get();
                    run(sortie, 900, FIRE);
                    return sortie;
                },
                sortie -> run(sortie, 1200, FIRE | Command.LEFT.bit()));

        assertEquals(0, allocated, "1200 steps allocated " + allocated + " bytes");
    }
}
