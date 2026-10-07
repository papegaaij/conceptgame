package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartCSpecs.count;
import static vanguard.sim.PartCSpecs.find;
import static vanguard.sim.PartCSpecs.hardenedTarget;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A hardened enemy (M5 part C, the Hive Node's armour): only {@code anti-ground} deliveries, the
 * Airstrike and the Smart Bomb damage it; other shots and blasts glance off with a spark; homing
 * missiles and the wingman do not pick it unless their weapon is {@code anti-ground}.
 */
class HardenedEnemyTest {
    private static final double HP = 1000;

    /** The target scrolls in at 130 px/s above the ship's column and passes under the ship after about 3.6 s. */
    private static LevelScript level() {
        return PartCSpecs.level(
                20, 130, List.of(), List.of(new LevelScript.GroundUnit(0, Ship.START_X, hardenedTarget(HP), -1)));
    }

    private static Sortie sortie(Loadout loadout) {
        return new Sortie(1, loadout, level(), TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static Loadout armed(WeaponSpec weapon) {
        return TestSpecs.loadout(new Armament.Mount(Armament.Slot.FRONT, weapon, weapon));
    }

    /** Fires for {@code seconds}; returns the glances seen. */
    private static int fire(Sortie sortie, double seconds) {
        int glanced = 0;
        for (int tick = 0; tick < SimStep.ticks(seconds); tick++) {
            sortie.step(Command.FIRE.bit());
            glanced += count(sortie, SimEvents.Type.SHOT_GLANCED);
        }
        return glanced;
    }

    private static double damage(Sortie sortie) {
        return HP - find(sortie, "bunker").hp();
    }

    @Test
    void standardBoltsGlanceAndAntiGroundBoltsHitAtDoubleDamage() {
        Sortie standard = sortie(armed(PartCSpecs.bolt(false)));
        assertTrue(fire(standard, 3) > 10);
        assertEquals(0, damage(standard));

        Sortie antiGround = sortie(armed(PartCSpecs.bolt(true)));
        assertEquals(0, fire(antiGround, 3));
        assertTrue(damage(antiGround) > 0);
        assertEquals(0, damage(antiGround) % 4, 1e-9, "2 damage × 2 on the ground");
    }

    @Test
    void aBlastWithoutAntiGroundGlancesOff() {
        Sortie standard = sortie(armed(PartCSpecs.bomb(false)));
        assertTrue(fire(standard, 4) > 0, "the bombs under the ship spark off it");
        assertEquals(0, damage(standard));

        Sortie antiGround = sortie(armed(PartCSpecs.bomb(true)));
        fire(antiGround, 4);
        assertTrue(damage(antiGround) >= 12, "a Bomb Rack's bombs hit");
    }

    @Test
    void aHomingMissileLocksOntoItOnlyWhenAntiGround() {
        assertEquals(0, locks(sortie(armed(PartCSpecs.homing(false)))));
        assertTrue(locks(sortie(armed(PartCSpecs.homing(true)))) > 0);
    }

    /** The shots locked onto the target over 3 s of fire. */
    private static int locks(Sortie sortie) {
        int locked = 0;
        for (int tick = 0; tick < SimStep.ticks(3); tick++) {
            sortie.step(Command.FIRE.bit());
            for (int i = 0; i < sortie.shotCount(); i++) {
                locked += sortie.shot(i).target() >= 0 ? 1 : 0;
            }
        }
        return locked;
    }

    @Test
    void theWingmanPicksItOnlyWithAnAntiGroundGun() {
        assertEquals(-1, rookTarget(WingmanTest.GUN));
        WeaponSpec mortarLike = PartCSpecs.bolt(true);
        assertTrue(rookTarget(mortarLike) >= 0);
    }

    /** Rook's target after 3 s, the target on the field ahead of him; -1 for none. */
    private static int rookTarget(WeaponSpec gun) {
        WingmanSpec rook = new WingmanSpec(WingmanSpec.Side.LEFT, 80, WingmanTest.CRAFT, WingmanTest.AI, gun);
        Sortie sortie = sortie(TestSpecs.LOADOUT.withWingman(rook));
        int target = -1;
        for (int tick = 0; tick < SimStep.ticks(3); tick++) {
            sortie.step(Command.NONE);
            target = Math.max(target, sortie.wingman().orElseThrow().target());
        }
        return target;
    }

    @Test
    void theAirstrikeHitsIt() {
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT.withSpecial(new SpecialSpec("Airstrike", 1, 4, 0.1, AirstrikeTest.AIRSTRIKE)),
                level(),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        while (sortie.levelSeconds() < 1.5) {
            sortie.step(Command.NONE);
        }
        sortie.step(Command.SPECIAL.bit());
        while (sortie.levelSeconds() < 3.5) {
            sortie.step(Command.NONE);
        }

        assertEquals(300, damage(sortie), 1e-9, "the ground cap of one strike");
    }
}
