package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import vanguard.content.campaign.Flight;
import vanguard.sim.Defences;
import vanguard.sim.Sortie;

/**
 * The right HUD panel (design/ui/hud, ship): armour and shield bars with their numbers (the shield
 * bar flickers while it is down after a break), the spare power with its shield regen bonus (lit
 * up during an overdrive), the four weapon slots with their level pips and the overdrive timer.
 * Under them, until the specials and utility modules fly (later in M4): the fitted items the sortie
 * leaves out, as "not yet available". Special and escort follow with the parts that need them.
 */
final class ShipPanel {
    private static final int X = PixelScreen.WIDTH - HudKit.PANEL_WIDTH;
    private static final int BAR_HEIGHT = 12;
    private static final int LEVEL_PIPS = 5;
    /** The spare power that gives the most regen bonus, MW (design/player/generator: +10 % per MW, at most +50 %). */
    private static final double FULL_SPARE = 5;
    /** The fitted items that do not fly yet: two bays and the special. */
    private static final int MAX_NOT_FLOWN = 3;
    /** The shield bar flickers at this many frames per phase while it is down after a break. */
    private static final int FLICKER_FRAMES = 8;

    private static final int ROW = 14;
    private static final int OVERDRIVE_PIPS = 10;

    private static final Color ARMOUR = Color.valueOf("FF4400");
    private static final Color ARMOUR_EMPTY = Color.valueOf("2A0B00");
    private static final Color SHIELD = Color.valueOf("00FFFF");
    private static final Color SHIELD_EMPTY = Color.valueOf("002A2A");
    private static final Color POWER = Color.valueOf("40FF80");
    private static final Color POWER_EMPTY = Color.valueOf("0B2A15");
    private static final Color PIP_EMPTY = Color.valueOf("3A3A20");
    /** The slot letters of the mock, front, rear, left and right (design/ui/hud). */
    private static final String[] SLOT_LETTERS = {"F", "R", "L", "R"};

    private final HudKit kit;
    /** How long an overdrive lasts (design/player, in-level pickups), the timer's full length. */
    private final double overdriveLength;

    private int frame;

    ShipPanel(HudKit kit, double overdriveLength) {
        this.kit = kit;
        this.overdriveLength = overdriveLength;
    }

    /**
     * @param weapons the flown weapons by slot (front, rear, left wing, right wing), {@code null} for an empty slot
     * @param sparePower MW
     * @param regenBonus the shield regen bonus of the spare power, 0..0.5
     */
    void draw(
            SpriteBatch batch,
            Sortie sortie,
            Flight.Weapon[] weapons,
            double sparePower,
            double regenBonus,
            List<String> notFlown) {
        frame++;
        Defences defences = sortie.ship().defences();
        kit.rightPanel(batch);
        int x = X + HudKit.INSET;
        int y = HudKit.TOP - HudKit.INSET;
        gauge(batch, "ARMOUR", defences.armour(), defences.maxArmour(), ARMOUR, ARMOUR_EMPTY, x, y, true);
        boolean flicker = defences.broken() && frame / FLICKER_FRAMES % 2 == 0;
        gauge(batch, "SHIELD", defences.shield(), defences.maxShield(), SHIELD, SHIELD_EMPTY, x, y - 52, !flicker);
        power(batch, sparePower, regenBonus, sortie.overdriveSeconds() > 0, x, y - 104);
        weapons(batch, weapons, sortie.overdriveSeconds(), x, y - 148);
        if (!notFlown.isEmpty()) {
            kit.label(batch, "NOT YET AVAILABLE", x, y - 268);
            for (int i = 0; i < Math.min(notFlown.size(), MAX_NOT_FLOWN); i++) {
                String name = notFlown.get(i).toUpperCase(Locale.ROOT);
                kit.text(batch, kit.small, name, HudKit.LABEL, x + 8, y - 286 - i * 13);
            }
        }
    }

    private void power(SpriteBatch batch, double spare, double bonus, boolean overdrive, int x, int y) {
        kit.label(batch, "POWER", x, y);
        String percent = "+" + Math.round(bonus * 100) + "%";
        kit.textRight(batch, kit.body, percent, HudKit.LABEL, x, y, HudKit.INNER_WIDTH);
        int barY = y - 34;
        kit.segments(
                batch,
                POWER,
                POWER_EMPTY,
                x,
                barY,
                HudKit.INNER_WIDTH,
                BAR_HEIGHT,
                Math.clamp(spare / FULL_SPARE, 0, 1));
        if (overdrive) {
            kit.glow(batch, HudKit.AMBER, x, barY, HudKit.INNER_WIDTH, BAR_HEIGHT);
        }
    }

    private void weapons(SpriteBatch batch, Flight.Weapon[] weapons, double overdriveSeconds, int x, int y) {
        kit.label(batch, "WEAPONS", x, y);
        int top = y - 22;
        int height = weapons.length * ROW + ROW + 10;
        kit.lcd(batch, x, top - height, HudKit.INNER_WIDTH, height);
        kit.glow(batch, HudKit.READOUT, x, top - height, HudKit.INNER_WIDTH, height);
        int pipsX = x + HudKit.INNER_WIDTH - 6 - LEVEL_PIPS * 10;
        for (int slot = 0; slot < weapons.length; slot++) {
            int rowTop = top - 5 - slot * ROW;
            Flight.Weapon weapon = weapons[slot];
            Color colour = weapon == null ? HudKit.LABEL : HudKit.READOUT;
            kit.text(batch, kit.small, SLOT_LETTERS[slot], HudKit.LABEL, x + 6, rowTop);
            String name = weapon == null ? "-" : hudName(weapon.name());
            kit.text(batch, kit.small, name, colour, x + 20, rowTop, pipsX - x - 24);
            int level = weapon == null ? 0 : weapon.level();
            for (int i = 0; i < LEVEL_PIPS; i++) {
                kit.fill(batch, i < level ? HudKit.AMBER : PIP_EMPTY, pipsX + i * 10, rowTop - 9, 8, 7);
            }
        }
        int rowTop = top - 7 - weapons.length * ROW;
        boolean on = overdriveSeconds > 0;
        kit.text(batch, kit.small, "OVERDRIVE", on ? HudKit.AMBER : HudKit.LABEL, x + 6, rowTop);
        int lit = (int) Math.ceil(overdriveSeconds / overdriveLength * OVERDRIVE_PIPS);
        int barX = x + 86;
        for (int i = 0; i < OVERDRIVE_PIPS; i++) {
            kit.fill(batch, i < lit ? HudKit.AMBER : PIP_EMPTY, barX + i * 8, rowTop - 9, 6, 7);
        }
        if (on) {
            kit.textRight(
                    batch,
                    kit.small,
                    (int) Math.ceil(overdriveSeconds) + "s",
                    HudKit.AMBER,
                    x,
                    rowTop,
                    HudKit.INNER_WIDTH - 6);
        }
    }

    /** The HUD's short weapon names: a pod is named by what it fires ("Micro-missile"). */
    static String hudName(String name) {
        String shortName = name.endsWith(" Pod") ? name.substring(0, name.length() - 4) : name;
        return shortName.toUpperCase(Locale.ROOT);
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
