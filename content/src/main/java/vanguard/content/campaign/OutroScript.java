package vanguard.content.campaign;

import java.util.List;
import java.util.Optional;
import vanguard.content.BriefingPage;

/**
 * What the briefing screen shows after an act's last debrief (design/campaign, Act intro and
 * outro): the act outro's pages, with its track under the first page.
 *
 * @param actDirectory the act's directory under design/campaign, {@code act-1-first-contact}
 * @param act the act line of the header, {@code ACT I - FIRST CONTACT}
 * @param first the act's first level
 * @param last the level that ended the act
 * @param nextLevel the level the campaign goes on with
 * @param music the track under the first page, a file {@code music/<music>.ogg}
 */
public record OutroScript(
        String actDirectory,
        String act,
        int first,
        int last,
        int nextLevel,
        Optional<String> music,
        List<BriefingPage> pages) {
    public OutroScript {
        pages = List.copyOf(pages);
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("an outro needs a page");
        }
    }
}
