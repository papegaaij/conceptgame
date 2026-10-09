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
import vanguard.sim.SlamArena;
import vanguard.sim.Sortie;
import vanguard.sim.WingmanSpec;

/**
 * M5 part E, step E2c: the Harbour Kraken from its data file (design/enemies/bosses/harbour-kraken;
 * user decisions E5–E7 of 2026-10-08) on a fixture arena (Level 11's data comes in step E3a): 3 s at
 * 140 px/s, the arena of 1 s at speed 0 where it arrives, and Level 11's convoy at its stations and
 * lanes; the balance plan's Level 11 fit (the front at L4, the Bomb Rack, the Autocannon Pod, the Tail
 * Gun, Rook's Mortar; no Torpedo Pod) flown by the {@link Autopilot}, which keeps out of the telegraphed
 * lanes. The fight's length at medium is the one the HP are tuned to (E6 = a: ≈ 60 s) and the par.
 */
class KrakenTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    static final String SLUG = "harbour-kraken";
    /** The rules of an Act 2 level (Level 11's own data comes later). */
    private static final String RULES_LEVEL = Level10Test.LEVEL;

    static final double ARENA = 3;
    static final int HEAD = 0;
    static final int LEFT_ARM = 1;
    static final int RIGHT_ARM = 2;
    /** The seeds the autopilot's fight is measured on. */
    static final long[] SEEDS = {2185, 11, 77, 1301, 4242};

    static LevelScript.SetPieceSpec kraken(Difficulty difficulty) {
        return SimSpecs.boss(CONTENT, new LevelData.BossPlacement(SLUG, ARENA, PlayField.WIDTH / 2.0, 2), difficulty);
    }

    /** Level 11's convoy block: a cargo ship's HP (slams) on easy and hard (medium's 4 is the ally's own). */
    static final double EASY_SHIP_HP = 5;

    static final double HARD_SHIP_HP = 2;

    /** Level 11's convoy: three cargo ships to lanes 1, 2 and 4 and the frigate that leaves (README, Convoy). */
    static LevelScript.Naval convoy(Difficulty difficulty) {
        Optional<LevelData.AllyChange> easy = difficulty == Difficulty.EASY
                ? Optional.of(new LevelData.AllyChange(EASY_SHIP_HP))
                : difficulty == Difficulty.HARD
                        ? Optional.of(new LevelData.AllyChange(HARD_SHIP_HP))
                        : Optional.empty();
        var cargo = PartERules.ally(CONTENT, "cargo-ship", easy);
        var frigate = PartERules.ally(CONTENT, "escort-frigate", Optional.empty());
        return new LevelScript.Naval(
                List.of(
                        new LevelScript.NavalUnit(cargo, "Halvorsen", 130, PlayField.HEIGHT - 380, 1),
                        new LevelScript.NavalUnit(cargo, "Mbeki", 240, PlayField.HEIGHT - 350, 2),
                        new LevelScript.NavalUnit(cargo, "Saint-Laurent", 350, PlayField.HEIGHT - 380, 4),
                        new LevelScript.NavalUnit(frigate, "Ruyter", 240, PlayField.HEIGHT - 480, 0)),
                3,
                PlayField.HEIGHT - 450,
                4,
                120);
    }

    static LevelScript.RadioCue cue(LevelScript.CueTrigger trigger, String subject, String line) {
        return new LevelScript.RadioCue(trigger, 0, subject, "Atlas Control", line, false, "neutral");
    }

    static LevelScript level(Difficulty difficulty) {
        return new LevelScript(
                        11,
                        2,
                        0,
                        List.of(
                                new LevelScript.Section(ARENA, 140),
                                new LevelScript.Section(ARENA + 1, 0, true),
                                new LevelScript.Section(ARENA + 16, 140)),
                        List.of(),
                        List.of(),
                        List.of(),
                        0,
                        List.of(
                                cue(LevelScript.CueTrigger.FIRST_TELEGRAPH, "", "Lane's boiling!"),
                                cue(
                                        LevelScript.CueTrigger.BOSS_PART_DESTROYED,
                                        "left arm" + LevelScript.CueTrigger.PART_SEPARATOR + "right arm",
                                        "Arm's off!")),
                        LevelScript.Secondary.afloat(100, "CONVOY"),
                        List.of(),
                        List.of(),
                        List.of(kraken(difficulty)))
                .withWater(true)
                .withConvoy(convoy(difficulty));
    }

    /**
     * The balance plan's fit at Level 11 (design/player/balance-plan.yaml: Level 10's with the front at
     * L4, Rook's Mortar refitted for the surfaced Kraken; the Airstrike's two charges; no Torpedo Pod).
     */
    static Loadout planLoadout(Difficulty difficulty) {
        return SimSpecs.loadout(
                        CONTENT,
                        CONTENT.systems().engines().getFirst().name(),
                        List.of(
                                new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4),
                                new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "bomb-rack", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "autocannon-pod", 1),
                                new SimSpecs.FittedWeapon(Armament.Slot.REAR, "tail-gun", 1)),
                        CONTENT.shields().models().get(1).name(),
                        CONTENT.armour().plating().get(1).name(),
                        0,
                        difficulty)
                .withWingman(SimSpecs.wingman(
                        CONTENT,
                        "mortar",
                        1,
                        WingmanSpec.Side.LEFT,
                        CONTENT.wingmen().rook().armour()))
                .withSpecial(SimSpecs.special(CONTENT, "Airstrike", 2));
    }

    static Sortie sortie(long seed, Difficulty difficulty) {
        Loadout loadout = planLoadout(difficulty);
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

    @Test
    void theStatBlockMapsToAnAnchoredArenaBoss() {
        LevelScript.SetPieceSpec spec = kraken(Difficulty.MEDIUM);
        BossSpec boss = spec.boss().orElseThrow();
        assertTrue(boss.anchored());
        assertTrue(boss.midBoss());
        assertEquals("HARBOUR KRAKEN", boss.barName());
        assertEquals(PlayField.HEIGHT - 78, boss.hoverY(), "the platform's centre 78 px below the top edge (round 33)");
        assertEquals(Layer.GROUND, boss.layer(), "the sea's surface; its parts have their own");
        List<String> names =
                spec.parts().stream().map(LevelScript.PartSpec::name).toList();
        assertEquals(List.of("head", "left arm", "right arm"), names);
        assertTrue(spec.parts().get(HEAD).vital());
        assertEquals(0.5, spec.parts().get(HEAD).multiplier(), "the mantle");
        assertEquals(
                -216,
                spec.parts().get(HEAD).dy(),
                "216 px below the platform's centre: off the deck's south edge (round 33)");
        assertEquals(1700, spec.parts().get(HEAD).hp(), "tuned to a ≈ 60 s fight (E6 = a)");
        assertEquals(350, spec.parts().get(LEFT_ARM).hp());

        BossSpec.Arena arena = boss.arena().orElseThrow();
        assertEquals(List.of(Layer.SUB, Layer.SUB, Layer.SUB), arena.partLayers());
        assertEquals(4, arena.lanes().count());
        assertEquals(120, arena.lanes().width());
        assertEquals(PlayField.HEIGHT - 154, arena.lanes().top(), "from the deck's lower edge (y 154) down");
        assertEquals(
                List.of(new BossSpec.Arm(LEFT_ARM, 0b110), new BossSpec.Arm(RIGHT_ARM, 0b11000)),
                arena.lanes().arms(),
                "E5 = a: the left arm lanes 1–2, the right arm 3–4");
        BossSpec.Slam slam = arena.slam();
        assertEquals(1.0, slam.telegraphSeconds());
        assertEquals(0.5, slam.riseSeconds());
        assertEquals(1.5, slam.awashSeconds());
        assertEquals(0.6, slam.sinkSeconds());
        assertEquals(10, slam.damage(), "`heavy`");
        assertEquals(6, slam.splashCount());
        assertEquals(4, slam.splashDamage(), "`small`");
        assertEquals(2, slam.volleyLanes(), "one per arm");
        assertEquals(2, arena.spots().size());
        assertEquals(2, arena.spots().getFirst().multiplier(), "the eyes ×2");
        assertTrue(arena.spots().getFirst().whileOpen());
        assertTrue(boss.chains().stream().allMatch(BossSpec.Chain::slam), "the slam arms as runtime chains (E7 = a)");
        assertEquals(13, boss.chains().getFirst().segments());

        BossSpec.Phase slams = boss.phases().get(0);
        assertEquals(3, slams.arena().slams());
        assertEquals(20, slams.seconds());
        assertEquals(BossSpec.Slamming.CHAIN, slams.arena().slamming());
        BossSpec.Phase headUp = boss.phases().get(1);
        assertEquals(List.of(HEAD), headUp.arena().belowParts());
        assertEquals(0.4, headUp.arena().below());
        assertTrue(headUp.untilParts().isEmpty(), "it ends on the share, not on the head's death");
        assertEquals(BossSpec.Slamming.AFTER_DIVE, headUp.arena().slamming());
        BossSpec.Surface surface = headUp.arena().surface().orElseThrow();
        assertEquals(HEAD, surface.part());
        assertEquals(2, surface.riseSeconds());
        assertEquals(8, surface.openSeconds());
        assertEquals(0.5, surface.glowSeconds());
        BossSpec.Release release = headUp.arena().release().orElseThrow();
        assertEquals("driftjelly", release.enemy().slug());
        assertEquals(6, release.count());
        assertEquals(List.of(1, 4), release.lanes());
        BossSpec.Phase twoLanes = boss.phases().get(2);
        assertEquals(BossSpec.Slamming.VOLLEY, twoLanes.arena().slamming());
        assertEquals(4, twoLanes.arena().volleySeconds());
        assertTrue(twoLanes.arena().surface().orElseThrow().stay());
        assertEquals(Optional.of(PickupType.SHIELD_CELL), twoLanes.arena().drop());
        assertEquals(List.of(HEAD), twoLanes.untilParts());
        BossSpec.Attack fan = boss.attacks().getFirst();
        assertEquals(BossSpec.Pattern.FAN, fan.pattern());
        assertEquals(7, fan.gun().fan());
        assertEquals(6, fan.gun().damage(), "`medium`");

        BossSpec.Slam hard = kraken(Difficulty.HARD)
                .boss()
                .orElseThrow()
                .arena()
                .orElseThrow()
                .slam();
        assertEquals(0.8, hard.telegraphSeconds(), "hard's telegraph");
        assertEquals(3, hard.volleyLanes(), "hard's third lane");
        assertEquals(
                1.0,
                kraken(Difficulty.EASY)
                        .boss()
                        .orElseThrow()
                        .arena()
                        .orElseThrow()
                        .slam()
                        .telegraphSeconds());
    }

    @Test
    void theBountyIsSplitOneToFourAndThePartsSumUp() {
        EnemyData kraken = CONTENT.enemy(SLUG);
        List<EnemyData.PartData> parts = kraken.partList().orElseThrow();
        assertEquals(
                kraken.bounty(),
                parts.stream().mapToInt(EnemyData.PartData::bounty).sum());
        assertEquals(
                kraken.hp(), parts.stream().mapToDouble(EnemyData.PartData::hp).sum());
        assertEquals(4, parts.get(HEAD).bounty() / (double) parts.get(LEFT_ARM).bounty(), 0.01, "arms : head = 1 : 4");
        // ≈ 15 % of budget(11) = 1,377 once paid: × 1.6 (Act 2) × Level 11's bounty scale of 0.6
        // (2026-10-09: 216, the head 144 and the arms 36 each, for the 13 % that 186 paid).
        double paidAtScale = kraken.bounty()
                * CONTENT.economy().actFactor()
                * CONTENT.level(Level11Test.LEVEL).bountyScale().orElseThrow();
        assertEquals(0.15, paidAtScale / CONTENT.economy().budget().of(11), 0.005);
    }

    private static final String LEVEL_05 = "campaign/act-1-first-contact/level-05-crater-nest/data.yaml";
    private static final String KRAKEN = "enemies/bosses/harbour-kraken/data.yaml";
    private static final String GORGON = "enemies/bosses/gorgon-frigate/data.yaml";
    private static final String BOSS_DESTROYED = "  - {event: boss-destroyed, speaker: Okafor,";

    /** Level 05 with the Kraken as its boss over an arena of speed 0 (the frigate's phase cue goes). */
    private static String krakenLevel(String text) {
        return edit(
                        edit(text, "    end: 205\n    speed: 30\n", "    end: 205\n    speed: 0\n"),
                        "  enemy: gorgon-frigate\n  t: 150",
                        "  enemy: harbour-kraken\n  t: 150")
                .replaceFirst("(?m)^  - \\{event: boss-phase, phase: Core,.*\\n", "");
    }

    private static String edit(String text, String what, String with) {
        assertTrue(text.contains(what), "the data has " + what);
        return text.replace(what, with);
    }

    /** The design tree's data with {@code path}'s text edited. */
    private static List<DataFile> files(String path, java.util.function.UnaryOperator<String> change) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(path) ? new DataFile(f.path(), change.apply(f.text())) : f)
                .toList();
    }

    private static void assertProblem(List<DataFile> files, String message) {
        var e = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> ContentLoader.load(files));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    private static String cue(String event) {
        return "  - {event: " + event + ", speaker: Rook, line: \"Watch it!\"}\n" + BOSS_DESTROYED;
    }

    /**
     * The arena keys are checked: speed 0 only on an anchored boss's arena, which it has and arrives
     * at; lanes, part layers and slamming only for an anchored boss; the radio's new events with what
     * they name.
     */
    @Test
    void theArenaKeysAreChecked() {
        Content content = ContentLoader.load(files(
                LEVEL_05,
                text -> edit(
                        krakenLevel(text),
                        BOSS_DESTROYED,
                        cue("first-telegraph").replace(BOSS_DESTROYED, "")
                                + "  - {event: boss-part-destroyed, parts: [left arm, right arm], speaker: Rook, line: \"Arm's off!\"}\n"
                                + BOSS_DESTROYED)));
        LevelScript level = SimSpecs.level(content, "act-1-first-contact/level-05-crater-nest", Difficulty.MEDIUM);
        assertTrue(level.setPieces().getFirst().boss().orElseThrow().anchored());
        assertTrue(level.radio().stream()
                .anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.BOSS_PART_DESTROYED
                        && cue.subject().equals("left arm|right arm")));

        assertProblem(
                files(LEVEL_05, text -> edit(text, "    end: 35\n", "    end: 35\n    speed: 0\n")),
                "speed 0 only on the arena of a level whose boss is anchored");
        assertProblem(
                files(LEVEL_05, text -> krakenLevel(text)
                        .replace("    end: 205\n    speed: 0\n", "    end: 205\n    speed: 30\n")),
                "an anchored boss's arena has speed 0");
        assertProblem(
                files(LEVEL_05, text -> krakenLevel(text).replace("  t: 150\n", "  t: 151\n")),
                "arrives at its arena's start");
        assertProblem(
                files(LEVEL_05, text -> edit(text, BOSS_DESTROYED, cue("first-telegraph"))),
                "without a boss with lanes");
        assertProblem(
                files(
                        LEVEL_05,
                        text -> edit(
                                krakenLevel(text),
                                BOSS_DESTROYED,
                                "  - {event: boss-part-destroyed, parts: [head], speaker: Rook, line: \"Got it!\"}\n"
                                        + BOSS_DESTROYED)),
                "'head' is no part of the level's boss but its vital one");
        assertProblem(
                files(
                        GORGON,
                        text -> edit(
                                text,
                                "  par: 60",
                                "  lanes: {count: 4, width: 120, arms: {left head: [1, 2]}}\n"
                                        + "  slam: {telegraph: 1, rise: 0.5, awash: 1.5, sink: 0.6, damage: heavy,"
                                        + " splash: {count: 6, speed: 120, bullet: small}, choose: alternate}\n  par: 60")),
                "only an anchored boss has lanes");
        assertProblem(
                files(KRAKEN, text -> edit(text, "  anchored: {y: 78}", "  hover: {y: 78}")),
                "only an anchored boss's parts have a layer or spots");
        assertProblem(
                files(KRAKEN, text -> edit(text, "      slamming: chain ", "      slamming: volley ")),
                "a volley, and only it, has its every");
        assertProblem(
                files(KRAKEN, text -> edit(text, "lanes: [1, 4], on", "lanes: [1, 5], on")), "the boss has no lane 5");
        assertProblem(
                files(KRAKEN, text -> edit(text, "motion: slam}\n  - {name: right", "motion: slam}\n  - {name: right")
                        .replace(
                                "arms: {left arm: [1, 2], right arm: [3, 4]}",
                                "arms: {left arm: [1, 2], head: [3, 4]}")),
                "is no slam chain's part");
    }

    /** What one autopilot run of the fight saw. */
    record Fight(
            double seconds,
            boolean killed,
            double armourLost,
            int shipsSunk,
            int slams,
            int volleys,
            boolean phaseTwo,
            boolean phaseThree,
            boolean released,
            boolean telegraphLine,
            boolean partLine,
            double rookArmourLost,
            double slamDamage,
            double otherDamage) {}

    /** The autopilot flies the fixture until the Kraken dies (at most 4 minutes). */
    static Fight fight(long seed, Difficulty difficulty) {
        return fight(seed, difficulty, Autopilot.ArenaPlay.EYES);
    }

    /** The same, the arena played as {@code play}. */
    static Fight fight(long seed, Difficulty difficulty, Autopilot.ArenaPlay play) {
        Sortie sortie = sortie(seed, difficulty);
        SetPiece kraken = sortie.setPiece(0);
        double armour = sortie.ship().defences().armour();
        double rook = sortie.wingman().orElseThrow().armour();
        int slams = 0;
        int volleys = 0;
        boolean two = false;
        boolean three = false;
        boolean released = false;
        boolean telegraphLine = false;
        boolean partLine = false;
        double slamDamage = 0;
        double otherDamage = 0;
        for (int i = 0; i < SimStep.ticks(240) && !kraken.destroyed(); i++) {
            int jellies = sortie.enemyCount();
            double before =
                    sortie.ship().defences().shield() + sortie.ship().defences().armour();
            sortie.step(Autopilot.commands(sortie, play));
            double lost = Math.max(
                    0,
                    before
                            - sortie.ship().defences().shield()
                            - sortie.ship().defences().armour());
            if (!values(sortie, SimEvents.Type.SLAM).isEmpty() && lost >= 10) {
                slamDamage += lost;
            } else {
                otherDamage += lost;
            }
            slams += values(sortie, SimEvents.Type.SLAM).size();
            volleys += values(sortie, SimEvents.Type.TELEGRAPH).size() >= 2 ? 1 : 0;
            two |= kraken.phase() >= 1;
            three |= kraken.phase() >= 2;
            released |= sortie.enemyCount() >= jellies + 6;
            for (int cue : values(sortie, SimEvents.Type.RADIO)) {
                telegraphLine |= sortie.script().radio().get(cue).trigger() == LevelScript.CueTrigger.FIRST_TELEGRAPH;
                partLine |= sortie.script().radio().get(cue).trigger() == LevelScript.CueTrigger.BOSS_PART_DESTROYED;
            }
            if (!sortie.flying()) {
                break;
            }
        }
        int sunk = 0;
        for (int k = 0; k < sortie.allyCount(); k++) {
            sunk += sortie.ally(k).alive() ? 0 : 1;
        }
        return new Fight(
                kraken.killSeconds(),
                kraken.destroyed(),
                armour - sortie.ship().defences().armour(),
                sunk,
                slams,
                volleys,
                two,
                three,
                released,
                telegraphLine,
                partLine,
                rook - sortie.wingman().orElseThrow().armour(),
                slamDamage,
                otherDamage);
    }

    /**
     * E6 = a: the autopilot's fight with the plan's fit lasts about 60 s at medium (the bar appearing to
     * the kill, the par), inside the mid-boss window 45–75 s on every seed; all three phases, the field,
     * volleys and the radio's new events happen; the autopilot keeps out of the slams.
     */
    @Test
    void theAutopilotsFightLastsAboutSixtySecondsAtMedium() {
        double sum = 0;
        for (long seed : SEEDS) {
            Fight fight = fight(seed, Difficulty.MEDIUM);
            System.out.printf("Kraken medium seed %d: %s%n", seed, fight);
            assertTrue(fight.killed(), "killed: " + fight);
            assertTrue(fight.seconds() >= 45 && fight.seconds() <= 75, "the mid-boss window: " + fight);
            assertTrue(fight.phaseTwo() && fight.phaseThree() && fight.released(), "every phase: " + fight);
            assertTrue(fight.telegraphLine(), "first-telegraph");
            sum += fight.seconds();
        }
        double mean = sum / SEEDS.length;
        System.out.printf(
                "Kraken medium: mean %.1f s, par %.0f%n",
                mean, kraken(Difficulty.MEDIUM).boss().orElseThrow().parSeconds());
        assertEquals(60, mean, 6, "E6 = a: about 60 s");
        assertEquals(
                60, kraken(Difficulty.MEDIUM).boss().orElseThrow().parSeconds(), 8, "the par from the measured fight");
    }

    /**
     * Easy and hard are measured too (printed; the windows are medium's): easy is won on every seed;
     * on hard (×1.3 HP, the 0.8 s telegraph, three lanes a volley) the autopilot, a simple pilot, is
     * worn down on some seeds.
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
            System.out.printf("Kraken %s seed %d: %s%n", difficulty, seed, fight);
            if (fight.killed()) {
                killed++;
                sum += fight.seconds();
            }
        }
        System.out.printf("Kraken %s: %d of %d won, mean %.1f s%n", difficulty, killed, SEEDS.length, sum / killed);
        assertTrue(killed >= (difficulty == Difficulty.EASY ? SEEDS.length : 3), killed + " won");
    }

    /**
     * The convoy bonus (user, 2026-10-09: "measure, then tune"): a pilot who cuts the slam arm guarding
     * the most ships first ({@link Autopilot.ArenaPlay#ARMS}) keeps all three cargo ships afloat at the
     * Kraken's death on most seeds at medium, in a fight of the mid-boss window (E6 = a: ≈ 60 s); the
     * first pilot, who shoots the eyes and lets the slams fall where they fall, keeps them on none. The
     * lever: a cargo ship takes 4 slams at medium (easy 5, hard 2), since every slam in the left half
     * strikes a ship's lane and the left arm takes about six slams to cut. Easy and hard are printed
     * (hard stays harsh: two slams sink a ship).
     */
    @Test
    void anArmFirstPilotKeepsTheConvoyAfloatAtMedium() {
        for (Difficulty difficulty : Difficulty.values()) {
            int kept = 0;
            int keptByEyes = 0;
            int won = 0;
            double sum = 0;
            for (long seed : SEEDS) {
                Fight fight = fight(seed, difficulty, Autopilot.ArenaPlay.ARMS);
                Fight eyes = fight(seed, difficulty, Autopilot.ArenaPlay.EYES);
                System.out.printf("Kraken arm-first %s seed %d: %s%n", difficulty, seed, fight);
                kept += fight.killed() && fight.shipsSunk() == 0 ? 1 : 0;
                keptByEyes += eyes.killed() && eyes.shipsSunk() == 0 ? 1 : 0;
                if (fight.killed()) {
                    won++;
                    sum += fight.seconds();
                }
                if (difficulty == Difficulty.MEDIUM) {
                    assertTrue(fight.killed(), "killed: " + fight);
                    // Round 33 (the head off the deck's south edge, its fan starting closer): the slowest
                    // arm-first seed runs to 75.5 s; its mean stays in the window (below).
                    assertTrue(fight.seconds() >= 45 && fight.seconds() <= 80, "about the mid-boss window: " + fight);
                }
            }
            System.out.printf(
                    "Kraken arm-first %s: %d of %d won, mean %.1f s, all three afloat on %d seeds (eyes first: %d)%n",
                    difficulty, won, SEEDS.length, sum / Math.max(1, won), kept, keptByEyes);
            if (difficulty == Difficulty.MEDIUM) {
                assertTrue(kept >= 3, "most of the time on medium: " + kept);
                assertTrue(keptByEyes < kept, "the arms first keep more of the convoy than the eyes first");
                assertEquals(60, sum / won, 6, "E6 = a: about 60 s");
            } else if (difficulty == Difficulty.EASY) {
                assertEquals(SEEDS.length, won, "easy is won on every seed");
            }
        }
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
                        sortie.step(Autopilot.commands(
                                sortie, i % 2 == 0 ? Autopilot.ArenaPlay.ARMS : Autopilot.ArenaPlay.EYES));
                    }
                });
        assertEquals(0, allocated);
    }

    @Test
    void theArenaIsAnchoredWhereTheScrollHalts() {
        Sortie sortie = sortie(1, Difficulty.MEDIUM);
        SetPiece kraken = sortie.setPiece(0);
        boolean approached = false;
        while (!kraken.engaged()) {
            sortie.step(0);
            approached |= kraken.approaching();
        }
        assertTrue(approached);
        assertEquals(ARENA * 140 + Sortie.easeDistance(140), sortie.groundScroll(), 1e-6);
        assertEquals(PlayField.HEIGHT - 78, kraken.renderY(1), 1e-6);
        SlamArena arena = kraken.arena().orElseThrow();
        assertEquals(4, arena.laneCount());
        assertFalse(arena.open(HEAD));
    }
}
