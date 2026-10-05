package vanguard.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vanguard.content.campaign.Campaign;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.input.MenuInput;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;
import vanguard.sim.LevelResult;

/**
 * The debrief after a won level (design/ui/debrief), in the glass style of the chosen debrief
 * concept (debrief-r08-a), drawn with the UI kit over the dimmed title scene (Earth orbit, Level
 * 01's setting): the tally, the credits by source with the grade bonus and the total, the score and
 * the grade stamp in its amber-trimmed glass, with "NEW BEST" when the grade beats the level's
 * best, and above it the grade's breakdown (each rating part's points, the rating and the next
 * grade's threshold). Lines appear 0.3 s apart with a tick while their numbers count up; confirm (or Back) skips the
 * animation, and once it is done the campaign goes on with the next level's briefing, or the
 * hangar while that level is not built yet. A replay's debrief shows no credits, since a replay
 * banks none, and goes back to the mission select (design/ui/mission-select).
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
    /** The title scene behind the panel is drawn this bright. */
    private static final float DIMMED = 0.35f;

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
    private final Campaign campaign;
    private final boolean newBest;
    private final String title;
    private final String subtitle;
    private final List<Row> rows = new ArrayList<>();
    private final String grade;
    private final LevelResult.Rating rating;
    private float elapsed;
    private int shownLines;
    private boolean stamped;

    /**
     * @param number the level number
     * @param name the level's name
     * @param launchBalance the credits the player launched with
     * @param newBest whether the grade is a new best for the level
     */
    public DebriefScreen(
            GameServices services,
            Campaign campaign,
            LevelResult result,
            int number,
            String name,
            int launchBalance,
            boolean newBest) {
        this.services = services;
        this.campaign = campaign;
        this.newBest = newBest;
        title = String.format(Locale.ROOT, "MISSION %02d COMPLETE", number);
        subtitle = name.toUpperCase(Locale.ROOT);
        grade = result.grade().letter();
        rating = result.rating();
        addTally(result);
        if (campaign.replay().isPresent()) {
            addReplay(result, launchBalance);
        } else {
            addCredits(result, launchBalance);
        }
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
        LevelResult.Escort escort = result.escort();
        if (escort.present()) {
            // The escort objective's own row: "CRAWLERS HOME 4 / 5 + 120 CR" (design/campaign, Level 04).
            rows.add(new Row(
                    alliesLabel(escort.ally()) + " HOME",
                    escort.home() + " / " + escort.units(),
                    escort.credits(),
                    "+ ",
                    " CR",
                    CREDITS));
        }
        rows.add(new Row(
                "SECONDARY OBJECTIVE",
                result.secondaryMet() ? "MET" : "MISSED",
                result.secondaryMet() ? result.credits().objectives() - escort.credits() : -1,
                "+ ",
                " CR",
                CREDITS));
    }

    /** {@code civilian-crawler} reads "CRAWLERS". */
    static String alliesLabel(String slug) {
        return slug.substring(slug.lastIndexOf('-') + 1).toUpperCase(Locale.ROOT) + "S";
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

    /** A replay banks nothing (design/ui/mission-select): the balance stays, the score is shown. */
    private void addReplay(LevelResult result, int launchBalance) {
        rows.add(Row.heading("REPLAY"));
        rows.add(new Row("NO CREDITS BANKED", "", -1, "", "", LABEL));
        rows.add(new Row("BALANCE", "", launchBalance, "", "", CREDITS));
        rows.add(new Row("SCORE", "", result.score(), "", "", GAIN));
    }

    private static long bonus(LevelResult result, String name) {
        return result.bonuses().stream()
                .filter(b -> b.name().equals(name))
                .mapToLong(LevelResult.BonusScore::score)
                .findFirst()
                .orElse(-1);
    }

    /**
     * Whether the frame's input goes on (skips the count-up, then leaves): confirm, and Back (Esc,
     * the gamepad's back button) too, since the debrief has nothing to go back to.
     */
    static boolean goesOn(MenuInput input) {
        return input.confirm() || input.back();
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        boolean done = stamped;
        if (goesOn(services.menu)) {
            if (done) {
                return Transition.replace(
                        campaign.replay().isPresent()
                                ? MissionSelectScreen.afterReplay(services)
                                : HangarScreen.beforeNextLevel(services, campaign));
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
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Fonts fonts = services.fonts;
        BitmapFont font = fonts.body;
        Glass glass = services.glass;
        services.titleScene.draw(batch, DIMMED);
        glass.panel(batch, 120, 30, 720, 480, 0.9f);
        text(fonts.heading, batch, title, TITLE, 0, 500, PixelScreen.WIDTH, Align.center);
        text(font, batch, subtitle, VALUE, 0, 468, PixelScreen.WIDTH, Align.center);
        for (int i = 0; i < shownLines; i++) {
            Row row = rows.get(i);
            float y = TOP - 20 - i * LINE;
            double progress = Math.clamp((elapsed - i * LINE_SECONDS) / LINE_SECONDS, 0, 1);
            if (row.colour() == HEADING) {
                text(font, batch, row.label(), HEADING, LEFT, y, 200, Align.left);
                glass.rule(batch, LEFT + 80, RIGHT, PixelScreen.HEIGHT - y + 6);
                continue;
            }
            text(font, batch, row.label(), LABEL, LEFT, y, 220, Align.left);
            text(font, batch, row.middle(), VALUE, LEFT + 230, y, 200, Align.left);
            text(font, batch, row.right(progress), row.colour(), RIGHT - 200, y, 200, Align.right);
        }
        if (stamped) {
            drawRating(batch, fonts);
            drawStamp(batch, fonts);
            text(font, batch, "[ENTER] CONTINUE", TITLE, 560, 60, 260, Align.right);
        }
    }

    /**
     * The grade's breakdown above the stamp: each part's points of its weight, the rating and the
     * next grade's threshold (the top grade's own once it is reached).
     */
    private void drawRating(SpriteBatch batch, Fonts fonts) {
        int x = 670;
        int right = 820;
        int y = TOP - 20;
        Glass glass = services.glass;
        text(fonts.body, batch, "RATING", HEADING, x, y, 100, Align.left);
        glass.rule(batch, x + 66, right, PixelScreen.HEIGHT - y + 6);
        String[] labels = {"KILLS", "ARMOUR", "SECRETS", "CHAIN"};
        LevelResult.Rating.Part[] parts = {rating.kills(), rating.armour(), rating.secrets(), rating.chain()};
        for (int i = 0; i < parts.length; i++) {
            float row = y - (i + 1) * LINE;
            text(fonts.label, batch, labels[i], LABEL, x, row - 4, 80, Align.left);
            text(
                    fonts.body,
                    batch,
                    ratingPart(parts[i]),
                    full(parts[i]) ? GAIN : VALUE,
                    right - 90,
                    row,
                    90,
                    Align.right);
        }
        float total = y - 5 * LINE - 10;
        glass.rule(batch, x, right, PixelScreen.HEIGHT - (y - 4 * LINE - 23));
        text(fonts.label, batch, "RATING", LABEL, x, total - 4, 80, Align.left);
        text(fonts.body, batch, ratingTotal(rating), TITLE, right - 90, total, 90, Align.right);
        text(fonts.label, batch, ratingTarget(rating), LABEL, x, total - LINE - 2, right - x, Align.right);
    }

    /** A part's points of its weight, "18 / 30". */
    static String ratingPart(LevelResult.Rating.Part part) {
        return Math.round(part.points()) + " / " + Math.round(part.most());
    }

    private static boolean full(LevelResult.Rating.Part part) {
        return part.points() >= part.most() - 1e-9;
    }

    /** The rating, rounded down so that it never shows a threshold the grade did not reach. */
    static String ratingTotal(LevelResult.Rating rating) {
        return Integer.toString((int) Math.floor(rating.total() + 1e-9));
    }

    /** The next grade's threshold, "A+ AT 85". */
    static String ratingTarget(LevelResult.Rating rating) {
        return rating.target().letter() + " AT " + Math.round(rating.target().minRating());
    }

    private void drawStamp(SpriteBatch batch, Fonts fonts) {
        int x = 690;
        int y = 120;
        services.glass.panel(batch, x, PixelScreen.HEIGHT - y - 110, 110, 110, 0.9f, true);
        BitmapFont heading = fonts.heading;
        heading.getData().setScale(3);
        if (grade.length() == 1) {
            text(heading, batch, grade, TITLE, x, y + 95, 110, Align.center);
        } else {
            // "A+": the letter at the stamp's size and its plus raised beside it at two thirds, centred
            // as a pair (two full-size glyphs of the 20 px heading font would overrun the 110 px glass).
            text(heading, batch, grade.substring(0, 1), TITLE, x + 12, y + 95, 60, Align.left);
            heading.getData().setScale(2);
            text(heading, batch, grade.substring(1), TITLE, x + 61, y + 98, 40, Align.left);
        }
        heading.getData().setScale(1);
        text(fonts.label, batch, "GRADE", LABEL, x, y - 8, 110, Align.center);
        if (newBest) {
            text(fonts.body, batch, "NEW BEST", GAIN, x, y + 134, 110, Align.center);
        }
    }

    private static void text(
            BitmapFont font, SpriteBatch batch, String text, Color colour, float x, float y, float width, int align) {
        font.setColor(colour);
        font.draw(batch, text, x, y, width, align, false);
        font.setColor(Color.WHITE);
    }

    private static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    /** Skipping the debrief must not leave the jingle playing over the next screen's music. */
    @Override
    public void dispose() {
        services.sfx.stop(Sfx.MISSION_COMPLETE);
    }
}
