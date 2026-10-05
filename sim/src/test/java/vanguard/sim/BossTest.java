package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The boss layer (design/enemies/bosses, M4 part E) on a small two-headed test boss: the descent,
 * the necks, the phases, the streams, the arena clock, the Boss rush and the boss checkpoint.
 */
class BossTest {
    private static final int LEFT_HEAD = 0;
    private static final int RIGHT_HEAD = 1;
    private static final int CORE = 2;
    /** The boss arrives at 1 s; the arena runs from 2 s to 32 s, then 10 s more to the end. */
    private static final double ARRIVE = 1;

    private static final double ARENA_END = 32;
    private static final double BEND = Math.toRadians(30);

    private static LevelScript.SetPieceSpec boss() {
        var left = new LevelScript.PartSpec(
                "left head", -80, -170, new Hitbox(36, 36), 20, false, 30, Optional.empty(), 0, 1.5);
        var right = new LevelScript.PartSpec(
                "right head", 80, -170, new Hitbox(36, 36), 20, false, 30, Optional.empty(), 0, 1.5);
        var core = new LevelScript.PartSpec("core", 0, 10, new Hitbox(60, 60), 30, true, 110, Optional.empty(), 0, 2);
        var burst = new BossSpec.Attack(
                "head-burst",
                BossSpec.Pattern.AIMED,
                EnemyGun.aimed(1.2, 0, 3, 160, 4, false),
                0.15,
                true,
                1,
                0,
                0,
                0,
                List.of(LEFT_HEAD, RIGHT_HEAD));
        var ring = new BossSpec.Attack(
                "core-ring",
                BossSpec.Pattern.RING,
                EnemyGun.aimed(2, 0, 1, 110, 4, false),
                0,
                false,
                12,
                0,
                0,
                0,
                List.of());
        var spiral = new BossSpec.Attack(
                "core-spiral",
                BossSpec.Pattern.SPIRAL,
                EnemyGun.aimed(0.25, 0, 1, 90, 4, false),
                0,
                false,
                1,
                3,
                Math.toRadians(60),
                3,
                List.of());
        List<Integer> heads = List.of(LEFT_HEAD, RIGHT_HEAD);
        var phases = List.of(
                new BossSpec.Phase(
                        "Two heads",
                        heads,
                        1,
                        List.of(0),
                        false,
                        Optional.of(new BossSpec.Stream(TestSpecs.SKITTER, 6, 10, 0.5, WaveSpec.Edge.ALTERNATING)),
                        List.of(),
                        Double.NaN),
                new BossSpec.Phase(
                        "Last head", heads, 0, List.of(0), false, Optional.empty(), List.of(), Math.toRadians(55)),
                new BossSpec.Phase(
                        "Core",
                        List.of(CORE),
                        0,
                        List.of(1, 2),
                        true,
                        Optional.empty(),
                        List.of(CORE),
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        BossSpec.PHASE_DELAY_SECONDS,
                        Optional.empty(),
                        Optional.empty()));
        var chains = List.of(
                new BossSpec.Chain("left neck", -60, -50, LEFT_HEAD, 5, new Hitbox(22, 22), 0.12, BEND),
                new BossSpec.Chain("right neck", 60, -50, RIGHT_HEAD, 5, new Hitbox(22, 22), 0.12, BEND));
        return new LevelScript.SetPieceSpec(
                "test-frigate",
                new Hitbox(300, 320),
                new Hitbox(200, 120),
                25,
                List.of(left, right, core),
                List.of(),
                Optional.empty(),
                Optional.of(new BossSpec(
                        ARRIVE,
                        240,
                        PlayField.HEIGHT - 110,
                        60,
                        40,
                        6,
                        Layer.AIR,
                        true,
                        "TEST FRIGATE",
                        20,
                        chains,
                        List.of(burst, ring, spiral),
                        phases)));
    }

    private static LevelScript level() {
        return new LevelScript(
                5,
                1,
                0,
                List.of(
                        new LevelScript.Section(2, 130),
                        new LevelScript.Section(ARENA_END, 30, true),
                        new LevelScript.Section(ARENA_END + 10, 130)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(
                        new LevelScript.RadioCue(
                                LevelScript.CueTrigger.BOSS_PHASE,
                                0,
                                "Last head",
                                "Rook",
                                "One left!",
                                false,
                                "neutral"),
                        new LevelScript.RadioCue(
                                LevelScript.CueTrigger.BOSS_DESTROYED,
                                0,
                                "test-frigate",
                                "Okafor",
                                "Down.",
                                false,
                                "neutral")),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(),
                List.of(boss()));
    }

    private static final Rules RUSH_RULES = new Rules(
            120,
            0,
            TestSpecs.RULES.pickups(),
            new ScoringRules(
                    10,
                    10,
                    TestSpecs.SCORING.chain(),
                    1,
                    1,
                    TestSpecs.SCORING.weights(),
                    List.of(new ScoringRules.Bonus(ScoringRules.BonusKind.BOSS_RUSH, "Boss rush", 2000, false, false)),
                    TestSpecs.SCORING.grades()),
            true);

    private static Sortie sortie() {
        return new Sortie(1, TestSpecs.LOADOUT, level(), RUSH_RULES, TestSpecs.FULL_ARMOUR);
    }

    /** Steps until {@code seconds} on the level clock (or the step limit), the ship idle. */
    private static void until(Sortie sortie, double seconds) {
        for (int i = 0; i < 60 * 120 && sortie.levelSeconds() < seconds - 1e-9; i++) {
            sortie.step(0);
        }
    }

    private static void steps(Sortie sortie, int steps) {
        for (int i = 0; i < steps; i++) {
            sortie.step(0);
        }
    }

    @Test
    void itArrivesOnTheClockAndTakesNoDamageUntilItSettles() {
        Sortie sortie = sortie();
        SetPiece boss = sortie.setPiece(0);
        until(sortie, ARRIVE - SimStep.SECONDS);
        assertFalse(boss.present());

        sortie.step(0);
        assertTrue(boss.present());
        assertEquals(1, sortie.events().count(SimEvents.Type.BOSS_ARRIVED));
        assertTrue(boss.renderY(1) > PlayField.HEIGHT, "it enters from above the top edge");
        assertTrue(boss.partShielded(LEFT_HEAD), "its descent takes no damage");

        int settledAt = -1;
        for (int i = 0; i < 600 && settledAt < 0; i++) {
            sortie.step(0);
            if (sortie.events().count(SimEvents.Type.BOSS_SETTLED) > 0) {
                settledAt = i;
            }
        }
        assertTrue(settledAt > 0);
        assertEquals(PlayField.HEIGHT - 110, boss.renderY(1), 1e-9);
        assertFalse(boss.partShielded(LEFT_HEAD));
        assertTrue(boss.partShielded(CORE), "the core is exposed in the last phase only");
        assertEquals(1, boss.barShare(), 1e-9);
    }

    @Test
    void theNecksBendTowardTheShipWithLaggedFollowThrough() {
        Sortie sortie = sortie();
        SetPiece boss = sortie.setPiece(0);
        until(sortie, 8);
        // The ship idles at the bottom centre: the left neck turns toward it (counter-clockwise).
        double first = boss.chainAngle(0, 0);
        double tip = boss.chainAngle(0, 5);
        assertTrue(first > 0 && first <= BEND + 1e-9, "the anchor turns at most its bend: " + first);
        assertTrue(Math.abs(tip - first) < 0.05, "settled, the neck has followed: " + tip);
        assertTrue(boss.partOffsetX(LEFT_HEAD) > -80, "the head swings in toward the ship");

        // Fly far right: the anchor leads, the tip follows later.
        for (int i = 0; i < 6; i++) {
            sortie.step(Command.RIGHT.bit());
        }
        for (int i = 0; i < 20; i++) {
            sortie.step(Command.RIGHT.bit());
            assertTrue(boss.chainAngle(1, 0) >= boss.chainAngle(1, 5) - 1e-9, "the tip lags the anchor");
        }
        assertEquals(10, boss.segmentCount());
    }

    @Test
    void thePhasesEndOnTheHeadsAndTheCoreEndsIt() {
        Sortie sortie = sortie();
        SetPiece boss = sortie.setPiece(0);
        until(sortie, 8);
        int skitters = sortie.enemyCount();
        assertTrue(skitters > 0, "a stream at the settle");
        assertEquals(0, boss.phase());

        sortie.destroyPart(0, LEFT_HEAD);
        sortie.step(0);
        assertEquals(1, boss.phase());
        assertEquals(1, sortie.events().count(SimEvents.Type.BOSS_PHASE));
        assertEquals(1, sortie.events().count(SimEvents.Type.RADIO), "the last-head line");
        assertTrue(boss.partShielded(CORE));

        sortie.destroyPart(0, RIGHT_HEAD);
        sortie.step(0);
        assertEquals(2, boss.phase());
        assertFalse(boss.partShielded(CORE));
        int ring = 0;
        for (int i = 0; i <= SimStep.ticks(BossSpec.PHASE_DELAY_SECONDS); i++) {
            int bullets = sortie.bulletCount();
            sortie.step(0);
            ring = Math.max(ring, sortie.bulletCount() - bullets);
        }
        assertEquals(12, ring, "the core opens with its ring once the crown is open (the phase's delay)");

        sortie.destroyPart(0, CORE);
        assertTrue(boss.destroyed());
        assertEquals(1, sortie.events().count(SimEvents.Type.BOSS_DESTROYED));
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == SimEvents.Type.BOSS_DESTROYED) {
                assertEquals(170, events.value(i), "the credit shower: the parts' bounties");
            }
        }
        assertEquals(0, boss.barShare(), 1e-9);
    }

    @Test
    void phaseOneSendsAStreamEveryTenSecondsAndTheLaterPhasesNone() {
        Sortie sortie = sortie();
        until(sortie, ARRIVE + 4);
        int released = 0;
        for (int i = 0; i < 60 * 14; i++) {
            int before = sortie.enemyTotal();
            sortie.step(0);
            released += sortie.enemyTotal() - before;
        }
        assertEquals(12, released, "two streams of 6");
        sortie.destroyPart(0, LEFT_HEAD);
        int total = sortie.enemyTotal();
        steps(sortie, 60 * 12);
        assertEquals(total, sortie.enemyTotal(), "no streams after phase one");
    }

    @Test
    void theArenaClockHaltsWhileTheBossLivesAndRampsBackUp() {
        Sortie sortie = sortie();
        until(sortie, ARENA_END - SimStep.SECONDS);
        double scroll = sortie.groundScroll();
        steps(sortie, 120);
        assertEquals(ARENA_END - SimStep.SECONDS, sortie.levelSeconds(), 1e-9, "the clock is held");
        assertTrue(sortie.arenaHalted());
        assertEquals(0, sortie.groundSpeed(), 1e-9);
        assertEquals(scroll, sortie.groundScroll(), 1e-9, "the scroll stands still");

        sortie.destroyPart(0, CORE);
        steps(sortie, 2);
        assertTrue(sortie.levelSeconds() > ARENA_END - 1e-9, "the next section starts");
        assertTrue(sortie.groundSpeed() < 130, "the scroll ramps up");
        steps(sortie, 60);
        assertEquals(130, sortie.groundSpeed(), 1e-9);
    }

    @Test
    void anEarlyKillJumpsTheClockToTheArenasEnd() {
        Sortie sortie = sortie();
        until(sortie, 10);
        double scroll = sortie.groundScroll();
        sortie.destroyPart(0, CORE);
        sortie.step(0);
        assertEquals(ARENA_END, sortie.levelSeconds(), 1e-9);
        assertEquals(scroll + (ARENA_END - SimStep.SECONDS - 10) * 30, sortie.groundScroll(), 0.6);
        assertEquals(3, sortie.section());
    }

    @Test
    void aKillUnderParPaysTheBossRush() {
        Sortie fast = sortie();
        until(fast, 10);
        fast.destroyPart(0, CORE);
        until(fast, ARENA_END + 10);
        steps(fast, 2);
        assertTrue(fast.complete());
        LevelResult result = fast.result();
        assertTrue(result.bossTime().underPar());
        assertEquals(9, result.bossTime().killSeconds(), 0.05);
        assertEquals(List.of(new LevelResult.BonusScore("Boss rush", 2000)), result.bonuses());

        Sortie slow = sortie();
        until(slow, ARENA_END - SimStep.SECONDS);
        steps(slow, 60 * 5);
        slow.destroyPart(0, CORE);
        until(slow, ARENA_END + 10);
        steps(slow, 2);
        assertTrue(slow.complete());
        assertFalse(slow.result().bossTime().underPar(), "35 s against a par of 20");
        assertTrue(slow.result().bonuses().isEmpty());
    }

    @Test
    void retryFromBossRestartsAtTheCheckpointOnAnEmptyField() {
        Sortie sortie = sortie();
        assertFalse(sortie.bossCheckpoint());
        until(sortie, ARRIVE + 8);
        assertTrue(sortie.bossCheckpoint());
        sortie.destroyPart(0, LEFT_HEAD);
        steps(sortie, 30);
        assertTrue(sortie.enemyCount() + sortie.bulletCount() > 0);
        assertTrue(sortie.credits() > 0);

        sortie.retryFromBoss();
        sortie.step(0);
        SetPiece boss = sortie.setPiece(0);
        assertEquals(2, sortie.attempt());
        assertEquals(ARRIVE, sortie.levelSeconds(), 1e-9, "the boss arrives again");
        assertEquals(1, sortie.events().count(SimEvents.Type.BOSS_ARRIVED));
        assertEquals(1, sortie.events().count(SimEvents.Type.BOSS_RETRY));
        assertEquals(0, sortie.enemyCount());
        assertEquals(0, sortie.bulletCount());
        assertEquals(0, sortie.credits(), "the credits of the checkpoint");
        assertFalse(boss.partWrecked(LEFT_HEAD));
        assertEquals(0, boss.phase());
        assertTrue(sortie.bossCheckpoint());

        // A retry from the level start drops the checkpoint until the boss is reached again.
        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(0);
        assertFalse(sortie.bossCheckpoint());
    }

    @Test
    void theCheckpointKeepsTheTalliesAndTheDefences() {
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, killLevel(), RUSH_RULES, TestSpecs.FULL_ARMOUR);
        while (sortie.levelSeconds() < 4) {
            sortie.step(Command.FIRE.bit());
        }
        int kills = sortie.kills();
        int credits = sortie.credits();
        long score = sortie.score();
        assertTrue(kills > 0, "the wave before the boss was shot down");
        until(sortie, ARRIVE + 4 + 6);
        sortie.retryFromBoss();
        sortie.step(0);
        assertEquals(kills, sortie.kills());
        assertEquals(credits, sortie.credits());
        assertEquals(score, sortie.score());
    }

    /** A Skitter line straight above the ship at the start, the boss at 5 s. */
    private static LevelScript killLevel() {
        LevelScript level = level();
        LevelScript.SetPieceSpec boss = boss();
        BossSpec late = boss.boss().orElseThrow();
        late = new BossSpec(
                ARRIVE + 4,
                late.x(),
                late.hoverY(),
                late.descentSpeed(),
                late.sineAmplitude(),
                late.sinePeriod(),
                late.layer(),
                late.midBoss(),
                late.barName(),
                late.parSeconds(),
                late.chains(),
                late.attacks(),
                late.phases());
        List<WaveSpec> waves = new ArrayList<>();
        waves.add(TestSpecs.wave(
                0.2, WaveSpec.Formation.LINE_ABREAST, TestSpecs.SKITTER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE));
        return new LevelScript(
                level.number(),
                level.act(),
                0,
                level.sections(),
                waves,
                List.of(),
                List.of(),
                0,
                level.radio(),
                level.secondary(),
                List.of(),
                List.of(),
                List.of(new LevelScript.SetPieceSpec(
                        boss.slug(),
                        boss.size(),
                        boss.body(),
                        boss.contactDamage(),
                        boss.parts(),
                        List.of(),
                        Optional.empty(),
                        Optional.of(late))));
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameStateThroughAFightAndARetry() {
        long[] hashes = new long[2];
        for (int run = 0; run < 2; run++) {
            Sortie sortie = sortie();
            for (int i = 0; i < 60 * 20; i++) {
                sortie.step(Command.FIRE.bit() | (i / 90 % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
                if (i == 60 * 12) {
                    sortie.destroyPart(0, LEFT_HEAD);
                }
                if (i == 60 * 15) {
                    sortie.retryFromBoss();
                }
            }
            hashes[run] = sortie.stateHash();
        }
        assertEquals(hashes[0], hashes[1]);
    }

    @Test
    void fightingTheBossDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        fight(sortie());
        Sortie sortie = sortie();
        long before = threads.getCurrentThreadAllocatedBytes();
        fight(sortie);
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;
        assertTrue(allocated < 1024, "the fight allocated " + allocated + " bytes");
    }

    /** All three phases, the halt and the retry, with the guns firing. */
    private static void fight(Sortie sortie) {
        for (int i = 0; i < 60 * 45; i++) {
            if (i == 60 * 12) {
                sortie.destroyPart(0, LEFT_HEAD);
            } else if (i == 60 * 16) {
                sortie.destroyPart(0, RIGHT_HEAD);
            } else if (i == 60 * 25) {
                sortie.retryFromBoss();
            }
            sortie.step(Command.FIRE.bit() | (i / 90 % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
        }
    }
}
