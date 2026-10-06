package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRoute;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Intel;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.hangar.HangarState;
import vanguard.game.hangar.HangarState.Command;
import vanguard.game.hangar.HangarState.Focus;
import vanguard.game.hangar.HangarState.Outcome;
import vanguard.game.hangar.HangarView;
import vanguard.game.hangar.Names;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;

/**
 * The hangar between levels (design/ui/hangar, chosen hangar-r07-b: layout B in the glass style
 * over the tactical map): the shop drawer, the ship's schematic with its module tiles and the power
 * bar, the intel on the next level, and the command bar with undo, repair, save and launch. Keys:
 * previous / next tab select the slot, up / down the shop row (and the command bar above it),
 * left / right the row's choice or the command, confirm makes it; selling, launching with a
 * warning and quitting ask first; the selected weapon's test fire loops in the shop's box. It
 * autosaves when it opens (design/systems/saves), except right after a save was loaded, and when
 * the player quits from it (never in a debug run, which says so instead), and plays the hangar theme.
 */
public final class HangarScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;
    /** The autosave line of a debug run, which writes no save. */
    private static final String DEBUG_RUN = "AUTOSAVE OFF - DEBUG RUN";

    private final GameServices services;
    private final Campaign campaign;
    private final Optional<String> nextLevel;
    private final HangarState state;
    private final HangarView view;
    private final Optional<MusicStreamer> music;
    private final String autosave;
    private Optional<Dialog> dialog = Optional.empty();
    private Optional<Pending> pending = Optional.empty();

    /** What a confirmation is for. */
    private enum Pending {
        SELL,
        LAUNCH,
        QUIT
    }

    /** @param autosave write the autosave as the hangar opens */
    public HangarScreen(GameServices services, Campaign campaign, boolean autosave) {
        this.services = services;
        this.campaign = campaign;
        nextLevel = CampaignRoute.launch(services.content, campaign);
        // A special's free charges at its unlock (the first Airstrike charge), before the autosave.
        List<Campaign.FreeCharges> given = campaign.giveFreeCharges(services.content.specials());
        state = new HangarState(new Hangar(services.catalogue, campaign), nextLevel.isPresent());
        for (Campaign.FreeCharges free : given) {
            state.notice(HangarState.freeChargesNotice(free));
        }
        view = new HangarView(services.files, services.glass, services.sprites, services.catalogue, services.content);
        if (autosave && services.debugRun()) {
            // A debug run writes no save (design/systems/saves).
            this.autosave = DEBUG_RUN;
        } else if (autosave) {
            boolean written = services.save(SaveSlots.Slot.AUTOSAVE, campaign);
            this.autosave = written ? "AUTOSAVED" : "AUTOSAVE FAILED";
            if (written) {
                services.play(Sfx.SAVE_DONE);
            }
        } else {
            this.autosave = "";
        }
        music = MusicStreamer.play(
                services.audio, services.files.internal("music/hangar-theme.ogg"), MUSIC_VOLUME, services.mixer);
    }

    /**
     * After a new game's difficulty select and after the act outro: the next level's briefing and
     * then the hangar, or the hangar while that level is not built.
     */
    public static GameScreen beforeNextLevel(GameServices services, Campaign campaign) {
        return screen(services, campaign, CampaignRoute.beforeNextLevel(services.content, campaign));
    }

    /**
     * After a won level's debrief: the act outro first when the level ended its act (design/campaign,
     * Act intro and outro), then as {@link #beforeNextLevel}.
     */
    public static GameScreen afterLevel(GameServices services, Campaign campaign) {
        return screen(services, campaign, CampaignRoute.afterLevel(services.content, campaign));
    }

    private static GameScreen screen(GameServices services, Campaign campaign, CampaignRoute.Step step) {
        return switch (step) {
            case CampaignRoute.Step.Briefing briefing ->
                new BriefingScreen(
                        services, campaign, briefing.script(), () -> new HangarScreen(services, campaign, true));
            case CampaignRoute.Step.Outro outro ->
                BriefingScreen.outro(services, campaign, outro.script(), () -> beforeNextLevel(services, campaign));
            case CampaignRoute.Step.Hangar hangar -> new HangarScreen(services, campaign, true);
        };
    }

    /** The next level's intel at the sensor level the loadout gives now. */
    private Optional<Intel> intel() {
        return nextLevel.map(
                key -> Intel.of(services.content, key, state.hangar().sensorLevel()));
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        MenuInput input = services.menu;
        Transition next = input(input);
        // The test fire follows what the input selected.
        view.update(seconds, state);
        return next;
    }

    private Transition input(MenuInput input) {
        if (dialog.isPresent()) {
            return answer(dialog.get().update(input));
        }
        if (input.back()) {
            if (state.back()) {
                services.play(Sfx.MENU_BACK);
            } else {
                ask(
                        Pending.QUIT,
                        new Dialog(
                                "QUIT TO MAIN MENU?",
                                services.debugRun()
                                        ? "A DEBUG RUN WRITES NO SAVE."
                                        : "THE AUTOSAVE KEEPS THIS HANGAR VISIT.",
                                "YES, QUIT",
                                "NO, BACK"));
            }
            return Transition.STAY;
        }
        Outcome outcome = Outcome.NONE;
        if (input.previousTab()) {
            outcome = state.previousSlot();
        } else if (input.nextTab()) {
            outcome = state.nextSlot();
        } else if (input.up()) {
            outcome = state.up();
        } else if (input.down()) {
            outcome = state.down();
        } else if (input.left()) {
            outcome = state.left();
        } else if (input.right()) {
            outcome = state.right();
        } else if (input.confirm()) {
            // What the confirmation makes, read before it changes the selection.
            Sfx done = doneSound(
                    state.focus() == Focus.SHOP ? state.choice().map(Hangar.Choice::action) : Optional.empty(),
                    state.focus(),
                    state.command());
            return react(state.confirm(), done);
        }
        return react(outcome, Sfx.MENU_CONFIRM);
    }

    /**
     * The sound of a confirmation that was made (design/audio/sfx, UI and radio): a shop action's
     * own ({@link Sfx#shop}), a paid repair's purchase, an undo's refund, else the menu's confirm.
     *
     * @param made the highlighted shop choice's action, when the shop had the focus
     * @param focus where the focus was
     * @param command the highlighted command
     */
    static Sfx doneSound(Optional<Hangar.Action> made, Focus focus, Command command) {
        if (made.isPresent()) {
            return Sfx.shop(made.get());
        }
        if (focus == Focus.REPAIR) {
            return Sfx.SHOP_BUY;
        }
        if (focus == Focus.COMMANDS && command == Command.UNDO) {
            return Sfx.SHOP_SELL;
        }
        return Sfx.MENU_CONFIRM;
    }

    private Transition react(Outcome outcome, Sfx done) {
        switch (outcome) {
            case NONE -> {}
            case MOVED -> services.play(Sfx.MENU_MOVE);
            case DONE -> services.play(done);
            case REFUSED -> services.play(Sfx.SHOP_DENIED);
            case SELL -> {
                services.play(Sfx.MENU_CONFIRM);
                var offer = state.selected().orElseThrow();
                int refund = -state.choice().orElseThrow().credits();
                ask(
                        Pending.SELL,
                        new Dialog(
                                "SELL " + Names.of(offer.item().name()) + " FOR " + Names.credits(refund) + "?",
                                offer.bought()
                                        ? "BOUGHT DURING THIS VISIT: THE FULL PRICE COMES BACK."
                                        : "UNDO RETURNS THIS VISIT'S TRANSACTIONS FOR 100 %.",
                                "YES, SELL",
                                "NO, KEEP"));
            }
            case SAVE -> {
                services.play(Sfx.MENU_CONFIRM);
                return Transition.open(SlotsScreen.save(services, campaign));
            }
            case LAUNCH -> {
                services.play(Sfx.MENU_CONFIRM);
                List<String> warnings = state.launchWarnings(intel());
                if (warnings.isEmpty()) {
                    return launch();
                }
                ask(
                        Pending.LAUNCH,
                        new Dialog(
                                warnings.getFirst() + ". LAUNCH ANYWAY?",
                                String.join(" - ", warnings.subList(1, warnings.size())),
                                "YES, LAUNCH",
                                "NO, BACK"));
            }
        }
        return Transition.STAY;
    }

    private void ask(Pending what, Dialog question) {
        pending = Optional.of(what);
        dialog = Optional.of(question);
    }

    private Transition answer(Dialog.Answer answer) {
        if (answer == Dialog.Answer.NONE) {
            return Transition.STAY;
        }
        Pending what = pending.orElseThrow();
        dialog = Optional.empty();
        pending = Optional.empty();
        if (answer == Dialog.Answer.NO) {
            services.play(Sfx.MENU_BACK);
            return Transition.STAY;
        }
        services.play(what == Pending.SELL ? Sfx.SHOP_SELL : Sfx.MENU_CONFIRM);
        return switch (what) {
            case SELL -> {
                state.sell();
                yield Transition.STAY;
            }
            case LAUNCH -> launch();
            case QUIT -> {
                services.save(SaveSlots.Slot.AUTOSAVE, campaign);
                yield Transition.replace(MainMenuScreen.menu(services));
            }
        };
    }

    private Transition launch() {
        return Transition.replace(new LevelScreen(services, campaign, nextLevel.orElseThrow()));
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        Optional<Intel> intel = intel();
        Optional<BriefingPage> teaser =
                nextLevel.map(key -> services.content.level(key).briefing().teaser());
        view.draw(batch, state, intel, teaser, nextLevel);
        drawTopBar(batch, glass);
        glass.right(
                batch,
                glass.fonts.label,
                autosave,
                autosave.endsWith("FAILED") ? Glass.ALERT : autosave.equals(DEBUG_RUN) ? Glass.AMBER : Glass.GREEN,
                PixelScreen.WIDTH - 16,
                PixelScreen.HEIGHT - 36);
        glass.hints(batch, hints());
        if (state.focus() == Focus.REPAIR) {
            drawRepair(batch, glass);
        }
        dialog.ifPresent(question -> glass.dialog(batch, question));
    }

    private void drawTopBar(SpriteBatch batch, Glass glass) {
        glass.panel(batch, 16, 6, PixelScreen.WIDTH - 32, 40, 0.86f);
        int number = campaign.nextLevel();
        glass.shadowed(batch, glass.fonts.heading, "HANGAR", Glass.WHITE, 26, 11);
        String mission = nextLevel
                .map(key -> String.format(Locale.ROOT, "BEFORE L%02d - %s", number, Content.levelName(key)))
                .orElse(String.format(Locale.ROOT, "BEFORE L%02d - NOT BUILT YET", number))
                .toUpperCase(Locale.ROOT);
        glass.shadowed(batch, glass.fonts.label, mission, Glass.CYAN, 160, 13);
        String act = services.content
                        .actOf(number)
                        .map(entry -> entry.getValue().titleCard().act() + " - "
                                + entry.getValue().titleCard().name() + " - ")
                        .orElse("")
                + campaign.difficulty().name();
        glass.shadowed(batch, glass.fonts.label, act, Glass.LABEL, 160, 27);
        float x = 448;
        for (Command command : List.of(Command.UNDO, Command.REPAIR, Command.SAVE)) {
            glass.chip(batch, glass.fonts.label, command.name(), x, 15, 64, 22, on(command), state.enabled(command));
            x += 70;
        }
        glass.inset(batch, 662, 12, 144, 28);
        glass.right(batch, glass.fonts.body, Names.credits(campaign.credits()), Glass.AMBER, 798, 16);
        glass.chip(
                batch,
                glass.fonts.body,
                "> LAUNCH",
                814,
                10,
                124,
                32,
                on(Command.LAUNCH),
                state.enabled(Command.LAUNCH));
    }

    private boolean on(Command command) {
        return state.focus() == Focus.COMMANDS && state.command() == command;
    }

    private String hints() {
        return switch (state.focus()) {
            case SHOP ->
                state.sideSelected()
                        ? "Q/E SLOT    UP/DOWN ITEM    LEFT/RIGHT SIDE    ESC MAIN MENU"
                        : "Q/E SLOT    UP/DOWN ITEM    LEFT/RIGHT CHOICE    ENTER CONFIRM    ESC MAIN MENU";
            case COMMANDS -> "LEFT/RIGHT COMMAND    ENTER CONFIRM    DOWN SHOP    ESC MAIN MENU";
            case REPAIR ->
                state.hangar().escortHired()
                        ? "Q/E SHIP/ROOK    LEFT/RIGHT 1 POINT    UP/DOWN 10 POINTS    ENTER REPAIR    ESC CLOSE"
                        : "LEFT/RIGHT 1 POINT    UP/DOWN 10 POINTS    ENTER REPAIR    ESC CLOSE";
        };
    }

    /**
     * The repair panel: the ship's armour line and, once Rook is hired, his ({@code SHIP} and
     * {@code ROOK}, design/ui/hangar, Escort), the selected line's points on the slider and its cost.
     */
    private void drawRepair(SpriteBatch batch, Glass glass) {
        Hangar hangar = state.hangar();
        boolean rook = hangar.escortHired();
        int lines = rook ? 2 : 1;
        int width = 400;
        int height = 124 + 16 * lines;
        float x = (PixelScreen.WIDTH - width) / 2f;
        float y = (PixelScreen.HEIGHT - height) / 2f;
        glass.dim(batch, 0.4f);
        glass.panel(batch, x, y, width, height, 0.94f);
        glass.header(batch, "REPAIR ARMOUR", x + 12, x + width - 12, y + 12);
        float lineY = y + 32;
        for (HangarState.RepairLine line : HangarState.RepairLine.values()) {
            if (line == HangarState.RepairLine.ROOK && !rook) {
                continue;
            }
            boolean on = state.repairLine() == line;
            String text = repairLine(line, rook);
            glass.shadowed(batch, glass.fonts.label, text, on ? Glass.AMBER : Glass.WHITE, x + 12, lineY);
            lineY += 16;
        }
        String rate = hangar.repairCost() == 0 ? "FREE ON EASY" : Names.credits(hangar.repairCost()) + " A POINT";
        glass.shadowed(batch, glass.fonts.label, rate, Glass.LABEL, x + 12, lineY);
        int points = state.repairPoints();
        glass.slider(
                batch,
                x + 12,
                lineY + 24,
                260,
                (double) points / Math.max(1, state.affordable(state.repairLine())),
                points + " PT");
        String cost = String.format(
                Locale.ROOT,
                "REPAIR %s%d POINTS FOR %s",
                state.repairLine() == HangarState.RepairLine.ROOK ? "ROOK: " : "",
                points,
                Names.credits((long) points * hangar.repairCost()));
        glass.chip(batch, glass.fonts.label, cost, x + 12, lineY + 52, width - 24, 24, true, true);
    }

    /**
     * A repair line: {@code SHIP  ARMOUR 64 / 80 - 16 POINTS MISSING}, or without Rook the ship's
     * line as before ({@code ARMOUR 64 / 80 - 16 POINTS MISSING}).
     */
    private String repairLine(HangarState.RepairLine line, boolean named) {
        boolean ship = line == HangarState.RepairLine.SHIP;
        double armour = ship ? campaign.armour() : campaign.gear().escort().armour();
        double max = ship ? campaign.maxArmour() : campaign.escortMaxArmour();
        String text = String.format(
                Locale.ROOT,
                "ARMOUR %d / %d - %d POINTS MISSING",
                (int) Math.ceil(armour),
                (int) max,
                state.missing(line));
        if (!named) {
            return text;
        }
        return (state.repairLine() == line ? "> " : "  ") + line.name() + "  " + text;
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
        view.dispose();
    }
}
