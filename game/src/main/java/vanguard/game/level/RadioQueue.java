package vanguard.game.level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vanguard.game.settings.GameplaySettings;
import vanguard.game.ui.Words;

/**
 * The radio chatter in the side HUD (design/ui/hud, left panel): messages queue up and play one
 * at a time, typed out in pages of three lines of up to 22 characters below the speaker's
 * portrait. A page holds for a moment once typed; a message closes after its last page.
 */
public final class RadioQueue {
    public static final int LINE_CHARS = 22;
    public static final int PAGE_LINES = 3;
    /**
     * How long a typed page stays up before the next page, and the last one before the radio closes;
     * long enough to read while flying (design/ui/hud, doubled after play-testing).
     */
    static final float PAGE_SECONDS = 3f;

    static final float LAST_PAGE_SECONDS = 5f;
    /** Silence between two messages. */
    static final float GAP_SECONDS = 0.4f;

    /** What changed in an update, for the squelch and typing sounds. */
    public enum Change {
        NONE,
        OPENED,
        TYPED,
        CLOSED
    }

    /**
     * A message wrapped into lines.
     *
     * @param expression the speaker's portrait expression ({@code grim})
     */
    public record Message(String speaker, String expression, List<String> lines, boolean distorted) {
        int pages() {
            return (lines.size() + PAGE_LINES - 1) / PAGE_LINES;
        }

        /** The characters of a page, counting a line break as one. */
        int length(int page) {
            int length = 0;
            for (String line : page(page)) {
                length += line.length() + 1;
            }
            return length;
        }

        List<String> page(int page) {
            return lines.subList(page * PAGE_LINES, Math.min(lines.size(), (page + 1) * PAGE_LINES));
        }
    }

    private final ArrayDeque<Message> queue = new ArrayDeque<>();
    private Optional<Message> current = Optional.empty();
    private int page;
    private float typed;
    private float held;
    private float opened;
    private float gap;
    private float charsPerSecond = GameplaySettings.DEFAULT_TEXT_SPEED;

    /** The typing speed: the Gameplay tab's text speed. */
    public void charsPerSecond(float speed) {
        charsPerSecond = speed;
    }

    /** Queues a line; it plays after the ones before it. */
    public void add(String speaker, String expression, String line, boolean distorted) {
        queue.add(new Message(speaker, expression, wrap(line), distorted));
    }

    /** Drops everything, as when the level restarts. */
    public void clear() {
        queue.clear();
        current = Optional.empty();
        gap = 0;
    }

    public Change update(float seconds) {
        if (current.isEmpty()) {
            gap = Math.max(0, gap - seconds);
            if (gap > 0 || queue.isEmpty()) {
                return Change.NONE;
            }
            current = Optional.of(queue.poll());
            page = 0;
            typed = 0;
            held = 0;
            opened = 0;
            return Change.OPENED;
        }
        opened += seconds;
        Message message = current.get();
        int length = message.length(page);
        if (typed < length) {
            int before = (int) typed;
            typed = Math.min(length, typed + seconds * charsPerSecond);
            return (int) typed > before ? Change.TYPED : Change.NONE;
        }
        held += seconds;
        boolean last = page == message.pages() - 1;
        if (held < (last ? LAST_PAGE_SECONDS : PAGE_SECONDS)) {
            return Change.NONE;
        }
        if (!last) {
            page++;
            typed = 0;
            held = 0;
            return Change.NONE;
        }
        current = Optional.empty();
        gap = GAP_SECONDS;
        return Change.CLOSED;
    }

    /** Whether nothing is on the radio or waiting for it. */
    public boolean idle() {
        return current.isEmpty() && queue.isEmpty();
    }

    /** The message on the radio now. */
    public Optional<Message> current() {
        return current;
    }

    /** Seconds since the message on the radio opened, for the portrait's static. */
    public float sinceOpened() {
        return opened;
    }

    /** Seconds until the message on the radio closes; infinite until its last page is typed out. */
    public float untilClosed() {
        if (current.isEmpty()) {
            return 0;
        }
        Message message = current.get();
        boolean typedOut = page == message.pages() - 1 && typed >= message.length(page);
        return typedOut ? LAST_PAGE_SECONDS - held : Float.POSITIVE_INFINITY;
    }

    /** The lines of the current page as typed so far. */
    public List<String> visibleLines() {
        if (current.isEmpty()) {
            return List.of();
        }
        List<String> visible = new ArrayList<>();
        int left = (int) typed;
        for (String line : current.get().page(page)) {
            if (left <= 0) {
                break;
            }
            visible.add(line.substring(0, Math.min(line.length(), left)));
            left -= line.length() + 1;
        }
        return visible;
    }

    /** Word-wraps a line into lines of at most {@link #LINE_CHARS}; longer words are cut. */
    public static List<String> wrap(String text) {
        return Words.wrap(text, LINE_CHARS);
    }
}
