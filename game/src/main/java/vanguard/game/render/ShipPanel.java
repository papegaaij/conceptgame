package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import vanguard.sim.Defences;

/**
 * The right HUD panel (design/ui/hud, ship): armour and shield bars with their numbers (the shield
 * bar flickers while it is down after a break) and the front weapon with its level pips. Under it,
 * TEMPORARY until M4 flies them: the fitted items the sortie leaves out, as "not yet available".
 * Power, special and escort follow with the parts that need them.
 */
final class ShipPanel {
    private static final int X = PixelScreen.WIDTH - HudKit.PANEL_WIDTH;
    private static final int BAR_HEIGHT = 12;
    private static final int LEVEL_PIPS = 5;
    /** The fitted items that do not fly yet: at most a mount each, two bays and the special. */
    private static final int MAX_NOT_FLOWN = 7;
    /** The shield bar flickers at this many frames per phase while it is down after a break. */
    private static final int FLICKER_FRAMES = 8;

    private static final Color ARMOUR = Color.valueOf("FF4400");
    private static final Color ARMOUR_EMPTY = Color.valueOf("2A0B00");
    private static final Color SHIELD = Color.valueOf("00FFFF");
    private static final Color SHIELD_EMPTY = Color.valueOf("002A2A");
    private static final Color PIP_EMPTY = Color.valueOf("3A3A20");

    private final HudKit kit;
    private int frame;

    ShipPanel(HudKit kit) {
        this.kit = kit;
    }

    void draw(SpriteBatch batch, Defences defences, String weapon, int weaponLevel, List<String> notFlown) {
        frame++;
        kit.rightPanel(batch);
        int x = X + HudKit.INSET;
        int y = HudKit.TOP - HudKit.INSET;
        gauge(batch, "ARMOUR", defences.armour(), defences.maxArmour(), ARMOUR, ARMOUR_EMPTY, x, y, true);
        boolean flicker = defences.broken() && frame / FLICKER_FRAMES % 2 == 0;
        gauge(batch, "SHIELD", defences.shield(), defences.maxShield(), SHIELD, SHIELD_EMPTY, x, y - 52, !flicker);

        kit.label(batch, "WEAPONS", x, y - 112);
        kit.lcd(batch, x, y - 186, HudKit.INNER_WIDTH, 52);
        kit.glow(batch, HudKit.READOUT, x, y - 186, HudKit.INNER_WIDTH, 52);
        kit.text(batch, kit.small, "FRONT", HudKit.LABEL, x + 8, y - 138);
        for (int i = 0; i < LEVEL_PIPS; i++) {
            kit.fill(
                    batch,
                    i < weaponLevel ? HudKit.AMBER : PIP_EMPTY,
                    x + HudKit.INNER_WIDTH - 8 - (LEVEL_PIPS - i) * 12,
                    y - 148,
                    9,
                    7);
        }
        kit.text(batch, kit.body, weapon, HudKit.READOUT, x + 8, y - 158);
        if (!notFlown.isEmpty()) {
            kit.label(batch, "NOT YET AVAILABLE", x, y - 204);
            for (int i = 0; i < Math.min(notFlown.size(), MAX_NOT_FLOWN); i++) {
                String name = notFlown.get(i).toUpperCase(Locale.ROOT);
                kit.text(batch, kit.small, name, HudKit.LABEL, x + 8, y - 222 - i * 13);
            }
        }
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
            boolean lit) {
        kit.label(batch, name, x, y);
        kit.textRight(
                batch, kit.body, Integer.toString((int) Math.ceil(value)), HudKit.LABEL, x, y, HudKit.INNER_WIDTH);
        kit.segments(batch, full, empty, x, y - 34, HudKit.INNER_WIDTH, BAR_HEIGHT, lit ? value / max : 0);
    }
}
