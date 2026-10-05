package vanguard.desktop;

import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio;
import java.util.Optional;
import org.lwjgl.openal.ALC10;

/**
 * libGDX's OpenAL audio with the {@link OutputLimiter} on the final mix: it turns the limiter on once the device is
 * open, before anything plays, and watches it from the render loop's {@link #update()}.
 */
final class LimitedAudio extends OpenALLwjgl3Audio {
    private final Optional<OutputLimiter> limiter;

    LimitedAudio(int simultaneousSources, int bufferCount, int bufferBytes) {
        super(simultaneousSources, bufferCount, bufferBytes);
        // libGDX made its context current; without a device it has none and the game runs silent.
        long context = ALC10.alcGetCurrentContext();
        limiter = OutputLimiter.start(context == 0 ? 0 : ALC10.alcGetContextsDevice(context));
        System.out.println("audio: output limiter " + (limiter.isPresent() ? "on" : "unavailable"));
    }

    @Override
    public void update() {
        super.update();
        limiter.ifPresent(watched -> watched.watch(System.nanoTime()));
    }
}
