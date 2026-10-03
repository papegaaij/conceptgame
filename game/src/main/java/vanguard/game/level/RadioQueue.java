package vanguard.game.level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import vanguard.game.settings.GameplaySettings;
import vanguard.game.ui.Words;

/**
 * The radio chatter in the side HUD (design/ui/hud, left panel): messages queue up and play one
 * at a time, typed out in pages of three lines of up to 22 characters below the speaker's
 * portrait. A page holds for a moment once typed; a message closes after its last page.
 *
 * <p>Timed lines go first (design/ui/hud, Radio): a queued timed line plays before any waiting event
 * line. An event line waits for a gap, a free radio long enough to play it before the next timed
 * line is due; one that has waited longer than {@link #STALE_SECONDS} is dropped as stale. The lines
 * that close a level (its end, a met secondary objective) wait for a gap too but never go stale:
 * the outro waits for them. The queue has no clock or randomness of its own, so the same updates
 * always play the same lines.
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
    /** An event line that has waited this long for a gap is dropped as stale. */
    public static final float STALE_SECONDS = 6f;

    /** How a line queues. */
    public enum Priority {
        /** A line at its time in the level script: plays first, in order, and is never dropped. */
        TIMED,
        /** A reaction to an event (an escaped enemy, a secret): waits for a gap, stale after {@link #STALE_SECONDS}. */
        EVENT,
        /** A line that closes the level (its end, a met secondary objective): waits for a gap, never stale. */
        CLOSING
    }

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
     * @param portrait whose portrait shows: the speaker's own, or a generic one ({@code generic-cdf})
     * @param expression the speaker's portrait expression ({@code grim})
     */
    public record Message(String speaker, String portrait, String expression, List<String> lines, boolean distorted) {
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

        /** How long it is on the radio at {@code charsPerSecond}: every page typed and held. */
        float seconds(float charsPerSecond) {
            float seconds = LAST_PAGE_SECONDS + PAGE_SECONDS * (pages() - 1);
            for (int page = 0; page < pages(); page++) {
                seconds += length(page) / charsPerSecond;
            }
            return seconds;
        }
    }

    /** A queued message, how it queues and how long it has waited. */
    private static final class Waiting {
        final Message message;
        final Priority priority;
        float waited;

        Waiting(Message message, Priority priority) {
            this.message = message;
            this.priority = priority;
        }
    }

    private final List<Waiting> queue = new ArrayList<>();
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

    /** Queues a timed line in the speaker's own portrait; it plays after the ones before it. */
    public void add(String speaker, String expression, String line, boolean distorted) {
        add(speaker, speaker, expression, line, distorted, Priority.TIMED);
    }

    /** Queues a line shown with {@code portrait}'s portrait, by its priority. */
    public void add(
            String speaker, String portrait, String expression, String line, boolean distorted, Priority priority) {
        queue.add(new Waiting(new Message(speaker, portrait, expression, wrap(line), distorted), priority));
    }

    /** Drops everything, as when the level restarts. */
    public void clear() {
        queue.clear();
        current = Optional.empty();
        gap = 0;
    }

    /** An update with no timed line ahead. */
    public Change update(float seconds) {
        return update(seconds, Float.POSITIVE_INFINITY);
    }

    /** @param untilTimed seconds until the level script's next timed line is due (infinite for none) */
    public Change update(float seconds, float untilTimed) {
        for (Iterator<Waiting> waiting = queue.iterator(); waiting.hasNext(); ) {
            Waiting next = waiting.next();
            next.waited += seconds;
            if (next.priority == Priority.EVENT && next.waited > STALE_SECONDS) {
                waiting.remove();
            }
        }
        if (current.isEmpty()) {
            gap = Math.max(0, gap - seconds);
            if (gap > 0) {
                return Change.NONE;
            }
            Optional<Waiting> next = next(untilTimed);
            if (next.isEmpty()) {
                return Change.NONE;
            }
            queue.remove(next.get());
            current = Optional.of(next.get().message);
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

    /**
     * The message to open now: the oldest timed line, else the oldest other line if it ends, with
     * the gap after it, before the next timed line is due.
     */
    private Optional<Waiting> next(float untilTimed) {
        Optional<Waiting> timed = queue.stream()
                .filter(waiting -> waiting.priority == Priority.TIMED)
                .findFirst();
        if (timed.isPresent() || queue.isEmpty()) {
            return timed;
        }
        Waiting oldest = queue.getFirst();
        return oldest.message.seconds(charsPerSecond) + GAP_SECONDS <= untilTimed
                ? Optional.of(oldest)
                : Optional.empty();
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
