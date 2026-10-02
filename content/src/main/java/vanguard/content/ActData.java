package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

/**
 * design/campaign/&lt;act&gt;/data.yaml: an act's levels and its intro, the title card and the act
 * briefing that open the act (design/campaign, act documents: Act intro and outro).
 *
 * @param levels the act's first and last level, by their global numbers
 */
public record ActData(Levels levels, TitleCard titleCard, List<BriefingPage> briefing) {
    public ActData {
        Check.notEmpty("briefing", briefing);
    }

    /** The first and last level of the act, written {@code [first, last]}. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Levels(int first, int last) {
        public Levels {
            Check.that(
                    first >= 1 && first <= last,
                    "levels must be written [first, last], was [" + first + ", " + last + "]");
        }

        public boolean contains(int level) {
            return level >= first && level <= last;
        }
    }

    /**
     * The act title card (design/ui/briefing, act-title-r08-a).
     *
     * @param act the act's number as the card shows it, {@code ACT I}
     * @param name the act's title, {@code FIRST CONTACT}
     * @param line the settings and the date under the title
     */
    public record TitleCard(String act, String name, String line) {}
}
