package vanguard.content;

import java.util.List;

/**
 * design/player/armor/data.yaml: the plating levels and the low-armour radio line.
 *
 * @param radio the line spoken once per attempt when an armour hit first leaves the armour at or
 *     below 15 % of its maximum (design/player/armor, Low-armour warnings)
 */
public record ArmourData(List<Plating> plating, LevelData.RadioLine radio) {
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
