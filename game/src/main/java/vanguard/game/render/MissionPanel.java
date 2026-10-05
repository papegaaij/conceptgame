package vanguard.game.render;

import static vanguard.game.render.MissionLayout.FRAME;
import static vanguard.game.render.MissionLayout.LINE;
import static vanguard.game.render.MissionLayout.PAD;
import static vanguard.game.render.MissionLayout.PAGE_WELL;
import static vanguard.game.render.MissionLayout.PLATE;
import static vanguard.game.render.MissionLayout.PORTRAIT;
import static vanguard.game.render.MissionLayout.TEXT_DROP;
import static vanguard.game.render.MissionLayout.TEXT_WIDTH;
import static vanguard.game.render.MissionLayout.WELL;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.sim.Ally;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * The left HUD panel (design/ui/hud, mission): mission number and name, score, credits (the
 * launch balance plus what the level has earned), the chain with its draining window, the radio
 * with the speaker's portrait and the typed subtitle, the control prompts, the objective tracker
 * for the secondary objective and the level progress, each in its region of the
 * {@link MissionLayout}; text is cut off at the end of its well rather than run over it. A level
 * with a convoy (an escort primary objective) has a two-line tracker: the convoy's pips over the
 * secondary objective, in a well that takes a line from the control prompts'.
 */
final class MissionPanel {
    private static final int X = HudKit.INSET;
    private static final int WIDTH = HudKit.INNER_WIDTH;
    private static final int GROUP_PIP_STEP = 16;
    private static final Color LOST = Color.valueOf("FF4400");
    private static final Color GROUP_OPEN = Color.valueOf("3A4060");
    /** The group objective's label: its groups' common first word in plural ("DOCKS"). */
    private String groupsLabel;
    /** The groups' states as last drawn, and the frame each last changed in. */
    private int[] groupStates = new int[0];

    private int[] groupChanged = new int[0];
    /** The step of a convoy unit's pip. */
    static final int ALLY_PIP_STEP = 14;
    /** A convoy pip flashes white this many simulation steps after a hit. */
    private static final int ALLY_HIT_TICKS = 6;
    /** The pip sprite's width (civilian-crawler-pip: 10x18, a line tall). */
    static final int ALLY_PIP_WIDTH = 10;

    private static final Color ALLY_DARK = Color.valueOf("3A4060");
    /** The convoy's label ("CRAWLERS") and pip, once known. */
    private String alliesLabel;

    private AtlasRegion allyPip;
    /** The frame each convoy unit was first drawn lost in; -1 while alive. */
    private int[] allyLost = new int[0];

    private int primaryFailedFrame = -1;

    /** The tracker flashes this many frames after the objective is met. */
    private static final int FLASH_FRAMES = 90;

    private static final Color CHAIN = Color.valueOf("FFB000");
    private static final Color CHAIN_EMPTY = Color.valueOf("2A1C00");
    private static final Color PROGRESS = Color.valueOf("8AD0FF");
    private static final Color PROGRESS_EMPTY = Color.valueOf("102030");
    private static final Color SUCCESS = Color.valueOf("40FF80");
    private static final Color CHOIR = Color.valueOf("C890FF");

    private final HudKit kit;
    private final GlyphLayout measure = new GlyphLayout();
    private final Sprites sprites;
    private final TransmissionStatic transmissionStatic;
    private final String mission;
    private final String name;
    private final int launchBalance;
    private int frame;
    private int metFrame = -1;
    private int failedFrame = -1;
    /** An escapes objective's label: its enemy's last word in plural ("BOMBERS"). */
    private String escapesLabel;
    /** The radio message whose portrait frames {@link #portrait} holds. */
    private RadioQueue.Message shownMessage;

    private Array<AtlasRegion> portrait;

    /**
     * @param number the level number
     * @param name the level's name
     * @param launchBalance credits at launch
     */
    MissionPanel(
            HudKit kit,
            Sprites sprites,
            TransmissionStatic transmissionStatic,
            int number,
            String name,
            int launchBalance) {
        this.kit = kit;
        this.sprites = sprites;
        this.transmissionStatic = transmissionStatic;
        this.mission = String.format(Locale.ROOT, "MISSION %02d", number);
        this.name = name.toUpperCase(Locale.ROOT);
        this.launchBalance = launchBalance;
    }

    void draw(SpriteBatch batch, Sortie sortie, RadioQueue radio, List<PromptTexts.Text> prompts) {
        frame++;
        if (groupsLabel == null) {
            groupsLabel = groupsLabel(sortie.script().groups());
            LevelScript.Secondary secondary = sortie.script().secondary();
            escapesLabel = secondary.label().isEmpty() ? escapesLabel(secondary.escapes()) : secondary.label();
            sortie.script().escort().ifPresent(escort -> {
                alliesLabel = escapesLabel(escort.ally().slug());
                String pip = escort.ally().slug() + "-pip";
                allyPip = sprites.has(pip) ? sprites.region(pip) : null;
            });
        }
        boolean targets = !sortie.script().targets().isEmpty();
        boolean two = sortie.allyCount() > 0 || targets;
        kit.leftPanel(batch);
        int top = MissionLayout.MISSION.yTop();
        plate(batch, mission, top);
        kit.text(batch, kit.body, name, HudKit.LABEL, X, top - PLATE - 6, WIDTH);

        readout(batch, "SCORE", MissionLayout.SCORE, grouped(sortie.score()), HudKit.READOUT);
        readout(batch, "CREDITS", MissionLayout.CREDITS, grouped(launchBalance + sortie.credits()), HudKit.AMBER);

        drawChain(batch, sortie);
        drawRadio(batch, radio);
        drawPrompts(batch, prompts, two);
        if (targets) {
            drawTargetsTracker(batch, sortie);
        } else if (two) {
            drawTwoTrackers(batch, sortie);
        } else {
            drawTracker(batch, sortie);
        }

        MissionLayout.Region progress = MissionLayout.PROGRESS;
        plate(batch, "PROGRESS", progress.yTop());
        kit.bar(
                batch,
                PROGRESS,
                PROGRESS_EMPTY,
                X,
                progress.yBottom() + FRAME,
                WIDTH,
                MissionLayout.PROGRESS_BAR,
                sortie.levelSeconds() / sortie.script().seconds());
    }

    /** A label plate whose top is at {@code top}. */
    private void plate(SpriteBatch batch, String label, int top) {
        kit.label(batch, label, X, top - 4);
    }

    /** An LCD well of {@code height} inside a frame whose top is at {@code top}; returns the well's top. */
    private int well(SpriteBatch batch, int top, int height) {
        int wellTop = top - FRAME;
        kit.lcd(batch, X, wellTop - height, WIDTH, height);
        return wellTop;
    }

    private void readout(SpriteBatch batch, String label, MissionLayout.Region region, String value, Color colour) {
        plate(batch, label, region.yTop());
        int wellTop = well(batch, region.yTop() - PLATE, WELL);
        kit.glow(batch, colour, X, wellTop - WELL, WIDTH, WELL);
        kit.textRight(batch, kit.body, value, colour, X + PAD, wellTop - TEXT_DROP, TEXT_WIDTH);
    }

    private void drawChain(SpriteBatch batch, Sortie sortie) {
        MissionLayout.Region region = MissionLayout.CHAIN;
        int y = region.yTop() - 2;
        kit.text(batch, kit.body, "CHAIN " + sortie.chain(), HudKit.LABEL, X, y, WIDTH);
        String multiplier = String.format(Locale.ROOT, "x%.1f", sortie.chainMultiplier());
        kit.textRight(batch, kit.body, multiplier, HudKit.LABEL, X, y, WIDTH);
        kit.bar(
                batch,
                CHAIN,
                CHAIN_EMPTY,
                X,
                region.yBottom() + FRAME,
                WIDTH,
                MissionLayout.CHAIN_BAR,
                sortie.chainWindow());
    }

    private void drawRadio(SpriteBatch batch, RadioQueue radio) {
        int top = MissionLayout.RADIO.yTop();
        plate(batch, "RADIO", top);
        int portraitTop = top - PLATE - FRAME;
        int portraitY = portraitTop - PORTRAIT;
        kit.portraitWell(batch, X, portraitY, PORTRAIT);
        int subtitleTop = well(batch, portraitY - FRAME - 4, PAGE_WELL);
        if (radio.current().isEmpty()) {
            return;
        }
        RadioQueue.Message message = radio.current().get();
        if (message != shownMessage) {
            shownMessage = message;
            portrait = Portraits.radio(sprites, message.portrait(), message.expression());
        }
        batch.draw(Portraits.frame(portrait, radio.sinceOpened()), X, portraitY);
        float noise = TransmissionStatic.strength(radio.sinceOpened(), radio.untilClosed());
        transmissionStatic.draw(batch, X, portraitY, PORTRAIT, PORTRAIT, noise);
        Color colour = message.distorted() ? CHOIR : HudKit.AMBER;
        String[] names = message.speaker().toUpperCase(Locale.ROOT).split(" ", 2);
        int nameX = X + PORTRAIT + MissionLayout.NAME_GAP;
        for (int i = 0; i < names.length; i++) {
            kit.text(batch, kit.body, names[i], colour, nameX, portraitTop - 2 - i * LINE, MissionLayout.NAME_WIDTH);
        }
        List<String> lines = radio.visibleLines();
        Color subtitle = message.distorted() ? CHOIR : HudKit.READOUT;
        kit.glow(batch, subtitle, X, subtitleTop - PAGE_WELL, WIDTH, PAGE_WELL);
        for (int i = 0; i < lines.size(); i++) {
            // A distorted transmission jitters a pixel now and then.
            float jitter = message.distorted() && (frame / 3 + i) % 7 == 0 ? 1 : 0;
            kit.text(
                    batch,
                    kit.small,
                    lines.get(i),
                    subtitle,
                    X + PAD + jitter,
                    subtitleTop - TEXT_DROP - i * LINE,
                    TEXT_WIDTH - jitter);
        }
    }

    private void drawPrompts(SpriteBatch batch, List<PromptTexts.Text> prompts, boolean two) {
        if (prompts.isEmpty()) {
            return;
        }
        int wellTop = two
                ? well(batch, MissionLayout.TWO_PROMPTS.yTop(), MissionLayout.TWO_LINE_WELL)
                : well(batch, MissionLayout.PROMPTS.yTop(), PAGE_WELL);
        int shown = two ? MissionLayout.TWO_PROMPT_LINES : MissionLayout.PROMPT_LINES;
        for (int i = 0; i < Math.min(prompts.size(), shown); i++) {
            PromptTexts.Text prompt = prompts.get(i);
            int y = wellTop - TEXT_DROP - i * LINE;
            // The keys take the rest of the line after the action ("LOW-AIR" leaves room for "BELOW YOU: FIRE").
            measure.setText(kit.small, prompt.action());
            int action = (int) Math.ceil(Math.min(measure.width, MissionLayout.PROMPT_ACTION_WIDTH));
            int keysX = X + PAD + action + MissionLayout.PROMPT_GAP;
            int keysWidth = TEXT_WIDTH - action - MissionLayout.PROMPT_GAP;
            kit.text(batch, kit.small, prompt.action(), HudKit.LABEL, X + PAD, y, MissionLayout.PROMPT_ACTION_WIDTH);
            kit.textRight(batch, kit.small, prompt.keys(), HudKit.READOUT, keysX, y, keysWidth);
        }
    }

    private void drawTracker(SpriteBatch batch, Sortie sortie) {
        int wellTop = well(batch, MissionLayout.OBJECTIVE.yTop(), WELL);
        drawSecondary(batch, sortie, wellTop, WELL);
    }

    /**
     * The two-line tracker (design/ui/hud, Level 04): the convoy's label and a pip per unit (green;
     * amber below half its HP; white on a hit; a red flash, then dark, when lost), the line flashing
     * red when the last one is lost; the secondary objective on the second line.
     */
    private void drawTwoTrackers(SpriteBatch batch, Sortie sortie) {
        int wellTop = well(batch, MissionLayout.TWO_OBJECTIVES.yTop(), MissionLayout.TWO_LINE_WELL);
        // Line one is a line tall at the top; line two takes the rest, as a one-line well's text does.
        if (sortie.primaryFailed() && primaryFailedFrame < 0) {
            primaryFailedFrame = frame;
        } else if (!sortie.primaryFailed()) {
            primaryFailedFrame = -1;
        }
        boolean failFlash = primaryFailedFrame >= 0
                && frame - primaryFailedFrame < FLASH_FRAMES
                && (frame - primaryFailedFrame) / 8 % 2 == 0;
        if (failFlash) {
            kit.fill(batch, LOST, X, wellTop - LINE, WIDTH, LINE);
        }
        Color colour = failFlash ? HudKit.LCD : sortie.primaryFailed() ? LOST : HudKit.LABEL;
        int y = wellTop - TEXT_DROP;
        kit.text(batch, kit.body, alliesLabel, colour, X + PAD, y, TEXT_WIDTH);
        int count = sortie.allyCount();
        if (allyLost.length != count) {
            allyLost = new int[count];
            Arrays.fill(allyLost, -1);
        }
        int pipX = X + PAD + TEXT_WIDTH - count * ALLY_PIP_STEP + ALLY_PIP_STEP - ALLY_PIP_WIDTH;
        for (int k = 0; k < count; k++) {
            Ally ally = sortie.ally(k);
            Color pip;
            if (ally.alive()) {
                allyLost[k] = -1;
                pip = ally.ticksSinceHit() < ALLY_HIT_TICKS
                        ? Color.WHITE
                        : ally.hpShare() < 0.5 ? HudKit.AMBER : SUCCESS;
            } else {
                if (allyLost[k] < 0) {
                    allyLost[k] = frame;
                }
                int since = frame - allyLost[k];
                pip = since < FLASH_FRAMES && since / 8 % 2 == 0 ? LOST : ALLY_DARK;
            }
            int left = pipX + k * ALLY_PIP_STEP;
            int bottom = y - LINE + 3;
            if (allyPip != null) {
                batch.setColor(pip);
                batch.draw(allyPip, left, bottom);
                batch.setColor(Color.WHITE);
            } else {
                kit.fill(batch, pip, left, bottom, ALLY_PIP_WIDTH, LINE);
            }
        }
        drawSecondary(batch, sortie, wellTop - LINE, MissionLayout.TWO_LINE_WELL - LINE);
    }

    /**
     * The two-line tracker of a destroy-targets primary (design/ui/hud, Level 05): its label
     * ({@code BATTERIES}) and each group's letter, dim while open, struck through in green once
     * cleared, red when lost (the line flashing red as the primary fails); the secondary below.
     */
    private void drawTargetsTracker(SpriteBatch batch, Sortie sortie) {
        int wellTop = well(batch, MissionLayout.TWO_OBJECTIVES.yTop(), MissionLayout.TWO_LINE_WELL);
        if (sortie.primaryFailed() && primaryFailedFrame < 0) {
            primaryFailedFrame = frame;
        } else if (!sortie.primaryFailed()) {
            primaryFailedFrame = -1;
        }
        boolean failFlash = primaryFailedFrame >= 0
                && frame - primaryFailedFrame < FLASH_FRAMES
                && (frame - primaryFailedFrame) / 8 % 2 == 0;
        if (failFlash) {
            kit.fill(batch, LOST, X, wellTop - LINE, WIDTH, LINE);
        }
        Color colour = failFlash ? HudKit.LCD : sortie.primaryFailed() ? LOST : HudKit.LABEL;
        int y = wellTop - TEXT_DROP;
        kit.text(batch, kit.body, groupsLabel, colour, X + PAD, y, TEXT_WIDTH);
        List<String> groups = sortie.script().groups();
        int count = groups.size();
        if (groupStates.length != count) {
            groupStates = new int[count];
            groupChanged = new int[count];
        }
        int letterX = X + PAD + TEXT_WIDTH - count * GROUP_PIP_STEP;
        for (int g = 0; g < count; g++) {
            int state = sortie.groupState(g);
            if (state != groupStates[g]) {
                groupStates[g] = state;
                groupChanged[g] = frame;
            }
            int since = frame - groupChanged[g];
            boolean blink = state != 0 && since < FLASH_FRAMES && since / 8 % 2 == 1;
            Color letter =
                    switch (state) {
                        case 1 -> SUCCESS;
                        case 2 -> LOST;
                        default -> HudKit.LABEL;
                    };
            if (failFlash) {
                letter = HudKit.LCD;
            } else if (blink) {
                letter = GROUP_OPEN;
            }
            String name = groups.get(g);
            String mark = name.substring(name.lastIndexOf(' ') + 1);
            int left = letterX + g * GROUP_PIP_STEP;
            kit.text(batch, kit.body, mark, letter, left, y, GROUP_PIP_STEP);
            if (state == 1) {
                kit.fill(batch, letter, left - 1, y - LINE / 2 - 1, GROUP_PIP_STEP - 4, 2);
            }
        }
        drawSecondary(batch, sortie, wellTop - LINE, MissionLayout.TWO_LINE_WELL - LINE);
    }

    /** The secondary objective's line in a well (or the lower half of one) whose top is at {@code top}. */
    private void drawSecondary(SpriteBatch batch, Sortie sortie, int top, int height) {
        int wellTop = top;
        if (sortie.secondaryMet() && metFrame < 0) {
            metFrame = frame;
        } else if (!sortie.secondaryMet()) {
            metFrame = -1;
        }
        if (sortie.secondaryFailed() && failedFrame < 0) {
            failedFrame = frame;
        } else if (!sortie.secondaryFailed()) {
            failedFrame = -1;
        }
        // The whole box flashes when the objective is won (green) or lost (red).
        boolean flash = metFrame >= 0 && frame - metFrame < FLASH_FRAMES && (frame - metFrame) / 8 % 2 == 0;
        boolean failFlash =
                failedFrame >= 0 && frame - failedFrame < FLASH_FRAMES && (frame - failedFrame) / 8 % 2 == 0;
        if (flash || failFlash) {
            kit.fill(batch, flash ? SUCCESS : LOST, X, wellTop - height, WIDTH, height);
        }
        Color colour = flash || failFlash
                ? HudKit.LCD
                : sortie.secondaryMet() ? SUCCESS : sortie.secondaryFailed() ? LOST : HudKit.LABEL;
        int y = wellTop - TEXT_DROP;
        if (sortie.script().secondary().byGroups()) {
            drawGroups(batch, sortie, colour, y);
            return;
        }
        if (sortie.secondaryByEscapes()) {
            // "Nothing gets through" (Level 03): the escapers destroyed of all the level sends.
            kit.text(batch, kit.body, escapesLabel, colour, X + PAD, y, TEXT_WIDTH);
            String count = sortie.secondaryMet()
                    ? "DONE"
                    : sortie.secondaryFailed() ? "FAILED" : sortie.escapesDestroyed() + " / " + sortie.escapesTotal();
            kit.textRight(batch, kit.body, count, colour, X + PAD, y, TEXT_WIDTH);
            return;
        }
        // A kill ratio is judged at the level's end: the count runs on past the share, DONE only then.
        kit.text(batch, kit.body, "KILLS", colour, X + PAD, y, TEXT_WIDTH);
        String count = sortie.secondaryMet() ? "DONE" : sortie.kills() + " / " + sortie.requiredKills();
        kit.textRight(batch, kit.body, count, colour, X + PAD, y, TEXT_WIDTH);
    }

    /**
     * A group objective (Level 02's docks): its name and a pip per group, dim while open, green
     * when cleared, red when lost.
     */
    private void drawGroups(SpriteBatch batch, Sortie sortie, Color colour, int y) {
        kit.text(batch, kit.body, groupsLabel, colour, X + PAD, y, TEXT_WIDTH);
        int count = sortie.groupCount();
        if (groupStates.length != count) {
            groupStates = new int[count];
            groupChanged = new int[count];
        }
        int pipX = X + PAD + TEXT_WIDTH - count * GROUP_PIP_STEP;
        for (int g = 0; g < count; g++) {
            int state = sortie.groupState(g);
            if (state != groupStates[g]) {
                groupStates[g] = state;
                groupChanged[g] = frame;
            }
            // A pip flashes as its group is cleared or lost (design/ui/hud, objective tracker).
            int since = frame - groupChanged[g];
            boolean blink = state != 0 && since < FLASH_FRAMES && since / 8 % 2 == 1;
            Color pip =
                    switch (state) {
                        case 1 -> SUCCESS;
                        case 2 -> LOST;
                        default -> GROUP_OPEN;
                    };
            kit.fill(batch, blink ? HudKit.LCD : pip, pipX + g * GROUP_PIP_STEP, y - 14, GROUP_PIP_STEP - 4, 10);
        }
    }

    /** "Dock One", "Dock Two" ... reads "DOCKS"; "Battery A" ... "BATTERIES". */
    static String groupsLabel(List<String> groups) {
        if (groups.isEmpty()) {
            return "";
        }
        String first = groups.getFirst();
        int space = first.indexOf(' ');
        String noun = (space > 0 ? first.substring(0, space) : first).toUpperCase(Locale.ROOT);
        return noun.endsWith("Y") ? noun.substring(0, noun.length() - 1) + "IES" : noun + "S";
    }

    /** {@code spore-bomber} reads "BOMBERS", {@code mantis} "MANTISES"; empty for none. */
    static String escapesLabel(String slug) {
        if (slug.isEmpty()) {
            return "";
        }
        String noun = slug.substring(slug.lastIndexOf('-') + 1).toUpperCase(Locale.ROOT);
        return noun + (noun.endsWith("S") ? "ES" : "S");
    }

    /** A number with thin-space thousands groups, as on the HUD mock: 1 204 350. */
    static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
