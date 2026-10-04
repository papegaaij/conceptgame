package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Armament;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * Level 05 as the game runs it: its credit budget, the README's totals, the difficulty variations
 * (the batteries' sizes, the sleds' period, the mortars' ring, the rocks), the stuck sled, the
 * supply canister and the group drop, and whole runs in which the autopilot destroys the four
 * batteries and the Gorgon Frigate.
 */
class Level05Test {
    static final String LEVEL = "act-1-first-contact/level-05-crater-nest";
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 05 (design/player/balance-plan.yaml): the Pulse
     * Cannon at L2, an Autocannon Pod on each wing and the second shield; on hard the Pulse Cannon
     * one level up (the plan is written for medium; the autopilot cannot dodge the hard frigate for
     * as long as the L2 fit needs). PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(
                                Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, difficulty == Difficulty.HARD ? 3 : 2),
                        new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "autocannon-pod", 1),
                        new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "autocannon-pod", 1)),
                content.shields().models().get(1).name(),
                content.armour().plating().getFirst().name(),
                0,
                difficulty);
    }

    static Sortie sortie(Content content, long seed, Difficulty difficulty) {
        Loadout loadout = planLoadout(content, difficulty);
        return new Sortie(
                seed,
                loadout,
                SimSpecs.level(content, LEVEL, difficulty),
                SimSpecs.rules(content, LEVEL, difficulty),
                loadout.plating().maxArmour());
    }

    @Test
    void aPerfectRunAtMediumEarnsTheLevelBudget() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        int kills = level.waves().stream()
                .mapToInt(wave -> wave.count() * wave.enemy().bounty())
                .sum();
        int released = level.waves().stream()
                .filter(wave -> wave.enemy().brood().isPresent())
                .mapToInt(wave -> {
                    EnemySpec.Brood brood = wave.enemy().brood().orElseThrow();
                    return wave.count() * brood.count() * brood.enemy().bounty();
                })
                .sum();
        int ground = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        LevelScript.SetPieceSpec frigate = level.setPieces().getFirst();
        int parts =
                frigate.parts().stream().mapToInt(LevelScript.PartSpec::bounty).sum();
        // The budget counts the two Skitter streams of a medium-par fight.
        int streams = 2 * 6 * 5;
        int small = content.player().pickups().salvage().credits().small();
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills
                + released
                + ground
                + parts
                + streams
                + small
                + crates
                + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_311, Math.round(budget));
        assertEquals(402, ground, "Polyp Mortar 14 × 15 + Spine Turret 16 × 12");
        assertEquals(200, parts, "heads 3 × 30 + core 110");
        assertEquals(65, crates);
        assertEquals(40, level.secondary().credits());
        System.out.printf(
                "Level 05 budget: kills %d, released %d, ground %d, frigate %d + streams %d, supply %d, crate %d,"
                        + " secondary %d: %d of %.0f (%+.1f %%)%n",
                kills,
                released,
                ground,
                parts,
                streams,
                small,
                crates,
                level.secondary().credits(),
                total,
                budget,
                100 * (total - budget) / budget);
        // The pacing filler (user decision D5) lifts a perfect run about 10-15 % above the curve.
        assertTrue(total >= budget * 0.95 && total <= budget * 1.15, "total " + total);
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> ground = new TreeMap<>();
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            ground.merge(unit.enemy().slug(), 1, Integer::sum);
        }
        Map<String, Integer> waves = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            waves.merge(wave.enemy().slug(), wave.count(), Integer::sum);
        }

        assertEquals(Map.of("polyp-mortar", 14, "spine-turret", 16), ground);
        assertEquals(12, waves.get("needler"));
        assertEquals(10, waves.get("stinger"));
        assertEquals(3, waves.get("brood-pod"));
        assertEquals(List.of("Battery A", "Battery B", "Battery C", "Battery D"), level.targets());
        assertEquals(List.of("polyp-mortar", "spine-turret"), level.secondary().killAll());
        assertEquals("NEST", level.secondary().label());
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(30, sortie.escapesTotal(), "every mortar and turret");
        assertEquals(4, sortie.groupCount());
        LevelScript.SetPieceSpec frigate = level.setPieces().getFirst();
        assertEquals("gorgon-frigate", frigate.slug());
        assertEquals(150, frigate.boss().orElseThrow().arriveSeconds(), 1e-9);
        assertTrue(level.sections().get(4).arena());
        assertEquals(30, level.sections().get(4).speed(), 1e-9);
        assertEquals(
                List.of(35.0, 75.0, 125.0, 150.0, 205.0, 215.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheBatteriesTheSledsTheRingsAndTheRocks(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        int[] units = new int[4];
        int[] mortars = new int[4];
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            if (unit.group() >= 0) {
                units[unit.group()]++;
                mortars[unit.group()] += unit.enemy().slug().equals("polyp-mortar") ? 1 : 0;
            }
        }
        EnemySpec mortar = level.groundUnits().stream()
                .map(LevelScript.GroundUnit::enemy)
                .filter(enemy -> enemy.slug().equals("polyp-mortar"))
                .findFirst()
                .orElseThrow();
        var lob = mortar.gun().orElseThrow().mortar().orElseThrow();
        LevelScript.SledSpec sled = level.sled().orElseThrow();

        int battery = difficulty == Difficulty.EASY ? 2 : 4;
        assertEquals(battery, units[0]);
        assertEquals(battery, units[2]);
        assertEquals(difficulty == Difficulty.HARD ? 5 : battery, units[3], "hard: battery D has a third mortar");
        assertEquals(difficulty == Difficulty.HARD ? 3 : battery / 2, mortars[3]);
        assertEquals(difficulty == Difficulty.HARD ? 12 : 8, lob.ring());
        assertEquals(1.0, lob.flightSeconds(), 1e-9);
        assertEquals(16, lob.impactRadius(), 1e-9, "the 32 px impact circle");
        assertEquals(
                switch (difficulty) {
                    case EASY -> 8;
                    case MEDIUM -> 5;
                    case HARD -> 4;
                },
                sled.periodSeconds(),
                1e-9);
        assertEquals(432, sled.x(), 1e-9);
        assertEquals(15, sled.damage(), 1e-9);
        assertEquals(difficulty != Difficulty.EASY, level.rocks().isPresent(), "no low-gravity debris on easy");
    }

    @Test
    void theStuckSledTheSupplyCanisterAndBatteryCsPatch() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec clamp = level.groundObjects().stream()
                .filter(LevelScript.GroundObjectSpec::trigger)
                .findFirst()
                .orElseThrow();
        LevelScript.GroundObjectSpec supply = level.groundObjects().stream()
                .filter(object -> object.bonusDrop().isPresent())
                .findFirst()
                .orElseThrow();

        assertEquals(3, clamp.hits());
        assertEquals(65, clamp.crateCredits());
        assertEquals("stuck sled", level.sled().orElseThrow().clampSecret());
        assertEquals(Optional.of(PickupType.SPECIAL_CHARGE), supply.bonusDrop());
        assertTrue(supply.t() + (540 + 24) / 150.0 < 150, "the canister leaves before the frigate");
        assertEquals(List.of(new LevelScript.GroupDrop(2, PickupType.ARMOUR_PATCH)), level.groupDrops());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotDestroysTheBatteriesAndTheFrigate(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        int lobs = 0;
        int directHits = 0;
        int sledHits = 0;
        int rocks = 0;
        int cleared = 0;
        int failures = 0;
        double killed = -1;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            if (sortie.primaryFailed()) {
                failures++;
            }
            step(sortie);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case MORTAR_LOBBED -> lobs++;
                    case MORTAR_IMPACT -> directHits += events.value(i);
                    case SLED_HIT -> sledHits++;
                    case ROCK_THROWN -> rocks++;
                    case GROUP_CLEARED -> cleared++;
                    case BOSS_DESTROYED -> killed = sortie.setPiece(0).killSeconds();
                    default -> {}
                }
            }
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 05 %s: attempt %d, batteries cleared %d (in all attempts), frigate killed in %.1f s, kills %d/%d,"
                        + " credits %d (%s), lobs %d (direct hits %d), sled hits %d, rocks %d, scorched crater %s,"
                        + " grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                cleared,
                killed,
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                lobs,
                directHits,
                sledHits,
                rocks,
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        for (int g = 0; g < 4; g++) {
            assertEquals(1, sortie.groupState(g), "battery " + g + " destroyed");
        }
        assertTrue(killed > 0, "the frigate is destroyed");
        assertTrue(lobs > 0);
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        fly(sortie(content, 2185, Difficulty.HARD));
        Sortie sortie = sortie(content, 2185, Difficulty.HARD);

        long before = threads.getCurrentThreadAllocatedBytes();
        fly(sortie);
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "the level allocated " + allocated + " bytes");
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameState() {
        Sortie first = sortie(content, 77, Difficulty.HARD);
        Sortie second = sortie(content, 77, Difficulty.HARD);
        fly(first);
        fly(second);

        assertEquals(first.stateHash(), second.stateHash());
    }

    /** A step of the autopilot; after a wreck or a failed primary, the boss checkpoint or the level again. */
    static void step(Sortie sortie) {
        if (!sortie.flying() && sortie.bossCheckpoint()) {
            sortie.retryFromBoss();
        } else if (!sortie.flying() || sortie.primaryFailed()) {
            sortie.retry(sortie.ship().defences().maxArmour());
        }
        sortie.step(Autopilot.commands(sortie));
    }

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            step(sortie);
        }
    }
}
