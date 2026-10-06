package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import org.junit.jupiter.api.Test;
import vanguard.game.ui.Fonts;

/**
 * The right panel's escort box (design/ui/hud, M5 part A) sits under the special's row without
 * overlapping it or the not-yet-available list, inside the panel, and what it shows fits its well,
 * measured with the label font's metrics (read without a GL context).
 */
class EscortBoxLayoutTest {
    private static final BitmapFont.BitmapFontData SMALL = new BitmapFont.BitmapFontData(
            new FileHandle(new File(System.getProperty("vanguard.assetsDir"), Fonts.LABEL_FILE)), false);
    /** The label plate's height (tools/art/hud.py): its top 4 px above the label's text top. */
    private static final int PLATE = 22;
    /** The well's frame around its glass. */
    private static final int FRAME = MissionLayout.FRAME;

    @Test
    void theBoxSitsUnderTheSpecialRowAboveTheNotFlownListInsideThePanel() {
        int specialBottom = ShipPanel.SPECIAL_TOP + ShipPanel.SPECIAL_HEIGHT + FRAME;
        int plateTop = ShipPanel.ESCORT_PLATE - 4;
        assertTrue(specialBottom < plateTop, "the plate under the special's well: " + specialBottom + " < " + plateTop);
        assertTrue(plateTop + PLATE <= ShipPanel.ESCORT_TOP - FRAME, "the well under the plate");
        int boxBottom = ShipPanel.ESCORT_TOP + ShipPanel.ESCORT_HEIGHT + FRAME;
        assertTrue(boxBottom < ShipPanel.NOT_FLOWN_PLATE - 4, "the not-flown plate under the box");
        int notFlownBottom = ShipPanel.NOT_FLOWN_PLATE + 18 + 3 * 13;
        assertTrue(
                HudKit.TOP - HudKit.INSET - notFlownBottom > HudKit.INSET,
                "the not-flown list ends inside the panel: " + notFlownBottom);
    }

    @Test
    void theIconNameAndNumberFitTheTopRowAndTheBarFitsUnderIt() {
        int icon = 16;
        assertTrue(ShipPanel.ESCORT_ICON_X + icon < ShipPanel.ESCORT_NAME_X, "the name right of the icon");
        int name = ShipPanel.ESCORT_NAME_X + width(ShipPanel.ESCORT_NAME);
        int widest = Math.max(width(ShipPanel.EJECTED), width("80"));
        int numberLeft = HudKit.INNER_WIDTH - ShipPanel.ESCORT_BAR_INSET - widest;
        assertTrue(name + 8 <= numberLeft, "ROOK and EJECTED apart on one row: " + name + " / " + numberLeft);
        int rowBottom = 4 + icon;
        int barTop = ShipPanel.ESCORT_HEIGHT - ShipPanel.ESCORT_BAR_INSET - ShipPanel.ESCORT_BAR_HEIGHT;
        assertTrue(rowBottom <= barTop, "the bar under the icon row: " + rowBottom + " <= " + barTop);
        assertTrue(5 + SMALL.capHeight - SMALL.descent <= barTop, "the text above the bar");
    }

    private static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += SMALL.getGlyph(text.charAt(i)).xadvance;
        }
        return width;
    }
}
