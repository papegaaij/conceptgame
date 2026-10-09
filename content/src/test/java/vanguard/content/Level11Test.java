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
import vanguard.sim.BossSpec;
import vanguard.sim.Command;
import vanguard.sim.GroundObject;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;
import vanguard.sim.WingmanSpec;

/**
 * Level 11 as the game runs it (design/campaign/act-2-homefront/level-11-atlantic-convoy; M5 part E,
 * step E3a): its credit budget and the README's totals, the water flag, the arena section where the
 * scroll halts for the Harbour Kraken, the naval convoy at its stations and lanes, the afloat
 * secondary, the sunken pod (a {@code sub} trigger of 3 torpedo hits, hard 5), the floating
 * containers whose last one drops the armour patch, the radio script with the arena's events, and
 * whole runs in which the autopilot flies the level to its end with Rook and the balance plan's fit
 * (no Torpedo Pod) on every difficulty.
 */
class Level11Test {
    static final String LEVEL = "act-2-homefront/level-11-atlantic-convoy";
    /**
     * An hour of steps: on hard a retry restarts the whole level (≈ 4 min with the fight), and a
     * pilot worn down in the Kraken's arena (hard is harsh) may take several.
     */
    private static final int MAX_STEPS = 60 * 60 * 60;
    /** Where a pilot waits under a target: px above the bottom edge (the ship's start line). */
    private static final double START_LINE = 80;
    /** How far right of a left-wing mount's shots the ship's centre is, px. */
    private static final double LEFT_WING = 16;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 11 (design/player/balance-plan.yaml, the stated
     * default of M5 part E): Level 10's fit with the front at L4 and Rook's owned Mortar fitted again
     * (for the rafts and the surfaced Kraken); the two Airstrike charges; no Torpedo Pod. PacingTest
     * flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "bomb-rack", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "autocannon-pod", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 1)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(1).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        content,
                        "mortar",
                        1,
                        WingmanSpec.Side.LEFT,
                        content.wingmen().rook().armour()))
                .withSpecial(SimSpecs.special(content, "Airstrike", 2));
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
        int rafts = level.groundUnits().stream()
                .mapToInt(unit -> unit.enemy().bounty())
                .sum();
        int salvage = (int) level.groundObjects().stream()
                        .filter(o -> o.drop().equals(Optional.of(PickupType.SMALL_SALVAGE)))
                        .count()
                * content.player().pickups().salvage().credits().small();
        int kraken = level.setPieces().getFirst().parts().stream()
                .mapToInt(LevelScript.PartSpec::bounty)
                .sum();
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int secondary = level.secondary().credits();
        int total = kills + rafts + salvage + kraken + crates + secondary;

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_377, Math.round(budget), "the typical haul's target, 700 × 1.07¹⁰");
        assertEquals(
                110 * 5 + 96 * 2 + 28 * 10 + 19 * 12 + 4 * 15, kills, "Skitter, Mote, Driftjelly, Needler, Stinger");
        assertEquals(12 * 14, rafts, "twelve Reef Spitters at medium");
        assertEquals(8 * 10, salvage, "eight floating containers' small salvage");
        assertEquals(216, kraken, "the Harbour Kraken: head 144 and two arms of 36");
        assertEquals(100, crates, "the sunken pod in Act 1 terms (pays 160)");
        assertEquals(100, secondary, "the convoy afloat in Act 1 terms (pays 160)");
        System.out.printf(
                "Level 11 budget (Act 1 terms): kills %d, rafts %d, salvage %d, Kraken %d, crate %d, secondary %d:"
                        + " %d (budget %.0f)%n",
                kills, rafts, salvage, kraken, crates, secondary, total, budget);
        assertEquals(1_974, total, "before the act factor and bounty_scale (TypicalHaulTest checks the paid run)");
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

        assertEquals(Map.of("driftjelly", 28, "mote-swarm", 96, "needler", 19, "skitter", 110, "stinger", 4), waves);
        assertEquals(Map.of(WaveSpec.Entry.FRONT, 253, WaveSpec.Entry.SIDES, 4), from, "no rear waves");
        assertEquals(
                List.of(20.0, 65.0, 110.0, 160.0, 161.0, 176.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertTrue(level.sections().get(4).arena(), "section 5 is the arena");
        assertEquals(0, level.sections().get(4).speed(), "where the scroll halts");
        assertEquals(12, level.groundUnits().size(), "three raft nests at medium");
        assertEquals(
                15,
                SimSpecs.level(content, LEVEL, Difficulty.HARD).groundUnits().size(),
                "hard's extra nest");
        assertTrue(level.water(), "E1 = a: the whole play field is water");
        assertTrue(level.groups().isEmpty(), "no objectives' groups");
    }

    /** The jelly fields: easy's smaller ones at t=50 and t=100, the formation lever elsewhere. */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theJellyFieldsPerDifficulty(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        Map<Double, Integer> fields = new TreeMap<>();
        for (WaveSpec wave : level.waves()) {
            if (wave.enemy().slug().equals("driftjelly")) {
                fields.put(wave.t(), wave.count());
            }
        }
        int six = content.difficulty().formationSize(6, difficulty);
        int eight = difficulty == Difficulty.EASY ? 6 : content.difficulty().formationSize(8, difficulty);
        assertEquals(Map.of(22.0, six, 50.0, eight, 100.0, eight, 140.0, six), fields);
    }

    @Test
    void theKrakenIsTheAnchoredMidBossOfTheArena() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        assertEquals(1, level.setPieces().size());
        LevelScript.SetPieceSpec kraken = level.setPieces().getFirst();
        BossSpec boss = kraken.boss().orElseThrow();
        assertTrue(boss.anchored());
        assertTrue(boss.midBoss());
        assertEquals(160, content.level(LEVEL).boss().orElseThrow().t(), 1e-9, "it arrives at the arena's start");
        assertEquals(4, boss.arena().orElseThrow().lanes().count());

        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        SetPiece piece = sortie.setPiece(0);
        while (!piece.engaged()) {
            sortie.step(0);
        }
        LevelData data = content.level(LEVEL);
        assertEquals(data.scrollAt(160) + Sortie.easeDistance(140), sortie.groundScroll(), 1e-6, "the halt scroll");
        assertEquals(
                PlayField.HEIGHT - 78, piece.renderY(1), 1e-6, "Tiamat's centre 78 px below the top edge (round 33)");
        BackdropData.PlacedPiece platform = data.backdrop().placed().stream()
                .filter(placed -> placed.piece().equals("platform-tiamat"))
                .findFirst()
                .orElseThrow();
        double screenY = data.pieceCentre(platform) - sortie.groundScroll();
        assertEquals(PlayField.HEIGHT - 78, screenY, 0.1, "the backdrop's Platform Tiamat lies under the Kraken");
        assertEquals(240, platform.x(), 1e-9);
    }

    /** The naval convoy (README, The convoy): four hulls at their stations, three to lanes 1, 2, 4. */
    @Test
    void theConvoyHoldsItsStationsAndTakesItsLanes() {
        LevelScript.Naval naval =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).convoy().orElseThrow();
        assertEquals(
                List.of("Halvorsen", "Mbeki", "Saint-Laurent", "Ruyter"),
                naval.units().stream().map(LevelScript.NavalUnit::name).toList());
        assertEquals(
                List.of(1, 2, 4, 0),
                naval.units().stream().map(LevelScript.NavalUnit::lane).toList(),
                "the frigate leaves at the halt");
        assertEquals(
                List.of(130.0, 240.0, 350.0, 240.0),
                naval.units().stream().map(LevelScript.NavalUnit::x).toList());
        assertEquals(
                List.of(
                        PlayField.HEIGHT - 380.0,
                        PlayField.HEIGHT - 350.0,
                        PlayField.HEIGHT - 380.0,
                        PlayField.HEIGHT - 480.0),
                naval.units().stream().map(LevelScript.NavalUnit::y).toList());
        assertEquals(3, naval.glideSeconds());
        assertEquals(PlayField.HEIGHT - 450.0, naval.laneY());
        assertEquals(3, naval.damageable(), "the three cargo ships, not the frigate");
        assertEquals(
                4,
                naval.units().getFirst().ally().hp(),
                1e-9,
                "a cargo ship sinks on its fourth slam (2026-10-09, the convoy bonus tuned)");
        assertEquals(
                2,
                SimSpecs.level(content, LEVEL, Difficulty.HARD)
                        .convoy()
                        .orElseThrow()
                        .units()
                        .getFirst()
                        .ally()
                        .hp(),
                1e-9,
                "hard: on its second");
        assertEquals(552, naval.holdClear(), "after the Kraken they hold clear while Tiamat's deck passes");
        assertTrue(naval.units().get(1).holds(), "Mbeki drops back off the bottom edge from lane 2");
        assertEquals(
                5,
                SimSpecs.level(content, LEVEL, Difficulty.EASY)
                        .convoy()
                        .orElseThrow()
                        .units()
                        .getFirst()
                        .ally()
                        .hp(),
                1e-9,
                "easy: on its fifth");

        LevelScript.Secondary secondary =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).secondary();
        assertTrue(secondary.afloat(), "E8 = a: all three afloat at the Kraken's death");
        assertEquals(100, secondary.credits());
        assertEquals("CONVOY", secondary.label());
    }

    /** The sunken pod: a `sub` trigger of 3 hits (hard 5; round 33, user) that reveals the crate of 100. */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theSunkenPodCountsHits(Difficulty difficulty) {
        LevelScript.GroundObjectSpec pod = SimSpecs.level(content, LEVEL, difficulty).groundObjects().stream()
                .filter(o -> o.secret().equals("sunken pod"))
                .findFirst()
                .orElseThrow();
        assertTrue(pod.submerged());
        assertEquals(difficulty == Difficulty.HARD ? 5 : 3, pod.hits());
        assertEquals(100, pod.crateCredits());
        assertEquals("sunken-pod", pod.look());
    }

    /**
     * The plan's fit (no torpedo) cannot free the pod. A pilot who waits on the start line with the
     * left wing's torpedo lined up on the pod lands one hit per torpedo in the ≈ 4 s it is in reach:
     * since round 33 (user, 2026-10-09: 4 hits → 3, hard 6 → 5) one Torpedo Pod at L1 (0.8 a second)
     * frees it on medium whether it fires from a second before the pod enters (a torpedo's run then
     * reaches it at the top edge) or from 4 s early, out of step (the first torpedo runs out of range
     * above the top edge, the next three land), as a pod at L3 (1 a second) does. The torpedoes fired
     * once it is on the screen seek the sunken trigger (2026-10-09): 24 px off the line the three of
     * them hit it and free it, where straight runs pass beside it. On hard (5 hits) it takes a pod on
     * each wing.
     */
    @Test
    void onlyATorpedoFreesTheSunkenPod() {
        assertEquals(
                -1,
                podRun(planLoadout(content, Difficulty.MEDIUM), Difficulty.MEDIUM, 0, 1),
                "nothing but anti-sub reaches it");
        double l1 = podRun(torpedoes(1, false, Difficulty.MEDIUM), Difficulty.MEDIUM, 0, 1);
        PodPass offLine = podPass(torpedoes(1, false, Difficulty.MEDIUM), Difficulty.MEDIUM, OFF_LINE, 1);
        int off = offLine.hits();
        double late = podRun(torpedoes(1, false, Difficulty.MEDIUM), Difficulty.MEDIUM, 0, 4);
        double l3 = podRun(torpedoes(3, false, Difficulty.MEDIUM), Difficulty.MEDIUM, 0, 4);
        double both = podRun(torpedoes(1, true, Difficulty.MEDIUM), Difficulty.MEDIUM, 0, 1);
        double hard = podRun(torpedoes(1, true, Difficulty.HARD), Difficulty.HARD, 0, 1);
        double hardOne = podRun(torpedoes(1, false, Difficulty.HARD), Difficulty.HARD, 0, 1);
        System.out.printf(
                "Level 11 sunken pod freed at t=%.2f with one L1 pod lined up (%d hits 24 px off the line, freed"
                        + " %.2f), %.2f firing out of step, %.2f with one L3 pod out of step, %.2f with two L1 pods,"
                        + " hard (5 hits) %.2f with two L1 pods, %.2f with one%n",
                l1, off, offLine.found(), late, l3, both, hard, hardOne);
        assertTrue(l1 > 0, "one L1 pod lined up frees it on medium");
        assertEquals(
                3,
                off,
                "24 px off the line every torpedo fired while it is on the screen seeks it and hits"
                        + " (a straight run passes beside it, as the one fired before it enters does)");
        assertTrue(offLine.found() > 0, "the three seeking torpedoes free it");
        assertTrue(late > 0, "out of step one L1 pod still lands the 3");
        assertTrue(l3 > 0, "one L3 pod frees it even out of step");
        assertTrue(both > 0, "two L1 pods free it");
        assertTrue(hard > 0, "two L1 pods free it on hard");
        assertEquals(-1, hardOne, "on hard one L1 pod is not enough");
    }

    /** How far beside the pod's line the off-line pilot waits, px. */
    private static final double OFF_LINE = 24;

    /** The plan's front gun with a Torpedo Pod at {@code level} on the left wing, and on the right with {@code both}. */
    private Loadout torpedoes(int level, boolean both, Difficulty difficulty) {
        List<SimSpecs.FittedWeapon> weapons = new java.util.ArrayList<>(List.of(
                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "torpedo-pod", level)));
        if (both) {
            weapons.add(new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "torpedo-pod", level));
        }
        return SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                weapons,
                content.shields().models().get(1).name(),
                content.armour().plating().get(1).name(),
                0,
                difficulty);
    }

    /**
     * The level time the pod's secret was found with the ship waiting under it ({@code off} px beside
     * the line) and firing from {@code early} s before it enters; -1 if never.
     */
    private double podRun(Loadout loadout, Difficulty difficulty, double off, double early) {
        return podPass(loadout, difficulty, off, early).found();
    }

    /** A pass under the pod: when its secret was found (-1 if never) and the hits it took. */
    private record PodPass(double found, int hits) {}

    /** The pass of {@link #podRun}, counting the pod's hits (the ground hits within 24 px of it). */
    private PodPass podPass(Loadout loadout, Difficulty difficulty, double off, double early) {
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, LEVEL, difficulty),
                SimSpecs.rules(content, LEVEL, difficulty).withInvulnerableShip(),
                loadout.plating().maxArmour());
        LevelScript.GroundObjectSpec pod = sortie.script().groundObjects().stream()
                .filter(o -> o.secret().equals("sunken pod"))
                .findFirst()
                .orElseThrow();
        int hits = 0;
        for (int step = 0; step < SimStep.ticks(pod.t() + 6); step++) {
            int commands = Command.NONE;
            // one torpedo pod: the left wing's runs 16 px left of the ship's centre; two straddle the pod
            double offset = (loadout.armament().mounts().size() > 2 ? 0 : LEFT_WING) + off;
            if (sortie.levelSeconds() > pod.t() - 5) {
                commands |= steer(sortie, pod.x() + offset);
            }
            if (sortie.levelSeconds() > pod.t() - early) {
                commands |= Command.FIRE.bit();
            }
            sortie.step(commands);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.GROUND_HIT) {
                    for (int g = 0; g < sortie.groundObjectCount(); g++) {
                        GroundObject object = sortie.groundObject(g);
                        if (object.spec() == pod
                                && Math.hypot(events.x(i) - object.renderX(), events.y(i) - object.renderY(1)) < 24) {
                            hits++;
                        }
                    }
                }
                if (events.type(i) == SimEvents.Type.SECRET_FOUND) {
                    return new PodPass(sortie.levelSeconds(), hits);
                }
            }
        }
        return new PodPass(-1, hits);
    }

    /**
     * The floating containers: eight destructibles of 3 HP with small salvage, a group of their own
     * whose last one destroyed drops the armour patch (a pilot who shoots each of them).
     */
    @Test
    void theLastFloatingContainerDropsTheArmourPatch() {
        List<LevelScript.GroundObjectSpec> containers =
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).groundObjects().stream()
                        .filter(o -> o.look().equals("floating-container"))
                        .toList();
        assertEquals(8, containers.size());
        for (LevelScript.GroundObjectSpec container : containers) {
            assertEquals(3, container.hp(), 1e-9);
            assertEquals(Optional.of(PickupType.SMALL_SALVAGE), container.drop());
            assertEquals(0, container.group());
            assertEquals(8, container.groupSize());
            assertEquals(Optional.of(PickupType.ARMOUR_PATCH), container.groupDrop());
        }

        Loadout loadout = planLoadout(content, Difficulty.MEDIUM);
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                loadout.plating().maxArmour());
        int destroyed = 0;
        int patchesBefore = -1;
        int patchesAt = -1;
        for (int step = 0; step < SimStep.ticks(containers.getLast().t() + 6); step++) {
            GroundObject target = null;
            for (int i = 0; i < sortie.groundObjectCount(); i++) {
                GroundObject object = sortie.groundObject(i);
                if (object.spec().look().equals("floating-container")
                        && object.renderY(1) < PlayField.HEIGHT
                        && (target == null || object.renderY(1) < target.renderY(1))) {
                    target = object;
                }
            }
            sortie.step(
                    target == null ? Autopilot.commands(sortie) : steer(sortie, target.renderX()) | Command.FIRE.bit());
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.GROUND_DESTROYED
                        && sortie.script()
                                .groundObjects()
                                .get(events.value(i))
                                .look()
                                .equals("floating-container")) {
                    destroyed++;
                    if (destroyed == containers.size() - 1) {
                        patchesBefore = patches(sortie);
                    } else if (destroyed == containers.size()) {
                        patchesAt = patches(sortie);
                    }
                }
            }
        }
        assertEquals(containers.size(), destroyed, "the pilot shot every container");
        assertEquals(0, patchesBefore, "no patch before the last container");
        assertEquals(1, patchesAt, "the last one drops the armour patch");
    }

    private static int patches(Sortie sortie) {
        int count = 0;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            count += sortie.pickup(i).type() == PickupType.ARMOUR_PATCH ? 1 : 0;
        }
        return count;
    }

    /** Commands that move the ship to {@code x} on its start line. */
    private static int steer(Sortie sortie, double x) {
        double dx = x - sortie.ship().x();
        double dy = START_LINE - sortie.ship().y();
        return (dx < -2 ? Command.LEFT.bit() : dx > 2 ? Command.RIGHT.bit() : 0)
                | (dy < -2 ? Command.DOWN.bit() : dy > 2 ? Command.UP.bit() : 0);
    }

    /**
     * The radio script: the timed lines in time order (README Radio chatter, retimed to the 1 s rule),
     * Rook's lines only while he flies, the arena's events (the first telegraph, the first slam arm
     * severed, a ship's first hit and its sinking with its name as {@code {ally}}, the Kraken's death,
     * the secondary), two level-end lines by the ships afloat and no mission-failed line.
     */
    @Test
    void theRadioScriptHasTheArenaEventsAndTwoLevelEndLines() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<Double> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .map(LevelScript.RadioCue::t)
                .toList();
        assertEquals(List.of(1.0, 14.0, 21.5, 34.0, 55.0, 62.5, 70.5, 110.0, 140.0, 146.5, 158.0), timed);
        assertTrue(timed.stream().allMatch(t -> t < 160), "every timed line before the halt");
        Map<LevelScript.CueTrigger, Integer> events = new TreeMap<>();
        for (LevelScript.RadioCue cue : level.radio()) {
            if (cue.trigger() != LevelScript.CueTrigger.TIME) {
                events.merge(cue.trigger(), 1, Integer::sum);
            }
        }
        assertEquals(
                Map.of(
                        LevelScript.CueTrigger.FIRST_TELEGRAPH, 1,
                        LevelScript.CueTrigger.BOSS_PART_DESTROYED, 1,
                        LevelScript.CueTrigger.ALLY_HIT, 1,
                        LevelScript.CueTrigger.ALLY_LOST, 1,
                        LevelScript.CueTrigger.BOSS_DESTROYED, 1,
                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE, 1,
                        LevelScript.CueTrigger.SECRET, 1,
                        LevelScript.CueTrigger.LEVEL_END, 2),
                events);
        assertTrue(level.radio().stream()
                .filter(cue -> cue.speaker().equals("Rook"))
                .allMatch(cue -> (cue.requires() & LevelScript.RadioCue.FITTED_ESCORT) != 0));
        assertTrue(level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.ALLY_HIT
                        || cue.trigger() == LevelScript.CueTrigger.ALLY_LOST)
                .allMatch(cue -> cue.line().contains("{ally}")));
    }

    /** What an autopilot run of the level saw. */
    private static final class Run {
        int shipHits;
        String sunk = "";
        double fight = -1;
        int secrets;
        /** Driftjellies destroyed before the arena (no torpedo: only while surfaced). */
        int jellies;
        /** Where each lost attempt ended: its level time, "boss" while the Kraken was engaged. */
        String deaths = "";
    }

    /**
     * The autopilot flies the level to its end with Rook and the plan's fit (no Torpedo Pod) on every
     * difficulty: the Kraken dies in the arena, the level completes. Printed: the attempt, the fight's
     * length, the ships afloat, the secondary, the grade and the credits.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEndWithRookAndTheConvoy(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        Run run = watch(sortie);
        LevelResult result = sortie.result();
        System.out.printf(
                "Level 11 %s: attempt %d, real %.1f s, Kraken %.1f s, kills %d/%d, credits %d (%s), secrets %d,"
                        + " ships afloat %d/3 (sunk %s, hits %d), secondary %s, armour lost %.0f, Airstrike charges used"
                        + " %d, Rook %s, grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                sortie.realSeconds(),
                run.fight,
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                run.secrets,
                sortie.alliesAfloat(),
                run.sunk,
                run.shipHits,
                result.secondaryMet() ? "met" : "failed",
                result.armourDamage(),
                sortie.special().used(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        assertFalse(sortie.primaryFailed(), "the convoy never fails the mission");
        assertTrue(sortie.setPiece(0).destroyed(), "the Kraken dies");
        assertEquals(
                sortie.alliesAfloat() == 3, result.secondaryMet(), "the secondary is the convoy afloat at the death");
        assertEquals(0, run.secrets, "no torpedo: the sunken pod stays");
    }

    /**
     * The same on KrakenTest's seeds: every run completes; at medium the Kraken's fight in the level
     * (the bar to the kill, with what the approach left on the screen) lasts the mid-boss window on
     * average, as the fixture's (E6 = a). Printed per difficulty: the fights, the ships afloat, the
     * secondary met, the grades and the credits.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotsRunsOnTheKrakensSeeds(Difficulty difficulty) {
        double sum = 0;
        StringBuilder text = new StringBuilder();
        int afloat = 0;
        int met = 0;
        for (long seed : KrakenTest.SEEDS) {
            Sortie sortie = sortie(content, seed, difficulty);
            Run run = watch(sortie);
            LevelResult result = sortie.result();
            assertTrue(sortie.complete(), "seed " + seed + " completes");
            sum += run.fight;
            afloat += sortie.alliesAfloat();
            met += result.secondaryMet() ? 1 : 0;
            text.append(String.format(
                    " [seed %d: attempt %d%s, Kraken %.1f s, afloat %d, jellies %d/%d, grade %s, credits %d]",
                    seed,
                    sortie.attempt(),
                    run.deaths.isEmpty() ? "" : " (lost at t=" + run.deaths.strip() + ")",
                    run.fight,
                    sortie.alliesAfloat(),
                    run.jellies,
                    jellies(sortie.script()),
                    result.grade().letter(),
                    result.credits().total()));
        }
        double mean = sum / KrakenTest.SEEDS.length;
        System.out.printf(
                "Level 11 %s on %d seeds: Kraken mean %.1f s, ships afloat %d of %d, secondary met %d%s%n",
                difficulty, KrakenTest.SEEDS.length, mean, afloat, 3 * KrakenTest.SEEDS.length, met, text);
        if (difficulty == Difficulty.MEDIUM) {
            assertTrue(mean >= 45 && mean <= 75, "the mid-boss window at medium: " + mean);
        }
    }

    /** The Driftjellies of the level's waves (not the Kraken's release). */
    private static int jellies(LevelScript level) {
        return level.waves().stream()
                .filter(wave -> wave.enemy().slug().equals("driftjelly"))
                .mapToInt(WaveSpec::count)
                .sum();
    }

    /** Flies the level with the autopilot, recording the events. */
    private static Run watch(Sortie sortie) {
        Run run = new Run();
        int steps = 0;
        int attempt = sortie.attempt();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            double t = sortie.levelSeconds();
            boolean boss = sortie.setPiece(0).engaged();
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                run.sunk = "";
                run.shipHits = 0;
                run.jellies = 0;
                run.deaths += String.format("%.0f%s ", t, boss ? " (boss)" : "");
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SECRET_FOUND -> run.secrets++;
                    case ALLY_HIT -> run.shipHits++;
                    case ALLY_LOST -> run.sunk += sortie.allyName(sortie.lastAllyLost()) + " ";
                    case ENEMY_DESTROYED -> {
                        if (!boss
                                && sortie.enemyKinds()
                                        .get(events.value(i))
                                        .slug()
                                        .equals("driftjelly")) {
                            run.jellies++;
                        }
                    }
                    default -> {}
                }
            }
            if (sortie.setPiece(0).destroyed() && run.fight < 0) {
                run.fight = sortie.setPiece(0).killSeconds();
            }
        }
        return run;
    }

    /**
     * The attempts after which the arm-first pilot plays the eyes instead (round 33: with the head off
     * the deck's south edge its fan starts closer to the ship, and on hard a deterministic arm-first
     * pilot can lose the same way from the boss checkpoint on every attempt).
     */
    private static final int GIVE_UP_ARMS = 3;

    /**
     * The convoy bonus measured on the whole level (user, 2026-10-09): an arm-first pilot ({@link
     * Autopilot.ArenaPlay#ARMS}) on KrakenTest's seeds, retrying from the boss checkpoint after a
     * wreck. Printed per difficulty: the fights, the ships afloat, the secondary met, the credits; at
     * medium it meets the secondary on most seeds and the fight lasts the mid-boss window on average.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void anArmFirstPilotMeetsTheConvoySecondaryMostOfTheTimeAtMedium(Difficulty difficulty) {
        double sum = 0;
        int afloat = 0;
        int met = 0;
        StringBuilder text = new StringBuilder();
        for (long seed : KrakenTest.SEEDS) {
            Sortie sortie = sortie(content, seed, difficulty);
            int steps = 0;
            int attempts = sortie.attempt();
            while (!sortie.complete() && steps++ < MAX_STEPS) {
                if (!sortie.flying() || sortie.primaryFailed()) {
                    sortie.retry(sortie.ship().defences().maxArmour());
                }
                // A pilot whom the arm-first play keeps wrecking (hard) plays the eyes from its fourth attempt.
                Autopilot.ArenaPlay play = sortie.attempt() - attempts < GIVE_UP_ARMS
                        ? Autopilot.ArenaPlay.ARMS
                        : Autopilot.ArenaPlay.EYES;
                sortie.step(Autopilot.commands(sortie, play));
            }
            assertTrue(sortie.complete(), "seed " + seed + " completes");
            LevelResult result = sortie.result();
            double fight = sortie.setPiece(0).killSeconds();
            sum += fight;
            afloat += sortie.alliesAfloat();
            met += result.secondaryMet() ? 1 : 0;
            text.append(String.format(
                    " [seed %d: attempt %d, Kraken %.1f s, afloat %d, credits %d, grade %s]",
                    seed,
                    sortie.attempt() - attempts + 1,
                    fight,
                    sortie.alliesAfloat(),
                    result.credits().total(),
                    result.grade().letter()));
        }
        double mean = sum / KrakenTest.SEEDS.length;
        System.out.printf(
                "Level 11 arm-first %s on %d seeds: Kraken mean %.1f s, ships afloat %d of %d, secondary met %d%s%n",
                difficulty, KrakenTest.SEEDS.length, mean, afloat, 3 * KrakenTest.SEEDS.length, met, text);
        if (difficulty == Difficulty.MEDIUM) {
            assertTrue(met >= 3, "most of the time at medium: " + met);
            assertTrue(mean >= 45 && mean <= 75, "the mid-boss window at medium: " + mean);
        }
    }

    /** Platform Tiamat's deck (design/campaign/act-2-homefront/level-11-atlantic-convoy, round 07): px. */
    private static final double DECK_WIDTH = 208;

    private static final double DECK_HEIGHT = 152;
    /** Its centre above the bottom edge at the halt (the Kraken's anchor), px. */
    private static final double DECK_AT_HALT = PlayField.HEIGHT - 78;

    /**
     * After the Kraken the ships hold clear of Platform Tiamat (2026-10-09): while its deck scrolls
     * down through the play field no ship afloat (its sprite's box) overlaps it (Mbeki drops back off
     * the bottom edge from lane 2, Halvorsen and Saint-Laurent hold in lanes 1 and 4, the frigate stays
     * away), and they are back on their stations before the level ends.
     */
    @Test
    void theShipsHoldClearOfTiamatAfterTheKraken() {
        Sortie sortie = sortie(content, 2185, Difficulty.EASY);
        double haltScroll = Double.NaN;
        boolean checked = false;
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
            if (Double.isNaN(haltScroll) && sortie.setPiece(0).engaged()) {
                haltScroll = sortie.groundScroll();
            }
            if (!sortie.setPiece(0).destroyed()) {
                continue;
            }
            double deckY = DECK_AT_HALT - (sortie.groundScroll() - haltScroll);
            if (deckY + DECK_HEIGHT / 2 < 0) {
                continue;
            }
            checked = true;
            for (int k = 0; k < sortie.allyCount(); k++) {
                var ally = sortie.ally(k);
                var size = sortie.allySpec(k).size();
                if (!ally.alive()
                        || ally.y() + size.height() / 2 < 0
                        || ally.y() - size.height() / 2 > PlayField.HEIGHT) {
                    continue;
                }
                boolean overlaps = Math.abs(ally.x() - PlayField.WIDTH / 2.0) < (DECK_WIDTH + size.width()) / 2
                        && Math.abs(ally.y() - deckY) < (DECK_HEIGHT + size.height()) / 2;
                assertFalse(
                        overlaps,
                        String.format(
                                "%s at (%.0f, %.0f) under the deck at y %.0f, t=%.2f",
                                sortie.allyName(k), ally.x(), ally.y(), deckY, sortie.levelSeconds()));
            }
        }
        assertTrue(checked, "the deck scrolled through after the Kraken");
        LevelScript.Naval naval = sortie.script().convoy().orElseThrow();
        for (int k = 0; k < sortie.allyCount(); k++) {
            if (sortie.ally(k).alive()) {
                assertEquals(
                        naval.units().get(k).x(), sortie.ally(k).x(), 1e-6, sortie.allyName(k) + " on its station");
                assertEquals(
                        naval.units().get(k).y(), sortie.ally(k).y(), 1e-6, sortie.allyName(k) + " on its station");
            }
        }
    }

    /** A Reef Spitter's raft: its drawn base, px (design/enemies/naval/reef-spitter). */
    private static final double RAFT = 84;

    /**
     * The rafts keep off the hulls (2026-10-09): on every difficulty no Reef Spitter's 84 px raft,
     * drifting on its nest's current, overlaps a cargo ship at its station (its sprite's box) as it
     * scrolls past them.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theRaftsKeepOffTheHulls(Difficulty difficulty) {
        LevelData data = content.level(LEVEL);
        LevelData.Convoy convoy = data.convoy().orElseThrow();
        double speed = data.scrollSpeed();
        double drift = content.enemy("reef-spitter")
                .movement()
                .terrain()
                .orElseThrow()
                .drift()
                .orElseThrow();
        int rafts = 0;
        for (LevelData.GroundTarget target : data.groundTargets()) {
            if (!target.enemy().equals(Optional.of("reef-spitter"))) {
                continue;
            }
            List<LevelData.Placement> at = target.at();
            if (difficulty == Difficulty.HARD
                    && target.hard().flatMap(LevelData.Placements::at).isPresent()) {
                at = target.hard().flatMap(LevelData.Placements::at).orElseThrow();
            }
            double current = Math.toRadians(target.current().orElse(0.0));
            for (LevelData.Placement placement : at) {
                rafts++;
                // y below the top edge as it scrolls in from above it; the drift along the current.
                for (double y = -RAFT / 2; y <= PlayField.HEIGHT + RAFT / 2; y += 2) {
                    double seconds = (y + RAFT / 2) / (speed + drift * Math.cos(current));
                    double x = placement.x() + drift * Math.sin(current) * seconds;
                    for (LevelData.ConvoyUnit unit : convoy.units()) {
                        var size = content.allies().allies().get(unit.ally()).size();
                        assertFalse(
                                Math.abs(x - unit.station().x()) < (RAFT + size.width()) / 2
                                        && Math.abs(y - unit.station().y()) < (RAFT + size.height()) / 2,
                                String.format(
                                        "the raft at t=%.2f x %.0f over %s",
                                        placement.t(), placement.x(), unit.name()));
                    }
                }
            }
        }
        assertEquals(difficulty == Difficulty.HARD ? 15 : 12, rafts);
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level11Test::fly);

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
