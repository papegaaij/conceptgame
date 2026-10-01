package vanguard.game.display;

import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3Monitor;
import java.util.Arrays;
import java.util.Optional;
import org.lwjgl.glfw.GLFW;

/** Monitor lookups for placing the window; works before and after the application has started. */
public final class Monitors {
    private Monitors() {}

    /** The connected monitor with the given name, or {@code fallback} when there is none. */
    public static Monitor find(Monitor[] monitors, Optional<String> name, Monitor fallback) {
        return name.flatMap(wanted -> Arrays.stream(monitors)
                        .filter(monitor -> monitor.name.equals(wanted))
                        .findFirst())
                .orElse(fallback);
    }

    /** The part of the monitor that task bars and docks leave free, in screen coordinates. */
    public static Bounds workArea(Monitor monitor) {
        int[] x = new int[1];
        int[] y = new int[1];
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetMonitorWorkarea(((Lwjgl3Monitor) monitor).getMonitorHandle(), x, y, width, height);
        return new Bounds(x[0], y[0], width[0], height[0]);
    }

    /** Whether a remembered window would still be reachable: its centre is on one of the monitors. */
    public static boolean isOnAny(Monitor[] monitors, Bounds window) {
        return Arrays.stream(monitors).anyMatch(monitor -> workArea(monitor).containsCentreOf(window));
    }

    /** A new window on the monitor, sized by {@link WindowSizing}. */
    public static Bounds newWindow(Monitor monitor) {
        return WindowSizing.largestWindow(workArea(monitor));
    }
}
