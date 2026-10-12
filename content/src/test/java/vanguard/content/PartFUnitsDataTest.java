package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.Armament;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;
import vanguard.sim.WeaponSpec;

/**
 * M5 part F, step 4: the Lamprey from its data file (design/enemies/air/lamprey: the {@code chase}
 * movement, the {@code latch} block with its hull spots, shake, stun and throw, the {@code drain}
 * attack and the hooks {@code drain} and {@code latch_slots}), loaded strictly, checked and mapped for
 * the simulation; and the weapons whose shots hit a latched Lamprey (user decision F3 = a: the {@code
 * side} and {@code rear} traits).
 */
class PartFUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String LAMPREY = "enemies/air/lamprey/data.yaml";

    private static EnemySpec spec(Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, "lamprey", difficulty, Optional.empty());
    }

    @Test
    void theLampreyChasesLatchesAndDrains() {
        EnemySpec lamprey = spec(Difficulty.MEDIUM);
        EnemySpec.Chase chase = lamprey.chase().orElseThrow();
        EnemySpec.Latch latch = lamprey.latch().orElseThrow();

        assertEquals(Layer.AIR, lamprey.layer());
        assertEquals(3, lamprey.hp());
        assertEquals(12, lamprey.bounty());
        assertEquals(220, lamprey.speed());
        assertTrue(lamprey.destroyedByRamming(), "small: a touch with every slot taken is a contact it dies in");
        assertTrue(lamprey.gun().isEmpty(), "the drain is its latch's, not a gun");
        assertTrue(lamprey.drop().isEmpty(), "it drops nothing");
        assertEquals(220, chase.speed());
        assertEquals(Math.PI, chase.turnRate(), 1e-12, "180°/s");
        assertEquals(4, chase.seconds());
        assertEquals(1, latch.slots());
        assertEquals(
                List.of(new EnemySpec.Spot(-22, -13), new EnemySpec.Spot(21, -13), new EnemySpec.Spot(0, -35)),
                latch.spots(),
                "left, right and tail, y up");
        assertEquals(2, latch.damageFactor());
        assertEquals(4, latch.reversals());
        assertEquals(1.2, latch.withinSeconds());
        assertEquals(1, latch.stunSeconds());
        assertEquals(120, latch.throwSpeed());
        assertEquals(8, latch.drainRate());
    }

    @Test
    void theHooksDrainHarderAndLatchTwoOnHard() {
        EnemySpec easy = spec(Difficulty.EASY);
        EnemySpec hard = spec(Difficulty.HARD);

        assertEquals(1, easy.latch().orElseThrow().slots());
        assertEquals(8, easy.latch().orElseThrow().drainRate());
        assertEquals(2, easy.hp(), "the HP lever");
        assertEquals(2, hard.latch().orElseThrow().slots());
        assertEquals(12, hard.latch().orElseThrow().drainRate());
        assertEquals(4, hard.hp());
    }

    @Test
    void itIsNewInLevel12WithoutTheActHpFactor() {
        EnemyData data = CONTENT.enemy("lamprey");
        assertEquals(12, data.firstLevel());
        assertEquals(1, SimSpecs.actHpFactor(CONTENT, "lamprey", 2, 12));
        assertEquals(List.of("homing", "side", "rear", "area"), data.traits());
    }

    @Test
    void sideAndRearWeaponsHitALatchedLamprey() {
        assertTrue(weapon(Armament.Slot.REAR, "tail-gun").flanking(), "rear");
        assertTrue(weapon(Armament.Slot.REAR, "side-splitter").flanking(), "side");
        assertTrue(weapon(Armament.Slot.LEFT_WING, "swivel-gun").flanking(), "side, homing");
        assertTrue(weapon(Armament.Slot.REAR, "proximity-mines").flanking(), "rear, area");
        assertFalse(weapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON).flanking());
        assertFalse(weapon(Armament.Slot.LEFT_WING, "micro-missile-pod").flanking());
        assertFalse(weapon(Armament.Slot.LEFT_WING, "autocannon-pod").flanking());
    }

    private static WeaponSpec weapon(Armament.Slot slot, String slug) {
        return SimSpecs.weapon(CONTENT, slot, slug, 1);
    }

    @Test
    void theNewKeysAreChecked() {
        assertProblem(
                replace(LAMPREY, "  chase: {speed: 220, turn_rate: 180, seconds: 4}", "  swoop: {}"),
                "a unit that latches chases the ship first");
        assertProblem(
                replace(LAMPREY, "  - pattern: drain\n    rate: 8 ", "  - pattern: aimed\n    rate: 8 "),
                "a drain has its rate, the others none");
        assertProblem(replace(LAMPREY, "    rate: 8 ", "    rate: 0 "), "rate must be > 0");
        assertProblem(
                replace(LAMPREY, "  shake: {reversals: 4, within: 1.2}", "  shake: {reversals: 1, within: 1.2}"),
                "reversals: at least 2");
        assertProblem(replace(LAMPREY, "    tail: [0, -35]", "    tail: [0, -95]"), "latch.spots.tail");
        assertProblem(replace(LAMPREY, "  slots: 1 ", "  slots: 0 "), "slots must be > 0");
        assertProblem(
                replace(LAMPREY, "  hard: {drain: 12, latch_slots: 2}", "  hard: {drain: 12, latch_slots: 2, spin: 1}"),
                "spin");
        // a drain and the hooks without a latch
        List<DataFile> noLatch = DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(LAMPREY)
                        ? new DataFile(f.path(), f.text().replaceAll("(?m)^latch:\\n(?:  .*\\n)+", ""))
                        : f)
                .toList();
        assertProblem(noLatch, "only a unit that latches drains");
        assertProblem(noLatch, "drain and latch_slots change a latch the unit does not have");
    }

    private static void assertProblem(List<DataFile> files, String message) {
        var e = assertThrows(Exception.class, () -> ContentLoader.load(files));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    private static List<DataFile> replace(String path, String from, String to) {
        return DesignTree.dataFiles().stream()
                .map(f -> {
                    if (!f.path().equals(path)) {
                        return f;
                    }
                    assertTrue(f.text().contains(from), path + " has " + from);
                    return new DataFile(f.path(), f.text().replace(from, to));
                })
                .toList();
    }
}
