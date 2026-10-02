package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

/** Every briefing page of the content fits the briefing screen's text panel. */
class BriefingLayoutTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void everyBriefingPageFitsTheTextPanel() {
        content.acts().values().forEach(act -> act.briefing().forEach(this::fits));
        content.levels().values().forEach(level -> level.briefing().pages().forEach(this::fits));
    }

    private void fits(BriefingPage page) {
        int lines = BriefingScreen.lines(page).size();
        assertTrue(lines <= BriefingScreen.maxLines(), lines + " lines: " + page.line());
    }
}
