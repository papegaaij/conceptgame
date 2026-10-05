package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;
import vanguard.content.campaign.Intel;

/**
 * The intel panel fits every level at every sensor level: no two of its parts overlap, nothing
 * reaches past the panel's edges, and every field ends above Varga's quote, whose last line stays
 * inside the panel. Text is measured in the kit's monospaced fonts by its ink below the capitals'
 * top, with the one-pixel shadow: 8 px wide and 10 high in the 8×12 label font (capitals 7, a comma
 * reaching 8), 10 by 14 in the 10×20 body font (capitals 11, a Q or comma 13).
 */
class IntelPanelLayoutTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    /** Below the panel's header line. */
    private static final int TOP = IntelPanel.Y + 20;

    private static final int BOTTOM = IntelPanel.Y + IntelPanel.HEIGHT;
    private static final int RIGHT = IntelPanel.X + IntelPanel.WIDTH;
    private static final int LABEL_INK = 10;
    private static final int BODY_INK = 14;

    /** A part's box: px from the top left, {@code right} and {@code bottom} exclusive. */
    private record Box(String what, int left, int top, int right, int bottom) {
        boolean overlaps(Box other) {
            return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
        }
    }

    /** Records where every part lands. */
    private static final class Measure implements IntelPanel.Canvas {
        final List<Box> parts = new ArrayList<>();
        final List<Box> quote = new ArrayList<>();

        @Override
        public void text(String text, Color colour, int x, int y, boolean body) {
            int cell = body ? 10 : 8;
            int height = body ? BODY_INK : LABEL_INK;
            parts.add(new Box("text \"" + text + "\"", x, y, x + cell * text.length(), y + height));
        }

        @Override
        public void portrait(BriefingPage teaser, int x, int y, int size) {
            parts.add(new Box("portrait", x - 3, y - 3, x + size + 3, y + size + 3));
        }

        @Override
        public void picture(String name, int x, int y, int size) {
            parts.add(new Box(name, x - 1, y - 1, x + size + 1, y + size + 1));
        }

        @Override
        public void chip(String text, int x, int y, int width, int height) {
            parts.add(new Box("chip " + text, x, y, x + width, y + height));
        }

        @Override
        public void bar(Color colour, int x, int y, int width, int height) {
            parts.add(new Box("bar", x, y, x + width, y + height));
        }

        @Override
        public void inset(int x, int y, int width, int height) {
            parts.add(new Box("wave strip", x, y, x + width, y + height));
        }

        @Override
        public void fill(Color colour, float x, int y, float width, int height) {
            // a wave's tick, inside the wave strip
        }

        @Override
        public void quote(String text, int x, int y) {
            quote.add(new Box("quote \"" + text + "\"", x, y, x + 8 * text.length(), y + LABEL_INK));
        }
    }

    @Test
    void everyLevelsIntelFitsThePanelAtEverySensorLevel() {
        for (String level : CONTENT.levels().keySet()) {
            BriefingPage teaser = CONTENT.level(level).briefing().teaser();
            for (int sensor = 0; sensor <= 3; sensor++) {
                assertFits(Intel.of(CONTENT, level, sensor), teaser, level, level + " at sensor L" + sensor + ": ");
            }
        }
    }

    /**
     * Varga has a line of her own for each sensor level of every level (design/ui/hangar, Intel; user
     * decision D4 of M4 part H): four different lines, each the one the panel quotes at that level.
     * That each fits the panel beside the fields it shows is checked above.
     */
    @Test
    void everyLevelHasVargasLineForEachSensorLevel() {
        for (String level : CONTENT.levels().keySet()) {
            Set<String> lines = new HashSet<>();
            for (int sensor = 0; sensor <= 3; sensor++) {
                Intel intel = Intel.of(CONTENT, level, sensor);
                String line = intel.varga();
                assertFalse(line == null || line.isBlank(), level + " has Varga's line for sensor L" + sensor);
                assertTrue(lines.add(line), level + " at sensor L" + sensor + " repeats a line: " + line);
                assertEquals(
                        String.join(" ", IntelPanel.quote(intel)),
                        Names.of("\"" + line + "\""),
                        level + " at sensor L" + sensor + ": the quote is the whole line, no word cut");
            }
        }
    }

    /**
     * An escort level's OBJECTIVE row (Level 04, from sensor L1) fits beside the other fields of every
     * level like it: one without set-piece contacts, whose row the escort levels do not have (Level 03
     * with both would run 2 px into Varga's quote). Level 04's own intel is checked above.
     */
    @Test
    void everyLevelsIntelFitsWithAnObjective() {
        for (String level : CONTENT.levels().keySet()) {
            BriefingPage teaser = CONTENT.level(level).briefing().teaser();
            for (int sensor = 0; sensor <= 3; sensor++) {
                Intel intel = Intel.of(CONTENT, level, sensor);
                if (!intel.contacts().isEmpty()) {
                    continue;
                }
                LevelData.ThreatProfile profile = intel.profile();
                LevelData.ThreatProfile escort = new LevelData.ThreatProfile(
                        profile.setting(),
                        profile.layers(),
                        profile.density(),
                        profile.traits(),
                        profile.hazards(),
                        profile.boss(),
                        profile.specials(),
                        Optional.of("ESCORT 5 CRAWLERS"),
                        profile.varga());
                Intel withObjective = new Intel(
                        intel.number(),
                        intel.sensor(),
                        escort,
                        intel.directions(),
                        intel.enemies(),
                        intel.waves(),
                        intel.contacts(),
                        intel.secrets(),
                        intel.seconds());
                Measure measure = assertFits(withObjective, teaser, level, level + " escort at L" + sensor + ": ");
                assertTrue(
                        measure.parts.stream().anyMatch(box -> box.what().equals("text \"OBJECTIVE\"")),
                        "the row is shown");
            }
        }
    }

    private static Measure assertFits(Intel intel, BriefingPage teaser, String level, String where) {
        Measure measure = new Measure();
        IntelPanel.layout(measure, intel, teaser, level);
        List<Box> all = new ArrayList<>(measure.parts);
        all.addAll(measure.quote);
        for (Box box : all) {
            assertTrue(
                    box.left() >= IntelPanel.X && box.right() <= RIGHT && box.top() >= TOP && box.bottom() <= BOTTOM,
                    where + box + " leaves the panel");
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                assertFalse(all.get(i).overlaps(all.get(j)), where + all.get(i) + " overlaps " + all.get(j));
            }
        }
        assertFalse(measure.quote.isEmpty(), where + "Varga's line is shown");
        int quoteTop = measure.quote.getFirst().top();
        for (Box box : measure.parts) {
            assertTrue(box.bottom() <= quoteTop, where + box + " runs into Varga's quote");
        }
        return measure;
    }
}
