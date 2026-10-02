package vanguard.content;

import java.util.List;

/** design/player/armor/data.yaml: the plating levels. */
public record ArmourData(List<Plating> plating) {
    public ArmourData {
        Check.notEmpty("plating", plating);
    }

    /** One plating level; {@code max} armour points. */
    public record Plating(String name, double max, int price, String available) {
        public Plating {
            Check.positive("max", max);
            Check.notNegative("price", price);
        }
    }
}
