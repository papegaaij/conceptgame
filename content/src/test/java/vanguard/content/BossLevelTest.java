package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.sim.Armament;
import vanguard.sim.BossSpec;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * A test level with the Gorgon Frigate (Level 05's own data comes with its waves): Level 01's
 * script cut at 150 s, where the frigate arrives over a 30 px/s arena section of 55 s with the
 * boss radio events, and a 10 s section after it, loaded at Level 05's place. It checks the boss
 * schema, the specs the simulation gets and an autopilot kill.
 */
class BossLevelTest {
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String PATH = "campaign/act-1-first-contact/level-05-crater-nest/data.yaml";
    static final String KEY = "act-1-first-contact/level-05-crater-nest";
    private static final int MAX_STEPS = 60 * 60 * 10;

    private static final String ARENA = """
              - name: Arena
                end: 205
                speed: 30
                arena: true
                atmosphere: clear
                tiles: [earth]
              - name: Lift-off
                end: 215
                atmosphere: clear
                tiles: [earth]

            waves:""";
    private static final String RADIO = """
              - {event: boss-phase, phase: Last head, speaker: Rook, line: "One head left!"}
              - {event: boss-destroyed, speaker: Okafor, line: "Frigate down. Good work."}
            """;

    /** The test level's data: Level 01's until 150 s, then the frigate's arena. */
    static String bossLevel(String level01) {
        String text = level01.replace("  - name: Pursuit\n    end: 160\n", "  - name: Pursuit\n    end: 150\n")
                .replaceFirst("(?s)  - name: Scout Leader\n.*?\n\nwaves:", ARENA)
                .replaceFirst("(?s)  - t: 162\n.*?\n  - \\{t: 166[^\\n]*\\n", "")
                .replaceFirst("(?m)^  - \\{t: 160, .*\\n  - \\{t: 161, .*\\n", RADIO);
        return text + "\nboss: {enemy: gorgon-frigate, t: 150, x: 240, section: 5}\n";
    }

    static Content content() {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        String level01 = files.stream()
                .filter(file -> file.path().equals(LEVEL_01))
                .findFirst()
                .orElseThrow()
                .text();
        // In place of Level 05's own data.
        files.removeIf(file -> file.path().equals(PATH));
        files.add(new DataFile(PATH, bossLevel(level01)));
        return ContentLoader.load(files);
    }

    private final Content content = content();

    @Test
    void theFrigateArrivesAtItsTimeOverTheArena() {
        LevelScript level = SimSpecs.level(content, KEY, Difficulty.MEDIUM);
        LevelScript.Section arena = level.sections().get(4);
        assertTrue(arena.arena());
        assertEquals(30, arena.speed(), 1e-9);
        assertEquals(205, arena.end(), 1e-9);
        LevelScript.SetPieceSpec frigate = level.setPieces().getFirst();
        BossSpec boss = frigate.boss().orElseThrow();
        assertEquals("gorgon-frigate", frigate.slug());
        assertEquals(150, boss.arriveSeconds(), 1e-9);
        assertEquals(540 - 110, boss.hoverY(), 1e-9);
        assertEquals(60, boss.parSeconds(), 1e-9);
        assertEquals("GORGON FRIGATE", boss.barName());
        assertTrue(boss.midBoss());
        assertEquals(4, frigate.parts().size());
        assertEquals(1.5, frigate.parts().getFirst().multiplier(), 1e-9);
        assertEquals(3, boss.chains().size());
        assertEquals(5, boss.chains().getFirst().segments());
        assertEquals(
                List.of("Three heads", "Last head", "Core"),
                boss.phases().stream().map(BossSpec.Phase::name).toList());
        assertEquals(List.of(3), boss.phases().get(2).exposes());
        assertEquals(Math.toRadians(55), boss.phases().get(1).bendRadians(), 1e-9);
        assertEquals(
                "skitter",
                boss.phases().getFirst().stream().orElseThrow().enemy().slug());
        assertEquals(3, boss.attacks().getFirst().gun().burst());
        assertEquals(List.of(0, 1, 2), boss.attacks().getFirst().parts());

        BossSpec hard = SimSpecs.level(content, KEY, Difficulty.HARD)
                .setPieces()
                .getFirst()
                .boss()
                .orElseThrow();
        assertEquals(5, hard.attacks().get(1).gun().burst(), "hard: the last head fires 5-shot bursts");
        assertEquals(16, hard.attacks().get(2).count(), "hard: core rings of 16");
        assertEquals(12, boss.attacks().get(2).count());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotKillsTheFrigate(Difficulty difficulty) {
        Sortie sortie = sortie(content, 2185, difficulty);
        int steps = 0;
        double arrived = -1;
        double killedAt = -1;
        double sectionSixAt = -1;
        List<Integer> phases = new ArrayList<>();
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            if (!sortie.flying()) {
                if (sortie.bossCheckpoint()) {
                    sortie.retryFromBoss();
                } else {
                    sortie.retry(sortie.ship().defences().maxArmour());
                }
            }
            sortie.step(Autopilot.commands(sortie));
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                switch (events.type(i)) {
                    case BOSS_ARRIVED -> arrived = sortie.levelSeconds();
                    case BOSS_PHASE -> phases.add(events.value(i));
                    case BOSS_DESTROYED -> killedAt = sortie.setPiece(0).killSeconds();
                    default -> {}
                }
            }
            if (sectionSixAt < 0 && sortie.section() == 6) {
                sectionSixAt = steps * SimStep.SECONDS;
            }
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "Boss level %s: attempt %d, frigate killed in %.1f s (par 60), phases %s, kills %d/%d, bonuses %s%n",
                difficulty, sortie.attempt(), killedAt, phases, result.kills(), result.enemies(), result.bonuses());
        assertEquals(150, arrived, 1e-9);
        assertTrue(sortie.complete());
        assertTrue(killedAt > 0, "the frigate is destroyed");
        assertTrue(phases.containsAll(List.of(1, 2)), "it went through its phases");
        assertEquals(killedAt, result.bossTime().killSeconds(), 1e-9);
        assertEquals(result.bossTime().underPar(), result.bonuses().stream().anyMatch(bonus -> bonus.name()
                .equals("Boss rush")));
    }

    @Test
    void theSameSeedAndCommandsGiveTheSameState() {
        Sortie first = sortie(content, 77, Difficulty.HARD);
        Sortie second = sortie(content, 77, Difficulty.HARD);
        for (int i = 0; i < 60 * 200; i++) {
            first.step(Autopilot.commands(first));
            second.step(Autopilot.commands(second));
        }
        assertEquals(first.stateHash(), second.stateHash());
    }

    /** A fit with the Pulse Cannon at level 4, as a Level 05 pilot might have. */
    static Sortie sortie(Content content, long seed, Difficulty difficulty) {
        Loadout starter = SimSpecs.starterLoadout(content, difficulty);
        Loadout loadout = SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 4)),
                content.shields().models().getFirst().name(),
                content.armour().plating().getFirst().name(),
                0,
                difficulty);
        return new Sortie(
                seed,
                loadout,
                SimSpecs.level(content, KEY, difficulty),
                SimSpecs.rules(content, KEY, difficulty),
                starter.plating().maxArmour());
    }
}
