package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Level 06's rules (design/campaign Level 06, M4 part F): the Coilwyrm's segment chain (path
 * history, cut and regrowth, chained death, the tail's first bonus, the loop-back with its
 * warning), the Mantis's side hover and laser sweep, the darkness that hides a dark trigger, the
 * Smart Bomb and the data core.
 */
class FarsideTest {
    private static final int SPECIAL = Command.SPECIAL.bit();
    private static final int SEGMENTS = 12;

    /** The Coilwyrm's numbers at medium (design/enemies/air/coilwyrm/data.yaml). */
    static final EnemySpec COILWYRM = coilwyrm();

    static final EnemySpec MANTIS = mantis();

    private static EnemySpec part(
            String slug,
            double hp,
            Hitbox box,
            double contact,
            boolean rammed,
            int bounty,
            Optional<EnemyGun> gun,
            double speed) {
        return new EnemySpec(
                slug,
                hp,
                box,
                Layer.AIR,
                contact,
                rammed,
                bounty,
                speed,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                gun,
                Optional.empty(),
                Optional.empty(),
                false);
    }

    private static EnemySpec coilwyrm() {
        EnemyGun fan = new EnemyGun(
                2.0,
                0,
                1,
                160,
                4,
                false,
                3,
                Math.toRadians(24),
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Optional.empty(),
                Optional.empty());
        List<Hitbox> boxes = new ArrayList<>();
        List<Double> lengths = new ArrayList<>(List.of(58.0));
        for (int i = 0; i < SEGMENTS; i++) {
            double size = 54 + (27 - 54) * (double) i / (SEGMENTS - 1);
            boxes.add(new Hitbox(size * 0.7, size * 0.7));
            lengths.add(size);
        }
        lengths.add(40.0);
        List<Double> offsets = new ArrayList<>(List.of(0.0));
        for (int i = 1; i < lengths.size(); i++) {
            offsets.add(offsets.get(i - 1) + 0.9 * (lengths.get(i - 1) + lengths.get(i)) / 2);
        }
        EnemySpec segment = part("coilwyrm-segment", 4, boxes.getFirst(), 10, true, 3, Optional.empty(), 180);
        EnemySpec tail = part("coilwyrm-tail", 10, new Hitbox(28, 28), 10, true, 10, Optional.empty(), 180);
        EnemySpec regrown = part("coilwyrm-regrown", 20, new Hitbox(41, 41), 20, false, 20, Optional.of(fan), 220);
        EnemySpec.ChainSpec chain = new EnemySpec.ChainSpec(segment, boxes, tail, 10, regrown, 0.6, 220, offsets, 0.06);
        EnemySpec head = part("coilwyrm", 40, new Hitbox(41, 41), 20, false, 40, Optional.of(fan), 180);
        return withChain(head, chain);
    }

    private static EnemySpec withChain(EnemySpec head, EnemySpec.ChainSpec chain) {
        return new EnemySpec(
                head.slug(),
                head.hp(),
                head.hitbox(),
                head.layer(),
                head.contactDamage(),
                head.destroyedByRamming(),
                head.bounty(),
                head.speed(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                head.gun(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(chain));
    }

    private static EnemySpec mantis() {
        return new EnemySpec(
                "mantis",
                26,
                new Hitbox(40, 56),
                Layer.AIR,
                10,
                false,
                30,
                150,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Hover(new Range(6, 6), new Range(300, 300))),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.SideHover(40, true)),
                Optional.of(new EnemySpec.Sweep(Math.toRadians(70), 1.2, 0.6, 300, 6, 3.0, 0.6, 8)),
                Optional.empty());
    }

    /** A chain wave at t=1 on a path straight down the centre, with an optional loop-back. */
    private static WaveSpec chainWave(double x, Optional<WaveSpec.LoopBack> loop) {
        return new WaveSpec(
                1,
                WaveSpec.Formation.SNAKE,
                COILWYRM,
                1,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(List.of(new WaveSpec.At(x, -60), new WaveSpec.At(x, 300), new WaveSpec.At(x, 700))),
                loop);
    }

    private static WaveSpec mantisWave(double t, WaveSpec.Formation formation, WaveSpec.Edge edge, int count) {
        return new WaveSpec(
                t,
                formation,
                MANTIS,
                count,
                WaveSpec.Entry.SIDES,
                edge,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty());
    }

    private static LevelScript level(
            List<WaveSpec> waves,
            List<LevelScript.GroundObjectSpec> objects,
            int secrets,
            Optional<LevelScript.Darkness> darkness) {
        return new LevelScript(
                6,
                1,
                0,
                List.of(new LevelScript.Section(40, 130)),
                waves,
                objects,
                List.of(),
                secrets,
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "mantis", List.of(), ""),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                darkness);
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static void run(Sortie sortie, double seconds, int commands) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(commands);
        }
    }

    private static int count(Sortie sortie, String slug) {
        int n = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            n += sortie.enemy(i).spec().slug().equals(slug) ? 1 : 0;
        }
        return n;
    }

    private static int indexOf(Sortie sortie, Chain chain, int link) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.chain() == chain && enemy.link() == link) {
                return i;
            }
        }
        return -1;
    }

    private static Enemy member(Sortie sortie, Chain chain, int link) {
        int index = indexOf(sortie, chain, link);
        return index < 0 ? null : sortie.enemy(index);
    }

    @Test
    void theSegmentsFollowTheHeadsPathAtTheirSpacingAndTheChainCountsOnce() {
        Sortie sortie = sortie(level(List.of(chainWave(240, Optional.empty())), List.of(), 0, Optional.empty()));
        run(sortie, 3, Command.NONE);

        assertEquals(1, sortie.enemyTotal(), "a chain counts as one enemy");
        assertEquals(1, sortie.chainCount());
        Chain chain = sortie.chain(0);
        assertEquals(SEGMENTS + 2, sortie.enemyCount());
        Enemy head = member(sortie, chain, 0);
        Enemy third = member(sortie, chain, 3);
        Enemy tail = member(sortie, chain, SEGMENTS + 1);
        assertEquals(240, head.x(), 1e-6);
        assertEquals(240, third.x(), 1e-6);
        assertEquals(COILWYRM.chain().orElseThrow().offsets().get(3), third.y() - head.y(), 0.5);
        assertEquals("coilwyrm-tail", tail.spec().slug());
        assertTrue(tail.y() > third.y(), "the tail trails above on a dive down the screen");
        assertEquals(0, head.facing(), 1e-6, "facing down its path");
        assertTrue(third.hitbox().width() > tail.hitbox().width() - 20, "the segments have their own boxes");
    }

    @Test
    void aCutSegmentSplitsTheChainAndTheRearPartGrowsAHeadThatLungesAtTheShipOnce() {
        Sortie sortie = sortie(level(List.of(chainWave(240, Optional.empty())), List.of(), 0, Optional.empty()));
        run(sortie, 3.5, Command.NONE);
        Chain chain = sortie.chain(0);
        int credits = sortie.credits();

        sortie.destroyEnemy(indexOf(sortie, chain, 5));

        assertEquals(credits + 3, sortie.credits(), "a segment pays its bounty");
        assertEquals(0, sortie.kills(), "a segment is not a kill");
        assertEquals(2, sortie.chainCount(), "the rear part is a chain of its own");
        Chain rear = sortie.chain(1);
        assertTrue(rear.regrowing());
        assertEquals(5, chain.size(), "the front part: the head and four segments");
        double cutY = rear.headY();
        int grownAt = -1;
        for (int i = 0; i < SimStep.ticks(1) && grownAt < 0; i++) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.CHAIN_REGROWN) {
                    grownAt = i + 1;
                }
            }
            assertTrue(i + 1 >= SimStep.ticks(0.6) || rear.headY() == cutY, "the rear part holds while it grows");
        }
        assertEquals(SimStep.ticks(0.6), grownAt, 1, "the new head grows in 0.6 s");
        Enemy head = member(sortie, rear, 0);
        assertNotNull(head);
        assertEquals("coilwyrm-regrown", head.spec().slug());
        assertEquals(20, head.hp());
        run(sortie, 0.5, Command.NONE);
        assertTrue(head.y() < cutY - 50, "it lunges at the ship below");

        // A second cut does not regrow: the severed part dies from the cut backwards.
        int chains = sortie.chainCount();
        sortie.destroyEnemy(indexOf(sortie, rear, 2));
        int pops = 0;
        for (int i = 0; i < SimStep.ticks(1); i++) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                pops += sortie.events().type(e) == SimEvents.Type.CHAIN_POP ? 1 : 0;
                assertNotEquals(SimEvents.Type.CHAIN_CUT, sortie.events().type(e));
            }
        }
        assertTrue(sortie.chainCount() <= chains, "no new chain");
        assertEquals(SEGMENTS + 1 - 5 - 2, pops, "every member behind the second cut popped");
        assertNotNull(member(sortie, rear, 0), "the regrown head flies on");
        assertNotNull(member(sortie, rear, 1), "with its first segment");
        assertEquals(null, member(sortie, rear, 2));
    }

    @Test
    void theHeadsDeathPopsTheWholeChainAndPaysTheHeadOnly() {
        Sortie sortie = sortie(level(List.of(chainWave(240, Optional.empty())), List.of(), 0, Optional.empty()));
        run(sortie, 3.5, Command.NONE);
        Chain chain = sortie.chain(0);

        sortie.destroyEnemy(indexOf(sortie, chain, 0));
        int credits = sortie.credits();
        int pops = 0;
        int lastPop = -1;
        int firstPop = -1;
        for (int i = 0; i < SimStep.ticks(2); i++) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.CHAIN_POP) {
                    pops++;
                    firstPop = firstPop < 0 ? i : firstPop;
                    lastPop = i;
                }
            }
        }

        assertEquals(40, credits, "the head's bounty");
        assertEquals(40, sortie.credits(), "the chained pops pay nothing");
        assertEquals(1, sortie.kills(), "one enemy");
        assertEquals(SEGMENTS + 1, pops);
        assertEquals(SimStep.ticks(0.06) * SEGMENTS, lastPop - firstPop, "0.06 s between pops");
        assertEquals(0, sortie.chainCount());
        assertEquals(0, sortie.enemyCount());
    }

    @Test
    void theTailDestroyedFirstPaysItsBonusAndDoesNotCutTheChain() {
        Sortie sortie = sortie(level(List.of(chainWave(240, Optional.empty())), List.of(), 0, Optional.empty()));
        run(sortie, 4.5, Command.NONE);
        Chain chain = sortie.chain(0);

        sortie.destroyEnemy(indexOf(sortie, chain, SEGMENTS + 1));
        assertEquals(20, sortie.credits(), "tail 10 + its first bonus 10");
        assertEquals(1, sortie.chainCount(), "nothing behind the tail to regrow");

        sortie.destroyEnemy(indexOf(sortie, chain, SEGMENTS));
        assertEquals(23, sortie.credits(), "the last segment pays its 3, no more bonus");
    }

    @Test
    void aLoopBackReentersFromTheBottomAtTheHeadsXWarnedThreeSecondsAhead() {
        WaveSpec wave = chainWave(120, Optional.of(new WaveSpec.LoopBack(6, List.of())));
        Sortie sortie = sortie(level(List.of(wave), List.of(), 0, Optional.empty()));
        // The path: 760 px at 180 px/s from t=1, then 6 s off the screen.
        double reentry = 1 + 760.0 / 180 + 6;
        double warnedFrom = -1;
        double back = -1;
        while (sortie.levelSeconds() < reentry + 3) {
            sortie.step(Command.NONE);
            if (warnedFrom < 0 && (sortie.edgeWarnings() & WarningEdge.BOTTOM.bit()) != 0) {
                warnedFrom = sortie.levelSeconds();
            }
            if (back < 0 && sortie.chainCount() == 1) {
                Enemy head = member(sortie, sortie.chain(0), 0);
                if (sortie.levelSeconds() > reentry - 1 && head != null && head.y() > 0 && head.y() < 100) {
                    back = sortie.levelSeconds();
                    assertEquals(120, head.x(), 1e-6, "at the head's x");
                }
            }
        }

        assertEquals(reentry - 3, warnedFrom, 0.1, "the bottom edge warned 3 s ahead");
        assertTrue(back > reentry && back < reentry + 1, "it is back from the bottom edge at " + back);
    }

    @Test
    void aMantisHoversAtItsEdgeSweepsTheShipOncePerSweepAndLeavesTheWayItCame() {
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT,
                level(
                        List.of(mantisWave(3.5, WaveSpec.Formation.SINGLE, WaveSpec.Edge.LEFT, 1)),
                        List.of(),
                        0,
                        Optional.empty()),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        run(sortie, 1, Command.NONE);
        assertTrue((sortie.edgeWarnings() & WarningEdge.LEFT.bit()) != 0, "the left edge is warned");

        List<Double> telegraphs = new ArrayList<>();
        List<Double> hits = new ArrayList<>();
        double minX = Double.MAX_VALUE;
        double settledX = -1;
        while (sortie.levelSeconds() < 14) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.SWEEP_TELEGRAPH) {
                    telegraphs.add(sortie.levelSeconds());
                    settledX = sortie.enemy(0).x();
                    double centre = sortie.enemy(0).sweepCentre();
                    assertTrue(centre >= -Math.PI / 2 && centre <= 0, "clamped between inward and down");
                } else if (sortie.events().type(e) == SimEvents.Type.SWEEP_HIT) {
                    hits.add(sortie.levelSeconds());
                }
            }
            if (sortie.enemyCount() > 0) {
                minX = Math.min(minX, sortie.enemy(0).x());
            }
        }

        assertEquals(40, settledX, 1e-6, "it hovers 40 px from its edge");
        assertEquals(2, telegraphs.size(), "two sweeps in its 6 s hover");
        assertEquals(3.0, telegraphs.get(1) - telegraphs.get(0), 0.02);
        assertEquals(2, hits.size(), "the ship that stays is hit once per sweep: " + hits);
        assertTrue(hits.getFirst() > telegraphs.getFirst() + 0.6, "after the telegraph");
        assertEquals(0, sortie.enemyCount(), "it left");
        assertTrue(minX < -20, "through the left edge it came by");
    }

    @Test
    void aMantisPincerSendsOneToEachEdgeAndTheFarSideIsOutOfReach() {
        Sortie sortie = sortie(level(
                List.of(mantisWave(3, WaveSpec.Formation.PINCER, WaveSpec.Edge.NONE, 2)),
                List.of(),
                0,
                Optional.empty()));
        run(sortie, 6, Command.NONE);
        assertEquals(2, sortie.enemyCount());
        double a = sortie.enemy(0).x();
        double b = sortie.enemy(1).x();
        assertEquals(PlayField.WIDTH, a + b, 1e-6);
        assertEquals(40, Math.min(a, b), 1e-6);
        // The beam ends 300 px from the eye: the far quarter of the field stays safe.
        Enemy left = a < b ? sortie.enemy(0) : sortie.enemy(1);
        assertTrue(left.x() + 300 < PlayField.WIDTH - 100);
    }

    private static final LevelScript.Darkness DARK = new LevelScript.Darkness(
            2,
            200,
            Math.toRadians(60),
            List.of(new LevelScript.Darkness.Flare(8, 360, 200)),
            8,
            120,
            40,
            List.of(),
            0.1);

    /** A dark survey cache at x, entering at the top edge at t=0.5 (3 hits). */
    private static LevelScript.GroundObjectSpec cache(double x) {
        return new LevelScript.GroundObjectSpec(
                0.5,
                x,
                new Hitbox(28, 22),
                0,
                0,
                Optional.empty(),
                3,
                135,
                "survey cache",
                false,
                0,
                1,
                Optional.empty(),
                "survey-cache",
                true,
                Optional.empty());
    }

    @Test
    void aDarkTriggerTakesHitsOnlyWhileTheHeadlightOrAFlareLightsIt() {
        LevelScript dark = level(List.of(), List.of(cache(Ship.START_X)), 1, Optional.of(DARK));
        Sortie sortie = sortie(dark);
        // Before the headlight is on (t=2) the cache ahead of the ship is dark: shots pass it.
        run(sortie, 1.9, Command.FIRE.bit());
        assertTrue(sortie.groundObject(0).shut(), "dark before the headlight");
        assertFalse(sortie.groundObject(0).spent());
        run(sortie, 0.7, Command.NONE);
        assertFalse(sortie.groundObject(0).shut(), "the headlight lights it ahead of the ship");

        // To the side, out of the cone: dark until the flare's pool drifts over it.
        Sortie aside = sortie(level(List.of(), List.of(cache(380)), 1, Optional.of(DARK)));
        run(aside, 6, Command.NONE);
        assertTrue(aside.groundObject(0).shut());
        assertFalse(aside.lit(aside.groundObject(0)));
    }

    @Test
    void aFlareLightsWhatItsPoolPasses() {
        assertTrue(DARK.lit(360, PlayField.HEIGHT - 200, new Hitbox(10, 10), 0, 0, 8.1));
        assertTrue(DARK.lit(360, PlayField.HEIGHT - 200 - 40 * 4, new Hitbox(10, 10), 0, 0, 12));
        assertFalse(DARK.lit(360, PlayField.HEIGHT - 200, new Hitbox(10, 10), 0, 0, 16.5), "burnt out after 8 s");
        assertFalse(DARK.lit(360, PlayField.HEIGHT - 200, new Hitbox(10, 10), 0, 0, 7.9), "not fired yet");
    }

    private static Sortie bomber(LevelScript level, int charges) {
        SmartBombSpec bomb = new SmartBombSpec(0.35, 120, 60, 1.0, 1.5, 0.1, 0.8, 0.25);
        return new Sortie(
                1,
                TestSpecs.LOADOUT.withSpecial(new SpecialSpec("Smart Bomb", charges, 3, 0.1, bomb)),
                level,
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
    }

    @Test
    void aSmartBombClearsTheBulletsHitsEveryEnemyOnScreenOnceAndGuardsTheShip() {
        Sortie sortie = bomber(
                level(
                        List.of(
                                chainWave(240, Optional.empty()),
                                TestSpecs.wave(
                                        1,
                                        WaveSpec.Formation.V_WING,
                                        TestSpecs.NEEDLER,
                                        3,
                                        WaveSpec.Entry.FRONT,
                                        WaveSpec.Edge.NONE)),
                        List.of(),
                        0,
                        Optional.empty()),
                2);
        run(sortie, 4.2, Command.NONE);
        assertTrue(sortie.bulletCount() > 0, "the needlers and the head fired");
        int onScreen = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            onScreen += PlayerFire.onField(sortie.enemy(i)) ? 1 : 0;
        }
        assertTrue(onScreen > 5);

        sortie.step(SPECIAL);
        assertEquals(0, sortie.bulletCount(), "every bullet at once");
        assertTrue(sortie.ship().defences().mercyTicks() >= SimStep.ticks(1.0) - 1, "invulnerable for 1 s");
        run(sortie, 0.4, Command.NONE);
        assertEquals(1, sortie.special().charges());
        // The ring reaches the head first (nearest the ship): the body is doomed and bursts in the
        // chained death's ripple, unhit by the rest of the ring, paying nothing.
        assertEquals(0, count(sortie, "coilwyrm"), "120 destroys the head");
        assertEquals(0, count(sortie, "coilwyrm-regrown"), "nothing regrows");
        assertTrue(sortie.kills() >= 4, "the chain's head and the needlers on screen: " + sortie.kills());

        // 1.5 s between bombs: a press now is denied, one after it goes off.
        sortie.step(SPECIAL);
        assertEquals(1, sortie.special().charges());
        run(sortie, 1.2, Command.NONE);
        assertEquals(0, sortie.chainCount(), "every member has burst");
        sortie.step(SPECIAL);
        assertEquals(0, sortie.special().charges());
    }

    @Test
    void aSmartBombPopsTheSporeMinesItsRingPasses() {
        EnemyGun spores = new EnemyGun(
                1.6,
                0,
                1,
                90,
                6,
                false,
                1,
                0,
                TestSpecs.INFINITE,
                TestSpecs.INFINITE,
                Optional.of(new EnemyGun.MineSpec(1, 8, 20, 1, 6, 4, true, 1)));
        Sortie sortie = bomber(level(List.of(), List.of(), 0, Optional.empty()), 1);
        sortie.dropMine(spores, 100, 400, 0);
        sortie.dropMine(spores, 380, 200, 0);
        sortie.step(Command.NONE);
        assertEquals(2, sortie.mineCount());

        sortie.step(SPECIAL);
        int popped = 0;
        for (int i = 0; i < SimStep.ticks(0.5); i++) {
            sortie.step(Command.NONE);
            popped += sortie.events().count(SimEvents.Type.MINE_DESTROYED);
        }

        assertEquals(2, popped, "both, armed or not, as the ring passes");
        assertEquals(0, sortie.mineCount());
    }

    @Test
    void aDataCoreDropsFromItsTerminalAndIsRecordedWhenCollected() {
        LevelScript.GroundObjectSpec terminal = new LevelScript.GroundObjectSpec(
                0.5,
                Ship.START_X,
                new Hitbox(30, 30),
                0,
                0,
                Optional.empty(),
                2,
                0,
                "settlement-log",
                false,
                0,
                1,
                Optional.empty(),
                "airlock-terminal",
                false,
                Optional.of(new LevelResult.DataCore("settlement-log", "Targeting computer")));
        Sortie sortie = sortie(level(List.of(), List.of(terminal), 1, Optional.empty()));
        boolean dropped = false;
        boolean collected = false;
        for (int i = 0; i < SimStep.ticks(12); i++) {
            boolean fire = i > SimStep.ticks(3) && i < SimStep.ticks(6);
            sortie.step(fire ? Command.FIRE.bit() : Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.SECRET_FOUND) {
                    dropped = sortie.pickup(sortie.pickupCount() - 1).type() == PickupType.DATA_CORE;
                }
                if (sortie.events().type(e) == SimEvents.Type.PICKUP_COLLECTED
                        && sortie.events().value(e) == PickupType.DATA_CORE.ordinal()) {
                    collected = true;
                }
            }
        }
        assertTrue(dropped);
        assertTrue(collected, "the core drifts down onto the ship");
        assertEquals(List.of(new LevelResult.DataCore("settlement-log", "Targeting computer")), sortie.dataCores());
        assertEquals(0, sortie.credits(), "a data core pays no credits");
    }

    private static LevelScript everything() {
        return level(
                List.of(
                        chainWave(240, Optional.of(new WaveSpec.LoopBack(6, List.of()))),
                        mantisWave(4, WaveSpec.Formation.PINCER, WaveSpec.Edge.NONE, 2),
                        TestSpecs.wave(
                                6,
                                WaveSpec.Formation.V_WING,
                                TestSpecs.NEEDLER,
                                5,
                                WaveSpec.Entry.FRONT,
                                WaveSpec.Edge.NONE)),
                List.of(cache(200)),
                1,
                Optional.of(DARK));
    }

    /** Fire on, the ship weaving, a Smart Bomb every few seconds. */
    private static int commands(int tick) {
        int weave = tick / 90 % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit();
        int bomb = tick % SimStep.ticks(4) == 0 ? SPECIAL : 0;
        return Command.FIRE.bit() | weave | bomb;
    }

    @Test
    void theSameInputGivesTheSameStateAndAnotherSeedADifferentOne() {
        Sortie first = bomber(everything(), 3);
        Sortie second = bomber(everything(), 3);
        for (int i = 0; i < SimStep.ticks(25); i++) {
            first.step(commands(i));
            second.step(commands(i));
            if (i % 60 == 0) {
                assertEquals(first.stateHash(), second.stateHash(), "at step " + i);
            }
        }
        assertEquals(first.stateHash(), second.stateHash());
    }

    @Test
    void flyingTheChainsTheSweepsTheDarkAndTheBombAllocatesNothing() {
        // A first flight loads the classes its events need once.
        Sortie warm = bomber(everything(), 3);
        for (int i = 0; i < SimStep.ticks(20); i++) {
            warm.step(commands(i));
        }
        Sortie sortie = bomber(everything(), 3);
        sortie.step(Command.NONE);
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 1; i < SimStep.ticks(20); i++) {
            sortie.step(commands(i));
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;
        assertTrue(allocated < 1024, SimStep.ticks(20) + " steps allocated " + allocated + " bytes");
    }
}
