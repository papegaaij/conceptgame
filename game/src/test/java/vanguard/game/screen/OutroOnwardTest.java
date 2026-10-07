package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.campaign.OutroScript;

/**
 * An act outro's NEXT panel and its last page's hint follow the campaign's route (Level 08's
 * capture, round 30: after Act 1 they named the hangar while Act 2's title card came first).
 */
class OutroOnwardTest {
    private final Content content = ContentLoader.fromClasspath();

    private static OutroScript outro(int last, int nextLevel) {
        return new OutroScript(
                "act-1-first-contact",
                "ACT I - FIRST CONTACT",
                1,
                last,
                nextLevel,
                Optional.empty(),
                List.of(new BriefingPage("Okafor", "The carrier is dead.", Optional.empty(), Optional.empty())));
    }

    @Test
    void afterAct1ComeAct2sTitleCardAndTheBriefingOfLevel08() {
        BriefingScreen.Onward onward = BriefingScreen.onward(content, outro(7, 8));

        assertEquals("ACT II: HOMEFRONT, THEN THE BRIEFING FOR MISSION 08: NEON SKYLINE", onward.next());
        assertEquals("ENTER TO ACT II", onward.hint());
    }

    /** {@code --act-end} on Level 06: Level 07's briefing follows, in the same act. */
    @Test
    void anOutroWithinAnActLeadsToTheNextBriefing() {
        BriefingScreen.Onward onward = BriefingScreen.onward(content, outro(6, 7));

        assertEquals("THE BRIEFING FOR MISSION 07: BROOD CARRIER", onward.next());
        assertEquals("ENTER TO THE BRIEFING", onward.hint());
    }

    /** A next level that is not built yet: the hangar follows. */
    @Test
    void withoutTheNextLevelsDataTheHangarFollows() {
        int unbuilt = 50;
        BriefingScreen.Onward onward = BriefingScreen.onward(content, outro(49, unbuilt));

        assertEquals("THE HANGAR BEFORE MISSION 50", onward.next());
        assertEquals("ENTER TO THE HANGAR", onward.hint());
    }
}
