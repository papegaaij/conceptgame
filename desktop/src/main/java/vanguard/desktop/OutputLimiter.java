package vanguard.desktop;

import static org.lwjgl.openal.ALC10.ALC_TRUE;
import static org.lwjgl.openal.ALC10.alcGetInteger;
import static org.lwjgl.openal.ALC10.alcIsExtensionPresent;
import static org.lwjgl.openal.SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT;

import java.util.Arrays;
import java.util.Optional;
import org.lwjgl.openal.SOFTHRTF;

/**
 * The master limiter on the game's final mix (design/audio, Master limiter): OpenAL Soft's output limiter
 * ({@code ALC_SOFT_output_limiter}). OpenAL Soft sums every effect and the music stream, and that sum can pass full
 * scale in a busy fight. OpenAL Soft turns its limiter on by itself only for integer output; its default output is
 * float, so the limiter stays off and an over is clipped later, by the system's mixer or the sound card. This turns
 * it on by resetting the device with the limiter attribute ({@code alcResetDeviceSOFT}). It holds the peaks at 1.0;
 * until the first over it passes the mix unchanged, 1 ms late (its look-ahead), and after one it releases over a few
 * seconds (about 0.1 dB on average).
 *
 * <p>libGDX reopens the device without attributes when the output device changes (a headset plugged in or pulled),
 * and once shortly after the start (its device observer's first pass), which turns the limiter off again: {@link
 * #watch} turns it back on within a second.
 */
final class OutputLimiter {
    /** How often {@link #watch} checks that the limiter is still on. */
    static final long WATCH_NANOS = 1_000_000_000L;
    /** After this many resets in a row that leave the limiter off, {@link #watch} stops resetting the device. */
    static final int MAX_REFUSALS = 3;

    private final long device;
    private final int[] deviceAttributes;
    private int refusals;
    private long nextCheck;

    private OutputLimiter(long device, int[] deviceAttributes) {
        this.device = device;
        this.deviceAttributes = deviceAttributes;
    }

    /**
     * Turns the limiter on for a device whose OpenAL supports it, and keeps {@link #watch}ing it.
     *
     * @param deviceAttributes the device's own attributes a reset must repeat (a loopback device's format), as
     *     key-value pairs without the terminating 0
     * @return empty if the OpenAL is not OpenAL Soft or too old
     */
    static Optional<OutputLimiter> start(long device, int... deviceAttributes) {
        if (device == 0
                || !alcIsExtensionPresent(device, "ALC_SOFT_output_limiter")
                || !alcIsExtensionPresent(device, "ALC_SOFT_HRTF")) {
            return Optional.empty();
        }
        var limiter = new OutputLimiter(device, deviceAttributes.clone());
        limiter.enable();
        return Optional.of(limiter);
    }

    /** Whether the device's limiter is on now. */
    boolean on() {
        return alcGetInteger(device, ALC_OUTPUT_LIMITER_SOFT) == ALC_TRUE;
    }

    /**
     * Turns the limiter back on if a device reopen turned it off; checks at most once per {@link #WATCH_NANOS}. After
     * {@link #MAX_REFUSALS} resets in a row that leave it off it gives up, so a device that refuses the limiter is not
     * reset (a short gap in the sound) every second.
     *
     * @param now {@link System#nanoTime()}
     */
    void watch(long now) {
        if (refusals >= MAX_REFUSALS || now - nextCheck < 0) {
            return;
        }
        nextCheck = now + WATCH_NANOS;
        if (!on()) {
            enable();
            System.out.println(
                    "audio: output limiter " + (refusals == 0 ? "back on after a device reopen" : "refused"));
        }
    }

    private void enable() {
        int[] attributes = Arrays.copyOf(deviceAttributes, deviceAttributes.length + 3);
        attributes[deviceAttributes.length] = ALC_OUTPUT_LIMITER_SOFT;
        attributes[deviceAttributes.length + 1] = ALC_TRUE;
        refusals = SOFTHRTF.alcResetDeviceSOFT(device, attributes) && on() ? 0 : refusals + 1;
    }
}
