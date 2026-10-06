package vanguard.content.campaign;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import vanguard.content.ArmourData;
import vanguard.content.Content;
import vanguard.content.Difficulty;
import vanguard.content.ScoringData;
import vanguard.content.WingmenData;
import vanguard.sim.WingmanSpec;

/**
 * The numbers the campaign state follows, from the design data: the starting credits
 * (design/systems/economy), the retries per level (design/systems/difficulty), the armour floor
 * of a retry (design/systems/retry), the starter loadout and its armour (design/player: the first
 * item of each list is the starter) and the grades from best to worst (design/systems/scoring).
 *
 * @param starterArmour the starter plating's maximum armour
 * @param plating the maximum armour of each plating, by name
 * @param escort Rook's rules in the escort slot (design/player/wingmen)
 */
public record CampaignRules(
        int startingCredits,
        Map<Difficulty, Optional<Integer>> retries,
        double armourFloor,
        Map<LoadoutSlot, Fitted> starterLoadout,
        double starterArmour,
        Map<String, Double> plating,
        List<String> grades,
        EscortRules escort) {
    public CampaignRules {
        retries = Map.copyOf(retries);
        starterLoadout = Map.copyOf(starterLoadout);
        plating = Map.copyOf(plating);
        grades = List.copyOf(grades);
    }

    /**
     * Rook in the campaign (design/player/wingmen).
     *
     * @param joins hired when the hangar opens with this level (or a later one) next
     * @param starterGun the id of the gun fitted when he joins, at L1
     * @param side a new campaign's side
     * @param maxArmour his full armour
     * @param launchWarning the launch warns while his armour is below this share
     */
    public record EscortRules(
            int joins, String starterGun, WingmanSpec.Side side, double maxArmour, double launchWarning) {}

    public static CampaignRules of(Content content) {
        Map<Difficulty, Optional<Integer>> retries = new EnumMap<>(Difficulty.class);
        retries.put(Difficulty.EASY, content.difficulty().retries().easy());
        retries.put(Difficulty.MEDIUM, content.difficulty().retries().medium());
        retries.put(Difficulty.HARD, content.difficulty().retries().hard());
        Map<LoadoutSlot, Fitted> starter = new EnumMap<>(LoadoutSlot.class);
        starter.put(LoadoutSlot.FRONT, new Fitted("pulse-cannon", 1));
        starter.put(
                LoadoutSlot.GENERATOR,
                new Fitted(content.generators().models().getFirst().name(), 1));
        starter.put(
                LoadoutSlot.SHIELD,
                new Fitted(content.shields().models().getFirst().name(), 1));
        starter.put(
                LoadoutSlot.ARMOUR,
                new Fitted(content.armour().plating().getFirst().name(), 1));
        starter.put(
                LoadoutSlot.ENGINE,
                new Fitted(content.systems().engines().getFirst().name(), 1));
        return new CampaignRules(
                content.economy().startingCredits(),
                retries,
                content.retry().armourFloor(),
                starter,
                content.armour().plating().getFirst().max(),
                content.armour().plating().stream()
                        .collect(Collectors.toMap(ArmourData.Plating::name, ArmourData.Plating::max)),
                content.scoring().grades().stream()
                        .map(ScoringData.Grade::grade)
                        .toList(),
                escortRules(content.wingmen()));
    }

    private static EscortRules escortRules(WingmenData wingmen) {
        WingmenData.Rook rook = wingmen.rook();
        return new EscortRules(
                rook.joins(),
                wingmen.guns().starter().id(),
                WingmanSpec.Side.valueOf(rook.side().toUpperCase(java.util.Locale.ROOT)),
                rook.armour(),
                rook.launchWarning());
    }

    /** The maximum armour of the plating with this name. */
    public double maxArmour(String plating) {
        Double max = this.plating.get(plating);
        if (max == null) {
            throw new IllegalArgumentException("no plating '" + plating + "'");
        }
        return max;
    }

    /** The retries per level on {@code difficulty}; empty for unlimited. */
    public Optional<Integer> retries(Difficulty difficulty) {
        return retries.get(difficulty);
    }
}
