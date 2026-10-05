package vanguard.game.display;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScreenshotsTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 4, 21, 5, 9);

    @TempDir
    Path temp;

    @Test
    void writesANewFileNamedByDateAndTimeInACreatedDirectory() throws IOException {
        Path directory = temp.resolve("screenshots");

        Path file = Screenshots.writeNew(directory, TIME, out -> out.write(new byte[] {1, 2, 3}));

        assertEquals(directory.resolve("terran-vanguard-2026-10-04_21-05-09.png"), file);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file));
    }

    @Test
    void aSecondScreenshotInTheSameSecondGetsANumber() throws IOException {
        Path first = Screenshots.writeNew(temp, TIME, out -> out.write(1));
        Path second = Screenshots.writeNew(temp, TIME, out -> out.write(2));
        Path third = Screenshots.writeNew(temp, TIME, out -> out.write(3));

        assertEquals(
                "terran-vanguard-2026-10-04_21-05-09-2.png",
                second.getFileName().toString());
        assertEquals(
                "terran-vanguard-2026-10-04_21-05-09-3.png", third.getFileName().toString());
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(first), "never overwritten");
    }

    @Test
    void aFailedEncodingLeavesNoFile() throws IOException {
        assertThrows(
                IOException.class,
                () -> Screenshots.writeNew(temp, TIME, out -> {
                    out.write(1);
                    throw new IOException("disk full");
                }));

        assertFalse(Files.exists(temp.resolve("terran-vanguard-2026-10-04_21-05-09.png")));
    }

    @Test
    void theNoteShowsForOneAndAHalfSecondsAfterTheOutcome() {
        var screenshots = new Screenshots(temp);
        assertEquals("", screenshots.note());

        screenshots.noteOutcome(Screenshots.SAVED);
        screenshots.update(0.016f);
        assertEquals(Screenshots.SAVED, screenshots.note());

        screenshots.update(Screenshots.NOTE_SECONDS);
        assertEquals("", screenshots.note());
    }
}
