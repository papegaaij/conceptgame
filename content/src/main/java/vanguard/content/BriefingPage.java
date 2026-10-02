package vanguard.content;

/**
 * A page of a briefing (design/ui/briefing): who speaks, with their portrait, and what they say.
 *
 * @param speaker the speaker's short name, as on the radio ({@code Okafor}, {@code Varga})
 */
public record BriefingPage(String speaker, String line) {
    public BriefingPage {
        Check.that(!speaker.isBlank(), "speaker: must not be empty");
        Check.that(!line.isBlank(), "line: must not be empty");
    }
}
