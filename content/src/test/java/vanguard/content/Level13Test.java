package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.Armament;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;
import vanguard.sim.WingmanSpec;

/**
 * Level 13 as the game runs it (design/campaign/act-2-homefront/level-13-polar-relay; M5 part G, step
 * G4; user decisions G1–G9 = a, pending the user's review): its credit budget and the README's totals,
 * the timed halt (the scroll eased to 0 over t=38–40 and back up from t=220, the clock running on), the
 * defend primary with the Nansen Relay (its integrity and hook modes per difficulty), the Skimmers' lanes
 * and the crosses from all edges, the mortar floe from the left edge, the circle above the relay, hard's
 * Stinger pairs, the integrity secondary, the supply drones' patches, the hut secret, the radio script
 * with its {@code ally-below} events, and whole runs in which the autopilot guards the relay with Rook
 * and the balance plan's fit on every difficulty.
 */
class Level13Test {
    static final String LEVEL = "act-2-homefront/level-13-polar-relay";
    /** An hour of steps: on hard a retry restarts the whole level (≈ 4 min). */
    private static final int MAX_STEPS = 60 * 60 * 60;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 13 (design/player/balance-plan.yaml, unchanged for M5 part
     * G): Level 12's fit with Armour Mk III and both Micro-missile Pods at level 2, Rook's Autocannon, the
     * three Airstrike charges. PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "micro-missile-pod", 2),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "micro-missile-pod", 2),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 1)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(2).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        content,
                        "autocannon",
                        1,
                        WingmanSpec.Side.LEFT,
                        content.wingmen().rook().armour()))
                .withSpecial(SimSpecs.special(content, "Airstrike", 3));
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
        int secondary = level.secondary().credits();
        int total = kills + ground + crates + secondary;

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_577, Math.round(budget), "the typical haul's target, 700 × 1.07¹²");
        assertEquals(
                111 * 5 + 41 * 12 + 8 * 15 + 4 * 30 + 31 * 14 + 11 * 25,
                kills,
                "Skitter, Needler, Stinger, Mantis, Skimmer, Scuttler");
        assertEquals(2 * 15, ground, "the mortar floe's two Polyp Mortars");
        assertEquals(110, crates, "the research hut's cache in Act 1 terms (pays 176)");
        assertEquals(95, secondary, "the relay at 50 % or more in Act 1 terms (pays 152)");
        System.out.printf(
                "Level 13 budget (Act 1 terms): kills %d, ground %d, crate %d, secondary %d: %d (budget %.0f)%n",
                kills, ground, crates, secondary, total, budget);
        assertEquals(0.67, content.level(LEVEL).bounties());
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
                        "mantis", 4,
                        "needler", 41,
                        "scuttler", 11,
                        "skimmer", 31,
                        "skitter", 111,
                        "stinger", 8),
                waves);
        assertEquals(2, level.groundUnits().size(), "the mortar floe");
        assertEquals(
                208,
                waves.values().stream().mapToInt(Integer::intValue).sum()
                        + level.groundUnits().size());
        assertEquals(
                List.of(40.0, 100.0, 160.0, 220.0, 235.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertTrue(level.sections().stream().noneMatch(LevelScript.Section::arena), "no boss");
        assertTrue(level.water(), "water: true over the whole field (G5 = a)");
        assertTrue(content.level(LEVEL).navalSplashOnly(), "the water splash only on a naval unit");
        Map<Difficulty, Integer> enemies = new TreeMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            LevelScript at = SimSpecs.level(content, LEVEL, difficulty);
            enemies.put(
                    difficulty,
                    at.waves().stream().mapToInt(WaveSpec::count).sum()
                            + at.groundUnits().size());
        }
        System.out.println("Level 13 enemies per difficulty: " + enemies);
    }

    /**
     * The timed halt (G3's simulation, the data's sections): speed 0 over sections 2–4 without an arena,
     * the 2 s eases; the approach scrolls ≈ 5,460 px to the halt and the exit ≈ 1,960 px; the relay stops at
     * (240, 290 y-up) and enters at the top edge at t≈37.
     */
    @Test
    void theScrollHaltsOverTheRelay() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        assertTrue(level.timedHalts());
        assertFalse(level.timedHaltAt(39.9));
        assertTrue(level.timedHaltAt(40));
        assertTrue(level.timedHaltAt(219.9));
        assertFalse(level.timedHaltAt(220));
        assertEquals(2, level.sections().get(1).rampSeconds());
        double approach = Sortie.scrollAt(level, 40);
        double exit = Sortie.scrollAt(level, 235) - Sortie.scrollAt(level, 220);
        assertEquals(5_460, approach, 3);
        assertEquals(1_960, exit, 3);
        assertEquals(Sortie.scrollAt(level, 40), Sortie.scrollAt(level, 220), 1e-9, "the ground stands in the halt");
        LevelData data = content.level(LEVEL);
        assertEquals(approach, data.scrollAt(40), 2, "the data's scroll eases as the simulation's");
        assertEquals(exit, data.scrollAt(235) - data.scrollAt(220), 2);
        // The relay stands 250 px below the top edge from t=40: it entered at the top edge (its centre
        // 48 px above it) when the ground still had 250 + 48 px to scroll.
        double entry = 0;
        while (approach - Sortie.scrollAt(level, entry) > 250 + 48) {
            entry += 0.05;
        }
        System.out.printf("Level 13: the relay enters at the top edge at t=%.2f%n", entry);
        assertEquals(37, entry, 1);
    }

    /** The defend primary per difficulty: the relay at (240, 290 y-up) from t=40 for 178 s, its integrity and hook modes. */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theRelayIsDefended(Difficulty difficulty) {
        LevelScript.Defend defend =
                SimSpecs.level(content, LEVEL, difficulty).defend().orElseThrow();
        assertEquals("nansen-relay", defend.ally().slug());
        assertEquals(240, defend.x());
        assertEquals(290, defend.y());
        assertEquals(40, defend.start());
        assertEquals(218, defend.end());
        assertEquals(10, defend.ally().dives());
        assertTrue(defend.ally().lobs());
        assertTrue(defend.ally().objectiveAimed());
        assertEquals(80, defend.ally().hitbox().width());
        assertEquals(
                switch (difficulty) {
                    case EASY -> 340.0;
                    case MEDIUM -> 240.0;
                    case HARD -> 520.0;
                },
                defend.ally().hp());
        assertEquals(LevelScript.HookMode.ALTERNATE, defend.mode("skimmer"));
        assertEquals(LevelScript.HookMode.IN_ARC, defend.mode("scuttler"));
        assertEquals(LevelScript.HookMode.ALWAYS, defend.mode("polyp-mortar"));
        assertEquals(
                difficulty == Difficulty.EASY ? LevelScript.HookMode.NONE : LevelScript.HookMode.ALWAYS,
                defend.mode("stinger"),
                "easy: the Stingers dive at the player");
        assertEquals(LevelScript.HookMode.NONE, defend.mode("skitter"), "the popcorn is never hooked");
        assertEquals(LevelScript.HookMode.NONE, defend.mode("needler"));
        assertEquals(LevelScript.HookMode.NONE, defend.mode("mantis"));
        LevelScript.Secondary secondary =
                SimSpecs.level(content, LEVEL, difficulty).secondary();
        assertTrue(secondary.byIntegrity());
        assertEquals(0.5, secondary.integrity());
    }

    /**
     * The waves of part G: the Skimmers on lanes (a cross's lane per unit on every difficulty), the
     * crosses from all edges, the mortar floe from the left edge, the circle above the relay, hard's pairs.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void thePartGWaves(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        for (WaveSpec wave : level.waves()) {
            if (wave.enemy().slug().equals("skimmer")) {
                assertFalse(wave.lanes().isEmpty(), "a Skimmer wave walks lanes: t=" + wave.t());
                if (wave.formation() == WaveSpec.Formation.CROSS) {
                    assertTrue(wave.lanes().size() >= wave.count(), "a cross's lane per unit: t=" + wave.t());
                }
            }
        }
        List<String> crosses = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.CROSS)
                .map(wave -> String.format("%s %d", wave.t(), wave.count()))
                .toList();
        assertEquals(
                List.of(
                        "56.0 " + size(4, difficulty),
                        "118.0 " + (difficulty == Difficulty.HARD ? 8 : size(4, difficulty)),
                        "176.0 " + size(8, difficulty),
                        "204.0 " + size(4, difficulty)),
                crosses);
        WaveSpec circle = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.CIRCLE)
                .findFirst()
                .orElseThrow();
        assertEquals(Optional.of(new WaveSpec.At(240, 150)), circle.at(), "the circle above the relay");
        WaveSpec column = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.COLUMN)
                .findFirst()
                .orElseThrow();
        assertEquals(
                difficulty == Difficulty.HARD ? Optional.of(0.4) : Optional.empty(),
                column.pairSeconds(),
                "hard's Stingers in pairs");
        List<LevelScript.GroundUnit> floe = level.groundUnits();
        assertEquals(2, floe.size());
        for (LevelScript.GroundUnit unit : floe) {
            assertEquals("polyp-mortar", unit.enemy().slug());
            assertEquals(66, unit.t());
            LevelScript.SideEntry side = unit.side().orElseThrow();
            assertTrue(side.fromLeft());
            assertEquals(10, side.drift());
        }
        EnemySpec skimmer = level.waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("skimmer"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                difficulty == Difficulty.HARD ? 2 : 1,
                skimmer.gun().orElseThrow().burst(),
                "hard: 2-shot bursts");
        assertEquals(40, skimmer.sine().orElseThrow().amplitude());
    }

    private int size(int count, Difficulty difficulty) {
        return content.difficulty().formationSize(count, difficulty);
    }

    /**
     * The introduction alone: no other wave enters from t=12 until 4 s after the Skimmers (the popcorn
     * stream at t=24 fills the stretch after them, for the pacing rule).
     */
    @Test
    void theIntroductionHasTheScreenToItself() {
        List<Double> times = content.level(LEVEL).waves().stream()
                .map(LevelData.Wave::t)
                .filter(t -> t > 12 && t < 24)
                .toList();
        assertEquals(List.of(20.0), times);
    }

    /**
     * The radio script: the timed lines retimed to the 1 s rule (as text until the voices, step G6), the
     * relay's two {@code ally-below} lines (50 % Nansen, 25 % Okafor), the secondary's, the secret's, Rook's
     * level-end quip and the mission-failed line; Rook's lines only while he flies.
     */
    @Test
    void theRadioScriptHasItsEvents() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<Double> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .map(LevelScript.RadioCue::t)
                .toList();
        assertEquals(
                List.of(1.0, 13.0, 24.0, 36.0, 47.5, 58.5, 102.0, 154.0, 162.0, 172.0, 179.0, 218.0, 233.5), timed);
        Map<LevelScript.CueTrigger, Integer> events = new TreeMap<>();
        for (LevelScript.RadioCue cue : level.radio()) {
            if (cue.trigger() != LevelScript.CueTrigger.TIME) {
                events.merge(cue.trigger(), 1, Integer::sum);
            }
        }
        assertEquals(
                Map.of(
                        LevelScript.CueTrigger.ALLY_BELOW, 2,
                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE, 1,
                        LevelScript.CueTrigger.SECRET, 1,
                        LevelScript.CueTrigger.LEVEL_END, 1,
                        LevelScript.CueTrigger.MISSION_FAILED, 1),
                events);
        assertEquals(
                List.of("0.5", "0.25"),
                level.radio().stream()
                        .filter(cue -> cue.trigger() == LevelScript.CueTrigger.ALLY_BELOW)
                        .map(LevelScript.RadioCue::subject)
                        .toList());
        assertTrue(level.radio().stream()
                .filter(cue -> cue.speaker().equals("Rook"))
                .allMatch(cue -> (cue.requires() & LevelScript.RadioCue.FITTED_ESCORT) != 0));
        assertTrue(content.voices().uncast("Nansen"), "Nansen is auditioned in round 35");
    }

    @Test
    void theThreatProfileTheBriefingAndTheDrops() {
        LevelData data = content.level(LEVEL);
        LevelData.ThreatProfile profile = data.threatProfile();
        assertEquals(List.of("ground", "air"), profile.layers());
        assertEquals(List.of("spread", "rear"), profile.traits());
        assertEquals(4, profile.density());
        assertEquals(Optional.of("DEFEND NANSEN RELAY"), profile.objective());
        assertTrue(profile.vargaLine(2).contains("Skimmer"));
        assertEquals(
                List.of("Okafor", "Varga"),
                data.briefing().pages().stream().map(BriefingPage::speaker).toList());
        assertEquals("Rook", data.briefing().teaser().speaker());
        List<LevelScript.PlacedPickup> placed =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).placedPickups();
        assertEquals(2, placed.size(), "the supply drones' patches");
        for (LevelScript.PlacedPickup patch : placed) {
            assertEquals(PickupType.ARMOUR_PATCH, patch.pickup());
            assertTrue(patch.atPoint(), "released at a point over the relay");
            assertEquals(240, patch.x());
            assertEquals(380, patch.y());
        }
        assertEquals(
                List.of(162.0, 202.0),
                placed.stream().map(LevelScript.PlacedPickup::t).toList());
        assertTrue(data.music().jingle().isPresent(), "the jingle at t=218 (G8 = a)");
        assertFalse(data.music().debriefJingle());
    }

    /** What an autopilot run of the level saw (the attempt that completed it, or the last one). */
    static final class Run {
        /** The relay's share of its integrity at the end of the synchronisation (0 when lost). */
        double integrity = 1;

        double armourLost;
        int secrets;
        int patches;
        int relayHits;
        /** Where each lost attempt ended, its level time, and why ("ship" or "relay"). */
        String deaths = "";
        /** Every attempt's relay share at its end. */
        String relays = "";
    }

    /** Flies the level with the autopilot, recording the completing attempt's relay integrity, armour lost and secrets. */
    static Run watch(Sortie sortie) {
        Run run = new Run();
        int steps = 0;
        int attempt = sortie.attempt();
        double share = 1;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            double t = sortie.levelSeconds();
            double armour = sortie.ship().defences().armour();
            boolean relayLost = sortie.primaryFailed();
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                run.deaths += String.format("%.0f(%s) ", t, relayLost || share <= 0 ? "relay" : "ship");
                run.relays += String.format("%.2f ", share);
                Run fresh = new Run();
                fresh.deaths = run.deaths;
                fresh.relays = run.relays;
                run = fresh;
                share = 1;
                continue;
            }
            if (sortie.allyCount() > 0) {
                share = sortie.ally(0).hpShare();
            }
            if (!sortie.defendStarted() || sortie.levelSeconds() <= 218) {
                run.integrity = share;
            }
            run.armourLost += Math.max(0, armour - sortie.ship().defences().armour());
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SECRET_FOUND -> run.secrets++;
                    case ALLY_HIT -> run.relayHits++;
                    default -> {}
                }
            }
        }
        run.relays += String.format("%.2f", share);
        return run;
    }

    /**
     * The autopilot guards the relay to the level's end with Rook and the plan's fit on every difficulty.
     * Printed: the attempts and where (and why) they were lost, the relay's integrity at the synchronisation's
     * end, the secondary, the armour lost, the hut, the grade and the credits.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotGuardsTheRelayToTheEnd(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        Run run = watch(sortie);
        LevelResult result = sortie.result();
        System.out.printf(
                "Level 13 %s: attempt %d%s, real %.1f s, kills %d/%d, relay %.0f %% (attempts: %s), secondary %s,"
                        + " armour lost %.0f, hut %s, credits %d (%s), Airstrike charges used %d, Rook %s, grade %s%n",
                difficulty,
                sortie.attempt(),
                run.deaths.isEmpty() ? "" : " (lost at t=" + run.deaths.strip() + ")",
                sortie.realSeconds(),
                result.kills(),
                result.enemies(),
                100 * run.integrity,
                run.relays,
                result.secondaryMet() ? "met" : "failed",
                run.armourLost,
                run.secrets > 0 ? "found" : "missed",
                result.credits().total(),
                result.credits(),
                sortie.special().used(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter());
        assertTrue(sortie.complete());
        assertFalse(sortie.primaryFailed());
        assertEquals(run.integrity >= 0.5, result.secondaryMet(), "the secondary is the relay at half or more");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level13Test::fly);

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

    private static void fly(Sortie sortie) {
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
        }
    }
}
