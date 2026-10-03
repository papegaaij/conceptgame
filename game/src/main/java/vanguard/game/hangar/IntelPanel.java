package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.LevelData.Entry;
import vanguard.content.campaign.Intel;
import vanguard.content.campaign.Intel.Field;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.Portraits;
import vanguard.game.render.Sprites;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Speaker;
import vanguard.game.ui.Words;

/**
 * The hangar's intel panel (right, design/ui/hangar, Intel): the speaker of the level's hangar
 * teaser (Dr. Varga) with her 72×72 radio portrait and the teaser beside it, the level's name and
 * the sensor level, and the threat profile's fields; a field the sensor level does not show names
 * the level that would (design/player/systems, Sensor levels and hangar intel); from sensor L2 the
 * enemy types show their sensor portraits, a boss its silhouette and a set piece its silhouette as
 * an unknown contact of its size tier (tools/art/intel.py). Varga's line for the sensor level closes
 * the panel, its last line at the panel's foot. Without a built next level the panel says so.
 *
 * <p>The panel lays itself out on a {@link Canvas}: the screen, or a test's measure of where every
 * part lands.
 */
final class IntelPanel {
    static final int X = 668;
    static final int Y = 52;
    static final int WIDTH = 276;
    static final int HEIGHT = 444;

    private static final int INNER = X + 8;
    private static final int RIGHT = X + WIDTH - 8;
    private static final int VALUE_X = X + 86;
    /** The teaser's speaker: the radio portrait (tools/art/portraits.py), and its name and lines beside it. */
    private static final int SPEAKER_PORTRAIT = 72;

    private static final int TEXT_X = INNER + SPEAKER_PORTRAIT + 10;
    private static final int STEP = 14;
    /** The label font's lines, the teaser's and Varga's quote's. */
    static final int LINE = 12;
    /** The top of the quote's last line: this far above the panel's bottom edge. */
    static final int QUOTE_FOOT = 10;

    private static final int VALUE_CHARS = (RIGHT - VALUE_X) / 8;
    /** The sensor L2 pictures (tools/art/intel.py): enemy portraits, and a boss's silhouette. */
    private static final int PORTRAIT = 30;

    private static final int SILHOUETTE = 40;
    private static final int PICTURE_GAP = 6;
    /** The characters of a contact's label beside its silhouette. */
    private static final int CONTACT_CHARS = (RIGHT - SILHOUETTE - PICTURE_GAP - VALUE_X) / 8;
    /** How many enemy portraits fit the value column. */
    private static final int PORTRAITS = (RIGHT - VALUE_X + PICTURE_GAP) / (PORTRAIT + PICTURE_GAP);

    /** Where the panel's parts go, each with its top left in screen px from the top left. */
    interface Canvas {
        /** A line of text in the 8x12 label font, or the 10x20 body font. */
        void text(String text, Color colour, int x, int y, boolean body);

        /** The teaser speaker's portrait in its frame, {@code size} px square. */
        void portrait(BriefingPage teaser, int x, int y, int size);

        /** A sensor picture ({@code intel/...}) in an inset, {@code size} px square. */
        void picture(String name, int x, int y, int size);

        /** The sensor level's chip. */
        void chip(String text, int x, int y, int width, int height);

        /** A lit bar cell, a recessed inset, or a flat fill. */
        void bar(Color colour, int x, int y, int width, int height);

        void inset(int x, int y, int width, int height);

        void fill(Color colour, float x, int y, float width, int height);

        /** The quote's lines, which close the panel. */
        default void quote(String text, int x, int y) {
            text(text, Glass.CYAN, x, y, false);
        }
    }

    private final Glass glass;
    private final Sprites sprites;

    IntelPanel(Glass glass, Sprites sprites) {
        this.glass = glass;
        this.sprites = sprites;
    }

    /**
     * @param teaser the level's hangar teaser, with its intel
     */
    void draw(SpriteBatch batch, int number, Optional<Intel> intel, Optional<BriefingPage> teaser, String levelKey) {
        glass.panel(batch, X, Y, WIDTH, HEIGHT, 0.86f);
        glass.header(batch, String.format(Locale.ROOT, "INTEL - L%02d", number), INNER, RIGHT, Y + 10);
        Canvas canvas = new Screen(batch);
        if (intel.isEmpty() || teaser.isEmpty()) {
            canvas.text(String.format(Locale.ROOT, "MISSION %02d", number), Glass.CYAN, INNER, Y + 34, false);
            canvas.text("NO INTEL YET: THE MISSION", Glass.LABEL, INNER, Y + 52, false);
            canvas.text("FOLLOWS IN A LATER BUILD (M4).", Glass.LABEL, INNER, Y + 64, false);
            return;
        }
        layout(canvas, intel.get(), teaser.get(), levelKey);
    }

    /** The intel on a level with its teaser, laid out on {@code canvas}. */
    static void layout(Canvas canvas, Intel level, BriefingPage teaser, String levelKey) {
        int portraitY = Y + 26;
        canvas.portrait(teaser, INNER, portraitY, SPEAKER_PORTRAIT);
        canvas.text(Speaker.plate(teaser.speaker()), Glass.AMBER, TEXT_X, portraitY, false);
        canvas.text("CDF INTEL", Glass.CYAN, TEXT_X, portraitY + 13, false);
        int line = portraitY + 30;
        for (String text : Words.wrap(Names.of("\"" + teaser.line() + "\""), (RIGHT - TEXT_X) / 8)) {
            canvas.text(text, Glass.BODY, TEXT_X, line, false);
            line += LINE;
        }
        int y = Math.max(portraitY + SPEAKER_PORTRAIT, line) + 10;
        canvas.text(Names.of(Content.levelName(levelKey)), Glass.WHITE, INNER, y, true);
        String sensor = level.sensor() == 0 ? "NO SENSOR" : "SENSOR L" + level.sensor();
        canvas.chip(sensor, RIGHT - 84, y + 2, 84, 16);
        y += 26;
        y = field(
                canvas,
                level,
                Field.SETTING,
                "SETTING",
                List.of(Names.of(level.profile().setting())),
                y);
        y = field(
                canvas,
                level,
                Field.LAYERS,
                "LAYERS",
                List.of(joined(level.profile().layers())),
                y);
        if (level.shows(Field.DIRECTIONS)) {
            List<String> shares = level.directions().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(entry -> direction(entry.getKey()) + " " + entry.getValue() + " %")
                    .toList();
            y = field(canvas, level, Field.DIRECTIONS, "FROM", packed(shares), y);
        } else {
            y = field(
                    canvas,
                    level,
                    Field.MAIN_DIRECTION,
                    "FROM",
                    List.of(direction(level.mainDirection()) + " (MAIN)"),
                    y);
        }
        y = density(canvas, level, y);
        y = field(
                canvas,
                level,
                Field.HAZARDS,
                "HAZARDS",
                List.of(joinedOrNone(level.profile().hazards())),
                y);
        if (level.profile().objective().isPresent()) {
            y = field(
                    canvas,
                    level,
                    Field.OBJECTIVE,
                    "OBJECTIVE",
                    List.of(Names.of(level.profile().objective().get())),
                    y);
        }
        y = field(
                canvas,
                level,
                Field.SPECIALS,
                "SPECIALS",
                List.of(level.profile().specials().map(Names::of).orElse("NO LIMIT")),
                y);
        y = boss(canvas, level, y);
        y = enemies(canvas, level, y);
        y = field(canvas, level, Field.SECRETS, "SECRETS", List.of(Integer.toString(level.secrets())), y);
        y = field(
                canvas,
                level,
                Field.TRAITS,
                "RECOMMEND",
                List.of(joined(level.profile().traits())),
                y);
        waves(canvas, level, y);
        level.varga().ifPresent(text -> {
            List<String> lines = Words.wrap(Names.of("\"" + text + "\""), (RIGHT - INNER) / 8);
            int quote = Y + HEIGHT - QUOTE_FOOT - LINE * (lines.size() - 1);
            for (String part : lines) {
                canvas.quote(part, INNER, quote);
                quote += LINE;
            }
        });
    }

    private static int field(Canvas canvas, Intel intel, Field field, String label, List<String> values, int y) {
        canvas.text(label, Glass.LABEL, INNER, y, false);
        if (!intel.shows(field)) {
            canvas.text("-- SENSOR L" + field.sensor(), Glass.DIM, VALUE_X, y, false);
            return y + STEP;
        }
        for (String value : values) {
            canvas.text(value, Glass.CYAN, VALUE_X, y, false);
            y += STEP;
        }
        return y;
    }

    /** The enemy types' sensor portraits in a row, their names below. */
    private static int enemies(Canvas canvas, Intel intel, int y) {
        List<String> names = Words.wrap(Names.of(String.join(", ", intel.enemies())), VALUE_CHARS);
        if (!intel.shows(Field.ENEMIES)) {
            return field(canvas, intel, Field.ENEMIES, "ENEMIES", names, y);
        }
        int x = VALUE_X;
        for (String enemy : intel.enemies().subList(0, Math.min(intel.enemies().size(), PORTRAITS))) {
            canvas.picture("intel/" + Portraits.slug(enemy), x, y, PORTRAIT);
            x += PORTRAIT + PICTURE_GAP;
        }
        return field(canvas, intel, Field.ENEMIES, "ENEMIES", names, y + PORTRAIT + PICTURE_GAP);
    }

    /**
     * The boss's name, with its sensor silhouette at the row's right end; below it each set piece as
     * an unknown contact of its size tier ("UNKNOWN HUGE CONTACT") with its silhouette, beside the
     * boss row's {@code NONE} where the level has no boss. Below sensor L2 the row names the level
     * that shows it, and nothing tells of a contact.
     */
    private static int boss(Canvas canvas, Intel intel, int y) {
        String boss = intel.profile().boss();
        int next = field(canvas, intel, Field.BOSS, "BOSS", List.of(Names.of(boss)), y);
        if (!intel.shows(Field.BOSS)) {
            return next;
        }
        int top = y;
        if (!boss.equals("none")) {
            canvas.picture("intel/boss-" + Portraits.slug(boss), RIGHT - SILHOUETTE, top, SILHOUETTE);
            top += SILHOUETTE + PICTURE_GAP;
            next = top;
        }
        if (!intel.shows(Field.CONTACTS)) {
            return next;
        }
        for (Intel.Contact contact : intel.contacts()) {
            canvas.picture("intel/boss-" + contact.enemy(), RIGHT - SILHOUETTE, top, SILHOUETTE);
            int line = next;
            for (String text : Words.wrap(Names.of(contact.label()), CONTACT_CHARS)) {
                canvas.text(text, Glass.CYAN, VALUE_X, line, false);
                line += STEP;
            }
            top = Math.max(line, top + SILHOUETTE + PICTURE_GAP);
            next = top;
        }
        return next;
    }

    /** Values side by side, as many to a line as fit the value column. */
    private static List<String> packed(List<String> values) {
        List<String> lines = new ArrayList<>();
        for (String value : values) {
            int last = lines.size() - 1;
            if (last >= 0 && lines.get(last).length() + 2 + value.length() <= VALUE_CHARS) {
                lines.set(last, lines.get(last) + "  " + value);
            } else {
                lines.add(value);
            }
        }
        return lines;
    }

    private static int density(Canvas canvas, Intel intel, int y) {
        if (!intel.shows(Field.DENSITY)) {
            return field(canvas, intel, Field.DENSITY, "DENSITY", List.of(), y);
        }
        canvas.text("DENSITY", Glass.LABEL, INNER, y, false);
        int density = intel.profile().density();
        for (int i = 0; i < 5; i++) {
            canvas.bar(i < density ? Glass.AMBER : Glass.UNLIT, VALUE_X + i * 14, y, 12, 8);
        }
        canvas.text(density + "/5", Glass.CYAN, VALUE_X + 76, y, false);
        return y + STEP;
    }

    private static int waves(Canvas canvas, Intel intel, int y) {
        if (!intel.shows(Field.WAVES)) {
            return field(canvas, intel, Field.WAVES, "WAVES", List.of(), y);
        }
        canvas.text("WAVES", Glass.LABEL, INNER, y, false);
        int width = RIGHT - VALUE_X;
        canvas.inset(VALUE_X - 2, y - 2, width + 4, 12);
        for (double t : intel.waves()) {
            canvas.fill(Glass.AMBER, (float) (VALUE_X + width * t / intel.seconds()), y, 2, 8);
        }
        return y + STEP;
    }

    private static String direction(Entry entry) {
        return entry.name();
    }

    private static String joined(List<String> values) {
        return Names.of(String.join(", ", values));
    }

    private static String joinedOrNone(List<String> values) {
        return values.isEmpty() ? "NONE" : joined(values);
    }

    /** The panel drawn with the glass kit. */
    private final class Screen implements Canvas {
        private final SpriteBatch batch;

        Screen(SpriteBatch batch) {
            this.batch = batch;
        }

        @Override
        public void text(String text, Color colour, int x, int y, boolean body) {
            BitmapFont font = body ? glass.fonts.body : glass.fonts.label;
            glass.shadowed(batch, font, text, colour, x, y);
        }

        @Override
        public void portrait(BriefingPage teaser, int x, int y, int size) {
            glass.frame(batch, x - 3, y - 3, size + 6, size + 6);
            batch.draw(
                    Portraits.radio(sprites, teaser.speaker(), teaser.portrait().slug())
                            .first(),
                    x,
                    PixelScreen.HEIGHT - y - size);
        }

        @Override
        public void picture(String name, int x, int y, int size) {
            glass.inset(batch, x - 1, y - 1, size + 2, size + 2);
            batch.draw(sprites.region(name), x, PixelScreen.HEIGHT - y - size);
        }

        @Override
        public void chip(String text, int x, int y, int width, int height) {
            glass.chip(batch, text, x, y, width, height, false);
        }

        @Override
        public void bar(Color colour, int x, int y, int width, int height) {
            glass.bar(batch, colour, x, y, width, height);
        }

        @Override
        public void inset(int x, int y, int width, int height) {
            glass.inset(batch, x, y, width, height);
        }

        @Override
        public void fill(Color colour, float x, int y, float width, int height) {
            glass.fill(batch, colour, x, y, width, height);
        }
    }
}
