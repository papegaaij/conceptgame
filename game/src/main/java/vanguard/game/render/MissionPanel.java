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
 * secondary objective, in a well that takes a line from the control prompts'. M5 part E: a naval
 * convoy outside the objectives (Level 11) has a one-line tracker for its afloat secondary,
 * {@code CONVOY} with a pip per cargo ship ({@link #drawConvoyTracker}).
 */
final class MissionPanel {
    private static final int X = HudKit.INSET;
    private static final int WIDTH = HudKit.INNER_WIDTH;
    static final int GROUP_PIP_STEP = 16;
    /**
     * M5 part C: a named target's mark wider than a letter (Level 09's {@code A1} … {@code C2}) takes
     * its width plus this gap per step, right-aligned in the line.
     */
    static final int MARK_GAP = 4;

    private static final Color LOST = Color.valueOf("FF4400");
    private static final Color GROUP_OPEN = Color.valueOf("3A4060");
    /** The group objective's label: its groups' common first word in plural ("DOCKS"). */
    private String groupsLabel;
    /** The step of the targets tracker's marks, once measured (Level 05's letters 16 px, Level 09's pairs wider). */
    private int markStep;
    /** The groups' states as last drawn, and the frame each last changed in. */
    private int[] groupStates = new int[0];

    private int[] groupChanged = new int[0];
    /** The step of a convoy unit's pip. */
    static final int ALLY_PIP_STEP = 14;
    /** A convoy pip flashes white this many simulation steps after a hit. */
    private static final int ALLY_HIT_TICKS = 6;
    /** The pip sprite's width (civilian-crawler-pip: 10x18, a line tall). */
    static final int ALLY_PIP_WIDTH = 10;
    /**
     * M5 part D: an air escort unit's armour bar (design/ui/hud, Level 10: "about 32 px wide"), five
     * of them filling the well's text width ({@code 4 × 41 + 32 = 196}).
     */
    static final int ALLY_BAR_WIDTH = 32;

    static final int ALLY_BAR_STEP = 41;
    static final int ALLY_BAR_HEIGHT = 8;

    private static final Color ALLY_DARK = Color.valueOf("3A4060");
    /**
     * 2026-10-08: a unit that climbed out home: its bar full in a pale mint, brighter than a flying
     * unit's green, under a green glow (the round 32 capture read a home unit's bar as a lost one's).
     */
    static final Color ALLY_HOME = Color.valueOf("C8FFE0");
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
            if (sortie.navalConvoy()) {
                alliesLabel =
                        secondary.label().isEmpty() ? CONVOY : secondary.label().toUpperCase(Locale.ROOT);
                for (int k = 0; k < sortie.allyCount() && allyPip == null; k++) {
                    String pip = sortie.allySpec(k).slug() + "-pip";
                    if (sortie.allySpec(k).damageable() && sprites.has(pip)) {
                        allyPip = sprites.region(pip);
                    }
                }
            }
        }
        boolean targets = !sortie.script().targets().isEmpty();
        boolean naval = sortie.navalConvoy();
        boolean two = (sortie.allyCount() > 0 && !naval) || targets;
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
        } else if (naval) {
            drawConvoyTracker(batch, sortie);
        } else if (sortie.airEscort()) {
            drawShuttleTracker(batch, sortie);
        } else if (two) {
            drawTwoTrackers(batch, sortie);
        } else if (!sortie.script().secondary().none()) {
            // M5 part D: a level without a secondary objective (Secondary.NONE) and no primary
            // tracker shows none.
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
        if (!sortie.script().secondary().none()) {
            drawSecondary(batch, sortie, wellTop - LINE, MissionLayout.TWO_LINE_WELL - LINE);
        }
    }

    /**
     * M5 part D, the air escort's two-line tracker (design/ui/hud, Level 10, user decision D3 = a):
     * line one the label and the saveable units still flying of all ({@code SHUTTLES n / 4}), red and
     * the line flashing red when the last is lost and the mission fails; line two a small armour bar
     * per unit in order ({@link #allyBarLeft}): green, white for a moment on a hit, amber below half,
     * a red flash and then dark when lost; the scripted loss's unit goes dark at once, the count
     * unchanged; home after the climb-out, full in pale mint under a green glow ({@link #ALLY_HOME}).
     * No secondary objective shares it (the level has none).
     */
    private void drawShuttleTracker(SpriteBatch batch, Sortie sortie) {
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
        kit.text(batch, kit.body, alliesLabel, colour, X + PAD, y, TEXT_WIDTH);
        kit.textRight(
                batch,
                kit.body,
                shuttleCount(sortie.saveableAlliesAlive(), sortie.saveableAllies()),
                colour,
                X + PAD,
                y,
                TEXT_WIDTH);
        int count = sortie.allyCount();
        if (allyLost.length != count) {
            allyLost = new int[count];
            Arrays.fill(allyLost, -1);
        }
        int scripted = sortie.scriptedAlly();
        int bottom = wellTop - LINE - (MissionLayout.TWO_LINE_WELL - LINE + ALLY_BAR_HEIGHT) / 2;
        for (int k = 0; k < count; k++) {
            Ally ally = sortie.ally(k);
            int left = X + PAD + allyBarLeft(k);
            if (ally.alive()) {
                allyLost[k] = -1;
                boolean home = ally.state() == Ally.State.HOME;
                Color bar = shuttleBar(home, ally.ticksSinceHit() < ALLY_HIT_TICKS, ally.hpShare());
                kit.bar(
                        batch,
                        bar,
                        ALLY_DARK,
                        left,
                        bottom,
                        ALLY_BAR_WIDTH,
                        ALLY_BAR_HEIGHT,
                        shuttleBarShare(home, ally.hpShare()));
                if (home) {
                    kit.glow(batch, SUCCESS, left - 3, bottom - 3, ALLY_BAR_WIDTH + 6, ALLY_BAR_HEIGHT + 6);
                }
                continue;
            }
            if (allyLost[k] < 0) {
                allyLost[k] = frame;
            }
            int since = frame - allyLost[k];
            boolean flash = k != scripted && since < FLASH_FRAMES && since / 8 % 2 == 0;
            kit.bar(batch, LOST, ALLY_DARK, left, bottom, ALLY_BAR_WIDTH, ALLY_BAR_HEIGHT, flash ? 1 : 0);
        }
    }

    /** M5 part E: the naval convoy tracker's label without one of its own. */
    static final String CONVOY = "CONVOY";

    /**
     * M5 part E, the naval convoy's one-line tracker (design/ui/hud, Level 11; the stated default of
     * 2026-10-08): {@code CONVOY} and a pip per cargo ship in order (green; amber after its first
     * slam; white for a moment on a hit; a red flash, then dark, when it sinks), the line flashing
     * green when the afloat secondary is met and red when it fails; {@code DONE} once met, {@code
     * FAILED} once failed and the boss is down (the pips show until then).
     */
    private void drawConvoyTracker(SpriteBatch batch, Sortie sortie) {
        int wellTop = well(batch, MissionLayout.OBJECTIVE.yTop(), WELL);
        boolean met = sortie.secondaryMet();
        boolean failed = sortie.afloatFailed();
        if (met && metFrame < 0) {
            metFrame = frame;
        } else if (!met) {
            metFrame = -1;
        }
        if (failed && failedFrame < 0) {
            failedFrame = frame;
        } else if (!failed) {
            failedFrame = -1;
        }
        boolean flash = metFrame >= 0 && frame - metFrame < FLASH_FRAMES && (frame - metFrame) / 8 % 2 == 0;
        boolean failFlash =
                failedFrame >= 0 && frame - failedFrame < FLASH_FRAMES && (frame - failedFrame) / 8 % 2 == 0;
        if (flash || failFlash) {
            kit.fill(batch, flash ? SUCCESS : LOST, X, wellTop - WELL, WIDTH, WELL);
        }
        Color colour = flash || failFlash ? HudKit.LCD : met ? SUCCESS : failed ? LOST : HudKit.LABEL;
        int y = wellTop - TEXT_DROP;
        kit.text(batch, kit.body, alliesLabel, colour, X + PAD, y, TEXT_WIDTH);
        String status = convoyStatus(met, failed, bossDown(sortie));
        if (status != null) {
            kit.textRight(batch, kit.body, status, colour, X + PAD, y, TEXT_WIDTH);
            return;
        }
        int count = sortie.damageableAllies();
        if (allyLost.length != sortie.allyCount()) {
            allyLost = new int[sortie.allyCount()];
            Arrays.fill(allyLost, -1);
        }
        int pipWidth = allyPip != null ? allyPip.getRegionWidth() : ALLY_PIP_WIDTH;
        int pipX = X + PAD + TEXT_WIDTH - count * ALLY_PIP_STEP + ALLY_PIP_STEP - pipWidth;
        int shown = 0;
        for (int k = 0; k < sortie.allyCount() && shown < count; k++) {
            if (!sortie.allySpec(k).damageable()) {
                continue;
            }
            Ally ally = sortie.ally(k);
            if (!ally.lost()) {
                allyLost[k] = -1;
            } else if (allyLost[k] < 0) {
                allyLost[k] = frame;
            }
            Color pip = convoyPip(
                    ally.lost(), ally.ticksSinceHit() < ALLY_HIT_TICKS, ally.hitsTaken() > 0, frame - allyLost[k]);
            int left = pipX + shown * ALLY_PIP_STEP;
            int bottom = y - LINE + 3;
            if (allyPip != null) {
                batch.setColor(pip);
                batch.draw(allyPip, left, bottom);
                batch.setColor(Color.WHITE);
            } else {
                kit.fill(batch, pip, left, bottom, ALLY_PIP_WIDTH, LINE);
            }
            shown++;
        }
    }

    /**
     * A cargo ship's pip: sunk, a red flash then dark ({@code sinceLost} frames after); a hit white;
     * amber once it took a slam; else green.
     */
    static Color convoyPip(boolean lost, boolean hit, boolean damaged, int sinceLost) {
        if (lost) {
            return sinceLost < FLASH_FRAMES && sinceLost / 8 % 2 == 0 ? LOST : ALLY_DARK;
        }
        return hit ? Color.WHITE : damaged ? HudKit.AMBER : SUCCESS;
    }

    /** The convoy tracker's word instead of its pips: {@code DONE} once met, {@code FAILED} once failed with the boss down; null for the pips. */
    static String convoyStatus(boolean met, boolean failed, boolean bossDown) {
        if (met) {
            return "DONE";
        }
        return failed && bossDown ? "FAILED" : null;
    }

    /** Whether the level's arena boss is down (the convoy's secondary is decided then). */
    private static boolean bossDown(Sortie sortie) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            if (sortie.setPiece(k).arena().isPresent() && sortie.setPiece(k).destroyed()) {
                return true;
            }
        }
        return false;
    }

    /** A flying (or home) unit's bar colour: home pale mint, white on a hit, amber below half, else green. */
    static Color shuttleBar(boolean home, boolean hit, double share) {
        if (home) {
            return ALLY_HOME;
        }
        return hit ? Color.WHITE : share < 0.5 ? HudKit.AMBER : SUCCESS;
    }

    /** How full a unit's bar is: its armour share, full once it is home. */
    static double shuttleBarShare(boolean home, double share) {
        return home ? 1 : share;
    }

    /** The air escort tracker's count, {@code 3 / 4}: the saveable units still flying of all. */
    static String shuttleCount(int alive, int saveable) {
        return alive + " / " + saveable;
    }

    /** Where unit {@code k}'s armour bar starts, px from the text's left edge: {@value #ALLY_BAR_STEP} px apart. */
    static int allyBarLeft(int k) {
        return k * ALLY_BAR_STEP;
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
        if (markStep == 0) {
            int widest = 0;
            for (String group : groups) {
                widest = Math.max(widest, advance(kit.body, mark(group)));
            }
            markStep = markStep(widest);
        }
        int step = markStep;
        int letterX = X + PAD + markLeft(count, step);
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
            int left = letterX + g * step;
            kit.text(batch, kit.body, mark(groups.get(g)), letter, left, y, step);
            if (state == 1) {
                kit.fill(batch, letter, left - 1, y - LINE / 2 - 1, step - MARK_GAP, 2);
            }
        }
        if (!sortie.script().secondary().none()) {
            drawSecondary(batch, sortie, wellTop - LINE, MissionLayout.TWO_LINE_WELL - LINE);
        }
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

    /** The advance of a text's glyphs (as the layout test measures it: each glyph's full step, the last one's too). */
    private static int advance(com.badlogic.gdx.graphics.g2d.BitmapFont font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph glyph =
                    font.getData().getGlyph(text.charAt(i));
            width += glyph == null ? 0 : glyph.xadvance;
        }
        return width;
    }

    /** A named target's mark on the tracker: its group name's last word ("Battery A": {@code A}, "Node C2": {@code C2}). */
    static String mark(String group) {
        return group.substring(group.lastIndexOf(' ') + 1).toUpperCase(Locale.ROOT);
    }

    /**
     * The step between the targets tracker's marks for a widest mark of {@code widest} px: a
     * letter's 16 px (Level 05), or the mark and {@value #MARK_GAP} px (Level 09's pairs, 24 px).
     */
    static int markStep(int widest) {
        return Math.max(GROUP_PIP_STEP, widest + MARK_GAP);
    }

    /**
     * Where the first of {@code count} marks starts, px from the text's left edge: Level 05's letters
     * as they were laid out; wider marks right-aligned, the last ending at the text's right edge.
     */
    static int markLeft(int count, int step) {
        int left = TEXT_WIDTH - count * step;
        return step == GROUP_PIP_STEP ? left : left + MARK_GAP;
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
