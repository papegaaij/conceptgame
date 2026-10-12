package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M5 part H, the root bursts (design/campaign Level 14, Hazards; design/enemies, Boss attacks and root
 * bursts; decision H2 = a): the telegraph at the burst's point, the move away from the ship, the growths
 * standing there and scrolling with the ground, invulnerable and silent while they grow, firing first
 * {@code firstShotSeconds} after they appeared, and the secondary's group counting them.
 */
class RootBurstTest {
    private static final double SPEED = 140;
    /** Burst 0 at t = 2 s, x 120, 200 px below the top edge; burst 1 at t = 5 s right under the ship's start x. */
    private static final LevelScript.RootBurst FIRST = new LevelScript.RootBurst(0, 2, 120, 200, 1, 1.5);

    private static final LevelScript.RootBurst UNDER_SHIP = new LevelScript.RootBurst(1, 5, Ship.START_X, 150, 1, 1.0);

    private static LevelScript level(LevelScript.RootBurst... bursts) {
        return level(SPEED, bursts);
    }

    private static LevelScript level(double speed, LevelScript.RootBurst... bursts) {
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        for (LevelScript.RootBurst burst : bursts) {
            units.add(LevelScript.GroundUnit.fromBurst(burst, -24, GroundAndDiveTest.TURRET, 0));
            units.add(LevelScript.GroundUnit.fromBurst(burst, 24, CraterNestTest.MORTAR, 0));
        }
        return new LevelScript(
                14,
                2,
                0,
                List.of(new LevelScript.Section(40, speed)),
                List.of(),
                List.of(),
                units,
                0,
                List.of(),
                new LevelScript.Secondary(0, 50, List.of("roots")),
                List.of(),
                List.of(),
                List.of());
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static List<Integer> values(Sortie sortie, SimEvents.Type type) {
        return PartHSpecs.values(sortie, type);
    }

    private static int eventIndex(Sortie sortie, SimEvents.Type type) {
        for (int i = 0; i < sortie.events().size(); i++) {
            if (sortie.events().type(i) == type) {
                return i;
            }
        }
        return -1;
    }

    private static List<Enemy> growths(Sortie sortie) {
        List<Enemy> growths = new ArrayList<>();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).rootBurst() >= 0) {
                growths.add(sortie.enemy(i));
            }
        }
        return growths;
    }

    @Test
    void theTelegraphStartsAtTheBurstsPointWhichScrollsWithTheGround() {
        Sortie sortie = sortie(level(FIRST));
        assertEquals(1, sortie.burstCount());
        assertFalse(sortie.burstTelegraphing(0));
        assertEquals(List.of(0), PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH, 2.1, 0));
        assertEquals(2, sortie.levelSeconds(), 1e-9, "at the burst's time");
        int i = eventIndex(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH);
        assertEquals(120, sortie.events().x(i), 1e-9, "clear of the ship: where the data puts it");
        assertEquals(PlayField.HEIGHT - 200, sortie.events().y(i), 1e-9, "200 px below the top edge");
        assertTrue(sortie.burstTelegraphing(0));
        assertEquals(PlayField.HEIGHT - 200, sortie.burstY(0), 1e-9);
        assertEquals(0, sortie.enemyCount(), "nothing stands there yet");
        PartHSpecs.steps(sortie, 0.5);
        assertEquals(PlayField.HEIGHT - 200 - SPEED * 0.5, sortie.burstY(0), 1e-6, "the point scrolls with the ground");
        assertEquals(0.5, sortie.burstTelegraphShare(0), 1e-9);
    }

    @Test
    void theGrowthsAppearAfterTheTelegraphAtThePointAndScrollWithTheGround() {
        Sortie sortie = sortie(level(FIRST));
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH, 2.1, 0);
        assertEquals(List.of(0), PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 1.1, 0));
        assertEquals(3, sortie.levelSeconds(), 1e-9, "the telegraph's 1 s later");
        double y = PlayField.HEIGHT - 200 - SPEED;
        int i = eventIndex(sortie, SimEvents.Type.ROOT_BURST_GREW);
        assertEquals(y, sortie.events().y(i), 1e-6, "where the point scrolled to");
        assertTrue(sortie.burstGrown(0));
        assertFalse(sortie.burstTelegraphing(0));
        List<Enemy> growths = growths(sortie);
        assertEquals(2, growths.size(), "a Spine Turret and a Polyp Mortar");
        assertEquals(96, growths.get(0).x(), 1e-9);
        assertEquals(144, growths.get(1).x(), 1e-9);
        for (Enemy growth : growths) {
            assertEquals(y, growth.y(), 1e-6, "on the burst's point");
            assertTrue(growth.growing());
            assertTrue(growth.grounded());
            assertEquals(0, growth.rootBurst());
        }
        sortie.step(0);
        assertEquals(y - SPEED * SimStep.SECONDS, growths.get(0).y(), 1e-6, "it scrolls with the ground");
    }

    @Test
    void aGrowthTakesNoDamageAndFiresNothingWhileItGrowsThenFiresAtOnce() {
        // A slow scroll keeps the growths on the screen until they have grown.
        Sortie sortie = sortie(level(60, FIRST));
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 3.1, 0);
        List<Enemy> growths = growths(sortie);
        Enemy turret = growths.get(0);
        double hp = turret.hp();
        assertFalse(turret.damage(100), "a growth takes no damage");
        assertEquals(hp, turret.hp(), 1e-9);
        assertEquals(0, turret.growShare(0), 1e-9);
        int steps = 0;
        while (true) {
            sortie.step(0);
            steps++;
            if (!turret.growing()) {
                break;
            }
            assertTrue(values(sortie, SimEvents.Type.ENEMY_FIRED).isEmpty(), "silent while it grows");
            assertTrue(values(sortie, SimEvents.Type.MORTAR_LOBBED).isEmpty(), "silent while it grows");
        }
        assertEquals(SimStep.ticks(1.5), steps, "it has grown 1.5 s after it appeared");
        assertEquals(1, turret.growShare(1), 1e-9);
        assertFalse(values(sortie, SimEvents.Type.ENEMY_FIRED).isEmpty(), "the turret fires as soon as it has grown");
        assertFalse(values(sortie, SimEvents.Type.MORTAR_LOBBED).isEmpty(), "and the mortar lobs");
        assertTrue(turret.damage(100), "it takes damage once grown");
    }

    @Test
    void aShotOnAGrowingGrowthGlancesOff() {
        Sortie sortie = sortie(level(new LevelScript.RootBurst(0, 0.5, Ship.START_X - 70, 300, 0.5, 3)));
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 1.1, 0);
        Enemy turret = growths(sortie).get(0);
        double hp = turret.hp();
        boolean glanced = false;
        for (int i = 0; i < SimStep.ticks(2) && !glanced; i++) {
            // The ship under the growths, firing up at them.
            sortie.step(Command.FIRE.bit() | (sortie.ship().x() > turret.x() ? Command.LEFT.bit() : 0));
            glanced = !values(sortie, SimEvents.Type.SHOT_GLANCED).isEmpty();
        }
        assertTrue(glanced, "the shot sparks off");
        assertTrue(turret.growing());
        assertEquals(hp, turret.hp(), 1e-9);
    }

    @Test
    void aBurstUnderTheShipMovesAwayFromItWithItsGrowths() {
        Sortie sortie = sortie(level(UNDER_SHIP));
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH, 5.1, 0);
        int i = eventIndex(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH);
        assertEquals(Ship.START_X, sortie.ship().x(), 1e-9);
        assertEquals(
                Ship.START_X + LevelScript.RootBurst.SHIP_CLEARANCE,
                sortie.events().x(i),
                1e-9,
                "60 px from the ship, toward the field's middle on a tie");
        assertEquals(Ship.START_X + 60, sortie.burstX(0), 1e-9);
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 1.1, 0);
        List<Enemy> growths = growths(sortie);
        assertEquals(Ship.START_X + 60 - 24, growths.get(0).x(), 1e-9, "its growths move with it");
        assertEquals(Ship.START_X + 60 + 24, growths.get(1).x(), 1e-9);
    }

    @Test
    void theMoveAwayFromTheShipKeepsTheBurstOnTheField() {
        // A burst 30 px right of a ship at x 440: to the right there is no room; it goes 60 px left.
        assertEquals(380 - 470, EnemyForce.clearOfShip(470, 48, 440), 1e-9);
        assertEquals(0, EnemyForce.clearOfShip(100, 48, 240), 1e-9, "far enough already");
        assertEquals(180 - 200, EnemyForce.clearOfShip(200, 48, 240), 1e-9, "away from the ship");
    }

    @Test
    void aBurstLiesOneToFourHundredPixelsBelowTheTopEdge() {
        assertThrows(IllegalArgumentException.class, () -> new LevelScript.RootBurst(0, 1, 100, 99, 1, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new LevelScript.RootBurst(0, 1, 100, 401, 1, 1.5));
        new LevelScript.RootBurst(0, 1, 100, 100, 1, 1.5);
        new LevelScript.RootBurst(0, 1, 100, 400, 1, 1.5);
    }

    @Test
    void theRootsGroupCountsTheGrowthsAndIsMetWhenAllAreDestroyed() {
        Sortie sortie = sortie(level(FIRST, UNDER_SHIP));
        assertEquals(1, sortie.groupCount());
        assertEquals(4, sortie.groupUnits(0), "ROOTS n / 4");
        assertEquals(4, sortie.enemyTotal(), "the growths count among the level's enemies");
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 3.1, 0);
        sortie.destroyEnemy(0);
        sortie.destroyEnemy(0);
        assertEquals(2, sortie.groupUnitsDestroyed(0));
        assertFalse(sortie.secondaryMet());
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 3.1, 0);
        sortie.destroyEnemy(0);
        sortie.destroyEnemy(0);
        assertEquals(4, sortie.groupUnitsDestroyed(0));
        assertTrue(sortie.secondaryMet(), "every growth destroyed before it left the screen");
    }

    @Test
    void aRetryTelegraphsTheBurstsAgain() {
        Sortie sortie = sortie(level(FIRST));
        PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_GREW, 3.1, 0);
        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(0);
        assertFalse(sortie.burstGrown(0));
        assertFalse(sortie.burstTelegraphing(0));
        assertEquals(List.of(0), PartHSpecs.until(sortie, SimEvents.Type.ROOT_BURST_TELEGRAPH, 2.1, 0));
    }

    @Test
    void theBurstsAreDeterministicAndAllocateNothing() {
        Sortie a = sortie(level(FIRST, UNDER_SHIP));
        Sortie b = sortie(level(FIRST, UNDER_SHIP));
        for (int i = 0; i < SimStep.ticks(9); i++) {
            int commands = (i / 40) % 2 == 0 ? Command.LEFT.bit() | Command.FIRE.bit() : Command.RIGHT.bit();
            a.step(commands);
            b.step(commands);
        }
        assertEquals(a.stateHash(), b.stateHash());
        long allocated = Allocations.least(() -> sortie(level(FIRST, UNDER_SHIP)), sortie -> {
            for (int i = 0; i < SimStep.ticks(9); i++) {
                sortie.step(Command.FIRE.bit() | ((i / 40) % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
            }
        });
        assertEquals(0, allocated, "the telegraphs, the growths and their fire allocate nothing per step");
    }
}
