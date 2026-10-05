package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.ALC10.ALC_FALSE;
import static org.lwjgl.openal.ALC10.ALC_FREQUENCY;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.SOFTLoopback.ALC_FLOAT_SOFT;
import static org.lwjgl.openal.SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT;
import static org.lwjgl.openal.SOFTLoopback.ALC_FORMAT_TYPE_SOFT;
import static org.lwjgl.openal.SOFTLoopback.ALC_STEREO_SOFT;
import static org.lwjgl.openal.SOFTLoopback.alcLoopbackOpenDeviceSOFT;
import static org.lwjgl.openal.SOFTLoopback.alcRenderSamplesSOFT;
import static org.lwjgl.openal.SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.SOFTHRTF;

/**
 * The master limiter on OpenAL Soft's loopback device, which renders the mix into memory: no sound card needed, and
 * the exact samples the game's device would play. Two sources play the same 220 Hz tone, so the mix is twice it.
 */
class OutputLimiterTest {
    private static final int RATE = 48_000;
    /** The loopback device's format; a reset must repeat it. */
    private static final int[] FORMAT = {
        ALC_FORMAT_CHANNELS_SOFT, ALC_STEREO_SOFT, ALC_FORMAT_TYPE_SOFT, ALC_FLOAT_SOFT, ALC_FREQUENCY, RATE
    };
    /** The limiter's look-ahead delays the mix by up to 1 ms. */
    private static final int MAX_DELAY = RATE / 1000;

    private long device;
    private long context;

    @BeforeEach
    void openLoopbackDevice() {
        ALC.createCapabilities(0L);
        device = alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
        assumeTrue(device != 0, "no OpenAL Soft loopback device");
        var capabilities = ALC.createCapabilities(device);
        context = alcCreateContext(device, attributes());
        alcMakeContextCurrent(context);
        AL.createCapabilities(capabilities);
    }

    @AfterEach
    void closeDevice() {
        if (device != 0) {
            alcMakeContextCurrent(0);
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }

    @Test
    void floatOutputHasNoLimiterOfItsOwnSoAnOverPassesFullScale() {
        float[] mix = render(0.8, 0.8);
        assertTrue(peak(mix) > 1.5, "OpenAL Soft passes an over unlimited to a float device");
    }

    @Test
    void holdsAnOverAtFullScale() {
        assertTrue(OutputLimiter.start(device, FORMAT).isPresent());
        float[] mix = render(0.8, 0.8);
        assertTrue(peak(mix) <= 1.0f, "peak " + peak(mix));
        assertTrue(peak(mix) > 0.99f, "the limiter only holds the over down, it does not duck the mix");
    }

    @Test
    void passesAMixBelowFullScaleUnchangedOneLookAheadLate() {
        assertTrue(OutputLimiter.start(device, FORMAT).isPresent());
        // 0.45 + 0.45 = 0.9: loud, but below full scale.
        float[] mix = render(0.45, 0.45);
        short[] tone = tone(0.45);
        int delay = delay(mix, tone);
        assertTrue(delay <= MAX_DELAY, "delay " + delay);
        for (int i = 0; i + 2 * delay < mix.length && i < tone.length; i++) {
            assertEquals(2 * tone[i] / 32768f, mix[i + 2 * delay], 1e-6f, "sample " + i);
        }
    }

    @Test
    void turnsBackOnAfterADeviceReopenTurnedItOff() {
        OutputLimiter limiter = OutputLimiter.start(device, FORMAT).orElseThrow();
        assertTrue(limiter.on());
        // libGDX reopens the device without attributes when the output device changes: the limiter falls back to
        // the float default, off. Here a reset stands in for the reopen.
        assertTrue(SOFTHRTF.alcResetDeviceSOFT(device, withLimiter(ALC_FALSE)));
        assertFalse(limiter.on());
        limiter.watch(System.nanoTime());
        assertTrue(limiter.on());
        assertTrue(peak(render(0.8, 0.8)) <= 1.0f);
    }

    @Test
    void checksAtMostOncePerInterval() {
        OutputLimiter limiter = OutputLimiter.start(device, FORMAT).orElseThrow();
        long now = System.nanoTime();
        limiter.watch(now);
        assertTrue(SOFTHRTF.alcResetDeviceSOFT(device, withLimiter(ALC_FALSE)));
        limiter.watch(now + OutputLimiter.WATCH_NANOS / 2);
        assertFalse(limiter.on());
        limiter.watch(now + OutputLimiter.WATCH_NANOS);
        assertTrue(limiter.on());
    }

    private static int[] withLimiter(int state) {
        return attributes(ALC_OUTPUT_LIMITER_SOFT, state);
    }

    /** The loopback format, these attributes and the terminating 0. */
    private static int[] attributes(int... more) {
        int[] attributes = Arrays.copyOf(FORMAT, FORMAT.length + more.length + 1);
        System.arraycopy(more, 0, attributes, FORMAT.length, more.length);
        return attributes;
    }

    /** Plays two sources with a 0.5 s tone each, of these amplitudes, and renders the mix: interleaved stereo. */
    private float[] render(double first, double second) {
        int[] buffers = {alGenBuffers(), alGenBuffers()};
        int[] sources = {alGenSources(), alGenSources()};
        alBufferData(buffers[0], AL_FORMAT_STEREO16, tone(first), RATE);
        alBufferData(buffers[1], AL_FORMAT_STEREO16, tone(second), RATE);
        for (int i = 0; i < 2; i++) {
            alSourcei(sources[i], AL_BUFFER, buffers[i]);
        }
        alSourcePlay(sources[0]);
        alSourcePlay(sources[1]);
        float[] mix = new float[RATE * 2];
        alcRenderSamplesSOFT(device, mix, RATE / 2 + MAX_DELAY * 2);
        for (int i = 0; i < 2; i++) {
            alSourceStop(sources[i]);
        }
        alDeleteSources(sources);
        alDeleteBuffers(buffers);
        return mix;
    }

    /** 0.5 s of a 220 Hz tone, the same in both channels. */
    private static short[] tone(double amplitude) {
        short[] samples = new short[RATE];
        for (int frame = 0; frame < RATE / 2; frame++) {
            short sample = (short) Math.round(Math.sin(2 * Math.PI * 220 * frame / RATE) * amplitude * 32767);
            samples[2 * frame] = sample;
            samples[2 * frame + 1] = sample;
        }
        return samples;
    }

    /** The delay in frames at which the mix best matches twice the tone. */
    private static int delay(float[] mix, short[] tone) {
        int best = 0;
        double bestError = Double.MAX_VALUE;
        for (int delay = 0; delay <= MAX_DELAY * 2; delay++) {
            double error = 0;
            for (int i = 0; i < RATE / 4; i += 2) {
                error += Math.abs(mix[i + 2 * delay] - 2 * tone[i] / 32768f);
            }
            if (error < bestError) {
                bestError = error;
                best = delay;
            }
        }
        return best;
    }

    private static float peak(float[] mix) {
        float peak = 0;
        for (float sample : mix) {
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }
}
