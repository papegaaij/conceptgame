package vanguard.content.campaign;

import java.util.List;
import java.util.Optional;
import vanguard.content.ActData;
import vanguard.content.BriefingPage;

/**
 * What the briefing screen shows before a level (design/ui/briefing): the act title card and the
 * act briefing when the level opens its act, then the mission briefing's pages, with the mission's
 * objectives and the hangar teaser.
 *
 * @param actDirectory the act's directory under design/campaign, {@code act-1-first-contact}
 * @param act the act line of the header, {@code ACT I - FIRST CONTACT}
 * @param mission the level number
 * @param missionName the level's name, {@code BREAK AT DAWN}
 * @param objectives the objective lines, the primary objective first
 */
public record BriefingScript(
        Optional<ActData.TitleCard> titleCard,
        String actDirectory,
        String act,
        int mission,
        String missionName,
        List<BriefingPage> pages,
        List<String> objectives,
        BriefingPage teaser) {
    public BriefingScript {
        pages = List.copyOf(pages);
        objectives = List.copyOf(objectives);
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("a briefing needs a page");
        }
    }
}
