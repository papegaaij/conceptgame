package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRoute;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.input.MenuInput;
import vanguard.game.ui.Dialog;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Menu;
import vanguard.game.ui.Speaker;
import vanguard.game.ui.Words;

/**
 * PLACEHOLDER for the hangar (design/ui/hangar), which part B2 of M3 builds: a glass panel with the
 * campaign's credits, armour, loadout and next mission with the level's hangar teaser, and three
 * items: Launch (the next level, while it is built), Save game (the slot list) and Main menu. It
 * autosaves when it opens (design/systems/saves), except right after a save was loaded, and plays
 * the hangar theme.
 *
 * <p>The seam for B2: the campaign arrives here after the intro briefing, every debrief (through
 * {@link #beforeNextLevel}), Back to hangar and Abort, and Load / Continue; Launch hands the
 * campaign to a {@link LevelScreen} built from {@link CampaignRoute#launch}. The real hangar
 * replaces this class behind the same constructor.
 */
public final class HangarScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final int PANEL_X = 160;
    private static final int PANEL_Y = 70;
    private static final int PANEL_WIDTH = 640;
    private static final int PANEL_HEIGHT = 400;
    private static final int LEFT = PANEL_X + 24;
    private static final int RIGHT = PANEL_X + 344;
    private static final int ITEM_Y = PANEL_Y + 306;
    private static final int ITEM_STEP = 26;

    private enum Item {
        LAUNCH,
        SAVE,
        MAIN_MENU
    }

    private final GameServices services;
    private final Campaign campaign;
    private final Optional<String> nextLevel;
    private final Menu<Item> menu;
    private final Optional<MusicStreamer> music;
    private final String autosave;
    private Optional<Dialog> quit = Optional.empty();

    /** @param autosave write the autosave as the hangar opens */
    public HangarScreen(GameServices services, Campaign campaign, boolean autosave) {
        this.services = services;
        this.campaign = campaign;
        nextLevel = CampaignRoute.launch(services.content, campaign);
        menu = new Menu<>(List.of(
                new Menu.Item<>(Item.LAUNCH, "LAUNCH", nextLevel.isPresent()),
                Menu.Item.of(Item.SAVE, "SAVE GAME"),
                Menu.Item.of(Item.MAIN_MENU, "MAIN MENU")));
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

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        MenuInput input = services.menu;
        if (quit.isPresent()) {
            return switch (quit.get().update(input)) {
                case YES -> Transition.replace(MainMenuScreen.menu(services));
                case NO -> {
                    services.play(Sfx.MENU_BACK);
                    quit = Optional.empty();
                    yield Transition.STAY;
                }
                case NONE -> Transition.STAY;
            };
        }
        if (input.back()) {
            askToQuit();
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
            case LAUNCH -> Transition.replace(new LevelScreen(services, campaign, nextLevel.orElseThrow()));
            case SAVE -> Transition.open(SlotsScreen.save(services, campaign));
            case MAIN_MENU -> {
                askToQuit();
                yield Transition.STAY;
            }
        };
    }

    private void askToQuit() {
        quit = Optional.of(
                new Dialog("QUIT TO MAIN MENU?", "THE AUTOSAVE KEEPS THIS HANGAR VISIT.", "YES, QUIT", "NO, BACK"));
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.3f);
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, 0.85f);
        glass.shadowed(batch, glass.fonts.heading, "HANGAR", Glass.AMBER, LEFT, PANEL_Y + 16);
        glass.right(
                batch,
                glass.fonts.heading,
                "CR " + grouped(campaign.credits()),
                Glass.AMBER,
                PANEL_X + PANEL_WIDTH - 24,
                PANEL_Y + 16);
        glass.shadowed(
                batch,
                glass.fonts.label,
                "PLACEHOLDER - SHOP, LOADOUT, REPAIR AND INTEL FOLLOW (M3 PART B2)",
                Glass.DIM,
                LEFT,
                PANEL_Y + 52);
        drawLoadout(batch, glass);
        drawMission(batch, glass);
        List<Menu.Item<Item>> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            Menu.Item<Item> item = items.get(i);
            glass.item(
                    batch,
                    glass.fonts.body,
                    item.label(),
                    LEFT + 24,
                    ITEM_Y + i * ITEM_STEP,
                    PANEL_X + 2,
                    300,
                    22,
                    i == menu.selected(),
                    item.enabled());
        }
        glass.right(
                batch,
                glass.fonts.label,
                autosave,
                autosave.endsWith("FAILED") ? Glass.ALERT : Glass.GREEN,
                PANEL_X + PANEL_WIDTH - 24,
                PANEL_Y + PANEL_HEIGHT - 20);
        glass.hints(batch, "UP/DOWN SELECT    ENTER CONFIRM    ESC MAIN MENU");
        quit.ifPresent(dialog -> glass.dialog(batch, dialog));
    }

    private void drawLoadout(SpriteBatch batch, Glass glass) {
        int y = PANEL_Y + 76;
        glass.header(batch, "LOADOUT", LEFT, RIGHT - 24, y);
        Map<LoadoutSlot, Fitted> loadout = campaign.loadout();
        int row = y + 18;
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            Fitted fitted = loadout.get(slot);
            if (fitted != null) {
                glass.shadowed(batch, glass.fonts.label, slot.name().replace('_', ' '), Glass.LABEL, LEFT, row + 4);
                String item = fitted.item().replace('-', ' ').toUpperCase(Locale.ROOT)
                        + (slot == LoadoutSlot.FRONT ? " L" + fitted.level() : "");
                glass.shadowed(batch, glass.fonts.body, item, Glass.WHITE, LEFT + 96, row);
                row += 22;
            }
        }
        row += 6;
        glass.shadowed(batch, glass.fonts.label, "ARMOUR", Glass.LABEL, LEFT, row + 4);
        double share = campaign.armour() / campaign.maxArmour();
        String armour = (int) Math.ceil(campaign.armour()) + " / " + (int) campaign.maxArmour();
        Color colour = share < 0.5 ? Glass.ALERT : Glass.GREEN;
        glass.fill(batch, colour, LEFT + 96, row + 4, (float) (120 * share), 8);
        glass.outline(batch, Glass.TRIM, LEFT + 96, row + 3, 120, 10);
        glass.shadowed(batch, glass.fonts.body, armour, colour, LEFT + 224, row);
    }

    private void drawMission(SpriteBatch batch, Glass glass) {
        int y = PANEL_Y + 76;
        int right = PANEL_X + PANEL_WIDTH - 24;
        glass.header(batch, "NEXT MISSION", RIGHT, right, y);
        int number = campaign.nextLevel();
        String mission = nextLevel
                .map(key -> String.format(Locale.ROOT, "M%02d %s", number, Content.levelName(key)))
                .orElse(String.format(Locale.ROOT, "M%02d NOT BUILT YET", number))
                .toUpperCase(Locale.ROOT);
        glass.shadowed(batch, glass.fonts.body, mission, Glass.CYAN, RIGHT, y + 18);
        int row = y + 46;
        Optional<BriefingPage> teaser =
                nextLevel.map(key -> services.content.level(key).briefing().teaser());
        if (teaser.isPresent()) {
            Speaker speaker = Speaker.of(teaser.get().speaker(), services.sprites);
            glass.shadowed(batch, glass.fonts.label, speaker.name(), Glass.AMBER, RIGHT, row);
            List<String> lines =
                    Words.wrap(BriefingScreen.displayed("\"" + teaser.get().line() + "\""), (right - RIGHT) / 8);
            for (String line : lines) {
                row += 12;
                glass.shadowed(batch, glass.fonts.label, line, Glass.BODY, RIGHT, row);
            }
        } else {
            glass.shadowed(batch, glass.fonts.label, "LAUNCH WAITS FOR THE NEXT BUILD (M4).", Glass.LABEL, RIGHT, row);
        }
        row += 28;
        String[][] facts = {
            {"DIFFICULTY", campaign.difficulty().name()},
            {"SCORE", grouped(campaign.score())},
            {"PLAYTIME", SlotsScreen.playtime(campaign.playtime())},
            {
                "RETRIES",
                campaign.retriesLeft()
                        .map(left -> left + " / " + campaign.retriesPerLevel().orElseThrow())
                        .orElse("UNLIMITED")
            }
        };
        for (String[] fact : facts) {
            glass.shadowed(batch, glass.fonts.label, fact[0], Glass.LABEL, RIGHT, row);
            glass.right(batch, glass.fonts.label, fact[1], Glass.WHITE, right, row);
            row += 14;
        }
    }

    static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
    }
}
