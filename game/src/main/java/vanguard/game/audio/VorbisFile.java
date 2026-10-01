package vanguard.game.audio;

import static org.lwjgl.stb.STBVorbis.stb_vorbis_close;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_comment;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_info;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_seek;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbisComment;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * An Ogg Vorbis file decoded by LWJGL's stb_vorbis, whose seek is sample-exact. The compressed
 * file stays in native memory (a 3 MB track) and is decoded on demand, so a track never needs its
 * full PCM in memory.
 */
public final class VorbisFile implements PcmSource {
    private static final int MAX_FRAMES_PER_READ = 4096;

    private final ByteBuffer encoded;
    private final long handle;
    private final int channels;
    private final int sampleRate;
    private final long frameCount;
    private final Map<String, String> comments;
    private final ShortBuffer decodeBuffer;

    public VorbisFile(byte[] oggBytes) {
        encoded = MemoryUtil.memAlloc(oggBytes.length).put(oggBytes).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var error = stack.mallocInt(1);
            handle = stb_vorbis_open_memory(encoded, error, null);
            if (handle == MemoryUtil.NULL) {
                MemoryUtil.memFree(encoded);
                throw new IllegalArgumentException("not an Ogg Vorbis stream (stb_vorbis error " + error.get(0) + ")");
            }
            STBVorbisInfo info = stb_vorbis_get_info(handle, STBVorbisInfo.malloc(stack));
            channels = info.channels();
            sampleRate = info.sample_rate();
            comments = readComments(stb_vorbis_get_comment(handle, STBVorbisComment.malloc(stack)));
        }
        frameCount = stb_vorbis_stream_length_in_samples(handle);
        decodeBuffer = MemoryUtil.memAllocShort(MAX_FRAMES_PER_READ * channels);
    }

    private static Map<String, String> readComments(STBVorbisComment comment) {
        Map<String, String> result = new HashMap<>();
        PointerBuffer list = comment.comment_list();
        for (int i = 0; list != null && i < comment.comment_list_length(); i++) {
            String entry = MemoryUtil.memUTF8(list.get(i));
            int equals = entry.indexOf('=');
            if (equals > 0) {
                result.put(entry.substring(0, equals).toUpperCase(Locale.ROOT), entry.substring(equals + 1));
            }
        }
        return Map.copyOf(result);
    }

    /** The Vorbis comments with upper-case keys, e.g. {@code LOOPSTART}. */
    public Map<String, String> comments() {
        return comments;
    }

    public LoopPoints loopPoints() {
        return LoopPoints.fromComments(comments, frameCount);
    }

    @Override
    public int read(short[] out, int offset, int frames) {
        int request = Math.min(frames, MAX_FRAMES_PER_READ);
        decodeBuffer.clear().limit(request * channels);
        int decoded = stb_vorbis_get_samples_short_interleaved(handle, channels, decodeBuffer);
        decodeBuffer.get(0, out, offset, decoded * channels);
        return decoded;
    }

    @Override
    public void seek(long frame) {
        if (!stb_vorbis_seek(handle, Math.toIntExact(frame))) {
            throw new IllegalStateException("seek to frame " + frame + " failed");
        }
    }

    @Override
    public int channels() {
        return channels;
    }

    @Override
    public int sampleRate() {
        return sampleRate;
    }

    @Override
    public long frameCount() {
        return frameCount;
    }

    @Override
    public void close() {
        stb_vorbis_close(handle);
        MemoryUtil.memFree(decodeBuffer);
        MemoryUtil.memFree(encoded);
    }
}
