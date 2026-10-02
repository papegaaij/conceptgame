package vanguard.game.hangar;

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
 * teaser (Dr. Varga) with her portrait and the teaser, the level's name and the sensor level, and
 * the threat profile's fields; a field the sensor level does not show names the level that would
 * (design/player/systems, Sensor levels and hangar intel); from sensor L2 the enemy types show their
 * sensor portraits and a boss its silhouette (tools/art/intel.py). Varga's line for the sensor level
 * closes the panel. Without a built next level the panel says so.
 */
final class IntelPanel {
    private static final int X = 668;
    private static final int Y = 52;
    private static final int WIDTH = 276;
    private static final int HEIGHT = 444;

    private static final int INNER = X + 8;
    private static final int RIGHT = X + WIDTH - 8;
    private static final int VALUE_X = X + 86;
    private static final int TEXT_X = X + 160;
    private static final int STEP = 14;
    private static final int VALUE_CHARS = (RIGHT - VALUE_X) / 8;
    /** The sensor L2 pictures (tools/art/intel.py): enemy portraits, and a boss's silhouette. */
    private static final int PORTRAIT = 30;

    private static final int SILHOUETTE = 40;
    private static final int PICTURE_GAP = 6;
    /** How many enemy portraits fit the value column. */
    private static final int PORTRAITS = (RIGHT - VALUE_X + PICTURE_GAP) / (PORTRAIT + PICTURE_GAP);

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
        if (intel.isEmpty() || teaser.isEmpty()) {
            glass.shadowed(
                    batch,
                    glass.fonts.label,
                    String.format(Locale.ROOT, "MISSION %02d", number),
                    Glass.CYAN,
                    INNER,
                    Y + 34);
            glass.shadowed(batch, glass.fonts.label, "NO INTEL YET: THE MISSION", Glass.LABEL, INNER, Y + 52);
            glass.shadowed(batch, glass.fonts.label, "FOLLOWS IN A LATER BUILD (M4).", Glass.LABEL, INNER, Y + 64);
            return;
        }
        Speaker speaker = Speaker.of(teaser.get().speaker(), teaser.get().portrait(), sprites);
        int portraitY = Y + 26;
        glass.frame(batch, INNER - 3, portraitY - 3, 150, 150);
        batch.draw(speaker.portrait(), INNER, PixelScreen.HEIGHT - portraitY - 144);
        glass.shadowed(batch, glass.fonts.label, speaker.name(), Glass.AMBER, TEXT_X, portraitY);
        glass.shadowed(batch, glass.fonts.label, "CDF INTEL", Glass.CYAN, TEXT_X, portraitY + 13);
        int line = portraitY + 32;
        for (String text : Words.wrap(Names.of("\"" + teaser.get().line() + "\""), (RIGHT - TEXT_X) / 8)) {
            glass.shadowed(batch, glass.fonts.label, text, Glass.BODY, TEXT_X, line);
            line += 12;
        }
        Intel level = intel.get();
        int y = portraitY + 154;
        glass.shadowed(batch, glass.fonts.body, Names.of(Content.levelName(levelKey)), Glass.WHITE, INNER, y);
        String sensor = level.sensor() == 0 ? "NO SENSOR" : "SENSOR L" + level.sensor();
        glass.chip(batch, sensor, RIGHT - 84, y + 2, 84, 16, false);
        y += 26;
        y = field(
                batch,
                level,
                Field.SETTING,
                "SETTING",
                List.of(Names.of(level.profile().setting())),
                y);
        y = field(
                batch,
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
            y = field(batch, level, Field.DIRECTIONS, "FROM", packed(shares), y);
        } else {
            y = field(
                    batch,
                    level,
                    Field.MAIN_DIRECTION,
                    "FROM",
                    List.of(direction(level.mainDirection()) + " (MAIN)"),
                    y);
        }
        y = density(batch, level, y);
        y = field(
                batch,
                level,
                Field.HAZARDS,
                "HAZARDS",
                List.of(joinedOrNone(level.profile().hazards())),
                y);
        y = field(
                batch,
                level,
                Field.SPECIALS,
                "SPECIALS",
                List.of(level.profile().specials().map(Names::of).orElse("NO LIMIT")),
                y);
        y = boss(batch, level, y);
        y = enemies(batch, level, y);
        y = field(batch, level, Field.SECRETS, "SECRETS", List.of(Integer.toString(level.secrets())), y);
        y = field(
                batch,
                level,
                Field.TRAITS,
                "RECOMMEND",
                List.of(joined(level.profile().traits())),
                y);
        y = waves(batch, level, y);
        level.varga().ifPresent(text -> {
            int quote = Y + HEIGHT - 34;
            for (String part : Words.wrap(Names.of("\"" + text + "\""), (RIGHT - INNER) / 8)) {
                glass.shadowed(batch, glass.fonts.label, part, Glass.CYAN, INNER, quote);
                quote += 12;
            }
        });
    }

    private int field(SpriteBatch batch, Intel intel, Field field, String label, List<String> values, int y) {
        glass.shadowed(batch, glass.fonts.label, label, Glass.LABEL, INNER, y);
        if (!intel.shows(field)) {
            glass.shadowed(batch, glass.fonts.label, "-- SENSOR L" + field.sensor(), Glass.DIM, VALUE_X, y);
            return y + STEP;
        }
        for (String value : values) {
            glass.shadowed(batch, glass.fonts.label, value, Glass.CYAN, VALUE_X, y);
            y += STEP;
        }
        return y;
    }

    /** The enemy types' sensor portraits in a row, their names below. */
    private int enemies(SpriteBatch batch, Intel intel, int y) {
        List<String> names = Words.wrap(Names.of(String.join(", ", intel.enemies())), VALUE_CHARS);
        if (!intel.shows(Field.ENEMIES)) {
            return field(batch, intel, Field.ENEMIES, "ENEMIES", names, y);
        }
        int x = VALUE_X;
        for (String enemy : intel.enemies().subList(0, Math.min(intel.enemies().size(), PORTRAITS))) {
            picture(batch, "intel/" + Portraits.slug(enemy), x, y, PORTRAIT);
            x += PORTRAIT + PICTURE_GAP;
        }
        return field(batch, intel, Field.ENEMIES, "ENEMIES", names, y + PORTRAIT + PICTURE_GAP);
    }

    /** The boss's name, with its sensor silhouette at the row's right end. */
    private int boss(SpriteBatch batch, Intel intel, int y) {
        String boss = intel.profile().boss();
        int next = field(batch, intel, Field.BOSS, "BOSS", List.of(Names.of(boss)), y);
        if (!intel.shows(Field.BOSS) || boss.equals("none")) {
            return next;
        }
        picture(batch, "intel/boss-" + Portraits.slug(boss), RIGHT - SILHOUETTE, y, SILHOUETTE);
        return y + SILHOUETTE + PICTURE_GAP;
    }

    /** A sensor picture of {@code size} px square in an inset, its top left at ({@code x}, {@code y}). */
    private void picture(SpriteBatch batch, String name, int x, int y, int size) {
        glass.inset(batch, x - 1, y - 1, size + 2, size + 2);
        batch.draw(sprites.region(name), x, PixelScreen.HEIGHT - y - size);
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

    private int density(SpriteBatch batch, Intel intel, int y) {
        if (!intel.shows(Field.DENSITY)) {
            return field(batch, intel, Field.DENSITY, "DENSITY", List.of(), y);
        }
        glass.shadowed(batch, glass.fonts.label, "DENSITY", Glass.LABEL, INNER, y);
        int density = intel.profile().density();
        for (int i = 0; i < 5; i++) {
            glass.bar(batch, i < density ? Glass.AMBER : Glass.UNLIT, VALUE_X + i * 14, y, 12, 8);
        }
        glass.shadowed(batch, glass.fonts.label, density + "/5", Glass.CYAN, VALUE_X + 76, y);
        return y + STEP;
    }

    private int waves(SpriteBatch batch, Intel intel, int y) {
        if (!intel.shows(Field.WAVES)) {
            return field(batch, intel, Field.WAVES, "WAVES", List.of(), y);
        }
        glass.shadowed(batch, glass.fonts.label, "WAVES", Glass.LABEL, INNER, y);
        int width = RIGHT - VALUE_X;
        glass.inset(batch, VALUE_X - 2, y - 2, width + 4, 12);
        for (double t : intel.waves()) {
            glass.fill(batch, Glass.AMBER, (float) (VALUE_X + width * t / intel.seconds()), y, 2, 8);
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
}
