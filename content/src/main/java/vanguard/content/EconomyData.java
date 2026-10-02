package vanguard.content;

/**
 * design/systems/economy/data.yaml (Act 1 values at medium).
 *
 * @param actFactor bounties and prices scale with actFactor^(act − 1)
 * @param sellBack the share of the total spent on an item that selling it returns
 */
public record EconomyData(int startingCredits, Budget budget, double actFactor, double sellBack) {
    public EconomyData {
        Check.notNegative("starting_credits", startingCredits);
        Check.positive("act_factor", actFactor);
        Check.share("sell_back", sellBack);
    }

    /** budget(n) = base × growth^(n − 1): the credits a perfect run of level n earns. */
    public record Budget(double base, double growth) {
        public Budget {
            Check.positive("base", base);
            Check.positive("growth", growth);
        }

        public double of(int level) {
            return base * Math.pow(growth, level - 1);
        }
    }
}
