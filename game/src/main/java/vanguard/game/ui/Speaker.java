package vanguard.game.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.game.render.Sprites;

/**
 * A briefing speaker's name plate and 144x144 portrait (design/story/characters, the chosen
 * portrait sheets: name and role as on their briefing frames).
 *
 * @param name the name plate, {@code CMDR A. OKAFOR}
 * @param role the line under it, {@code CDF COMMAND}
 */
public record Speaker(String name, String role, TextureRegion portrait) {
    /** The speaker of a briefing page by its short name, as in the data files. */
    public static Speaker of(String speaker, Sprites sprites) {
        return switch (speaker) {
            case "Okafor" -> new Speaker("CMDR A. OKAFOR", "CDF COMMAND", sprites.briefingOkafor);
            case "Varga" -> new Speaker("DR. E. VARGA", "CDF INTELLIGENCE", sprites.briefingVarga);
            default -> throw new IllegalArgumentException("no briefing portrait for " + speaker);
        };
    }
}
