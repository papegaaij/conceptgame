package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.SaveGame;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Menu;

/**
 * The title screen and the main menu (design/ui/main-menu, chosen menu A): the logo over the hero
 * scene with the title theme, first with "PRESS START" (Enter / A, or Start), then with the menu in a glass panel. The
 * screens opened from the menu (difficulty, options, credits) lie over it, so the theme plays on.
 *
 * <p>Continue loads the most recent save into the hangar and is hidden while there is none; Load
 * game opens the slot list and is disabled while every slot is empty (design/systems/saves). New
 * game goes through the difficulty select into the intro briefing.
 */
public final class MainMenuScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;
    /** "PRESS START" blinks at this period. */
    private static final float BLINK_SECONDS = 1;

    private static final int PANEL_X = 586;
    private static final int PANEL_Y = 232;
    private static final int PANEL_WIDTH = 334;
    private static final int ITEM_X = 616;
    private static final int ITEM_STEP = 33;
    private static final int PADDING = 18;

    private enum Item {
        CONTINUE,
        NEW_GAME,
        LOAD_GAME,
        OPTIONS,
        CREDITS,
        QUIT
    }

    private final GameServices services;
    private final Optional<MusicStreamer> music;
    private final Optional<SaveGame> latest;
    private final Menu<Item> menu;
    private boolean title;
    private Optional<Dialog> quit = Optional.empty();
    private float elapsed;

    private MainMenuScreen(GameServices services, boolean title) {
        this.services = services;
        this.title = title;
        latest = services.saves.mostRecent();
        boolean anySave = services.saves.list().stream().anyMatch(entry -> !(entry instanceof SaveSlots.Entry.Empty));
        List<Menu.Item<Item>> items = new ArrayList<>();
        latest.ifPresent(save -> items.add(Menu.Item.of(Item.CONTINUE, "CONTINUE")));
        items.add(Menu.Item.of(Item.NEW_GAME, "NEW GAME"));
        items.add(new Menu.Item<>(Item.LOAD_GAME, "LOAD GAME", anySave));
        items.add(Menu.Item.of(Item.OPTIONS, "OPTIONS"));
        items.add(Menu.Item.of(Item.CREDITS, "CREDITS"));
        items.add(Menu.Item.of(Item.QUIT, "QUIT"));
        menu = new Menu<>(items);
        music = MusicStreamer.play(
                services.audio, services.files.internal("music/title-theme.ogg"), MUSIC_VOLUME, services.mixer);
    }

    /** The title screen, as the game starts. */
    public static MainMenuScreen title(GameServices services) {
        return new MainMenuScreen(services, true);
    }

    /** The main menu, as when a level is quit. */
    public static MainMenuScreen menu(GameServices services) {
        return new MainMenuScreen(services, false);
    }

    @Override
    public Transition update(float seconds) {
        elapsed += seconds;
        MenuInput input = services.menu;
        if (quit.isPresent()) {
            return switch (quit.get().update(input)) {
                case YES -> Transition.QUIT;
                case NO -> {
                    services.play(Sfx.MENU_BACK);
                    quit = Optional.empty();
                    yield Transition.STAY;
                }
                case NONE -> Transition.STAY;
            };
        }
        if (title) {
            if (input.back()) {
                askToQuit();
            } else if (input.confirm() || services.input.pressed(Action.PAUSE)) {
                services.play(Sfx.MENU_CONFIRM);
                title = false;
            }
            return Transition.STAY;
        }
        if (input.back()) {
            services.play(Sfx.MENU_BACK);
            title = true;
            elapsed = 0;
            return Transition.STAY;
        }
        if (menu.navigate(input)) {
            services.play(Sfx.MENU_MOVE);
        }
        if (!input.confirm()) {
            return Transition.STAY;
        }
        services.play(Sfx.MENU_CONFIRM);
        return switch (menu.selectedId()) {
            case CONTINUE ->
                Transition.replace(
                        new HangarScreen(services, Campaign.load(services.campaignRules, latest.orElseThrow()), false));
            case NEW_GAME -> Transition.open(new DifficultyScreen(services));
            case LOAD_GAME -> Transition.open(SlotsScreen.load(services));
            case OPTIONS -> Transition.open(new OptionsScreen(services));
            case CREDITS -> Transition.open(new CreditsScreen(services));
            case QUIT -> {
                askToQuit();
                yield Transition.STAY;
            }
        };
    }

    private void askToQuit() {
        quit = Optional.of(new Dialog("QUIT THE GAME?", "BACK TO THE DESKTOP.", "YES, QUIT", "NO, BACK"));
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 1);
        services.titleScene.logo(batch, 20, 460);
        if (title) {
            if (elapsed % BLINK_SECONDS < BLINK_SECONDS * 0.7f) {
                glass.centred(batch, glass.fonts.heading, "PRESS START", Glass.WHITE, PixelScreen.WIDTH / 2f, 440);
            }
        } else {
            drawMenu(batch, glass);
            glass.hints(batch, "UP/DOWN SELECT    ENTER CONFIRM    ESC BACK");
        }
        glass.right(batch, glass.fonts.label, "(C) 2185 UTC DEFENCE FORCE", Glass.DIM, PixelScreen.WIDTH - 16, 518);
        quit.ifPresent(dialog -> glass.dialog(batch, dialog));
    }

    private void drawMenu(SpriteBatch batch, Glass glass) {
        BitmapFont font = glass.fonts.heading;
        List<Menu.Item<Item>> items = menu.items();
        int height = 2 * PADDING + (items.size() - 1) * ITEM_STEP + (int) font.getCapHeight();
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, height);
        for (int i = 0; i < items.size(); i++) {
            Menu.Item<Item> item = items.get(i);
            glass.item(
                    batch,
                    font,
                    item.label(),
                    ITEM_X,
                    PANEL_Y + PADDING + i * ITEM_STEP,
                    PANEL_X + 2,
                    PANEL_WIDTH - 4,
                    30,
                    i == menu.selected(),
                    item.enabled());
        }
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
    }
}
