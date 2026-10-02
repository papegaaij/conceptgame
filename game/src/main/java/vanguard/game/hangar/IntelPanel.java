package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.LevelData.Entry;
import vanguard.content.campaign.Intel;
import vanguard.content.campaign.Intel.Field;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.Sprites;
import vanguard.game.ui.Glass;
import vanguard.game.ui.Speaker;
import vanguard.game.ui.Words;

/**
 * The hangar's intel panel (right, design/ui/hangar, Intel): the speaker of the level's hangar
 * teaser (Dr. Varga) with her portrait and the teaser, the level's name and the sensor level, and
 * the threat profile's fields; a field the sensor level does not show names the level that would
 * (design/player/systems, Sensor levels and hangar intel). Varga's line for the sensor level
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
    private static final Color EMPTY = new Color(0.10f, 0.12f, 0.22f, 1);

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
        Speaker speaker = Speaker.of(teaser.get().speaker(), sprites);
        int portraitY = Y + 26;
        glass.outline(batch, Glass.TRIM_LIGHT, INNER - 2, portraitY - 2, 148, 148);
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
            y = field(batch, level, Field.DIRECTIONS, "FROM", shares, y);
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
        y = field(
                batch,
                level,
                Field.BOSS,
                "BOSS",
                List.of(Names.of(level.profile().boss())),
                y);
        y = field(
                batch,
                level,
                Field.ENEMIES,
                "ENEMIES",
                Words.wrap(Names.of(String.join(", ", level.enemies())), VALUE_CHARS),
                y);
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

    private int density(SpriteBatch batch, Intel intel, int y) {
        if (!intel.shows(Field.DENSITY)) {
            return field(batch, intel, Field.DENSITY, "DENSITY", List.of(), y);
        }
        glass.shadowed(batch, glass.fonts.label, "DENSITY", Glass.LABEL, INNER, y);
        int density = intel.profile().density();
        for (int i = 0; i < 5; i++) {
            glass.fill(batch, i < density ? Glass.AMBER : EMPTY, VALUE_X + i * 14, y, 12, 8);
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
        glass.fill(batch, EMPTY, VALUE_X, y, width, 8);
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
