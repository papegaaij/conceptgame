package vanguard.game.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MenuTest {
    private final Menu<String> menu = new Menu<>(List.of(
            Menu.Item.disabled("continue", "CONTINUE"),
            Menu.Item.of("new", "NEW GAME"),
            Menu.Item.disabled("load", "LOAD GAME"),
            Menu.Item.of("options", "OPTIONS"),
            Menu.Item.of("quit", "QUIT")));

    @Test
    void theCursorStartsOnTheFirstEnabledItem() {
        assertEquals("new", menu.selectedId());
    }

    @Test
    void theCursorSkipsDisabledItems() {
        assertTrue(menu.move(1));

        assertEquals("options", menu.selectedId());
    }

    @Test
    void theCursorWrapsAtBothEnds() {
        menu.move(-1);
        assertEquals("quit", menu.selectedId());

        menu.move(1);

        assertEquals("new", menu.selectedId());
    }

    @Test
    void aSingleEnabledItemDoesNotMove() {
        var single = new Menu<>(List.of(Menu.Item.of("resume", "RESUME"), Menu.Item.disabled("abort", "ABORT")));

        assertFalse(single.move(1));
    }
}
