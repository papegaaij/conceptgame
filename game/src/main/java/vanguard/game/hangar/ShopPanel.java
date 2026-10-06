package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.campaign.Catalogue.Item;
import vanguard.content.campaign.Catalogue.Stat;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Flight;
import vanguard.content.campaign.Hangar.Choice;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.Refusal;
import vanguard.content.campaign.Hangar.State;
import vanguard.content.campaign.ItemKind;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Words;
import vanguard.sim.WingmanSpec;

/**
 * The hangar's shop drawer (left, design/ui/hangar): the rows for the selected slot (fitted, owned,
 * buyable with price and ◆ trait matches and the NEW tag, locked with their unlock level), each
 * with its icon on a glass row band, the scroll markers, the selected item with its large icon,
 * traits, numbers and the deltas against the fitted item, its choices, the last transaction's
 * message; the test-fire box below them is the {@link TestFirePanel}'s. The escort's shop (M5 part
 * A) lists Rook's guns and, last, the row of his side with its {@code LEFT} / {@code RIGHT} choice.
 */
final class ShopPanel {
    static final int X = 16;
    static final int Y = 52;
    static final int WIDTH = 284;
    static final int HEIGHT = 444;
    /** The last transaction's message: two lines from here, 12 px apart, above the test-fire box. */
    static final int MESSAGE_Y = Y + HEIGHT - 100;

    private static final int INNER = X + 10;
    private static final int RIGHT = X + WIDTH - 10;
    private static final int ROWS_Y = Y + 26;
    private static final int ROW = 22;
    /** A row's name starts right of its icon. */
    private static final int NAME_X = INNER + 20;

    private static final Color NEW_TAG = new Color(1, 0, 0.67f, 1);
    /** A locked item's icon is drawn this much darker. */
    private static final Color LOCKED_ICON = new Color(0.45f, 0.45f, 0.55f, 1);

    private final Glass glass;
    private final ItemIcons icons;
    private final Content content;

    ShopPanel(Glass glass, ItemIcons icons, Content content) {
        this.content = content;
        this.glass = glass;
        this.icons = icons;
    }

    void draw(SpriteBatch batch, HangarState state, List<String> markedTraits) {
        glass.panel(batch, X, Y, WIDTH, HEIGHT, 0.86f);
        glass.header(batch, "SHOP - " + Names.slot(state.slot()), INNER, RIGHT, Y + 10);
        List<Offer> rows = state.rows();
        int count = state.rowCount();
        boolean shop = state.focus() == HangarState.Focus.SHOP;
        for (int i = state.top(); i < Math.min(count, state.top() + HangarState.VISIBLE_ROWS); i++) {
            int y = ROWS_Y + (i - state.top()) * ROW;
            if (i < rows.size()) {
                drawRow(batch, rows.get(i), y, shop && i == state.row(), markedTraits);
            } else {
                drawSideRow(batch, state, y, shop && i == state.row());
            }
        }
        if (count > HangarState.VISIBLE_ROWS) {
            String more =
                    (state.top() + 1) + "-" + Math.min(count, state.top() + HangarState.VISIBLE_ROWS) + " OF " + count;
            int moreY = ROWS_Y + HangarState.VISIBLE_ROWS * ROW;
            glass.right(batch, glass.fonts.label, more, Glass.DIM, RIGHT, moreY);
            boolean below = state.top() + HangarState.VISIBLE_ROWS < count;
            glass.scrollMarkers(
                    batch, RIGHT - Fonts.width(glass.fonts.label, more) - 6, moreY - 2, state.top() > 0, below);
        }
        int detail = ROWS_Y + HangarState.VISIBLE_ROWS * ROW + 16;
        glass.header(batch, "SELECTED", INNER, RIGHT, detail);
        state.selected().ifPresent(offer -> drawDetail(batch, state, offer, detail + 16));
        if (state.sideSelected()) {
            drawSide(batch, state, detail + 16);
        }
        Optional<Choice> refused = state.focus() == HangarState.Focus.SHOP
                ? state.choice().filter(choice -> !choice.allowed())
                : Optional.empty();
        String message = refused.map(HangarState::refusal).orElse(state.message());
        List<String> lines = Words.wrap(message, (WIDTH - 20) / Fonts.advance(glass.fonts.label));
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            Color colour = refused.isPresent() ? Glass.ALERT : Glass.CYAN;
            glass.shadowed(batch, glass.fonts.label, lines.get(i), colour, INNER, MESSAGE_Y + i * 12);
        }
    }

    private void drawRow(SpriteBatch batch, Offer offer, int y, boolean selected, List<String> markedTraits) {
        boolean locked = offer.state() == State.LOCKED;
        glass.row(batch, X + 4, y - 5, WIDTH - 8, ROW - 2);
        if (selected) {
            glass.selection(batch, X + 2, y - 5, WIDTH - 4, ROW - 2);
            glass.cursor(batch, glass.fonts.label, X + 4, y + 4);
        }
        drawIcon(batch, icons.of(offer.item()).small(), INNER - 2, y - 3, locked);
        String status = status(offer);
        glass.right(batch, glass.fonts.label, status, locked ? Glass.DIM : Glass.CYAN, RIGHT, y);
        float x = RIGHT - Fonts.width(glass.fonts.label, status) - 6;
        if (offer.isNew()) {
            x -= 30;
            glass.tag(batch, "NEW", NEW_TAG, x, y - 2, 28, 14);
        }
        long matches = 0;
        for (String trait : offer.item().traits()) {
            if (markedTraits.contains(trait)) {
                matches++;
            }
        }
        for (int i = 1; i <= matches; i++) {
            glass.diamond(batch, x - i * 10, y + 1);
        }
        Color colour = selected ? Glass.AMBER : locked ? Glass.DIM : Glass.WHITE;
        glass.shadowed(
                batch,
                glass.fonts.label,
                Names.of(offer.item().name()),
                colour,
                NAME_X,
                y,
                x - matches * 10 - NAME_X - 4);
    }

    /** The escort's last row: Rook's side, {@code ROOK'S SIDE ... LEFT}. */
    private void drawSideRow(SpriteBatch batch, HangarState state, int y, boolean selected) {
        glass.row(batch, X + 4, y - 5, WIDTH - 8, ROW - 2);
        if (selected) {
            glass.selection(batch, X + 2, y - 5, WIDTH - 4, ROW - 2);
            glass.cursor(batch, glass.fonts.label, X + 4, y + 4);
        }
        drawIcon(batch, icons.escort().small(), INNER - 2, y - 3, false);
        glass.right(batch, glass.fonts.label, state.escortSide().name(), Glass.CYAN, RIGHT, y);
        glass.shadowed(batch, glass.fonts.label, SIDE, selected ? Glass.AMBER : Glass.WHITE, NAME_X, y);
    }

    /** The side row's detail: where Rook flies, and the {@code LEFT} / {@code RIGHT} choice. */
    private void drawSide(SpriteBatch batch, HangarState state, int y) {
        glass.shadowed(batch, glass.fonts.body, SIDE, Glass.AMBER, INNER, y);
        glass.shadowed(batch, glass.fonts.label, "WHERE ROOK FLIES BESIDE YOU.", Glass.WHITE, INNER, y + 24);
        glass.shadowed(batch, glass.fonts.label, "FREE: LEFT / RIGHT SET IT.", Glass.LABEL, INNER, y + 38);
        boolean shop = state.focus() == HangarState.Focus.SHOP;
        float x = INNER;
        for (WingmanSpec.Side side : WingmanSpec.Side.values()) {
            String text = side.name();
            float width = Fonts.width(glass.fonts.label, text) + 14;
            boolean on = state.escortSide() == side;
            glass.chip(batch, glass.fonts.label, text, x, y + 64, width, 20, on && shop, true);
            if (on && !shop) {
                glass.outline(batch, Glass.AMBER, x, y + 64, width, 20);
            }
            x += width + 6;
        }
    }

    /** The side row's name. */
    static final String SIDE = "ROOK'S SIDE";

    /** An item's icon with its top-left corner at (x, y), darkened while it is locked. */
    private static void drawIcon(SpriteBatch batch, TextureRegion icon, float x, float y, boolean locked) {
        if (locked) {
            batch.setColor(LOCKED_ICON);
        }
        batch.draw(icon, x, PixelScreen.HEIGHT - y - icon.getRegionHeight());
        batch.setColor(Color.WHITE);
    }

    private static String status(Offer offer) {
        String level = offer.item().maxLevel() > 1 ? "L" + offer.level() + " " : "";
        return switch (offer.state()) {
            case FITTED -> offer.item().kind() == ItemKind.SPECIAL ? "FITTED" : level + "FITTED";
            case OWNED -> level + "OWNED";
            case BUYABLE -> Names.credits(offer.item().price());
            case LOCKED ->
                String.format(Locale.ROOT, "LOCKED L%02d", offer.item().unlock());
        };
    }

    private void drawDetail(SpriteBatch batch, HangarState state, Offer offer, int y) {
        Item item = offer.item();
        String level = item.maxLevel() > 1 ? "  L" + offer.level() : "";
        TextureRegion icon = icons.of(item).large();
        drawIcon(batch, icon, RIGHT - icon.getRegionWidth(), y - 6, offer.state() == State.LOCKED);
        glass.shadowed(
                batch, glass.fonts.body, Names.of(item.name()) + level, Glass.AMBER, INNER, y, RIGHT - 28 - INNER);
        int traitX = INNER;
        for (String trait : item.traits()) {
            String text = "<" + trait.toUpperCase(Locale.ROOT) + ">";
            glass.shadowed(batch, glass.fonts.label, text, Glass.AMBER, traitX, y + 24);
            traitX += Fonts.width(glass.fonts.label, text) + 8;
        }
        int line = y + (item.traits().isEmpty() ? 24 : 40);
        Map<Stat, Double> stats = item.stats(offer.level());
        Optional<Item> fitted = state.hangar().fitted(state.slot());
        StringBuilder numbers = new StringBuilder();
        stats.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(stat -> numbers.append(
                        label(stat.getKey()))
                .append(' ')
                .append(Names.number(stat.getValue()))
                .append("   "));
        numbers.append("DRAW ").append(Names.megawatts(item.draw(offer.level())));
        glass.shadowed(batch, glass.fonts.label, numbers.toString(), Glass.WHITE, INNER, line);
        if (offer.state() != State.FITTED && fitted.isPresent()) {
            drawDeltas(batch, item, offer.level(), fitted.get(), fittedLevel(state), line + 14);
        }
        if (!flies(state.slot(), item)) {
            glass.shadowed(batch, glass.fonts.label, "NOT YET IN FLIGHT", Glass.DIM, INNER, line + 28);
        }
        if (offer.state() == State.LOCKED) {
            String unlock = String.format(Locale.ROOT, "IN THE SHOP BEFORE MISSION %02d", item.unlock());
            glass.shadowed(batch, glass.fonts.label, unlock, Glass.LABEL, INNER, line + 42);
            return;
        }
        drawChoices(batch, state, offer, line + 44);
    }

    private static int fittedLevel(HangarState state) {
        Fitted fitted = state.hangar().campaign().loadout().get(state.slot());
        return fitted == null ? 1 : fitted.level();
    }

    private boolean flies(LoadoutSlot slot, Item item) {
        return Flight.flies(content, slot, new Fitted(item.id(), 1));
    }

    private void drawDeltas(SpriteBatch batch, Item item, int level, Item fitted, int fittedLevel, int y) {
        float x = INNER;
        glass.shadowed(batch, glass.fonts.label, "VS FITTED:", Glass.LABEL, x, y);
        x += Fonts.width(glass.fonts.label, "VS FITTED:") + 8;
        Map<Stat, Double> mine = item.stats(level);
        Map<Stat, Double> theirs = fitted.stats(fittedLevel);
        for (Stat stat : Stat.values()) {
            if (mine.containsKey(stat) && theirs.containsKey(stat)) {
                x = delta(batch, label(stat), mine.get(stat) - theirs.get(stat), stat.higherIsBetter(), x, y);
            }
        }
        delta(batch, "DRAW", item.draw(level) - fitted.draw(fittedLevel), false, x, y);
    }

    private float delta(SpriteBatch batch, String label, double change, boolean higherIsBetter, float x, int y) {
        if (Math.abs(change) < 1e-9) {
            return x;
        }
        String text = label + " " + (change > 0 ? "+" : "") + Names.number(change);
        Color colour = (change > 0) == higherIsBetter ? Glass.GREEN : Glass.ALERT;
        glass.shadowed(batch, glass.fonts.label, text, colour, x, y);
        return x + Fonts.width(glass.fonts.label, text) + 10;
    }

    private void drawChoices(SpriteBatch batch, HangarState state, Offer offer, int y) {
        Optional<Choice> selected = state.focus() == HangarState.Focus.SHOP ? state.choice() : Optional.empty();
        float x = INNER;
        for (Choice choice : offer.choices()) {
            String text = choiceLabel(choice);
            float width = Fonts.width(glass.fonts.label, text) + 14;
            boolean on = selected.filter(c -> c.action() == choice.action()).isPresent();
            glass.chip(batch, glass.fonts.label, text, x, y, width, 20, on && choice.allowed(), choice.allowed());
            if (on && !choice.allowed()) {
                glass.outline(batch, Glass.AMBER, x, y, width, 20);
            }
            x += width + 6;
        }
    }

    /** A choice's chip: the action and its credits, without "CR" so three chips fit the drawer. */
    static String choiceLabel(Choice choice) {
        String credits = Names.grouped(Math.abs(choice.credits()));
        return switch (choice.action()) {
            case BUY -> "BUY " + credits;
            case UPGRADE ->
                choice.refusal().filter(Refusal.MAX_LEVEL::equals).isPresent() ? "MAX LEVEL" : "UPGRADE " + credits;
            case FIT -> "FIT";
            case UNFIT -> "UNFIT";
            case SELL -> "SELL " + credits;
            case BUY_CHARGE -> "CHARGE " + credits;
        };
    }

    static String label(Stat stat) {
        return switch (stat) {
            case DPS -> "DPS";
            case OUTPUT -> "OUTPUT";
            case CAPACITY -> "CAP";
            case REGEN -> "REGEN";
            case DELAY -> "DELAY";
            case ARMOUR -> "ARMOUR";
            case SPEED -> "SPEED";
            case CHARGES -> "MAX";
        };
    }
}
