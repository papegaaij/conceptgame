package vanguard.game.render;

import java.util.List;

/**
 * The fixed layout of the left HUD panel (design/ui/hud, Left panel layout): its regions stacked
 * from the top with one gap between them, each as tall as the most it can show, so a region never
 * grows into the next one whatever it shows. Offsets are px down from the panel's top edge, as in
 * the design document; {@link Region} turns them into the batch's y-up coordinates.
 */
final class MissionLayout {
    /** libGDX's built-in 15 px font, the placeholder until the UI kit's fonts exist, sets lines 18 px apart. */
    static final int LINE = 18;
    /** Between two regions. */
    static final int GAP = 6;
    /** From the panel's top edge to the first region, and from the last one to the bottom edge. */
    static final int MARGIN = 16;
    /** A label plate. */
    static final int PLATE = 22;
    /** The dark frame around an LCD well or a bar. */
    static final int FRAME = 2;
    /** Text keeps this far from the left and right sides of its well. */
    static final int PAD = 6;
    /** The top of a line's capitals lies this far below the top of its well, centring one line in a {@link #WELL}. */
    static final int TEXT_DROP = 6;
    /** The width text has in a well. */
    static final int TEXT_WIDTH = HudKit.INNER_WIDTH - 2 * PAD;
    /** An LCD well for one line of text. */
    static final int WELL = LINE + 6;
    /** An LCD well for three lines: a radio page, or the control prompts. */
    static final int PAGE_WELL = 3 * LINE + 6;

    static final int PORTRAIT = 72;
    /** Between the portrait and the speaker's name. */
    static final int NAME_GAP = 10;
    /** The width the speaker's name has beside the portrait, one word per line. */
    static final int NAME_WIDTH = HudKit.INNER_WIDTH - PORTRAIT - NAME_GAP;
    /** The most control prompts shown at once: one line each. */
    static final int PROMPT_LINES = 3;
    /** A prompt's action is left-aligned in this column, its keys right-aligned in the rest of the line. */
    static final int PROMPT_ACTION_WIDTH = 88;

    static final int CHAIN_BAR = 6;
    static final int PROGRESS_BAR = 10;

    static final Region MISSION = new Region(MARGIN, PLATE + 4 + LINE);
    static final Region SCORE = below(MISSION, block(framed(WELL)));
    static final Region CREDITS = below(SCORE, block(framed(WELL)));
    static final Region CHAIN = below(CREDITS, LINE + framed(CHAIN_BAR));
    /** Label, portrait with the speaker's name beside it, and the subtitle page below. */
    static final Region RADIO = below(CHAIN, block(framed(PORTRAIT)) + 4 + framed(PAGE_WELL));

    static final Region PROMPTS = below(RADIO, framed(PAGE_WELL));
    static final Region OBJECTIVE = below(PROMPTS, framed(WELL));
    static final Region PROGRESS = below(OBJECTIVE, block(framed(PROGRESS_BAR)));

    static final List<Region> REGIONS = List.of(MISSION, SCORE, CREDITS, CHAIN, RADIO, PROMPTS, OBJECTIVE, PROGRESS);

    private MissionLayout() {}

    /** A region {@code top} px below the panel's top edge, {@code height} px tall. */
    record Region(int top, int height) {
        int bottom() {
            return top + height;
        }

        /** The batch's y of the region's top edge. */
        int yTop() {
            return PixelScreen.HEIGHT - top;
        }

        /** The batch's y of the region's bottom edge. */
        int yBottom() {
            return PixelScreen.HEIGHT - bottom();
        }
    }

    private static Region below(Region above, int height) {
        return new Region(above.bottom() + GAP, height);
    }

    /** A label plate over something {@code framed} px tall, whose frame touches the plate. */
    private static int block(int framed) {
        return PLATE + framed;
    }

    private static int framed(int height) {
        return height + 2 * FRAME;
    }
}
