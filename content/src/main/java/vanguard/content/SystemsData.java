package vanguard.content;

import java.util.List;

/** design/player/systems/data.yaml: engines and utility modules. */
public record SystemsData(List<Engine> engines, List<Utility> utility) {
    public SystemsData {
        Check.notEmpty("engines", engines);
    }

    /** One engine; {@code speed} in px/s, {@code draw} in MW. */
    public record Engine(String name, double speed, double draw, int price, String available) {
        public Engine {
            Check.positive("speed", speed);
            Check.notNegative("draw", draw);
            Check.notNegative("price", price);
        }
    }

    /** A utility module with one price per upgrade level; {@code design} is its design status. */
    public record Utility(String name, double draw, List<Integer> prices, String available, String design) {
        public Utility {
            Check.notNegative("draw", draw);
            Check.notEmpty("prices", prices);
        }
    }
}
