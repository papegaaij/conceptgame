package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.Optional;

/**
 * design/campaign/&lt;act&gt;/data.yaml: an act's levels, its intro (the title card and the act
 * briefing that open the act) and its outro after the act's last level (design/campaign, act
 * documents: Act intro and outro).
 *
 * @param levels the act's first and last level, by their global numbers
 * @param outro the act-end outro, if the act has one yet
 */
public record ActData(Levels levels, TitleCard titleCard, List<BriefingPage> briefing, Optional<Outro> outro) {
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

    /**
     * The act-end outro (design/campaign, Act intro and outro): briefing pages shown after the
     * debrief of the act's last level, before the hangar of the next act.
     *
     * @param music the track under the first page, a file {@code music/<music>.ogg} played once
     *     ({@code act-complete}, track 24)
     * @param pages the pages, as a briefing's
     */
    public record Outro(Optional<String> music, List<BriefingPage> pages) {
        public Outro {
            Check.notEmpty("pages", pages);
            Check.that(
                    music.map(name -> name.matches("[a-z0-9]+(-[a-z0-9]+)*")).orElse(true),
                    "music: a lower-case kebab-case name");
        }
    }
}
