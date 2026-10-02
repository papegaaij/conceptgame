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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Locale;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.sim.Sortie;

/**
 * The left HUD panel (design/ui/hud, mission): mission number and name, score, credits (the
 * launch balance plus what the level has earned), the chain with its draining window, the radio
 * with the speaker's portrait and the typed subtitle, the control prompts, the objective tracker
 * for the secondary objective and the level progress, each in its region of the
 * {@link MissionLayout}; text is cut off at the end of its well rather than run over it.
 */
final class MissionPanel {
    private static final int X = HudKit.INSET;
    private static final int WIDTH = HudKit.INNER_WIDTH;
    /** The tracker flashes this many frames after the objective is met. */
    private static final int FLASH_FRAMES = 90;

    private static final Color CHAIN = Color.valueOf("FFB000");
    private static final Color CHAIN_EMPTY = Color.valueOf("2A1C00");
    private static final Color PROGRESS = Color.valueOf("8AD0FF");
    private static final Color PROGRESS_EMPTY = Color.valueOf("102030");
    private static final Color SUCCESS = Color.valueOf("40FF80");
    private static final Color CHOIR = Color.valueOf("C890FF");

    private final HudKit kit;
    private final Sprites sprites;
    private final TransmissionStatic transmissionStatic;
    private final String mission;
    private final String name;
    private final int launchBalance;
    private int frame;
    private int metFrame = -1;

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
        kit.leftPanel(batch);
        int top = MissionLayout.MISSION.yTop();
        plate(batch, mission, top);
        kit.text(batch, kit.body, name, HudKit.LABEL, X, top - PLATE - 6, WIDTH);

        readout(batch, "SCORE", MissionLayout.SCORE, grouped(sortie.score()), HudKit.READOUT);
        readout(batch, "CREDITS", MissionLayout.CREDITS, grouped(launchBalance + sortie.credits()), HudKit.AMBER);

        drawChain(batch, sortie);
        drawRadio(batch, radio);
        drawPrompts(batch, prompts);
        drawTracker(batch, sortie);

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
        batch.draw(portrait(message.speaker()), X, portraitY);
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

    private TextureRegion portrait(String speaker) {
        return switch (speaker) {
            case "Rook" -> sprites.rook;
            case "Okafor" -> sprites.okafor;
            case "Varga" -> sprites.varga;
            case "The Choir" -> sprites.choir;
            default -> throw new IllegalArgumentException("no portrait for " + speaker);
        };
    }

    private void drawPrompts(SpriteBatch batch, List<PromptTexts.Text> prompts) {
        if (prompts.isEmpty()) {
            return;
        }
        int wellTop = well(batch, MissionLayout.PROMPTS.yTop(), PAGE_WELL);
        int keysX = X + PAD + MissionLayout.PROMPT_ACTION_WIDTH;
        int keysWidth = TEXT_WIDTH - MissionLayout.PROMPT_ACTION_WIDTH;
        for (int i = 0; i < Math.min(prompts.size(), MissionLayout.PROMPT_LINES); i++) {
            PromptTexts.Text prompt = prompts.get(i);
            int y = wellTop - TEXT_DROP - i * LINE;
            kit.text(batch, kit.small, prompt.action(), HudKit.LABEL, X + PAD, y, MissionLayout.PROMPT_ACTION_WIDTH);
            kit.textRight(batch, kit.small, prompt.keys(), HudKit.READOUT, keysX, y, keysWidth);
        }
    }

    private void drawTracker(SpriteBatch batch, Sortie sortie) {
        if (sortie.secondaryMet() && metFrame < 0) {
            metFrame = frame;
        } else if (!sortie.secondaryMet()) {
            metFrame = -1;
        }
        boolean flash = metFrame >= 0 && frame - metFrame < FLASH_FRAMES && (frame - metFrame) / 8 % 2 == 0;
        int wellTop = well(batch, MissionLayout.OBJECTIVE.yTop(), WELL);
        if (flash) {
            kit.fill(batch, SUCCESS, X, wellTop - WELL, WIDTH, WELL);
        }
        Color colour = flash ? HudKit.LCD : sortie.secondaryMet() ? SUCCESS : HudKit.LABEL;
        int y = wellTop - TEXT_DROP;
        kit.text(batch, kit.body, "KILLS", colour, X + PAD, y, TEXT_WIDTH);
        String count = sortie.secondaryMet() ? "DONE" : sortie.kills() + " / " + sortie.requiredKills();
        kit.textRight(batch, kit.body, count, colour, X + PAD, y, TEXT_WIDTH);
    }

    /** A number with thin-space thousands groups, as on the HUD mock: 1 204 350. */
    static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
