package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;
import vanguard.game.input.Bindings;
import vanguard.game.level.ControlPrompts;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.ui.Fonts;

/**
 * The left panel's regions do not overlap, and what they show fits them, measured with the
 * metrics of the UI kit's fonts the HUD draws with (read without a GL context): the 10x20 body font
 * for the one-line readouts and the speakers' names, the 8x12 label font for the radio pages and
 * the control prompts.
 */
class MissionLayoutTest {
    private static final BitmapFont.BitmapFontData SMALL = font(Fonts.LABEL_FILE);
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);

    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void theRegionsStackDownThePanelWithAGapAndNoOverlap() {
        List<MissionLayout.Region> regions = MissionLayout.REGIONS;
        assertEquals(MissionLayout.MARGIN, regions.getFirst().top());
        for (int i = 1; i < regions.size(); i++) {
            assertEquals(
                    regions.get(i - 1).bottom() + MissionLayout.GAP,
                    regions.get(i).top(),
                    "region " + i);
        }
        assertTrue(regions.getLast().bottom() <= PixelScreen.HEIGHT - MissionLayout.MARGIN);
    }

    @Test
    void theTwoObjectiveWellsShareThePromptsAndTrackerSpace() {
        List<MissionLayout.Region> regions = MissionLayout.TWO_OBJECTIVE_REGIONS;
        for (int i = 1; i < regions.size(); i++) {
            assertEquals(
                    regions.get(i - 1).bottom() + MissionLayout.GAP,
                    regions.get(i).top(),
                    "region " + i);
        }
        // design/ui/hud: control prompts 384–430, objective tracker 436–482, the progress bar unmoved.
        assertEquals(new MissionLayout.Region(384, 46), MissionLayout.TWO_PROMPTS);
        assertEquals(new MissionLayout.Region(436, 46), MissionLayout.TWO_OBJECTIVES);
        assertEquals(MissionLayout.PROMPTS.top(), MissionLayout.TWO_PROMPTS.top());
        assertEquals(MissionLayout.OBJECTIVE.bottom(), MissionLayout.TWO_OBJECTIVES.bottom());
        float lines = MissionLayout.LINE + drop(BODY);
        assertTrue(lines <= MissionLayout.TWO_LINE_WELL, "two body lines fit the tracker's well: " + lines);
        float prompts = (MissionLayout.TWO_PROMPT_LINES - 1) * MissionLayout.LINE + drop(SMALL);
        assertTrue(prompts <= MissionLayout.TWO_LINE_WELL, "two prompt lines fit their well: " + prompts);
    }

    /**
     * Level 04's two-line tracker: {@code CRAWLERS} with a pip per unit, then the secondary's label and
     * its widest count, {@code DONE} or {@code FAILED}.
     */
    @Test
    void theConvoyTrackerFitsItsLines() {
        String crawlers = MissionPanel.escapesLabel("civilian-crawler");
        assertEquals("CRAWLERS", crawlers);
        int pips = 5 * MissionPanel.ALLY_PIP_STEP;
        assertTrue(
                width(BODY, crawlers) + pips <= MissionLayout.TEXT_WIDTH,
                "the label and five pips fit: " + (width(BODY, crawlers) + pips));
        String pods = MissionPanel.escapesLabel("brood-pod");
        for (String count : List.of("6 / 6", "DONE", "FAILED")) {
            assertFits(BODY, pods + " " + count, MissionLayout.TEXT_WIDTH);
        }
    }

    /**
     * Level 09's two-line tracker (M5 part C, design/ui/hud): {@code NODES} and six two-character
     * marks ({@code A1} … {@code C2}, the widest case of a targets tracker), right-aligned with a gap
     * after the label, over the bridge secondary {@code RAVAGERS 10 / 10} (hard's count) or its end.
     */
    @Test
    void theNodesTrackerFitsItsSixPairsAndTheRavagerLine() {
        List<String> groups = List.of("Node A1", "Node A2", "Node B1", "Node B2", "Node C1", "Node C2");
        String label = MissionPanel.groupsLabel(groups);
        assertEquals("NODES", label);
        int widest = 0;
        for (String group : groups) {
            String mark = MissionPanel.mark(group);
            assertEquals(2, mark.length(), group);
            widest = Math.max(widest, width(BODY, mark));
        }
        int step = MissionPanel.markStep(widest);
        assertTrue(step > MissionPanel.GROUP_PIP_STEP, "pairs need a wider step than letters: " + step);
        int left = MissionPanel.markLeft(groups.size(), step);
        assertTrue(
                width(BODY, label) + MissionPanel.MARK_GAP <= left,
                "the label (" + width(BODY, label) + " px) and a gap fit before the first mark at " + left);
        int right = left + (groups.size() - 1) * step + width(BODY, MissionPanel.mark(groups.getLast()));
        assertTrue(right <= MissionLayout.TEXT_WIDTH, "the last mark ends at " + right);
        String ravagers = MissionPanel.escapesLabel("ravager");
        assertEquals("RAVAGERS", ravagers);
        for (String count : List.of("10 / 10", "DONE", "FAILED")) {
            assertFits(BODY, ravagers + " " + count, MissionLayout.TEXT_WIDTH);
        }
    }

    /**
     * Level 10's air escort tracker (M5 part D, design/ui/hud, D3 = a): {@code SHUTTLES} and its count
     * on line one, five armour bars about 32 px wide side by side on line two, within the well's text
     * width and the line's height.
     */
    @Test
    void theShuttleTrackerFitsItsCountAndFiveArmourBars() {
        String shuttles = MissionPanel.escapesLabel("evacuation-shuttle");
        assertEquals("SHUTTLES", shuttles);
        assertEquals("3 / 4", MissionPanel.shuttleCount(3, 4));
        for (int alive = 0; alive <= 4; alive++) {
            String count = MissionPanel.shuttleCount(alive, 4);
            assertTrue(
                    width(BODY, shuttles) + MissionPanel.MARK_GAP + width(BODY, count) <= MissionLayout.TEXT_WIDTH,
                    "the label, a gap and " + count + " fit");
        }
        int units = CONTENT.levels().values().stream()
                .flatMap(level -> level.objectives().escort().stream())
                .filter(escort -> escort.ally().equals("evacuation-shuttle"))
                .mapToInt(LevelData.Escort::units)
                .max()
                .orElse(5);
        assertEquals(5, units, "Lifeline One to Five");
        assertTrue(MissionPanel.ALLY_BAR_WIDTH >= 30 && MissionPanel.ALLY_BAR_WIDTH <= 34, "about 32 px wide");
        for (int k = 1; k < units; k++) {
            assertTrue(
                    MissionPanel.allyBarLeft(k) >= MissionPanel.allyBarLeft(k - 1) + MissionPanel.ALLY_BAR_WIDTH + 4,
                    "bar " + k + " keeps a gap after the one before");
        }
        assertEquals(0, MissionPanel.allyBarLeft(0));
        assertTrue(
                MissionPanel.allyBarLeft(units - 1) + MissionPanel.ALLY_BAR_WIDTH <= MissionLayout.TEXT_WIDTH,
                "the last bar ends at " + (MissionPanel.allyBarLeft(units - 1) + MissionPanel.ALLY_BAR_WIDTH));
        assertTrue(
                MissionPanel.ALLY_BAR_HEIGHT + 2 * MissionLayout.FRAME
                        <= MissionLayout.TWO_LINE_WELL - MissionLayout.LINE,
                "a bar and its trough fit line two");
    }

    /**
     * 2026-10-08 (the round 32 capture read a home shuttle's bar as a lost one's): a unit home after
     * the climb-out shows a full bar in its own pale mint, unlike a flying unit's (green, white on a
     * hit, amber below half) and a lost one's (dark); its glow stays in the gap to the next bar.
     */
    @Test
    void aShuttleHomeShowsAFullBarOfItsOwnColour() {
        assertEquals(MissionPanel.ALLY_HOME, MissionPanel.shuttleBar(true, false, 0.3));
        assertEquals(MissionPanel.ALLY_HOME, MissionPanel.shuttleBar(true, true, 1));
        assertEquals(1, MissionPanel.shuttleBarShare(true, 0.3), 1e-9, "full whatever its armour");
        assertEquals(0.3, MissionPanel.shuttleBarShare(false, 0.3), 1e-9);
        for (boolean hit : new boolean[] {false, true}) {
            for (double share : new double[] {0.2, 0.8}) {
                assertTrue(!MissionPanel.ALLY_HOME.equals(MissionPanel.shuttleBar(false, hit, share)));
            }
        }
        assertEquals(Color.WHITE, MissionPanel.shuttleBar(false, true, 0.8));
        assertEquals(HudKit.AMBER, MissionPanel.shuttleBar(false, false, 0.2));
        assertTrue(
                3 + 3 < MissionPanel.ALLY_BAR_STEP - MissionPanel.ALLY_BAR_WIDTH, "two glows fit the gap between bars");
    }

    /** Level 05's letters keep their 16 px step and place. */
    @Test
    void theBatteryLettersKeepTheirLayout() {
        int widest = width(BODY, MissionPanel.mark("Battery D"));
        assertEquals(MissionPanel.GROUP_PIP_STEP, MissionPanel.markStep(widest));
        assertEquals(MissionLayout.TEXT_WIDTH - 4 * 16, MissionPanel.markLeft(4, MissionPanel.GROUP_PIP_STEP));
    }

    /** Level 06's secondary: every Mantis destroyed before it leaves, {@code MANTISES 8 / 8}. */
    @Test
    void theMantisTrackerReadsMantisesAndFits() {
        String mantises = MissionPanel.escapesLabel("mantis");
        assertEquals("MANTISES", mantises);
        for (String count : List.of("8 / 8", "DONE", "FAILED")) {
            assertFits(BODY, mantises + " " + count, MissionLayout.TEXT_WIDTH);
        }
    }

    private static BitmapFont.BitmapFontData font(String file) {
        return new BitmapFont.BitmapFontData(
                new FileHandle(new File(System.getProperty("vanguard.assetsDir"), file)), false);
    }

    /** From the top of the capitals down to the lowest descender, below the top of a well. */
    private static float drop(BitmapFont.BitmapFontData font) {
        return MissionLayout.TEXT_DROP + font.capHeight - font.descent;
    }

    @Test
    void linesOfTheFontsFitTheWells() {
        assertTrue(drop(BODY) <= MissionLayout.WELL, "a body line fits its well: " + drop(BODY));
        assertTrue(SMALL.lineHeight <= MissionLayout.LINE, "label lines fit their spacing");
        float page = (RadioQueue.PAGE_LINES - 1) * MissionLayout.LINE + drop(SMALL);
        assertTrue(page <= MissionLayout.PAGE_WELL, "a full radio page fits its well: " + page);
        assertTrue(MissionLayout.PROMPT_LINES <= RadioQueue.PAGE_LINES, "the prompts share the page well's size");
    }

    @Test
    void everyControlAndLevelPromptFitsItsLine() {
        var texts = new PromptTexts(Bindings.defaults());
        for (LevelData level : CONTENT.levels().values()) {
            List<ControlPrompts.Prompt> prompts = level.controlPrompts().stream()
                    .map(ControlPrompts.Prompt::of)
                    .toList();
            assertTrue(prompts.size() <= MissionLayout.PROMPT_LINES, "at most one line per prompt");
            if (level.objectives().escort().isPresent()) {
                assertTrue(
                        prompts.size() <= MissionLayout.TWO_PROMPT_LINES,
                        "beside a two-line tracker the prompts have two lines");
            }
            List<PromptTexts.Text> lines = new ArrayList<>(texts.of(prompts));
            level.prompts()
                    .orElse(List.of())
                    .forEach(prompt -> lines.add(new PromptTexts.Text(prompt.action(), prompt.keys())));
            for (PromptTexts.Text text : lines) {
                assertFits(SMALL, text.action(), MissionLayout.PROMPT_ACTION_WIDTH - MissionLayout.PAD);
                // As MissionPanel lays a line out: the keys after the action and the gap.
                assertFits(
                        SMALL,
                        text.keys(),
                        MissionLayout.TEXT_WIDTH - width(SMALL, text.action()) - MissionLayout.PROMPT_GAP);
            }
        }
    }

    @Test
    void everyRadioLineFitsTheSubtitleAndEverySpeakerBesideThePortrait() {
        List<LevelData.RadioLine> lines = new ArrayList<>();
        for (LevelData level : CONTENT.levels().values()) {
            level.radio()
                    .forEach(cue -> lines.add(
                            new LevelData.RadioLine(cue.speaker(), cue.line(), cue.distorted(), cue.expression())));
            level.secrets().forEach(secret -> lines.add(secret.radio()));
        }
        lines.add(CONTENT.armour().radio());
        for (LevelData.RadioLine line : lines) {
            for (String typed : RadioQueue.wrap(line.line())) {
                assertFits(SMALL, typed, MissionLayout.TEXT_WIDTH);
            }
            for (String word : line.speaker().toUpperCase(Locale.ROOT).split(" ", 2)) {
                assertFits(BODY, word, MissionLayout.NAME_WIDTH);
            }
        }
    }

    private static void assertFits(BitmapFont.BitmapFontData font, String text, int width) {
        int measured = width(font, text);
        assertTrue(measured <= width, "'" + text + "' is " + measured + " px, more than " + width);
    }

    /** The advance of the text's glyphs with their kerning, as libGDX lays out one line. */
    private static int width(BitmapFont.BitmapFontData font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            BitmapFont.Glyph glyph = font.getGlyph(text.charAt(i));
            assertNotNull(glyph, "the font has '" + text.charAt(i) + "'");
            width += glyph.xadvance;
            if (i + 1 < text.length()) {
                width += glyph.getKerning(text.charAt(i + 1));
            }
        }
        return width;
    }
}
