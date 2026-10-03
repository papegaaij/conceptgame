package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * Level 03 as the game runs it: its credit budget, the README's totals, the difficulty
 * variations of the clusters, the debris and the Leviathan, the lifeboat rack and whole runs.
 */
class Level03Test {
    static final String LEVEL = "act-1-first-contact/level-03-spore-drift";
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
        int large = content.player().pickups().salvage().credits().large();

        int kills = level.waves().stream()
                .mapToInt(wave -> wave.count() * wave.enemy().bounty())
                .sum();
        int setPieces = level.setPieces().stream()
                .mapToInt(LevelScript.SetPieceSpec::bounty)
                .sum();
        int drops = (int) level.setPieces().stream()
                        .filter(piece -> piece.drop()
                                .filter(PickupType.LARGE_SALVAGE::equals)
                                .isPresent())
                        .count()
                * large;
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills + setPieces + drops + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(630, kills);
        assertEquals(190, setPieces);
        assertEquals(1_145, total, "kills " + kills + ", Leviathan " + setPieces + ", drops " + drops);
        assertEquals(budget, total, budget * 0.05);
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> totals = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            totals.merge(wave.enemy().slug(), wave.count(), Integer::sum);
        }

        assertEquals(Map.of("needler", 9, "skitter", 22, "spore-bomber", 10, "stinger", 6, "whirl-seed", 24), totals);
        assertEquals(
                List.of("leviathan"),
                level.setPieces().stream().map(LevelScript.SetPieceSpec::slug).toList());
        assertEquals("spore-bomber", level.secondary().escapes());
        assertEquals(50, level.secondary().credits());
        assertEquals(10, sortie(content, 1, Difficulty.MEDIUM).escapesTotal());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theWhirlClustersHoldSixSeedsOrEightOnHard(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        List<Integer> clusters = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.WHIRL_CLUSTER)
                .map(WaveSpec::count)
                .toList();

        int size =
                switch (difficulty) {
                    case EASY -> 5; // the formation size lever: 6 × 0.8
                    case MEDIUM -> 6;
                    case HARD -> 8;
                };
        assertEquals(List.of(size, size, size, size), clusters);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theLeviathanHoldsThirtySecondsOrTwentyFourOnHard(Difficulty difficulty) {
        LevelScript.SetPieceSpec leviathan =
                SimSpecs.level(content, LEVEL, difficulty).setPieces().getFirst();
        LevelScript.Pass second = leviathan.passes().get(1);

        assertEquals(difficulty == Difficulty.HARD ? 24 : 30, second.leaveAt() - second.descendAt(), 1e-9);
        int hp = (int)
                leviathan.parts().stream().mapToDouble(LevelScript.PartSpec::hp).sum();
        assertEquals(
                switch (difficulty) {
                    case EASY -> 382;
                    case MEDIUM -> 510;
                    case HARD -> 663;
                },
                hp);
        assertEquals(190, leviathan.bounty());
        assertEquals(25, leviathan.contactDamage());
        assertEquals(java.util.Optional.of(PickupType.LARGE_SALVAGE), leviathan.drop());
    }

    @Test
    void theVentsFireStaggeredAndTheFinsEveryFourSeconds() {
        LevelScript.SetPieceSpec leviathan =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).setPieces().getFirst();
        List<Double> firsts = leviathan.parts().stream()
                .filter(part -> part.gun().isPresent())
                .map(LevelScript.PartSpec::firstShotSeconds)
                .toList();

        List<Double> expected = List.of(0.6, 1.2, 1.8, 2.4, 4.0, 4.0);
        assertEquals(expected.size(), firsts.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), firsts.get(i), 1e-9, "part " + i);
        }
        assertEquals(2.2, leviathan.parts().get(1).gun().orElseThrow().intervalSeconds(), 1e-9);
        assertEquals(1, leviathan.parts().get(1).gun().orElseThrow().burst());
        assertEquals(
                2,
                SimSpecs.level(content, LEVEL, Difficulty.HARD)
                        .setPieces()
                        .getFirst()
                        .parts()
                        .get(1)
                        .gun()
                        .orElseThrow()
                        .burst(),
                "hard: 2-orb vent bursts");
        assertEquals(5, leviathan.parts().get(5).gun().orElseThrow().fan());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void easyLeavesOutEverySecondLargeChunkAndHardDriftsThemFaster(Difficulty difficulty) {
        List<LevelScript.DebrisSpec> debris =
                SimSpecs.level(content, LEVEL, difficulty).debris();
        long large = debris.stream().filter(LevelScript.DebrisSpec::large).count();
        double fastest = debris.stream()
                .mapToDouble(chunk -> Math.hypot(chunk.vx(), chunk.vy()))
                .max()
                .orElseThrow();

        assertEquals(difficulty == Difficulty.EASY ? 5 : 10, large);
        assertEquals(10, debris.size() - large, "every small chunk on every difficulty");
        assertTrue(fastest <= (difficulty == Difficulty.HARD ? 60 : 40), "fastest " + fastest);
        assertTrue(debris.stream().filter(LevelScript.DebrisSpec::large).allMatch(chunk -> chunk.damage() == 15));
        assertTrue(debris.stream().filter(chunk -> !chunk.large()).allMatch(chunk -> chunk.hp() == 6));
    }

    @Test
    void theFourLifeboatLightsRevealOneCrateTogether() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<LevelScript.GroundObjectSpec> lights = level.groundObjects().stream()
                .filter(LevelScript.GroundObjectSpec::trigger)
                .toList();

        assertEquals(4, lights.size());
        assertTrue(lights.stream()
                .allMatch(light -> light.hits() == 1
                        && light.secretTriggers() == 4
                        && light.crateCredits() == 75
                        && light.secret().equals("lifeboat rack")));
        assertEquals(1, level.secrets());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheWholeLevel(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        boolean descended = false;
        int parts = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level01Test.step(sortie);
            SimEvents events = sortie.events();
            descended |= events.count(SimEvents.Type.SET_PIECE_DESCENDED) > 0;
            parts += events.count(SimEvents.Type.PART_DESTROYED);
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 03 %s: attempt %d, kills %d/%d, credits %d (%s), Leviathan %s (%d parts), bombers %d/%d%s, grade %s%n",
                difficulty,
                sortie.attempt(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                sortie.setPiece(0).destroyed()
                        ? "destroyed"
                        : sortie.setPiece(0).escaped() ? "escaped" : "?",
                parts,
                sortie.escapesDestroyed(),
                sortie.escapesTotal(),
                sortie.secondaryFailed() ? " (one got through)" : "",
                result.grade().letter());
        assertTrue(sortie.complete());
        assertTrue(descended, "the Leviathan came down to the player's layer");
        assertTrue(sortie.setPiece(0).destroyed() || sortie.setPiece(0).escaped());
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

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level01Test.step(sortie);
        }
    }
}
