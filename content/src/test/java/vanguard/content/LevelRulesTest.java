package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.Armament;
import vanguard.sim.Command;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.Tow;
import vanguard.sim.WaveSpec;

/**
 * Part G's level rules (design/campaign, Level 07) on {@link BossLevelTest}'s frigate level: a tow
 * whose cable releases a secret's crate, a pickup dropped by the first boss part shot off, the
 * parts objective (met, and failed when the boss moves on with a part alive), radio cues that
 * require a homing weapon or its absence, the boss-phase timeout trigger, a wave's hold changed on
 * a difficulty and a placed pickup left out on one.
 */
class LevelRulesTest {
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String KEY = BossLevelTest.KEY;
    private static final int MAX_STEPS = 60 * 60 * 10;
    private static final String HEADS = "[left head, centre head, right head]";
    private static final String FRIGATE = "enemies/bosses/gorgon-frigate/data.yaml";
    private static final String LAST_HEAD_UNTIL = "until: {parts: " + HEADS + ", left: 0}";

    private static final String TOW =
            "\ntows:\n  - {t: 8, x: 240, drift: [0, -60], boat: [60, 30], pod: [28, 28], tether: [0, 60],"
                    + " cable: [24, 40], hits: 3, reveals: tow cache}\n";

    private static final String RADIO = """
              - {t: 151, requires: homing, speaker: Varga, line: "Missiles reach it."}
              - {t: 153, requires_not: homing, speaker: Varga, line: "Nothing reaches it."}
              - {event: boss-phase, phase: Core, timeout: true, speaker: Okafor, line: "Forget the heads."}
            """;

    /** The frigate level with part G's rules; the heads must all die before phase {@code before} ends. */
    static Content content(String before) {
        return content(before, false);
    }

    /** As {@link #content(String)}; with {@code timeout}, the frigate's last-head phase times out 2 s after it engages. */
    static Content content(String before, boolean timeout) {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        if (timeout) {
            files.replaceAll(file -> file.path().equals(FRIGATE)
                    ? new DataFile(
                            FRIGATE,
                            replaceOnce(
                                    file.text(),
                                    LAST_HEAD_UNTIL,
                                    LAST_HEAD_UNTIL.replace("left: 0}", "left: 0, seconds: 2}")))
                    : file);
        }
        String level01 = files.stream()
                .filter(file -> file.path().equals(LEVEL_01))
                .findFirst()
                .orElseThrow()
                .text();
        String text = BossLevelTest.bossLevel(level01);
        text = replaceOnce(
                text,
                "\nsecrets:\n",
                "\nsecrets:\n  - name: tow cache\n    crate: 75\n    radio: {speaker: Rook, line: \"Pod's loose.\"}\n");
        text = replaceOnce(
                text,
                "  - {pickup: armour patch, dropped_by: {wave: 122, unit: last}}\n",
                "  - {pickup: armour patch, dropped_by: {wave: 122, unit: last}, skip: [hard]}\n"
                        + "  - {pickup: overdrive, dropped_by: {parts: " + HEADS + ", unit: first}}\n");
        text = replaceOnce(text, "hold: 4, notes:", "hold: 4, easy: {hold: 2}, notes:");
        text = replaceOnce(
                text,
                "  secondary: {kill_ratio: 0.65, credits: 50}",
                "  secondary: {parts: " + HEADS + ", before: " + before + ", label: HEADS, credits: 50}");
        text = text.replaceFirst("(?m)^radio:.*\\n", "$0" + RADIO.replace("$", "\\$"));
        files.removeIf(file -> file.path().equals(BossLevelTest.PATH));
        files.add(new DataFile(BossLevelTest.PATH, text + TOW));
        return ContentLoader.load(files);
    }

    private static String replaceOnce(String text, String what, String with) {
        assertEquals(1, text.split(java.util.regex.Pattern.quote(what), -1).length - 1, "once in the level: " + what);
        return text.replace(what, with);
    }

    private final Content content = content("Last head");

    @Test
    void theRulesReachTheSimulation() {
        LevelScript level = SimSpecs.level(content, KEY, Difficulty.MEDIUM);

        LevelScript.TowSpec tow = level.tows().getFirst();
        assertEquals(75, tow.crateCredits());
        assertEquals("tow cache", tow.secret());
        assertEquals(3, tow.hits());
        assertEquals(60, tow.podDy(), 1e-9);
        assertEquals(-60, tow.vy(), 1e-9);

        LevelScript.PartDrop drop = level.partDrops().getFirst();
        assertEquals("gorgon-frigate", drop.slug());
        assertEquals(List.of(0, 1, 2), drop.parts());
        assertEquals(1, drop.dropsAt());
        assertEquals(PickupType.OVERDRIVE, drop.pickup());

        LevelScript.Secondary secondary = level.secondary();
        assertTrue(secondary.byParts());
        assertTrue(secondary.byEscapes(), "the HUD counts the parts as it counts escapers");
        assertEquals("gorgon-frigate", secondary.partsOf());
        assertEquals(List.of(0, 1, 2), secondary.parts());
        assertEquals(1, secondary.beforePhase());
        assertEquals("HEADS", secondary.label());

        LevelScript.RadioCue homing = cueAt(level, 151);
        assertEquals(LevelScript.RadioCue.FITTED_HOMING, homing.requires());
        assertEquals(0, homing.requiresNot());
        assertFalse(homing.requiresSpecial());
        LevelScript.RadioCue noHoming = cueAt(level, 153);
        assertEquals(0, noHoming.requires());
        assertEquals(LevelScript.RadioCue.FITTED_HOMING, noHoming.requiresNot());
        LevelScript.RadioCue timeout =
                level.radio().get(BossLevelTest.cueIndex(level, LevelScript.CueTrigger.BOSS_TIMEOUT));
        assertEquals("Core", timeout.subject());

        assertEquals(4, pincer(level).holdSeconds().orElseThrow(), 1e-9);
        assertEquals(
                2,
                pincer(SimSpecs.level(content, KEY, Difficulty.EASY))
                        .holdSeconds()
                        .orElseThrow(),
                1e-9);
        assertFalse(pincer(level).carried().isEmpty(), "medium: the pincer's last Needler carries the patch");
        assertTrue(
                pincer(SimSpecs.level(content, KEY, Difficulty.HARD)).carried().isEmpty(),
                "hard: the patch is left out");
    }

    @Test
    void threeHitsOnTheCableReleaseTheCrateAndShotsPassTheBoat() {
        Sortie sortie = BossLevelTest.sortie(content, 7, Difficulty.MEDIUM);
        int secretCue = secretCue(sortie.script(), "tow cache");
        int cableHits = 0;
        boolean found = false;
        boolean lineStarted = false;
        boolean crate = false;
        boolean passed = false;
        for (int step = 0; step < 60 * 20 && !(passed && crate); step++) {
            sortie.step(Command.FIRE.bit());
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case CLAMP_HIT -> cableHits++;
                    case SECRET_FOUND -> found = true;
                    case RADIO -> lineStarted |= events.value(i) == secretCue;
                    default -> {}
                }
            }
            for (int i = 0; i < sortie.pickupCount(); i++) {
                crate |= sortie.pickup(i).type() == PickupType.HIDDEN_CRATE;
            }
            Tow tow = sortie.tow(0);
            if (found && tow.present()) {
                for (int i = 0; i < sortie.shotCount(); i++) {
                    passed |= Math.abs(sortie.shot(i).renderX(1) - tow.x()) < 20
                            && sortie.shot(i).renderY(1) > tow.y() + 20;
                }
            }
            assertFalse(cableHits > 3, "the cut cable takes no more hits");
        }
        assertEquals(3, cableHits);
        assertTrue(found, "the cut releases the secret");
        assertTrue(crate, "the crate falls out of the loose pod");
        assertTrue(lineStarted, "the secret's line starts");
        assertFalse(sortie.tow(0).holding());
        assertTrue(passed, "shots fly on through the boat");
    }

    @Test
    void theFirstHeadDropsTheOverdriveAndAllThreeMeetTheObjective() {
        Sortie sortie = fly(content, false);
        assertTrue(sortie.secondaryMet(), "every head shot off before the last-head phase ended");
        assertFalse(sortie.secondaryFailed());
        assertEquals(3, sortie.escapesDestroyed());
        assertEquals(3, sortie.escapesTotal());
    }

    @Test
    void aHeadAliveWhenThePhaseEndsFailsTheObjective() {
        Sortie sortie = fly(content("Three heads"), false);
        assertTrue(sortie.secondaryFailed(), "a head lived on into the last-head phase");
        assertFalse(sortie.secondaryMet());
    }

    @Test
    void theHomingLinesFollowTheFit() {
        Sortie without = fly(content, false);
        Sortie with = fly(content, true);
        assertTrue(radioLines.get(without).contains(cueAt(without.script(), 153)));
        assertFalse(radioLines.get(without).contains(cueAt(without.script(), 151)));
        assertTrue(radioLines.get(with).contains(cueAt(with.script(), 151)));
        assertFalse(radioLines.get(with).contains(cueAt(with.script(), 153)));
        LevelScript.RadioCue timeout =
                with.script().radio().get(BossLevelTest.cueIndex(with.script(), LevelScript.CueTrigger.BOSS_TIMEOUT));
        assertFalse(
                radioLines.get(with).contains(timeout), "the frigate's phases end on their parts, never on a timeout");
    }

    /**
     * The boss-phase timeout trigger: when the last-head phase runs out of time with a head alive,
     * the timeout line starts before the core phase's own, and the parts objective fails.
     */
    @Test
    void aPhaseThatTimesOutStartsItsTimeoutLine() {
        Sortie sortie = fly(content("Last head", true), false);
        LevelScript script = sortie.script();
        LevelScript.RadioCue timeout =
                script.radio().get(BossLevelTest.cueIndex(script, LevelScript.CueTrigger.BOSS_TIMEOUT));
        List<LevelScript.RadioCue> started = radioLines.get(sortie);
        assertTrue(started.contains(timeout), "the timeout line starts");
        assertTrue(sortie.secondaryFailed(), "a head outlived the last-head phase");
    }

    /** The radio cues each flown sortie started. */
    private final java.util.Map<Sortie, List<LevelScript.RadioCue>> radioLines = new java.util.IdentityHashMap<>();

    /**
     * Flies the level on medium with the autopilot to its end, retrying from the boss checkpoint
     * after a wreck; checks the overdrive falls in the step the first head breaks and in no later
     * head's step.
     */
    private Sortie fly(Content level, boolean homing) {
        Sortie sortie = homing ? homingSortie(level) : BossLevelTest.sortie(level, 2185, Difficulty.MEDIUM);
        List<LevelScript.RadioCue> started = new ArrayList<>();
        int headsOff = 0;
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            if (!sortie.flying()) {
                if (sortie.bossCheckpoint()) {
                    sortie.retryFromBoss();
                } else {
                    sortie.retry(sortie.ship().defences().maxArmour());
                }
            }
            int overdrives = overdrives(sortie);
            sortie.step(Autopilot.commands(sortie));
            SimEvents events = sortie.events();
            boolean restarted = false;
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case RADIO -> started.add(sortie.script().radio().get(events.value(i)));
                    case BOSS_RETRY, SORTIE_RESTARTED -> restarted = true;
                    default -> {}
                }
            }
            if (restarted) {
                headsOff = 0;
                continue;
            }
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.PART_DESTROYED && events.value(i) < 3 && headsOff++ == 0) {
                    assertEquals(overdrives + 1, overdrives(sortie), "the first head drops the overdrive");
                }
            }
        }
        assertTrue(sortie.complete());
        radioLines.put(sortie, started);
        return sortie;
    }

    private static int overdrives(Sortie sortie) {
        int count = 0;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            count += sortie.pickup(i).type() == PickupType.OVERDRIVE ? 1 : 0;
        }
        return count;
    }

    /** The frigate fit with a Micro-missile Pod on the right wing as well. */
    private static Sortie homingSortie(Content content) {
        Difficulty difficulty = Difficulty.MEDIUM;
        Loadout starter = SimSpecs.starterLoadout(content, difficulty);
        Loadout loadout = SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                        new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "micro-missile-pod", 2)),
                content.shields().models().getFirst().name(),
                content.armour().plating().getFirst().name(),
                0,
                difficulty);
        return new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, KEY, difficulty),
                SimSpecs.rules(content, KEY, difficulty),
                starter.plating().maxArmour());
    }

    private static LevelScript.RadioCue cueAt(LevelScript level, double t) {
        return level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME && cue.t() == t)
                .findFirst()
                .orElseThrow();
    }

    private static int secretCue(LevelScript level, String secret) {
        for (int i = 0; i < level.radio().size(); i++) {
            LevelScript.RadioCue cue = level.radio().get(i);
            if (cue.trigger() == LevelScript.CueTrigger.SECRET && cue.subject().equals(secret)) {
                return i;
            }
        }
        throw new AssertionError("no line for the secret " + secret);
    }

    private static WaveSpec pincer(LevelScript level) {
        return level.waves().stream()
                .filter(wave -> wave.t() == 122)
                .findFirst()
                .orElseThrow();
    }
}
