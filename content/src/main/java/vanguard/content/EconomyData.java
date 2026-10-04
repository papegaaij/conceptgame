package vanguard.content;

/**
 * design/systems/economy/data.yaml (Act 1 values at medium).
 *
 * @param actFactor bounties and prices scale with actFactor^(act − 1)
 * @param sellBack the share of the total spent on an item that selling it returns
 * @param typicalPlayer the typical medium player the budget is set for
 */
public record EconomyData(
        int startingCredits, Budget budget, double actFactor, double sellBack, TypicalPlayer typicalPlayer) {
    public EconomyData {
        Check.notNegative("starting_credits", startingCredits);
        Check.positive("act_factor", actFactor);
        Check.share("sell_back", sellBack);
    }

    /**
     * The share of each credit source the typical medium player collects (design/systems/economy,
     * per-level budget): a level's typical haul is its perfect run weighted by these.
     */
    public record TypicalPlayer(
            double airKills, double groundTargets, double secrets, double pickups, double primary, double secondary) {
        public TypicalPlayer {
            Check.share("air_kills", airKills);
            Check.share("ground_targets", groundTargets);
            Check.share("secrets", secrets);
            Check.share("pickups", pickups);
            Check.share("primary", primary);
            Check.share("secondary", secondary);
        }
    }

    /** budget(n) = base × growth^(n − 1): the credits a typical run of level n earns at medium. */
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
