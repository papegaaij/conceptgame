package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Allocations;
import vanguard.sim.Armament;
import vanguard.sim.BossSpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WingmanSpec;

/**
 * M5 part H, step H4: the Siege Spire from its data file (design/enemies/bosses/siege-spire; decisions H1,
 * H3–H5 and H10 = a of 2026-10-11, pending the user's review) on a fixture arena: 3 s at 140 px/s, then the
 * arena at speed 0 where it arrives, then the daylight; the balance plan's Level 14 fit (the front at L4, two
 * Micro-missile Pods at L2, the Tail Gun at L2, Armour Mk III, Rook's Autocannon, the Airstrike's charges)
 * flown by the {@link Autopilot}, which shoots the lowest exposed part, sidesteps the lobs' markers, keeps
 * out of the sweep's reach and lines up over the maw's Wraiths for the rear gun. The fight's length at
 * medium is the one the HP are tuned to (H10 = a: a mean of about 135 s, every seed in the act-boss window
 * of 90–180 s) and the par.
 */
class SiegeSpireTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    static final String SLUG = "siege-spire";
    /** The rules of an Act 2 level. */
    private static final String RULES_LEVEL = Level13Test.LEVEL;

    static final double ARENA = 3;
    static final int POD_1 = 0;
    static final int MORTAR_LEFT = 4;
    static final int MORTAR_RIGHT = 5;
    static final int MAW = 6;
    static final int CORE = 7;
    static final int TRUNK = 8;
    static final List<Integer> ROOTS = List.of(0, 1, 2, 3, 4, 5);
    /** The seeds the autopilot's fight is measured on. */
    static final long[] SEEDS = {2185, 11, 77, 1301, 4242};

    static LevelScript.SetPieceSpec spire(Difficulty difficulty) {
        return SimSpecs.boss(CONTENT, new LevelData.BossPlacement(SLUG, ARENA, PlayField.WIDTH / 2.0, 2), difficulty);
    }

    static LevelScript level(Difficulty difficulty) {
        return new LevelScript(
                14,
                2,
                0,
                List.of(
                        new LevelScript.Section(ARENA, 140),
                        new LevelScript.Section(ARENA + 200, 0, true),
                        new LevelScript.Section(ARENA + 212, 140)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(),
                LevelScript.Secondary.NONE,
                List.of(),
                List.of(),
                List.of(spire(difficulty)));
    }

    /**
     * The balance plan's fit at Level 14 (design/player/balance-plan.yaml: Level 13's with the Tail Gun at
     * L2; Rook's Autocannon; the Airstrike's charges at their most, 4).
     */
    static Loadout planLoadout(Content content, Difficulty difficulty) {
        return SimSpecs.loadout(
                        content,
                        content.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "micro-missile-pod", 2),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "micro-missile-pod", 2),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 2)),
                        content.shields().models().get(1).name(),
                        content.armour().plating().get(2).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        content,
                        "autocannon",
                        1,
                        WingmanSpec.Side.LEFT,
                        content.wingmen().rook().armour()))
                .withSpecial(SimSpecs.special(content, "Airstrike", 4));
    }

    static Sortie sortie(long seed, Difficulty difficulty) {
        Loadout loadout = planLoadout(CONTENT, difficulty);
        return new Sortie(
                seed,
                loadout,
                level(difficulty),
                SimSpecs.rules(CONTENT, RULES_LEVEL, difficulty),
                loadout.plating().maxArmour());
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

    /** What one autopilot run of the fight saw. */
    record Fight(
            double seconds,
            boolean killed,
            double armourLost,
            double rootsSeconds,
            double mawSeconds,
            boolean lifted,
            int launched,
            int lobs,
            int sweeps,
            double sweepDamage,
            boolean flying,
            double[] lostByPhase,
            double lobDamage) {
        @Override
        public String toString() {
            return String.format(
                    "%.1f s%s, armour lost %.0f, roots %.1f s, maw %.1f s, lifted %b, launched %d, lobs %d, sweeps %d"
                            + " (hit %.0f), lost by phase %.0f/%.0f/%.0f (lob impacts %.0f)",
                    seconds,
                    killed ? "" : " NOT KILLED",
                    armourLost,
                    rootsSeconds,
                    mawSeconds - rootsSeconds,
                    lifted,
                    launched,
                    lobs,
                    sweeps,
                    sweepDamage,
                    lostByPhase[0],
                    lostByPhase[1],
                    lostByPhase[2],
                    lobDamage);
        }
    }

    /** The autopilot flies the fixture until the Spire dies (at most 4 minutes). */
    static Fight fight(long seed, Difficulty difficulty) {
        Sortie sortie = sortie(seed, difficulty);
        SetPiece spire = sortie.setPiece(0);
        double armour = sortie.ship().defences().armour();
        double roots = Double.NaN;
        double maw = Double.NaN;
        boolean lifted = false;
        int launched = 0;
        int lobs = 0;
        int sweeps = 0;
        double sweepDamage = 0;
        int present = 0;
        double[] lostByPhase = new double[3];
        double lobDamage = 0;
        for (int i = 0; i < SimStep.ticks(240) && !spire.destroyed(); i++) {
            present += spire.present() ? 1 : 0;
            double before =
                    sortie.ship().defences().shield() + sortie.ship().defences().armour();
            sortie.step(Autopilot.commands(sortie));
            double lost = Math.max(
                    0,
                    before
                            - sortie.ship().defences().shield()
                            - sortie.ship().defences().armour());
            if (!values(sortie, SimEvents.Type.SWEEP_HIT).isEmpty()) {
                sweepDamage += lost;
            }
            if (!values(sortie, SimEvents.Type.MORTAR_IMPACT).isEmpty()) {
                lobDamage += lost;
            }
            lostByPhase[Math.min(2, spire.phase())] += lost;
            if (spire.phase() >= 1 && Double.isNaN(roots)) {
                roots = present * SimStep.SECONDS;
            }
            if (spire.phase() >= 2 && Double.isNaN(maw)) {
                maw = present * SimStep.SECONDS;
            }
            lifted |= spire.lifted();
            launched += values(sortie, SimEvents.Type.BOSS_LAUNCHED).size();
            lobs += values(sortie, SimEvents.Type.MORTAR_LOBBED).size();
            sweeps += values(sortie, SimEvents.Type.SPIRE_SWEEP).size();
            if (!sortie.flying()) {
                break;
            }
        }
        return new Fight(
                spire.destroyed() ? spire.killSeconds() : present * SimStep.SECONDS,
                spire.destroyed(),
                armour - sortie.ship().defences().armour(),
                roots,
                maw,
                lifted,
                launched,
                lobs,
                sweeps,
                sweepDamage,
                sortie.flying(),
                lostByPhase,
                lobDamage);
    }

    @Test
    void theStatBlockMapsToAnAnchoredArenaBossWithoutLanes() {
        LevelScript.SetPieceSpec spec = spire(Difficulty.MEDIUM);
        BossSpec boss = spec.boss().orElseThrow();
        assertTrue(boss.anchored());
        assertFalse(boss.midBoss(), "the act boss's long bar");
        assertEquals("SIEGE SPIRE", boss.barName());
        assertEquals(PlayField.HEIGHT - 196, boss.hoverY(), "the anchor 196 px below the top edge (the concept's)");
        assertEquals(Layer.GROUND, boss.layer());
        assertEquals(
                List.of("pod 1", "pod 2", "pod 3", "pod 4", "mortar left", "mortar right", "maw", "core", "trunk"),
                spec.parts().stream().map(LevelScript.PartSpec::name).toList());
        assertEquals(List.of(TRUNK), boss.armoured(), "the trunk: shots glance off it, it stays as the stump");
        assertTrue(spec.parts().get(CORE).vital());
        assertEquals(2, spec.parts().get(MAW).multiplier(), "the lime maw ×2");
        assertEquals(2, spec.parts().get(CORE).multiplier(), "the lime core ×2");
        assertEquals(665, spec.parts().get(POD_1).hp(), "tuned to the measured fight (H10 = a)");
        assertEquals(775, spec.parts().get(MORTAR_LEFT).hp());
        assertEquals(2880, spec.parts().get(MAW).hp());
        assertEquals(4950, spec.parts().get(CORE).hp());
        assertEquals(130, boss.parSeconds(), "the par: the measured mean");

        BossSpec.Arena arena = boss.arena().orElseThrow();
        assertEquals(BossSpec.Lanes.NONE, arena.lanes(), "no slam lanes (H1 = a)");
        assertTrue(arena.partLayers().stream().allMatch(layer -> layer == Layer.GROUND), "rooted: all on the ground");

        BossSpec.Attack orbs = boss.attacks().get(0);
        assertEquals(BossSpec.Pattern.AIMED, orbs.pattern());
        assertTrue(orbs.stagger(), "each pod on its own clock");
        assertEquals(List.of(0, 1, 2, 3), orbs.parts());
        assertEquals(1.6, orbs.gun().intervalSeconds(), 1e-9);
        BossSpec.Attack acid = boss.attacks().get(1);
        assertEquals(BossSpec.Pattern.LOB, acid.pattern(), "the mortar roots' lob (H5 = a)");
        assertTrue(acid.stagger());
        assertEquals(List.of(MORTAR_LEFT, MORTAR_RIGHT), acid.parts());
        assertEquals(4, acid.gun().intervalSeconds(), 1e-9);
        assertEquals(1.0, acid.gun().mortar().orElseThrow().flightSeconds());
        assertEquals(10, acid.gun().mortar().orElseThrow().ring());
        assertEquals(10, acid.gun().damage(), "the direct hit `heavy`");
        BossSpec.Attack spit = boss.attacks().get(2);
        assertEquals(BossSpec.Pattern.FAN, spit.pattern());
        assertEquals(9, spit.gun().fan());
        BossSpec.Attack sweep = boss.attacks().get(3);
        assertEquals(BossSpec.Pattern.SWEEP, sweep.pattern(), "the free spire's laser sweep (H5 = a)");
        assertEquals(Math.toRadians(120), sweep.sweep().orElseThrow().arcRadians(), 1e-9);
        assertEquals(0.6, sweep.sweep().orElseThrow().telegraphSeconds());
        assertEquals(8, sweep.sweep().orElseThrow().damage(), "`laser`");
        assertEquals(List.of(), sweep.parts(), "fired by the part its phase ends on, the core");
        assertEquals(14, boss.attacks().get(4).count(), "the ring of 14");

        BossSpec.Phase roots = boss.phases().get(0);
        assertEquals(ROOTS, roots.untilParts());
        BossSpec.Phase maw = boss.phases().get(1);
        assertEquals(List.of(MAW), maw.exposes());
        BossSpec.Windows windows = maw.windows().orElseThrow();
        assertEquals(8, windows.everySeconds());
        BossSpec.Spawn wraiths = windows.spawns().getFirst();
        assertEquals("wraith", wraiths.enemy().slug());
        assertEquals(2, wraiths.count());
        assertEquals(4, wraiths.ambushMost(), "ambush launches, at most 4 alive (H3 = a)");
        BossSpec.Phase free = boss.phases().get(2);
        BossSpec.Move lift = free.move().orElseThrow();
        assertEquals(Layer.AIR, lift.layer(), "the core onto air (H4 = a)");
        assertEquals(3, lift.descendSeconds(), "the 3 s lift");
        assertEquals(PlayField.WIDTH / 2.0, lift.x());
        assertEquals(PlayField.HEIGHT - 120, lift.y(), "its station in the upper half");
        assertEquals(1, lift.pose());
        assertEquals("free", boss.poses().get(1).name());
        assertTrue(free.alternate(), "the sweep and the ring in turn");
        assertEquals(Optional.of(PickupType.ARMOUR_PATCH), free.arena().drop(), "the shockwave's patch");
        assertEquals(100, boss.sineAmplitude(), "the free spire's sway ±100 px");
        assertEquals(50, 2 * Math.PI * boss.sineAmplitude() / boss.sinePeriod(), 0.01, "at 50 px/s at most");

        BossSpec hard = spire(Difficulty.HARD).boss().orElseThrow();
        assertEquals(12, hard.attacks().get(1).gun().mortar().orElseThrow().ring(), "hard: the lob's ring of 12");
        assertEquals(
                Math.toRadians(180), hard.attacks().get(3).sweep().orElseThrow().arcRadians(), 1e-9);
        assertEquals(
                3,
                hard.phases().get(1).windows().orElseThrow().spawns().getFirst().count(),
                "hard: 3 Wraiths");
        assertEquals(665 * 1.3, spire(Difficulty.HARD).parts().get(POD_1).hp(), 1);
    }

    /**
     * H10 = a: the autopilot's fight with the plan's fit lasts about 135 s at medium (the bar appearing to the
     * kill, the par), inside the act-boss window 90–180 s on every seed; every phase happens (the roots, the
     * maw with its launches, the lift and the free spire's sweeps).
     */
    @Test
    void theAutopilotsFightLastsAboutAHundredAndThirtyFiveSecondsAtMedium() {
        double sum = 0;
        for (long seed : SEEDS) {
            Fight fight = fight(seed, Difficulty.MEDIUM);
            System.out.printf("Spire medium seed %d: %s%n", seed, fight);
            assertTrue(fight.killed(), "killed: " + fight);
            assertTrue(fight.seconds() >= 90 && fight.seconds() <= 180, "the act-boss window: " + fight);
            assertTrue(fight.lifted() && fight.launched() >= 2 && fight.sweeps() >= 1, "every phase: " + fight);
            sum += fight.seconds();
        }
        double mean = sum / SEEDS.length;
        double par = spire(Difficulty.MEDIUM).boss().orElseThrow().parSeconds();
        System.out.printf("Spire medium: mean %.1f s, par %.0f%n", mean, par);
        assertEquals(135, mean, 6, "H10 = a: about 135 s");
        assertEquals(mean, par, 2, "the par is the measured mean");
    }

    /**
     * Easy and hard are measured too (printed; the window is medium's): easy is won on every seed; on hard
     * (×1.3 HP, the ring of 12, the 180° sweep, 3 Wraiths a launch) the autopilot, a simple pilot, is worn
     * down on some seeds.
     */
    @ParameterizedTest
    @EnumSource(
            value = Difficulty.class,
            names = {"EASY", "HARD"})
    void theFightOnEasyAndHard(Difficulty difficulty) {
        double sum = 0;
        int killed = 0;
        for (long seed : SEEDS) {
            Fight fight = fight(seed, difficulty);
            System.out.printf("Spire %s seed %d: %s%n", difficulty, seed, fight);
            if (fight.killed()) {
                killed++;
                sum += fight.seconds();
            }
        }
        System.out.printf("Spire %s: %d of %d won, mean %.1f s%n", difficulty, killed, SEEDS.length, sum / killed);
        assertTrue(killed >= (difficulty == Difficulty.EASY ? SEEDS.length : 3), killed + " won");
    }

    @Test
    void theArenaIsAnchoredWhereTheScrollHaltsWithAllSixRootsOnTheField() {
        Sortie sortie = sortie(1, Difficulty.MEDIUM);
        SetPiece spire = sortie.setPiece(0);
        boolean approached = false;
        while (!spire.engaged()) {
            sortie.step(0);
            approached |= spire.approaching();
        }
        assertTrue(approached);
        assertEquals(ARENA * 140 + Sortie.easeDistance(140), sortie.groundScroll(), 1e-6);
        assertEquals(PlayField.HEIGHT - 196, spire.renderY(1), 1e-6);
        for (int p : ROOTS) {
            assertTrue(
                    spire.partY(p) > 0
                            && spire.partY(p) < PlayField.HEIGHT
                            && spire.partX(p) > 0
                            && spire.partX(p) < PlayField.WIDTH,
                    "root part " + p + " on the field from the start (H1 = a)");
            assertFalse(spire.partShielded(p));
        }
        assertTrue(spire.partShielded(MAW), "the maw from phase 2");
        assertTrue(spire.partShielded(CORE), "the core from phase 3");
    }

    @Test
    void theFightAllocatesNothingPerStep() {
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(2185, Difficulty.HARD);
                    for (int i = 0; i < SimStep.ticks(ARENA + 2); i++) {
                        sortie.step(Autopilot.commands(sortie));
                    }
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(30); i++) {
                        sortie.step(Autopilot.commands(sortie));
                    }
                });
        assertEquals(0, allocated);
    }

    @Test
    void theStatBlocksBountyIsTheBudgetsActBossShare() {
        EnemyData spire = CONTENT.enemy(SLUG);
        List<EnemyData.PartData> parts = spire.partList().orElseThrow();
        assertEquals(
                spire.bounty(),
                parts.stream().mapToInt(EnemyData.PartData::bounty).sum());
        assertEquals(
                spire.hp(), parts.stream().mapToDouble(EnemyData.PartData::hp).sum());
        double paid = spire.bounty()
                * CONTENT.economy().actFactor()
                * CONTENT.level(Level14Test.LEVEL).bounties();
        System.out.printf(
                "Siege Spire paid %.0f = %.1f %% of budget(14)%n",
                paid, 100 * paid / CONTENT.economy().budget().of(14));
        assertEquals(0.30, paid / CONTENT.economy().budget().of(14), 0.30 * BalanceTest.SHARE_TOLERANCE);
    }
}
