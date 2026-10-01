package vanguard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.game.GameOptions.SceneKind;

class GameOptionsTest {
    @Test
    void defaultsToAnInteractivePlaySceneWithVsync() {
        assertEquals(new GameOptions(SceneKind.PLAY, false, 0, true, Optional.empty()), GameOptions.parse());
    }

    @Test
    void parsesABenchmarkRun() {
        GameOptions options = GameOptions.parse("--scene", "halo", "--bench", "60", "--no-vsync", "--autopilot",
                "--record", "run.rec");
        assertEquals(SceneKind.HALO, options.scene());
        assertTrue(options.benchmark());
        assertFalse(options.vsync());
        assertEquals(Optional.of(Path.of("run.rec")), options.recordTo());
    }

    @Test
    void rejectsUnknownOptionsAndMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> GameOptions.parse("--fast"));
        assertThrows(IllegalArgumentException.class, () -> GameOptions.parse("--bench"));
    }
}
