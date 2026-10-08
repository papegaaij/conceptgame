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
import vanguard.sim.Ally;
import vanguard.sim.Armament;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WaveSpec;
import vanguard.sim.WingmanSpec;

/**
 * Level 10 as the game runs it (design/campaign/act-2-homefront/level-10-evacuation-corridor): its
 * credit budget and the README's totals, the Wraith ambushes and the Mote Swarms per difficulty
 * (their sizes and loop-backs), the air escort of five shuttles on real data (the stations, the
 * liftoff, the climb-out, Lifeline Three's scripted loss and the fail rule), the ferry hatch secret,
 * the pickups, the radio script, and whole runs in which the autopilot flies the level to its end
 * with Rook on every difficulty, keeping at least three of the four saveable shuttles on medium
 * (user decision D2 = a of M5 part D).
 */
class Level10Test {
    static final String LEVEL = "act-2-homefront/level-10-evacuation-corridor";
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    /**
     * The fit the balance plan expects at Level 10 (design/player/balance-plan.yaml, user decision D7 =
     * a of M5 part D): Level 09's fit with the Tail Gun bought for the rear, and Rook's Autocannon
     * (owned since Level 08) refitted in place of his Mortar; the two Airstrike charges. PacingTest
     * flies it too.
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
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "autocannon-pod", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 1)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(1).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        content, "autocannon", 1, side, content.wingmen().rook().armour()))
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
        int crates = content.level(LEVEL).secrets().stream()
                .mapToInt(LevelData.Secret::crate)
                .sum();
        LevelScript.Escort escort = level.escort().orElseThrow();
        int escortPay = escort.saveable() * escort.credits();
        int total = kills + crates + escortPay;

        double budget = content.economy().budget().of(level.number());
        assertEquals(1_287, Math.round(budget), "the typical haul's target, 700 × 1.07⁹");
        assertEquals(169 * 5 + 47 * 12 + 11 * 15 + 80 * 2 + 11 * 30, kills, "Skitter, Needler, Stinger, Mote, Wraith");
        assertEquals(0, level.groundUnits().size(), "no ground enemies");
        assertEquals(100, crates, "the ferry cache in Act 1 terms (pays 160)");
        assertEquals(4 * 25, escortPay, "four saveable shuttles home × 25 in Act 1 terms (pays 40 each)");
        assertEquals(0, level.secondary().credits(), "no secondary (D5 = a)");
        System.out.printf(
                "Level 10 budget (Act 1 terms): kills %d, crate %d, escort %d: %d (budget %.0f)%n",
                kills, crates, escortPay, total, budget);
        assertEquals(2_264, total, "before the act factor and bounty_scale (TypicalHaulTest checks the paid run)");
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

        assertEquals(Map.of("mote-swarm", 80, "needler", 47, "skitter", 169, "stinger", 11, "wraith", 11), waves);
        assertEquals(Map.of(WaveSpec.Entry.FRONT, 262, WaveSpec.Entry.REAR, 52, WaveSpec.Entry.SIDES, 4), from);
        assertEquals(
                List.of(20.0, 70.0, 130.0, 175.0, 200.0),
                level.sections().stream().map(LevelScript.Section::end).toList());
        assertTrue(level.holds().isEmpty(), "no holds: script time is real time");
        assertTrue(level.groups().isEmpty());
        Sortie sortie = sortie(content, 1, Difficulty.MEDIUM);
        assertEquals(318, sortie.enemyTotal());
    }

    /**
     * The Wraith ambushes (as the README's difficulty notes: easy one Wraith fewer at t≈106 and 157;
     * hard the extra pair at t=170, 7-shot bursts and 3.0 s holds; the counts authored, so the
     * formation lever does not make the three-Wraith ambushes four on hard or two in easy's finale) and the Mote Swarms
     * (the formation lever: 16 for 20; hard: every looping swarm loops back twice).
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theDifficultiesChangeTheAmbushesAndTheSwarms(Difficulty difficulty) {
        LevelScript level = SimSpecs.level(content, LEVEL, difficulty);
        List<WaveSpec> ambushes = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.REAR_AMBUSH)
                .toList();
        assertEquals(
                switch (difficulty) {
                    case EASY -> List.of(1, 2, 1, 2, 3);
                    case MEDIUM -> List.of(1, 2, 2, 3, 3);
                    case HARD -> List.of(1, 2, 2, 3, 2, 3);
                },
                ambushes.stream().map(WaveSpec::count).toList());
        for (WaveSpec ambush : ambushes) {
            assertEquals(WaveSpec.Entry.REAR, ambush.entry());
            assertEquals("wraith", ambush.enemy().slug());
        }
        List<WaveSpec> swarms = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.SWARM)
                .toList();
        assertEquals(
                switch (difficulty) {
                    case EASY -> List.of(8, 5, 11, 13, 6, 6, 6, 8);
                    case MEDIUM -> List.of(10, 6, 14, 16, 8, 8, 8, 10);
                    case HARD -> List.of(12, 7, 17, 19, 10, 10, 10, 12);
                },
                swarms.stream().map(WaveSpec::count).toList());
        assertEquals(
                List.of(0, 0, 1, 1, 1, 0, 0, 1).stream()
                        .map(loops -> difficulty == Difficulty.HARD ? 2 * loops : loops)
                        .toList(),
                swarms.stream()
                        .map(wave ->
                                wave.loopBack().map(WaveSpec.LoopBack::count).orElse(0))
                        .toList(),
                "the looping swarms loop back once, twice on hard");
        assertEquals(
                difficulty == Difficulty.HARD ? 7 : 5,
                ambushes.getFirst().enemy().gun().orElseThrow().burst(),
                "the stat block's 5-shot burst (the pattern lever leaves aimed bursts alone), hard's authored 7");
        int armour =
                switch (difficulty) {
                    case EASY -> 180;
                    case MEDIUM -> 120;
                    case HARD -> 90;
                };
        assertEquals(armour, level.escort().orElseThrow().ally().hp(), 1e-9, "the shuttles' armour");
    }

    /**
     * The air escort on real data: five shuttles, Lifeline Three the scripted loss (so four
     * saveable), the liftoff from the pads at t=1 over 6 s, the climb-out at t=196, the pay of 25
     * per saveable shuttle home; no road, no secondary; the rear trait required.
     */
    @Test
    void theShuttlesAreAnAirEscortWithLifelineThreeTheScriptedLoss() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.Escort escort = level.escort().orElseThrow();
        LevelScript.Air air = escort.air().orElseThrow();
        assertEquals("evacuation-shuttle", escort.ally().slug());
        assertEquals(5, escort.units());
        assertEquals(4, escort.saveable());
        assertEquals(2, escort.scriptedUnit(), "Lifeline Three, from 0");
        assertEquals(25, escort.credits());
        assertEquals(Optional.of(new LevelScript.ScriptedLoss(2, 118, 2)), air.scriptedLoss());
        assertEquals(Optional.of(new LevelScript.Climb(196, 2)), air.climb());
        LevelScript.Liftoff liftoff = air.liftoff().orElseThrow();
        assertEquals(1, liftoff.t());
        assertEquals(6, liftoff.seconds());
        assertEquals(
                new LevelScript.Pad(240, PlayField.HEIGHT - 400), liftoff.pads().getFirst());
        assertEquals(
                new LevelScript.Station(240, PlayField.HEIGHT - 165, 16, 4, 9, 0),
                air.stations().getFirst());
        assertTrue(level.road().isEmpty(), "no road");
        assertEquals(LevelScript.Secondary.NONE, level.secondary());
        LevelData data = content.level(LEVEL);
        assertEquals(List.of("rear"), data.threatProfile().requiredTraits());
        assertEquals(
                Optional.of(new LevelData.Music.Duck(LevelData.Music.DuckOn.SCRIPTED_LOSS, -6, 3)),
                data.music().duck());
        assertEquals(
                List.of(new LevelData.Music.AmbienceChange(4, "earth-ocean", 4)),
                data.music().ambienceChangeList());
        assertFalse(data.music().full(2), "the base stem from section 2");
        assertTrue(data.music().full(3), "the full mix from section 3");
    }

    @Test
    void theFerryHatchHidesTheCacheAndTheNeedlersCarryThePickups() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec hatch = level.groundObjects().stream()
                .filter(o -> o.secret().equals("ferry cache"))
                .findFirst()
                .orElseThrow();
        assertFalse(hatch.hardened(), "any weapon opens it");
        assertEquals(50, hatch.hp(), 1e-9, "about a second of the plan's forward guns lined up on it");
        assertEquals(100, hatch.crateCredits());
        assertEquals(0, hatch.bounty());
        assertEquals(Optional.empty(), hatch.drop());
        assertEquals(
                List.of(new WaveSpec.Carried(PickupType.OVERDRIVE, WaveSpec.Carried.LAST)),
                wave(level, 111.5).carried(),
                "the overdrive on the V-wing after the t≈106 ambush");
        assertEquals(
                List.of(new WaveSpec.Carried(PickupType.ARMOUR_PATCH, WaveSpec.Carried.LAST)),
                wave(level, 154.5).carried(),
                "the armour patch on the V-wing before the t≈157 ambush");
        for (WaveSpec wave : level.waves()) {
            if (wave.enemy().slug().equals("wraith")) {
                assertTrue(wave.carried().isEmpty(), "a Wraith dies near the bottom edge: it carries nothing");
            }
        }
    }

    /**
     * The ferry secret takes deliberate fire and its crate can be caught (the round 32 capture: the
     * hatch, 8 HP then, opened at the top edge by passing fire, and its crate, drifting at 40 px/s,
     * ran out its 6 s before it reached the ship). A ship holding its start line under the hatch with
     * the plan's guns: firing from the moment the hatch enters, it opens it no higher than a third of
     * the screen down; firing once it is a sixth or a third of the way down, lower; each time the
     * crate then drifts to the ship, which collects it without moving off its line.
     */
    @Test
    void theFerryHatchTakesAimedFireAndItsCrateReachesAShipOnItsStartLine() {
        for (double from : new double[] {0, 90, 180}) {
            double[] run = hatchRun(content, planLoadout(content, Difficulty.MEDIUM), from);
            double opened = run[0];
            double collectedAfter = run[1];
            System.out.printf(
                    "Level 10 ferry hatch, firing from %.0f px below the top edge: opened %.0f px down, the crate"
                            + " collected %.2f s later%n",
                    from, opened, collectedAfter);
            assertTrue(opened >= Math.max(PlayField.HEIGHT / 3.0, from), "opened at " + opened);
            assertCaught(collectedAfter);
        }
    }

    /**
     * The real case (the 2026-10-08 capture: the plan's fit with Rook, lined up under the hatch from
     * t≈147 with the fire held, opened it at t≈150.8, about 150 px down, and the crate, with 6 s to
     * live, ran out at y≈370 above the start line): a pilot holding fire has his shots in the air before
     * the hatch enters, so it opens far higher than when the fire starts at its entry (above, 215 px).
     * The plan's fit with Rook (Autocannon level 1, the plan's, and level 2, the capture's) and every
     * fitted gun at its top level with Rook's at his: lined up under the hatch from 10 s before it
     * enters, firing throughout, the crate reaches the ship on its start line before it expires.
     */
    @Test
    void theFerryCrateReachesTheStartLineWhenTheFireIsHeldBeforeTheHatchEnters() {
        Loadout plan = planLoadout(content, Difficulty.MEDIUM);
        double rookArmour = content.wingmen().rook().armour();
        Map<String, Loadout> fits = new java.util.LinkedHashMap<>();
        fits.put("the plan's fit, Rook's Autocannon 1", plan);
        fits.put(
                "the plan's fit, Rook's Autocannon 2",
                plan.withWingman(SimSpecs.wingman(content, "autocannon", 2, WingmanSpec.Side.LEFT, rookArmour)));
        fits.put("every gun at its top level", topLevels(rookArmour));
        for (Map.Entry<String, Loadout> fit : fits.entrySet()) {
            double[] run = hatchRun(content, fit.getValue(), HOLD_FIRE);
            System.out.printf(
                    "Level 10 ferry hatch, %s, fire held from t-10: opened %.0f px down, the crate collected %.2f s"
                            + " later%n",
                    fit.getKey(), run[0], run[1]);
            assertTrue(run[0] > -20 && run[0] < PlayField.HEIGHT / 3.0, fit.getKey() + ": opened at " + run[0]);
            assertCaught(run[1]);
        }
    }

    /**
     * Wherever the hatch opens on the screen, its crate reaches the ship on its start line under it
     * before it expires: dropped at the hatch's centre as it enters (half its height above the top
     * edge), it drifts to the ship's collection radius within the level's crate life (12 s, the other
     * pickups' 6 s would carry it 240 px).
     */
    @Test
    void aCrateDroppedAtTheTopEdgeReachesTheStartLineWithinItsLife() {
        LevelScript.GroundObjectSpec hatch = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM).groundObjects().stream()
                .filter(o -> o.secret().equals("ferry cache"))
                .findFirst()
                .orElseThrow();
        vanguard.sim.PickupRules pickups =
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).pickups();
        double drift = PlayField.HEIGHT + hatch.size().height() / 2 - START_LINE - pickups.collectionRadius();
        double seconds = drift / pickups.driftSpeed();
        System.out.printf(
                "Level 10 ferry crate from the top edge: %.0f px to the start line, %.2f s of its %.0f s%n",
                drift, seconds, pickups.crateSeconds());
        assertEquals(12, pickups.crateSeconds(), 1e-9);
        assertEquals(6, pickups.seconds(), 1e-9, "the other pickups keep their life");
        assertTrue(seconds < pickups.crateSeconds() - 1, "with a second to spare: " + seconds);
    }

    private void assertCaught(double collectedAfter) {
        assertTrue(collectedAfter >= 0, "the crate reaches the ship on its start line");
        assertTrue(
                collectedAfter
                        < SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM)
                                .pickups()
                                .crateSeconds(),
                "within its life: " + collectedAfter);
    }

    /** The plan's slots with every gun at its top level and Rook's Autocannon at its top level. */
    private Loadout topLevels(double rookArmour) {
        String[][] guns = {
            {"FRONT", SimSpecs.PULSE_CANNON},
            {"LEFT_WING", "bomb-rack"},
            {"RIGHT_WING", "autocannon-pod"},
            {"REAR", "tail-gun"}
        };
        List<SimSpecs.FittedWeapon> fitted = new java.util.ArrayList<>();
        for (String[] gun : guns) {
            fitted.add(new SimSpecs.FittedWeapon(
                    Armament.Slot.valueOf(gun[0]),
                    gun[1],
                    content.weapon(gun[1]).levels().size()));
        }
        int rook = content.weapon(content.wingmen().guns().gun("autocannon").base())
                .levels()
                .size();
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        fitted,
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(1).name(),
                        0,
                        Difficulty.MEDIUM)
                .withWingman(SimSpecs.wingman(content, "autocannon", rook, WingmanSpec.Side.LEFT, rookArmour));
    }

    /** The ship's start line, px above the bottom edge (Sortie: where it holds after the launch). */
    private static final double START_LINE = 96;

    /** {@link #hatchRun}: the fire held from when the ship lines up, before the hatch enters. */
    private static final double HOLD_FIRE = -1;

    /**
     * A ship with {@code loadout} on its start line under the ferry hatch (x 322) from 10 s before it
     * enters, the ship invulnerable (the waves fly as in play), firing from when the hatch is {@code
     * from} px below the top edge, or throughout from then with {@link #HOLD_FIRE}: how far down the
     * hatch opened and the seconds until the crate was collected (-1 when it never was).
     */
    private static double[] hatchRun(Content content, Loadout loadout, double from) {
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(content, LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                loadout.plating().maxArmour());
        LevelScript.GroundObjectSpec spec = sortie.script().groundObjects().stream()
                .filter(o -> o.secret().equals("ferry cache"))
                .findFirst()
                .orElseThrow();
        double opened = -1;
        double openedAt = -1;
        double collectedAt = -1;
        for (int step = 0; step < SimStep.ticks(spec.t() + 16) && collectedAt < 0; step++) {
            int commands = vanguard.sim.Command.NONE;
            if (sortie.levelSeconds() > spec.t() - 10) {
                double dx = spec.x() - sortie.ship().x();
                double dy = START_LINE - sortie.ship().y();
                commands |= dx < -2 ? vanguard.sim.Command.LEFT.bit() : dx > 2 ? vanguard.sim.Command.RIGHT.bit() : 0;
                commands |= dy < -2 ? vanguard.sim.Command.DOWN.bit() : dy > 2 ? vanguard.sim.Command.UP.bit() : 0;
                if (from == HOLD_FIRE) {
                    commands |= vanguard.sim.Command.FIRE.bit();
                }
            }
            for (int i = 0; i < sortie.groundObjectCount(); i++) {
                vanguard.sim.GroundObject hatch = sortie.groundObject(i);
                if (hatch.spec() == spec && from >= 0 && PlayField.HEIGHT - hatch.renderY(1) >= from) {
                    commands |= vanguard.sim.Command.FIRE.bit();
                }
            }
            sortie.step(commands);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.SECRET_FOUND) {
                    opened = PlayField.HEIGHT - events.y(i);
                    openedAt = sortie.levelSeconds();
                } else if (events.type(i) == SimEvents.Type.PICKUP_COLLECTED
                        && events.value(i) == PickupType.HIDDEN_CRATE.ordinal()) {
                    collectedAt = sortie.levelSeconds();
                }
            }
        }
        return new double[] {opened, collectedAt < 0 ? -1 : collectedAt - openedAt};
    }

    /**
     * The radio script: the timed lines in time order (README Radio chatter, retimed to the 1 s rule;
     * RadioTimelineTest plays them), Rook's lines only while he flies, the part D events, the four
     * level-end lines by the count home and the failed screen's line; the Lifeline speakers cast in
     * round 32.
     */
    @Test
    void theRadioScriptHasItsEventsAndFourLevelEndLines() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);
        List<LevelScript.RadioCue> timed = level.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .toList();
        assertEquals(
                List.of(1.0, 12.0, 24.0, 32.0, 116.0, 131.5, 139.0, 180.0, 188.5, 196.0),
                timed.stream().map(LevelScript.RadioCue::t).toList());
        List<LevelData.RadioCue> cues = content.level(LEVEL).radio();
        for (LevelData.RadioCue cue : cues) {
            if (cue.speaker().equals("Rook")) {
                assertEquals(Optional.of("escort"), cue.requires(), cue.line());
            }
        }
        for (LevelData.CueEvent event : List.of(
                LevelData.CueEvent.FIRST_DECLOAK,
                LevelData.CueEvent.FIRST_LOOP_BACK,
                LevelData.CueEvent.FIRST_ALLY_HIT,
                LevelData.CueEvent.SCRIPTED_LOSS,
                LevelData.CueEvent.ALLY_LOST,
                LevelData.CueEvent.MISSION_FAILED)) {
            assertEquals(
                    1,
                    cues.stream()
                            .filter(cue -> cue.event().orElse(null) == event)
                            .count(),
                    event.name());
        }
        assertEquals(
                List.of(4, 3, 2, 1),
                level.radio().stream()
                        .filter(cue -> cue.trigger() == LevelScript.CueTrigger.LEVEL_END)
                        .map(cue -> {
                            assertEquals(cue.alliesMin(), cue.alliesMax());
                            return cue.alliesMin();
                        })
                        .toList(),
                "one level-end line per count of saveable shuttles home");
        assertEquals(Optional.of("lifeline"), content.voices().voiceOf("Lifeline One"));
        assertEquals(Optional.of("lifeline"), content.voices().voiceOf("Lifeline"));
        assertEquals(Optional.of("lifeline-three"), content.voices().voiceOf("Lifeline Three"));
    }

    /**
     * The autopilot flies the plan's fit (the Tail Gun and Rook's Autocannon) to the level's end with
     * Rook on every difficulty: Lifeline Three is lost to the lance at t=118 and nothing else (no
     * ally-lost event for it, no pay, never the fail), and on medium at least three of the four
     * saveable shuttles get home (D2 = a; re-measured 2026-10-08 with the Wraiths' bursts straight up
     * through the band). Printed: the grade, the shuttles home and the Airstrike charges used.
     */
    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelToItsEndWithRookAndTheShuttles(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        Run run = watch(sortie);
        LevelResult result = sortie.result();
        System.out.printf(
                "Level 10 %s: attempt %d, real %.1f s, kills %d/%d, credits %d (%s), secrets %d, shuttles home %d/4 (lost %s),"
                        + " scripted loss at t=%.2f, decloaks %d, loop-backs %d, Airstrike charges used %d, armour lost %.0f,"
                        + " Rook %s, grade %s, bonuses %s%n",
                difficulty,
                sortie.attempt(),
                sortie.realSeconds(),
                result.kills(),
                result.enemies(),
                result.credits().total(),
                result.credits(),
                run.secrets,
                result.escort().home(),
                run.lost,
                run.scriptedLossAt,
                run.decloaks,
                run.loopBacks,
                sortie.special().used(),
                result.armourDamage(),
                sortie.wingman()
                        .map(rook -> rook.ejected() ? "ejected" : "home")
                        .orElse("none"),
                result.grade().letter(),
                result.bonuses());
        assertTrue(sortie.complete());
        assertFalse(sortie.primaryFailed());
        assertEquals(118, run.scriptedLossAt, 1.5 * SimStep.SECONDS, "the lance at t=118");
        assertEquals(1, run.scriptedLosses);
        assertFalse(run.lost.contains("2"), "Lifeline Three raises no ally-lost event");
        assertEquals(Ally.State.WRECK, sortie.ally(2).state(), "Lifeline Three is lost");
        assertEquals(
                sortie.saveableAlliesAlive(),
                result.escort().home(),
                "the saveable shuttles alive at the climb are home");
        assertEquals(4, result.escort().units(), "of four saveable");
        assertEquals(
                Math.round(result.escort().home()
                        * 40
                        * content.difficulty().creditIncome().of(difficulty)),
                result.escort().credits(),
                "40 a shuttle home (25 × 1.6), × the difficulty's credit income");
        assertTrue(run.decloaks > 0 && run.loopBacks > 0);
        if (difficulty == Difficulty.MEDIUM) {
            assertTrue(
                    result.escort().home() >= 3,
                    "at least 3 of 4 home on medium: " + result.escort().home());
        }
    }

    /**
     * The fail rule on real data (D5 = a): a pilot who never fires and never moves loses the four
     * saveable shuttles on hard before the lance (on medium, since the Skitter snakes and streams keep
     * out of the band (2026-10-08), some last past it); the fourth loss fails the primary at once (the
     * failed screen's line cued), each loss cues the {@code ally-lost} line, and Lifeline Three, which
     * nothing hurts before t=118, is still flying.
     */
    @Test
    void losingTheFourSaveableShuttlesFailsTheLevelAtOnceEvenBeforeTheLance() {
        Sortie sortie = sortie(content, 2185, Difficulty.HARD);
        LevelScript level = sortie.script();
        List<Integer> lost = new java.util.ArrayList<>();
        int allyLostCues = 0;
        int failedCues = 0;
        double failedAt = -1;
        double lastLossAt = -1;
        int steps = 0;
        while (sortie.flying() && !sortie.primaryFailed() && steps++ < MAX_STEPS) {
            sortie.step(vanguard.sim.Command.NONE);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case ALLY_LOST -> {
                        lost.add(events.value(i));
                        lastLossAt = sortie.levelSeconds();
                    }
                    case PRIMARY_FAILED -> failedAt = sortie.levelSeconds();
                    case SCRIPTED_LOSS -> throw new AssertionError("the lance before the fail");
                    case RADIO -> {
                        LevelScript.CueTrigger trigger =
                                level.radio().get(events.value(i)).trigger();
                        if (trigger == LevelScript.CueTrigger.ALLY_LOST) {
                            allyLostCues++;
                        } else if (trigger == LevelScript.CueTrigger.MISSION_FAILED) {
                            failedCues++;
                        }
                    }
                    default -> {}
                }
            }
        }
        System.out.printf("Level 10 passive pilot (hard): shuttles lost %s, failed at t=%.1f%n", lost, failedAt);

        assertTrue(sortie.primaryFailed());
        assertEquals(List.of(0, 1, 3, 4), lost.stream().sorted().toList(), "the four saveable shuttles");
        assertEquals(lastLossAt, failedAt, "at once, in the step of the fourth loss");
        assertTrue(failedAt < 118, "before the lance");
        assertTrue(sortie.ally(2).alive(), "Lifeline Three is still flying");
        assertEquals(0, sortie.saveableAlliesAlive());
        assertEquals(4, allyLostCues, "the ally-lost line on every loss");
        assertEquals(1, failedCues, "the failed screen's line");
    }

    /** What a run saw. */
    private static final class Run {
        int secrets;
        int decloaks;
        int loopBacks;
        int scriptedLosses;
        double scriptedLossAt = -1;
        String lost = "";
    }

    /** Flies the level with the autopilot, recording the events. */
    private static Run watch(Sortie sortie) {
        Run run = new Run();
        int steps = 0;
        int attempt = sortie.attempt();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                run.lost = "";
                run.scriptedLosses = 0;
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case SECRET_FOUND -> run.secrets++;
                    case DECLOAK -> run.decloaks++;
                    case LOOP_BACK -> run.loopBacks++;
                    case SCRIPTED_LOSS -> {
                        run.scriptedLosses++;
                        run.scriptedLossAt = sortie.levelSeconds();
                    }
                    case ALLY_LOST -> run.lost += sortie.lastAllyLost();
                    default -> {}
                }
            }
        }
        return run;
    }

    @Test
    void steppingAWholeLevelDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(content, 2185, Difficulty.HARD), Level10Test::fly);

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

    private static WaveSpec wave(LevelScript level, double t) {
        return level.waves().stream().filter(w -> w.t() == t).findFirst().orElseThrow();
    }
}
