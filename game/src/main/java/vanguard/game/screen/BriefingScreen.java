package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import vanguard.content.ActData;
import vanguard.content.BriefingPage;
import vanguard.content.campaign.BriefingScript;
import vanguard.content.campaign.Campaign;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.briefing.BriefingPager;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Speaker;
import vanguard.game.ui.Words;

/**
 * The briefing before a level (design/ui/briefing, chosen briefing-r08-a) over the dimmed hero
 * scene with the briefing theme: the act title card first when the level opens its act
 * (act-title-r08-a, held 3.5 s), then the pages, each with its speaker's portrait and name plate,
 * typed out at the Gameplay tab's text speed with a soft blip; the mission's objectives and the
 * hangar teaser stay below. Confirm shows the whole page, then the next; Back skips to the last
 * page. After the last page the campaign goes on with {@code next} (the hangar).
 *
 * <p>Not built yet: the tactical map image (no level has one so far), the threat summary of the
 * concept (it is the hangar intel panel's, part B2) and the portraits' expressions and static.
 */
public final class BriefingScreen implements GameScreen {
    static final float TITLE_CARD_SECONDS = 3.5f;

    private static final float MUSIC_VOLUME = 0.6f;
    private static final float TYPING_VOLUME = 0.15f;
    private static final float CURSOR_BLINK_SECONDS = 0.5f;

    private static final int HEADER_Y = 12;
    private static final int COLUMN_Y = 52;
    private static final int COLUMN_HEIGHT = 380;
    private static final int LEFT_X = 12;
    private static final int LEFT_WIDTH = 220;
    private static final int TEXT_PANEL_X = 240;
    private static final int TEXT_PANEL_WIDTH = 708;
    private static final int TEXT_X = 258;
    private static final int TEXT_Y = 74;
    private static final int LINE = 24;
    private static final int BOTTOM_Y = 440;
    private static final int BOTTOM_HEIGHT = 90;
    private static final int TEASER_X = 492;
    /** Letterbox bars of the title card. */
    private static final int BAR = 58;

    private final GameServices services;
    private final Campaign campaign;
    private final BriefingScript script;
    private final Supplier<GameScreen> next;
    private final Optional<MusicStreamer> music;
    private final Optional<Texture> titleLettering;
    private final List<List<String>> pageLines = new ArrayList<>();
    private final BriefingPager pager;
    private final List<String> teaser;
    private float titleCard;
    private float elapsed;
    private int typed;

    /** @param next the screen after the last page */
    public BriefingScreen(GameServices services, Campaign campaign, BriefingScript script, Supplier<GameScreen> next) {
        this.services = services;
        this.campaign = campaign;
        this.script = script;
        this.next = next;
        List<Integer> lengths = new ArrayList<>();
        for (BriefingPage page : script.pages()) {
            List<String> lines = lines(page);
            pageLines.add(lines);
            lengths.add(lines.stream().mapToInt(String::length).sum());
        }
        pager = new BriefingPager(lengths);
        teaser = Words.wrap(
                displayed(Speaker.of(script.teaser().speaker(), services.sprites)
                                .name() + ": \"" + script.teaser().line() + "\""),
                (PixelScreen.WIDTH - LEFT_X - 14 - TEASER_X) / 8);
        titleCard = script.titleCard().isPresent() ? TITLE_CARD_SECONDS : 0;
        titleLettering = script.titleCard().map(card -> {
            var texture = new Texture(services.files.internal("ui/" + script.actDirectory() + "-title.png"));
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            return texture;
        });
        music = MusicStreamer.play(
                services.audio, services.files.internal("music/briefing-theme.ogg"), MUSIC_VOLUME, services.mixer);
    }

    /** Upper case, with the characters the bitmap fonts lack replaced. */
    static String displayed(String text) {
        return text.toUpperCase(Locale.ROOT).replace('–', '-').replace('’', '\'');
    }

    /** A page's text in quotes, word-wrapped to the text panel's width in the body font (10 px a character). */
    static List<String> lines(BriefingPage page) {
        return Words.wrap(displayed("\"" + page.line() + "\""), (TEXT_PANEL_X + TEXT_PANEL_WIDTH - 18 - TEXT_X) / 10);
    }

    /** How many lines the text panel holds. */
    static int maxLines() {
        return (COLUMN_Y + COLUMN_HEIGHT - 20 - TEXT_Y) / LINE;
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        elapsed += seconds;
        MenuInput input = services.menu;
        if (titleCard > 0) {
            titleCard -= seconds;
            if (input.confirm() || input.back()) {
                services.play(Sfx.MENU_CONFIRM);
                titleCard = 0;
            }
            return Transition.STAY;
        }
        if (input.back()) {
            services.play(Sfx.MENU_BACK);
            pager.skip();
        } else if (input.confirm()) {
            services.play(Sfx.MENU_CONFIRM);
            pager.confirm();
        }
        if (pager.done()) {
            return Transition.replace(next.get());
        }
        int appeared = pager.update(seconds, services.settings().gameplay().textSpeed());
        for (int i = 0; i < appeared; i++) {
            if (++typed % 2 == 0) {
                services.sfx.play(Sfx.TYPEWRITER, TYPING_VOLUME, 1, 0);
            }
        }
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        if (titleCard > 0) {
            drawTitleCard(batch, glass, script.titleCard().orElseThrow());
            return;
        }
        services.titleScene.draw(batch, 0.35f);
        glass.panel(batch, LEFT_X, HEADER_Y, PixelScreen.WIDTH - 2 * LEFT_X, 34, 0.85f);
        glass.shadowed(batch, glass.fonts.body, displayed(script.act()), Glass.AMBER, LEFT_X + 14, HEADER_Y + 8);
        String mission = String.format(Locale.ROOT, "MISSION %02d: %s", script.mission(), script.missionName());
        glass.right(batch, glass.fonts.body, mission, Glass.WHITE, PixelScreen.WIDTH - LEFT_X - 14, HEADER_Y + 8);
        drawSpeaker(batch, glass);
        drawText(batch, glass);
        drawBottom(batch, glass);
    }

    private void drawSpeaker(SpriteBatch batch, Glass glass) {
        glass.panel(batch, LEFT_X, COLUMN_Y, LEFT_WIDTH, COLUMN_HEIGHT, 0.8f);
        Speaker speaker = Speaker.of(script.pages().get(pager.page()).speaker(), services.sprites);
        float centre = LEFT_X + LEFT_WIDTH / 2f;
        int portraitX = Math.round(centre - 72);
        int portraitY = COLUMN_Y + 18;
        glass.outline(batch, Glass.TRIM_LIGHT, portraitX - 2, portraitY - 2, 148, 148);
        batch.draw(speaker.portrait(), portraitX, PixelScreen.HEIGHT - portraitY - 144);
        glass.centred(batch, glass.fonts.body, speaker.name(), Glass.AMBER, centre, portraitY + 158);
        glass.centred(batch, glass.fonts.label, speaker.role(), Glass.CYAN, centre, portraitY + 180);
        String page = "PAGE " + (pager.page() + 1) + " / " + pager.pages();
        glass.centred(batch, glass.fonts.label, page, Glass.LABEL, centre, portraitY + 196);
    }

    private void drawText(SpriteBatch batch, Glass glass) {
        glass.panel(batch, TEXT_PANEL_X, COLUMN_Y, TEXT_PANEL_WIDTH, COLUMN_HEIGHT, 0.8f);
        BitmapFont font = glass.fonts.body;
        int left = pager.shown();
        List<String> lines = pageLines.get(pager.page());
        int cursorX = TEXT_X;
        int cursorY = TEXT_Y;
        for (int i = 0; i < lines.size() && left > 0; i++) {
            String line = lines.get(i).substring(0, Math.min(left, lines.get(i).length()));
            left -= line.length();
            glass.shadowed(batch, font, line, Glass.WHITE, TEXT_X, TEXT_Y + i * LINE);
            cursorX = TEXT_X + line.length() * 10 + 2;
            cursorY = TEXT_Y + i * LINE;
        }
        if (!pager.pageComplete() || elapsed % (2 * CURSOR_BLINK_SECONDS) < CURSOR_BLINK_SECONDS) {
            glass.fill(batch, Glass.AMBER, cursorX, cursorY - 1, 10, 16);
        }
    }

    private void drawBottom(SpriteBatch batch, Glass glass) {
        int width = PixelScreen.WIDTH - 2 * LEFT_X;
        glass.panel(batch, LEFT_X, BOTTOM_Y, width, BOTTOM_HEIGHT, 0.85f);
        glass.header(batch, "OBJECTIVES", LEFT_X + 12, TEASER_X - 20, BOTTOM_Y + 10);
        for (int i = 0; i < script.objectives().size(); i++) {
            int y = BOTTOM_Y + 26 + i * 14;
            glass.fill(batch, i == 0 ? Glass.AMBER : Glass.CYAN, LEFT_X + 14, y + 2, 5, 5);
            glass.shadowed(
                    batch,
                    glass.fonts.label,
                    script.objectives().get(i),
                    i == 0 ? Glass.WHITE : Glass.CYAN,
                    LEFT_X + 26,
                    y);
        }
        glass.header(batch, "IN THE HANGAR", TEASER_X, LEFT_X + width - 12, BOTTOM_Y + 10);
        for (int i = 0; i < Math.min(3, teaser.size()); i++) {
            glass.shadowed(batch, glass.fonts.label, teaser.get(i), Glass.BODY, TEASER_X, BOTTOM_Y + 26 + i * 12);
        }
        String hints = pager.page() == pager.pages() - 1 && pager.pageComplete()
                ? "ENTER TO THE HANGAR"
                : "ENTER CONTINUE    ESC SKIP TO OBJECTIVES";
        glass.right(batch, glass.fonts.label, hints, Glass.DIM, LEFT_X + width - 12, BOTTOM_Y + 72);
    }

    /** The act title card: the letterboxed scene, the chrome act number and name, the settings and the missions. */
    private void drawTitleCard(SpriteBatch batch, Glass glass, ActData.TitleCard card) {
        glass.fill(batch, Color.BLACK, 0, 0, PixelScreen.WIDTH, PixelScreen.HEIGHT);
        services.titleScene.draw(batch, 0.45f);
        glass.fill(batch, Color.BLACK, 0, 0, PixelScreen.WIDTH, BAR);
        glass.fill(batch, Color.BLACK, 0, PixelScreen.HEIGHT - BAR, PixelScreen.WIDTH, BAR);
        Texture lettering = titleLettering.orElseThrow();
        int x = (PixelScreen.WIDTH - lettering.getWidth()) / 2;
        int top = 138;
        batch.draw(lettering, x, PixelScreen.HEIGHT - top - lettering.getHeight());
        glass.fill(batch, Glass.TRIM_LIGHT, 180, top + 22, 380 - 180, 1);
        glass.fill(batch, Glass.TRIM_LIGHT, 580, top + 22, 780 - 580, 1);
        int below = top + lettering.getHeight() + 24;
        glass.centred(batch, glass.fonts.body, displayed(card.line()), Glass.CYAN, PixelScreen.WIDTH / 2f, below);
        ActData.Levels levels =
                services.content.acts().get(script.actDirectory()).levels();
        String missions = String.format(Locale.ROOT, "MISSIONS %02d - %02d", levels.first(), levels.last());
        glass.centred(batch, glass.fonts.label, missions, Glass.LABEL, PixelScreen.WIDTH / 2f, PixelScreen.HEIGHT - 36);
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
        titleLettering.ifPresent(Texture::dispose);
    }
}
