package vanguard.game.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.content.Expression;
import vanguard.game.render.Portraits;
import vanguard.game.render.Sprites;

/**
 * A briefing speaker's name plate and 144x144 portrait in an expression (design/story/characters,
 * tools/art/portraits.py: name and role as on the chosen briefing frames).
 *
 * @param name the name plate, {@code CMDR A. OKAFOR}
 * @param role the line under it, {@code CDF COMMAND}
 */
public record Speaker(String name, String role, TextureRegion portrait) {
    /** The speaker of a briefing page by its short name, as in the data files. */
    public static Speaker of(String speaker, Expression expression, Sprites sprites) {
        TextureRegion portrait = Portraits.briefing(sprites, speaker, expression);
        String role =
                switch (speaker) {
                    case "Okafor" -> "CDF COMMAND";
                    case "Varga" -> "CDF INTELLIGENCE";
                    default -> throw new IllegalArgumentException("no briefing name plate for " + speaker);
                };
        return new Speaker(plate(speaker), role, portrait);
    }

    /** The name plate of a briefing speaker by its short name: {@code Varga} is {@code DR. E. VARGA}. */
    public static String plate(String speaker) {
        return switch (speaker) {
            case "Okafor" -> "CMDR A. OKAFOR";
            case "Varga" -> "DR. E. VARGA";
            default -> throw new IllegalArgumentException("no briefing name plate for " + speaker);
        };
    }
}
