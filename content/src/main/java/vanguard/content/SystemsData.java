package vanguard.content;

import java.util.List;
import java.util.Optional;

/** design/player/systems/data.yaml: engines, utility bays and utility modules. */
public record SystemsData(List<Engine> engines, Bays bays, List<Utility> utility) {
    public SystemsData {
        Check.notEmpty("engines", engines);
    }

    /**
     * The utility bays: {@code start} are fitted from the start, each of {@code extra} is bought once
     * (the third bay, from Act 3).
     */
    public record Bays(int start, List<Bay> extra) {
        public Bays {
            Check.positive("start", start);
        }
    }

    /** A utility bay bought in the hangar; {@code price} in credits. */
    public record Bay(int price, String available) {
        public Bay {
            Check.positive("price", price);
        }
    }

    /** One engine; {@code speed} in px/s, {@code draw} in MW. */
    public record Engine(String name, double speed, double draw, int price, String available) {
        public Engine {
            Check.positive("speed", speed);
            Check.notNegative("draw", draw);
            Check.notNegative("price", price);
        }
    }

    /**
     * A utility module with one price per upgrade level; {@code design} is its design status.
     *
     * @param magnet the Pickup magnet's numbers per level; empty for the other modules
     * @param forSale false keeps the module out of the shop although it is unlocked (an M5 module
     *     whose effects are not built yet); true if not given
     * @param targeting the Targeting computer's numbers; empty for the other modules
     * @param salvage the Salvage scanner's bonus per level; empty for the other modules
     */
    public record Utility(
            String name,
            double draw,
            List<Integer> prices,
            String available,
            String design,
            Optional<Magnet> magnet,
            Optional<Boolean> forSale,
            Optional<Targeting> targeting,
            Optional<Salvage> salvage) {
        public Utility {
            Check.notNegative("draw", draw);
            Check.notEmpty("prices", prices);
            magnet.ifPresent(numbers -> Check.that(
                    numbers.radius().size() == prices.size() && numbers.pull().size() == prices.size(),
                    "magnet: one radius and one pull per level"));
            salvage.ifPresent(
                    numbers -> Check.that(numbers.bonus().size() == prices.size(), "salvage: one bonus per level"));
        }

        /** Whether the shop sells it once it is unlocked. */
        public boolean sold() {
            return forSale.orElse(true);
        }
    }

    /**
     * The Targeting computer (design/player/systems): {@code turnBonus} is the share added to the
     * Stormhawk's homing turn rates and the Swivel Gun's slew; an enemy's HP bar stays {@code
     * barSeconds} after its last hit and then fades out over {@code barFade} s.
     */
    public record Targeting(double turnBonus, double barSeconds, double barFade) {
        public Targeting {
            Check.notNegative("turn_bonus", turnBonus);
            Check.positive("bar_seconds", barSeconds);
            Check.notNegative("bar_fade", barFade);
        }
    }

    /** The Salvage scanner: per level, the share added to salvage pickups' and hidden crates' credits. */
    public record Salvage(List<Double> bonus) {
        public Salvage {
            bonus.forEach(value -> Check.notNegative("bonus", value));
        }
    }

    /** A Pickup magnet's reach from the ship's centre (px) and pull speed (px/s), one of each per level. */
    public record Magnet(List<Double> radius, List<Double> pull) {
        public Magnet {
            radius.forEach(value -> Check.positive("radius", value));
            pull.forEach(value -> Check.positive("pull", value));
        }
    }
}
