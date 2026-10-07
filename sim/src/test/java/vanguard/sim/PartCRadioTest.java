package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.LevelClockSpecs.NODE;
import static vanguard.sim.LevelClockSpecs.cue;
import static vanguard.sim.LevelClockSpecs.radioed;
import static vanguard.sim.PartCSpecs.count;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part C's radio (design/campaign Level 09, Radio chatter): {@code requires: escort} plays a cue
 * only while an escort flies (hired, fitted, not ejected), {@code hold-start} is the level's first
 * hold, once, and {@code first-pounce} the attempt's first pounce, once.
 */
class PartCRadioTest {
    private static final int ESCORT = LevelScript.RadioCue.FITTED_ESCORT;

    private static LevelScript timedLines() {
        return LevelClockSpecs.level(
                10,
                150,
                List.of(),
                List.of(),
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of(cue(LevelScript.CueTrigger.TIME, 1, ESCORT), cue(LevelScript.CueTrigger.TIME, 2, ESCORT)),
                List.of(),
                Optional.empty());
    }

    /** The steps (0-based) at which each cue started over {@code seconds}. */
    private static int[] started(Sortie sortie, double seconds, Runnable at1point5) {
        int[] at = {-1, -1};
        for (int step = 0; step < SimStep.ticks(seconds); step++) {
            if (step == SimStep.ticks(1.5)) {
                at1point5.run();
            }
            sortie.step(Command.NONE);
            for (int c = 0; c < at.length; c++) {
                if (radioed(sortie, c)) {
                    at[c] = step;
                }
            }
        }
        return at;
    }

    @Test
    void anEscortCuePlaysWhileHeFlies() {
        Sortie sortie = WingmanTest.sortie(timedLines(), WingmanSpec.Side.LEFT);
        int[] at = started(sortie, 3, () -> {});

        assertTrue(at[0] >= 0 && at[1] >= 0, "both lines with Rook flying");
    }

    @Test
    void anEscortCueIsSkippedWithoutHimOrAfterHeEjected() {
        Sortie without = new Sortie(1, TestSpecs.LOADOUT, timedLines(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        int[] none = started(without, 3, () -> {});
        assertEquals(-1, none[0], "not without an escort");
        assertEquals(-1, none[1]);

        Sortie ejecting = WingmanTest.sortie(timedLines(), WingmanSpec.Side.LEFT);
        Wingman rook = ejecting.wingman().orElseThrow();
        int[] at = started(
                ejecting,
                3,
                () -> rook.hit(1000, ejecting.ship().x(), ejecting.ship().y(), ejecting.events()));
        assertTrue(rook.ejected());
        assertTrue(at[0] >= 0, "the first while he flew");
        assertEquals(-1, at[1], "not after he ejected");
    }

    @Test
    void holdStartIsTheFirstHoldsOnce() {
        LevelScript level = LevelClockSpecs.level(
                40,
                150,
                List.of(),
                List.of(new LevelScript.GroundUnit(2, 240, NODE, 0), new LevelScript.GroundUnit(4, 240, NODE, 1)),
                List.of("Node A", "Node B"),
                new LevelScript.Secondary(0.8, 50),
                List.of(cue(LevelScript.CueTrigger.HOLD_START, 0, 0)),
                List.of(new LevelScript.Hold(List.of(0), 200, 30, 1), new LevelScript.Hold(List.of(1), 200, 30, 1)),
                Optional.empty());
        Sortie sortie =
                new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        int holds = 0;
        int lines = 0;
        for (int step = 0; step < SimStep.ticks(40) && holds < 2; step++) {
            sortie.step(Command.NONE);
            holds += count(sortie, SimEvents.Type.HOLD_START);
            lines += radioed(sortie, 0) ? 1 : 0;
            if (count(sortie, SimEvents.Type.HOLD_START) > 0) {
                assertEquals(holds == 1, radioed(sortie, 0), "with the first hold's start only");
            }
            if (sortie.holdActive() && sortie.holdEase() == 1) {
                // Clear the running hold: its node is the lowest one on the screen.
                int lowest = -1;
                for (int i = 0; i < sortie.enemyCount(); i++) {
                    if (lowest < 0 || sortie.enemy(i).y() < sortie.enemy(lowest).y()) {
                        lowest = i;
                    }
                }
                sortie.destroyEnemy(lowest);
            }
        }

        assertEquals(2, holds, "both holds ran");
        assertEquals(1, lines, "one line");
    }

    @Test
    void firstPounceIsTheAttemptsFirstPounceOnce() {
        // A Ravager running in 150 px above the ship, leaping again 0.5 s after it lands; the ship
        // follows it to the right so it stays in range.
        LevelScript level = LevelClockSpecs.level(
                20,
                5,
                List.of(PartCSpecs.walkers(
                        0,
                        WaveSpec.Formation.PACK,
                        PartCSpecs.ravager(0.3, 0.5),
                        1,
                        List.of(List.of(
                                new WaveSpec.At(-40, PlayField.HEIGHT - 96 - 150),
                                new WaveSpec.At(PlayField.WIDTH + 200, PlayField.HEIGHT - 96 - 150))))),
                List.of(),
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of(
                        cue(LevelScript.CueTrigger.FIRST_POUNCE, 0, 0),
                        cue(LevelScript.CueTrigger.FIRST_POUNCE, 0, ESCORT)),
                List.of(),
                Optional.empty());
        Sortie sortie =
                new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        int pounces = 0;
        int lines = 0;
        int rookLines = 0;
        for (int step = 0; step < SimStep.ticks(6); step++) {
            sortie.step(pounces > 0 ? Command.RIGHT.bit() : Command.NONE);
            pounces += count(sortie, SimEvents.Type.POUNCE);
            lines += radioed(sortie, 0) ? 1 : 0;
            rookLines += radioed(sortie, 1) ? 1 : 0;
            if (count(sortie, SimEvents.Type.POUNCE) > 0 && pounces == 1) {
                assertTrue(radioed(sortie, 0), "with the first pounce");
            }
        }

        assertTrue(pounces >= 2, "it pounced again: " + pounces);
        assertEquals(1, lines, "one line");
        assertEquals(0, rookLines, "Rook's needs him flying");
    }
}
