package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/** Level 02 as the game runs it: its credit budget, the docks' nests per difficulty, Crane Four and whole runs. */
class Level02Test {
    static final String LEVEL = "act-1-first-contact/level-02-shipyard-burning";
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
        int salvage = content.player().pickups().salvage().credits().small();

        int kills = level.waves().stream()
                .mapToInt(wave -> wave.count() * wave.enemy().bounty())
                .sum();
        int turrets = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        int pods = level.groundObjects().stream()
                .mapToInt(object -> object.bounty()
                        + (object.drop()
                                        .filter(PickupType.SMALL_SALVAGE::equals)
                                        .isPresent()
                                ? salvage
                                : 0))
                .sum();
        int crates = level.cranes().stream()
                .mapToInt(LevelScript.CraneSpec::crateCredits)
                .sum();
        int docks = level.secondary().credits() * level.secondary().groups().size();
        int total = kills + turrets + pods + crates + docks;

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_070, total, "kills " + kills + ", turrets " + turrets + ", pods " + pods);
        assertEquals(budget, total, budget * 0.05);
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);

        assertEquals(
                64, level.waves().stream().mapToInt(WaveSpec::count).sum(), "34 Skitters, 16 Needlers, 14 Stingers");
        assertEquals(24, level.groundUnits().size(), "24 Spine Turrets");
        assertEquals(
                List.of("Dock One", "Dock Two", "Dock Three", "Dock Four"),
                level.secondary().groups());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void eachDockCarriesTwoThreeOrFourTurrets(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        int expected =
                switch (difficulty) {
                    case EASY -> 2;
                    case MEDIUM -> 3;
                    case HARD -> 4;
                };

        for (int dock = 0; dock < 4; dock++) {
            int group = dock;
            assertEquals(
                    expected,
                    level.groundUnits().stream()
                            .filter(unit -> unit.group() == group)
                            .count(),
                    level.secondary().groups().get(dock));
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void craneFourSweepsOnceTwiceOrThreeTimes(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        int expected =
                switch (difficulty) {
                    case EASY -> 1;
                    case MEDIUM -> 2;
                    case HARD -> 3;
                };

        assertEquals(expected, level.cranes().getFirst().swings().size());
        assertEquals(80, level.cranes().getFirst().crateCredits());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheWholeLevel(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level01Test.step(sortie);
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 02 %s: attempt %d, kills %d/%d, credits %d (%s), docks %s, grade %s%n",
                difficulty,
                sortie.attempt(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                docks(sortie),
                result.grade().letter());
        assertTrue(sortie.complete());
        assertTrue(result.kills() > result.enemies() / 3, "the autopilot shoots many of them down");
    }

    private static String docks(Sortie sortie) {
        StringBuilder text = new StringBuilder();
        for (int g = 0; g < sortie.groupCount(); g++) {
            text.append("-CL".charAt(sortie.groupState(g)));
        }
        return text.toString();
    }
}
