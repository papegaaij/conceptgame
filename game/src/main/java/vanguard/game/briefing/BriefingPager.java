package vanguard.game.briefing;

import java.util.List;

/**
 * The paging of a briefing (design/ui/briefing): each page types out at the text speed; confirm
 * first shows the whole page, then turns to the next one, and on the last page ends the briefing.
 * Skip jumps to the last page, the one with the objectives, shown whole.
 */
public final class BriefingPager {
    private final List<Integer> pageLengths;
    private int page;
    private double typed;
    private boolean done;

    /** @param pageLengths the characters of each page */
    public BriefingPager(List<Integer> pageLengths) {
        if (pageLengths.isEmpty()) {
            throw new IllegalArgumentException("a briefing needs a page");
        }
        this.pageLengths = List.copyOf(pageLengths);
    }

    /**
     * Types on.
     *
     * @return how many characters appeared in this frame
     */
    public int update(double seconds, int charsPerSecond) {
        int before = shown();
        typed = Math.min(length(), typed + seconds * charsPerSecond);
        return shown() - before;
    }

    /** Confirm: the whole page, then the next page, then the end. */
    public void confirm() {
        if (shown() < length()) {
            typed = length();
        } else if (page < pageLengths.size() - 1) {
            page++;
            typed = 0;
        } else {
            done = true;
        }
    }

    /** Skip: the last page, whole. */
    public void skip() {
        page = pageLengths.size() - 1;
        typed = length();
    }

    public int page() {
        return page;
    }

    public int pages() {
        return pageLengths.size();
    }

    /** How many characters of the page show. */
    public int shown() {
        return (int) typed;
    }

    /** Whether the page is typed out whole. */
    public boolean pageComplete() {
        return shown() == length();
    }

    /** Whether the briefing is over. */
    public boolean done() {
        return done;
    }

    private int length() {
        return pageLengths.get(page);
    }
}
