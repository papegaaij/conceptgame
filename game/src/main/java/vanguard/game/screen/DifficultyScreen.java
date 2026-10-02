package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.Difficulty;
import vanguard.content.DifficultyData;
import vanguard.content.campaign.Campaign;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Words;

/**
 * The difficulty select of a new game (design/ui/main-menu, chosen difficulty A): three rank cards
 * over the dimmed hero scene, each with its chevrons, rank, one-line description and the key
 * levers from design/systems/difficulty. Left and right choose, confirm starts the campaign, back
 * returns to the menu. A new campaign at the chosen difficulty starts with the intro briefing.
 */
public final class DifficultyScreen implements GameScreen {
    private static final int CARD_WIDTH = 270;
    private static final int CARD_HEIGHT = 312;
    private static final int GAP = 24;
    private static final int CARD_Y = 172;
    /** The selected card is raised and taller. */
    private static final int RAISE = 10;

    /** A card's rank and its description (design/ui/main-menu, Difficulty descriptions). */
    private record Rank(String name, String description) {}

    /** A row of a card: a lever and its value on the card's difficulty. */
    private record Lever(String name, String value) {}

    private static final List<Rank> RANKS = List.of(
            new Rank("RECRUIT", "Forgiving fire, free repairs, generous pay."),
            new Rank("PILOT", "The war as it was meant to be fought."),
            new Rank("ACE", "Denser fire, costly repairs, three retries per mission."));

    private final GameServices services;
    private final List<List<Lever>> levers = new ArrayList<>();
    private int selected;

    public DifficultyScreen(GameServices services) {
        this.services = services;
        selected = services.difficulty.ordinal();
        DifficultyData data = services.content.difficulty();
        for (Difficulty difficulty : Difficulty.values()) {
            levers.add(levers(data, difficulty));
        }
    }

    private static List<Lever> levers(DifficultyData data, Difficulty difficulty) {
        int repair = data.repairCost().of(difficulty);
        Optional<Integer> limit =
                switch (difficulty) {
                    case EASY -> data.retries().easy();
                    case MEDIUM -> data.retries().medium();
                    case HARD -> data.retries().hard();
                };
        String retries = limit.map(n -> n + ", THEN GAME OVER").orElse("UNLIMITED");
        return List.of(
                new Lever("ENEMY HP", factor(data.enemyHp().of(difficulty))),
                new Lever("FIRE RATE", factor(data.enemyFireRate().of(difficulty))),
                new Lever("CREDITS", factor(data.creditIncome().of(difficulty))),
                new Lever("REPAIRS", repair == 0 ? "FREE" : repair + " CR/PT"),
                new Lever("RETRIES", retries),
                new Lever("CHECKPOINT", data.bossCheckpoint().of(difficulty) ? "YES" : "NO"));
    }

    private static String factor(double value) {
        return "×" + (value == Math.rint(value) ? String.format(Locale.ROOT, "%.1f", value) : Double.toString(value));
    }

    @Override
    public Transition update(float seconds) {
        MenuInput input = services.menu;
        if (input.back()) {
            services.play(Sfx.MENU_BACK);
            return Transition.BACK;
        }
        if (input.confirm()) {
            services.play(Sfx.MENU_CONFIRM);
            Campaign campaign = Campaign.start(services.campaignRules, Difficulty.values()[selected]);
            return Transition.replace(HangarScreen.beforeNextLevel(services, campaign));
        }
        int before = selected;
        if (input.left()) {
            selected = Math.max(0, selected - 1);
        } else if (input.right()) {
            selected = Math.min(Difficulty.values().length - 1, selected + 1);
        }
        if (selected != before) {
            services.play(Sfx.MENU_MOVE);
        }
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.5f);
        services.titleScene.logo(batch, 12, 250);
        glass.centred(batch, glass.fonts.heading, "SELECT DIFFICULTY", Glass.WHITE, PixelScreen.WIDTH / 2f, 126);
        int x0 = (PixelScreen.WIDTH - 3 * CARD_WIDTH - 2 * GAP) / 2;
        for (Difficulty difficulty : Difficulty.values()) {
            int i = difficulty.ordinal();
            drawCard(batch, glass, i, x0 + i * (CARD_WIDTH + GAP), i == selected);
        }
        glass.hints(batch, "LEFT/RIGHT CHOOSE    ENTER START CAMPAIGN    ESC BACK");
    }

    private void drawCard(SpriteBatch batch, Glass glass, int index, int x, boolean on) {
        int y = on ? CARD_Y - RAISE : CARD_Y;
        int height = CARD_HEIGHT + (on ? 2 * RAISE : 0);
        float centre = x + CARD_WIDTH / 2f;
        glass.panel(batch, x, y, CARD_WIDTH, height, on ? 0.8f : 0.63f);
        if (on) {
            glass.outline(batch, Glass.AMBER, x - 2, y - 2, CARD_WIDTH + 4, height + 4);
        }
        Color accent = on ? Glass.AMBER : Glass.TRIM_LIGHT;
        int chevrons = index + 1;
        for (int k = 0; k < chevrons; k++) {
            glass.chevron(batch, accent, centre, y + 20 + k * 12);
        }
        int top = y + 30 + chevrons * 12 + 14;
        Difficulty difficulty = Difficulty.values()[index];
        glass.centred(batch, glass.fonts.heading, difficulty.name(), on ? Glass.AMBER : Glass.WHITE, centre, top);
        Rank rank = RANKS.get(index);
        glass.centred(batch, glass.fonts.body, rank.name(), Glass.CYAN, centre, top + 30);
        List<String> lines = Words.wrap(rank.description().toUpperCase(Locale.ROOT), (CARD_WIDTH - 24) / 8);
        for (int j = 0; j < lines.size(); j++) {
            glass.centred(batch, glass.fonts.label, lines.get(j), Glass.WHITE, centre, top + 52 + j * 12);
        }
        List<Lever> rows = levers.get(index);
        int rowsTop = y + height - 16 - rows.size() * 21;
        for (int k = 0; k < rows.size(); k++) {
            int rowY = rowsTop + k * 21;
            glass.fill(batch, Glass.TRIM, x + 16, rowY + 17, CARD_WIDTH - 32, 1);
            Lever lever = rows.get(k);
            glass.shadowed(batch, glass.fonts.label, lever.name(), Glass.LABEL, x + 18, rowY + 6);
            // A value too long for the body font beside its name takes the label font, as on the concept.
            int room = CARD_WIDTH - 36 - Fonts.width(glass.fonts.label, lever.name()) - 12;
            boolean fits = Fonts.width(glass.fonts.body, lever.value()) <= room;
            glass.right(
                    batch,
                    fits ? glass.fonts.body : glass.fonts.label,
                    lever.value(),
                    on ? Glass.GREEN : Glass.VALUE,
                    x + CARD_WIDTH - 18,
                    fits ? rowY + 2 : rowY + 6);
        }
    }

    @Override
    public void dispose() {}
}
