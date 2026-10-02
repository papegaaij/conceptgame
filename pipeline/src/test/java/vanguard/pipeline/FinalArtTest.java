package vanguard.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FinalArtTest {
    @TempDir
    Path dir;

    @Test
    void aSourceChunkMarksFinalArt() throws IOException {
        write("ship_0.png", "Source", "tools/art/stormhawk.py");

        assertTrue(FinalArt.exists(dir, "ship"));
    }

    @Test
    void placeholdersAndMissingFilesAreNotFinal() throws IOException {
        write("portrait-rook.png", null, null);
        write("beacon_0.png", "Placeholder", "tools/concept/ground_targets.py");

        assertFalse(FinalArt.exists(dir, "portrait-rook"));
        assertFalse(FinalArt.exists(dir, "beacon"));
        assertFalse(FinalArt.exists(dir, "skitter"));
    }

    @Test
    void aSourceCommentMarksAFinalSound() throws IOException {
        // A vendor string longer than one lacing segment, so the comment packet spans two.
        writeOgg("hit-metal-r08-a.ogg", "x".repeat(300), "TITLE=ping", "SOURCE=tools/art/sfx_originals.py");

        assertTrue(FinalArt.isFinalSound(dir.resolve("hit-metal-r08-a.ogg")));
    }

    @Test
    void placeholderAndMissingSoundsAreNotFinal() throws IOException {
        writeOgg("ui-menu-move-r08-a.ogg", "Lavf", "TITLE=menu move");
        Files.write(dir.resolve("broken.ogg"), new byte[] {'O', 'g', 'g', 'S', 0});

        assertFalse(FinalArt.isFinalSound(dir.resolve("ui-menu-move-r08-a.ogg")));
        assertFalse(FinalArt.isFinalSound(dir.resolve("broken.ogg")));
        assertFalse(FinalArt.isFinalSound(dir.resolve("explosion-r02-a.ogg")));
    }

    /** A minimal OGG Vorbis start: one page with the identification and the comment header packets. */
    static void writeOgg(Path file, String vendor, String... comments) throws IOException {
        var comment = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        comment.put(new byte[] {3, 'v', 'o', 'r', 'b', 'i', 's'});
        comment.putInt(vendor.length()).put(vendor.getBytes(StandardCharsets.UTF_8));
        comment.putInt(comments.length);
        for (String c : comments) {
            byte[] bytes = c.getBytes(StandardCharsets.UTF_8);
            comment.putInt(bytes.length).put(bytes);
        }
        byte[] identification = new byte[30];
        identification[0] = 1;
        byte[] second = Arrays.copyOf(comment.array(), comment.position());
        var lacing = new ByteArrayOutputStream();
        lacing.write(identification.length);
        for (int left = second.length; left >= 0; left -= 255) {
            lacing.write(Math.min(left, 255));
        }
        var page = new ByteArrayOutputStream();
        page.write(new byte[] {'O', 'g', 'g', 'S'});
        page.write(new byte[22]);
        page.write(lacing.size());
        page.write(lacing.toByteArray());
        page.write(identification);
        page.write(second);
        Files.write(file, page.toByteArray());
    }

    private void writeOgg(String name, String vendor, String... comments) throws IOException {
        writeOgg(dir.resolve(name), vendor, comments);
    }

    /** A minimal PNG: signature, header, an optional text chunk, image data and end (CRCs zero). */
    private void write(String name, String keyword, String text) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(out, "IHDR", new byte[13]);
        if (keyword != null) {
            chunk(out, "tEXt", (keyword + "\0" + text).getBytes(StandardCharsets.ISO_8859_1));
        }
        chunk(out, "IDAT", new byte[4]);
        chunk(out, "IEND", new byte[0]);
        Files.write(dir.resolve(name), bytes.toByteArray());
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(type.getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.writeInt(0);
    }
}
