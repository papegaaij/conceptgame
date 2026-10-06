package vanguard.game.briefing;

import java.util.Optional;
import vanguard.content.campaign.Campaign;
import vanguard.game.input.MenuInput;
import vanguard.game.ui.Dialog;

/**
 * Back out of a briefing to the main menu (design/ui/briefing): Back (Esc / B) asks first, as the
 * hangar's quit does, and a yes leaves for the main menu. The campaign is left as it is: after a
 * won level it is written to the autosave the hangar would have written as it opened, so the won
 * level stays won and Continue goes on in the hangar; a new game's intro briefing has nothing to
 * keep yet and leaves the saves alone, and so does a debug run, which writes no save at all.
 */
public final class BriefingExit {
    /** What the briefing does this frame. */
    public enum Step {
        /** Nothing to do with leaving: the briefing goes on. */
        BRIEFING,
        /** The question is open: the briefing waits. */
        ASKING,
        /** The player said no: the briefing goes on. */
        STAYED,
        /** The player said yes: to the main menu. */
        LEAVE
    }

    /** The question's detail in a debug run. */
    static final String DEBUG_RUN = "A DEBUG RUN WRITES NO SAVE.";

    private final String detail;
    private Optional<Dialog> dialog = Optional.empty();

    /** @param autosaves whether leaving writes the autosave, see {@link #autosaves(Campaign)} */
    public BriefingExit(boolean autosaves) {
        this(autosaves ? "THE AUTOSAVE KEEPS YOUR PROGRESS." : "THE NEW CAMPAIGN IS NOT SAVED YET.");
    }

    private BriefingExit(String detail) {
        this.detail = detail;
    }

    /** The exit of a debug run's briefing: leaving writes no save (design/systems/saves), and says so. */
    public static BriefingExit debugRun() {
        return new BriefingExit(DEBUG_RUN);
    }

    /** Reads the frame's menu input. */
    public Step update(MenuInput input) {
        if (dialog.isPresent()) {
            return switch (dialog.get().update(input)) {
                case YES -> {
                    dialog = Optional.empty();
                    yield Step.LEAVE;
                }
                case NO -> {
                    dialog = Optional.empty();
                    yield Step.STAYED;
                }
                case NONE -> Step.ASKING;
            };
        }
        if (input.back()) {
            dialog = Optional.of(new Dialog("QUIT TO MAIN MENU?", detail, "YES, QUIT", "NO, BACK"));
            return Step.ASKING;
        }
        return Step.BRIEFING;
    }

    /** The open question, to draw over the briefing. */
    public Optional<Dialog> dialog() {
        return dialog;
    }

    /** Whether leaving writes the autosave: after a won level, not for a new game's intro. */
    public static boolean autosaves(Campaign campaign) {
        return campaign.replay().isEmpty() && campaign.nextLevel() > 1;
    }
}
