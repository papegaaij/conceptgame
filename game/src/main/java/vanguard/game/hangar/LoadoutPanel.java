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
 * callout box per weapon mount (level pips and the fitted item) and the escort slot (locked until
 * Act 2), the module tiles of the core parts, the special and the two utility bays, and the power
 * bar with the load, the load the selected choice would make in a lighter colour (red when it
 * would not fit) and the generator's output.
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
    private static final Color EMPTY = new Color(0.10f, 0.12f, 0.22f, 1);
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
    private static final int TILE_WIDTH = 48;
    private static final int TILE_GAP = 2;

    private static final int WING_WIDTH = 132;

    private record Callout(LoadoutSlot slot, int x, int y, int width) {}

    private static final List<Callout> CALLOUTS = List.of(
            new Callout(LoadoutSlot.FRONT, CENTRE_X - 70, Y + 26, 140),
            new Callout(LoadoutSlot.LEFT_WING, X + 8, Y + 120, WING_WIDTH),
            new Callout(LoadoutSlot.RIGHT_WING, X + WIDTH - 8 - WING_WIDTH, Y + 120, WING_WIDTH),
            new Callout(LoadoutSlot.REAR, CENTRE_X - 70, Y + 274, 140));

    private final Glass glass;
    private final TextureRegion ship;

    LoadoutPanel(Glass glass, TextureRegion ship) {
        this.glass = glass;
        this.ship = ship;
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
        glass.fill(batch, LINE, CENTRE_X, Y + 66, 1, SHIP_Y - shipHeight / 2 - Y - 66);
        glass.fill(batch, LINE, CENTRE_X, SHIP_Y + shipHeight / 2, 1, Y + 274 - SHIP_Y - shipHeight / 2);
        int wingLine = X + 8 + WING_WIDTH;
        glass.fill(batch, LINE, wingLine, Y + 140, CENTRE_X - 12 - wingLine, 1);
        glass.fill(batch, LINE, CENTRE_X + 12, Y + 140, X + WIDTH - 8 - WING_WIDTH - CENTRE_X - 12, 1);
        batch.draw(
                ship, CENTRE_X - shipWidth / 2f, PixelScreen.HEIGHT - SHIP_Y - shipHeight / 2f, shipWidth, shipHeight);
        for (Callout callout : CALLOUTS) {
            drawCallout(batch, hangar, callout, state.slot() == callout.slot());
        }
        int escortY = Y + 270;
        glass.fill(batch, BLUEPRINT, X + 12, escortY, 96, 48);
        glass.outline(batch, Glass.DIM, X + 12, escortY, 96, 48);
        glass.shadowed(batch, glass.fonts.label, "ESCORT", Glass.DIM, X + 18, escortY + 8);
        glass.shadowed(batch, glass.fonts.label, "ACT 2", Glass.DIM, X + 18, escortY + 26);
        drawTiles(batch, hangar, state.slot());
        drawPower(batch, hangar, state.choice());
    }

    private void drawCallout(SpriteBatch batch, Hangar hangar, Callout callout, boolean selected) {
        Color edge = selected ? Glass.AMBER : LINE;
        glass.fill(
                batch,
                selected ? new Color(0.25f, 0.22f, 0, 0.95f) : BLUEPRINT,
                callout.x(),
                callout.y(),
                callout.width(),
                40);
        glass.outline(batch, edge, callout.x(), callout.y(), callout.width(), 40);
        glass.shadowed(batch, glass.fonts.label, Names.slot(callout.slot()), edge, callout.x() + 6, callout.y() + 6);
        Optional<Fitted> fitted =
                Optional.ofNullable(hangar.campaign().loadout().get(callout.slot()));
        String name = fitted.flatMap(f -> hangar.fitted(callout.slot()))
                .map(item -> Names.tile(item.name()))
                .orElse("EMPTY");
        glass.shadowed(
                batch,
                glass.fonts.label,
                name,
                fitted.isPresent() ? Glass.WHITE : Glass.DIM,
                callout.x() + 6,
                callout.y() + 22);
        int level = fitted.map(Fitted::level).orElse(0);
        for (int i = 0; i < PIPS; i++) {
            glass.fill(
                    batch,
                    i < level ? Glass.AMBER : EMPTY,
                    callout.x() + callout.width() - 6 - (PIPS - i) * 9,
                    callout.y() + 6,
                    7,
                    7);
        }
    }

    private void drawTiles(SpriteBatch batch, Hangar hangar, LoadoutSlot selected) {
        Campaign campaign = hangar.campaign();
        int x = X + (WIDTH - TILES.size() * TILE_WIDTH - (TILES.size() - 1) * TILE_GAP) / 2;
        for (LoadoutSlot slot : TILES) {
            boolean on = slot == selected;
            glass.fill(
                    batch,
                    on ? new Color(0.25f, 0.22f, 0, 0.95f) : new Color(0.03f, 0.05f, 0.16f, 0.9f),
                    x,
                    TILES_Y,
                    TILE_WIDTH,
                    48);
            glass.outline(batch, on ? Glass.AMBER : Glass.TRIM, x, TILES_Y, TILE_WIDTH, 48);
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
        glass.fill(batch, EMPTY, barX, barY, barWidth, 16);
        glass.fill(batch, Glass.AMBER, barX, barY, (float) (barWidth * Math.min(load, top) / top), 16);
        projected
                .filter(c -> c.load() > load)
                .ifPresent(c -> glass.fill(
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
        glass.outline(batch, Glass.TRIM, barX, barY, barWidth, 16);
    }
}
