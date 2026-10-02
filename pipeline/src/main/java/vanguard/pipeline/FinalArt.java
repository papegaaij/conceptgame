package vanguard.pipeline;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * How the build tells final art from placeholders (design/art-direction/production, Pipeline):
 * every PNG a production generator in tools/art/ writes carries a {@code Source} text chunk naming
 * it; placeholders carry none or a {@code Placeholder} chunk. Every OGG it writes carries a
 * {@code SOURCE} Vorbis comment, which no placeholder sound has.
 */
final class FinalArt {
    static final String KEYWORD = "Source";
    static final String SOUND_KEY = "SOURCE";
    private static final int OGG_PAGE_HEADER = 27;
    private static final byte[] VORBIS_COMMENT = {3, 'v', 'o', 'r', 'b', 'i', 's'};
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private FinalArt() {}

    /** Whether the sprite {@code name} (a single {@code name.png} or frames from {@code name_0.png}) is final. */
    static boolean exists(Path folder, String name) throws IOException {
        return isFinal(folder.resolve(name + ".png")) || isFinal(folder.resolve(name + "_0.png"));
    }

    /** Whether the PNG exists and has a {@code Source} tEXt chunk before its image data. */
    static boolean isFinal(Path png) throws IOException {
        if (!Files.isRegularFile(png)) {
            return false;
        }
        try (InputStream file = Files.newInputStream(png);
                DataInputStream in = new DataInputStream(file)) {
            byte[] signature = in.readNBytes(SIGNATURE.length);
            if (!Arrays.equals(signature, SIGNATURE)) {
                return false;
            }
            while (true) {
                int length = in.readInt();
                String type = new String(in.readNBytes(4), StandardCharsets.US_ASCII);
                if (type.equals("IDAT") || type.equals("IEND")) {
                    return false;
                }
                byte[] data = in.readNBytes(length);
                in.skipNBytes(4);
                if (type.equals("tEXt") && keyword(data).equals(KEYWORD)) {
                    return true;
                }
            }
        } catch (EOFException truncated) {
            return false;
        }
    }

    /**
     * Whether the OGG Vorbis file exists and its comment header (the stream's second packet) has a
     * {@code SOURCE} comment.
     */
    static boolean isFinalSound(Path ogg) throws IOException {
        if (!Files.isRegularFile(ogg)) {
            return false;
        }
        try (InputStream file = Files.newInputStream(ogg);
                InputStream in = new BufferedInputStream(file)) {
            ByteArrayOutputStream packet = new ByteArrayOutputStream();
            int complete = 0;
            while (true) {
                byte[] header = in.readNBytes(OGG_PAGE_HEADER);
                if (header.length < OGG_PAGE_HEADER
                        || !new String(header, 0, 4, StandardCharsets.US_ASCII).equals("OggS")) {
                    return false;
                }
                // The segment table: a packet ends with the first lacing value below 255.
                for (byte lacing : in.readNBytes(header[OGG_PAGE_HEADER - 1] & 0xFF)) {
                    int size = lacing & 0xFF;
                    packet.write(in.readNBytes(size));
                    if (size < 255) {
                        if (complete == 1) {
                            return hasSourceComment(packet.toByteArray());
                        }
                        complete++;
                        packet.reset();
                    }
                }
            }
        }
    }

    private static boolean hasSourceComment(byte[] packet) {
        if (packet.length < VORBIS_COMMENT.length
                || !Arrays.equals(packet, 0, VORBIS_COMMENT.length, VORBIS_COMMENT, 0, VORBIS_COMMENT.length)) {
            return false;
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        String prefix = SOUND_KEY + "=";
        try {
            in.position(VORBIS_COMMENT.length);
            int vendor = in.getInt();
            in.position(in.position() + vendor);
            int comments = in.getInt();
            for (int i = 0; i < comments; i++) {
                int length = in.getInt();
                if (length < 0 || length > in.remaining()) {
                    return false;
                }
                byte[] comment = new byte[length];
                in.get(comment);
                if (new String(comment, StandardCharsets.UTF_8).regionMatches(true, 0, prefix, 0, prefix.length())) {
                    return true;
                }
            }
            return false;
        } catch (BufferUnderflowException | IllegalArgumentException truncated) {
            return false;
        }
    }

    private static String keyword(byte[] text) {
        int end = 0;
        while (end < text.length && text[end] != 0) {
            end++;
        }
        return new String(text, 0, end, StandardCharsets.ISO_8859_1);
    }
}
