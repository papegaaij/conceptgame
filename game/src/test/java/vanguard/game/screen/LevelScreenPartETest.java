package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.game.level.RadioSchedule;
import vanguard.game.render.ArenaFixture;
import vanguard.sim.LevelScript;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;
import vanguard.sim.WingmanSpec;

/**
 * M5 part E's wiring in the level screen: the {@code ally-hit} cue names the ship hit last by its
 * name (Level 11's convoy, {@code {ally}}), Rook's line on a severed arm counts for his barks, and the
 * debrief's afloat row reads {@code HULLS AFLOAT n / 3}.
 */
class LevelScreenPartETest {
    @Test
    void theAllyHitCueNamesTheShipHitLast() {
        Sortie sortie = ArenaFixture.sortie();
        assertEquals(-1, LevelScreen.cueUnit(LevelScript.CueTrigger.ALLY_HIT, sortie), "before any hit");
        int hit = -1;
        for (int t = 0; t < 60 * 60 && hit < 0; t++) {
            sortie.step(0);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.ALLY_HIT) {
                    hit = events.value(i);
                }
            }
        }
        assertTrue(hit >= 0, "a slam hit a ship in a minute");
        assertEquals(hit, LevelScreen.cueUnit(LevelScript.CueTrigger.ALLY_HIT, sortie));
        String line = RadioSchedule.line("The {ally} is hit!", hit, sortie.allyName(hit), WingmanSpec.Side.LEFT);
        assertEquals("The " + ArenaFixture.SHIPS.get(hit) + " is hit!", line);
    }

    @Test
    void anUnnamedUnitKeepsItsNumberWord() {
        assertEquals("Crawler Two is hit!", RadioSchedule.line("Crawler {ally} is hit!", 1, "", WingmanSpec.Side.LEFT));
        assertEquals("Pull {ally} out!", RadioSchedule.line("Pull {ally} out!", -1, "Mbeki", WingmanSpec.Side.RIGHT));
    }

    @Test
    void rooksArmLineCountsForHisBarksSpacing() {
        assertTrue(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.BOSS_PART_DESTROYED));
    }

    @Test
    void theDebriefsAfloatRowCountsTheHulls() {
        DebriefScreen.Row met = DebriefScreen.hullsRow(new DebriefScreen.Hulls(3, 3), true, 160);
        assertEquals("HULLS AFLOAT", met.label());
        assertEquals("3 / 3", met.middle());
        assertEquals("+ 160 CR", met.right(1));
        DebriefScreen.Row missed = DebriefScreen.hullsRow(new DebriefScreen.Hulls(2, 3), false, 0);
        assertEquals("2 / 3", missed.middle());
        assertEquals("", missed.right(1), "nothing paid");
    }
}
