package vanguard.content;

import java.util.List;

/** design/player/specials/data.yaml: the charge-based specials. */
public record SpecialsData(List<Special> specials) {
    /** A special bought by the charge; available from level {@code unlock}. */
    public record Special(String name, int chargePrice, int maxCharges, int unlock) {
        public Special {
            Check.notNegative("charge_price", chargePrice);
            Check.positive("max_charges", maxCharges);
            Check.positive("unlock", unlock);
        }
    }
}
