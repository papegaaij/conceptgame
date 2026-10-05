package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Armament;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WarningEdge;
import vanguard.sim.WaveSpec;

/**
 * Level 06 as the game runs it: its credit budget, the README's totals, the difficulty variations
 * (the loop-backs' warnings, the hard Coilwyrm pair, the flares and the headlight), Rook's "six"
 * lines at the start of the rear strikes' warnings, the survey cache and the data core, and whole
 * runs in which the autopilot flies the level to its end.
 */
class Level06Test {
    static final String LEVEL = "act-1-first-contact/level-06-farside";
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 06 (design/player/balance-plan.yaml): Level 05's
     * fit with the Pulse Cannon at L3.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 3),
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
    void aPerfectRunAtMediumEarnsTheReadmesTotal() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        int kills = 0;
        for (WaveSpec wave : level.waves()) {
            EnemySpec enemy = wave.enemy();
            kills += wave.count() * enemy.bounty();
            if (enemy.chain().isPresent()) {
                EnemySpec.ChainSpec chain = enemy.chain().orElseThrow();
                kills += wave.count()
                        * (chain.segmentBoxes().size() * chain.segment().bounty()
                                + chain.tail().bounty());
            }
        }
        int ground = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        int medium = content.player().pickups().salvage().credits().medium();
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills + ground + medium + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(982, Math.round(budget), "the typical haul's target");
        assertEquals(
                4 * 86,
                level.waves().stream()
                        .filter(wave -> wave.enemy().slug().equals("coilwyrm"))
                        .mapToInt(wave -> wave.count() * 86)
                        .sum());
        assertEquals(210, ground, "Spine Turret 10 × 12 + Polyp Mortar 6 × 15");
        assertEquals(135, crates, "the survey cache; the data core pays none");
        assertEquals(50, level.secondary().credits());
        System.out.printf(
                "Level 06 budget: kills %d, ground %d, ore cart %d, crate %d, secondary %d: %d of %.0f (%+.1f %%)%n",
                kills,
                ground,
                medium,
                crates,
                level.secondary().credits(),
                total,
                budget,
                100 * (total - budget) / budget);
        assertEquals(2_059, total, "before the bounty_scale (TypicalHaulTest checks the scaled run)");
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

        assertEquals(Map.of("polyp-mortar", 6, "spine-turret", 10), ground);
        assertEquals(Map.of("coilwyrm", 4, "mantis", 8, "needler", 20, "skitter", 122, "stinger", 12), waves);
        assertEquals("mantis", level.secondary().escapes());
        assertEquals(
                List.of(25.0, 70.0, 110.0, 160.0, 190.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(8, sortie.escapesTotal(), "every Mantis");
        assertEquals(122 + 8 + 20 + 12 + 4 + 16, sortie.enemyTotal(), "a Coilwyrm counts once");
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheLoopBacksTheCoilwyrmPairTheFlaresAndTheHeadlight(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        int coilwyrms = 0;
        int skitterSnakesAt146 = 0;
        for (WaveSpec wave : level.waves()) {
            if (wave.enemy().slug().equals("coilwyrm")) {
                coilwyrms += wave.count();
                if (wave.loopBack().isPresent()) {
                    assertEquals(
                            difficulty == Difficulty.EASY ? 4.0 : 3.0,
                            Math.max(3, wave.warningSeconds().orElse(0.0)),
                            1e-9,
                            "the loop-back's warning");
                }
            }
            if (wave.t() == 146 && wave.enemy().slug().equals("skitter")) {
                skitterSnakesAt146++;
            }
        }
        assertEquals(difficulty == Difficulty.HARD ? 6 : 4, coilwyrms, "hard: the pair crossing at t=146");
        assertEquals(difficulty == Difficulty.HARD ? 0 : 1, skitterSnakesAt146, "replaced on hard");
        LevelScript.Darkness darkness = level.darkness().orElseThrow();
        assertEquals(difficulty == Difficulty.HARD ? 2 : 3, darkness.flares().size(), "hard: no t=112 flare");
        assertEquals(difficulty == Difficulty.EASY ? 12 : 8, darkness.flareSeconds(), 1e-9);
        assertEquals(difficulty == Difficulty.EASY ? 260 : 200, darkness.headlightLength(), 1e-9);
        EnemySpec coilwyrm = level.waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("coilwyrm"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                difficulty == Difficulty.HARD ? 14 : 12,
                coilwyrm.chain().orElseThrow().segmentBoxes().size());
    }

    /**
     * Rook calls each rear strike at the start of its bottom-edge warning (README, Radio chatter):
     * the "six" lines lie within half a second of the warnings' starts at medium.
     */
    @Test
    void rooksSixLinesStartWithTheRearWarnings() {
        Sortie sortie = new Sortie(
                5,
                planLoadout(content, Difficulty.MEDIUM),
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                planLoadout(content, Difficulty.MEDIUM).plating().maxArmour());
        List<Double> starts = new ArrayList<>();
        boolean before = false;
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            sortie.step(Autopilot.commands(sortie));
            boolean bottom = (sortie.edgeWarnings() & WarningEdge.BOTTOM.bit()) != 0;
            if (bottom && !before) {
                starts.add(sortie.levelSeconds());
            }
            before = bottom;
        }
        List<Double> six = sortie.script().radio().stream()
                .filter(cue -> cue.speaker().equals("Rook") && cue.line().matches("(?s).*(six|round).*"))
                .map(LevelScript.RadioCue::t)
                .toList();
        System.out.printf("Level 06 rear warnings from %s s; Rook's calls at %s s%n", starts, six);
        assertEquals(3, starts.size(), "two loop-backs and the rear entry");
        for (int i = 0; i < starts.size(); i++) {
            assertEquals(starts.get(i), six.get(i), 0.5, "Rook's call of rear strike " + (i + 1));
        }
    }

    @Test
    void theSurveyCacheIsDarkAndTheTerminalHoldsTheDataCore() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec cache = level.groundObjects().stream()
                .filter(o -> o.secret().equals("survey cache"))
                .findFirst()
                .orElseThrow();
        assertTrue(cache.dark());
        assertEquals(3, cache.hits());
        assertEquals(135, cache.crateCredits());
        LevelScript.GroundObjectSpec terminal = level.groundObjects().stream()
                .filter(o -> o.secret().equals("settlement log"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                new LevelResult.DataCore("settlement log", "Targeting computer"),
                terminal.core().orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEnd(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        int sweeps = 0;
        int regrown = 0;
        int secrets = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level05Test.step(sortie);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SWEEP_TELEGRAPH -> sweeps++;
                    case CHAIN_REGROWN -> regrown++;
                    case SECRET_FOUND -> secrets++;
                    default -> {}
                }
            }
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 06 %s: attempt %d, kills %d/%d, credits %d (%s), sweeps %d, regrown heads %d, secrets %d,"
                        + " data cores %s, clear the edges %s, armour lost %.0f, grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                sweeps,
                regrown,
                secrets,
                result.dataCores(),
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                result.armourDamage(),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        assertTrue(sweeps > 0, "the Mantis swept");
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

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level05Test.step(sortie);
        }
    }
}
