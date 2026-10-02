package vanguard.content;

import java.util.List;

/**
 * design/player/shields/data.yaml.
 *
 * @param breakSeconds how long a broken shield stays down before the regen delay starts
 */
public record ShieldData(double breakSeconds, List<Model> models) {
    public ShieldData {
        Check.notNegative("break_seconds", breakSeconds);
        Check.notEmpty("models", models);
    }

    /** One shield; {@code regen} in points per second, {@code delay} in seconds, {@code draw} in MW. */
    public record Model(
            String name, double capacity, double regen, double delay, double draw, int price, String available) {
        public Model {
            Check.positive("capacity", capacity);
            Check.positive("regen", regen);
            Check.notNegative("delay", delay);
            Check.notNegative("draw", draw);
            Check.notNegative("price", price);
        }
    }
}
