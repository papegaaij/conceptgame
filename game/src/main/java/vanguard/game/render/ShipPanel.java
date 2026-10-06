package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Locale;
import vanguard.content.campaign.Flight;
import vanguard.game.level.LowArmour;
import vanguard.sim.Defences;
import vanguard.sim.Sortie;
import vanguard.sim.SpecialSlot;
import vanguard.sim.Wingman;

/**
 * The right HUD panel (design/ui/hud, ship): armour and shield bars with their numbers (the armour
 * gauge flashes red at 30 % and faster at 15 %, {@link LowArmour}; the shield bar flickers while it
 * is down after a break), the spare power with its shield regen bonus (lit
 * up during an overdrive), the four weapon slots with their level pips and the overdrive timer,
 * and the special's row in the same style: its 16 px hangar icon, name and charges, greyed while
 * its strike flies or with no charge left, flashing red when the special button is denied. Under
 * the special the escort box (M5 part A) while Rook flies in the level: the {@code ESCORT} plate, his
 * 16 px hangar icon and {@code ROOK}, his armour as a bar with the number, flashing red at 30 % as
 * the player's; after he ejects the bar is empty and the well reads {@code EJECTED} in red. Without
 * an escort its region stays empty. Under it: the fitted items the sortie leaves out, as "not yet
 * available".
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
    /** The special's row flashes red this many frames after a denied press, switching every few. */
    private static final int DENIED_FRAMES = 30;

    private static final int DENIED_PHASE_FRAMES = 5;
    /**
     * The armour gauge's red flash: lit for this many simulation steps, then dark as long, at 30 %
     * and at 15 %: half the low-armour beep's period (1.2 s and 0.6 s, FlightSounds), one flash a beep.
     */
    static final int LOW_ARMOUR_PHASE_TICKS = 36;

    static final int CRITICAL_ARMOUR_PHASE_TICKS = 18;
    private static final float GREYED = 0.35f;
    /** The special's row: its well's top below the panel's top inset, and its height. */
    static final int SPECIAL_TOP = 258;

    static final int SPECIAL_HEIGHT = ROW + 10;
    /** The escort's plate, below the special's row, and its well under the plate. */
    static final int ESCORT_PLATE = 292;

    static final int ESCORT_TOP = ESCORT_PLATE + 22;
    static final int ESCORT_HEIGHT = 38;
    /** The items that do not fly yet, under the escort's region (empty without an escort). */
    static final int NOT_FLOWN_PLATE = ESCORT_TOP + ESCORT_HEIGHT + 18;
    /** The escort's well: the icon's and the name's left, the bar's inset and height. */
    static final int ESCORT_ICON_X = 6;

    static final int ESCORT_NAME_X = 28;
    static final int ESCORT_BAR_INSET = 6;
    static final int ESCORT_BAR_HEIGHT = 8;
    static final String ESCORT_NAME = "ROOK";
    static final String EJECTED = "EJECTED";

    private static final Color ARMOUR = Color.valueOf("FF4400");
    private static final Color ARMOUR_EMPTY = Color.valueOf("2A0B00");
    /** The low-armour flash laid over the armour gauge: the HUD's alert red, see-through. */
    private static final Color ALARM = new Color(HudKit.ALERT.r, HudKit.ALERT.g, HudKit.ALERT.b, 0.45f);

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

    /** The fitted special's 16 px hangar icon; {@code null} without a special that flies. */
    private final TextureRegion specialIcon;
    /** Rook's 16 px hangar icon. */
    private final TextureRegion escortIcon;
    /** "×" when the font has it, else "x". */
    private final String times;

    private int frame;
    private int denied;

    ShipPanel(HudKit kit, double overdriveLength, TextureRegion specialIcon, TextureRegion escortIcon) {
        this.kit = kit;
        this.overdriveLength = overdriveLength;
        this.specialIcon = specialIcon;
        this.escortIcon = escortIcon;
        times = kit.small.getData().hasGlyph('\u00d7') ? "\u00d7" : "x";
    }

    /** The special button was denied: the special's row flashes. */
    void specialDenied() {
        denied = DENIED_FRAMES;
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
        boolean alarm = armourFlash(LowArmour.of(defences), sortie.tick());
        gauge(batch, "ARMOUR", defences.armour(), defences.maxArmour(), ARMOUR, ARMOUR_EMPTY, x, y, true, alarm);
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
                !flicker,
                false);
        power(batch, sparePower, regenBonus, sortie.overdriveSeconds() > 0, x, y - 104);
        weapons(batch, weapons, sortie.overdriveSeconds(), x, y - 148);
        special(batch, sortie.special(), x, y - SPECIAL_TOP);
        if (denied > 0) {
            denied--;
        }
        Wingman rook = sortie.wingman().orElse(null);
        if (rook != null) {
            escort(batch, rook, sortie.tick(), x, y);
        }
        if (!notFlown.isEmpty()) {
            kit.label(batch, "NOT YET AVAILABLE", x, y - NOT_FLOWN_PLATE);
            for (int i = 0; i < Math.min(notFlown.size(), MAX_NOT_FLOWN); i++) {
                String name = notFlown.get(i).toUpperCase(Locale.ROOT);
                kit.text(batch, kit.small, name, HudKit.LABEL, x + 8, y - NOT_FLOWN_PLATE - 18 - i * 13);
            }
        }
    }

    /**
     * The escort box under the special's row: {@code ESCORT}, then {@code [icon] ROOK} with his armour
     * number over his armour bar; the bar empty and {@code EJECTED} in red once he is out.
     */
    private void escort(SpriteBatch batch, Wingman rook, long tick, int x, int y) {
        kit.label(batch, "ESCORT", x, y - ESCORT_PLATE);
        int top = y - ESCORT_TOP;
        kit.lcd(batch, x, top - ESCORT_HEIGHT, HudKit.INNER_WIDTH, ESCORT_HEIGHT);
        boolean out = rook.ejected();
        boolean alarm = !out && armourFlash(LowArmour.of(rook.armour(), rook.maxArmour()), tick);
        kit.glow(batch, out ? HudKit.ALERT : HudKit.READOUT, x, top - ESCORT_HEIGHT, HudKit.INNER_WIDTH, ESCORT_HEIGHT);
        int rowTop = top - 5;
        batch.draw(escortIcon, x + ESCORT_ICON_X, top - 4 - escortIcon.getRegionHeight());
        kit.text(batch, kit.small, ESCORT_NAME, out ? HudKit.LABEL : HudKit.READOUT, x + ESCORT_NAME_X, rowTop);
        String number = out ? EJECTED : Integer.toString((int) Math.ceil(rook.armour()));
        kit.textRight(
                batch,
                kit.small,
                number,
                out || alarm ? HudKit.ALERT : HudKit.LABEL,
                x,
                rowTop,
                HudKit.INNER_WIDTH - ESCORT_BAR_INSET);
        int barX = x + ESCORT_BAR_INSET;
        int barWidth = HudKit.INNER_WIDTH - 2 * ESCORT_BAR_INSET;
        int barY = top - ESCORT_HEIGHT + ESCORT_BAR_INSET;
        kit.fill(batch, ARMOUR_EMPTY, barX, barY, barWidth, ESCORT_BAR_HEIGHT);
        double share = out ? 0 : Math.clamp(rook.armour() / rook.maxArmour(), 0, 1);
        kit.fill(batch, ARMOUR, barX, barY, (float) Math.floor(barWidth * share), ESCORT_BAR_HEIGHT);
        if (alarm) {
            kit.fill(batch, ALARM, barX, barY, barWidth, ESCORT_BAR_HEIGHT);
            kit.glow(batch, HudKit.ALERT, barX - 4, barY - 4, barWidth + 8, ESCORT_BAR_HEIGHT + 8);
        }
    }

    /** The special's row below the weapons box: {@code SPECIAL [icon] AIRSTRIKE ×2}. */
    private void special(SpriteBatch batch, SpecialSlot special, int x, int top) {
        int height = SPECIAL_HEIGHT;
        kit.lcd(batch, x, top - height, HudKit.INNER_WIDTH, height);
        boolean flash = denied > 0 && denied / DENIED_PHASE_FRAMES % 2 == 0;
        kit.glow(batch, flash ? HudKit.ALERT : HudKit.READOUT, x, top - height, HudKit.INNER_WIDTH, height);
        int rowTop = top - 5;
        kit.text(batch, kit.small, "SPECIAL", flash ? HudKit.ALERT : HudKit.LABEL, x + 6, rowTop);
        if (!special.fitted() || specialIcon == null) {
            kit.text(batch, kit.small, "-", HudKit.LABEL, x + 84, rowTop);
            return;
        }
        boolean ready = special.ready();
        float shade = ready ? 1 : GREYED;
        batch.setColor(1, 1, 1, shade);
        batch.draw(specialIcon, x + 64, top - 4 - specialIcon.getRegionHeight());
        batch.setColor(Color.WHITE);
        Color colour = flash ? HudKit.ALERT : ready ? HudKit.READOUT : HudKit.LABEL;
        String charges = times + special.charges();
        kit.text(batch, kit.small, special.name().toUpperCase(Locale.ROOT), colour, x + 84, rowTop, 90);
        kit.textRight(batch, kit.small, charges, colour, x, rowTop, HudKit.INNER_WIDTH - 6);
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

    /**
     * Whether the armour gauge shows its red flash at {@code tick}: never above 30 %, then lit and
     * dark in turns, faster at 15 % (design/ui/hud, right panel), lit from the stage's first step.
     */
    static boolean armourFlash(LowArmour stage, long tick) {
        return switch (stage) {
            case NONE -> false;
            case LOW -> tick / LOW_ARMOUR_PHASE_TICKS % 2 == 0;
            case CRITICAL -> tick / CRITICAL_ARMOUR_PHASE_TICKS % 2 == 0;
        };
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
            boolean lit,
            boolean alarm) {
        kit.label(batch, name, x, y);
        kit.textRight(
                batch,
                kit.body,
                Integer.toString((int) Math.ceil(value)),
                alarm ? HudKit.ALERT : HudKit.LABEL,
                x,
                y,
                HudKit.INNER_WIDTH);
        kit.segments(batch, full, empty, x, y - 34, HudKit.INNER_WIDTH, BAR_HEIGHT, lit ? value / max : 0);
        if (alarm) {
            kit.fill(batch, ALARM, x, y - 34, HudKit.INNER_WIDTH, BAR_HEIGHT);
            kit.glow(batch, HudKit.ALERT, x - 4, y - 38, HudKit.INNER_WIDTH + 8, BAR_HEIGHT + 8);
        }
    }
}
