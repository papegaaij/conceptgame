package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/** Level 01 as the game runs it: its credit budget, whole headless runs at every difficulty and allocation. */
class Level01Test {
    static final String LEVEL = "act-1-first-contact/level-01-break-at-dawn";
    /** Long enough for a few retries. */
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    static Sortie sortie(Content content, long seed, Difficulty difficulty) {
        return new Sortie(
                seed,
                SimSpecs.starterLoadout(content, difficulty),
                SimSpecs.level(content, LEVEL, difficulty),
                SimSpecs.rules(content, LEVEL, difficulty));
    }

    @Test
    void aPerfectRunAtMediumEarnsTheLevelBudget() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        int salvage = content.player().pickups().salvage().credits().small();

        int kills = level.waves().stream()
                .mapToInt(wave -> wave.count() * wave.enemy().bounty())
                .sum();
        int ground = level.groundObjects().stream()
                .mapToInt(object -> object.bounty()
                        + (object.drop()
                                        .filter(PickupType.SMALL_SALVAGE::equals)
                                        .isPresent()
                                ? salvage
                                : 0))
                .sum();
        int crates = level.groundObjects().stream()
                .mapToInt(LevelScript.GroundObjectSpec::crateCredits)
                .sum();
        int total = kills + ground + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(budget, total, budget * 0.05, "kills " + kills + ", ground " + ground + ", crates " + crates);
    }

    @Test
    void theSecondaryObjectiveNeeds80PercentOfTheEnemies() {
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);

        assertEquals(95, sortie.enemyTotal());
        assertEquals(76, sortie.requiredKills());
        assertEquals(
                95,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).waves().stream()
                        .mapToInt(WaveSpec::count)
                        .sum());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheWholeLevel(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            sortie.step(Autopilot.commands(sortie));
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 01 %s: attempt %d, kills %d/%d, credits %d (%s), score %d, grade %s (%.1f)%n",
                difficulty,
                sortie.attempt(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                result.score(),
                result.grade().letter(),
                result.rating());
        assertTrue(sortie.complete());
        assertTrue(result.kills() > result.enemies() / 2, "the autopilot shoots most of them down");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        // A first run loads and initialises every class the level touches (Trig's table, enum switch maps, ...).
        fly(sortie(content, 2185, Difficulty.HARD));
        Sortie sortie = sortie(content, 2185, Difficulty.HARD);

        long before = threads.getCurrentThreadAllocatedBytes();
        fly(sortie);
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "the level allocated " + allocated + " bytes");
    }

    private static void fly(Sortie sortie) {
        while (!sortie.complete()) {
            sortie.step(Autopilot.commands(sortie));
        }
    }
}
