package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Optional;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Catalogue.Item;
import vanguard.content.campaign.Catalogue.Stat;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Hangar.Choice;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Glass;

/**
 * The hangar's centre (design/ui/hangar, layout B): the Stormhawk's holographic schematic with a
 * holographic callout per weapon mount (level pips, the fitted item's icon and name) and the escort
 * slot (locked until Rook joins at Level 08, with his craft; then his callout: his icon, his fitted
 * gun's name with five level pips, his armour {@code nn/80} (amber below the launch warning's 50 %)
 * or {@code GROUNDED} in red, and his side
 * {@code L} or {@code R}), the module tiles of the core parts, the special and
 * the two utility bays, and the power bar in its trough with the load, the load the selected choice
 * would make in a lighter colour (red when it would not fit) and the generator's output.
 */
final class LoadoutPanel {
    private static final int X = 306;
    private static final int Y = 52;
    private static final int WIDTH = 356;
    private static final int SCHEMATIC_HEIGHT = 328;
    private static final int TILES_Y = Y + SCHEMATIC_HEIGHT + 6;
    private static final int POWER_Y = TILES_Y + 54;

    private static final Color BLUEPRINT = new Color(0.03f, 0.06f, 0.20f, 0.92f);
    private static final Color GRID = new Color(0.10f, 0.20f, 0.45f, 0.6f);
    private static final Color LINE = new Color(0.25f, 0.75f, 0.95f, 1);
    private static final Color PROJECTED = new Color(1, 1, 0.55f, 1);
    /** The locked escort's craft is drawn this much darker. */
    private static final Color LOCKED_ICON = new Color(0.45f, 0.45f, 0.55f, 1);

    private static final Color TICK = new Color(0, 0, 0.12f, 1);
    private static final int CENTRE_X = X + WIDTH / 2;
    private static final int SHIP_Y = Y + 200;
    private static final int SHIP_SCALE = 3;
    private static final int PIPS = 5;
    private static final List<LoadoutSlot> TILES = List.of(
            LoadoutSlot.GENERATOR,
            LoadoutSlot.SHIELD,
            LoadoutSlot.ARMOUR,
            LoadoutSlot.ENGINE,
            LoadoutSlot.SPECIAL,
            LoadoutSlot.UTILITY_1,
            LoadoutSlot.UTILITY_2);
    /** A module tile's width; its name is centred in the 8 px label font ({@link Names#tile}). */
    static final int TILE_WIDTH = 48;

    private static final int TILE_GAP = 2;

    private static final int WING_WIDTH = 132;
    private static final int FRONT_WIDTH = 160;
    /** Narrower than the front's, to clear the escort slot beside it. */
    private static final int REAR_WIDTH = 154;

    static final int ESCORT_WIDTH = 86;
    /** Rook's callout once he is hired: taller than the locked slot, between the left wing's callout and the panel's foot. */
    static final int ESCORT_X = X + 6;
    /** Wider than the locked slot (to the rear callout's edge), so his longest gun name fits. */
    static final int ESCORT_CALLOUT_WIDTH = 92;
    /** The callout's text starts this far in from its left edge. */
    static final int ESCORT_INSET = 4;

    static final int ESCORT_Y = Y + 226;
    static final int ESCORT_HEIGHT = 92;
    /** Rows of Rook's callout below its top: the plate, his name, his gun, its pips, his armour. */
    static final int ESCORT_NAME_ROW = 20;

    static final int ESCORT_GUN_ROW = 48;
    static final int ESCORT_PIPS_ROW = 62;
    static final int ESCORT_ARMOUR_ROW = 76;
    static final String GROUNDED = "GROUNDED";
    static final String ROOK = "ROOK";
    private static final int CALLOUT_HEIGHT = 46;
    private static final int FRONT_Y = Y + 24;
    private static final int REAR_Y = Y + 276;
    private static final int WING_Y = Y + 116;
    /** The fitted item's name starts right of its icon. */
    static final int NAME_INDENT = 32;

    private record Callout(LoadoutSlot slot, int x, int y, int width) {}

    private static final List<Callout> CALLOUTS = List.of(
            new Callout(LoadoutSlot.FRONT, CENTRE_X - FRONT_WIDTH / 2, FRONT_Y, FRONT_WIDTH),
            new Callout(LoadoutSlot.LEFT_WING, X + 8, WING_Y, WING_WIDTH),
            new Callout(LoadoutSlot.RIGHT_WING, X + WIDTH - 8 - WING_WIDTH, WING_Y, WING_WIDTH),
            new Callout(LoadoutSlot.REAR, CENTRE_X - REAR_WIDTH / 2, REAR_Y, REAR_WIDTH));

    private final Glass glass;
    private final TextureRegion ship;
    private final ItemIcons icons;

    LoadoutPanel(Glass glass, TextureRegion ship, ItemIcons icons) {
        this.glass = glass;
        this.ship = ship;
        this.icons = icons;
    }

    void draw(SpriteBatch batch, HangarState state) {
        Hangar hangar = state.hangar();
        glass.panel(batch, X, Y, WIDTH, SCHEMATIC_HEIGHT, 0.6f);
        glass.fill(batch, BLUEPRINT, X + 4, Y + 22, WIDTH - 8, SCHEMATIC_HEIGHT - 26);
        for (int x = X + 4; x < X + WIDTH - 4; x += 16) {
            glass.fill(batch, GRID, x, Y + 22, 1, SCHEMATIC_HEIGHT - 26);
        }
        for (int y = Y + 22; y < Y + SCHEMATIC_HEIGHT - 4; y += 16) {
            glass.fill(batch, GRID, X + 4, y, WIDTH - 8, 1);
        }
        glass.header(batch, "AF-12 STORMHAWK - SCHEMATIC", X + 10, X + WIDTH - 10, Y + 8);
        int shipWidth = ship.getRegionWidth() * SHIP_SCALE;
        int shipHeight = ship.getRegionHeight() * SHIP_SCALE;
        int frontBottom = FRONT_Y + CALLOUT_HEIGHT;
        glass.fill(batch, LINE, CENTRE_X, frontBottom, 1, SHIP_Y - shipHeight / 2 - frontBottom);
        glass.fill(batch, LINE, CENTRE_X, SHIP_Y + shipHeight / 2, 1, REAR_Y - SHIP_Y - shipHeight / 2);
        int wingLine = X + 8 + WING_WIDTH;
        int wingLineY = WING_Y + CALLOUT_HEIGHT / 2;
        glass.fill(batch, LINE, wingLine, wingLineY, CENTRE_X - 12 - wingLine, 1);
        glass.fill(batch, LINE, CENTRE_X + 12, wingLineY, X + WIDTH - 8 - WING_WIDTH - CENTRE_X - 12, 1);
        batch.draw(
                ship, CENTRE_X - shipWidth / 2f, PixelScreen.HEIGHT - SHIP_Y - shipHeight / 2f, shipWidth, shipHeight);
        for (Callout callout : CALLOUTS) {
            drawCallout(batch, hangar, callout, state.slot() == callout.slot());
        }
        if (hangar.escortHired()) {
            drawEscort(batch, hangar, state.slot() == LoadoutSlot.ESCORT);
        } else {
            int escortY = Y + 270;
            glass.lockedCallout(batch, X + 12, escortY, ESCORT_WIDTH, 48);
            glass.shadowed(batch, glass.fonts.label, "ESCORT", Glass.DIM, X + 18, escortY + 8);
            glass.shadowed(batch, glass.fonts.label, "ACT 2", Glass.DIM, X + 18, escortY + 26);
            TextureRegion rook = icons.escort().large();
            batch.setColor(LOCKED_ICON);
            batch.draw(
                    rook,
                    X + 12 + ESCORT_WIDTH - 6 - rook.getRegionWidth(),
                    PixelScreen.HEIGHT - escortY - 12 - rook.getRegionHeight());
            batch.setColor(Color.WHITE);
        }
        drawTiles(batch, hangar, state.slot());
        drawPower(batch, hangar, state.choice());
    }

    private void drawCallout(SpriteBatch batch, Hangar hangar, Callout callout, boolean selected) {
        Color edge = selected ? Glass.AMBER : LINE;
        glass.callout(batch, callout.x(), callout.y(), callout.width(), CALLOUT_HEIGHT, selected);
        glass.shadowed(batch, glass.fonts.label, Names.slot(callout.slot()), edge, callout.x() + 6, callout.y() + 6);
        Optional<Fitted> fitted =
                Optional.ofNullable(hangar.campaign().loadout().get(callout.slot()));
        Optional<Item> item = fitted.flatMap(f -> hangar.fitted(callout.slot()));
        if (item.isPresent()) {
            TextureRegion icon = icons.of(item.get()).large();
            batch.draw(icon, callout.x() + 5, PixelScreen.HEIGHT - callout.y() - 18 - icon.getRegionHeight());
        }
        glass.shadowed(
                batch,
                glass.fonts.label,
                item.map(i -> Names.callout(i.name())).orElse("EMPTY"),
                item.isPresent() ? Glass.WHITE : Glass.DIM,
                callout.x() + (item.isPresent() ? NAME_INDENT : 6),
                callout.y() + 26,
                callout.width() - NAME_INDENT - 2);
        int level = fitted.map(Fitted::level).orElse(0);
        for (int i = 0; i < PIPS; i++) {
            glass.bar(
                    batch,
                    i < level ? Glass.AMBER : Glass.UNLIT,
                    callout.x() + callout.width() - 6 - (PIPS - i) * 9,
                    callout.y() + 6,
                    7,
                    7);
        }
    }

    /**
     * Rook's callout (design/ui/hangar, Escort): {@code ESCORT} and his side, his icon and name, his
     * fitted gun with its level pips, his armour or {@code GROUNDED} in red.
     */
    private void drawEscort(SpriteBatch batch, Hangar hangar, boolean selected) {
        Campaign campaign = hangar.campaign();
        Color edge = selected ? Glass.AMBER : LINE;
        int x = ESCORT_X;
        int y = ESCORT_Y;
        glass.callout(batch, x, y, ESCORT_CALLOUT_WIDTH, ESCORT_HEIGHT, selected);
        glass.shadowed(batch, glass.fonts.label, Names.slot(LoadoutSlot.ESCORT), edge, x + ESCORT_INSET, y + 6);
        glass.right(
                batch, glass.fonts.label, side(campaign), Glass.CYAN, x + ESCORT_CALLOUT_WIDTH - ESCORT_INSET, y + 6);
        TextureRegion rook = icons.escort().large();
        batch.draw(rook, x + ESCORT_INSET, PixelScreen.HEIGHT - y - 18 - rook.getRegionHeight());
        glass.shadowed(batch, glass.fonts.label, ROOK, Glass.WHITE, x + NAME_INDENT, y + ESCORT_NAME_ROW);
        Optional<Item> gun = hangar.fitted(LoadoutSlot.ESCORT);
        glass.shadowed(
                batch,
                glass.fonts.label,
                gun.map(i -> Names.callout(i.name())).orElse("-"),
                Glass.WHITE,
                x + ESCORT_INSET,
                y + ESCORT_GUN_ROW,
                ESCORT_CALLOUT_WIDTH - 2 * ESCORT_INSET);
        Fitted fitted = campaign.loadout().get(LoadoutSlot.ESCORT);
        int level = fitted == null ? 0 : fitted.level();
        for (int i = 0; i < PIPS; i++) {
            glass.bar(
                    batch, i < level ? Glass.AMBER : Glass.UNLIT, x + ESCORT_INSET + i * 9, y + ESCORT_PIPS_ROW, 7, 7);
        }
        boolean grounded = campaign.escortGrounded();
        glass.shadowed(
                batch,
                glass.fonts.label,
                grounded ? GROUNDED : armour(campaign),
                grounded ? Glass.ALERT : campaign.escortArmourLow() ? Glass.AMBER : Glass.CYAN,
                x + ESCORT_INSET,
                y + ESCORT_ARMOUR_ROW);
    }

    /** Rook's side on the callout: {@code L} or {@code R}. */
    static String side(Campaign campaign) {
        return campaign.gear().escort().side().name().substring(0, 1);
    }

    /** Rook's armour on the callout: {@code 34/80}. */
    static String armour(Campaign campaign) {
        return (int) Math.ceil(campaign.gear().escort().armour()) + "/" + (int) campaign.escortMaxArmour();
    }

    private void drawTiles(SpriteBatch batch, Hangar hangar, LoadoutSlot selected) {
        Campaign campaign = hangar.campaign();
        int x = X + (WIDTH - TILES.size() * TILE_WIDTH - (TILES.size() - 1) * TILE_GAP) / 2;
        for (LoadoutSlot slot : TILES) {
            boolean on = slot == selected;
            glass.button(batch, x, TILES_Y, TILE_WIDTH, 48, on, true);
            glass.centred(
                    batch,
                    glass.fonts.body,
                    Names.code(slot),
                    on ? Glass.AMBER : Glass.WHITE,
                    x + TILE_WIDTH / 2f,
                    TILES_Y + 4);
            Optional<Item> item = hangar.fitted(slot);
            String model = item.map(i -> Names.tile(i.name())).orElse("-");
            glass.centred(batch, glass.fonts.label, model, Glass.BODY, x + TILE_WIDTH / 2f, TILES_Y + 24);
            glass.centred(
                    batch,
                    glass.fonts.label,
                    value(campaign, slot, item),
                    Glass.CYAN,
                    x + TILE_WIDTH / 2f,
                    TILES_Y + 35);
            x += TILE_WIDTH + TILE_GAP;
        }
    }

    private static String value(Campaign campaign, LoadoutSlot slot, Optional<Item> item) {
        if (item.isEmpty()) {
            return "";
        }
        Fitted fitted = campaign.loadout().get(slot);
        return switch (slot) {
            case GENERATOR -> Names.number(item.get().stats(1).get(Stat.OUTPUT)) + "MW";
            case SHIELD -> Names.number(item.get().stats(1).get(Stat.CAPACITY));
            case ARMOUR -> (int) Math.ceil(campaign.armour()) + "/" + (int) campaign.maxArmour();
            case ENGINE -> Names.number(item.get().stats(1).get(Stat.SPEED));
            case SPECIAL -> "X" + campaign.gear().charges(fitted.item());
            default -> "L" + fitted.level();
        };
    }

    private void drawPower(SpriteBatch batch, Hangar hangar, Optional<Choice> choice) {
        glass.panel(batch, X, POWER_Y, WIDTH, 52, 0.86f);
        double load = hangar.load();
        double output = hangar.output();
        StringBuilder text = new StringBuilder("POWER " + Names.number(load) + " / " + Names.megawatts(output));
        Optional<Choice> projected =
                choice.filter(c -> Math.abs(c.load() - load) > 1e-9 || Math.abs(c.output() - output) > 1e-9);
        projected.ifPresent(c -> text.append("   AFTER ")
                .append(Names.number(c.load()))
                .append(" / ")
                .append(Names.megawatts(c.output())));
        boolean over = projected.map(c -> c.load() > c.output() + 1e-9).orElse(false);
        glass.shadowed(
                batch, glass.fonts.label, text.toString(), over ? Glass.ALERT : Glass.AMBER, X + 10, POWER_Y + 8);
        int barX = X + 10;
        int barWidth = WIDTH - 20;
        int barY = POWER_Y + 26;
        double scale = Math.max(output, projected.map(Choice::output).orElse(output));
        double top = Math.max(scale, projected.map(Choice::load).orElse(load));
        glass.inset(batch, barX - 2, barY - 2, barWidth + 4, 20);
        glass.bar(batch, Glass.AMBER, barX, barY, (float) (barWidth * Math.min(load, top) / top), 16);
        projected
                .filter(c -> c.load() > load)
                .ifPresent(c -> glass.bar(
                        batch,
                        over ? Glass.ALERT : PROJECTED,
                        (float) (barX + barWidth * load / top),
                        barY,
                        (float) (barWidth * (c.load() - load) / top),
                        16));
        for (int mw = 1; mw < top; mw++) {
            glass.fill(batch, TICK, (float) (barX + barWidth * mw / top), barY, 1, 16);
        }
        glass.fill(batch, Glass.WHITE, (float) (barX + barWidth * output / top) - 1, barY - 3, 2, 22);
    }
}
