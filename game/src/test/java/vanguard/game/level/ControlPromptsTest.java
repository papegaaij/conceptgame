package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.Command;

class ControlPromptsTest {
    @Test
    void eachPromptStaysUntilThePlayerDoesWhatItSays() {
        var prompts = new ControlPrompts(List.of("move", "fire", "precision"));

        prompts.update(Command.FIRE.bit());
        assertEquals(List.of(ControlPrompts.Prompt.MOVE, ControlPrompts.Prompt.PRECISION), prompts.pending());

        prompts.update(Command.of(Command.LEFT, Command.PRECISION));
        assertEquals(List.of(), prompts.pending());
    }
}
