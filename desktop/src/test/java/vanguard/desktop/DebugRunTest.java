package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.DebugFit;
import vanguard.content.campaign.SaveSlots;

/**
 * Debug runs never write saves (design/systems/saves): every debug option makes the save slots
 * read-only, so the hangar's, a failure's and the act end's autosave and a manual save leave the
 * saves directory as it was; a normal start still autosaves.
 */
class DebugRunTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);

    @TempDir
    Path directory;

    private Path settings() {
        return directory.resolve("settings.properties");
    }

    private LaunchOptions parse(String options) {
        List<String> args = new ArrayList<>(List.of("--settings", settings().toString()));
        if (!options.isBlank()) {
            args.addAll(List.of(options.split(" ")));
        }
        return LaunchOptions.parse(args.toArray(String[]::new));
    }

    /** The campaign a run plays: the level start's debug campaign, as TerranVanguard builds it, or a new game. */
    private static Campaign campaign(LaunchOptions options) {
        if (!options.startLevel()) {
            return Campaign.start(RULES, options.difficulty());
        }
        Campaign campaign = DebugFit.startAt(RULES, options.difficulty(), options.level());
        options.debugFit().ifPresent(fit -> fit.applyTo(campaign, CATALOGUE));
        if (options.actEnd()) {
            campaign.debugActEnd();
        }
        return campaign;
    }

    /** Every file in the saves directory with its contents. */
    private Map<String, String> savesOnDisk() throws IOException {
        Path saves = directory.resolve("saves");
        Map<String, String> files = new TreeMap<>();
        if (Files.isDirectory(saves)) {
            try (Stream<Path> list = Files.list(saves)) {
                for (Path file : list.toList()) {
                    files.put(file.getFileName().toString(), Files.readString(file));
                }
            }
        }
        return files;
    }

    /** The saves a run writes on its way: the hangar's autosave, a failure's, the act end's and a manual save. */
    private static List<Boolean> playAndSave(SaveSlots slots, Campaign campaign) throws IOException {
        List<Boolean> written = new ArrayList<>();
        // The hangar as it opens (also after the act outro, the act end's autosave).
        written.add(slots.write(SaveSlots.Slot.AUTOSAVE, campaign.save(Instant.now())));
        campaign.launch();
        campaign.fail();
        // The failure's used retry, at once.
        written.add(slots.write(SaveSlots.Slot.AUTOSAVE, campaign.save(Instant.now())));
        // Save game from the hangar.
        written.add(slots.write(new SaveSlots.Slot(1), campaign.save(Instant.now())));
        return written;
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "--level 4",
                "--loadout front=scatter-vulcan:3,left=bomb-rack",
                "--special airstrike:2",
                "--escort rook:missiles:3,side=right",
                "--escort none --level 9",
                "--act-end",
                "--start level",
                "--bench 3",
                "--bench 3 --start title",
                "--invulnerable",
                "--debug-speed 4",
                "--difficulty hard --invulnerable"
            })
    void aDebugRunWritesNoSave(String option) throws IOException {
        LaunchOptions options = parse(option);
        assertTrue(options.debugRun(), option + " is a debug run");
        SaveSlots slots = DesktopLauncher.saveSlots(options, settings());
        assertFalse(slots.writable());

        assertEquals(List.of(false, false, false), playAndSave(slots, campaign(options)));
        assertFalse(Files.exists(directory.resolve("saves")), "not even the saves directory is created");
    }

    @Test
    void aDebugRunLeavesThePlayersSavesAsTheyWere() throws IOException {
        Campaign real = Campaign.start(RULES, Difficulty.EASY);
        SaveSlots normal = DesktopLauncher.saveSlots(parse(""), settings());
        normal.write(SaveSlots.Slot.AUTOSAVE, real.save(Instant.parse("2026-10-01T10:00:00Z")));
        normal.write(new SaveSlots.Slot(1), real.save(Instant.parse("2026-10-01T10:00:00Z")));
        Map<String, String> before = savesOnDisk();

        LaunchOptions options = parse("--level 12 --loadout front=scatter-vulcan:5 --special airstrike");
        SaveSlots slots = DesktopLauncher.saveSlots(options, settings());
        playAndSave(slots, campaign(options));

        assertEquals(before, savesOnDisk());
        assertEquals(Difficulty.EASY, slots.mostRecent().orElseThrow().difficulty(), "the real saves are still read");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "--difficulty hard"})
    void aNormalRunStillAutosaves(String option) throws IOException {
        LaunchOptions options = parse(option);
        assertFalse(options.debugRun());
        SaveSlots slots = DesktopLauncher.saveSlots(options, settings());

        assertEquals(List.of(true, true, true), playAndSave(slots, campaign(options)));
        assertEquals(
                List.of("autosave.json", "slot-1.json"),
                List.copyOf(savesOnDisk().keySet()));
    }
}
