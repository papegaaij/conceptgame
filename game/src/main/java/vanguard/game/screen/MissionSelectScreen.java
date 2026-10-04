package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Missions;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Glass;

/**
 * The mission select (design/ui/mission-select): the current campaign's levels act by act in a
 * glass panel over the dimmed title scene, the flown ones open with their best grade, the later
 * ones locked with their names hidden ("???"), as is the name of an act not reached yet. Up / down
 * select an open mission, confirm flies it as a replay ({@link Campaign#replay}), back returns to
 * the main menu. The current campaign is the most recent save, as for Continue.
 *
 * <p>Opened over the main menu it keeps the menu's title theme; after a replay it stands alone with
 * its own.
 */
public final class MissionSelectScreen implements GameScreen {
    private static final Logger LOG = Logger.getLogger(MissionSelectScreen.class.getName());
    private static final float MUSIC_VOLUME = 0.6f;
    private static final float DIMMED = 0.45f;

    private static final int PANEL_X = 230;
    private static final int PANEL_Y = 40;
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 440;
    private static final int TEXT_X = PANEL_X + 22;
    private static final int RIGHT = PANEL_X + PANEL_WIDTH - 22;
    private static final int LIST_Y = PANEL_Y + 84;
    private static final int ROW = 22;
    private static final int VISIBLE_ROWS = 14;

    /** A line of the list: an act's header or one of its missions. */
    private sealed interface Line {
        record Header(String text) implements Line {}

        record Entry(Missions.Mission mission) implements Line {}
    }

    private final GameServices services;
    private final boolean overMenu;
    private final Optional<SaveSlots.Entry.Saved> current;
    private final List<Line> lines = new ArrayList<>();
    private final Optional<MusicStreamer> music;
    private int selected = -1;
    private int top;

    /** @param overMenu opened over the main menu, whose theme plays on; otherwise it plays the theme itself */
    MissionSelectScreen(GameServices services, boolean overMenu) {
        this.services = services;
        this.overMenu = overMenu;
        current = services.saves.mostRecentEntry();
        current.ifPresent(saved -> {
            for (Missions.Act act : Missions.of(services.content, saved.save())) {
                lines.add(new Line.Header(act.label() + "  " + act.name().orElse(Missions.HIDDEN)));
                act.missions().forEach(mission -> lines.add(new Line.Entry(mission)));
            }
        });
        // The most recent flown mission first, as the one a player most likely replays.
        for (int i = lines.size() - 1; i >= 0 && selected < 0; i--) {
            if (open(i)) {
                selected = i;
            }
        }
        scroll();
        music = overMenu
                ? Optional.empty()
                : MusicStreamer.play(
                        services.audio, services.files.internal("music/title-theme.ogg"), MUSIC_VOLUME, services.mixer);
    }

    /** Over the main menu. */
    static MissionSelectScreen overMenu(GameServices services) {
        return new MissionSelectScreen(services, true);
    }

    /** Where a replay ends (won, aborted or given up): back at the mission select. */
    static GameScreen afterReplay(GameServices services) {
        return new MissionSelectScreen(services, false);
    }

    /** Whether the current campaign has a mission to replay, for the main menu's item. */
    static boolean anyOpen(GameServices services) {
        return services.saves
                .mostRecentEntry()
                .map(saved -> Missions.of(services.content, saved.save()).stream()
                        .flatMap(act -> act.missions().stream())
                        .anyMatch(Missions.Mission::open))
                .orElse(false);
    }

    /**
     * Writes a replay's better grade into the save it was started from; nothing else of the save
     * changes. A failure is logged, since it must not stop the game.
     */
    static void keepGrade(GameServices services, Campaign.Replay replay, String grade) {
        if (!(services.saves.read(replay.slot()) instanceof SaveSlots.Entry.Saved saved)) {
            return;
        }
        try {
            services.saves.write(
                    replay.slot(),
                    Missions.withGrade(saved.save(), replay.level(), grade, services.campaignRules.grades()));
        } catch (IOException | RuntimeException e) {
            LOG.log(
                    Level.WARNING,
                    "could not keep the replay's grade in slot " + replay.slot().index(),
                    e);
        }
    }

    private boolean open(int line) {
        return lines.get(line) instanceof Line.Entry entry && entry.mission().open();
    }

    @Override
    public Transition update(float seconds) {
        MenuInput input = services.menu;
        if (input.back()) {
            services.play(Sfx.MENU_BACK);
            return overMenu ? Transition.BACK : Transition.replace(MainMenuScreen.menu(services));
        }
        if (selected < 0) {
            return Transition.STAY;
        }
        int direction = input.up() ? -1 : input.down() ? 1 : 0;
        if (direction != 0 && move(direction)) {
            services.play(Sfx.MENU_MOVE);
        }
        if (!input.confirm()) {
            return Transition.STAY;
        }
        services.play(Sfx.MENU_CONFIRM);
        int level = ((Line.Entry) lines.get(selected)).mission().number();
        SaveSlots.Entry.Saved saved = current.orElseThrow();
        Campaign replay = Campaign.replay(services.campaignRules, saved.slot(), saved.save(), level);
        return Transition.replace(new LevelScreen(
                services, replay, services.content.levelKey(level).orElseThrow()));
    }

    /** Moves the selection to the next open mission that way, without wrapping. */
    private boolean move(int direction) {
        for (int i = selected + direction; i >= 0 && i < lines.size(); i += direction) {
            if (open(i)) {
                selected = i;
                scroll();
                return true;
            }
        }
        return false;
    }

    /** Keeps the selection (and its act's header above it, when it fits) in view. */
    private void scroll() {
        int focus = Math.max(0, selected);
        if (focus - 1 < top) {
            top = Math.max(0, focus - 1);
        } else if (focus >= top + VISIBLE_ROWS) {
            top = focus - VISIBLE_ROWS + 1;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, DIMMED);
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        glass.centred(batch, glass.fonts.heading, "MISSION SELECT", Glass.WHITE, PixelScreen.WIDTH / 2f, PANEL_Y + 18);
        String subtitle = current.map(
                        saved -> "REPLAY - " + saved.save().difficulty().name())
                .orElse("NO CAMPAIGN");
        glass.centred(batch, glass.fonts.label, subtitle, Glass.LABEL, PixelScreen.WIDTH / 2f, PANEL_Y + 52);
        glass.rule(batch, TEXT_X, RIGHT, PANEL_Y + 70);
        for (int i = top; i < Math.min(lines.size(), top + VISIBLE_ROWS); i++) {
            drawLine(batch, glass, lines.get(i), i == selected, LIST_Y + (i - top) * ROW);
        }
        glass.scrollMarkers(batch, RIGHT + 14, LIST_Y, top > 0, top + VISIBLE_ROWS < lines.size());
        glass.centred(
                batch,
                glass.fonts.label,
                "NO CREDITS EARNED - BEST GRADE KEPT - CAMPAIGN UNCHANGED",
                Glass.DIM,
                PixelScreen.WIDTH / 2f,
                PANEL_Y + PANEL_HEIGHT - 24);
        glass.hints(batch, "UP/DOWN SELECT    ENTER FLY    ESC BACK");
    }

    private void drawLine(SpriteBatch batch, Glass glass, Line line, boolean isSelected, int y) {
        switch (line) {
            case Line.Header header -> glass.header(batch, header.text(), TEXT_X, RIGHT, y + 4);
            case Line.Entry entry -> {
                Missions.Mission mission = entry.mission();
                String label = String.format(
                        Locale.ROOT,
                        "%02d  %s",
                        mission.number(),
                        mission.name().orElse(Missions.HIDDEN));
                glass.item(
                        batch,
                        glass.fonts.body,
                        label,
                        TEXT_X + 24,
                        y,
                        PANEL_X + 2,
                        PANEL_WIDTH - 4,
                        ROW,
                        isSelected,
                        mission.open());
                String right = mission.open() ? mission.grade().orElse("-") : "LOCKED";
                Color colour = isSelected ? Glass.AMBER : mission.open() ? Glass.GREEN : Glass.DIM;
                glass.right(batch, mission.open() ? glass.fonts.body : glass.fonts.label, right, colour, RIGHT, y);
            }
        }
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
    }
}
