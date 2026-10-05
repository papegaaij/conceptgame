package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.Armament;
import vanguard.sim.BossSpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;

/**
 * Level 07 as the game runs it: its credit budget, the README's totals, the Brood Carrier's script
 * from its data (the overhead pass, the move into broadside, the windows and their spawns, the core,
 * the fire-only mandibles, the difficulty hooks), the level's rules (the lifeboat tow, the part drop,
 * the bays secondary, the homing radio lines, the difficulty variations), and whole runs in which the
 * autopilot flies the approach and brings the carrier down through its three phases.
 */
class Level07Test {
    static final String LEVEL = "act-1-first-contact/level-07-brood-carrier";
    private static final int MAX_STEPS = 60 * 60 * 20;
    private static final List<String> SACS = List.of(
            "bay 1 left",
            "bay 1 right",
            "bay 2 left",
            "bay 2 right",
            "bay 3 left",
            "bay 3 right",
            "bay 4 left",
            "bay 4 right");

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 07 (design/player/balance-plan.yaml): Level 06's fit
     * (the Pulse Cannon at L3, an Autocannon Pod on each wing, the second shield) with Composite I
     * plating. PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return loadout(content, difficulty, "autocannon-pod");
    }

    /** The plan's fit with {@code wings} on both wing mounts (the Micro-missile Pod for the homing runs). */
    static Loadout loadout(Content content, Difficulty difficulty, String wings) {
        return SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 3),
                        new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, wings, 1),
                        new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, wings, 1)),
                content.shields().models().get(1).name(),
                content.armour().plating().get(1).name(),
                0,
                difficulty);
    }

    static Sortie sortie(Content content, long seed, Difficulty difficulty, Loadout loadout) {
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
        int released = 0;
        for (WaveSpec wave : level.waves()) {
            kills += wave.count() * wave.enemy().bounty();
            released += wave.enemy()
                    .brood()
                    .map(brood -> wave.count() * brood.count() * brood.enemy().bounty())
                    .orElse(0);
        }
        LevelScript.SetPieceSpec carrier = level.setPieces().getFirst();
        int parts =
                carrier.parts().stream().mapToInt(LevelScript.PartSpec::bounty).sum();
        // the window launches of a typical fight (boss.notes.spawns): 41 Skitters, 8 Needlers
        int spawns = 41 * 5 + 8 * 12;
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total =
                kills + released + parts + spawns + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_051, Math.round(budget), "the typical haul's target");
        assertEquals(450, parts, "bays 8 × 25, core 250; the mandibles pay nothing");
        assertEquals(4 * 6 * 5, released, "the four Brood Pods' Skitters");
        assertEquals(75, crates, "the lifeboat tow");
        assertEquals(50, level.secondary().credits());
        System.out.printf(
                "Level 07 budget: kills %d, released %d, carrier %d, spawns %d, crate %d, secondary %d: %d of %.0f%n",
                kills, released, parts, spawns, crates, level.secondary().credits(), total, budget);
        assertEquals(1_972, total, "before the bounty_scale (TypicalHaulTest checks the scaled run)");
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> waves = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            waves.merge(wave.enemy().slug(), wave.count(), Integer::sum);
        }
        assertEquals(
                Map.of(
                        "brood-pod", 4,
                        "mantis", 2,
                        "needler", 23,
                        "skitter", 62,
                        "spore-bomber", 4,
                        "stinger", 10),
                waves);
        assertTrue(level.groundUnits().isEmpty(), "the picket is wreckage");
        assertEquals(
                List.of(30.0, 70.0, 100.0, 235.0, 270.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertTrue(level.sections().get(3).arena());
        assertEquals(20, level.sections().get(3).speed(), 1e-9);
        assertEquals(40, level.sections().get(4).speed(), 1e-9);
        assertEquals(35, level.seconds() - level.sections().get(3).end(), 1e-9, "the aftermath");
    }

    @Test
    void theCarrierFliesItsScript() {
        LevelScript.SetPieceSpec carrier =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).setPieces().getFirst();
        BossSpec boss = carrier.boss().orElseThrow();
        assertEquals("brood-carrier", carrier.slug());
        assertEquals(Layer.HIGH_AIR, boss.layer());
        assertFalse(boss.midBoss(), "the act boss's long bar");
        assertTrue(boss.engagesOnArrival());
        assertEquals(100, boss.arriveSeconds(), 1e-9);
        assertEquals(100, boss.parSeconds(), 1e-9);
        assertEquals(3, boss.deathSeconds(), 1e-9);
        assertEquals(PlayField.HEIGHT - 270, boss.hoverY(), 1e-9);
        assertEquals(40, boss.descentSpeed(), 1e-9);
        assertEquals(
                List.of("arrival", "broadside"),
                boss.poses().stream().map(BossSpec.Pose::name).toList(),
                "exactly two poses, nose-down first");
        assertEquals(List.of(carrier.parts().size() - 1), boss.armoured(), "the mandibles are fire-only");
        assertEquals(
                3_840,
                carrier.parts().stream().mapToDouble(LevelScript.PartSpec::hp).sum() - 1,
                1e-9);

        // part_list tail to head: nose-down, every part lies further down the hull than the one before
        for (int p = 1; p < carrier.parts().size(); p++) {
            assertTrue(
                    carrier.parts().get(p).dy() <= carrier.parts().get(p - 1).dy(),
                    carrier.parts().get(p).name() + " lies nearer the head");
        }
        // broadside: the head to the right, inside the field at the station
        BossSpec.Pose broadside = boss.poses().get(1);
        BossSpec.Phase overhead = boss.phases().get(0);
        BossSpec.Phase side = boss.phases().get(1);
        BossSpec.Phase core = boss.phases().get(2);
        BossSpec.Move move = side.move().orElseThrow();
        assertEquals(170, move.x(), 1e-9);
        assertEquals(PlayField.HEIGHT - 150, move.y(), 1e-9);
        assertEquals(Layer.AIR, move.layer());
        assertEquals(4, move.descendSeconds() + move.turnSeconds(), 1e-9, "the turn's about 4 s");
        for (int p = 0; p < carrier.parts().size(); p++) {
            double x = move.x() + broadside.offsets().get(p).dx();
            double y = move.y() + broadside.offsets().get(p).dy();
            assertTrue(
                    inField(x, y, carrier.parts().get(p).box()),
                    carrier.parts().get(p).name() + " in the field at the broadside station");
        }

        assertEquals(
                List.of("Overhead", "Broadside", "Core"),
                boss.phases().stream().map(BossSpec.Phase::name).toList());
        assertTrue(overhead.timed());
        assertEquals(25, overhead.seconds(), 1e-9);
        assertTrue(overhead.untilParts().isEmpty());
        BossSpec.Windows pass = overhead.windows().orElseThrow();
        assertEquals(2.5, pass.everySeconds(), 1e-9);
        assertEquals(6, pass.offsetSeconds(), 1e-9);
        assertEquals(
                List.of(4, 2), pass.spawns().stream().map(BossSpec.Spawn::count).toList());
        assertEquals(
                List.of("skitter", "needler"),
                pass.spawns().stream().map(s -> s.enemy().slug()).toList());
        assertEquals(70, side.seconds(), 1e-9, "the broadside times out");
        assertEquals(SACS.size(), side.untilParts().size());
        assertEquals(1, side.windows().orElseThrow().spawns().getFirst().count() / 2, "1 Skitter per open sac");
        assertEquals(
                List.of(BossSpec.Pattern.FAN),
                side.attacks().stream()
                        .map(a -> boss.attacks().get(a).pattern())
                        .toList());
        assertEquals(1, core.delaySeconds(), 1e-9, "the iris opens");
        assertTrue(core.windows().orElseThrow().all(), "after a timeout every surviving pair opens together");
        assertEquals(
                List.of(BossSpec.Pattern.SPIRAL, BossSpec.Pattern.RING),
                core.attacks().stream()
                        .map(a -> boss.attacks().get(a).pattern())
                        .toList());
        assertFalse(core.alternate(), "the spiral and the ring together");
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultyHooksChangeTheSpawnsAndTheSpiral(Difficulty difficulty) {
        BossSpec boss = SimSpecs.level(content, LEVEL, difficulty)
                .setPieces()
                .getFirst()
                .boss()
                .orElseThrow();
        List<Integer> pass = boss.phases().get(0).windows().orElseThrow().spawns().stream()
                .map(BossSpec.Spawn::count)
                .toList();
        int broadside =
                boss.phases().get(1).windows().orElseThrow().spawns().getFirst().count();
        int arms = boss.attacks().stream()
                .filter(a -> a.pattern() == BossSpec.Pattern.SPIRAL)
                .findFirst()
                .orElseThrow()
                .arms();
        switch (difficulty) {
            case EASY -> {
                assertEquals(List.of(3, 1), pass);
                assertEquals(2, broadside);
                assertEquals(3, arms);
            }
            case MEDIUM -> {
                assertEquals(List.of(4, 2), pass);
                assertEquals(2, broadside);
                assertEquals(3, arms);
            }
            case HARD -> {
                assertEquals(List.of(5, 2), pass, "one more Skitter per opening");
                assertEquals(3, broadside);
                assertEquals(4, arms);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheMantisHoldThePodsAndThePatches(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        WaveSpec pincer = wave(level, 74);
        assertEquals(difficulty == Difficulty.EASY ? 4 : 6, pincer.holdSeconds().orElseThrow(), 1e-9);
        assertEquals(difficulty == Difficulty.HARD ? 3 : 2, wave(level, 80).count(), "hard: 3 Brood Pods at t=80");
        int patches = 0;
        for (WaveSpec wave : level.waves()) {
            patches += (int) wave.carried().stream()
                    .filter(carried -> carried.pickup() == PickupType.ARMOUR_PATCH)
                    .count();
        }
        assertEquals(difficulty == Difficulty.HARD ? 0 : 2, patches, "no armour patch before the boss on hard");
    }

    @Test
    void theLevelsRulesReachTheSimulation() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.TowSpec tow = level.tows().getFirst();
        assertEquals("lifeboat tow", tow.secret());
        assertEquals(75, tow.crateCredits());
        assertEquals(3, tow.hits());
        assertTrue(tow.vy() < 0, "it drifts down the screen");

        LevelScript.PartDrop drop = level.partDrops().getFirst();
        assertEquals("brood-carrier", drop.slug());
        assertEquals(SACS.size(), drop.parts().size());
        assertEquals(1, drop.dropsAt(), "the first sac destroyed");
        assertEquals(PickupType.OVERDRIVE, drop.pickup());

        LevelScript.Secondary secondary = level.secondary();
        assertTrue(secondary.byParts());
        assertEquals(SACS.size(), secondary.parts().size());
        assertEquals(1, secondary.beforePhase(), "before the broadside ends");
        assertEquals("BAYS", secondary.label());

        List<LevelScript.RadioCue> at107 = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME && cue.t() == 107)
                .toList();
        assertEquals(2, at107.size());
        int homing = LevelScript.RadioCue.FITTED_HOMING;
        assertEquals(1, at107.stream().filter(cue -> cue.allowedWith(homing)).count(), "one line with missiles");
        assertEquals(1, at107.stream().filter(cue -> cue.allowedWith(0)).count(), "one without");
        assertEquals(
                4,
                level.radio().stream()
                        .filter(cue -> cue.trigger() == LevelScript.CueTrigger.BOSS_DESTROYED)
                        .filter(cue -> cue.subject().equals("brood-carrier"))
                        .count(),
                "the four closing lines");
        assertEquals(
                1,
                level.radio().stream()
                        .filter(cue -> cue.trigger() == LevelScript.CueTrigger.BOSS_TIMEOUT)
                        .count(),
                "Okafor's timeout line");
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotBringsTheCarrierDown(Difficulty difficulty) {
        Run plan = fly(difficulty, planLoadout(content, difficulty), "the plan's fit");
        assertTrue(plan.launched[0] + plan.launched[1] > 0, "the overhead pass launches");
        assertEquals(0, plan.phase1Damage, 1e-9, "nothing but homing reaches the carrier on high air");
        assertEquals(1, plan.lines107.size());
        assertTrue(plan.lines107.getFirst().contains("can't touch it"), "the no-homing line");

        Run homing = fly(difficulty, loadout(content, difficulty, "micro-missile-pod"), "Micro-missile Pods");
        assertTrue(homing.phase1Damage > 0, "missiles reach the open sacs in phase 1");
        assertEquals(1, homing.lines107.size());
        assertTrue(homing.lines107.getFirst().contains("missiles"), "the homing line");
    }

    /** The overhead pass alone: a sac takes damage only while its window is open. */
    @Test
    void onlyOpenSacsTakeDamageOverhead() {
        Loadout missiles = loadout(content, Difficulty.MEDIUM, "micro-missile-pod");
        Sortie sortie = new Sortie(
                7,
                missiles,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                missiles.plating().maxArmour());
        double[] hp = null;
        int steps = 0;
        while (steps++ < MAX_STEPS) {
            sortie.step(Autopilot.commands(sortie));
            SetPiece boss = sortie.setPiece(0);
            if (!boss.present()) {
                continue;
            }
            if (boss.phase() > 0) {
                break;
            }
            if (hp == null) {
                hp = new double[boss.partCount()];
                for (int p = 0; p < hp.length; p++) {
                    hp[p] = boss.partHp(p);
                }
            }
            for (int p = 0; p < hp.length; p++) {
                if (boss.partHp(p) < hp[p]) {
                    assertTrue(
                            boss.partOpen(p) || boss.partWrecked(p),
                            boss.spec().parts().get(p).name() + " hit while shut");
                    hp[p] = boss.partHp(p);
                }
            }
        }
    }

    /** What a run of the autopilot saw. */
    private record Run(int[] launched, double phase1Damage, List<String> lines107) {}

    private Run fly(Difficulty difficulty, Loadout loadout, String fit) {
        Sortie sortie = sortie(content, 2185, difficulty, loadout);
        int steps = 0;
        int[] launched = new int[6];
        double[] phaseAt = {-1, -1, -1};
        double killed = -1;
        double phase1Share = 1;
        int secrets = 0;
        List<String> lines107 = new java.util.ArrayList<>();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            int attempt = sortie.attempt();
            Level05Test.step(sortie);
            SetPiece boss = sortie.setPiece(0);
            if (sortie.attempt() != attempt) {
                Arrays.fill(launched, 0);
                Arrays.fill(phaseAt, -1);
                lines107.clear();
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case BOSS_LAUNCHED -> {
                        String slug = sortie.enemyKinds().get(events.value(i)).slug();
                        launched[boss.phase() * 2 + (slug.equals("skitter") ? 0 : 1)]++;
                    }
                    case BOSS_DESTROYED -> killed = boss.killSeconds();
                    case SECRET_FOUND -> secrets++;
                    case RADIO -> {
                        LevelScript.RadioCue cue = sortie.script().radio().get(events.value(i));
                        if (cue.trigger() == LevelScript.CueTrigger.TIME && cue.t() == 107) {
                            lines107.add(cue.line());
                        }
                    }
                    default -> {}
                }
            }
            if (boss.present() && !boss.destroyed()) {
                if (phaseAt[boss.phase()] < 0) {
                    phaseAt[boss.phase()] = boss.bossSeconds();
                }
                if (boss.phase() == 0) {
                    phase1Share = boss.barShare();
                }
            }
        }
        LevelResult result = sortie.result();
        SetPiece boss = sortie.setPiece(0);
        System.out.printf(
                "Level 07 %s, %s: attempt %d, phases from %s s, broadside timeout %s, carrier killed in %.1f s,"
                        + " launched (Skitters, Needlers per phase) %s, phase-1 damage %.0f, secrets %d, kills %d/%d,"
                        + " credits %d (%s), gut the bays %s, armour lost %.0f, grade %s, bonuses %s%n",
                difficulty,
                fit,
                sortie.attempt(),
                Arrays.toString(Arrays.stream(phaseAt)
                        .map(t -> Math.round(t * 10) / 10.0)
                        .toArray()),
                boss.endedOnTimeout(1),
                killed,
                Arrays.toString(launched),
                (1 - phase1Share) * 3_840,
                secrets,
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                result.armourDamage(),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        assertTrue(killed > 0, "the carrier is destroyed");
        assertEquals(2, boss.phase(), "through its three phases");
        assertEquals(level(difficulty).seconds(), sortie.levelSeconds(), 0.05, "the aftermath runs after the kill");
        return new Run(launched, (1 - phase1Share) * 3_840, lines107);
    }

    private LevelScript level(Difficulty difficulty) {
        return SimSpecs.level(content, LEVEL, difficulty);
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(
                () -> sortie(content, 2185, Difficulty.HARD, planLoadout(content, Difficulty.HARD)), Level07Test::run);

        assertEquals(0, allocated, "the level allocated " + allocated + " bytes");
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameState() {
        Sortie first = sortie(content, 77, Difficulty.HARD, planLoadout(content, Difficulty.HARD));
        Sortie second = sortie(content, 77, Difficulty.HARD, planLoadout(content, Difficulty.HARD));
        run(first);
        run(second);

        assertEquals(first.stateHash(), second.stateHash());
    }

    private static void run(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level05Test.step(sortie);
        }
    }

    private static boolean inField(double x, double y, vanguard.sim.Hitbox box) {
        return x - box.width() / 2 >= 0
                && x + box.width() / 2 <= PlayField.WIDTH
                && y - box.height() / 2 >= 0
                && y + box.height() / 2 <= PlayField.HEIGHT;
    }

    private static WaveSpec wave(LevelScript level, double t) {
        return level.waves().stream().filter(w -> w.t() == t).findFirst().orElseThrow();
    }
}
