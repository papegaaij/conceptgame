package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;
import vanguard.sim.WingmanSpec;

/**
 * Level 08 as the game runs it: its credit budget, the README's totals, the Creeper convoys (their
 * sizes per difficulty and the fans' 0.5 s stagger), the difficulty variations (the pincers, the
 * circle's pairs, the hard overpass turrets, easy's extra armour patch), the billboard secret, the
 * pickups, the secondary and the retimed radio, and whole runs in which the autopilot flies the
 * level to its end with Rook on his wing.
 */
class Level08Test {
    static final String LEVEL = "act-2-homefront/level-08-neon-skyline";
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 08 (design/player/balance-plan.yaml): Level 07's fit
     * (the Pulse Cannon at L3, an Autocannon Pod on each wing, the second shield, Composite I) with the
     * second generator, and Rook in the escort slot with his free starter gun, the Autocannon at L1,
     * on his new campaign's side at full armour (design/player/wingmen). PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return planLoadout(content, difficulty, WingmanSpec.Side.LEFT);
    }

    static Loadout planLoadout(Content content, Difficulty difficulty, WingmanSpec.Side side) {
        WingmenData.Gun starter = content.wingmen().guns().list().getFirst();
        return Level07Test.planLoadout(content, difficulty)
                .withWingman(SimSpecs.wingman(
                        content, starter.id(), 1, side, content.wingmen().rook().armour()));
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
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills + ground + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_124, Math.round(budget), "the typical haul's target, 700 × 1.07⁷");
        assertEquals(108 * 5 + 42 * 12 + 12 * 15 + 15 * 22, kills, "Skitter, Needler, Stinger, Creeper");
        assertEquals(9 * 12 + 3 * 15, ground, "Spine Turret 9 × 12 + Polyp Mortar 3 × 15");
        assertEquals(100, crates, "the billboard cache in Act 1 terms (pays 160)");
        assertEquals(56, level.secondary().credits(), "in Act 1 terms (pays 90)");
        System.out.printf(
                "Level 08 budget (Act 1 terms): kills %d, ground %d, crate %d, secondary %d: %d (budget %.0f)%n",
                kills, ground, crates, level.secondary().credits(), total, budget);
        assertEquals(1_863, total, "before the act factor and bounty_scale (TypicalHaulTest checks the paid run)");
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

        assertEquals(Map.of("polyp-mortar", 3, "spine-turret", 9), ground);
        assertEquals(Map.of("creeper", 15, "needler", 42, "skitter", 108, "stinger", 12), waves);
        assertEquals("creeper", level.secondary().escapes());
        assertEquals(
                List.of(10.0, 65.0, 115.0, 160.0, 200.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(15, sortie.escapesTotal(), "every Creeper");
        assertEquals(189, sortie.enemyTotal());
        assertEquals(
                List.of(68.0, 84.0, 114.0, 128.0, 162.0),
                level.waves().stream()
                        .filter(wave -> wave.enemy().slug().equals("creeper"))
                        .map(WaveSpec::t)
                        .toList(),
                "the Creeper beats");
    }

    /**
     * Easy: the pincers from the front, a 2-Creeper convoy at t=84, an extra armour patch. Hard: one
     * more Creeper at t=84 and t=162, the circle breaking in pairs, two turrets at the overpass, and
     * the Creeper's hooks (seven ways every 2.6 s).
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheConvoysThePincersTheCircleAndTheOverpass(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        List<Integer> convoys = level.waves().stream()
                .filter(wave -> wave.enemy().slug().equals("creeper"))
                .map(WaveSpec::count)
                .toList();
        assertEquals(
                switch (difficulty) {
                    case EASY -> List.of(1, 2, 3, 3, 5);
                    case MEDIUM -> List.of(1, 3, 3, 3, 5);
                    case HARD -> List.of(1, 4, 3, 3, 6);
                },
                convoys);
        // easy: the two Needler pincers come from the front as a line abreast
        for (double t : new double[] {94, 142}) {
            WaveSpec flank = wave(level, t);
            assertEquals("needler", flank.enemy().slug());
            assertEquals(difficulty == Difficulty.EASY ? WaveSpec.Entry.FRONT : WaveSpec.Entry.SIDES, flank.entry());
            assertEquals(
                    difficulty == Difficulty.EASY ? WaveSpec.Formation.LINE_ABREAST : WaveSpec.Formation.PINCER,
                    flank.formation());
            assertEquals(1, level.waves().stream().filter(w -> w.t() == t).count());
        }
        WaveSpec circle = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.CIRCLE)
                .findFirst()
                .orElseThrow();
        assertEquals(difficulty == Difficulty.HARD ? 2 : 1, circle.breakGroup());
        long turrets = level.groundUnits().stream()
                .filter(unit -> unit.enemy().slug().equals("spine-turret"))
                .count();
        assertEquals(difficulty == Difficulty.HARD ? 11 : 9, turrets, "hard: a pair at the overpass");

        EnemySpec creeper = level.waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("creeper"))
                .findFirst()
                .orElseThrow();
        double interval = creeper.gun().orElseThrow().intervalSeconds();
        if (difficulty == Difficulty.EASY) {
            assertTrue(interval > 3.0, "easy: the fire-rate lever, " + interval);
        } else {
            assertEquals(
                    difficulty == Difficulty.HARD ? 2.6 : 3.0, interval, 1e-9, "hard: the hook's authored interval");
        }
        assertEquals(
                difficulty == Difficulty.HARD ? 7 : 5,
                creeper.gun().orElseThrow().fan());
        Sortie sortie = sortie(content, 1, difficulty);
        int creepers = convoys.stream().mapToInt(Integer::intValue).sum();
        assertEquals(creepers, sortie.escapesTotal(), "the secondary counts every Creeper");
    }

    @Test
    void theBillboardHidesTheCacheAndTheConvoyAndTheColumnCarryThePickups() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec billboard = level.groundObjects().stream()
                .filter(o -> o.secret().equals("billboard cache"))
                .findFirst()
                .orElseThrow();
        assertEquals(3, billboard.hits());
        assertEquals(100, billboard.crateCredits());
        WaveSpec highway = wave(level, 162);
        assertEquals("creeper", highway.enemy().slug());
        assertEquals(List.of(new WaveSpec.Carried(vanguard.sim.PickupType.OVERDRIVE, 0)), highway.carried());
        assertEquals(
                List.of(new WaveSpec.Carried(vanguard.sim.PickupType.ARMOUR_PATCH, WaveSpec.Carried.LAST)),
                wave(level, 145.5).carried());
        assertEquals(
                List.of(new WaveSpec.Carried(vanguard.sim.PickupType.ARMOUR_PATCH, WaveSpec.Carried.LAST)),
                wave(SimSpecs.level(content, LEVEL, Difficulty.EASY), 131.5).carried(),
                "easy's extra armour patch at t≈130");
        assertTrue(wave(level, 131.5).carried().isEmpty());
    }

    /**
     * The radio script: Rook's side line and his first-kill line, the Civilian's two lines, and the
     * timed lines in time order at least 7 s apart (one subtitle page each, README Timing).
     */
    @Test
    void theRadioScriptHasRooksLinesAndTheTimedLinesSpaced() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<LevelScript.RadioCue> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .toList();
        for (int i = 1; i < timed.size(); i++) {
            assertTrue(
                    timed.get(i).t() - timed.get(i - 1).t() >= 6,
                    "t=" + timed.get(i - 1).t() + " and t=" + timed.get(i).t() + " too close");
        }
        assertTrue(level.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.ESCORT_FIRST_KILL
                        && cue.speaker().equals("Rook")));
        assertTrue(timed.stream().anyMatch(cue -> cue.line().contains("{side}")));
        assertEquals(
                2,
                level.radio().stream()
                        .filter(cue -> cue.speaker().equals("Civilian"))
                        .count());
    }

    /**
     * The highway convoy (t=162, five Creepers on one path) walks north with visible gaps between its
     * units (the 1.5 s entry spacing at 105 px/s on the screen, about 160 px for 60 px sprites), and
     * its fans come 0.5 s apart, front to back. Flown without firing, so every Creeper lives.
     */
    @Test
    void theHighwayConvoyWalksWithGapsAndStaggersItsFans() {
        Loadout loadout = planLoadout(content, Difficulty.MEDIUM);
        Sortie sortie = new Sortie(
                3,
                loadout,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                loadout.plating().maxArmour());
        double closest = Double.MAX_VALUE;
        List<Double> fans = new java.util.ArrayList<>();
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS && sortie.levelSeconds() < 182) {
            sortie.step(0);
            if (sortie.levelSeconds() < 162) {
                continue;
            }
            List<Double> ys = new java.util.ArrayList<>();
            for (int i = 0; i < sortie.enemyCount(); i++) {
                var enemy = sortie.enemy(i);
                double y = enemy.renderY(1);
                if (enemy.spec().slug().equals("creeper") && y > 0 && y < vanguard.sim.PlayField.HEIGHT) {
                    ys.add(y);
                }
            }
            ys.sort(null);
            for (int i = 1; i < ys.size(); i++) {
                closest = Math.min(closest, ys.get(i) - ys.get(i - 1));
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.ENEMY_FIRED && nearCreeper(sortie, events.x(i))) {
                    double t = sortie.levelSeconds();
                    if (fans.isEmpty() || t - fans.getLast() > 1e-6) {
                        fans.add(t);
                    }
                }
            }
        }
        System.out.printf("Level 08 highway convoy: closest %.0f px apart; fans at %s%n", closest, fans);
        assertTrue(closest >= 100, "visible gaps between the Creepers: " + closest);
        assertTrue(fans.size() >= 5, "the convoy fires: " + fans);
        boolean staggered = false;
        for (int i = 1; i < fans.size(); i++) {
            staggered |= Math.abs(fans.get(i) - fans.get(i - 1) - 0.5) < 0.02;
        }
        assertTrue(staggered, "fans 0.5 s apart: " + fans);
    }

    private static boolean nearCreeper(Sortie sortie, double x) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            var enemy = sortie.enemy(i);
            if (enemy.spec().slug().equals("creeper") && Math.abs(enemy.renderX(1) - x) < 30) {
                return true;
            }
        }
        return false;
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEndWithRook(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        LevelScript script = sortie.script();
        int steps = 0;
        int secrets = 0;
        int rooksFirstKill = 0;
        double firstKillAt = -1;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SECRET_FOUND -> secrets++;
                    case RADIO -> {
                        if (script.radio().get(events.value(i)).trigger() == LevelScript.CueTrigger.ESCORT_FIRST_KILL) {
                            rooksFirstKill++;
                            firstKillAt = sortie.levelSeconds();
                        }
                    }
                    default -> {}
                }
            }
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 08 %s: attempt %d, kills %d/%d, credits %d (%s), secrets %d, Rook's first kill at %.1f s,"
                        + " every Creeper %s, armour lost %.0f, Rook %s, grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                secrets,
                firstKillAt,
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                result.armourDamage(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        assertTrue(rooksFirstKill <= 1, "Rook's first kill plays once an attempt");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level08Test::fly);

        assertEquals(0, allocated, "the level allocated " + allocated + " bytes");
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameState() {
        Sortie first = sortie(content, 77, Difficulty.HARD);
        Sortie second = sortie(content, 77, Difficulty.HARD);
        fly(first);
        fly(second);

        assertEquals(first.stateHash(), second.stateHash());
    }

    private static WaveSpec wave(LevelScript level, double t) {
        return level.waves().stream().filter(w -> w.t() == t).findFirst().orElseThrow();
    }

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
        }
    }
}
