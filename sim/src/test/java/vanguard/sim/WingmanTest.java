package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.SKITTER;
import static vanguard.sim.TestSpecs.muzzle;
import static vanguard.sim.TestSpecs.wave;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Rook in the escort slot (design/player/wingmen, M5 part A): his formation, the mirrored slot, the
 * wave formations, dodging, his fire whenever (and only while) the player fires and its kills
 * paying like the player's, his damage, the ejection, the retry and the boss checkpoint,
 * determinism and no allocation.
 */
class WingmanTest {
    /** His gun: an Autocannon-like bolt from his nose, 2.4 damage at 5 volleys/s (12 DPS). */
    static final WeaponSpec GUN =
            TestSpecs.bolt("autocannon-pod", 5, 2.4, 1000, new Hitbox(4, 8), INFINITE, 1, muzzle(0, 18, 0));

    static final WingmanSpec.Craft CRAFT =
            new WingmanSpec.Craft(42, 80, new Hitbox(11, 11), 250, 0.2, 40, 12, 20, 0.3, 120, 3);
    static final WingmanSpec.Ai AI = new WingmanSpec.Ai(
            new WingmanSpec.Offset(64, 28),
            new WingmanSpec.Offset(120, 10),
            new WingmanSpec.Offset(40, 90),
            0.6,
            1.0,
            160,
            0.25,
            0.1,
            0.4,
            14,
            48,
            1,
            Math.toRadians(15),
            360,
            1.0);

    static WingmanSpec rook(WingmanSpec.Side side) {
        return new WingmanSpec(side, 80, CRAFT, AI, GUN);
    }

    static Sortie sortie(LevelScript level, WingmanSpec.Side side) {
        return new Sortie(1, TestSpecs.LOADOUT.withWingman(rook(side)), level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static Wingman wingman(Sortie sortie) {
        return sortie.wingman().orElseThrow();
    }

    private static void run(Sortie sortie, int steps, int commands) {
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
        }
    }

    private static boolean happened(Sortie sortie, SimEvents.Type type) {
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == type) {
                return true;
            }
        }
        return false;
    }

    /** A bullet from his left that hits him in the next steps, too close for a dodge. */
    private static void shoot(Sortie sortie, double damage) {
        Wingman rook = wingman(sortie);
        sortie.fireBullet(rook.x() - 20, rook.y(), 0, 900, damage);
    }

    @Test
    void heKeepsHisWingSlotBesideAndBehindThePlayer() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        assertEquals(Ship.START_X - 64, rook.x());
        assertEquals(Ship.START_Y - 28, rook.y());
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < 180; i++) {
            sortie.step(i < 60 ? Command.RIGHT.bit() : i < 90 ? Command.UP.bit() : Command.NONE);
            Ship ship = sortie.ship();
            closest = Math.min(closest, Math.hypot(rook.x() - ship.x(), rook.y() - ship.y()));
        }

        Ship ship = sortie.ship();
        assertEquals(ship.x() - 64, rook.x(), 0.5);
        assertEquals(ship.y() - 28, rook.y(), 0.5);
        assertEquals(Wingman.Formation.WING, rook.formation());
        assertTrue(closest >= 40 - 1e-9, "never closer than 40 px: " + closest);
        assertEquals(0, rook.bank());
    }

    @Test
    void theRightSideMirrorsHisSlot() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.RIGHT);
        run(sortie, 30, Command.NONE);

        assertEquals(Ship.START_X + 64, wingman(sortie).x(), 1e-9);
    }

    @Test
    void heTakesTheMirroredSlotWhileHisOwnIsOutsideTheFieldAndReturnsAfterASecond() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 120, Command.LEFT.bit());
        assertEquals(WingmanSpec.Side.RIGHT, rook.side());
        run(sortie, 60, Command.NONE);
        assertEquals(sortie.ship().x() + 64, rook.x(), 1);

        // Back at the centre his own slot is inside again: after a second and his reaction delay he returns.
        run(sortie, 40, Command.RIGHT.bit());
        assertEquals(WingmanSpec.Side.RIGHT, rook.side());
        run(sortie, 150, Command.NONE);
        assertEquals(WingmanSpec.Side.LEFT, rook.side());
        assertEquals(sortie.ship().x() - 64, rook.x(), 1);
    }

    @Test
    void aRearWaveMovesHimToTrailAndASidesWaveToWide() {
        Sortie rear = sortie(
                TestSpecs.level(
                        30,
                        List.of(wave(
                                1,
                                WaveSpec.Formation.LINE_ABREAST,
                                SKITTER,
                                3,
                                WaveSpec.Entry.REAR,
                                WaveSpec.Edge.NONE))),
                WingmanSpec.Side.LEFT);
        run(rear, 60 + 10, Command.NONE);
        assertEquals(Wingman.Formation.WING, wingman(rear).formation(), "his reaction delay");
        run(rear, 10, Command.NONE);
        assertEquals(Wingman.Formation.TRAIL, wingman(rear).formation());
        run(rear, 40, Command.NONE);
        assertTrue(
                wingman(rear).y() < rear.ship().y() - 50,
                "behind the player: " + wingman(rear).y());

        Sortie sides = sortie(
                TestSpecs.level(
                        30,
                        List.of(wave(
                                1, WaveSpec.Formation.SNAKE, SKITTER, 4, WaveSpec.Entry.SIDES, WaveSpec.Edge.RIGHT))),
                WingmanSpec.Side.LEFT);
        run(sides, 60 + 20, Command.NONE);
        assertEquals(Wingman.Formation.WIDE, wingman(sides).formation());
    }

    @Test
    void heSidestepsABulletHeSeesComing() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 10, Command.NONE);
        // Coming straight down 6 px to his right: it would hit him where he flies.
        double slotX = rook.x();
        sortie.fireBullet(rook.x() + 6, rook.y() + 300, -Math.PI / 2, 150, 6);
        boolean dodged = false;
        double furthest = slotX;
        for (int i = 0; i < 240; i++) {
            sortie.step(Command.NONE);
            dodged |= rook.dodging();
            furthest = Math.min(furthest, rook.x());
        }

        assertTrue(dodged);
        assertTrue(furthest < slotX - 10, "a sidestep away from the bullet's line: " + furthest);
        assertEquals(80, rook.armour());
        assertEquals(slotX, rook.x(), 0.5, "back in his slot");
        assertEquals(0, sortie.bulletCount());
    }

    /** {@link #AI}, but reacting to {@code reacts} of the bullets he predicts. */
    private static WingmanSpec.Ai reacting(double reacts) {
        return new WingmanSpec.Ai(
                AI.wing(),
                AI.wide(),
                AI.trail(),
                AI.glideSeconds(),
                AI.swapSeconds(),
                AI.flankDistance(),
                AI.reactionSeconds(),
                AI.dodgeInterval(),
                AI.lookAhead(),
                AI.clearance(),
                AI.dodgeStep(),
                reacts,
                AI.coneHalfAngle(),
                AI.range(),
                AI.recentHitSeconds());
    }

    /** Steps per bullet: a whole number of his prediction intervals, so each comes at the same phase. */
    private static final int BULLET_STEPS = 240;

    /**
     * {@code count} single bullets, one every {@link #BULLET_STEPS}, straight down 8 px beside his
     * centre (a hit unless he moves), closest 123 steps after it is fired: he decides on it half
     * way through his look-ahead, early enough to clear it. Returns which of them hit him; checks
     * that each one is decided exactly once (his generator draws once per bullet).
     */
    private static boolean[] singleBullets(long seed, double reacts, int count) {
        WingmanSpec spec = new WingmanSpec(WingmanSpec.Side.LEFT, 80, CRAFT, reacting(reacts), GUN);
        Sortie sortie = new Sortie(
                seed,
                TestSpecs.LOADOUT.withWingman(spec),
                TestSpecs.level(10 + 4.0 * count, List.of()),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        Wingman rook = wingman(sortie);
        run(sortie, 30, Command.NONE);
        boolean[] hits = new boolean[count];
        SplitMix64 once = new SplitMix64(0);
        for (int b = 0; b < count; b++) {
            long before = rook.luck();
            sortie.fireBullet(rook.x() + 8, rook.y() + 150 * 123.0 / SimStep.PER_SECOND, -Math.PI / 2, 150, 0.1);
            for (int i = 0; i < BULLET_STEPS; i++) {
                sortie.step(Command.NONE);
                hits[b] |= happened(sortie, SimEvents.Type.WINGMAN_HIT);
            }
            assertEquals(0, sortie.bulletCount());
            once.state(before);
            once.nextDouble();
            assertEquals(once.state(), rook.luck(), "bullet " + b + " is decided once");
        }
        return hits;
    }

    private static int count(boolean[] hits) {
        int n = 0;
        for (boolean hit : hits) {
            n += hit ? 1 : 0;
        }
        return n;
    }

    @Test
    void heReactsToHisShareOfTheBulletsEachDecidedOnceTheSameForTheSameSeed() {
        assertEquals(20, count(singleBullets(1, 0.0, 20)), "reacting to none, every one hits him");
        assertEquals(0, count(singleBullets(1, 1.0, 20)), "reacting to every one, no single bullet hits him");

        boolean[] hits = singleBullets(1, 0.7, 200);
        int n = count(hits);
        // Decided again at every prediction (four per bullet here), one would hit only 0.3^4 of the time.
        assertTrue(n >= 40 && n <= 80, "about 30 % of 200 single bullets hit him: " + n);
        assertTrue(Arrays.equals(hits, singleBullets(1, 0.7, 200)), "the same seed, the same hits");
        assertFalse(Arrays.equals(hits, singleBullets(2, 0.7, 200)), "another seed, other hits");
    }

    /** A Coilwyrm (Level 06's numbers) entering from {@code entry} at t=1 on {@code path} (depths). */
    private static WaveSpec chainWave(WaveSpec.Entry entry, List<WaveSpec.At> path, Optional<WaveSpec.LoopBack> loop) {
        return new WaveSpec(
                1,
                WaveSpec.Formation.SNAKE,
                FarsideTest.COILWYRM,
                1,
                entry,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(path),
                loop);
    }

    /** Only his formation matters here: nothing hurts the ship or him. */
    private static Sortie unhurt(LevelScript level) {
        return new Sortie(
                1,
                TestSpecs.LOADOUT.withWingman(rook(WingmanSpec.Side.LEFT)),
                level,
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
    }

    @Test
    void aChainFromTheRearMovesHimToTrail() {
        Sortie sortie = unhurt(TestSpecs.level(
                30,
                List.of(chainWave(
                        WaveSpec.Entry.REAR,
                        List.of(new WaveSpec.At(420, 620), new WaveSpec.At(420, 300), new WaveSpec.At(420, -80)),
                        Optional.empty()))));
        run(sortie, 60 + 20, Command.NONE);

        assertEquals(Wingman.Formation.TRAIL, wingman(sortie).formation());
    }

    @Test
    void aFrontChainThatLoopsBackMovesHimToTrailOnceItIsBackFromTheRear() {
        Sortie sortie = unhurt(TestSpecs.level(
                40,
                List.of(chainWave(
                        WaveSpec.Entry.FRONT,
                        List.of(new WaveSpec.At(420, -60), new WaveSpec.At(420, 300), new WaveSpec.At(420, 700)),
                        Optional.of(new WaveSpec.LoopBack(6, List.of()))))));
        Wingman rook = wingman(sortie);
        // The path: 760 px at 180 px/s from t=1, then 6 s off the screen.
        double reentry = 1 + 760.0 / 180 + 6;
        boolean trailBefore = false;
        while (sortie.levelSeconds() < reentry - 0.1) {
            sortie.step(Command.NONE);
            trailBefore |= rook.formation() == Wingman.Formation.TRAIL;
        }
        assertFalse(trailBefore, "a front chain is no rear wave");
        while (sortie.levelSeconds() < reentry + 1) {
            sortie.step(Command.NONE);
        }

        assertEquals(Wingman.Formation.TRAIL, rook.formation(), "the loop-back is a rear wave");
    }

    /** The gap between his hit box and an enemy's, px: negative where they overlap. */
    private static double gap(Wingman rook, Enemy enemy) {
        Hitbox his = rook.spec().craft().hitbox();
        Hitbox its = enemy.hitbox();
        return Math.max(
                Math.abs(rook.x() - enemy.x()) - (his.width() + its.width()) / 2,
                Math.abs(rook.y() - enemy.y()) - (his.height() + its.height()) / 2);
    }

    /** One step's command towards (x, y), with a small dead zone. */
    private static int towards(Ship ship, double x, double y) {
        int commands = Command.NONE;
        if (ship.x() < x - 2) {
            commands |= Command.RIGHT.bit();
        } else if (ship.x() > x + 2) {
            commands |= Command.LEFT.bit();
        }
        if (ship.y() < y - 2) {
            commands |= Command.UP.bit();
        } else if (ship.y() > y + 2) {
            commands |= Command.DOWN.bit();
        }
        return commands;
    }

    @Test
    void hisWideSlotKeepsClearOfAMantisBody() {
        WaveSpec mantis = new WaveSpec(
                2,
                WaveSpec.Formation.SINGLE,
                FarsideTest.MANTIS,
                1,
                WaveSpec.Entry.SIDES,
                WaveSpec.Edge.LEFT,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty());
        Sortie sortie = sortie(TestSpecs.level(30, List.of(mantis)), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        boolean wide = false;
        boolean contact = false;
        double closest = Double.MAX_VALUE;
        int steps = 0;
        // The player where his Wide slot (120 px beside, 10 px behind) lies on the Mantis hovering
        // 40 px from the left edge, 300 px below the top.
        while (sortie.levelSeconds() < 14) {
            sortie.step(towards(sortie.ship(), 170, 250));
            wide |= rook.formation() == Wingman.Formation.WIDE;
            contact |= happened(sortie, SimEvents.Type.WINGMAN_HIT);
            for (int i = 0; i < sortie.enemyCount(); i++) {
                closest = Math.min(closest, gap(rook, sortie.enemy(i)));
                steps++;
            }
        }

        assertTrue(steps > 0 && wide);
        assertTrue(sortie.flying());
        assertFalse(contact, "no contact with the Mantis");
        assertTrue(closest >= 0, "his hit box never touches its body: " + closest);
        assertEquals(WingmanSpec.Side.LEFT, rook.side(), "his own side");
    }

    @Test
    void heKeepsFortyPixelsFromThePlayerWhileHeSwapsSidesInABottomCorner() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < 360; i++) {
            sortie.step(i < 180 ? Command.LEFT.bit() | Command.DOWN.bit() : Command.RIGHT.bit() | Command.DOWN.bit());
            if (i == 179) {
                assertEquals(WingmanSpec.Side.RIGHT, rook.side(), "mirrored in the bottom left corner");
            }
            Ship ship = sortie.ship();
            closest = Math.min(closest, Math.hypot(rook.x() - ship.x(), rook.y() - ship.y()));
        }

        assertEquals(WingmanSpec.Side.LEFT, rook.side(), "his own side again in the bottom right corner");
        assertTrue(closest >= 40 - 1e-9, "never closer than 40 px: " + closest);
    }

    /**
     * Level 08's capture (round 30): low on the screen, back from the left wall, his way from the
     * mirrored slot to his own passed beneath the player, where the bottom margin leaves no room at
     * his minimum distance; pushed back he stayed beneath the player to the level's end. He goes
     * round over the player instead.
     */
    @Test
    void heGoesRoundOverThePlayerToHisOwnSlotWhenTheWayBeneathIsClosed() {
        Sortie sortie = sortie(TestSpecs.level(60, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 180, Command.LEFT.bit() | Command.DOWN.bit());
        assertEquals(WingmanSpec.Side.RIGHT, rook.side(), "mirrored at the left wall");
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < 300 && sortie.ship().x() < Ship.START_X; i++) {
            sortie.step(Command.RIGHT.bit() | Command.DOWN.bit());
        }
        for (int i = 0; i < 240; i++) {
            sortie.step(Command.DOWN.bit());
            Ship ship = sortie.ship();
            closest = Math.min(closest, Math.hypot(rook.x() - ship.x(), rook.y() - ship.y()));
        }

        Ship ship = sortie.ship();
        assertTrue(ship.y() < 40 + 28 + 12 + 21, "the way beneath him is closed: " + ship.y());
        assertEquals(WingmanSpec.Side.LEFT, rook.side());
        assertEquals(Wingman.Formation.WING, rook.formation());
        assertEquals(ship.x() - 64, rook.x(), 1, "in his own Wing slot, not beneath the player");
        assertTrue(closest >= 40 - 1e-9, "never closer than 40 px: " + closest);
    }

    @Test
    void aBulletTooCloseHitsHimAndHisLowArmourIsCalledOnce() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 10, Command.NONE);
        shoot(sortie, 30);
        boolean hit = false;
        for (int i = 0; i < 5; i++) {
            sortie.step(Command.NONE);
            hit |= happened(sortie, SimEvents.Type.WINGMAN_HIT);
        }
        assertTrue(hit);
        assertEquals(50, rook.armour());
        assertEquals(TestSpecs.FULL_ARMOUR, sortie.ship().defences().armour(), "the player is not hurt");

        shoot(sortie, 30);
        boolean critical = false;
        for (int i = 0; i < 5; i++) {
            sortie.step(Command.NONE);
            critical |= happened(sortie, SimEvents.Type.WINGMAN_CRITICAL);
        }
        assertTrue(critical);
        assertTrue(rook.wasCritical());
        assertEquals(20, rook.armour());
    }

    /** Ground units in a column straight ahead of him, out of the player's line of fire. */
    private static LevelScript groundAheadOfRook() {
        EnemySpec husk = new EnemySpec(
                "husk",
                4,
                new Hitbox(16, 16),
                Layer.GROUND,
                6,
                false,
                10,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false);
        double x = Ship.START_X - 64;
        return new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(30, 130)),
                List.of(),
                List.of(),
                List.of(
                        new LevelScript.GroundUnit(0.5, x, husk, -1),
                        new LevelScript.GroundUnit(1.0, x, husk, -1),
                        new LevelScript.GroundUnit(1.5, x, husk, -1)),
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
    }

    @Test
    void hisKillsPayLikeThePlayersAndChain() {
        Sortie sortie = sortie(groundAheadOfRook(), WingmanSpec.Side.LEFT);
        int destroyed = 0;
        for (int i = 0; i < 300; i++) {
            sortie.step(Command.FIRE.bit());
            destroyed += happened(sortie, SimEvents.Type.ENEMY_DESTROYED) ? 1 : 0;
        }

        assertEquals(3, destroyed);
        assertEquals(3, sortie.kills());
        assertTrue(sortie.credits() > 0, "bounty: " + sortie.credits());
        assertTrue(sortie.score() > 0);
        assertTrue(sortie.chain() >= 2, "chain: " + sortie.chain());
    }

    /**
     * User decision 2026-10-07: he aims his Mortar. A Hive Node-sized hardened target scrolls down the
     * player's column, 64 px beside his Wing slot: his shells land on it (a lob of the full 200 px ahead
     * of him would land 64 px beside it, out of its blast's and its snap's reach), each on an arc as
     * short as the distance, and only his shells hurt it (the player's Pulse Cannon glances off).
     */
    @Test
    void heLobsHisMortarOntoTheGroundTargetThePlayerIsOver() {
        double hp = 1_000_000;
        LevelScript level = PartCSpecs.level(
                20, 60, List.of(), List.of(new LevelScript.GroundUnit(0, Ship.START_X, PartCSpecs.nodeTarget(hp), -1)));
        WingmanSpec rook = new WingmanSpec(WingmanSpec.Side.LEFT, 80, CRAFT, AI, PartCSpecs.mortar());
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT.withWingman(rook),
                level,
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        int aimed = 0;
        for (int i = 0; i < SimStep.ticks(12); i++) {
            sortie.step(Command.FIRE.bit());
            Enemy node = PartCSpecs.find(sortie, "node");
            for (int s = 0; s < sortie.shotCount(); s++) {
                Shot shot = sortie.shot(s);
                if (shot.mount() != sortie.wingmanMount() || node == null) {
                    continue;
                }
                if (Math.abs(shot.landX() - node.x()) < 1e-9 && Math.abs(shot.landY() - node.y()) < 1e-9) {
                    aimed++;
                    assertTrue(shot.arc() > 0 && shot.arc() <= 1, "a lob no longer than the range: " + shot.arc());
                } else {
                    assertTrue(
                            Math.hypot(shot.landX() - node.x(), shot.landY() - node.y()) > 56,
                            "a shell not aimed at it lands clear of it");
                }
            }
        }

        assertTrue(aimed > 0, "his shells land on the node");
        Enemy node = PartCSpecs.find(sortie, "node");
        double damage = hp - (node == null ? 0 : node.hp());
        assertTrue(damage >= 12 * 5, "his shells hurt it: " + damage);
    }

    @Test
    void heFiresOnlyWhileThePlayerFires() {
        Sortie sortie = sortie(groundAheadOfRook(), WingmanSpec.Side.LEFT);
        run(sortie, 300, Command.NONE);

        assertEquals(0, sortie.kills());
        assertEquals(Integer.MAX_VALUE, wingman(sortie).ticksSinceShot());
        assertEquals(0, sortie.shotCount());
    }

    @Test
    void heFiresWheneverThePlayerFiresEvenWithoutATargetAtHisGunsRate() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 120, Command.NONE);
        int volleys = 0;
        int seconds = 10;
        for (int i = 0; i < seconds * SimStep.PER_SECOND; i++) {
            sortie.step(Command.FIRE.bit());
            assertEquals(-1, rook.target(), "nothing to aim at");
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.SHOT_FIRED && events.value(e) == sortie.wingmanMount()) {
                    volleys++;
                }
            }
        }

        // His gun's rate, 5 volleys/s, while the player fires: not only when a target is in his cone.
        assertEquals(GUN.rate() * seconds, volleys, 1);
    }

    @Test
    void heMarksHisShotsWithTheMountPastTheArmament() {
        Sortie sortie = sortie(groundAheadOfRook(), WingmanSpec.Side.LEFT);
        boolean his = false;
        for (int i = 0; i < 120 && !his; i++) {
            sortie.step(Command.FIRE.bit());
            for (int s = 0; s < sortie.shotCount(); s++) {
                his |= sortie.shot(s).mount() == sortie.wingmanMount();
            }
        }

        assertEquals(sortie.armament().size(), sortie.wingmanMount());
        assertTrue(his);
    }

    @Test
    void atZeroArmourHeEjectsWithoutFailingTheLevelAndARetryBringsHimBack() {
        Sortie sortie = sortie(TestSpecs.level(30, List.of()), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 10, Command.NONE);
        boolean ejected = false;
        for (int k = 0; k < 3 && !rook.ejected(); k++) {
            shoot(sortie, 30);
            for (int i = 0; i < 5; i++) {
                sortie.step(Command.NONE);
                ejected |= happened(sortie, SimEvents.Type.WINGMAN_EJECTED);
            }
        }
        assertTrue(ejected);
        assertTrue(rook.ejected());
        assertEquals(0, rook.armour());
        assertTrue(sortie.flying());
        assertFalse(sortie.primaryFailed());
        double pod = rook.podX(0);
        run(sortie, 30, Command.FIRE.bit());
        assertTrue(rook.podX(0) < pod, "the pod drifts to the nearer (left) edge");
        assertEquals(Integer.MAX_VALUE, rook.ticksSinceShot(), "out of the fight");

        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);
        assertFalse(rook.ejected());
        assertEquals(80, rook.armour());

        sortie.retry(TestSpecs.FULL_ARMOUR, 40);
        sortie.step(Command.NONE);
        assertEquals(40, rook.armour());
    }

    @Test
    void theBossCheckpointKeepsHisArmourAndARetryFromBossBringsHimBackInIt() {
        Sortie sortie = sortie(TestSpecs.carrierLevel(TestSpecs.carrier(false, 2)), WingmanSpec.Side.LEFT);
        Wingman rook = wingman(sortie);
        run(sortie, 5, Command.NONE);
        shoot(sortie, 10);
        double before = rook.armour();
        while (!sortie.bossCheckpoint()) {
            before = rook.armour();
            sortie.step(Command.NONE);
        }
        assertTrue(before < 80, "hit before the checkpoint: " + before);
        for (int k = 0; k < 5 && !rook.ejected(); k++) {
            shoot(sortie, 40);
            run(sortie, 5, Command.NONE);
        }
        assertTrue(rook.ejected());

        sortie.retryFromBoss();
        sortie.step(Command.NONE);

        assertFalse(rook.ejected());
        assertEquals(before, rook.armour());
    }

    @Test
    void theSameRunGivesTheSameHashAndARunWithoutHimKeepsItsOwn() {
        long first = hash(true);
        assertEquals(first, hash(true));
        assertNotEquals(first, hash(false));
        Sortie plain =
                new Sortie(3, TestSpecs.LOADOUT, SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        assertTrue(plain.wingman().isEmpty());
    }

    private static long hash(boolean withRook) {
        Loadout loadout = withRook ? TestSpecs.LOADOUT.withWingman(rook(WingmanSpec.Side.RIGHT)) : TestSpecs.LOADOUT;
        Sortie sortie = new Sortie(3, loadout, SortieTest.mixedLevel(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        for (int i = 0; i < 3000; i++) {
            sortie.step(SortieTest.Pilot.commands(i));
        }
        return sortie.stateHash();
    }

    @Test
    void steppingWithHimDoesNotAllocate() {
        long allocated = Allocations.least(
                () -> {
                    var sortie = new Sortie(
                            3,
                            TestSpecs.LOADOUT.withWingman(rook(WingmanSpec.Side.LEFT)),
                            SortieTest.mixedLevel(),
                            TestSpecs.RULES,
                            TestSpecs.FULL_ARMOUR);
                    for (int i = 0; i < 600; i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                    return sortie;
                },
                sortie -> {
                    for (int i = 600; i < 600 + 3600; i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "3600 steps allocated " + allocated + " bytes");
    }
}
