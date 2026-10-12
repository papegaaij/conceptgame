package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.BossSpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * M5 part H, step H4: Level 14 Siege Spire from its data (design/campaign/act-2-homefront/level-14-siege-spire;
 * decisions H1–H10 = a of 2026-10-11 and the stated defaults, pending the user's review): the README's
 * totals, the arena at speed 0 where the Spire arrives at t=108 with the warning and the checkpoint (none
 * on hard), the four root bursts and their growths in the secondary's group, the daylight section after the
 * arena, the radio's events, the pickups, and the autopilot flying the level to its end with the plan's fit
 * and Rook on every difficulty.
 */
class Level14Test {
    static final String LEVEL = "act-2-homefront/level-14-siege-spire";
    private static final int MAX_STEPS = 60 * 60 * 30;

    private final Content content = ContentLoader.fromClasspath();

    /** The balance plan's fit at Level 14 (SiegeSpireTest's): Level 13's with the Tail Gun at L2, 4 Airstrikes. */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SiegeSpireTest.planLoadout(content, difficulty);
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
    void aPerfectRunAtMediumEarnsTheReadmesTotal() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        int kills = 0;
        for (WaveSpec wave : level.waves()) {
            kills += wave.count() * wave.enemy().bounty();
        }
        int ground = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        int boss = level.setPieces().getFirst().parts().stream()
                .mapToInt(LevelScript.PartSpec::bounty)
                .sum();
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int secondary = level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_687, Math.round(budget), "the typical haul's target, 700 × 1.07¹³");
        assertEquals(55 * 5 + 30 * 12 + 6 * 22 + 4 * 18 + 4 * 30, kills, "Skitter, Needler, Creeper, Ravager, Wraith");
        assertEquals(
                (3 + 4) * 12 + 2 * 45 + 4 * 15,
                ground,
                "the cathedral nest's and the root bursts' Spine Turrets, the hive nodes, the bursts' Polyp Mortars");
        assertEquals(6 * 31 + 62 + 124, boss, "the pods and mortar roots, the maw and the core (Act 1 terms)");
        assertEquals(88, crates, "the armoured car's crate in Act 1 terms (pays 141)");
        assertEquals(63, secondary, "the root grid in Act 1 terms (pays 101)");
        assertEquals(0.76, content.level(LEVEL).bounties());
        double paid =
                boss * content.economy().actFactor() * content.level(LEVEL).bounties();
        System.out.printf(
                "Level 14 (Act 1 terms): kills %d, ground %d, boss %d (paid %.0f, %.1f %% of the budget), crate %d,"
                        + " secondary %d%n",
                kills, ground, boss, paid, 100 * paid / budget, crates, secondary);
        assertEquals(0.27, paid / budget, 0.01, "the act boss's share, about 30 % (BalanceTest's tolerance)");
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> waves = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            waves.merge(wave.enemy().slug(), wave.count(), Integer::sum);
        }
        assertEquals(Map.of("creeper", 6, "needler", 30, "ravager", 4, "skitter", 55, "wraith", 4), waves);
        Map<String, Integer> ground = new TreeMap<>();
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            ground.merge(unit.enemy().slug(), 1, Integer::sum);
        }
        assertEquals(Map.of("spine-turret", 7, "hive-node", 2, "polyp-mortar", 4), ground);
        assertEquals(
                List.of(20.0, 60.0, 95.0, 108.0, 250.0, 262.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        Map<Difficulty, Integer> enemies = new TreeMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            LevelScript at = SimSpecs.level(content, LEVEL, difficulty);
            enemies.put(
                    difficulty,
                    at.waves().stream().mapToInt(WaveSpec::count).sum()
                            + at.groundUnits().size());
        }
        System.out.println("Level 14 enemies per difficulty (waves and ground units): " + enemies);
    }

    /**
     * H1 = a: the arena at speed 0 from t=108, where the Siege Spire arrives with the warning and the
     * checkpoint (easy and medium; none on hard), the level clock halting at its end (t=250) while the Spire
     * lives; the daylight section of 12 s at 25 px/s after it (steps H7b and H9: a drift), with no enemy in it.
     */
    @Test
    void theSpireArrivesAtTheArenaStartWithTheWarning() {
        LevelData data = content.level(LEVEL);
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.Section arena = level.sections().get(4);
        assertTrue(arena.arena());
        assertEquals(0, arena.speed());
        assertEquals(25, level.sections().get(5).speed(), "the daylight drifts (steps H7b, H9)");
        BossSpec boss = level.setPieces().getFirst().boss().orElseThrow();
        assertTrue(boss.anchored(), "an arena boss without lanes (H1 = a)");
        assertEquals(0, boss.arena().orElseThrow().lanes().count());
        assertEquals(108, boss.arriveSeconds(), "the arena's start, the warning and the checkpoint (the default)");
        assertEquals("SIEGE SPIRE", boss.barName());
        assertFalse(boss.midBoss(), "the act boss's long bar");
        assertEquals("reach-end", data.objectives().primary());
        assertTrue(
                level.waves().stream().allMatch(wave -> wave.t() < 108), "nothing enters in the fight or the daylight");
        assertTrue(level.groundUnits().stream().allMatch(unit -> unit.t() < 108));
        Map<String, Boolean> checkpoint = new TreeMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            checkpoint.put(
                    difficulty.name(), content.difficulty().bossCheckpoint().of(difficulty));
        }
        assertEquals(Map.of("EASY", true, "MEDIUM", true, "HARD", false), checkpoint, "no boss checkpoint on hard");
    }

    /**
     * H2 = a: four root bursts at t=63, 74, 84 and 92, each growing a Spine Turret and a Polyp Mortar (easy a
     * Spine Turret only) 1 s after its telegraph, silent for 0.75 s (hard 0.5 s; step H7b), 100–400 px below the top
     * edge; the growths are the secondary's group "roots": 8 (easy 4).
     */
    @Test
    void theRootBurstsGrowTheSecondarysGroup() {
        for (Difficulty difficulty : Difficulty.values()) {
            LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
            List<LevelScript.GroundUnit> growths = level.groundUnits().stream()
                    .filter(unit -> unit.burst().isPresent())
                    .toList();
            assertEquals(difficulty == Difficulty.EASY ? 4 : 8, growths.size(), difficulty + ": the growths");
            assertEquals(
                    List.of(63.0, 74.0, 84.0, 92.0),
                    growths.stream()
                            .map(unit -> unit.burst().orElseThrow().t())
                            .distinct()
                            .toList());
            for (LevelScript.GroundUnit unit : growths) {
                LevelScript.RootBurst burst = unit.burst().orElseThrow();
                assertEquals(burst.t() + 1.0, unit.t(), 1e-9, "the growths appear after the 1 s telegraph");
                assertEquals(difficulty == Difficulty.HARD ? 0.5 : 0.75, burst.firstShotSeconds());
                assertEquals(0, unit.group(), "the secondary's group roots");
                assertTrue(difficulty != Difficulty.EASY || unit.enemy().slug().equals("spine-turret"));
            }
            assertEquals(List.of("roots"), level.secondary().groups());
        }
    }

    @Test
    void theRadioThePickupsAndTheBriefing() {
        LevelData data = content.level(LEVEL);
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<LevelScript.CueTrigger> events = level.radio().stream()
                .map(LevelScript.RadioCue::trigger)
                .filter(trigger -> trigger != LevelScript.CueTrigger.TIME)
                .toList();
        assertEquals(
                List.of(
                        LevelScript.CueTrigger.BOSS_PHASE,
                        LevelScript.CueTrigger.BOSS_PHASE,
                        LevelScript.CueTrigger.BOSS_PHASE,
                        LevelScript.CueTrigger.BOSS_DESTROYED,
                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE,
                        LevelScript.CueTrigger.LEVEL_END,
                        LevelScript.CueTrigger.SECRET),
                events,
                "the arena's lines are events; the car's line as its secret is found");
        assertEquals(
                List.of(PickupType.OVERDRIVE),
                level.partDrops().stream().map(LevelScript.PartDrop::pickup).toList(),
                "the maw's overdrive");
        assertEquals(
                List.of(PickupType.ARMOUR_PATCH),
                level.placedPickups().stream()
                        .map(LevelScript.PlacedPickup::pickup)
                        .toList(),
                "the supply drone's patch");
        assertEquals(2, data.briefing().pages().size(), "Okafor's and Varga's pages");
        assertEquals("Rook", data.briefing().teaser().speaker());
        assertEquals("DESTROY THE SIEGE SPIRE", data.threatProfile().objective().orElseThrow());
        assertEquals(
                "ROOTS", data.objectives().secondary().orElseThrow().label().orElseThrow());
    }

    /**
     * The autopilot flies the level to its end with the plan's fit and Rook on every difficulty. Printed: the
     * attempts, the fight's length (the bar to the kill), the secondary, the armour lost, the car, the credits
     * and the grade.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEnd(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        double fight = -1;
        int secrets = 0;
        String deaths = "";
        int attempt = sortie.attempt();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            double t = sortie.levelSeconds();
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                deaths += String.format("%.0f ", t);
            }
            SetPiece spire = sortie.setPiece(0);
            if (spire.destroyed() && fight < 0) {
                fight = spire.killSeconds();
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                secrets += events.type(i) == SimEvents.Type.SECRET_FOUND ? 1 : 0;
            }
        }
        LevelResult result = sortie.result();
        System.out.printf(
                "Level 14 %s roots %d of %d%n", difficulty, sortie.groupUnitsDestroyed(0), sortie.groupUnits(0));
        System.out.printf(
                "Level 14 %s: attempt %d%s, real %.1f s, fight %.1f s, kills %d/%d, secondary %s, car %s, credits %d"
                        + " (%s), Airstrike charges used %d, Rook %s, grade %s%n",
                difficulty,
                sortie.attempt(),
                deaths.isEmpty() ? "" : " (lost at t=" + deaths.strip() + ")",
                sortie.realSeconds(),
                fight,
                result.kills(),
                result.enemies(),
                result.secondaryMet() ? "met" : "failed",
                secrets > 0 ? "found" : "missed",
                result.credits().total(),
                result.credits(),
                sortie.special().used(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter());
        assertTrue(sortie.complete());
        assertTrue(fight > 0, "the Spire killed");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level14Test::fly);

        assertEquals(0, allocated, "the level allocated " + allocated + " bytes");
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameState() {
        Sortie first = sortie(content, 77, Difficulty.MEDIUM);
        Sortie second = sortie(content, 77, Difficulty.MEDIUM);
        fly(first);
        fly(second);

        assertEquals(first.stateHash(), second.stateHash());
    }

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
        }
    }
}
