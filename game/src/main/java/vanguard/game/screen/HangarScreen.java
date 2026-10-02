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
 * warning and quitting ask first. It autosaves when it opens (design/systems/saves), except right
 * after a save was loaded, and when the player quits from it, and plays the hangar theme.
 */
public final class HangarScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;

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
        state = new HangarState(new Hangar(services.catalogue, campaign), nextLevel.isPresent());
        view = new HangarView(services.files, services.glass, services.sprites, services.catalogue);
        if (autosave) {
            this.autosave = services.save(SaveSlots.Slot.AUTOSAVE, campaign) ? "AUTOSAVED" : "AUTOSAVE FAILED";
        } else {
            this.autosave = "";
        }
        music = MusicStreamer.play(
                services.audio, services.files.internal("music/hangar-theme.ogg"), MUSIC_VOLUME, services.mixer);
    }

    /** After a won level: the next level's briefing and then the hangar, or the hangar while that level is not built. */
    public static GameScreen beforeNextLevel(GameServices services, Campaign campaign) {
        return switch (CampaignRoute.beforeNextLevel(services.content, campaign)) {
            case CampaignRoute.Step.Briefing briefing ->
                new BriefingScreen(
                        services, campaign, briefing.script(), () -> new HangarScreen(services, campaign, true));
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
                                "THE AUTOSAVE KEEPS THIS HANGAR VISIT.",
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
            outcome = state.confirm();
        }
        return react(outcome);
    }

    private Transition react(Outcome outcome) {
        switch (outcome) {
            case NONE -> {}
            case MOVED -> services.play(Sfx.MENU_MOVE);
            case DONE -> services.play(Sfx.MENU_CONFIRM);
            case REFUSED -> services.play(Sfx.MENU_BACK);
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
        services.play(Sfx.MENU_CONFIRM);
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
                autosave.endsWith("FAILED") ? Glass.ALERT : Glass.GREEN,
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
            case SHOP -> "Q/E SLOT    UP/DOWN ITEM    LEFT/RIGHT CHOICE    ENTER CONFIRM    ESC MAIN MENU";
            case COMMANDS -> "LEFT/RIGHT COMMAND    ENTER CONFIRM    DOWN SHOP    ESC MAIN MENU";
            case REPAIR -> "LEFT/RIGHT 1 POINT    UP/DOWN 10 POINTS    ENTER REPAIR    ESC CLOSE";
        };
    }

    private void drawRepair(SpriteBatch batch, Glass glass) {
        Hangar hangar = state.hangar();
        int width = 400;
        int height = 140;
        float x = (PixelScreen.WIDTH - width) / 2f;
        float y = (PixelScreen.HEIGHT - height) / 2f;
        glass.dim(batch, 0.4f);
        glass.panel(batch, x, y, width, height, 0.94f);
        glass.header(batch, "REPAIR ARMOUR", x + 12, x + width - 12, y + 12);
        String armour = String.format(
                Locale.ROOT,
                "ARMOUR %d / %d - %d POINTS MISSING",
                (int) Math.ceil(campaign.armour()),
                (int) campaign.maxArmour(),
                hangar.missingArmour());
        glass.shadowed(batch, glass.fonts.label, armour, Glass.WHITE, x + 12, y + 32);
        String rate = hangar.repairCost() == 0 ? "FREE ON EASY" : Names.credits(hangar.repairCost()) + " A POINT";
        glass.shadowed(batch, glass.fonts.label, rate, Glass.LABEL, x + 12, y + 48);
        int points = state.repairPoints();
        glass.slider(
                batch, x + 12, y + 72, 260, (double) points / Math.max(1, hangar.affordableRepair()), points + " PT");
        String cost = String.format(
                Locale.ROOT, "REPAIR %d POINTS FOR %s", points, Names.credits((long) points * hangar.repairCost()));
        glass.chip(batch, glass.fonts.label, cost, x + 12, y + 100, width - 24, 24, true, true);
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
        view.dispose();
    }
}
