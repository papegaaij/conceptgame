package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.game.ui.Fonts;
import vanguard.sim.Armament;
import vanguard.sim.Loadout;
import vanguard.sim.Sortie;

/**
 * M5 part E's HUD (design/ui/hud): the naval convoy's one-line tracker ({@code CONVOY} with a pip per
 * cargo ship, {@code DONE} or {@code FAILED}) fits its well and colours its pips by each ship's state,
 * and a Torpedo Pod in a level without water reads {@code NO WATER} in its weapon row (it fits beside
 * the weapon's name), but not over water.
 */
class PartEHudTest {
    private static final BitmapFont.BitmapFontData SMALL = font(Fonts.LABEL_FILE);
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);

    @Test
    void theConvoyTrackerFitsOneLine() {
        int pips = 3 * MissionPanel.ALLY_PIP_STEP;
        assertTrue(
                width(BODY, MissionPanel.CONVOY) + pips <= MissionLayout.TEXT_WIDTH,
                "the label and three pips fit: " + (width(BODY, MissionPanel.CONVOY) + pips));
        for (String status : List.of("DONE", "FAILED")) {
            assertTrue(width(BODY, MissionPanel.CONVOY) + 8 + width(BODY, status) <= MissionLayout.TEXT_WIDTH, status);
        }
    }

    @Test
    void aShipsPipIsGreenAmberAfterASlamWhiteOnAHitAndFlashesRedThenDarkWhenSunk() {
        Color green = MissionPanel.convoyPip(false, false, false, 0);
        Color amber = MissionPanel.convoyPip(false, false, true, 0);
        assertEquals(HudKit.AMBER, amber);
        assertTrue(!green.equals(amber), "amber after its first slam");
        assertEquals(Color.WHITE, MissionPanel.convoyPip(false, true, true, 0), "a hit flashes white");
        Color sunk = MissionPanel.convoyPip(true, false, true, 0);
        Color dark = MissionPanel.convoyPip(true, false, true, 1000);
        assertTrue(!sunk.equals(dark), "a red flash, then dark");
        assertEquals("DONE", MissionPanel.convoyStatus(true, false, true));
        assertNull(MissionPanel.convoyStatus(false, true, false), "the pips until the boss is down");
        assertEquals("FAILED", MissionPanel.convoyStatus(false, true, true));
        assertNull(MissionPanel.convoyStatus(false, false, false));
    }

    @Test
    void noWaterFitsBesideTheTorpedoPodsName() {
        String name = ShipPanel.hudName("Torpedo Pod");
        assertEquals("TORPEDO", name);
        // The row: the slot letter, the name from 20 px, the reason right-aligned 6 px from the well's edge.
        assertTrue(
                20 + width(SMALL, name) + 4 + width(SMALL, ShipPanel.NO_WATER) <= HudKit.INNER_WIDTH - 6,
                "name and reason: " + (20 + width(SMALL, name) + 4 + width(SMALL, ShipPanel.NO_WATER)));
    }

    @Test
    void aTorpedoPodIsIdleOverLandButNotOverWater() {
        Loadout loadout = SimSpecs.loadout(
                ArenaFixture.CONTENT,
                ArenaFixture.CONTENT.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, SimSpecs.PULSE_CANNON, 1),
                        new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "torpedo-pod", 1)),
                ArenaFixture.CONTENT.shields().models().getFirst().name(),
                ArenaFixture.CONTENT.armour().plating().getFirst().name(),
                0,
                Difficulty.MEDIUM);
        String land = ArenaFixture.CONTENT.levelKey(1).orElseThrow();
        Sortie overLand = new Sortie(
                1,
                loadout,
                SimSpecs.level(ArenaFixture.CONTENT, land, Difficulty.MEDIUM),
                SimSpecs.rules(ArenaFixture.CONTENT, land, Difficulty.MEDIUM),
                60);
        boolean[] idle = new boolean[Armament.Slot.values().length];
        Hud.idle(overLand, idle);
        boolean[] expected = new boolean[idle.length];
        expected[Armament.Slot.LEFT_WING.ordinal()] = true;
        assertArrayEquals(expected, idle, "NO WATER in the left wing's row");
        Sortie overWater = new Sortie(
                1,
                loadout,
                ArenaFixture.level(),
                SimSpecs.rules(ArenaFixture.CONTENT, ArenaFixture.RULES_LEVEL, Difficulty.MEDIUM),
                60);
        Hud.idle(overWater, idle);
        assertArrayEquals(new boolean[idle.length], idle, "over water it runs");
    }

    private static BitmapFont.BitmapFontData font(String file) {
        return new BitmapFont.BitmapFontData(
                new FileHandle(new File(System.getProperty("vanguard.assetsDir"), file)), false);
    }

    private static int width(BitmapFont.BitmapFontData font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            BitmapFont.Glyph glyph = font.getGlyph(text.charAt(i));
            assertNotNull(glyph, "the font has '" + text.charAt(i) + "'");
            width += glyph.xadvance;
            if (i + 1 < text.length()) {
                width += glyph.getKerning(text.charAt(i + 1));
            }
        }
        return width;
    }
}
