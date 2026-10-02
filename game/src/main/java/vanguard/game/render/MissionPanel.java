package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Locale;
import vanguard.game.level.RadioQueue;
import vanguard.sim.Sortie;

/**
 * The left HUD panel (design/ui/hud, mission): mission number and name, score, credits (the
 * launch balance plus what the level has earned), the chain with its draining window, the radio
 * with the speaker's portrait and the typed subtitle, the control prompts of the first section,
 * the objective tracker for the secondary objective and the level progress.
 */
final class MissionPanel {
    private static final int X = HudKit.INSET;
    private static final int PORTRAIT = 72;
    private static final int LINE = 18;
    private static final int PROMPT_LINE = 15;
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
    MissionPanel(HudKit kit, Sprites sprites, int number, String name, int launchBalance) {
        this.kit = kit;
        this.sprites = sprites;
        this.mission = String.format(Locale.ROOT, "MISSION %02d", number);
        this.name = name.toUpperCase(Locale.ROOT);
        this.launchBalance = launchBalance;
    }

    void draw(SpriteBatch batch, Sortie sortie, RadioQueue radio, List<String> prompts) {
        frame++;
        kit.panel(batch, 0);
        int width = HudKit.INNER_WIDTH;
        kit.label(batch, mission, X, 524);
        kit.lcd(batch, X, 472, width, 30);
        kit.text(batch, name, HudKit.READOUT, X + 8, 494);

        kit.label(batch, "SCORE", X, 462);
        kit.lcd(batch, X, 410, width, 30);
        kit.textRight(batch, grouped(sortie.score()), HudKit.READOUT, X, 432, width - 8);
        kit.label(batch, "CREDITS", X, 400);
        kit.lcd(batch, X, 348, width, 30);
        kit.textRight(batch, grouped(launchBalance + sortie.credits()), HudKit.AMBER, X, 370, width - 8);

        kit.text(batch, "CHAIN " + sortie.chain(), HudKit.LABEL, X, 338);
        String multiplier = String.format(Locale.ROOT, "x%.1f", sortie.chainMultiplier());
        kit.textRight(batch, multiplier, HudKit.LABEL, X, 338, width);
        kit.bar(batch, CHAIN, CHAIN_EMPTY, X, 312, width, 6, sortie.chainWindow());

        drawRadio(batch, radio);
        drawPrompts(batch, prompts);
        drawTracker(batch, sortie);

        kit.label(batch, "PROGRESS", X, 56);
        kit.bar(
                batch,
                PROGRESS,
                PROGRESS_EMPTY,
                X,
                18,
                width,
                10,
                sortie.levelSeconds() / sortie.script().seconds());
    }

    private void drawRadio(SpriteBatch batch, RadioQueue radio) {
        int width = HudKit.INNER_WIDTH;
        kit.label(batch, "RADIO", X, 300);
        kit.lcd(batch, X, 202, PORTRAIT, PORTRAIT);
        kit.lcd(batch, X, 146, width, 3 * LINE + 2);
        if (radio.current().isEmpty()) {
            return;
        }
        RadioQueue.Message message = radio.current().get();
        TextureRegion portrait = portrait(message.speaker());
        batch.draw(portrait, X, 202);
        Color colour = message.distorted() ? CHOIR : HudKit.AMBER;
        String[] names = message.speaker().toUpperCase(Locale.ROOT).split(" ", 2);
        for (int i = 0; i < names.length; i++) {
            kit.text(batch, names[i], colour, X + PORTRAIT + 10, 270 - i * LINE);
        }
        List<String> lines = radio.visibleLines();
        Color subtitle = message.distorted() ? CHOIR : HudKit.READOUT;
        for (int i = 0; i < lines.size(); i++) {
            // A distorted transmission jitters a pixel now and then.
            float jitter = message.distorted() && (frame / 3 + i) % 7 == 0 ? 1 : 0;
            kit.text(batch, lines.get(i), subtitle, X + 6 + jitter, 198 - i * LINE);
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

    private void drawPrompts(SpriteBatch batch, List<String> prompts) {
        if (prompts.isEmpty()) {
            return;
        }
        kit.lcd(batch, X, 142 - prompts.size() * PROMPT_LINE, HudKit.INNER_WIDTH, prompts.size() * PROMPT_LINE);
        for (int i = 0; i < prompts.size(); i++) {
            kit.text(batch, prompts.get(i), HudKit.LABEL, X + 6, 140 - i * PROMPT_LINE);
        }
    }

    private void drawTracker(SpriteBatch batch, Sortie sortie) {
        if (sortie.secondaryMet() && metFrame < 0) {
            metFrame = frame;
        } else if (!sortie.secondaryMet()) {
            metFrame = -1;
        }
        boolean flash = metFrame >= 0 && frame - metFrame < FLASH_FRAMES && (frame - metFrame) / 8 % 2 == 0;
        kit.lcd(batch, X, 70, HudKit.INNER_WIDTH, 22);
        if (flash) {
            kit.fill(batch, SUCCESS, X, 70, HudKit.INNER_WIDTH, 22);
        }
        Color colour = flash ? HudKit.LCD : sortie.secondaryMet() ? SUCCESS : HudKit.LABEL;
        kit.text(batch, "KILLS", colour, X + 6, 88);
        String count = sortie.secondaryMet() ? "DONE" : sortie.kills() + " / " + sortie.requiredKills();
        kit.textRight(batch, count, colour, X, 88, HudKit.INNER_WIDTH - 6);
    }

    /** A number with thin-space thousands groups, as on the HUD mock: 1 204 350. */
    static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
