package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.SpecialSpec;
import vanguard.sim.WarningEdge;
import vanguard.sim.WaveSpec;

/**
 * A test level with Level 06's mechanics (its own data comes in the next step): Level 01's script
 * with its waves replaced by a Mantis, a pincer of two and a looping Coilwyrm, a dark survey cache,
 * an airlock terminal with the data core and the far side's darkness, loaded at Level 06's place.
 * It checks the new schema keys, the specs the simulation gets and a run to the end.
 */
class FarsideLevelTest {
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String PATH = "campaign/act-1-first-contact/level-06-farside/data.yaml";
    static final String KEY = "act-1-first-contact/level-06-farside";
    private static final int MAX_STEPS = 60 * 60 * 10;

    private static final String WAVES = """
            waves:
              - {t: 20, formation: single, enemy: mantis, count: 1, from: sides, edge: left}
              - t: 40
                formation: snake
                enemy: coilwyrm
                count: 1
                from: front
                paths: [[[120, -60], [360, 200], [120, 400], [240, 700]]]
                loop_back: {after: 6, path: [[240, 600], [300, 300], [240, -80]]}
                easy: {warning: 4}
              - {t: 70, formation: pincer, enemy: mantis, count: 2, from: sides}

            ground_targets:""";
    private static final String TARGETS = """
              - target: survey cache
                section: 3
                layer: ground
                size: [28, 22]
                at: [[95, 200]]
                hits: 3
                reveals: survey cache
                dark: true
              - target: airlock terminal
                section: 3
                layer: ground
                size: [30, 30]
                at: [[100, 240]]
                hits: 2
                reveals: settlement log

            secrets:
              - name: survey cache
                crate: 135
                radio: {speaker: Rook, line: "A survey cache. Finders keepers."}
              - name: settlement log
                crate: 0
                data_core: {unlocks: Targeting computer}
                radio: {speaker: Varga, line: "That's the settlement log. I'll... read it later."}
            """;
    private static final String DARKNESS = """

            darkness:
              headlight: {from: 22, length: 200, easy: 260, angle: 60}
              flare: {seconds: 8, easy_seconds: 12, radius: 120, drift: 40}
              flares:
                - {t: 70, x: 180, y: 160}
                - {t: 112, x: 300, y: 160, skip: [hard]}
              lights:
                - {t: 30, x: 100, radius: 60}
              ambient: 0.12
            """;

    /** The test level's data: Level 01's with the far side's waves, targets, secrets and darkness. */
    static String farsideLevel(String level01) {
        String text = level01.replaceFirst("(?s)\nwaves:\n.*?\n\nground_targets:", "\n" + WAVES)
                .replaceFirst("(?s)\nsecrets:\n.*?\n\npickups:", "\n" + TARGETS + "\npickups:")
                .replaceFirst("(?s)\n  - target: beacon\n.*?\n\n", "\n")
                .replaceFirst(
                        "(?m)^pickups:.*\n  - \\{pickup: armour patch, dropped_by: \\{wave: 122[^\n]*\n",
                        "pickups: []\n")
                .replaceFirst("(?m)^  - \\{event: first-kill, enemy: skitter[^\n]*\n", "")
                .replaceFirst("(?s)  easy:  # Needlers.*?\n  hard:", "  hard:");
        return text + DARKNESS;
    }

    static Content content() {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        String level01 = files.stream()
                .filter(file -> file.path().equals(LEVEL_01))
                .findFirst()
                .orElseThrow()
                .text();
        files.removeIf(file -> file.path().equals(PATH));
        files.add(new DataFile(PATH, farsideLevel(level01)));
        return ContentLoader.load(files);
    }

    private final Content content = content();

    @Test
    void theMantisAndTheCoilwyrmGetTheirStatBlocks() {
        LevelScript level = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        EnemySpec mantis = level.waves().getFirst().enemy();
        assertEquals(40, mantis.sideHover().orElseThrow().edgeX(), 1e-9);
        assertTrue(mantis.sideHover().orElseThrow().exitBack());
        EnemySpec.Sweep sweep = mantis.sweep().orElseThrow();
        assertEquals(Math.toRadians(70), sweep.arcRadians(), 1e-9);
        assertEquals(3.0, sweep.intervalSeconds(), 1e-9);
        assertEquals(300, sweep.length(), 1e-9);
        assertEquals(8, sweep.damage(), 1e-9, "the laser class");
        assertTrue(mantis.gun().isEmpty(), "the sweep is its only attack");

        EnemySpec coilwyrm = level.waves().get(1).enemy();
        assertEquals(40, coilwyrm.hp(), 1e-9, "the unit is its head");
        assertEquals(40, coilwyrm.bounty());
        assertEquals(180, coilwyrm.speed(), 1e-9);
        EnemySpec.ChainSpec chain = coilwyrm.chain().orElseThrow();
        assertEquals(12, chain.segmentBoxes().size());
        assertEquals(54 * 0.7, chain.segmentBoxes().getFirst().width(), 1e-9);
        assertEquals(27 * 0.7, chain.segmentBoxes().getLast().width(), 1e-9);
        assertEquals(0.5 * (58 + 54) / 2, chain.offsets().get(1), 1e-9);
        assertEquals(0.25, chain.popSeconds(), 1e-9);
        assertEquals(2, chain.headMultiplier(), 1e-9, "the head's weak point (its part's multiplier)");
        assertEquals(4, chain.segment().hp(), 1e-9);
        assertEquals(3, chain.segment().bounty());
        assertEquals(10, chain.segment().contactDamage(), 1e-9, "small contact");
        assertEquals(10, chain.tailFirstBonus());
        assertEquals(20, chain.regrown().hp(), 1e-9);
        assertEquals(220, chain.regrowSpeed(), 1e-9);
        assertEquals(3, chain.regrown().gun().orElseThrow().fan());
        WaveSpec wave = level.waves().get(1);
        assertEquals(6, wave.loopBack().orElseThrow().afterSeconds(), 1e-9);

        LevelScript hard = SimSpecs.level(content, KEY, Difficulty.HARD);
        EnemySpec.Sweep hardSweep = hard.waves().getFirst().enemy().sweep().orElseThrow();
        assertEquals(Math.toRadians(90), hardSweep.arcRadians(), 1e-9);
        assertEquals(2.5, hardSweep.intervalSeconds(), 1e-9, "authored: no fire-rate lever on top");
        EnemySpec.ChainSpec hardChain = hard.waves().get(1).enemy().chain().orElseThrow();
        assertEquals(14, hardChain.segmentBoxes().size());
        assertEquals(5, hardChain.regrown().gun().orElseThrow().fan());
        assertEquals(
                3.0 / 0.7,
                SimSpecs.level(content, KEY, Difficulty.EASY)
                        .waves()
                        .getFirst()
                        .enemy()
                        .sweep()
                        .orElseThrow()
                        .intervalSeconds(),
                1e-9);
    }

    @Test
    void theDarknessTheDarkCacheAndTheDataCoreAreRead() {
        LevelScript.Darkness medium =
                SimSpecs.level(content, KEY, Difficulty.MEDIUM).darkness().orElseThrow();
        assertEquals(200, medium.headlightLength(), 1e-9);
        assertEquals(Math.toRadians(60), medium.headlightAngle(), 1e-9);
        assertEquals(2, medium.flares().size());
        assertEquals(8, medium.flareSeconds(), 1e-9);
        LevelScript.Darkness easy =
                SimSpecs.level(content, KEY, Difficulty.EASY).darkness().orElseThrow();
        assertEquals(260, easy.headlightLength(), 1e-9);
        assertEquals(12, easy.flareSeconds(), 1e-9);
        assertEquals(
                1,
                SimSpecs.level(content, KEY, Difficulty.HARD)
                        .darkness()
                        .orElseThrow()
                        .flares()
                        .size());

        List<LevelScript.GroundObjectSpec> objects =
                SimSpecs.level(content, KEY, Difficulty.MEDIUM).groundObjects();
        LevelScript.GroundObjectSpec cache = objects.stream()
                .filter(o -> o.secret().equals("survey cache"))
                .findFirst()
                .orElseThrow();
        assertTrue(cache.dark());
        assertTrue(cache.core().isEmpty());
        LevelScript.GroundObjectSpec terminal = objects.stream()
                .filter(o -> o.secret().equals("settlement log"))
                .findFirst()
                .orElseThrow();
        assertFalse(terminal.dark());
        assertEquals(
                new LevelResult.DataCore("settlement log", "Targeting computer"),
                terminal.core().orElseThrow());
    }

    @Test
    void theSmartBombFliesWithItsNumbers() {
        assertTrue(SimSpecs.fliesSpecial("Smart Bomb"));
        SpecialSpec bomb = SimSpecs.special(content, "Smart Bomb", 1);
        assertEquals(0.35, bomb.smartBomb().ringSeconds(), 1e-9);
        assertEquals(120, bomb.smartBomb().damage(), 1e-9);
        assertEquals(60, bomb.smartBomb().bossPartDamage(), 1e-9);
        assertEquals(1.0, bomb.smartBomb().invulnerableSeconds(), 1e-9);
        assertEquals(1.5, bomb.smartBomb().repeatSeconds(), 1e-9);
        assertEquals(3, bomb.maxCharges());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesTheLevelWithTheLoopBackWarnedAhead(Difficulty difficulty) {
        Sortie sortie = new Sortie(
                2186,
                SimSpecs.starterLoadout(content, difficulty),
                SimSpecs.level(content, KEY, difficulty),
                SimSpecs.rules(content, KEY, difficulty).withInvulnerableShip(),
                SimSpecs.starterLoadout(content, difficulty).plating().maxArmour());
        int steps = 0;
        double bottomFrom = -1;
        boolean telegraphed = false;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            if (!sortie.flying()) {
                sortie.retry(sortie.ship().defences().maxArmour());
            }
            sortie.step(Autopilot.commands(sortie));
            if (bottomFrom < 0
                    && sortie.levelSeconds() > 45
                    && (sortie.edgeWarnings() & WarningEdge.BOTTOM.bit()) != 0) {
                bottomFrom = sortie.levelSeconds();
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                telegraphed |= events.type(i) == SimEvents.Type.SWEEP_TELEGRAPH;
            }
        }
        assertTrue(sortie.complete());
        assertTrue(telegraphed, "a Mantis swept");
        // The loop-back's warning: the bottom edge, 3 s (easy 4 s) before it re-enters.
        assertTrue(bottomFrom > 45, "the loop-back was warned");
        assertEquals(4 + sortie.setPieceCount(), sortie.enemyTotal(), "a chain counts once");
    }
}
