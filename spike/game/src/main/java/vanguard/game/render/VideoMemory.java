package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/** Free video memory from the {@code GL_NVX_gpu_memory_info} extension (NVIDIA only). */
public final class VideoMemory {
    private static final int GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX = 0x9049;

    private final IntBuffer result = ByteBuffer.allocateDirect(16 * Integer.BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer();

    public boolean supported() {
        return Gdx.graphics.supportsExtension("GL_NVX_gpu_memory_info");
    }

    /** Currently free video memory in KiB, or -1 without the extension. Waits for the GPU first. */
    public int freeKib() {
        if (!supported()) {
            return -1;
        }
        Gdx.gl.glFinish();
        result.clear();
        Gdx.gl.glGetIntegerv(GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, result);
        return result.get(0);
    }
}
