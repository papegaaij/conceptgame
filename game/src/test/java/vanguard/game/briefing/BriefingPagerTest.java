package vanguard.game.briefing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BriefingPagerTest {
    private final BriefingPager pager = new BriefingPager(List.of(60, 30, 90));

    @Test
    void aPageTypesOutAtTheTextSpeed() {
        assertEquals(30, pager.update(0.5, 60));
        assertEquals(30, pager.shown());

        assertEquals(30, pager.update(5, 60), "no further than the page");
        assertTrue(pager.pageComplete());
    }

    @Test
    void confirmShowsTheWholePageThenTurnsToTheNext() {
        pager.update(0.1, 60);

        pager.confirm();
        assertEquals(0, pager.page());
        assertTrue(pager.pageComplete());

        pager.confirm();
        assertEquals(1, pager.page());
        assertEquals(0, pager.shown());
    }

    @Test
    void confirmOnTheLastWholePageEndsTheBriefing() {
        for (int i = 0; i < 5; i++) {
            pager.confirm();
        }
        assertEquals(2, pager.page());
        assertFalse(pager.done());

        pager.confirm();

        assertTrue(pager.done());
    }

    @Test
    void skipJumpsToTheObjectivesOnTheLastPage() {
        pager.skip();

        assertEquals(2, pager.page());
        assertTrue(pager.pageComplete());
        pager.confirm();
        assertTrue(pager.done());
    }
}
