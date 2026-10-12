package vanguard.game.render;

import java.util.List;
import vanguard.content.LevelData;

/**
 * M5 part G: how strongly a stretch of the level's atmosphere shows at a level time, as the backdrop
 * blends it ({@link Backdrop}: each change ramps over the backdrop's {@code ramp} s, centred on the
 * stretch's boundary, along a smoothstep). Level 13 reads it for the arctic snow by the atmosphere
 * ({@link SnowLooks}) and for the wind's rise through the whiteout peak ({@link #ambienceDb}).
 */
public final class AtmosphereRamp {
    private AtmosphereRamp() {}

    /** How far a change at {@code boundary} has gone at {@code t}, 0 to 1, over {@code ramp} s centred on it. */
    static double entered(double t, double boundary, double ramp) {
        double progress = Math.clamp((t - boundary) / ramp + 0.5, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    /** The weight at {@code t} of a stretch from {@code start} to {@code end}: ramping in at its start, out at its end. */
    static double weight(double t, double start, double end, double ramp, boolean first, boolean last) {
        double in = first ? 1 : entered(t, start, ramp);
        double out = last ? 0 : entered(t, end, ramp);
        return Math.clamp(in - out, 0, 1);
    }

    /**
     * The level's atmosphere density at {@code t}: each stretch's {@code density} of its atmosphere,
     * weighted as the backdrop blends them.
     */
    static double density(List<LevelData.Stretch> stretches, double ramp, double t, double[] density) {
        double sum = 0;
        for (int s = 0; s < stretches.size(); s++) {
            LevelData.Stretch stretch = stretches.get(s);
            double d = density[stretch.atmosphere().ordinal()];
            if (d > 0) {
                sum += d * weight(t, stretch.start(), stretch.end(), ramp, s == 0, s == stretches.size() - 1);
            }
        }
        return sum;
    }

    /**
     * The setting's ambience rise at level time {@code t}, dB: each section peak's {@code ambience_db}
     * (Level 13's whiteout, +3) through the peak, ramping in and out with its atmosphere; 0 elsewhere.
     */
    public static double ambienceDb(LevelData level, boolean easy, double t) {
        return ambienceDb(level.sections(), level.backdrop().ramp(), easy, t);
    }

    /** As {@link #ambienceDb(LevelData, boolean, double)} over {@code sections} with the backdrop's {@code ramp}. */
    public static double ambienceDb(List<LevelData.Section> sections, double ramp, boolean easy, double t) {
        double db = 0;
        for (LevelData.Section section : sections) {
            if (section.peak().isEmpty() || section.peak().get().ambienceRise() == 0) {
                continue;
            }
            LevelData.Peak peak =
                    easy ? section.peak().get().onEasy() : section.peak().get();
            db += peak.ambienceRise() * weight(t, peak.from(), peak.to(), ramp, false, false);
        }
        return db;
    }
}
