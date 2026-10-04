package vanguard.game.briefing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.input.ActionInput;
import vanguard.game.input.Bindings;
import vanguard.game.input.FakeDevices;
import vanguard.game.input.MenuInput;

class BriefingExitTest {
    private static final CampaignRules RULES = CampaignRules.of(ContentLoader.fromClasspath());

    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());
    private final MenuInput menu = new MenuInput(input);
    private final BriefingExit exit = new BriefingExit(true);

    private BriefingExit.Step press(int key) {
        devices.keys.clear();
        input.update(devices);
        menu.update(1 / 60f);
        devices.keys.add(key);
        input.update(devices);
        menu.update(1 / 60f);
        return exit.update(menu);
    }

    @Test
    void escapeAsksAndYesLeavesForTheMainMenu() {
        assertEquals(BriefingExit.Step.ASKING, press(Keys.ESCAPE));
        assertTrue(exit.dialog().isPresent());
        assertEquals(BriefingExit.Step.ASKING, press(Keys.LEFT), "the cursor moves to yes");

        assertEquals(BriefingExit.Step.LEAVE, press(Keys.ENTER));
        assertFalse(exit.dialog().isPresent());
    }

    @Test
    void escapeAgainOrNoStaysInTheBriefing() {
        press(Keys.ESCAPE);
        assertEquals(BriefingExit.Step.STAYED, press(Keys.ESCAPE));
        press(Keys.ESCAPE);
        assertEquals(BriefingExit.Step.STAYED, press(Keys.ENTER), "the cursor starts on no");
        assertEquals(BriefingExit.Step.BRIEFING, press(Keys.ENTER), "confirm goes on with the pages");
    }

    @Test
    void aWonLevelIsAutosavedButANewGameAndAReplayAreNot() {
        Campaign fresh = Campaign.start(RULES, Difficulty.MEDIUM);
        assertFalse(BriefingExit.autosaves(fresh), "the intro has nothing to keep");

        Campaign won = Campaign.start(RULES, Difficulty.MEDIUM);
        won.complete(
                new vanguard.sim.LevelResult(
                        80,
                        95,
                        10,
                        1,
                        1,
                        30,
                        2,
                        true,
                        new vanguard.sim.LevelResult.Credits(300, 50, 0, 0, 50),
                        java.util.List.of(),
                        1000,
                        80,
                        new vanguard.sim.ScoringRules.Grade("A", 0, 0),
                        80),
                40);
        assertTrue(BriefingExit.autosaves(won), "the won level stays won");

        Campaign replay = Campaign.replay(RULES, SaveSlots.Slot.AUTOSAVE, won.save(Instant.EPOCH), 1);
        assertFalse(BriefingExit.autosaves(replay));
    }
}
