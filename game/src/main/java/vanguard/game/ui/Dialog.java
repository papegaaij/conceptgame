package vanguard.game.ui;

import vanguard.game.input.MenuInput;

/**
 * A confirmation (design/ui, the UI kit's confirm dialog): a question, a line of detail and two
 * buttons. The cursor starts on the safe answer; Back answers no.
 */
public final class Dialog {
    /** What the player did in a frame. */
    public enum Answer {
        NONE,
        YES,
        NO
    }

    private final String question;
    private final String detail;
    private final String yes;
    private final String no;
    private boolean yesSelected;

    /**
     * @param yes the label of the button that goes ahead, such as "YES, QUIT"
     * @param no the label of the button that stays, such as "NO, BACK"
     */
    public Dialog(String question, String detail, String yes, String no) {
        this.question = question;
        this.detail = detail;
        this.yes = yes;
        this.no = no;
    }

    public Answer update(MenuInput input) {
        if (input.back()) {
            return Answer.NO;
        }
        if (input.confirm()) {
            return yesSelected ? Answer.YES : Answer.NO;
        }
        if (input.left() || input.right() || input.up() || input.down()) {
            yesSelected = !yesSelected;
        }
        return Answer.NONE;
    }

    public String question() {
        return question;
    }

    public String detail() {
        return detail;
    }

    public String yes() {
        return yes;
    }

    public String no() {
        return no;
    }

    public boolean yesSelected() {
        return yesSelected;
    }
}
