package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.loadout;
import static vanguard.sim.TestSpecs.muzzle;
import static vanguard.sim.TorpedoTest.count;
import static vanguard.sim.TorpedoTest.dummy;
import static vanguard.sim.TorpedoTest.run;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2a: the {@code sub} layer (design/enemies, Layer rules; user decision E2 = a and
 * the stated defaults 10–12). Only torpedoes and the Smart Bomb reach it: standard bolts pass over
 * it, homing missiles and turrets never pick it, bombs, mines and the Airstrike's blasts stop at the
 * surface, nothing on it touches the ship, and Rook skips it. A trigger under the water (Level 11's
 * sunken pod) is hit by torpedoes only and spent at once by a Smart Bomb.
 */
class SubLayerTest {
    private static final int FIRE = Command.FIRE.bit();
    private static final int SPECIAL = Command.SPECIAL.bit();

    /** Units and ground objects coming down with the scroll (130 px/s) over water. */
    private static LevelScript water(List<LevelScript.GroundObjectSpec> ground, LevelScript.GroundUnit... units) {
        return new LevelScript(
                        1,
                        1,
                        0,
                        List.of(new LevelScript.Section(60, 130)),
                        List.of(),
                        ground,
                        List.of(units),
                        (int) ground.stream()
                                .filter(LevelScript.GroundObjectSpec::trigger)
                                .count(),
                        List.of(),
                        new LevelScript.Secondary(0.8, 50),
                        List.of())
                .withWater(true);
    }

    private static LevelScript water(LevelScript.GroundUnit... units) {
        return water(List.of(), units);
    }

    /** A trigger of {@code hits} entering at {@code t}, under the water or on the ground. */
    private static LevelScript.GroundObjectSpec trigger(double t, double x, Hitbox size, int hits, boolean submerged) {
        return new LevelScript.GroundObjectSpec(
                t,
                x,
                size,
                0,
                0,
                Optional.empty(),
                hits,
                100,
                "sunken pod",
                false,
                0,
                1,
                Optional.empty(),
                LevelScript.GroundObjectSpec.CARGO_CONTAINER,
                false,
                Optional.empty(),
                submerged);
    }

    private static Sortie sortie(LevelScript level, Loadout loadout) {
        return new Sortie(1, loadout, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static Sortie sortie(LevelScript level, WeaponSpec weapon, Armament.Slot slot) {
        return sortie(level, loadout(new Armament.Mount(slot, weapon, weapon)));
    }

    /** A 1000 HP unit as wide as the Airstrike's two carpets (its test's targets). */
    private static EnemySpec wide(Layer layer) {
        return new EnemySpec(
                "wide-" + layer,
                1000,
                new Hitbox(120, 60),
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

    private static Enemy find(Sortie sortie, Layer layer) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).spec().layer() == layer) {
                return sortie.enemy(i);
            }
        }
        throw new AssertionError("no unit on " + layer);
    }

    @Test
    void subIsAppendedSoTheOtherLayersKeepTheirOrdinalsAndItNeitherTakesStandardShotsNorTouchesTheShip() {
        assertEquals(
                List.of(Layer.GROUND, Layer.LOW_AIR, Layer.AIR, Layer.HIGH_AIR, Layer.SUB), List.of(Layer.values()));
        assertFalse(Layer.SUB.hitByStandardShots());
        assertFalse(Layer.SUB.collidesWithPlayer());
        for (WeaponSpec.Delivery delivery : WeaponSpec.Delivery.values()) {
            assertEquals(delivery == WeaponSpec.Delivery.TORPEDO, delivery.reaches(Layer.SUB), delivery.name());
        }
        assertTrue(WeaponSpec.Delivery.TORPEDO.reaches(Layer.GROUND));
        assertFalse(WeaponSpec.Delivery.TORPEDO.reaches(Layer.LOW_AIR));
        assertFalse(WeaponSpec.Delivery.TORPEDO.reaches(Layer.AIR));
        assertFalse(WeaponSpec.Delivery.TORPEDO.reaches(Layer.HIGH_AIR));
    }

    @Test
    void boltsPassOverAUnitUnderTheWaterAndItPassesUnderTheShip() {
        // Not invulnerable: the unit passes right under the ship without touching it.
        var sortie = new Sortie(
                1,
                TestSpecs.LOADOUT,
                water(new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.SUB, 10), -1)),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        assertEquals(0, count(sortie, SimStep.ticks(4), FIRE, SimEvents.Type.ENEMY_HIT));
        assertEquals(10, find(sortie, Layer.SUB).hp());
        assertEquals(TestSpecs.SHIELD.capacity(), sortie.ship().defences().shield(), "no contact");
        assertEquals(TestSpecs.FULL_ARMOUR, sortie.ship().defences().armour(), "no contact");
    }

    @Test
    void homingMissilesAndTurretsNeverPickAUnitUnderTheWater() {
        var homing = sortie(
                water(new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.SUB, 10), -1)),
                TestSpecs.HOMING,
                Armament.Slot.FRONT);
        for (int i = 0; i < SimStep.ticks(3.5); i++) {
            homing.step(FIRE);
            assertEquals(0, homing.events().count(SimEvents.Type.ENEMY_HIT));
            for (int s = 0; s < homing.shotCount(); s++) {
                assertEquals(-1, homing.shot(s).target(), "no lock on a unit under the water");
            }
        }

        WeaponSpec swivel = new WeaponSpec(
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
        var turret = sortie(
                water(new LevelScript.GroundUnit(0, Ship.START_X + 70, dummy(Layer.SUB, 10), -1)),
                swivel,
                Armament.Slot.RIGHT_WING);
        assertEquals(0, count(turret, SimStep.ticks(5), FIRE, SimEvents.Type.SHOT_FIRED), "it holds fire");
    }

    @Test
    void bombsBurstOnTheSurfaceAndReachGroundUnitsButNotOnesUnderTheWater() {
        WeaponSpec bombs = new WeaponSpec(
                "bomb-rack",
                "bomb",
                "bomb",
                WeaponSpec.Delivery.DROPPED,
                false,
                3,
                10,
                0,
                new Hitbox(8, 8),
                0,
                INFINITE,
                1,
                30,
                0,
                Math.PI,
                0.3,
                0,
                List.of(muzzle(0, 0, 0)));
        // The submerged unit passes under the ship at 3.5 s, the ground one at 5 s.
        var sortie = sortie(
                water(
                        new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.SUB, 5), -1),
                        new LevelScript.GroundUnit(1.5, Ship.START_X, dummy(Layer.GROUND, 5), -1)),
                bombs,
                Armament.Slot.RIGHT_WING);
        assertTrue(count(sortie, SimStep.ticks(4), FIRE, SimEvents.Type.BLAST) > 0);
        assertEquals(5, find(sortie, Layer.SUB).hp());
        run(sortie, SimStep.ticks(3), FIRE);
        assertEquals(1, sortie.kills(), "the ground unit only");
    }

    @Test
    void aMinesBlastDoesNotReachAUnitUnderTheWater() {
        WeaponSpec mines = new WeaponSpec(
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
                10,
                1,
                48,
                0,
                Math.PI,
                0,
                0,
                List.of(muzzle(0, -24, 180)),
                60,
                0,
                new WeaponSpec.Mines(0.5, 0.4, 40, 1));
        // The low flyer sets the mine off; the submerged unit 30 px beside it is in the blast.
        var sortie = sortie(
                water(
                        new LevelScript.GroundUnit(0, Ship.START_X, dummy(Layer.LOW_AIR, 5), -1),
                        new LevelScript.GroundUnit(0, Ship.START_X + 30, dummy(Layer.SUB, 5), -1)),
                mines,
                Armament.Slot.REAR);
        int blasts = 0;
        double subHp = -1;
        for (int i = 0; i < SimStep.ticks(4.2); i++) {
            sortie.step(FIRE);
            blasts += sortie.events().count(SimEvents.Type.BLAST);
            if (blasts > 0 && subHp < 0) {
                subHp = find(sortie, Layer.SUB).hp();
            }
        }
        assertEquals(1, blasts);
        assertEquals(1, sortie.kills(), "the low flyer only");
        assertEquals(5, subHp);
    }

    @Test
    void aSmartBombHitsAUnitUnderTheWaterAndSpendsASunkenTriggerAtOnce() {
        SmartBombSpec bomb = new SmartBombSpec(0.35, 120, 60, 1.0, 1.5, 0.1, 0.8, 0.25);
        var sortie = sortie(
                water(
                        List.of(trigger(0.5, Ship.START_X + 100, new Hitbox(30, 30), 4, true)),
                        new LevelScript.GroundUnit(0, Ship.START_X - 80, dummy(Layer.SUB, 100), -1)),
                TestSpecs.LOADOUT.withSpecial(new SpecialSpec("Smart Bomb", 1, 3, 0.1, bomb)));
        run(sortie, SimStep.ticks(1.5), Command.NONE);
        sortie.step(SPECIAL);
        int spent = count(sortie, SimStep.ticks(0.5), Command.NONE, SimEvents.Type.TRIGGER_SPENT);
        assertEquals(1, spent, "all four hits at once");
        assertTrue(sortie.groundObject(0).spent());
        assertEquals(1, sortie.kills(), "120 on the 100 HP unit");
    }

    @Test
    void anAirstrikeDoesNotReachUnderTheWater() {
        AirstrikeSpec spec = new AirstrikeSpec(0.6, 64, 600, new Hitbox(56, 64), 36, 0.25, 32, 100, 300, 20, 60, 150);
        var sortie = sortie(
                water(
                        List.of(trigger(200 / 130.0, Ship.START_X, new Hitbox(120, 60), 1, true)),
                        new LevelScript.GroundUnit(0, Ship.START_X, wide(Layer.SUB), -1),
                        new LevelScript.GroundUnit(100 / 130.0, Ship.START_X, wide(Layer.GROUND), -1)),
                TestSpecs.LOADOUT.withSpecial(new SpecialSpec("Airstrike", 1, 4, 0.1, spec)));
        while (sortie.levelSeconds() < 2.4) {
            sortie.step(Command.NONE);
        }
        sortie.step(SPECIAL);
        int blasts = 0;
        while (sortie.levelSeconds() < 4.3) {
            sortie.step(Command.NONE);
            blasts += sortie.events().count(SimEvents.Type.AIRSTRIKE_BLAST);
        }
        assertTrue(blasts > 0);
        assertEquals(700, find(sortie, Layer.GROUND).hp(), "the ground unit takes its cap");
        assertEquals(1000, find(sortie, Layer.SUB).hp());
        assertFalse(sortie.groundObject(0).damaged(), "the sunken trigger is not hit");
    }

    @Test
    void onlyTorpedoesHitASunkenTriggerWhileBoltsHitOneOnTheGround() {
        // The pulse cannon's bolts on its line: a trigger on the ground is spent, a sunken one untouched.
        for (boolean submerged : List.of(false, true)) {
            var sortie = sortie(
                    water(List.of(trigger(0, Ship.START_X, new Hitbox(30, 30), 3, submerged))), TestSpecs.LOADOUT);
            int spent = count(sortie, SimStep.ticks(3.5), FIRE, SimEvents.Type.TRIGGER_SPENT);
            assertEquals(submerged ? 0 : 1, spent, "submerged " + submerged);
            assertEquals(!submerged, sortie.groundObject(0).damaged(), "submerged " + submerged);
        }
        // Torpedoes from the left pod, 16 px left of the centre: three of them spend it.
        var sortie = sortie(
                water(List.of(trigger(0, Ship.START_X - 16, new Hitbox(30, 30), 3, true))),
                TorpedoTest.TORPEDO,
                Armament.Slot.LEFT_WING);
        run(sortie, SimStep.ticks(0.3), Command.NONE);
        int hits = 0;
        int spent = 0;
        for (int i = 0; i < SimStep.ticks(3.2); i++) {
            sortie.step(FIRE);
            hits += sortie.events().count(SimEvents.Type.GROUND_HIT);
            spent += sortie.events().count(SimEvents.Type.TRIGGER_SPENT);
        }
        assertEquals(3, hits);
        assertEquals(1, spent);
    }

    @Test
    void rookSkipsAUnitUnderTheWater() {
        // Right ahead of him in his slot on the left (as WingmanTest's ground units).
        var sub = WingmanTest.sortie(
                water(new LevelScript.GroundUnit(0, Ship.START_X - 64, dummy(Layer.SUB, 1000), -1)),
                WingmanSpec.Side.LEFT);
        for (int i = 0; i < SimStep.ticks(3.5); i++) {
            sub.step(Command.NONE);
            assertEquals(-1, sub.wingman().orElseThrow().target());
        }
        assertEquals(1000, find(sub, Layer.SUB).hp());

        var ground = WingmanTest.sortie(
                water(new LevelScript.GroundUnit(0, Ship.START_X - 64, dummy(Layer.GROUND, 1000), -1)),
                WingmanSpec.Side.LEFT);
        boolean picked = false;
        for (int i = 0; i < SimStep.ticks(3.5); i++) {
            ground.step(Command.NONE);
            picked |= ground.wingman().orElseThrow().target() >= 0;
        }
        assertTrue(picked, "he picks a ground unit there");
    }
}
