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
 * Level 12 as the game runs it (design/campaign/act-2-homefront/level-12-storm-front; M5 part F, step
 * 4): its credit budget and the README's totals, the water flag, the weather per difficulty (gusts,
 * the wall's drift, lightning), the Lampreys' chase and latch, the clockwise circle in the eye and the
 * whirl clusters on the wind, the sondes and their {@code collect} secondary, the drifting canister (a
 * {@code low-air} trigger of 3 hits, hard 5), the drop factor and the placed armour patch, the radio
 * script with its events, and whole runs in which the autopilot flies the level to its end with Rook
 * and the balance plan's fit on every difficulty, shaking off the Lampreys, leaning against the gusts
 * and taking the sondes.
 */
class Level12Test {
    static final String LEVEL = "act-2-homefront/level-12-storm-front";
    /** An hour of steps: on hard a retry restarts the whole level (≈ 3.5 min). */
    private static final int MAX_STEPS = 60 * 60 * 60;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 12 (design/player/balance-plan.yaml, M5 part F's stated
     * default): Level 11's fit with both wing pods sold for two Micro-missile Pods, Rook's owned
     * Autocannon fitted again (no ground targets here), a third Airstrike charge. PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "micro-missile-pod", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "micro-missile-pod", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 1)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(1).name(),
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
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int secondary = level.secondary().credits();
        int total = kills + crates + secondary;

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_473, Math.round(budget), "the typical haul's target, 700 × 1.07¹¹");
        assertEquals(
                111 * 5 + 43 * 12 + 42 * 3 + 27 * 12 + 4 * 25 + 5 * 30 + 2 * 30,
                kills,
                "Skitter, Needler, Whirl Seed, Lamprey, Spore Bomber, Wraith, Mantis");
        assertTrue(level.groundUnits().isEmpty(), "no ground targets");
        assertEquals(130, crates, "the drifting canister in Act 1 terms (pays 208)");
        assertEquals(75, secondary, "the three sondes in Act 1 terms (pays 120)");
        System.out.printf(
                "Level 12 budget (Act 1 terms): kills %d, crate %d, secondary %d: %d (budget %.0f)%n",
                kills, crates, secondary, total, budget);
        assertEquals(2_036, total, "before the act factor and bounty_scale (TypicalHaulTest checks the paid run)");
        assertEquals(0.75, content.level(LEVEL).bounties());
    }

    @Test
    void theEnemiesAreTheReadmesTotals() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        Map<String, Integer> waves = new TreeMap<>();
        Map<WaveSpec.Entry, Integer> from = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            waves.merge(wave.enemy().slug(), wave.count(), Integer::sum);
            from.merge(wave.entry(), wave.count(), Integer::sum);
        }

        assertEquals(
                Map.of(
                        "lamprey", 27,
                        "mantis", 2,
                        "needler", 43,
                        "skitter", 111,
                        "spore-bomber", 4,
                        "whirl-seed", 42,
                        "wraith", 5),
                waves);
        assertEquals(
                Map.of(WaveSpec.Entry.FRONT, 164, WaveSpec.Entry.SIDES, 56, WaveSpec.Entry.REAR, 14),
                from,
                "the rear only the Wraiths and the Lampreys (skitters do not stream in from the rear)");
        assertEquals(
                List.of(25.0, 75.0, 125.0, 150.0, 175.0, 205.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertTrue(level.sections().stream().noneMatch(LevelScript.Section::arena), "no boss");
        assertTrue(level.setPieces().isEmpty());
        assertTrue(level.water(), "open sea throughout");
        assertEquals(
                4,
                SimSpecs.level(content, LEVEL, Difficulty.HARD).waves().stream()
                        .filter(wave -> wave.enemy().slug().equals("mantis"))
                        .mapToInt(WaveSpec::count)
                        .sum(),
                "hard's Mantis pincer in the eye");
    }

    /** The Lampreys' waves: the t=32 introduction, the chaser swarms and the rear stream; the t=136 swarm's easy 4. */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theLampreyWaves(Difficulty difficulty) {
        List<String> waves = SimSpecs.level(content, LEVEL, difficulty).waves().stream()
                .filter(wave -> wave.enemy().slug().equals("lamprey"))
                .map(wave -> String.format("%s %s %s %d", wave.t(), wave.formation(), wave.entry(), wave.count()))
                .toList();
        assertEquals(
                List.of(
                        "32.0 STREAM FRONT " + size(3, difficulty),
                        "58.0 SWARM FRONT " + size(4, difficulty),
                        "88.0 SWARM SIDES " + size(5, difficulty),
                        "112.0 STREAM REAR " + size(4, difficulty),
                        "136.0 SWARM FRONT " + (difficulty == Difficulty.EASY ? 4 : size(6, difficulty)),
                        "186.0 SWARM REAR " + size(5, difficulty)),
                waves,
                "the formation size lever, the t=136 swarm's authored easy 4");
        EnemySpec lamprey = SimSpecs.level(content, LEVEL, difficulty).waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("lamprey"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                difficulty == Difficulty.HARD ? 2 : 1,
                lamprey.latch().orElseThrow().slots());
        assertEquals(
                difficulty == Difficulty.HARD ? 12 : 8,
                lamprey.latch().orElseThrow().drainRate());
    }

    private int size(int count, Difficulty difficulty) {
        return content.difficulty().formationSize(count, difficulty);
    }

    /** The introduction alone: no other wave enters from t=32 until 6 s have passed. */
    @Test
    void theIntroductionHasTheScreenToItself() {
        List<Double> times = content.level(LEVEL).waves().stream()
                .map(LevelData.Wave::t)
                .filter(t -> t > 27.5 && t < 40)
                .toList();
        assertEquals(List.of(32.0, 38.0), times);
    }

    /**
     * The radio script: the timed lines retimed to the 1 s rule (the eye's at 149.5, 157 and 169.5, the
     * breakout's at 176 and 182.5), the first latch's line, the canister's, the secondary's and the level end's; Rook's lines
     * only while he flies.
     */
    @Test
    void theRadioScriptHasItsEvents() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<Double> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .map(LevelScript.RadioCue::t)
                .toList();
        assertEquals(
                List.of(2.0, 14.0, 30.0, 44.0, 58.0, 86.0, 128.0, 149.5, 157.0, 169.5, 176.0, 182.5, 200.0), timed);
        Map<LevelScript.CueTrigger, Integer> events = new TreeMap<>();
        for (LevelScript.RadioCue cue : level.radio()) {
            if (cue.trigger() != LevelScript.CueTrigger.TIME) {
                events.merge(cue.trigger(), 1, Integer::sum);
            }
        }
        assertEquals(
                Map.of(
                        LevelScript.CueTrigger.FIRST_LATCH, 1,
                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE, 1,
                        LevelScript.CueTrigger.SECRET, 1,
                        LevelScript.CueTrigger.LEVEL_END, 1),
                events);
        assertTrue(level.radio().stream()
                .filter(cue -> cue.speaker().equals("Rook"))
                .allMatch(cue -> (cue.requires() & LevelScript.RadioCue.FITTED_ESCORT) != 0));
    }

    @Test
    void theThreatProfileAndTheBriefing() {
        LevelData data = content.level(LEVEL);
        LevelData.ThreatProfile profile = data.threatProfile();
        assertEquals(List.of("air", "low-air"), profile.layers());
        assertEquals(List.of("homing", "spread"), profile.traits());
        assertEquals(4, profile.density());
        assertEquals(Optional.of("SONDES 3"), profile.objective());
        assertTrue(profile.vargaLine(2).contains("drains your shield"));
        assertEquals(
                List.of("Okafor", "Varga"),
                data.briefing().pages().stream().map(BriefingPage::speaker).toList());
        assertEquals("Rook", data.briefing().teaser().speaker());
        assertEquals(
                Optional.of(PickupType.ARMOUR_PATCH),
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).placedPickups().stream()
                        .map(LevelScript.PlacedPickup::pickup)
                        .findFirst());
    }

    /** What an autopilot run of the level saw (the attempt that completed it). */
    static final class Run {
        int latches;
        int shakes;
        /** Shield points lost while something was latched (the drain, and any hit then). */
        double drained;

        double armourLost;
        int sondes;
        int secrets;
        /** Where each lost attempt ended: its level time. */
        String deaths = "";
    }

    /** Flies the level with the autopilot, recording the completing attempt's latches, drain and sondes. */
    static Run watch(Sortie sortie) {
        Run run = new Run();
        int steps = 0;
        int attempt = sortie.attempt();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            double t = sortie.levelSeconds();
            double shield = sortie.ship().defences().shield();
            double armour = sortie.ship().defences().armour();
            boolean draining = sortie.draining();
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                run.deaths += String.format("%.0f ", t);
                Run fresh = new Run();
                fresh.deaths = run.deaths;
                run = fresh;
                continue;
            }
            if (draining || sortie.draining()) {
                run.drained += Math.max(0, shield - sortie.ship().defences().shield());
            }
            run.armourLost += Math.max(0, armour - sortie.ship().defences().armour());
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case LAMPREY_LATCHED -> run.latches++;
                    case LAMPREY_SHAKEN -> run.shakes++;
                    case SONDE_COLLECTED -> run.sondes++;
                    case SECRET_FOUND -> run.secrets++;
                    default -> {}
                }
            }
        }
        return run;
    }

    /**
     * The autopilot flies the level to its end with Rook and the plan's fit on every difficulty, shaking
     * off every latch. Printed: the attempts and where they were lost, the latches and shake-offs, the
     * shield drained, the armour lost, the sondes and the secondary, the grade and the credits.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEndWithRook(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        Run run = watch(sortie);
        LevelResult result = sortie.result();
        System.out.printf(
                "Level 12 %s: attempt %d%s, real %.1f s, kills %d/%d, latches %d, shaken %d, shield drained %.1f,"
                        + " armour lost %.0f, sondes %d/3, secondary %s, canister %s, credits %d (%s), Airstrike charges"
                        + " used %d, Rook %s, grade %s%n",
                difficulty,
                sortie.attempt(),
                run.deaths.isEmpty() ? "" : " (lost at t=" + run.deaths.strip() + ")",
                sortie.realSeconds(),
                result.kills(),
                result.enemies(),
                run.latches,
                run.shakes,
                run.drained,
                run.armourLost,
                run.sondes,
                result.secondaryMet() ? "met" : "failed",
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
        assertEquals(run.sondes == 3, result.secondaryMet(), "the secondary is the three sondes");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level12Test::fly);

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
