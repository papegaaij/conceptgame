package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;

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
                List.of("SURVIVE TO THE END OF THE MISSION", "BONUS: DESTROY 65 % OF ALL ENEMIES"), intro.objectives());
        assertTrue(intro.teaser().line().startsWith("Unknown contacts"));
    }

    @Test
    void anEscortLevelBriefsItsConvoyAndASpawnersSecondary() {
        BriefingScript level04 = Briefings.before(content, 4).orElseThrow();
        assertEquals(
                List.of("ESCORT THE 5 CRAWLERS TO THE END", "BONUS: KILL EVERY BROOD POD BEFORE IT BURSTS"),
                level04.objectives());
    }

    /** Level 07's "Gut the bays": a parts secondary, the parts to die before a boss phase ends (part G). */
    @Test
    void aPartsSecondaryBriefsItsPartsAndThePhase() {
        List<String> bays = List.of(
                "bay 1 left",
                "bay 1 right",
                "bay 2 left",
                "bay 2 right",
                "bay 3 left",
                "bay 3 right",
                "bay 4 left",
                "bay 4 right");
        var secondary = new LevelData.Secondary(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(bays),
                Optional.of("broadside"),
                Optional.of("BAYS"),
                50);
        var objectives =
                new LevelData.Objectives("reach-end", Optional.empty(), Optional.empty(), Optional.of(secondary));

        assertEquals(
                List.of("SURVIVE TO THE END OF THE MISSION", "BONUS: DESTROY ALL 8 BAYS BEFORE THE BROADSIDE ENDS"),
                Briefings.objectives(content, objectives));
    }

    @Test
    void theActOutroHasFourPagesEachWithItsImageAndTrack24() {
        var act = content.acts().get("act-1-first-contact");
        OutroScript outro = Briefings.outro(new CampaignRoute.ActEnd("act-1-first-contact", act, 7), 8)
                .orElseThrow();

        assertEquals("ACT I - FIRST CONTACT", outro.act());
        assertEquals(1, outro.first());
        assertEquals(Optional.of("act-complete"), outro.music());
        assertEquals(
                List.of("Okafor", "Okafor", "Varga", "Okafor"),
                outro.pages().stream().map(BriefingPage::speaker).toList());
        assertEquals(
                List.of(
                        "act-1-outro-carcass",
                        "act-1-outro-daedalus-rim",
                        "act-1-outro-second-fleet",
                        "act-1-outro-rook"),
                outro.pages().stream().map(page -> page.image().orElseThrow()).toList());
        assertTrue(outro.pages().getFirst().line().startsWith("The carrier is dead."));
    }

    /** Act 2 opens with its title card and four act pages, then Level 08's Okafor and Varga pages (M5 part B). */
    @Test
    void theBriefingBeforeLevel08IsTheAct2IntroThenItsOwnPages() {
        BriefingScript intro = Briefings.before(content, 8).orElseThrow();

        assertEquals("HOMEFRONT", intro.titleCard().orElseThrow().name());
        assertEquals("ACT II - HOMEFRONT", intro.act());
        assertEquals("NEON SKYLINE", intro.missionName());
        assertEquals(
                List.of("Okafor", "Okafor", "Okafor", "Okafor", "Okafor", "Varga"),
                intro.pages().stream().map(BriefingPage::speaker).toList(),
                "four act briefing pages, then Okafor and Varga");
        assertEquals(
                List.of(
                        "act-2-landfall",
                        "act-2-front-lines",
                        "act-2-over-home",
                        "act-2-scramble",
                        "level-08-nova-lagos",
                        "level-08-walker-scan"),
                intro.pages().stream().map(page -> page.image().orElseThrow()).toList());
        assertEquals(
                List.of("SURVIVE TO THE END OF THE MISSION", "BONUS: NO CREEPER GETS THROUGH"), intro.objectives());
        assertEquals("Rook", intro.teaser().speaker());
    }

    /** Level 09 (M5 part C): Okafor's and Varga's pages with their images, the six nodes and the bridge. */
    @Test
    void theBriefingBeforeLevel09IsItsOwnTwoPages() {
        BriefingScript briefing = Briefings.before(content, 9).orElseThrow();

        assertEquals(Optional.empty(), briefing.titleCard(), "no act intro inside the act");
        assertEquals("ARCOLOGY FALL", briefing.missionName());
        assertEquals(
                List.of("Okafor", "Varga"),
                briefing.pages().stream().map(BriefingPage::speaker).toList());
        assertEquals(
                List.of("level-09-arcology-district", "level-09-node-scan"),
                briefing.pages().stream()
                        .map(page -> page.image().orElseThrow())
                        .toList());
        assertEquals("Rook", briefing.teaser().speaker());
        System.out.println("Level 09 briefing objectives: " + briefing.objectives());
        assertEquals("BONUS: HOLD THE BRIDGE", briefing.objectives().getLast(), "the secondary by its name");
    }

    /** Level 10 (M5 part D): Okafor's and Varga's pages with their images, the escort and no bonus line. */
    @Test
    void theBriefingBeforeLevel10IsItsOwnTwoPages() {
        BriefingScript briefing = Briefings.before(content, 10).orElseThrow();

        assertEquals(Optional.empty(), briefing.titleCard(), "no act intro inside the act");
        assertEquals("EVACUATION CORRIDOR", briefing.missionName());
        assertEquals(
                List.of("Okafor", "Varga"),
                briefing.pages().stream().map(BriefingPage::speaker).toList());
        assertEquals(
                List.of("level-10-evacuation-route", "level-10-wraith-scan"),
                briefing.pages().stream()
                        .map(page -> page.image().orElseThrow())
                        .toList());
        assertEquals("Rook", briefing.teaser().speaker());
        System.out.println("Level 10 briefing objectives: " + briefing.objectives());
        assertTrue(briefing.objectives().getFirst().startsWith("ESCORT"), "the escort");
        assertTrue(
                briefing.objectives().stream().noneMatch(line -> line.startsWith("BONUS")),
                "no secondary objective (D5 = a)");
    }

    @Test
    void aLevelThatIsNotBuiltYetHasNoBriefing() {
        assertEquals(Optional.empty(), Briefings.before(content, 11));
    }
}
