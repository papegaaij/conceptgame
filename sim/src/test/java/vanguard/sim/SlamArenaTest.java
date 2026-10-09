package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2c: an arena boss's anchored arrival, lanes, slams and surfacing ({@link SlamArena};
 * design/enemies/bosses/harbour-kraken; user decisions E5 = a and E7 = a and the stated defaults of
 * 2026-10-08), on a test Kraken shaped like the data's: a head 152 px below the platform's centre at y 110 (since round 33 the data's
 * head lies 216 px below a platform centre at y 78: about the same place on the screen) with
 * two eyes, two slam arms owning lanes 1–2 and 3–4, the arena from 3 s to 4 s at speed 0.
 */
class SlamArenaTest {
    static final int HEAD = 0;
    static final int LEFT_ARM = 1;
    static final int RIGHT_ARM = 2;
    static final double ARRIVE = 3;
    static final double SPEED = 140;
    private static final double EYE_X = PlayField.WIDTH / 2.0 - 41.5;

    static final EnemyGun FAN = new EnemyGun(
            2, 0, 1, 140, 6, false, 7, Math.toRadians(70), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /** The test Kraken: medium has two lanes a volley, hard three ({@code volleyLanes}). */
    static LevelScript.SetPieceSpec kraken(int volleyLanes) {
        var head = new LevelScript.PartSpec(
                "head", 0, -152, new Hitbox(130, 200), 100, true, 124, Optional.empty(), 0, 0.5);
        var left = new LevelScript.PartSpec(
                "left arm", -54, -262, new Hitbox(26, 26), 30, false, 31, Optional.empty(), 0, 1);
        var right = new LevelScript.PartSpec(
                "right arm", 54, -262, new Hitbox(26, 26), 30, false, 31, Optional.empty(), 0, 1);
        var fan = new BossSpec.Attack("beak-fan", BossSpec.Pattern.FAN, FAN, 0.15, false, 7, 0, 0, 0, List.of(HEAD));
        var slams = new BossSpec.Phase(
                "Slams",
                List.of(),
                0,
                List.of(),
                false,
                Optional.empty(),
                List.of(),
                Double.NaN,
                20,
                1,
                Optional.empty(),
                Optional.empty(),
                new BossSpec.PhaseArena(
                        3,
                        List.of(),
                        Double.NaN,
                        BossSpec.Slamming.CHAIN,
                        0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
        var headUp = new BossSpec.Phase(
                "Head up",
                List.of(),
                0,
                List.of(0),
                false,
                Optional.empty(),
                List.of(),
                Double.NaN,
                Double.POSITIVE_INFINITY,
                0,
                Optional.empty(),
                Optional.empty(),
                new BossSpec.PhaseArena(
                        0,
                        List.of(HEAD),
                        0.4,
                        BossSpec.Slamming.AFTER_DIVE,
                        0,
                        Optional.of(new BossSpec.Surface(HEAD, 2, 8, 1.5, 0.5, false)),
                        Optional.of(new BossSpec.Release(TestSpecs.SKITTER, 6, List.of(1, 4))),
                        Optional.empty()));
        var twoLanes = new BossSpec.Phase(
                "Two lanes",
                List.of(HEAD),
                0,
                List.of(0),
                false,
                Optional.empty(),
                List.of(),
                Double.NaN,
                Double.POSITIVE_INFINITY,
                1,
                Optional.empty(),
                Optional.empty(),
                new BossSpec.PhaseArena(
                        0,
                        List.of(),
                        Double.NaN,
                        BossSpec.Slamming.VOLLEY,
                        4,
                        Optional.of(new BossSpec.Surface(HEAD, 2, 0, 0, 0.5, true)),
                        Optional.empty(),
                        Optional.of(PickupType.SHIELD_CELL)));
        var arena = new BossSpec.Arena(
                new BossSpec.Lanes(
                        4,
                        120,
                        PlayField.HEIGHT - 110 - 76,
                        List.of(new BossSpec.Arm(LEFT_ARM, 0b110), new BossSpec.Arm(RIGHT_ARM, 0b11000))),
                new BossSpec.Slam(1, 0.5, 1.5, 0.6, 10, 6, 120, 4, volleyLanes, 0.5),
                List.of(Layer.SUB, Layer.SUB, Layer.SUB),
                List.of(
                        new BossSpec.Spot("left eye", HEAD, -41.5, -52.5, new Hitbox(28, 28), 2, true),
                        new BossSpec.Spot("right eye", HEAD, 41.5, -52.5, new Hitbox(28, 28), 2, true)));
        return new LevelScript.SetPieceSpec(
                "test-kraken",
                new Hitbox(344, 380),
                new Hitbox(208, 152),
                25,
                List.of(head, left, right),
                List.of(),
                Optional.empty(),
                Optional.of(new BossSpec(
                        ARRIVE,
                        PlayField.WIDTH / 2.0,
                        PlayField.HEIGHT - 110,
                        0,
                        0,
                        1,
                        Layer.GROUND,
                        true,
                        "TEST KRAKEN",
                        60,
                        List.of(
                                new BossSpec.Chain(
                                        "left slam arm", -12.4, -269, LEFT_ARM, 13, new Hitbox(30, 30), 0.1, 0, true),
                                new BossSpec.Chain(
                                        "right slam arm", 12.4, -269, RIGHT_ARM, 13, new Hitbox(30, 30), 0.1, 0, true)),
                        List.of(fan),
                        List.of(slams, headUp, twoLanes),
                        false,
                        List.of(),
                        List.of(),
                        0,
                        Optional.of(arena))));
    }

    /** The arena level: 3 s at 140 px/s, the arena of 1 s at speed 0, 10 s after it; with a naval convoy when asked. */
    static LevelScript level(int volleyLanes, boolean convoy) {
        LevelScript level = new LevelScript(
                        11,
                        2,
                        0,
                        List.of(
                                new LevelScript.Section(ARRIVE, SPEED),
                                new LevelScript.Section(ARRIVE + 1, 0, true),
                                new LevelScript.Section(ARRIVE + 11, SPEED)),
                        List.of(),
                        List.of(),
                        List.of(),
                        0,
                        List.of(),
                        LevelScript.Secondary.NONE,
                        List.of(),
                        List.of(),
                        List.of(kraken(volleyLanes)))
                .withWater(true);
        return convoy ? level.withConvoy(NavalConvoyTest.convoy(NavalConvoyTest.CARGO)) : level;
    }

    private static Sortie sortie(LevelScript level, Rules rules) {
        return new Sortie(1, TestSpecs.LOADOUT, level, rules, TestSpecs.FULL_ARMOUR);
    }

    private static SetPiece boss(Sortie sortie) {
        return sortie.setPiece(0);
    }

    private static SlamArena arena(Sortie sortie) {
        return boss(sortie).arena().orElseThrow();
    }

    /** Steps until {@code type} happens (returns its values in that step), at most {@code seconds}. */
    private static List<Integer> until(Sortie sortie, SimEvents.Type type, double seconds, int commands) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(commands);
            List<Integer> values = values(sortie, type);
            if (!values.isEmpty()) {
                return values;
            }
        }
        throw new AssertionError(type + " within " + seconds + " s");
    }

    private static List<Integer> values(Sortie sortie, SimEvents.Type type) {
        List<Integer> values = new ArrayList<>();
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == type) {
                values.add(events.value(i));
            }
        }
        return values;
    }

    private static void steps(Sortie sortie, double seconds) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(0);
        }
    }

    @Test
    void itScrollsInWithTheGroundAndEngagesWhenTheScrollHalts() {
        Sortie sortie = sortie(level(2, false), TestSpecs.RULES);
        SetPiece boss = boss(sortie);
        boolean seen = false;
        while (sortie.levelSeconds() < ARRIVE - SimStep.SECONDS) {
            sortie.step(0);
            seen |= boss.approaching();
            assertFalse(boss.present(), "no bar before it arrives");
        }
        assertTrue(seen, "it scrolls in before it arrives");
        assertEquals(List.of(0), until(sortie, SimEvents.Type.BOSS_ARRIVED, 0.1, 0));
        assertTrue(sortie.bossCheckpoint(), "the checkpoint at its arrival");
        assertTrue(boss.partShielded(HEAD), "invulnerable while the scroll eases");
        assertTrue(sortie.groundSpeed() < SPEED, "the scroll eases into the halt");
        until(sortie, SimEvents.Type.BOSS_SETTLED, 1.1, 0);
        assertEquals(PlayField.HEIGHT - 110, boss.y(), 1e-6, "the platform's centre 110 px below the top edge");
        assertEquals(ARRIVE * SPEED + Sortie.easeDistance(SPEED), sortie.groundScroll(), 1e-6);
        assertEquals(68.83, Sortie.easeDistance(SPEED), 0.01, "about half a second at 140 px/s");
        assertTrue(boss.engaged());
        assertFalse(boss.partShielded(HEAD), "a torpedo can reach the submerged head");
        assertEquals(Layer.SUB, boss.partLayer(HEAD));
        sortie.step(0);
        assertEquals(0, sortie.groundSpeed(), "halted");
        assertTrue(sortie.arenaHalted());
    }

    @Test
    void aSlamTelegraphsItsLaneRisesStrikesLiesAwashAndSinks() {
        Sortie sortie = sortie(level(2, false), TestSpecs.RULES);
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        SlamArena arena = arena(sortie);
        // The ship's start x 240 lies in lane 3: the right arm's.
        assertEquals(3, arena.laneOf(sortie.ship().x()));
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0), "the ship's lane first");
        assertEquals(SlamArena.ArmState.TELEGRAPH, arena.armState(1));
        assertEquals(0b1000, arena.telegraphed());
        assertEquals(Layer.SUB, boss(sortie).partLayer(RIGHT_ARM), "still under the water");
        steps(sortie, 1);
        assertEquals(SlamArena.ArmState.RISE, arena.armState(1));
        assertEquals(Layer.GROUND, boss(sortie).partLayer(RIGHT_ARM), "out of the water from its rise");
        assertEquals(3, arena.armLane(1));
        assertEquals(300, boss(sortie).partX(RIGHT_ARM), 1e-9, "laid along lane 3");
        assertEquals(SlamArena.TIP_MARGIN, boss(sortie).partY(RIGHT_ARM), 1e-9);
        Defences defences = sortie.ship().defences();
        double before = defences.shield() + defences.armour();
        assertEquals(List.of(3), until(sortie, SimEvents.Type.SLAM, 0.6, 0));
        assertEquals(before - 10, defences.shield() + defences.armour(), 1e-9, "`heavy` on the ship in the lane");
        assertEquals(0, arena.telegraphed(), "the lane is safe after its impact");
        sortie.step(0);
        assertEquals(SlamArena.ArmState.AWASH, arena.armState(1));
        steps(sortie, 1.5);
        assertEquals(SlamArena.ArmState.SINK, arena.armState(1));
        assertEquals(Layer.SUB, boss(sortie).partLayer(RIGHT_ARM));
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 0.7, 0), "the next when it has sunk");
    }

    @Test
    void aShipOutsideTheLaneIsSparedAndSplashFliesFromAlongTheArm() {
        Sortie sortie = sortie(level(2, false), TestSpecs.RULES);
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        for (int i = 0; i < SimStep.ticks(0.3); i++) {
            sortie.step(Command.LEFT.bit());
        }
        // The ship is in lane 2 now: the left arm slams it.
        assertEquals(2, arena(sortie).laneOf(sortie.ship().x()));
        assertEquals(List.of(2), until(sortie, SimEvents.Type.TELEGRAPH, 1, 0));
        for (int i = 0; i < SimStep.ticks(0.5); i++) {
            sortie.step(Command.RIGHT.bit());
        }
        Defences defences = sortie.ship().defences();
        double before = defences.shield() + defences.armour();
        int bullets = sortie.bulletCount();
        until(sortie, SimEvents.Type.SLAM, 1.2, 0);
        assertTrue(sortie.ship().x() - 22 > 240, "out of lane 2");
        assertEquals(before, defences.shield() + defences.armour(), 1e-9, "the ship outside the lane is spared");
        assertTrue(sortie.bulletCount() > bullets, "the splash");
    }

    @Test
    void slamsAlternateTheShipsLaneAndTheNearestConvoyShipsAndHitTheShipInIt() {
        Sortie sortie = sortie(level(2, true), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0), "the ship's lane");
        // Halvorsen in lane 1 (x 60), Mbeki in 2 (180), Saint-Laurent in 4 (420): 180 is nearest x 240.
        assertEquals(List.of(2), until(sortie, SimEvents.Type.TELEGRAPH, 4, 0), "the nearest ship's lane");
        assertEquals(List.of(2), until(sortie, SimEvents.Type.SLAM, 2, 0));
        assertEquals(List.of(1), values(sortie, SimEvents.Type.ALLY_HIT), "Mbeki takes the hit");
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 4, 0));
        until(sortie, SimEvents.Type.SLAM, 2, 0);
        assertEquals(List.of(1), until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0), "phase 1 ends on its third slam");
    }

    @Test
    void aSeveredArmMakesItsHalfSafe() {
        Sortie sortie = sortie(level(2, true), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        sortie.destroyPart(0, RIGHT_ARM);
        assertTrue(boss(sortie).partWrecked(RIGHT_ARM));
        // The ship's lane 3 is the severed arm's: the other choice (Mbeki's lane 2) lies in the living half.
        assertEquals(List.of(2), until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0));
        for (int i = 0; i < 2; i++) {
            List<Integer> lanes = until(sortie, SimEvents.Type.TELEGRAPH, 5, 0);
            assertTrue(lanes.stream().allMatch(lane -> lane <= 2), "only the living half: " + lanes);
        }
    }

    @Test
    void anArmSeveredMidCycleStopsAndBothSeveredEndPhaseOne() {
        Sortie sortie = sortie(level(2, false), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0);
        steps(sortie, 1.2);
        sortie.destroyPart(0, RIGHT_ARM);
        sortie.step(0);
        assertEquals(SlamArena.ArmState.IDLE, arena(sortie).armState(1));
        assertEquals(0, arena(sortie).telegraphed() & 0b1000, "no impact from a severed arm");
        assertEquals(
                List.of(2),
                values(sortie, SimEvents.Type.TELEGRAPH),
                "the living arm slams its half's lane nearest the ship");
        sortie.destroyPart(0, LEFT_ARM);
        assertEquals(List.of(1), until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0));
    }

    @Test
    void theHeadSurfacesReleasesTheFieldOpensItsEyesFansAndDivesThenOneSlam() {
        Sortie sortie = sortie(level(2, false), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_PHASE, 20, 0);
        SetPiece boss = boss(sortie);
        SlamArena arena = arena(sortie);
        assertEquals(List.of(HEAD), values(sortie, SimEvents.Type.SURFACE), "it surfaces as phase 2 begins");
        assertEquals(6, sortie.enemyCount(), "the field released at the first surfacing");
        int enemies = sortie.enemyTotal();
        assertEquals(Layer.SUB, boss.partLayer(HEAD));
        steps(sortie, 1);
        assertEquals(Layer.GROUND, boss.partLayer(HEAD), "the layer flips at the rise's middle");
        assertFalse(arena.open(HEAD));
        steps(sortie, 1);
        assertTrue(arena.open(HEAD), "the eyes open");
        assertTrue(arena.glowing(), "the beak's tell");
        until(sortie, SimEvents.Type.ENEMY_FIRED, 0.6, 0);
        assertFalse(arena.glowing());

        // A shot from below meets the head's box at its lower edge; under an eye it flies on to it.
        double edge = boss.partY(HEAD) - 100;
        double hp = boss.partHp(HEAD);
        boss.damagePartAt(HEAD, 4, EYE_X, edge, 0, 1);
        assertEquals(hp - 8, boss.partHp(HEAD), 1e-9, "an open eye takes ×2");
        boss.damagePartAt(HEAD, 4, PlayField.WIDTH / 2.0, edge, 0, 1);
        assertEquals(hp - 10, boss.partHp(HEAD), 1e-9, "the mantle takes ×0.5");
        boss.damagePartAt(HEAD, 4, EYE_X, boss.partY(HEAD) - 52.5, 0, 0);
        assertEquals(hp - 18, boss.partHp(HEAD), 1e-9, "a blast in the eye");

        assertEquals(List.of(HEAD), until(sortie, SimEvents.Type.DIVE, 8, 0));
        assertFalse(arena.open(HEAD));
        boss.damagePartAt(HEAD, 4, EYE_X, edge, 0, 1);
        assertEquals(hp - 20, boss.partHp(HEAD), 1e-9, "the shut eyes are mantle");
        assertEquals(1, until(sortie, SimEvents.Type.TELEGRAPH, 1.6, 0).size(), "one slam after the dive");
        assertEquals(Layer.SUB, boss.partLayer(HEAD));
        assertEquals(
                List.of(HEAD), until(sortie, SimEvents.Type.SURFACE, 4, 0), "it surfaces again once the arm has sunk");
        assertEquals(enemies, sortie.enemyTotal(), "the field comes once");
    }

    /** Phase 3: the head's HP under 40 %, from phase 2. */
    private static Sortie phaseThree(int volleyLanes) {
        Sortie sortie = sortie(level(volleyLanes, true), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_PHASE, 20, 0);
        // The third slam's arm sinks first (a busy arm sits a volley out).
        while (arena(sortie).armState(0) != SlamArena.ArmState.IDLE
                || arena(sortie).armState(1) != SlamArena.ArmState.IDLE) {
            sortie.step(0);
        }
        boss(sortie).damagePart(HEAD, 140);
        assertEquals(List.of(2), until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0));
        return sortie;
    }

    @Test
    void phaseThreeDropsAShieldCellKeepsTheHeadUpAndSlamsBothHalves() {
        Sortie sortie = phaseThree(2);
        boolean cell = false;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            cell |= sortie.pickup(i).type() == PickupType.SHIELD_CELL;
        }
        assertTrue(cell, "the shield cell as it begins");
        // The ship's lane 3 (right arm) and Mbeki's lane 2 (left arm), at once.
        assertEquals(
                List.of(2, 3),
                until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0).stream()
                        .sorted()
                        .toList());
        until(sortie, SimEvents.Type.SLAM, 1.6, 0);
        assertEquals(2, values(sortie, SimEvents.Type.SLAM).size(), "both strike at once");
        steps(sortie, 1);
        assertTrue(arena(sortie).open(HEAD), "it stays up");
        assertEquals(2, until(sortie, SimEvents.Type.TELEGRAPH, 4, 0).size(), "every 4 s");
        assertTrue(arena(sortie).open(HEAD));
    }

    @Test
    void hardsThirdLaneIsASecondSlamOfTheArmWhoseHalfHoldsTwo() {
        Sortie sortie = phaseThree(3);
        // The ship's lane 3; Mbeki's lane 2; the next-nearest ship: Halvorsen (lane 1) and Saint-Laurent
        // (lane 4) are both 180 px away, the lower lane wins. The left arm holds 2 and 1.
        List<Integer> lanes = until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0);
        assertEquals(List.of(3, 2, 1), lanes.stream().sorted((a, b) -> b - a).toList());
        assertEquals(2, until(sortie, SimEvents.Type.SLAM, 1.6, 0).size(), "lanes 3 and 2 first");
        steps(sortie, 0.5 - SimStep.SECONDS);
        assertEquals(List.of(1), until(sortie, SimEvents.Type.SLAM, 0.1, 0), "lane 1 0.5 s later");
    }

    @Test
    void rookKeepsOutOfATelegraphedLane() {
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT.withWingman(WingmanTest.rook(WingmanSpec.Side.LEFT)),
                level(2, false),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        // His Wing slot lies 64 px left of the ship, in lane 2; the first slam is the ship's lane 3.
        for (int slam = 0; slam < 3; slam++) {
            List<Integer> lanes = until(sortie, SimEvents.Type.TELEGRAPH, 5, 0);
            int lane = lanes.getFirst();
            until(sortie, SimEvents.Type.SLAM, 1.6, 0);
            Wingman rook = sortie.wingman().orElseThrow();
            double half = rook.spec().craft().hitbox().width() / 2;
            assertTrue(
                    rook.x() + half <= (lane - 1) * 120 || rook.x() - half >= lane * 120,
                    "Rook beside lane " + lane + " at its impact: x " + rook.x());
        }
    }

    @Test
    void aSlamHitsRookInItsLane() {
        // A Rook too slow to leave: his Right slot (64 px right of the ship's 240) lies in lane 3.
        WingmanSpec.Craft c = WingmanTest.CRAFT;
        var slow = new WingmanSpec.Craft(
                c.size(),
                c.maxArmour(),
                c.hitbox(),
                5,
                c.accelerationSeconds(),
                c.minDistance(),
                c.edgeGap(),
                c.ramDamage(),
                c.lowArmour(),
                c.podSpeed(),
                c.bankStepTicks());
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT.withWingman(
                        new WingmanSpec(WingmanSpec.Side.RIGHT, 80, slow, WingmanTest.AI, WingmanTest.GUN)),
                level(2, false),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        Wingman rook = sortie.wingman().orElseThrow();
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0));
        double armour = rook.armour();
        until(sortie, SimEvents.Type.SLAM, 1.6, 0);
        assertTrue(rook.x() > 240 && rook.x() < 360, "still in lane 3: " + rook.x());
        assertEquals(armour - 10, rook.armour(), 1e-9, "`heavy` on Rook in the lane");
    }

    @Test
    void retryFromBossBringsItBackUnharmed() {
        Sortie sortie = sortie(level(2, true), TestSpecs.RULES.withInvulnerableShip());
        until(sortie, SimEvents.Type.BOSS_PHASE, 20, 0);
        sortie.retryFromBoss();
        sortie.step(0);
        SetPiece boss = boss(sortie);
        assertEquals(0, boss.phase());
        assertFalse(boss.engaged());
        assertEquals(Layer.SUB, boss.partLayer(HEAD));
        until(sortie, SimEvents.Type.BOSS_SETTLED, 2, 0);
        assertEquals(PlayField.HEIGHT - 110, boss.y(), 1e-6, "halted at the same place");
        assertEquals(List.of(3), until(sortie, SimEvents.Type.TELEGRAPH, 1.1, 0));
    }

    @Test
    void theFightIsDeterministicAndAllocatesNothing() {
        Sortie a = sortie(level(3, true), TestSpecs.RULES);
        Sortie b = sortie(level(3, true), TestSpecs.RULES);
        for (int i = 0; i < SimStep.ticks(40); i++) {
            int commands = (i / 50) % 2 == 0 ? Command.LEFT.bit() | Command.FIRE.bit() : Command.RIGHT.bit();
            a.step(commands);
            b.step(commands);
        }
        assertEquals(a.stateHash(), b.stateHash());
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(level(3, true), TestSpecs.RULES.withInvulnerableShip());
                    steps(sortie, 4);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(40); i++) {
                        sortie.step(
                                Command.FIRE.bit() | ((i / 40) % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
                    }
                });
        assertEquals(0, allocated, "the slams, the surfacing and the release allocate nothing per step");
    }
}
