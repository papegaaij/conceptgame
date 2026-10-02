package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.render.PixelScreen;
import vanguard.sim.LevelResult;

/**
 * The debrief after a won level (design/ui/debrief), a placeholder in the glass style of the
 * chosen debrief concept (debrief-r08-a): the tally, the credits by source with the grade bonus
 * and the total, the score and the grade stamp. Lines appear 0.3 s apart with a tick while their
 * numbers count up; confirm skips the animation, and once it is done returns to the title (the
 * campaign loop follows in M3).
 */
public final class DebriefScreen implements GameScreen {
    private static final float LINE_SECONDS = 0.3f;
    private static final float STAMP_DELAY_SECONDS = 0.5f;
    private static final float JINGLE_VOLUME = 0.8f;
    private static final float TICK_VOLUME = 0.4f;

    private static final int LEFT = 150;
    private static final int RIGHT = 640;
    private static final int TOP = 470;
    private static final int LINE = 21;
    private static final Color GLASS = new Color(0.03f, 0.04f, 0.12f, 0.92f);
    private static final Color RULE = Color.valueOf("5A64A0");
    private static final Color TITLE = Color.valueOf("FFE84A");
    private static final Color LABEL = Color.valueOf("A8B4D8");
    private static final Color VALUE = Color.WHITE;
    private static final Color GAIN = Color.valueOf("40FF80");
    private static final Color CREDITS = Color.valueOf("FFE04A");
    private static final Color HEADING = Color.valueOf("FFE04A");

    /**
     * One debrief line: a label, a middle column and a number that counts up on the right.
     *
     * @param amount the number; negative for none
     */
    private record Row(String label, String middle, long amount, String prefix, String suffix, Color colour) {
        static Row heading(String text) {
            return new Row(text, "", -1, "", "", HEADING);
        }

        String right(double progress) {
            return amount < 0 ? "" : prefix + grouped(Math.round(amount * progress)) + suffix;
        }
    }

    private final GameServices services;
    private final String title;
    private final String subtitle;
    private final List<Row> rows = new ArrayList<>();
    private final String grade;
    private float elapsed;
    private int shownLines;
    private boolean stamped;

    /**
     * @param number the level number
     * @param name the level's name
     * @param launchBalance the credits the player launched with
     */
    public DebriefScreen(GameServices services, LevelResult result, int number, String name, int launchBalance) {
        this.services = services;
        title = String.format(Locale.ROOT, "MISSION %02d COMPLETE", number);
        subtitle = name.toUpperCase(Locale.ROOT);
        grade = result.grade().letter();
        addTally(result);
        addCredits(result, launchBalance);
        services.sfx.play(Sfx.MISSION_COMPLETE, JINGLE_VOLUME, 1, 0);
    }

    private void addTally(LevelResult result) {
        rows.add(Row.heading("TALLY"));
        rows.add(new Row(
                "ENEMIES DESTROYED",
                result.kills() + " / " + result.enemies() + "   " + result.killPercent() + " %",
                bonus(result, "Destruction"),
                "+ ",
                "",
                GAIN));
        rows.add(new Row(
                "ARMOUR DAMAGE TAKEN",
                Integer.toString((int) Math.ceil(result.armourDamage())),
                bonus(result, "Untouched"),
                "UNTOUCHED + ",
                "",
                GAIN));
        rows.add(new Row(
                "SECRETS FOUND",
                result.secretsFound() + " / " + result.secrets(),
                bonus(result, "Explorer"),
                "+ ",
                "",
                GAIN));
        rows.add(new Row(
                "MAX CHAIN",
                result.maxChain() + "   x" + String.format(Locale.ROOT, "%.1f", result.maxMultiplier()),
                -1,
                "",
                "",
                VALUE));
        rows.add(new Row(
                "SECONDARY OBJECTIVE",
                result.secondaryMet() ? "MET" : "MISSED",
                result.secondaryMet() ? result.credits().objectives() : -1,
                "+ ",
                " CR",
                CREDITS));
    }

    private void addCredits(LevelResult result, int launchBalance) {
        LevelResult.Credits credits = result.credits();
        rows.add(Row.heading("CREDITS"));
        rows.add(new Row("BALANCE AT LAUNCH", "", launchBalance, "", "", CREDITS));
        rows.add(new Row("KILLS", "", credits.kills(), "", "", CREDITS));
        rows.add(new Row("GROUND TARGETS", "", credits.groundTargets(), "", "", CREDITS));
        rows.add(new Row("SALVAGE", "", credits.salvage(), "", "", CREDITS));
        rows.add(new Row("SECRETS", "", credits.secrets(), "", "", CREDITS));
        rows.add(new Row("OBJECTIVES", "", credits.objectives(), "", "", CREDITS));
        String bonusShare = String.format(
                Locale.ROOT, "%s  +%.0f %%", grade, 100 * result.grade().creditBonus());
        rows.add(new Row("GRADE BONUS", bonusShare, result.gradeBonus(), "", "", CREDITS));
        long total = launchBalance + credits.total() + result.gradeBonus();
        rows.add(new Row("TOTAL CREDITS", "", total, "", "", CREDITS));
        rows.add(new Row("SCORE", "", result.score(), "", "", GAIN));
    }

    private static long bonus(LevelResult result, String name) {
        return result.bonuses().stream()
                .filter(b -> b.name().equals(name))
                .mapToLong(LevelResult.BonusScore::score)
                .findFirst()
                .orElse(-1);
    }

    @Override
    public GameScreen update(float seconds) {
        boolean done = stamped;
        if (services.input.pressed(Action.MENU_CONFIRM)) {
            if (done) {
                return new TitleScreen(services);
            }
            elapsed = rows.size() * LINE_SECONDS + STAMP_DELAY_SECONDS;
        } else {
            elapsed += seconds;
        }
        int lines = Math.min(rows.size(), (int) (elapsed / LINE_SECONDS) + 1);
        if (lines > shownLines) {
            shownLines = lines;
            boolean total = rows.get(lines - 1).label().equals("TOTAL CREDITS");
            services.sfx.play(total ? Sfx.TALLY_TOTAL : Sfx.TALLY_TICK, TICK_VOLUME, 1, 0);
        }
        if (!stamped && elapsed >= rows.size() * LINE_SECONDS + STAMP_DELAY_SECONDS) {
            stamped = true;
            services.sfx.play(Sfx.GRADE_STAMP, 1, 1, 0);
        }
        return this;
    }

    @Override
    public void draw(SpriteBatch batch) {
        BitmapFont font = services.font;
        fill(batch, Color.BLACK, 0, 0, PixelScreen.WIDTH, PixelScreen.HEIGHT);
        fill(batch, GLASS, 120, 30, 720, 480);
        fill(batch, RULE, 120, 509, 720, 1);
        fill(batch, RULE, 120, 30, 720, 1);
        text(font, batch, title, TITLE, 0, 500, PixelScreen.WIDTH, Align.center);
        text(font, batch, subtitle, VALUE, 0, 482, PixelScreen.WIDTH, Align.center);
        for (int i = 0; i < shownLines; i++) {
            Row row = rows.get(i);
            float y = TOP - 10 - i * LINE;
            double progress = Math.clamp((elapsed - i * LINE_SECONDS) / LINE_SECONDS, 0, 1);
            if (row.colour() == HEADING) {
                text(font, batch, row.label(), HEADING, LEFT, y, 200, Align.left);
                fill(batch, RULE, LEFT + 80, y - 8, 520, 1);
                continue;
            }
            text(font, batch, row.label(), LABEL, LEFT, y, 220, Align.left);
            text(font, batch, row.middle(), VALUE, LEFT + 230, y, 200, Align.left);
            text(font, batch, row.right(progress), row.colour(), RIGHT - 200, y, 200, Align.right);
        }
        if (stamped) {
            drawStamp(batch, font);
            text(font, batch, "[ENTER] CONTINUE", TITLE, 560, 60, 260, Align.right);
        }
    }

    private void drawStamp(SpriteBatch batch, BitmapFont font) {
        int x = 690;
        int y = 120;
        fill(batch, TITLE, x, y, 110, 110);
        fill(batch, GLASS, x + 6, y + 6, 98, 98);
        font.getData().setScale(4);
        text(font, batch, grade, TITLE, x, y + 86, 110, Align.center);
        font.getData().setScale(1);
        text(font, batch, "GRADE", LABEL, x, y - 8, 110, Align.center);
    }

    private static void text(
            BitmapFont font, SpriteBatch batch, String text, Color colour, float x, float y, float width, int align) {
        font.setColor(colour);
        font.draw(batch, text, x, y, width, align, false);
        font.setColor(Color.WHITE);
    }

    private void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(services.sprites.pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    @Override
    public void dispose() {}
}
