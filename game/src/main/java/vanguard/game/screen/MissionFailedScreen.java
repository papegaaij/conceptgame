package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.Expression;
import vanguard.content.campaign.Campaign;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.hangar.Names;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Menu;
import vanguard.game.ui.Speaker;
import vanguard.game.ui.Words;
import vanguard.sim.LevelScript;

/**
 * The mission failed screen (design/systems/retry, chosen mission-failed-r08-a) over the frozen
 * level, tinted red: Okafor's portrait and line (a failed primary objective's own line and speaker
 * when the level has one), Retry (the level at once, from its start state),
 * Back to hangar (the level-start state, to change the loadout first) and Quit to main menu (after
 * a confirmation), with what the attempt earned and, on hard, the retries left: the failure has
 * used its retry already (and the autosave holds it), so Retry and Back to hangar only take it. Once
 * the attempt reached a boss checkpoint (easy and medium), Retry from boss comes first: the boss's
 * arrival on an empty field with the defences and tallies of then; it uses no retry.
 */
public final class MissionFailedScreen implements GameScreen {
    private static final Color TINT = new Color(0.45f, 0.02f, 0.02f, 0.5f);
    private static final int PANEL_X = 212;
    private static final int PANEL_Y = 111;
    private static final int PANEL_WIDTH = 537;
    private static final int PANEL_HEIGHT = 328;
    private static final int ITEM_X = 421;
    private static final int ITEM_Y = 180;
    private static final int ITEM_STEP = 34;
    /** The line under the portrait: two lines of the label font fit there. */
    private static final int LINE_CHARS = 22;

    private static final String DEFAULT_LINE = "Pull back, Lancer. Regroup and try again.";

    private enum Item {
        BOSS,
        RETRY,
        HANGAR,
        QUIT
    }

    private final GameServices services;
    private final LevelScreen level;
    private final Campaign campaign;
    private final Menu<Item> menu;
    private final Speaker speaker;
    private final List<String> lines;
    private Optional<Dialog> quit = Optional.empty();

    MissionFailedScreen(GameServices services, LevelScreen level) {
        this.services = services;
        this.level = level;
        this.campaign = level.campaign();
        List<Menu.Item<Item>> items = new java.util.ArrayList<>();
        if (level.bossCheckpoint()) {
            items.add(Menu.Item.of(Item.BOSS, "RETRY FROM BOSS"));
        }
        items.add(Menu.Item.of(Item.RETRY, "RETRY"));
        items.add(Menu.Item.of(Item.HANGAR, "BACK TO HANGAR"));
        items.add(Menu.Item.of(Item.QUIT, "QUIT TO MAIN MENU"));
        menu = new Menu<>(items);
        speaker = level.failureLine()
                .map(cue -> Speaker.of(
                        cue.speaker(), Expression.valueOf(cue.expression().toUpperCase(Locale.ROOT)), services.sprites))
                .orElseGet(() -> Speaker.of("Okafor", Expression.GRIM, services.sprites));
        lines = Words.wrap(
                level.failureLine()
                        .map(LevelScript.RadioCue::line)
                        .orElse(DEFAULT_LINE)
                        .toUpperCase(Locale.ROOT),
                LINE_CHARS);
        // The level's own line is spoken too (design/audio/voice); the default line has no voice.
        level.failureLine()
                .flatMap(cue -> services.voices.radio(cue.speaker(), cue.line(), cue.expression()))
                .ifPresent(services.voices::play);
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        services.voices.update(seconds);
        MenuInput input = services.menu;
        if (quit.isPresent()) {
            return switch (quit.get().update(input)) {
                case YES -> {
                    services.voices.stop();
                    yield Transition.replace(MainMenuScreen.menu(services));
                }
                case NO -> {
                    services.play(Sfx.MENU_BACK);
                    quit = Optional.empty();
                    yield Transition.STAY;
                }
                case NONE -> Transition.STAY;
            };
        }
        if (menu.navigate(input)) {
            services.play(Sfx.MENU_MOVE);
        }
        if (!input.confirm()) {
            return Transition.STAY;
        }
        services.play(Sfx.MENU_CONFIRM);
        if (menu.selectedId() != Item.QUIT) {
            services.voices.stop();
        }
        return switch (menu.selectedId()) {
            case BOSS -> {
                level.retryFromBoss();
                yield Transition.BACK;
            }
            case RETRY -> {
                level.retry(campaign.armour());
                yield Transition.BACK;
            }
            case HANGAR -> Transition.replace(new HangarScreen(services, campaign, true));
            case QUIT -> {
                quit = Optional.of(new Dialog(
                        "QUIT TO MAIN MENU?", "PROGRESS SINCE THE LAST SAVE IS LOST.", "YES, QUIT", "NO, BACK"));
                yield Transition.STAY;
            }
        };
    }

    @Override
    public void draw(SpriteBatch batch) {
        level.draw(batch);
        Glass glass = services.glass;
        glass.fill(batch, TINT, 0, 0, PixelScreen.WIDTH, PixelScreen.HEIGHT);
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, 0.9f);
        float centre = PANEL_X + PANEL_WIDTH / 2f;
        glass.centred(batch, glass.fonts.heading, "MISSION FAILED", Glass.ALERT, centre, PANEL_Y + 14);
        String mission = level.mission() + " - " + campaign.difficulty().name();
        glass.centred(batch, glass.fonts.label, mission, Glass.LABEL, centre, PANEL_Y + 48);
        int portraitX = PANEL_X + 16;
        int portraitY = PANEL_Y + 66;
        glass.frame(batch, portraitX - 3, portraitY - 3, 150, 150);
        batch.draw(speaker.portrait(), portraitX, PixelScreen.HEIGHT - portraitY - 144);
        glass.shadowed(batch, glass.fonts.label, speaker.name(), Glass.AMBER, portraitX, portraitY + 152);
        for (int i = 0; i < lines.size(); i++) {
            glass.shadowed(batch, glass.fonts.label, lines.get(i), Glass.WHITE, portraitX, portraitY + 166 + 12 * i);
        }
        List<Menu.Item<Item>> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            Menu.Item<Item> item = items.get(i);
            glass.item(
                    batch,
                    glass.fonts.body,
                    item.label(),
                    ITEM_X,
                    ITEM_Y + i * ITEM_STEP,
                    ITEM_X - 24,
                    PANEL_X + PANEL_WIDTH - ITEM_X + 22,
                    26,
                    i == menu.selected(),
                    item.enabled());
        }
        int notes = PANEL_Y + 220;
        glass.shadowed(batch, glass.fonts.label, "THIS ATTEMPT IS DISCARDED:", Glass.CYAN, ITEM_X - 18, notes);
        String lost = String.format(
                Locale.ROOT, "%s CR   %s PTS", Names.grouped(level.attemptCredits()), Names.grouped(level.score()));
        glass.shadowed(batch, glass.fonts.label, lost, Glass.AMBER, ITEM_X - 18, notes + 14);
        campaign.retriesLeft()
                .ifPresent(left -> glass.shadowed(
                        batch,
                        glass.fonts.label,
                        "RETRIES LEFT " + left + " / "
                                + campaign.retriesPerLevel().orElseThrow(),
                        Glass.ALERT,
                        ITEM_X - 18,
                        notes + 32));
        glass.shadowed(batch, glass.fonts.label, "BACK TO HANGAR LETS YOU", Glass.DIM, ITEM_X - 18, notes + 52);
        glass.shadowed(batch, glass.fonts.label, "CHANGE THE LOADOUT FIRST.", Glass.DIM, ITEM_X - 18, notes + 64);
        glass.hints(batch, "UP/DOWN SELECT    ENTER CONFIRM");
        quit.ifPresent(dialog -> glass.dialog(batch, dialog));
    }

    @Override
    public void dispose() {}
}
