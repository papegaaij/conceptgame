package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import vanguard.sim.Defences;

/**
 * The minimal M1 HUD in the two side panels (design/ui/hud): shield and armour bars with their
 * numbers and the front weapon on the right, the sandbox's name and attempt on the left. A
 * placeholder in the metal style of the chosen HUD A (colours sampled from hud-r08-a); the UI kit
 * and its bitmap fonts replace the frames and libGDX's default font later.
 */
public final class HudPanels {
    private static final int PANEL_WIDTH = PixelScreen.PLAY_FIELD_X;
    private static final int RIGHT_X = PixelScreen.WIDTH - PANEL_WIDTH;
    private static final int TOP = PixelScreen.HEIGHT;
    private static final int INSET = 16;
    private static final int BAR_HEIGHT = 12;
    private static final int LEVEL_PIPS = 5;
    private static final int LABEL_WIDTH = 120;
    /** The shield bar flickers at this many frames per phase while it is down after a break. */
    private static final int FLICKER_FRAMES = 8;

    private static final Color METAL = Color.valueOf("5C669A");
    private static final Color METAL_LIGHT = Color.valueOf("8A96D0");
    private static final Color METAL_DARK = Color.valueOf("121632");
    private static final Color LCD = Color.valueOf("06061A");
    private static final Color LABEL = Color.valueOf("E0E6FF");
    private static final Color READOUT = Color.valueOf("40FF80");
    private static final Color ARMOUR = Color.valueOf("FF4400");
    private static final Color ARMOUR_EMPTY = Color.valueOf("2A0B00");
    private static final Color SHIELD = Color.valueOf("00FFFF");
    private static final Color SHIELD_EMPTY = Color.valueOf("002A2A");
    private static final Color PIP_EMPTY = Color.valueOf("3A3A20");
    private static final Color PIP = Color.valueOf("FFE04A");

    private final TextureRegion pixel;
    private final BitmapFont font;
    private int frame;

    public HudPanels(TextureRegion pixel, BitmapFont font) {
        this.pixel = pixel;
        this.font = font;
    }

    /**
     * @param weapon the front weapon's name
     * @param weaponLevel its upgrade level, 1..5
     * @param attempt the sortie attempt, counting retries
     */
    public void draw(SpriteBatch batch, Defences defences, String weapon, int weaponLevel, int attempt) {
        frame++;
        panel(batch, 0);
        panel(batch, RIGHT_X);

        int y = TOP - INSET;
        label(batch, "TEST SORTIE", INSET, y);
        lcd(batch, INSET, y - 52, 30);
        readout(batch, "M1 SANDBOX", INSET + 8, y - 30);
        label(batch, "ATTEMPT", INSET, y - 72);
        lcd(batch, INSET, y - 124, 30);
        readout(batch, Integer.toString(attempt), INSET + 8, y - 102);

        int x = RIGHT_X + INSET;
        int width = PANEL_WIDTH - 2 * INSET;
        gauge(batch, "ARMOUR", defences.armour(), defences.maxArmour(), ARMOUR, ARMOUR_EMPTY, x, y, width, true);
        boolean flicker = defences.broken() && frame / FLICKER_FRAMES % 2 == 0;
        gauge(
                batch,
                "SHIELD",
                defences.shield(),
                defences.maxShield(),
                SHIELD,
                SHIELD_EMPTY,
                x,
                y - 52,
                width,
                !flicker);

        label(batch, "WEAPONS", x, y - 112);
        lcd(batch, x, y - 186, 52);
        font.setColor(LABEL);
        font.draw(batch, "FRONT", x + 8, y - 138);
        for (int i = 0; i < LEVEL_PIPS; i++) {
            fill(batch, i < weaponLevel ? PIP : PIP_EMPTY, x + width - 8 - (LEVEL_PIPS - i) * 12, y - 148, 9, 7);
        }
        readout(batch, weapon, x + 8, y - 158);
        batch.setColor(Color.WHITE);
    }

    private void gauge(
            SpriteBatch batch,
            String name,
            double value,
            double max,
            Color full,
            Color empty,
            int x,
            int y,
            int width,
            boolean lit) {
        label(batch, name, x, y);
        font.setColor(LABEL);
        font.draw(batch, Integer.toString((int) Math.ceil(value)), x, y, width, Align.right, false);
        int barY = y - 34;
        fill(batch, METAL_DARK, x - 2, barY - 2, width + 4, BAR_HEIGHT + 4);
        fill(batch, empty, x, barY, width, BAR_HEIGHT);
        if (lit) {
            fill(batch, full, x, barY, (float) (width * Math.clamp(value / max, 0, 1)), BAR_HEIGHT);
        }
    }

    private void panel(SpriteBatch batch, int x) {
        fill(batch, METAL_DARK, x, 0, PANEL_WIDTH, TOP);
        fill(batch, METAL, x + 4, 4, PANEL_WIDTH - 8, TOP - 8);
        fill(batch, METAL_LIGHT, x + 4, TOP - 5, PANEL_WIDTH - 8, 1);
        fill(batch, METAL_LIGHT, x + 4, 4, 1, TOP - 8);
    }

    private void label(SpriteBatch batch, String text, int x, int y) {
        fill(batch, METAL_DARK, x - 4, y - 18, LABEL_WIDTH, 22);
        font.setColor(LABEL);
        font.draw(batch, text, x, y);
    }

    private void lcd(SpriteBatch batch, int x, int y, int height) {
        fill(batch, METAL_DARK, x - 2, y - 2, PANEL_WIDTH - 2 * INSET + 4, height + 4);
        fill(batch, LCD, x, y, PANEL_WIDTH - 2 * INSET, height);
    }

    private void readout(SpriteBatch batch, String text, int x, int y) {
        font.setColor(READOUT);
        font.draw(batch, text, x, y);
    }

    private void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }
}
