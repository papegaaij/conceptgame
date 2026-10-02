package vanguard.content;

import java.util.List;

/** design/player/generator/data.yaml. */
public record GeneratorData(SparePower sparePower, List<Model> models) {
    public GeneratorData {
        Check.notEmpty("models", models);
    }

    /** Spare MW boost the shield regen by {@code regenBonusPerMw} each, up to {@code maxRegenBonus}. */
    public record SparePower(double regenBonusPerMw, double maxRegenBonus) {
        public SparePower {
            Check.notNegative("regen_bonus_per_mw", regenBonusPerMw);
            Check.notNegative("max_regen_bonus", maxRegenBonus);
        }
    }

    /** One generator; {@code output} in MW. */
    public record Model(String name, double output, int price, String available) {
        public Model {
            Check.positive("output", output);
            Check.notNegative("price", price);
        }
    }
}
