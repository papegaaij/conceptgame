package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part D's air escort (design/allies, evacuation shuttle; design/campaign Level 10; user
 * decisions D1, D2, D4 and D5 = a): five shuttles stand on their pads, lift off untouchable to their
 * stations in the band and drift on their lane sway; every enemy bullet and every contact on the
 * player's plane hurts them (a small rammer dies on one and pays); Lifeline Three is untouchable
 * until its scripted loss, which costs nothing; the level fails at once when the four saveable
 * shuttles are lost; each one home pays; they climb out untouchable. Level 10's times are scaled
 * down: the loss at 20 s (glow from 18 s), the climb-out at 40 s, the end at 45 s.
 */
class ShuttleEscortTest {
    private static final double SPEED = 190;
    private static final double END = 45;
    private static final double LIFT_T = 1;
    private static final double LIFT_SECONDS = 6;
    private static final double LOSS_T = 20;
    private static final double GLOW = 2;
    private static final double CLIMB_T = 40;
    private static final double CLIMB_SECONDS = 2;
    private static final int THREE = 2;
    private static final int PAY = 25;
    private static final double HP = 120;

    static final AllySpec SHUTTLE = new AllySpec(
            "evacuation-shuttle", new Hitbox(64, 40), new Hitbox(48, 28), HP, false, 0, 0.5, 0, true, true, 5, 60, 3);

    /** Level 10's stations (x, px below the top edge; period, phase) with the 16 × 4 px sway. */
    static final List<LevelScript.Station> STATIONS = List.of(
            station(240, 165, 9, 0),
            station(168, 215, 11, 0.25),
            station(312, 215, 10, 0.5),
            station(168, 270, 12, 0.75),
            station(312, 270, 8, 0.1));

    private static final List<LevelScript.Pad> PADS =
            List.of(pad(240, 400), pad(176, 440), pad(304, 440), pad(176, 490), pad(304, 490));

    private static LevelScript.Station station(double x, double below, double period, double phase) {
        return new LevelScript.Station(x, PlayField.HEIGHT - below, 16, 4, period, phase);
    }

    private static LevelScript.Pad pad(double x, double below) {
        return new LevelScript.Pad(x, PlayField.HEIGHT - below);
    }

    static LevelScript.Escort escort() {
        return LevelScript.Escort.air(
                SHUTTLE,
                new LevelScript.Air(
                        STATIONS,
                        Optional.of(new LevelScript.Liftoff(LIFT_T, LIFT_SECONDS, PADS)),
                        Optional.of(new LevelScript.Climb(CLIMB_T, CLIMB_SECONDS)),
                        Optional.of(new LevelScript.ScriptedLoss(THREE, LOSS_T, GLOW))),
                PAY);
    }

    /** The radio cues the tests listen for, by index. */
    private static final List<LevelScript.CueTrigger> CUES = List.of(
            LevelScript.CueTrigger.FIRST_ALLY_HIT,
            LevelScript.CueTrigger.FIRST_ALLY_LOST,
            LevelScript.CueTrigger.ALLY_LOST,
            LevelScript.CueTrigger.SCRIPTED_LOSS,
            LevelScript.CueTrigger.MISSION_FAILED,
            LevelScript.CueTrigger.FIRST_DECLOAK,
            LevelScript.CueTrigger.FIRST_LOOP_BACK);

    private static List<LevelScript.RadioCue> radio() {
        List<LevelScript.RadioCue> cues = new ArrayList<>();
        for (LevelScript.CueTrigger trigger : CUES) {
            cues.add(new LevelScript.RadioCue(trigger, 0, "", "Okafor", trigger.name(), false, "neutral"));
        }
        for (int home = 1; home <= 4; home++) {
            cues.add(new LevelScript.RadioCue(
                    LevelScript.CueTrigger.LEVEL_END,
                    0,
                    "",
                    "Okafor",
                    home + " home",
                    false,
                    "neutral",
                    "Okafor",
                    false,
                    home,
                    home));
        }
        return cues;
    }

    static LevelScript level(List<WaveSpec> waves) {
        return new LevelScript(
                10,
                1,
                0,
                List.of(new LevelScript.Section(END, SPEED)),
                waves,
                List.of(),
                List.of(),
                0,
                radio(),
                new LevelScript.Secondary(0.8, 0),
                List.of(),
                List.of(),
                List.of(),
                Optional.of(escort()),
                Optional.empty());
    }

    private static Sortie sortie(List<WaveSpec> waves) {
        return new Sortie(1, TestSpecs.LOADOUT, level(waves), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    /** An event seen while running: when, what, its value and where. */
    record Seen(double seconds, SimEvents.Type type, int value, double x, double y) {}

    private static List<Seen> run(Sortie sortie, double until) {
        List<Seen> seen = new ArrayList<>();
        while (sortie.levelSeconds() < until - 1e-9) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                seen.add(new Seen(sortie.levelSeconds(), events.type(i), events.value(i), events.x(i), events.y(i)));
            }
        }
        return seen;
    }

    private static long count(List<Seen> seen, SimEvents.Type type) {
        return seen.stream().filter(s -> s.type() == type).count();
    }

    /** How often the cue {@code trigger} started. */
    private static long cued(List<Seen> seen, LevelScript.CueTrigger trigger) {
        return seen.stream()
                .filter(s -> s.type() == SimEvents.Type.RADIO && s.value() == CUES.indexOf(trigger))
                .count();
    }

    private static long levelEnd(List<Seen> seen, int home) {
        return seen.stream()
                .filter(s -> s.type() == SimEvents.Type.RADIO && s.value() == CUES.size() + home - 1)
                .count();
    }

    private static double hp(Sortie sortie, int k) {
        return sortie.ally(k).hpShare() * HP;
    }

    /** A bullet from the right edge at unit {@code k}'s height, flying left at 600 px/s. */
    private static void fromTheRight(Sortie sortie, int k, double damage) {
        sortie.fireEnemyBullet(PlayField.WIDTH - 4, sortie.ally(k).y(), -600, 0, damage);
    }

    /** A bullet from 40 px below unit {@code k}, flying up at 600 px/s. */
    private static void fromBelow(Sortie sortie, int k, double damage) {
        sortie.fireEnemyBullet(sortie.ally(k).x(), sortie.ally(k).y() - 40, 0, 600, damage);
    }

    @Test
    void theShuttlesHoldTheirStationsWithTheLaneSway() {
        Sortie sortie = sortie(List.of());
        run(sortie, 12);
        assertEquals(5, sortie.allyCount());
        assertEquals(4, sortie.saveableAllies());
        assertEquals(THREE, sortie.scriptedAlly());
        assertTrue(sortie.airEscort());

        for (int step = 0; step < 300; step++) {
            sortie.step(Command.NONE);
            double t = sortie.levelSeconds();
            for (int k = 0; k < 5; k++) {
                Ally ally = sortie.ally(k);
                LevelScript.Station station = STATIONS.get(k);
                assertEquals(Ally.State.FLYING, ally.state());
                assertEquals(station.xAt(t), ally.x(), 1e-9, "unit " + k);
                assertEquals(station.yAt(t), ally.y(), 1e-9, "unit " + k);
                assertEquals(1, ally.lift(0.5), 1e-12);
                assertEquals(k == THREE, ally.untouchable(), "only Lifeline Three before its loss");
                assertEquals(k != THREE, ally.touchable());
            }
        }
        // The sway: a 16 px figure-eight round x, 4 px round y; the bank follows the sideways speed.
        LevelScript.Station one = STATIONS.getFirst();
        double t = sortie.levelSeconds();
        assertEquals(
                one.x() + 16 * Math.sin(2 * Math.PI * t / 9), sortie.ally(0).x(), 1e-6);
        assertEquals(one.y() - 4 * Math.sin(4 * Math.PI * t / 9), sortie.ally(0).y(), 1e-6);
        double velocity = 16 * 2 * Math.PI / 9 * Math.cos(2 * Math.PI * t / 9);
        assertEquals(velocity, sortie.ally(0).bankVelocity(), 0.2);
    }

    @Test
    void theyStandOnTheirPadsAndLiftOffUntouchable() {
        Sortie sortie = sortie(List.of());
        sortie.step(Command.NONE);
        for (int k = 0; k < 5; k++) {
            assertEquals(Ally.State.PAD, sortie.ally(k).state());
            assertTrue(sortie.ally(k).untouchable());
            assertEquals(0, sortie.ally(k).lift(1), 1e-12, "on the ground layer's scale");
        }
        // The pads scroll down with the ground and reach their points at the liftoff.
        double before = sortie.ally(0).y();
        run(sortie, 0.5);
        assertEquals(
                before - (sortie.levelSeconds() - SimStep.SECONDS) * SPEED,
                sortie.ally(0).y(),
                1e-6);
        run(sortie, LIFT_T);
        for (int k = 0; k < 5; k++) {
            assertEquals(PADS.get(k).x(), sortie.ally(k).x(), 1e-6, "unit " + k);
            assertEquals(PADS.get(k).y(), sortie.ally(k).y(), 1e-6, "unit " + k);
        }

        run(sortie, 3);
        double lift = sortie.ally(0).lift(1);
        assertEquals(Ally.State.LIFTING, sortie.ally(0).state());
        assertTrue(lift > 0 && lift < 1, "rising to the air scale: " + lift);
        double armour = hp(sortie, 0);
        fromBelow(sortie, 0, 50);
        int bullets = sortie.bulletCount();
        run(sortie, 3.1);
        assertEquals(armour, hp(sortie, 0), 1e-9, "fire passes through a lifting shuttle");
        assertEquals(bullets, sortie.bulletCount(), "and is not spent");

        run(sortie, LIFT_T + LIFT_SECONDS + 0.05);
        for (int k = 0; k < 5; k++) {
            Ally ally = sortie.ally(k);
            assertEquals(Ally.State.FLYING, ally.state(), "unit " + k);
            assertEquals(STATIONS.get(k).xAt(sortie.levelSeconds()), ally.x(), 1e-9);
            assertEquals(1, ally.lift(1), 1e-12);
        }
    }

    @Test
    void aBulletHurtsTheFirstShuttleItTouchesAndIsSpent() {
        Sortie sortie = sortie(List.of());
        run(sortie, 10);
        fromBelow(sortie, 3, 6);
        List<Seen> seen = run(sortie, 10.5);

        assertEquals(HP - 6, hp(sortie, 3), 1e-9, "Lifeline Four takes the bullet's damage");
        assertEquals(HP, hp(sortie, 1), 1e-9, "Lifeline Two above it is not hit: the bullet is spent");
        assertEquals(0, sortie.bulletCount());
        assertEquals(1, count(seen, SimEvents.Type.ALLY_HIT));
        assertEquals(1, cued(seen, LevelScript.CueTrigger.FIRST_ALLY_HIT));
        assertTrue(sortie.ally(3).ticksSinceHit() < SimStep.ticks(0.5), "its hit flash's clock");
    }

    @Test
    void lifelineThreeLetsFireAndContactPassUntilItsLoss() {
        // A bullet across the band at Lifeline Three's height: through Three, into Two.
        Sortie sortie = sortie(List.of());
        run(sortie, 10);
        fromTheRight(sortie, THREE, 6);
        run(sortie, 11);
        assertEquals(HP, hp(sortie, THREE), 1e-9, "no damage before t=118 (here 20)");
        assertEquals(HP - 6, hp(sortie, 1), 1e-9, "no free shield: it hits Lifeline Two beyond");

        // A medium body down the right lane: through Three (not hurt, not stopped), into Five.
        WaveSpec brute =
                TestSpecs.wave(9, WaveSpec.Formation.SINGLE, BRUTE, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.RIGHT);
        Sortie rammed = sortie(List.of(brute));
        run(rammed, 16);
        assertEquals(HP, hp(rammed, THREE), 1e-9);
        assertEquals(HP - 15, hp(rammed, 4), 1e-9, "Five takes its contact, once");
    }

    /** A medium air body that is not destroyed by ramming: contact 15, flying straight down. */
    private static final EnemySpec BRUTE = new EnemySpec(
            "brute",
            1000,
            new Hitbox(40, 40),
            Layer.AIR,
            15,
            false,
            10,
            100,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false);

    @Test
    void aSmallRammerDiesOnAShuttleAndPaysItsBounty() {
        WaveSpec skitter = TestSpecs.wave(
                9, WaveSpec.Formation.SINGLE, TestSpecs.SKITTER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = sortie(List.of(skitter));
        List<Seen> seen = run(sortie, 14);

        assertEquals(HP - 6, hp(sortie, 0), 1e-9, "Lifeline One takes the Skitter's tiny contact");
        assertEquals(1, sortie.kills(), "the Skitter died on it");
        assertEquals(TestSpecs.SKITTER.bounty(), sortie.credits(), "and paid as when it rams the ship");
        assertEquals(1, count(seen, SimEvents.Type.ENEMY_DESTROYED));
        assertEquals(0, sortie.ship().defences().armourLost(), "it never reached the ship below");
    }

    @Test
    void aMediumBodyHurtsOncePerContact() {
        WaveSpec brute =
                TestSpecs.wave(9, WaveSpec.Formation.SINGLE, BRUTE, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = sortie(List.of(brute));
        List<Seen> seen = run(sortie, 16);

        assertEquals(HP - 15, hp(sortie, 0), 1e-9);
        assertEquals(0, sortie.kills());
        assertEquals(
                1,
                seen.stream()
                        .filter(s -> s.type() == SimEvents.Type.ALLY_HIT && s.value() == 0)
                        .count());
    }

    @Test
    void theScriptedLossGlowsThenTakesLifelineThreeAtItsTimeAndCostsNothing() {
        Sortie sortie = sortie(List.of());
        List<Seen> seen = run(sortie, LOSS_T + 1);

        List<Seen> glow =
                seen.stream().filter(s -> s.type() == SimEvents.Type.LOSS_GLOW).toList();
        List<Seen> loss = seen.stream()
                .filter(s -> s.type() == SimEvents.Type.SCRIPTED_LOSS)
                .toList();
        assertEquals(1, glow.size());
        assertEquals(1, loss.size());
        assertEquals(LOSS_T - GLOW, glow.getFirst().seconds(), 1e-9);
        assertEquals(THREE, glow.getFirst().value());
        assertEquals(LOSS_T, loss.getFirst().seconds(), 1e-9);
        assertEquals(THREE, loss.getFirst().value());
        assertEquals(1, cued(seen, LevelScript.CueTrigger.SCRIPTED_LOSS));

        Ally three = sortie.ally(THREE);
        assertEquals(Ally.State.WRECK, three.state());
        assertTrue(three.lost() && three.untouchable() && !three.touchable());
        assertEquals(0, three.hpShare(), 1e-12, "its bar goes dark");
        assertEquals(SimStep.ticks(1), three.ticksSinceLost(), "its glide's clock");
        double x = three.x();
        run(sortie, LOSS_T + 2);
        assertEquals(x, sortie.ally(THREE).x(), 1e-12, "the sim keeps it where it was lost");

        assertEquals(0, count(seen, SimEvents.Type.ALLY_LOST), "no loss event");
        assertEquals(0, cued(seen, LevelScript.CueTrigger.FIRST_ALLY_LOST), "no loss cue");
        assertEquals(0, cued(seen, LevelScript.CueTrigger.ALLY_LOST));
        assertEquals(-1, sortie.firstAllyLost());
        assertEquals(-1, sortie.lastAllyLost());
        assertEquals(4, sortie.saveableAlliesAlive(), "it does not count");
        assertEquals(4, sortie.alliesAlive());
        assertFalse(sortie.primaryFailed());

        run(sortie, END + 0.1);
        assertTrue(sortie.complete());
        assertEquals(
                new LevelResult.Escort("evacuation-shuttle", 4, 4, 4 * PAY),
                sortie.result().escort());
    }

    @Test
    void losingTheFourSaveableShuttlesFailsAtOnceEvenBeforeTheScriptedLoss() {
        Sortie sortie = sortie(List.of());
        run(sortie, 10);
        List<Seen> seen = new ArrayList<>();
        int[] order = {0, 1, 3, 4};
        for (int i = 0; i < order.length; i++) {
            fromBelow(sortie, order[i], 500);
            seen.addAll(run(sortie, sortie.levelSeconds() + 0.5));
            assertEquals(Ally.State.WRECK, sortie.ally(order[i]).state());
            assertEquals(order[i], sortie.lastAllyLost(), "the ally-lost line names it");
            assertEquals(i < 3, !sortie.primaryFailed(), "after " + (i + 1) + " lost");
        }

        assertEquals(0, sortie.saveableAlliesAlive());
        assertTrue(sortie.ally(THREE).alive(), "Lifeline Three is still flying");
        assertEquals(1, count(seen, SimEvents.Type.PRIMARY_FAILED));
        assertEquals(1, cued(seen, LevelScript.CueTrigger.MISSION_FAILED));
        assertEquals(1, cued(seen, LevelScript.CueTrigger.FIRST_ALLY_LOST));
        assertEquals(4, cued(seen, LevelScript.CueTrigger.ALLY_LOST), "on every loss, the first too");
        assertEquals(0, sortie.firstAllyLost());
        // The first loss's cues: first-ally-lost's before ally-lost's.
        List<Integer> radio = seen.stream()
                .filter(s -> s.type() == SimEvents.Type.RADIO)
                .map(Seen::value)
                .toList();
        assertTrue(radio.indexOf(CUES.indexOf(LevelScript.CueTrigger.FIRST_ALLY_LOST))
                < radio.indexOf(CUES.indexOf(LevelScript.CueTrigger.ALLY_LOST)));

        List<Seen> after = run(sortie, LOSS_T + 1);
        assertEquals(0, count(after, SimEvents.Type.SCRIPTED_LOSS), "the failed level has no lance");
    }

    @Test
    void eachSaveableShuttleHomePaysAndTheLevelEndLineCountsThem() {
        Sortie sortie = sortie(List.of());
        run(sortie, 10);
        fromBelow(sortie, 1, 500);
        List<Seen> seen = run(sortie, END + 0.1);

        assertTrue(sortie.complete());
        assertEquals(
                new LevelResult.Escort("evacuation-shuttle", 3, 4, 3 * PAY),
                sortie.result().escort());
        assertEquals(1, levelEnd(seen, 3));
        assertEquals(0, levelEnd(seen, 4));
        assertEquals(0, levelEnd(seen, 2));
    }

    @Test
    void theyClimbOutUntouchableAndAreHome() {
        Sortie sortie = sortie(List.of());
        run(sortie, CLIMB_T + 1);
        for (int k = 0; k < 5; k++) {
            Ally ally = sortie.ally(k);
            if (k == THREE) {
                assertEquals(Ally.State.WRECK, ally.state());
                continue;
            }
            assertEquals(Ally.State.CLIMBING, ally.state(), "unit " + k);
            assertTrue(ally.untouchable());
        }
        double armour = hp(sortie, 0);
        fromBelow(sortie, 0, 50);
        run(sortie, CLIMB_T + 1.05);
        assertEquals(armour, hp(sortie, 0), 1e-9, "fire passes through a climbing shuttle");

        run(sortie, CLIMB_T + CLIMB_SECONDS + 0.05);
        for (int k : new int[] {0, 1, 3, 4}) {
            Ally ally = sortie.ally(k);
            assertEquals(Ally.State.HOME, ally.state());
            assertTrue(ally.y() - 20 > PlayField.HEIGHT, "off the top edge");
            assertTrue(ally.alive());
        }
    }

    @Test
    void theFirstDecloakAndTheFirstLoopBackStartTheirCuesOnce() {
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT,
                level(List.of(PartDSpecs.ambush(9, PartDSpecs.wraith(), 2), PartDSpecs.swarm(9, 12, 2))),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        List<Seen> seen = run(sortie, 30);

        assertEquals(2, count(seen, SimEvents.Type.DECLOAK));
        assertTrue(count(seen, SimEvents.Type.LOOP_BACK) >= 1);
        assertEquals(1, cued(seen, LevelScript.CueTrigger.FIRST_DECLOAK));
        assertEquals(1, cued(seen, LevelScript.CueTrigger.FIRST_LOOP_BACK));
        double decloak = seen.stream()
                .filter(s -> s.type() == SimEvents.Type.DECLOAK)
                .findFirst()
                .orElseThrow()
                .seconds();
        double cue = seen.stream()
                .filter(s -> s.type() == SimEvents.Type.RADIO
                        && s.value() == CUES.indexOf(LevelScript.CueTrigger.FIRST_DECLOAK))
                .findFirst()
                .orElseThrow()
                .seconds();
        assertEquals(decloak, cue, 1e-9, "in the step of the first decloak");
    }

    private static List<WaveSpec> busy() {
        return List.of(
                TestSpecs.wave(
                        8, WaveSpec.Formation.SINGLE, TestSpecs.SKITTER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE),
                TestSpecs.wave(9, WaveSpec.Formation.SINGLE, BRUTE, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.LEFT),
                TestSpecs.wave(
                        10,
                        WaveSpec.Formation.LINE_ABREAST,
                        TestSpecs.NEEDLER,
                        4,
                        WaveSpec.Entry.FRONT,
                        WaveSpec.Edge.NONE),
                PartDSpecs.ambush(12, PartDSpecs.wraith(), 3),
                PartDSpecs.swarm(14, 20, 2));
    }

    @Test
    void theSameInputFliesTheSameEscort() {
        Sortie a = sortie(busy());
        Sortie b = sortie(busy());
        for (int step = 0; step < SimStep.ticks(END); step++) {
            int commands = SortieTest.Pilot.commands(step);
            a.step(commands);
            b.step(commands);
        }
        assertEquals(a.stateHash(), b.stateHash());
    }

    @Test
    void theAirEscortDoesNotAllocate() {
        long allocated = Allocations.least(() -> sortie(busy()), sortie -> {
            for (int step = 0; step < SimStep.ticks(END); step++) {
                sortie.step(SortieTest.Pilot.commands(step));
            }
        });

        assertEquals(0, allocated);
    }
}
