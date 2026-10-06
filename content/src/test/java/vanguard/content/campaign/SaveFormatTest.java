package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
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

        assertTrue(json.contains("\"version\" : 3"), json);
        assertEquals(save, SaveFormat.read(json));
        assertEquals(
                Map.of(1, new SaveGame.LevelStats(480, 80)),
                SaveFormat.read(json).stats().levels());
    }

    /** Format version 1 (before M4 part G) had no level stats: such a save loads with none recorded. */
    @Test
    void aVersion1SaveLoadsWithoutLevelStats() throws SaveException {
        SaveGame save = save();
        String json = SaveFormat.write(save);
        ObjectNode tree = (ObjectNode) JsonMapper.builder().build().readTree(json);
        tree.put("version", 1);
        ((ObjectNode) tree.get("stats")).remove("levels");
        tree.remove("escort");
        String old = tree.toString();
        assertTrue(!old.contains("\"levels\""), old);

        SaveGame loaded = SaveFormat.read(old);

        assertEquals(SaveFormat.VERSION, loaded.version());
        assertEquals(Map.of(), loaded.stats().levels());
        assertEquals(save.stats().kills(), loaded.stats().kills());
        assertEquals(save.credits(), loaded.credits());
        Campaign campaign = Campaign.load(CampaignTest.RULES, loaded);
        assertEquals(Optional.empty(), campaign.levelStats(1));
    }

    @Test
    void aVersion1SaveWithLevelStatsIsRejected() {
        String json = SaveFormat.write(save()).replace("\"version\" : 3", "\"version\" : 1");
        // Its stats.levels (version 2) is what makes it invalid; the escort (version 3) goes first.

        assertThrows(SaveException.class, () -> SaveFormat.read(json));
    }

    @Test
    void anOldTopGradeSLoadsAsAPlus() throws SaveException {
        String json = SaveFormat.write(save());
        String old = json.replaceFirst("(\"grades\" : \\{\\s*\"1\" : )\"A\"", "$1\"S\"");
        assertTrue(old.contains("\"S\""), old);

        assertEquals("A+", SaveFormat.read(old).grades().get(1));
    }

    @Test
    void aNewerFormatVersionIsRejected() {
        String json = SaveFormat.write(save()).replace("\"version\" : 3", "\"version\" : 4");

        SaveException e = assertThrows(SaveException.class, () -> SaveFormat.read(json));
        assertTrue(e.getMessage().contains("newer version"), e.getMessage());
    }

    @Test
    void anUnknownOrMissingVersionIsRejected() {
        String json = SaveFormat.write(save());

        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 3", "\"version\" : 0")));
        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 3,", "")));
        assertThrows(
                SaveException.class, () -> SaveFormat.read(json.replace("\"version\" : 3", "\"version\" : \"3\"")));
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
