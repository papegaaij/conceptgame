package vanguard.game.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;

/**
 * The screenshot key (design/ui/controls): F12 on every screen writes the 960x540 internal screen,
 * without the window's letterbox or scanlines, as a PNG into the screenshots directory next to the
 * settings file, named by date and time. The PNG is encoded on a background thread so a level does
 * not stutter; a note shows the outcome for {@link #NOTE_SECONDS}, drawn after the capture so it is
 * not in the image.
 */
public final class Screenshots {
    private static final Logger LOG = Logger.getLogger(Screenshots.class.getName());
    private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    static final float NOTE_SECONDS = 1.5f;
    static final String SAVED = "SCREENSHOT SAVED";
    static final String FAILED = "SCREENSHOT FAILED";

    private static final int NOTE_Y = 6;
    private static final int NOTE_PADDING = 4;

    private final Path directory;
    /** The background threads' outcomes, {@link #SAVED} or {@link #FAILED}, for the note. */
    private final ConcurrentLinkedQueue<String> outcomes = new ConcurrentLinkedQueue<>();

    private String note = "";
    private float noteSeconds;

    /** @param directory where the PNGs go; it is created with the first one */
    public Screenshots(Path directory) {
        this.directory = directory;
    }

    /** Whether the screenshot key went down since the previous frame. */
    public boolean requested() {
        return Gdx.input.isKeyJustPressed(Keys.F12);
    }

    /** Writes the internal screen as it is now; call while it is drawing, before anything that must not be in the image. */
    public void capture(PixelScreen screen, SpriteBatch batch) {
        Pixmap frame = screen.read(batch);
        LocalDateTime time = LocalDateTime.now();
        Thread.ofPlatform().name("screenshot").daemon().start(() -> write(frame, time));
    }

    private void write(Pixmap frame, LocalDateTime time) {
        try {
            opaque(frame.getPixels());
            Path file = writeNew(directory, time, out -> {
                var png = new PixmapIO.PNG(PixelScreen.WIDTH * PixelScreen.HEIGHT * 4);
                try {
                    // Frame buffers are stored bottom-up.
                    png.setFlipY(true);
                    png.write(out, frame);
                } finally {
                    png.dispose();
                }
            });
            LOG.info("screenshot " + file);
            noteOutcome(SAVED);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "could not write a screenshot into " + directory, e);
            noteOutcome(FAILED);
        } finally {
            frame.dispose();
        }
    }

    /** Sets every pixel's alpha to opaque: blending leaves the frame buffer's alpha below 1 in places. */
    private static void opaque(ByteBuffer rgba) {
        for (int i = 3; i < rgba.limit(); i += 4) {
            rgba.put(i, (byte) 0xff);
        }
    }

    /** Writes the PNG's bytes. */
    interface Encoder {
        void encode(OutputStream out) throws IOException;
    }

    /**
     * Writes a new file {@code terran-vanguard-<date>_<time>.png} in {@code directory}, creating the
     * directory if needed; a second screenshot in the same second gets {@code -2}, {@code -3}, ...
     * appended. Never overwrites a file.
     *
     * @return the file written
     */
    static synchronized Path writeNew(Path directory, LocalDateTime time, Encoder encoder) throws IOException {
        Files.createDirectories(directory);
        String base = "terran-vanguard-" + NAME.format(time);
        for (int n = 1; ; n++) {
            Path file = directory.resolve(base + (n == 1 ? "" : "-" + n) + ".png");
            OutputStream out;
            try {
                out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException taken) {
                continue;
            }
            try (out) {
                encoder.encode(out);
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(file);
                throw e;
            }
            return file;
        }
    }

    /** Shows {@code outcome} from the next {@link #update} on; any thread. */
    void noteOutcome(String outcome) {
        outcomes.add(outcome);
    }

    /** Picks up the outcome of a finished screenshot and counts the note down. */
    public void update(float seconds) {
        for (String outcome = outcomes.poll(); outcome != null; outcome = outcomes.poll()) {
            note = outcome;
            noteSeconds = NOTE_SECONDS;
        }
        noteSeconds = Math.max(0, noteSeconds - seconds);
    }

    /** The note's text while it shows, empty otherwise. */
    String note() {
        return noteSeconds > 0 ? note : "";
    }

    /** Draws the note at the top centre of the internal screen; {@code batch} is drawing. */
    public void draw(SpriteBatch batch, Glass glass, Fonts fonts) {
        String text = note();
        if (text.isEmpty()) {
            return;
        }
        BitmapFont font = fonts.label;
        int width = Fonts.width(font, text) + 2 * NOTE_PADDING;
        int height = Math.round(font.getCapHeight()) + 2 * NOTE_PADDING + 1;
        int x = (PixelScreen.WIDTH - width) / 2;
        glass.fill(batch, Glass.PANEL, x, NOTE_Y, width, height);
        glass.outline(batch, Glass.TRIM, x, NOTE_Y, width, height);
        glass.centred(
                batch,
                font,
                text,
                text.equals(SAVED) ? Glass.CYAN : Glass.ALERT,
                PixelScreen.WIDTH / 2f,
                NOTE_Y + NOTE_PADDING);
    }
}
