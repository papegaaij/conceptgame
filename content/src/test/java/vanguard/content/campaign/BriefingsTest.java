package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;

class BriefingsTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theIntroBriefingIsTheActIntroThenTheLevel01Briefing() {
        BriefingScript intro = Briefings.before(content, 1).orElseThrow();

        assertEquals("FIRST CONTACT", intro.titleCard().orElseThrow().name());
        assertEquals("ACT I - FIRST CONTACT", intro.act());
        assertEquals("BREAK AT DAWN", intro.missionName());
        assertEquals(5 + 2, intro.pages().size(), "five act briefing pages, then Okafor and Varga");
        assertEquals("Varga", intro.pages().getLast().speaker());
        assertEquals(
                List.of("SURVIVE TO THE END OF THE MISSION", "BONUS: DESTROY 80 % OF ALL ENEMIES"), intro.objectives());
        assertTrue(intro.teaser().line().startsWith("Unknown contacts"));
    }

    @Test
    void anEscortLevelBriefsItsConvoyAndASpawnersSecondary() {
        BriefingScript level04 = Briefings.before(content, 4).orElseThrow();
        assertEquals(
                List.of("ESCORT THE 5 CRAWLERS TO THE END", "BONUS: KILL EVERY BROOD POD BEFORE IT BURSTS"),
                level04.objectives());
    }

    @Test
    void aLevelThatIsNotBuiltYetHasNoBriefing() {
        assertEquals(Optional.empty(), Briefings.before(content, 5));
    }
}
