package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopLauncherTest {
    @Test
    void fullScreenStaysOnLinuxWhenTheFocusGoes() {
        assertFalse(DesktopLauncher.autoIconify("Linux"));
        assertFalse(DesktopLauncher.autoIconify("FreeBSD"));
    }

    @Test
    void fullScreenMinimisesOnWindowsAndMacOsWhenTheFocusGoes() {
        assertTrue(DesktopLauncher.autoIconify("Windows 11"));
        assertTrue(DesktopLauncher.autoIconify("Mac OS X"));
    }
}
