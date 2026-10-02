package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Expression;
import vanguard.content.LevelData;
import vanguard.content.campaign.Intel;

/**
 * tools/art/portraits.py and tools/art/intel.py render every portrait the data asks for, under the
 * names the game derives.
 */
class PortraitsTest {
    private static final Path SPRITES = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "sprites");
    private static final Path PORTRAITS = SPRITES.resolve("portraits");
    private static final Path INTEL = SPRITES.resolve("intel");

    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theSpeakerSlugIsTheShortNameInLowerCaseWithHyphens() {
        assertEquals("okafor", Portraits.slug("Okafor"));
        assertEquals("the-choir", Portraits.slug("The Choir"));
    }

    @Test
    void everyRadioLineHasItsSpeakersPortraitInItsExpression() {
        for (LevelData level : content.levels().values()) {
            for (LevelData.RadioCue cue : level.radio()) {
                assertRadio(cue.speaker(), cue.expression().orElse(Expression.NEUTRAL));
            }
            for (LevelData.Secret secret : level.secrets()) {
                assertRadio(
                        secret.radio().speaker(), secret.radio().expression().orElse(Expression.NEUTRAL));
            }
        }
    }

    @Test
    void everyBriefingPageHasItsSpeakersPortraitInItsExpression() {
        content.acts().values().forEach(act -> act.briefing().forEach(this::assertBriefing));
        content.levels().values().forEach(level -> {
            level.briefing().pages().forEach(this::assertBriefing);
            assertBriefing(level.briefing().teaser());
        });
    }

    /** tools/art/intel.py renders the sensor portrait of every enemy type a level's intel lists. */
    @Test
    void everyEnemyTypeOfALevelHasItsIntelPortrait() {
        for (String level : content.levels().keySet()) {
            for (String enemy : Intel.of(content, level, 2).enemies()) {
                String name = Portraits.slug(enemy);
                assertTrue(Files.isRegularFile(INTEL.resolve(name + ".png")), name);
            }
        }
    }

    private static void assertRadio(String speaker, Expression expression) {
        String name = "radio-" + Portraits.slug(speaker) + "-" + expression.slug();
        assertTrue(
                Files.isRegularFile(PORTRAITS.resolve(name + ".png"))
                        || Files.isRegularFile(PORTRAITS.resolve(name + "_0.png")),
                name);
    }

    private void assertBriefing(BriefingPage page) {
        String name = "briefing-" + Portraits.slug(page.speaker()) + "-"
                + page.portrait().slug();
        assertTrue(Files.isRegularFile(PORTRAITS.resolve(name + ".png")), name);
    }
}
