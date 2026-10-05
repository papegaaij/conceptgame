package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WarningEdge;
import vanguard.sim.WaveSpec;

/**
 * Level 01 as the game runs it: its credit budget, the t=134 wave's warning and radio line, whole
 * headless runs at every difficulty and allocation.
 */
class Level01Test {
    static final String LEVEL = "act-1-first-contact/level-01-break-at-dawn";
    /** Long enough for a few retries. */
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

    /** Flies a step of the autopilot; a destroyed ship retries at full armour, as the presentation would. */
    static void step(Sortie sortie) {
        if (!sortie.flying()) {
            sortie.retry(sortie.ship().defences().maxArmour());
        }
        sortie.step(Autopilot.commands(sortie));
    }

    @Test
    void aPerfectRunAtMediumEarnsTheReadmesTotal() {
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

        // The budget is the typical haul now (TypicalHaulTest); a perfect run earns well above it.
        assertEquals(1_000, total, "kills " + kills + ", ground " + ground + ", crates " + crates);
    }

    @Test
    void theSecondaryObjectiveNeeds65PercentOfTheEnemies() {
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);

        assertEquals(95, sortie.enemyTotal());
        assertEquals(62, sortie.requiredKills());
        assertEquals(
                95,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).waves().stream()
                        .mapToInt(WaveSpec::count)
                        .sum());
    }

    @ParameterizedTest
    @EnumSource(
            value = Difficulty.class,
            names = {"EASY", "MEDIUM"})
    void theT134WaveIsWarnedWhereItEnters(Difficulty difficulty) {
        boolean rear = difficulty != Difficulty.EASY;
        Sortie sortie = new Sortie(
                2185,
                SimSpecs.starterLoadout(content, difficulty),
                SimSpecs.level(content, LEVEL, difficulty),
                SimSpecs.rules(content, LEVEL, difficulty).withInvulnerableShip(),
                1);
        int warned = 0;
        String line = "";
        while (sortie.levelSeconds() < 134) {
            sortie.step(0);
            if (sortie.levelSeconds() > 130) {
                warned |= sortie.edgeWarnings();
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.RADIO && sortie.levelSeconds() > 130) {
                    line = sortie.script().radio().get(events.value(i)).line();
                }
            }
        }

        assertEquals(rear ? WarningEdge.BOTTOM.bit() : 0, warned, "the bottom edge warns only for a rear entry");
        assertEquals(rear ? "Contacts on your six, Lancer!" : "More contacts, dead ahead!", line);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheWholeLevel(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            step(sortie);
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
                result.rating().total());
        assertTrue(sortie.complete());
        assertTrue(result.kills() > result.enemies() / 2, "the autopilot shoots most of them down");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level01Test::fly);

        assertEquals(0, allocated, "the level allocated " + allocated + " bytes");
    }

    private static void fly(Sortie sortie) {
        while (!sortie.complete()) {
            step(sortie);
        }
    }
}
