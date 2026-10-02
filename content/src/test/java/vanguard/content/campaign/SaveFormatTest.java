package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import vanguard.content.Difficulty;

class SaveFormatTest {
    static SaveGame save() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.HARD);
        campaign.complete(CampaignTest.won("A", 80, 12_000), 41.5);
        campaign.play(300);
        return campaign.save(Instant.parse("2026-10-02T10:00:00Z"));
    }

    @Test
    void aSaveSurvivesTheRoundTrip() throws SaveException {
        SaveGame save = save();

        String json = SaveFormat.write(save);

        assertTrue(json.contains("\"version\" : 1"), json);
        assertEquals(save, SaveFormat.read(json));
    }

    @Test
    void aNewerFormatVersionIsRejected() {
        String json = SaveFormat.write(save()).replace("\"version\" : 1", "\"version\" : 2");

        SaveException e = assertThrows(SaveException.class, () -> SaveFormat.read(json));
        assertTrue(e.getMessage().contains("newer version"), e.getMessage());
    }

    @Test
    void anUnknownOrMissingVersionIsRejected() {
        String json = SaveFormat.write(save());

        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 1", "\"version\" : 0")));
        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 1,", "")));
        assertThrows(
                SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 1", "\"version\" : \"1\"")));
    }

    @Test
    void aMalformedOrIncompleteSaveIsRejected() {
        String json = SaveFormat.write(save());

        assertThrows(SaveException.class, () -> SaveFormat.read("{not json"));
        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"credits\"", "\"cash\"")));
        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"difficulty\" : \"HARD\",", "")));
        assertThrows(
                SaveException.class, () -> SaveFormat.read(json.replace("\"nextLevel\" : 2", "\"nextLevel\" : 0")));
    }
}
