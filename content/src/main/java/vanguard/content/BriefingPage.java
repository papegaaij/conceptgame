package vanguard.content;

import java.util.Optional;

/**
 * A page of a briefing (design/ui/briefing): who speaks, with their portrait, what they say and
 * optionally the page's tactical map or mission image.
 *
 * @param speaker the speaker's short name, as on the radio ({@code Okafor}, {@code Varga})
 * @param expression the portrait's expression, neutral when not given
 * @param image the image's name, a file {@code ui/briefing/<image>.png} in the assets
 */
public record BriefingPage(String speaker, String line, Optional<Expression> expression, Optional<String> image) {
    public BriefingPage {
        Check.that(!speaker.isBlank(), "speaker: must not be empty");
        Check.that(!line.isBlank(), "line: must not be empty");
        Check.that(
                image.map(name -> name.matches("[a-z0-9]+(-[a-z0-9]+)*")).orElse(true),
                "image: a lower-case kebab-case name");
    }

    /** The portrait's expression on this page. */
    public Expression portrait() {
        return expression.orElse(Expression.NEUTRAL);
    }
}
