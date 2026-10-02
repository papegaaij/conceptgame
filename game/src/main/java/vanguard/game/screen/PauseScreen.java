package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Menu;

/**
 * The pause menu (design/ui/pause, chosen pause-r08-a) over the frozen, dimmed level: resume after
 * a one-second 3-2-1, restart the mission (a retry, after a confirmation), the options, abort to
 * the hangar with the level-start state (after a confirmation) and quit to the main menu (after a
 * confirmation). Restart and abort follow the retry rules (design/systems/retry): what the attempt
 * earned is lost, and on hard each uses one of the level's retries, so with none left both are
 * disabled. Back or Pause resumes; Pause during the countdown returns to the menu.
 */
public final class PauseScreen implements GameScreen {
    static final float COUNTDOWN_SECONDS = 1;

    private static final int PANEL_X = 318;
    private static final int PANEL_Y = 120;
    private static final int PANEL_WIDTH = 324;
    private static final int PANEL_HEIGHT = 298;
    private static final int ITEM_X = 349;
    private static final int ITEM_Y = 210;
    private static final int ITEM_STEP = 32;
    private static final float DIM = 0.55f;

    enum Item {
        RESUME,
        RESTART,
        OPTIONS,
        ABORT,
        QUIT
    }

    private final GameServices services;
    private final LevelScreen level;
    private final Menu<Item> menu;
    private Optional<Dialog> dialog = Optional.empty();
    /** Seconds left of the resume countdown; negative while the menu shows. */
    private float countdown = -1;

    PauseScreen(GameServices services, LevelScreen level) {
        this.services = services;
        this.level = level;
        boolean retry = level.campaign().canRetry();
        menu = new Menu<>(List.of(
                Menu.Item.of(Item.RESUME, "RESUME"),
                new Menu.Item<>(Item.RESTART, "RESTART MISSION", retry),
                Menu.Item.of(Item.OPTIONS, "OPTIONS"),
                new Menu.Item<>(Item.ABORT, "ABORT TO HANGAR", retry),
                Menu.Item.of(Item.QUIT, "QUIT TO MAIN MENU")));
    }

    @Override
    public Transition update(float seconds) {
        MenuInput input = services.menu;
        boolean pause = services.input.pressed(Action.PAUSE);
        if (countdown >= 0) {
            if (pause) {
                countdown = -1;
                return Transition.STAY;
            }
            countdown -= seconds;
            return countdown <= 0 ? Transition.BACK : Transition.STAY;
        }
        if (dialog.isPresent()) {
            return answer(dialog.get().update(input));
        }
        if (input.back() || pause) {
            resume();
            return Transition.STAY;
        }
        if (menu.navigate(input)) {
            services.play(Sfx.MENU_MOVE);
        }
        if (!input.confirm()) {
            return Transition.STAY;
        }
        services.play(Sfx.MENU_CONFIRM);
        switch (menu.selectedId()) {
            case RESUME -> resume();
            case RESTART ->
                dialog = Optional.of(new Dialog(
                        "RESTART MISSION?", "WHAT THIS ATTEMPT EARNED IS LOST.", "YES, RESTART", "NO, BACK"));
            case OPTIONS -> {
                return Transition.open(new OptionsScreen(services));
            }
            case ABORT ->
                dialog = Optional.of(
                        new Dialog("ABORT TO HANGAR?", "WHAT THIS ATTEMPT EARNED IS LOST.", "YES, ABORT", "NO, BACK"));
            case QUIT ->
                dialog = Optional.of(new Dialog(
                        "QUIT TO MAIN MENU?", "PROGRESS SINCE THE LAST SAVE IS LOST.", "YES, QUIT", "NO, BACK"));
        }
        return Transition.STAY;
    }

    private Transition answer(Dialog.Answer answer) {
        if (answer == Dialog.Answer.NONE) {
            return Transition.STAY;
        }
        dialog = Optional.empty();
        if (answer == Dialog.Answer.NO) {
            services.play(Sfx.MENU_BACK);
            return Transition.STAY;
        }
        services.play(Sfx.MENU_CONFIRM);
        return switch (menu.selectedId()) {
            case RESTART -> {
                level.retry(level.campaign().retry());
                // Like a failure's, the used retry goes into the autosave at once.
                services.save(SaveSlots.Slot.AUTOSAVE, level.campaign());
                yield Transition.BACK;
            }
            case ABORT -> {
                level.campaign().retry();
                yield Transition.replace(new HangarScreen(services, level.campaign(), true));
            }
            default -> Transition.replace(MainMenuScreen.menu(services));
        };
    }

    private void resume() {
        services.play(Sfx.MENU_BACK);
        countdown = COUNTDOWN_SECONDS;
    }

    @Override
    public void draw(SpriteBatch batch) {
        level.draw(batch);
        Glass glass = services.glass;
        if (countdown >= 0) {
            drawCountdown(batch, glass);
            return;
        }
        glass.dim(batch, DIM);
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, 0.85f);
        float centre = PANEL_X + PANEL_WIDTH / 2f;
        glass.centred(batch, glass.fonts.heading, "PAUSED", Glass.AMBER, centre, PANEL_Y + 16);
        glass.centred(batch, glass.fonts.label, level.mission(), Glass.CYAN, centre, PANEL_Y + 50);
        String status = String.format(
                Locale.ROOT,
                "TIME %d:%02d    SCORE %s    %s",
                (int) level.seconds() / 60,
                (int) level.seconds() % 60,
                String.format(Locale.ROOT, "%,d", level.score()).replace(',', ' '),
                level.campaign().difficulty().name());
        glass.centred(batch, glass.fonts.label, status, Glass.LABEL, centre, PANEL_Y + 66);
        BitmapFont font = glass.fonts.body;
        List<Menu.Item<Item>> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            Menu.Item<Item> item = items.get(i);
            glass.item(
                    batch,
                    font,
                    item.label(),
                    ITEM_X,
                    ITEM_Y + i * ITEM_STEP,
                    PANEL_X + 2,
                    PANEL_WIDTH - 4,
                    26,
                    i == menu.selected(),
                    item.enabled());
        }
        int notes = PANEL_Y + PANEL_HEIGHT - 44;
        glass.shadowed(batch, glass.fonts.label, "RESTART RETURNS TO THE LEVEL-START", Glass.DIM, ITEM_X, notes);
        glass.shadowed(batch, glass.fonts.label, "STATE (CONFIRMATION).", Glass.DIM, ITEM_X, notes + 12);
        String last = level.campaign()
                .retriesLeft()
                .map(left -> "RETRIES LEFT " + left + " / "
                        + level.campaign().retriesPerLevel().orElseThrow() + " (RESTART USES ONE)")
                .orElse("RESUME COUNTS 3-2-1 BEFORE PLAY.");
        glass.shadowed(batch, glass.fonts.label, last, Glass.DIM, ITEM_X, notes + 24);
        glass.hints(batch, "UP/DOWN SELECT    ENTER CONFIRM    ESC RESUME");
        dialog.ifPresent(d -> glass.dialog(batch, d));
    }

    /** 3, 2 and 1 in a third of a second each, large in the middle of the play field. */
    private void drawCountdown(SpriteBatch batch, Glass glass) {
        BitmapFont font = glass.fonts.heading;
        int number = Math.min(3, (int) Math.ceil(countdown / COUNTDOWN_SECONDS * 3));
        font.getData().setScale(3);
        glass.centred(batch, font, Integer.toString(number), Glass.AMBER, PixelScreen.WIDTH / 2f, 220);
        font.getData().setScale(1);
    }

    @Override
    public void dispose() {}
}
