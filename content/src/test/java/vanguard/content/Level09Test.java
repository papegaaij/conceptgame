package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.Armament;
import vanguard.sim.Command;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;
import vanguard.sim.WingmanSpec;

/**
 * Level 09 as the game runs it (design/campaign/act-2-homefront/level-09-arcology-fall): its credit
 * budget and the README's totals, the six Hive Nodes as named targets with their three hold zones
 * (20 / 30 / 40 px/s), the collapse over cluster C, the Ravager packs (their sizes per difficulty)
 * and the scoped bridge secondary (6 / 8 / 10), the cocoon secret, the pickups, the radio script, and
 * whole runs in which the autopilot bombs the nodes and flies the level to its end with Rook.
 */
class Level09Test {
    static final String LEVEL = "act-2-homefront/level-09-arcology-fall";
    private static final int MAX_STEPS = 60 * 60 * 20;
    private static final List<String> NODES = List.of("Node A1", "Node A2", "Node B1", "Node B2", "Node C1", "Node C2");

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 09 (design/player/balance-plan.yaml, user decision D8 =
     * c of M5 part C): Level 08's fit with a Bomb Rack swapped for the left Autocannon Pod, and Rook in
     * the escort slot with his Mortar at L1, on his new campaign's side at full armour, and the two
     * Airstrike charges the plan holds by then (the free one and the one bought at Level 04).
     * PacingTest flies it too.
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return planLoadout(content, difficulty, WingmanSpec.Side.LEFT);
    }

    static Loadout planLoadout(Content content, Difficulty difficulty, WingmanSpec.Side side) {
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 3),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "bomb-rack", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "autocannon-pod", 1)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(1).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        content, "mortar", 1, side, content.wingmen().rook().armour()))
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
        int ground = 0;
        int released = 0;
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            ground += unit.enemy().bounty();
            // one opening per node: its Skitters pay their own bounty
            released += unit.enemy()
                    .spawner()
                    .map(spawner -> spawner.count() * spawner.enemy().bounty())
                    .orElse(0);
        }
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        int total = kills + ground + released + crates + level.secondary().credits();

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_203, Math.round(budget), "the typical haul's target, 700 × 1.07⁸");
        assertEquals(
                146 * 5 + 38 * 12 + 9 * 15 + 9 * 22 + 22 * 18, kills, "Skitter, Needler, Stinger, Creeper, Ravager");
        assertEquals(6 * 45 + 7 * 12 + 4 * 15, ground, "Hive Node 6 × 45 + Spine Turret 7 × 12 + Polyp Mortar 4 × 15");
        assertEquals(12 * 5, released, "one opening of 2 Skitters per node");
        assertEquals(100, crates, "the cocoon cache in Act 1 terms (pays 160)");
        assertEquals(63, level.secondary().credits(), "in Act 1 terms (pays 101)");
        System.out.printf(
                "Level 09 budget (Act 1 terms): kills %d, ground %d, node Skitters %d, crate %d, secondary %d: %d"
                        + " (budget %.0f)%n",
                kills, ground, released, crates, level.secondary().credits(), total, budget);
        assertEquals(2_552, total, "before the act factor and bounty_scale (TypicalHaulTest checks the paid run)");
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

        assertEquals(Map.of("hive-node", 6, "polyp-mortar", 4, "spine-turret", 7), ground);
        assertEquals(Map.of("creeper", 9, "needler", 38, "ravager", 22, "skitter", 146, "stinger", 9), waves);
        assertEquals(
                List.of(20.0, 58.0, 96.0, 138.0, 186.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertEquals(NODES, level.groups(), "the primary's named targets");
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(8, sortie.escapesTotal(), "the two bridge packs");
        System.out.printf("Level 09 enemy total at the start: %d%n", sortie.enemyTotal());
        assertEquals(241, sortie.enemyTotal(), "the node Skitters count as they are released");
    }

    /**
     * Easy: the bridge packs of 3 (the secondary counts 6), the pincers from the front, holds at
     * 20 px/s, a second Airstrike charge. Hard: every pack one more (the bridge 10), holds at 40 px/s,
     * the cluster C turret pair, 3 Skitters per opening, pounces every 2 s.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeThePacksTheHoldsThePincersAndClusterC(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        List<Integer> packs = level.waves().stream()
                .filter(wave -> wave.enemy().slug().equals("ravager"))
                .map(WaveSpec::count)
                .toList();
        assertEquals(
                switch (difficulty) {
                    case EASY -> List.of(3, 3, 3, 3, 3, 3);
                    case MEDIUM -> List.of(3, 4, 4, 4, 3, 4);
                    case HARD -> List.of(4, 5, 5, 5, 4, 5);
                },
                packs);
        assertEquals(
                switch (difficulty) {
                    case EASY -> 6;
                    case MEDIUM -> 8;
                    case HARD -> 10;
                },
                sortie(content, 1, difficulty).escapesTotal(),
                "the secondary counts the two bridge packs");
        double speed =
                switch (difficulty) {
                    case EASY -> 20;
                    case MEDIUM -> 30;
                    case HARD -> 40;
                };
        assertEquals(3, level.holds().size());
        for (LevelScript.Hold hold : level.holds()) {
            assertEquals(speed, hold.speed());
            assertEquals(200, hold.depth());
            assertEquals(1, hold.rampSeconds());
        }
        for (double t : new double[] {49, 128}) {
            WaveSpec flank = wave(level, t);
            assertEquals("needler", flank.enemy().slug());
            assertEquals(difficulty == Difficulty.EASY ? WaveSpec.Entry.FRONT : WaveSpec.Entry.SIDES, flank.entry());
            assertEquals(1, level.waves().stream().filter(w -> w.t() == t).count());
        }
        long turrets = level.groundUnits().stream()
                .filter(unit -> unit.enemy().slug().equals("spine-turret"))
                .count();
        assertEquals(difficulty == Difficulty.HARD ? 9 : 7, turrets, "hard: the cluster C pair");
        EnemySpec node = level.groundUnits().stream()
                .map(LevelScript.GroundUnit::enemy)
                .filter(enemy -> enemy.slug().equals("hive-node"))
                .findFirst()
                .orElseThrow();
        assertTrue(node.hardened());
        assertEquals(
                difficulty == Difficulty.HARD ? 3 : 2,
                node.spawner().orElseThrow().count());
        EnemySpec ravager = level.waves().stream()
                .map(WaveSpec::enemy)
                .filter(enemy -> enemy.slug().equals("ravager"))
                .findFirst()
                .orElseThrow();
        if (difficulty == Difficulty.HARD) {
            assertEquals(2.0, ravager.pounce().orElseThrow().intervalSeconds(), 1e-9, "hard's authored interval");
        }
    }

    /** Each node is its own named target; the holds wait for the clusters; the collapse for cluster C. */
    @Test
    void theNodesAreTheTargetsOfTheHoldsAndTheCollapse() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        assertEquals(
                List.of(List.of(0, 1), List.of(2, 3), List.of(4, 5)),
                level.holds().stream().map(LevelScript.Hold::groups).toList());
        for (int g = 0; g < NODES.size(); g++) {
            int group = g;
            List<LevelScript.GroundUnit> units = level.groundUnits().stream()
                    .filter(unit -> unit.group() == group)
                    .toList();
            assertEquals(1, units.size(), NODES.get(g));
            assertEquals("hive-node", units.getFirst().enemy().slug());
        }
        LevelScript.Collapse collapse = level.collapse().orElseThrow();
        assertEquals(List.of(4, 5), collapse.groups());
        assertEquals(1.5, collapse.warningSeconds(), "the lean");
        assertEquals(1.5, collapse.dropSeconds(), "the drop: the impact 3 s after the warning starts");
        assertEquals(1, collapse.blastSeconds(), "the blast's front");
        assertEquals(100, collapse.blastFrom());
        assertEquals(390, collapse.blastTo());
        assertEquals(100, collapse.x(), 1e-9, "round the arcology's foot");
        LevelData.Collapse data = content.level(LEVEL).collapse().orElseThrow();
        assertEquals("arcology-heap", data.rubble());
        assertEquals("arcology", data.tower());
        double centre = arcologyCentre(content);
        assertEquals(centre - 90, collapse.bottom(), 1e-9, "the band: the arcology's footprint on the ground");
        assertEquals(centre + 90, collapse.top(), 1e-9);
        assertTrue(content.level(LEVEL).backdrop().pieces().containsKey("arcology"), "the tower that falls");
        assertEquals(
                List.of("anti-ground"), content.level(LEVEL).threatProfile().requiredTraits());
        assertTrue(content.level(LEVEL).music().fullOn(LevelData.Music.FullOn.HOLD));
        assertTrue(content.level(LEVEL).music().fullOn(LevelData.Music.FullOn.COLLAPSE));
        assertTrue(!content.level(LEVEL).music().full(5), "the base stem until a hold or the collapse");
    }

    @Test
    void theCocoonHidesTheCacheAndTheWavesCarryThePickups() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec cocoon = level.groundObjects().stream()
                .filter(o -> o.secret().equals("cocoon cache"))
                .findFirst()
                .orElseThrow();
        assertTrue(cocoon.hardened(), "only anti-ground, the Airstrike or the Smart Bomb open it");
        assertEquals(100, cocoon.crateCredits());
        assertEquals(java.util.Optional.of(PickupType.ARMOUR_PATCH), cocoon.drop());
        assertEquals(
                List.of(new WaveSpec.Carried(PickupType.SPECIAL_CHARGE, WaveSpec.Carried.LAST)),
                wave(level, 31).carried(),
                "the Airstrike charge ahead of the plaza's turret pair");
        assertEquals(
                List.of(new WaveSpec.Carried(PickupType.OVERDRIVE, WaveSpec.Carried.LAST)),
                wave(level, 76).carried());
        assertTrue(wave(level, 116).carried().isEmpty());
        assertEquals(
                List.of(new WaveSpec.Carried(PickupType.SPECIAL_CHARGE, WaveSpec.Carried.LAST)),
                wave(SimSpecs.level(content, LEVEL, Difficulty.EASY), 116).carried(),
                "easy's second charge over the bridgehead");
    }

    /**
     * The radio script: the timed lines in time order at least 6 s apart (README Radio chatter,
     * retimed to the 1 s rule), Rook's lines only while he flies, the new events, the Kilo Lead's two
     * lines and the failed screen's line naming the node.
     */
    @Test
    void theRadioScriptIsRetimedWithTheNewEvents() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<LevelScript.RadioCue> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .toList();
        assertEquals(
                List.of(1.0, 8.5, 24.0, 58.0, 95.0, 107.0, 140.0, 152.5),
                timed.stream().map(LevelScript.RadioCue::t).toList());
        for (int i = 1; i < timed.size(); i++) {
            assertTrue(
                    timed.get(i).t() - timed.get(i - 1).t() >= 6,
                    "t=" + timed.get(i - 1).t() + " and t=" + timed.get(i).t() + " too close");
        }
        List<LevelData.RadioCue> cues = content.level(LEVEL).radio();
        for (LevelData.RadioCue cue : cues) {
            if (cue.speaker().equals("Rook")) {
                assertEquals(java.util.Optional.of("escort"), cue.requires(), cue.line());
            }
        }
        for (LevelData.CueEvent event : List.of(
                LevelData.CueEvent.HOLD_START,
                LevelData.CueEvent.FIRST_POUNCE,
                LevelData.CueEvent.COLLAPSE,
                LevelData.CueEvent.FIRST_KILL,
                LevelData.CueEvent.MISSION_FAILED)) {
            assertEquals(
                    1,
                    cues.stream()
                            .filter(cue -> cue.event().orElse(null) == event)
                            .count(),
                    event.name());
        }
        assertEquals(
                2,
                cues.stream().filter(cue -> cue.speaker().equals("Kilo Lead")).count());
        assertTrue(cues.stream()
                .anyMatch(cue -> cue.event().orElse(null) == LevelData.CueEvent.MISSION_FAILED
                        && cue.line().contains("{group}")));
    }

    /**
     * The autopilot flies the plan's fit (the Bomb Rack and Rook's Mortar) through the holds: it
     * bombs every node, the collapse falls, and the level ends with Rook on its wing.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotBombsTheNodesAndFliesTheLevelToItsEndWithRook(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        int secrets = 0;
        int holds = 0;
        int pounces = 0;
        int failures = 0;
        double collapseAt = -1;
        double collapseBand = Double.NaN;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            if (sortie.primaryFailed()) {
                failures++;
            }
            Level04Test.step(sortie);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SECRET_FOUND -> secrets++;
                    case HOLD_START -> holds++;
                    case POUNCE -> pounces++;
                    case COLLAPSE_WARNING -> {
                        collapseAt = sortie.levelSeconds();
                        double arcology = arcologyCentre(content) - sortie.groundScroll();
                        collapseBand = PlayField.HEIGHT - arcology;
                        // The band lies on the arcology's footprint, whenever the nodes die.
                        assertEquals(
                                arcology,
                                (sortie.collapseBandTop() + sortie.collapseBandBottom()) / 2,
                                1e-6,
                                "the band round the arcology");
                        assertEquals(180, sortie.collapseBandTop() - sortie.collapseBandBottom(), 1e-6);
                    }
                    default -> {}
                }
            }
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Level 09 %s: attempt %d (%d failed steps), real %.1f s, holds %d, Airstrike charges used %d, pounces %d, collapse at t=%.2f"
                        + " (the arcology %.0f px below the top), kills %d/%d, credits %d (%s), secrets %d,"
                        + " bridge %s, armour lost %.0f, Rook %s, grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                failures,
                sortie.realSeconds(),
                holds,
                sortie.special().used(),
                pounces,
                collapseAt,
                collapseBand,
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                secrets,
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                result.armourDamage(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        for (int g = 0; g < NODES.size(); g++) {
            assertEquals(1, sortie.groupState(g), NODES.get(g) + " destroyed");
        }
        assertTrue(collapseAt > 0, "the arcology falls");
        assertEquals(3, holds, "three holds in the winning attempt");
    }

    /** The ground position of the arcology's footprint's centre (the backdrop's placement of the tower that falls). */
    private static double arcologyCentre(Content content) {
        LevelData level = content.level(LEVEL);
        return level.pieceCentre(level.backdrop().placements().stream()
                .filter(placed -> placed.piece().equals("arcology"))
                .findFirst()
                .orElseThrow());
    }

    /**
     * Hold C lasts until the collapse's dust has settled (user decision, 2026-10-07). Cluster C killed
     * late, as in round 31's capture (Node C2 dead 7.6 s into hold C, the tower's foot about 60 px
     * above the bottom edge at the impact): the scroll stays at the hold's 30 px/s until 9.0 s after
     * the warning starts, so the rubble heap round the foot stays on the screen (sliding down under
     * the settling dust) until the hold ends; then the scroll eases back to the section's 150 px/s.
     * The pilot (the capture's two Bomb Racks, no Rook) holds its fire until 1.8 s into hold C.
     */
    @Test
    void withALateKillTheHeapStaysInSightUntilTheDustHasSettled() {
        Loadout plan = planLoadout(content, Difficulty.MEDIUM);
        Loadout loadout = SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 3),
                        new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "bomb-rack", 2),
                        new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "bomb-rack", 2)),
                content.shields().models().get(1).name(),
                content.armour().plating().get(1).name(),
                0,
                Difficulty.MEDIUM);
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                plan.plating().maxArmour());
        double heap = content.level(LEVEL)
                        .backdrop()
                        .pieces()
                        .get("arcology-heap")
                        .size()
                        .height()
                / 2;
        int steps = 0;
        double holdC = -1;
        double warning = -1;
        double footAtImpact = Double.NaN;
        double holdEnd = -1;
        double footAtHoldEnd = Double.NaN;
        double eased = -1;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            boolean fire = holdC >= 0 && sortie.realSeconds() - holdC >= 1.8;
            sortie.step(fire ? Autopilot.commands(sortie) : Command.NONE);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case HOLD_START -> {
                        if (sortie.activeHold() == 2) {
                            holdC = sortie.realSeconds();
                        }
                    }
                    case COLLAPSE_WARNING -> warning = sortie.realSeconds();
                    case COLLAPSE_IMPACT -> footAtImpact = sortie.collapseFootY();
                    case HOLD_END -> {
                        if (warning >= 0) {
                            holdEnd = sortie.realSeconds() - warning;
                            footAtHoldEnd = sortie.collapseFootY();
                        }
                    }
                    default -> {}
                }
            }
            if (warning >= 0 && sortie.holdActive()) {
                assertEquals(30, sortie.groundSpeed(), 1e-9, "hold C's speed");
                assertTrue(
                        sortie.collapseFootY() + heap > 0,
                        String.format(
                                "the heap on the screen %.2f s after the warning", sortie.realSeconds() - warning));
            }
            if (holdEnd >= 0 && eased < 0 && sortie.groundSpeed() == 150) {
                eased = sortie.realSeconds() - warning - holdEnd;
            }
        }
        System.out.printf(
                "Level 09 late kill: the warning %.2f s into hold C, the foot %.0f px above the bottom edge at the"
                        + " impact; hold C ended %.2f s after the warning (the foot at %.0f px), eased back in %.2f s%n",
                warning - holdC, footAtImpact, holdEnd, footAtHoldEnd, eased);
        assertTrue(warning > 0, "the arcology falls");
        assertEquals(7.6, warning - holdC, 0.5, "late, as in the capture");
        assertEquals(60, footAtImpact, 15, "the foot low on the screen at the impact, as in the capture");
        assertEquals(9.0, holdEnd, 1.5 / 60, "hold C ends once the dust has settled");
        assertTrue(eased > 0 && eased <= 1.1, "then the scroll eases back to 150 px/s: " + eased);
    }

    /**
     * The music's run-time hook ({@code full_on: [hold, collapse]}, the game's {@code
     * LevelScreen.runTimeFull}): the autopilot's run asks for the full mix at every step from hold C's
     * start through the collapse's warning, fall and end (hold C ending once its dust has settled) to the level's
     * end, so the music never drops to the base stem there.
     */
    @Test
    void theFullMixIsAskedForFromHoldCThroughTheCollapseToTheEnd() {
        LevelData.Music music = content.level(LEVEL).music();
        assertTrue(music.fullOn(LevelData.Music.FullOn.HOLD));
        assertTrue(music.fullOn(LevelData.Music.FullOn.COLLAPSE));
        Sortie sortie = sortie(content, 2185, Difficulty.MEDIUM);
        int attempt = sortie.attempt();
        boolean holdC = false;
        Set<SimEvents.Type> seen = EnumSet.noneOf(SimEvents.Type.class);
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                holdC = false;
                seen.clear();
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                SimEvents.Type type = events.type(i);
                if (type == SimEvents.Type.HOLD_START && sortie.activeHold() == 2) {
                    holdC = true;
                }
                if (holdC) {
                    seen.add(type);
                }
            }
            if (holdC) {
                boolean full = (sortie.holdActive() && music.fullOn(LevelData.Music.FullOn.HOLD))
                        || (sortie.collapseStarted() && music.fullOn(LevelData.Music.FullOn.COLLAPSE));
                assertTrue(full, String.format("the full mix at real t=%.2f", sortie.realSeconds()));
            }
        }
        assertTrue(sortie.complete());
        assertTrue(holdC, "hold C ran");
        assertTrue(
                seen.containsAll(EnumSet.of(
                        SimEvents.Type.COLLAPSE_WARNING,
                        SimEvents.Type.COLLAPSE_FALL,
                        SimEvents.Type.COLLAPSE_END,
                        SimEvents.Type.HOLD_END)),
                "hold C, the collapse's warning, fall and end: " + seen);
        assertFalse(sortie.holdActive(), "hold C ended once the dust had settled");
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level09Test::fly);

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
