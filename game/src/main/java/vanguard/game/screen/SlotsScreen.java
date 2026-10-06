package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.ActData;
import vanguard.content.Content;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveGame;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.hangar.Names;
import vanguard.game.input.MenuInput;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;

/**
 * The slot list (design/systems/saves, chosen load-game-r06-a): the autosave and 8 manual slots
 * over the hero scene, each with its act icon, next mission, difficulty, credits, playtime and
 * date, and a preview of the selected save. Loading (from the main menu) opens the hangar with the
 * save; saving (from the hangar) writes into a manual slot, after a confirmation when it holds a
 * save; in a debug run it writes nothing and says that saving is off. Back returns to the screen
 * below.
 */
public final class SlotsScreen implements GameScreen {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());
    private static final int LIST_X = 40;
    private static final int LIST_WIDTH = 572;
    private static final int LIST_Y = 102;
    private static final int ROW = 44;
    private static final int PREVIEW_X = 632;
    private static final int PREVIEW_Y = 150;
    private static final int PREVIEW_WIDTH = 300;
    private static final int PREVIEW_HEIGHT = 300;
    /** Saving in a debug run, which writes no save (design/systems/saves). */
    private static final String SAVING_OFF = "SAVING OFF IN A DEBUG RUN";

    private final GameServices services;
    /** The campaign to save; empty when loading. */
    private final Optional<Campaign> campaign;

    private List<SaveSlots.Entry> entries;
    private int selected;
    private Optional<Dialog> overwrite = Optional.empty();
    private String message = "";

    private SlotsScreen(GameServices services, Optional<Campaign> campaign) {
        this.services = services;
        this.campaign = campaign;
        entries = services.saves.list();
        selected = campaign.isPresent() ? 1 : firstSaved();
        message = idleMessage();
    }

    /** The message while nothing was tried: in a debug run's save screen that saving is off. */
    private String idleMessage() {
        return campaign.isPresent() && services.debugRun() ? SAVING_OFF : "";
    }

    /** Load game, from the main menu. */
    public static SlotsScreen load(GameServices services) {
        return new SlotsScreen(services, Optional.empty());
    }

    /** Save game, from the hangar: the manual slots only. */
    public static SlotsScreen save(GameServices services, Campaign campaign) {
        return new SlotsScreen(services, Optional.of(campaign));
    }

    private int firstSaved() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof SaveSlots.Entry.Saved) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public Transition update(float seconds) {
        campaign.ifPresent(c -> c.play(seconds));
        MenuInput input = services.menu;
        if (overwrite.isPresent()) {
            switch (overwrite.get().update(input)) {
                case YES -> {
                    overwrite = Optional.empty();
                    write();
                }
                case NO -> {
                    services.play(Sfx.MENU_BACK);
                    overwrite = Optional.empty();
                }
                case NONE -> {}
            }
            return Transition.STAY;
        }
        if (input.back()) {
            services.play(Sfx.MENU_BACK);
            return Transition.BACK;
        }
        int first = campaign.isPresent() ? 1 : 0;
        int before = selected;
        if (input.up()) {
            selected = selected == first ? entries.size() - 1 : selected - 1;
        } else if (input.down()) {
            selected = selected == entries.size() - 1 ? first : selected + 1;
        }
        if (selected != before) {
            services.play(Sfx.MENU_MOVE);
            message = idleMessage();
        }
        return input.confirm() ? confirm() : Transition.STAY;
    }

    private Transition confirm() {
        SaveSlots.Entry entry = entries.get(selected);
        if (campaign.isPresent()) {
            if (services.debugRun()) {
                services.play(Sfx.MENU_BACK);
            } else if (entry instanceof SaveSlots.Entry.Empty) {
                write();
            } else {
                services.play(Sfx.MENU_CONFIRM);
                overwrite = Optional.of(new Dialog(
                        "OVERWRITE SLOT " + entry.slot().index() + "?",
                        "THE SAVE IN IT IS REPLACED.",
                        "YES, OVERWRITE",
                        "NO, BACK"));
            }
            return Transition.STAY;
        }
        if (entry instanceof SaveSlots.Entry.Saved saved) {
            services.play(Sfx.MENU_CONFIRM);
            return Transition.replace(
                    new HangarScreen(services, Campaign.load(services.campaignRules, saved.save()), false));
        }
        services.play(Sfx.MENU_BACK);
        return Transition.STAY;
    }

    /** Writes the slot: the save-done sound when it is written, the back blip when it failed. */
    private void write() {
        SaveSlots.Slot slot = entries.get(selected).slot();
        boolean written = services.save(slot, campaign.orElseThrow());
        services.play(written ? Sfx.SAVE_DONE : Sfx.MENU_BACK);
        message = written ? "SAVED TO SLOT " + slot.index() : "SAVE FAILED - SEE THE LOG";
        entries = services.saves.list();
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.55f);
        glass.shadowed(
                batch, glass.fonts.heading, campaign.isPresent() ? "SAVE GAME" : "LOAD GAME", Glass.WHITE, LIST_X, 64);
        glass.panel(batch, LIST_X, LIST_Y - 6, LIST_WIDTH, entries.size() * ROW + 6, 0.7f);
        for (int i = 0; i < entries.size(); i++) {
            drawRow(batch, glass, entries.get(i), LIST_Y + i * ROW, i == selected);
        }
        drawPreview(batch, glass, entries.get(selected));
        String action = campaign.isPresent() ? "ENTER SAVE" : "ENTER LOAD";
        glass.hints(batch, "UP/DOWN SELECT    " + action + "    ESC BACK");
        overwrite.ifPresent(dialog -> glass.dialog(batch, dialog));
    }

    private void drawRow(SpriteBatch batch, Glass glass, SaveSlots.Entry entry, int y, boolean on) {
        boolean disabled = campaign.isPresent() && entry.slot().autosave();
        if (on) {
            glass.selection(batch, LIST_X + 2, y - 2, LIST_WIDTH - 4, ROW - 4);
        }
        String label =
                entry.slot().autosave() ? "AUTOSAVE" : "SLOT " + entry.slot().index();
        glass.shadowed(
                batch, glass.fonts.label, label, on ? Glass.AMBER : disabled ? Glass.DIM : Glass.LABEL, LIST_X + 12, y);
        int lineY = y + 14;
        switch (entry) {
            case SaveSlots.Entry.Empty empty ->
                glass.shadowed(batch, glass.fonts.body, "- EMPTY -", Glass.DIM, LIST_X + 50, lineY);
            case SaveSlots.Entry.Unreadable unreadable -> {
                glass.shadowed(batch, glass.fonts.body, "UNREADABLE", Glass.ALERT, LIST_X + 50, lineY);
                String reason =
                        unreadable.reason() == null ? "" : unreadable.reason().toUpperCase(Locale.ROOT);
                glass.shadowed(
                        batch,
                        glass.fonts.label,
                        reason.substring(0, Math.min(reason.length(), 40)),
                        Glass.DIM,
                        LIST_X + 180,
                        lineY + 4);
            }
            case SaveSlots.Entry.Saved saved -> {
                SaveGame save = saved.save();
                int act = actNumber(save.nextLevel());
                Color box = act == 1 ? Glass.CYAN : Glass.GREEN;
                glass.outline(batch, box, LIST_X + 12, lineY - 2, 18, 18);
                glass.centred(batch, glass.fonts.body, act > 0 ? Integer.toString(act) : "?", box, LIST_X + 21, lineY);
                glass.shadowed(
                        batch,
                        glass.fonts.body,
                        mission(save.nextLevel()),
                        on ? Glass.AMBER : Glass.WHITE,
                        LIST_X + 40,
                        lineY);
                String facts = save.difficulty().name() + "   CR " + Names.grouped(save.credits()) + "   "
                        + playtime(save.playtime());
                glass.right(batch, glass.fonts.label, facts, Glass.GREEN, LIST_X + LIST_WIDTH - 12, y);
                glass.right(
                        batch,
                        glass.fonts.label,
                        DATE.format(save.created()),
                        Glass.LABEL,
                        LIST_X + LIST_WIDTH - 12,
                        y + 16);
            }
        }
    }

    private void drawPreview(SpriteBatch batch, Glass glass, SaveSlots.Entry entry) {
        glass.panel(batch, PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT, 0.8f);
        int x = PREVIEW_X + 14;
        int right = PREVIEW_X + PREVIEW_WIDTH - 14;
        if (!message.isEmpty()) {
            glass.shadowed(
                    batch,
                    glass.fonts.label,
                    message,
                    message.startsWith("SAVE FAILED") || message.equals(SAVING_OFF) ? Glass.ALERT : Glass.GREEN,
                    x,
                    PREVIEW_Y + PREVIEW_HEIGHT - 22);
        }
        if (!(entry instanceof SaveSlots.Entry.Saved saved)) {
            glass.shadowed(
                    batch,
                    glass.fonts.body,
                    entry.slot().autosave() && campaign.isPresent() ? "WRITTEN BY THE HANGAR" : "NO SAVE",
                    Glass.DIM,
                    x,
                    PREVIEW_Y + 16);
            return;
        }
        SaveGame save = saved.save();
        Optional<ActData.TitleCard> card = services.content
                .actOf(save.nextLevel())
                .map(act -> act.getValue().titleCard());
        glass.shadowed(
                batch,
                glass.fonts.body,
                card.map(c -> c.act() + " - " + c.name()).orElse(""),
                Glass.AMBER,
                x,
                PREVIEW_Y + 16);
        glass.shadowed(batch, glass.fonts.label, "NEXT: " + mission(save.nextLevel()), Glass.WHITE, x, PREVIEW_Y + 40);
        String[][] facts = {
            {"DIFFICULTY", save.difficulty().name()},
            {"CREDITS", "CR " + Names.grouped(save.credits())},
            {"SCORE", Names.grouped(save.score())},
            {"ARMOUR", (int) Math.ceil(save.armour()) + ""},
            {"PLAYTIME", playtime(save.playtime())},
            {"SAVED", DATE.format(save.created())}
        };
        int y = PREVIEW_Y + 64;
        for (String[] fact : facts) {
            glass.shadowed(batch, glass.fonts.label, fact[0], Glass.LABEL, x, y);
            glass.shadowed(batch, glass.fonts.label, fact[1], Glass.GREEN, x + 96, y);
            y += 16;
        }
        y += 8;
        glass.header(batch, "LOADOUT", x, right, y);
        Map<LoadoutSlot, Fitted> loadout = save.loadout();
        Fitted front = loadout.get(LoadoutSlot.FRONT);
        if (front != null) {
            glass.shadowed(
                    batch,
                    glass.fonts.label,
                    front.item().replace('-', ' ').toUpperCase(Locale.ROOT) + " L" + front.level(),
                    Glass.CYAN,
                    x,
                    y + 16);
        }
    }

    /** "M01 BREAK AT DAWN", or "M02" while the level is not built. */
    private String mission(int level) {
        String number = String.format(Locale.ROOT, "M%02d", level);
        return services.content
                .levelKey(level)
                .map(key -> number + " " + Content.levelName(key).toUpperCase(Locale.ROOT))
                .orElse(number);
    }

    private int actNumber(int level) {
        return services.content
                .actOf(level)
                .map(act -> Integer.parseInt(act.getKey().split("-")[1]))
                .orElse(0);
    }

    /** Playtime as h:mm:ss. */
    static String playtime(double seconds) {
        long total = (long) seconds;
        return String.format(Locale.ROOT, "%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60);
    }

    @Override
    public void dispose() {}
}
