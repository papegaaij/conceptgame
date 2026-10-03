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
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * Level 04 as the game runs it: its credit budget, the README's totals, the difficulty variations
 * (crawler HP, the pods' hooks, the Scuttlers' fan and walk, the hard pincer), the dugout and the
 * supply drop, and whole runs in which the autopilot brings the convoy home.
 */
class Level04Test {
    static final String LEVEL = "act-1-first-contact/level-04-tranquility-run";
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    static Sortie sortie(Content content, long seed, Difficulty difficulty) {
        Loadout loadout = SimSpecs.starterLoadout(content, difficulty);
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
        int medium = content.player().pickups().salvage().credits().medium();

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
        int turrets = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        LevelScript.Escort escort = level.escort().orElseThrow();
        int convoy = escort.stations().size() * escort.credits();
        int drops = (int) level.groundObjects().stream()
                        .filter(object -> object.drop()
                                .filter(PickupType.MEDIUM_SALVAGE::equals)
                                .isPresent())
                        .count()
                * medium;
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills
                + released
                + turrets
                + convoy
                + drops
                + crates
                + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(714, kills, "539 before the pacing filler: 35 Skitters × 5");
        assertEquals(180, released, "36 Skitters × 5");
        assertEquals(156, turrets);
        assertEquals(150, convoy);
        assertEquals(1_400, total, "kills " + kills + ", released " + released + ", drops " + drops);
        // The pacing filler (2026-10-03) lifts a perfect run 14 % above the curve's 1,225.
        assertEquals(budget, total, budget * 0.15);
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> totals = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            totals.merge(wave.enemy().slug(), wave.count(), Integer::sum);
        }
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            totals.merge(unit.enemy().slug(), 1, Integer::sum);
        }

        assertEquals(Map.of("brood-pod", 6, "needler", 12, "scuttler", 8, "skitter", 50, "spine-turret", 13), totals);
        assertEquals("brood-pod", level.secondary().escapes());
        assertEquals(50, level.secondary().credits());
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(6, sortie.escapesTotal());
        assertEquals(6 + 12 + 8 + 50 + 13 + 36, sortie.enemyTotal(), "the released Skitters count");
        assertEquals(5, sortie.allyCount());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheConvoyThePodsAndTheWalkers(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        EnemySpec pod = enemy(level, "brood-pod");
        EnemySpec scuttler = enemy(level, "scuttler");
        EnemySpec.Brood brood = pod.brood().orElseThrow();
        EnemySpec.Walker walker = scuttler.walker().orElseThrow();
        List<Integer> pincers = level.waves().stream()
                .filter(wave -> wave.enemy().slug().equals("scuttler") && wave.formation() == WaveSpec.Formation.PINCER)
                .map(WaveSpec::count)
                .toList();

        assertEquals(
                switch (difficulty) {
                    case EASY -> 90;
                    case MEDIUM -> 60;
                    case HARD -> 45;
                },
                level.escort().orElseThrow().ally().hp(),
                1e-9);
        assertEquals(difficulty == Difficulty.HARD ? 8 : 6, brood.count());
        assertEquals(difficulty == Difficulty.EASY ? 10 : 8, brood.afterSeconds(), 1e-9);
        assertEquals("skitter", brood.enemy().slug());
        assertEquals(
                difficulty == Difficulty.HARD ? 7 : 5,
                scuttler.gun().orElseThrow().fan());
        assertEquals(difficulty == Difficulty.EASY ? 32 : 40, walker.speed(), 1e-9);
        assertEquals(List.of(2, difficulty == Difficulty.HARD ? 4 : 2), pincers);
        assertEquals(Math.toRadians(45), walker.frontArc(), 1e-9);
        assertTrue(walker.spit().isPresent());
    }

    @Test
    void theDugoutIsHardenedAndHidesTheCacheAndTheSupplyDropAddsACharge() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec dugout = level.groundObjects().stream()
                .filter(object -> object.secretIndex() >= 0)
                .findFirst()
                .orElseThrow();
        LevelScript.GroundObjectSpec drop = level.groundObjects().stream()
                .filter(object -> object.bonusDrop().isPresent())
                .findFirst()
                .orElseThrow();

        assertTrue(dugout.hardened());
        assertEquals(20, dugout.hp(), 1e-9);
        assertEquals(100, dugout.crateCredits());
        assertTrue(!dugout.trigger(), "a destructible, not a trigger");
        assertEquals(Optional.of(PickupType.MEDIUM_SALVAGE), drop.drop());
        assertEquals(Optional.of(PickupType.SPECIAL_CHARGE), drop.bonusDrop());
        assertEquals(3, drop.hp(), 1e-9);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotBringsTheConvoyHome(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        int bursts = 0;
        int hatched = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            step(sortie);
            SimEvents events = sortie.events();
            bursts += events.count(SimEvents.Type.BROOD_BURST);
            hatched += events.count(SimEvents.Type.BROOD_HATCHED);
        }

        LevelResult result = sortie.result();
        int home = 0;
        for (int k = 0; k < sortie.allyCount(); k++) {
            home += sortie.ally(k).alive() ? 1 : 0;
        }
        System.out.printf(
                "Level 04 %s: attempt %d, crawlers home %d/5, kills %d/%d, credits %d (%s), pods hatched %d (%d burst"
                        + " on their own), quick hands %s, grade %s%n",
                difficulty,
                sortie.attempt(),
                home,
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                hatched,
                bursts,
                sortie.secondaryFailed() ? "failed" : "met",
                result.grade().letter());
        assertTrue(sortie.complete());
        assertTrue(home >= 1, "at least one crawler home");
        assertTrue(result.kills() > result.enemies() / 3, "the autopilot shoots many of them down");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        // A first run loads and initialises every class the level touches.
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

    /** A step of the autopilot; after a wreck or a lost convoy, the level again. */
    static void step(Sortie sortie) {
        if (!sortie.flying() || sortie.primaryFailed()) {
            sortie.retry(sortie.ship().defences().maxArmour());
        }
        sortie.step(Autopilot.commands(sortie));
    }

    private static EnemySpec enemy(LevelScript level, String slug) {
        return level.waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals(slug))
                .findFirst()
                .orElseThrow();
    }

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            step(sortie);
        }
    }
}
