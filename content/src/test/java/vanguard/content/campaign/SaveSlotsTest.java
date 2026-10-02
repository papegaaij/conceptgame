package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vanguard.content.Difficulty;

class SaveSlotsTest {
    @TempDir
    Path directory;

    private SaveGame saveAt(String time) {
        return Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM).save(Instant.parse(time));
    }

    @Test
    void emptySlotsBeforeTheFirstSave() {
        SaveSlots slots = new SaveSlots(directory.resolve("saves"));

        assertEquals(1 + SaveSlots.MANUAL_SLOTS, slots.list().size());
        slots.list().forEach(entry -> assertInstanceOf(SaveSlots.Entry.Empty.class, entry));
        assertEquals(Optional.empty(), slots.mostRecent());
    }

    @Test
    void aWrittenSaveIsReadBackAndTheMostRecentIsContinued() throws IOException {
        SaveSlots slots = new SaveSlots(directory.resolve("saves"));
        SaveGame older = saveAt("2026-10-01T10:00:00Z");
        SaveGame newer = saveAt("2026-10-02T10:00:00Z");

        slots.write(new SaveSlots.Slot(3), newer);
        slots.write(SaveSlots.Slot.AUTOSAVE, older);

        assertEquals(new SaveSlots.Entry.Saved(new SaveSlots.Slot(3), newer), slots.read(new SaveSlots.Slot(3)));
        assertEquals(Optional.of(newer), slots.mostRecent());
        assertFalse(Files.exists(directory.resolve("saves/slot-3.json.tmp")), "no temporary file is left");
    }

    @Test
    void aWriteThatFailsLeavesThePreviousSaveUntouched() throws IOException {
        SaveSlots slots = new SaveSlots(directory);
        SaveGame first = saveAt("2026-10-01T10:00:00Z");
        slots.write(SaveSlots.Slot.AUTOSAVE, first);
        // The temporary file cannot be written: a directory is in its place.
        Files.createDirectory(directory.resolve("autosave.json.tmp"));

        assertThrows(IOException.class, () -> slots.write(SaveSlots.Slot.AUTOSAVE, saveAt("2026-10-02T10:00:00Z")));

        assertEquals(new SaveSlots.Entry.Saved(SaveSlots.Slot.AUTOSAVE, first), slots.read(SaveSlots.Slot.AUTOSAVE));
    }

    @Test
    void anUnreadableFileShowsAsSuchAndIsNotContinued() throws IOException {
        SaveSlots slots = new SaveSlots(directory);
        Files.writeString(directory.resolve("slot-1.json"), "{\"version\": 99}");

        SaveSlots.Entry entry = slots.read(new SaveSlots.Slot(1));

        assertInstanceOf(SaveSlots.Entry.Unreadable.class, entry);
        assertEquals(Optional.empty(), slots.mostRecent());
    }
}
