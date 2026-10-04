package vanguard.content.campaign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The save slots on disk (design/systems/saves): the autosave and 8 manual slots, one JSON file
 * each in the saves directory ({@code autosave.json}, {@code slot-1.json} … {@code slot-8.json}).
 * A save is written next to its file and moved into place, so a crash never leaves half a save.
 */
public final class SaveSlots {
    public static final int MANUAL_SLOTS = 8;

    /** A slot: 0 is the autosave, 1–8 the manual slots. */
    public record Slot(int index) {
        public static final Slot AUTOSAVE = new Slot(0);

        public Slot {
            if (index < 0 || index > MANUAL_SLOTS) {
                throw new IllegalArgumentException("no slot " + index);
            }
        }

        public boolean autosave() {
            return index == 0;
        }

        /** The autosave, then the manual slots. */
        public static List<Slot> all() {
            return IntStream.rangeClosed(0, MANUAL_SLOTS).mapToObj(Slot::new).toList();
        }

        String fileName() {
            return autosave() ? "autosave.json" : "slot-" + index + ".json";
        }
    }

    /** What a slot holds. */
    public sealed interface Entry {
        Slot slot();

        record Empty(Slot slot) implements Entry {}

        record Saved(Slot slot, SaveGame save) implements Entry {}

        /** A file that is there but cannot be loaded; it is kept and can be overwritten. */
        record Unreadable(Slot slot, String reason) implements Entry {}
    }

    private final Path directory;

    public SaveSlots(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return directory;
    }

    public Entry read(Slot slot) {
        Path file = directory.resolve(slot.fileName());
        if (!Files.isRegularFile(file)) {
            return new Entry.Empty(slot);
        }
        try {
            return new Entry.Saved(slot, SaveFormat.read(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException | SaveException e) {
            return new Entry.Unreadable(slot, e.getMessage());
        }
    }

    /** Every slot, the autosave first. */
    public List<Entry> list() {
        return Slot.all().stream().map(this::read).toList();
    }

    /** The most recently written save of any slot, for Continue. */
    public Optional<SaveGame> mostRecent() {
        return mostRecentEntry().map(Entry.Saved::save);
    }

    /** The most recently written save with its slot: the current campaign (Continue, the mission select). */
    public Optional<Entry.Saved> mostRecentEntry() {
        return list().stream()
                .<Entry.Saved>mapMulti((entry, saves) -> {
                    if (entry instanceof Entry.Saved saved) {
                        saves.accept(saved);
                    }
                })
                .max(Comparator.comparing(saved -> saved.save().created()));
    }

    /** Writes a save into a slot, replacing what it held. */
    public void write(Slot slot, SaveGame save) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(slot.fileName());
        Path temporary = directory.resolve(slot.fileName() + ".tmp");
        Files.writeString(temporary, SaveFormat.write(save), StandardCharsets.UTF_8);
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
