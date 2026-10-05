package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.game.render.TestFireView;
import vanguard.sim.Armament;
import vanguard.sim.PlayField;

/**
 * The test-fire box fits the shop drawer, below the message's two lines, and what it shows fits the
 * box: the play field turned a quarter at {@link TestFireView#SCALE}, the ship's whole hull and every
 * dummy lane of every slot across its 64 px (inside the one-pixel frame), nearly the whole field's
 * length along its 264 px, and the longest label in the 8 px label font.
 */
class TestFirePanelLayoutTest {
    /** The label font's cell and the ink of a line, with its one-pixel shadow. */
    private static final int LABEL_CELL = 8;

    private static final int LABEL_INK = 10;
    /** The inset's frame, box px. */
    private static final int FRAME = 1;
    /** Half the 48 px hull. */
    private static final double HULL = 24;

    @Test
    void theBoxSitsInTheShopDrawerBelowTheMessage() {
        assertTrue(TestFirePanel.X >= ShopPanel.X + 4, "left edge");
        assertTrue(TestFirePanel.X + TestFirePanel.WIDTH <= ShopPanel.X + ShopPanel.WIDTH - 4, "right edge");
        assertTrue(TestFirePanel.Y + TestFirePanel.HEIGHT <= ShopPanel.Y + ShopPanel.HEIGHT - 4, "bottom edge");
        assertTrue(ShopPanel.MESSAGE_Y + 12 + LABEL_INK <= TestFirePanel.Y, "below the message's second line");
    }

    @Test
    void theShipAndEveryDummyLaneFitAcrossTheBox() {
        double half = (TestFirePanel.HEIGHT / 2.0 - FRAME) / TestFireView.SCALE;
        double low = TestFirePanel.CENTRE_X - half;
        double high = TestFirePanel.CENTRE_X + half;
        assertTrue(TestFire.SHIP_X - HULL >= low && TestFire.SHIP_X + HULL <= high, "the hull");
        for (Armament.Slot slot : Armament.Slot.values()) {
            for (double lane : TestFire.LANES) {
                double x = TestFire.SHIP_X + TestFire.line(slot) + lane;
                assertTrue(
                        x - TestFire.TARGET_SIZE / 2 >= low && x + TestFire.TARGET_SIZE / 2 <= high,
                        slot + " lane " + lane + " at " + x + " outside " + low + ".." + high);
            }
        }
    }

    @Test
    void nearlyTheWholeFieldShowsAlongTheBox() {
        double half = (TestFirePanel.WIDTH / 2.0 - FRAME) / TestFireView.SCALE;
        double low = TestFirePanel.CENTRE_Y - half;
        double high = TestFirePanel.CENTRE_Y + half;
        // The dummies show from their first steps in to their last ones before they leave at the bottom.
        assertTrue(low <= TestFire.TARGET_SIZE / 2 && low >= 0, "behind: " + low);
        assertTrue(high >= PlayField.HEIGHT - TestFire.TARGET_SIZE / 2 && high <= PlayField.HEIGHT, "ahead: " + high);
        assertTrue(TestFire.SHIP_Y - HULL >= low && TestFire.SHIP_Y + HULL <= high, "the hull");
    }

    @Test
    void theLongestLabelFitsTheBox() {
        for (String label :
                List.of(TestFirePanel.NOT_IN_FLIGHT, TestFirePanel.WEAPONS_ONLY, TestFirePanel.label(5, true))) {
            assertTrue(
                    TestFirePanel.LABEL_X + label.length() * LABEL_CELL <= TestFirePanel.X + TestFirePanel.WIDTH - 4,
                    label);
        }
        assertTrue(TestFirePanel.LABEL_Y + LABEL_INK <= TestFirePanel.Y + TestFirePanel.HEIGHT, "label height");
    }
}
