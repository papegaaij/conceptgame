package vanguard.content;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * design/player/data.yaml: when shop items become available and what the in-level pickups do.
 *
 * @param availability the first level of each availability key ({@code start}, {@code act 1}, …)
 * @param pickupSeconds how long an uncollected pickup stays before it leaves the screen
 * @param pickupDriftSpeed how fast uncollected pickups drift down the screen, px/s
 */
public record PlayerData(
        Map<String, Integer> availability, double pickupSeconds, double pickupDriftSpeed, Pickups pickups) {
    private static final Pattern LEVEL = Pattern.compile("L(\\d{2})");

    public PlayerData {
        availability = Map.copyOf(availability);
        Check.positive("pickup_seconds", pickupSeconds);
        Check.positive("pickup_drift_speed", pickupDriftSpeed);
    }

    /** The level from whose hangar visit an item with this availability is in the shop: 2 for {@code act 1}, 22 for {@code L22}. */
    public int firstLevel(String available) {
        Integer level = availability.get(available);
        if (level != null) {
            return level;
        }
        Matcher matcher = LEVEL.matcher(available);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unknown availability '" + available + "'");
        }
        return Integer.parseInt(matcher.group(1));
    }

    /** Whether {@code available} is an availability key or an exact level ({@code L22}). */
    boolean knows(String available) {
        return availability.containsKey(available) || LEVEL.matcher(available).matches();
    }

    public record Pickups(
            Salvage salvage,
            Overdrive overdrive,
            ShieldCell shieldCell,
            ArmourPatch armourPatch,
            SpecialCharge specialCharge,
            DataCore dataCore) {}

    /** Credits before the difficulty multiplier. */
    public record Salvage(Credits credits) {}

    public record Credits(int small, int medium, int large) {}

    /** All weapons {@code levels} higher for {@code seconds}. */
    public record Overdrive(int levels, double seconds) {}

    public record ShieldCell(double shieldPercent) {}

    public record ArmourPatch(double armour) {}

    public record SpecialCharge(int charges) {}

    /** A lore entry that unlocks one shop item early (design/systems/economy, Data cores). */
    public record DataCore() {}
}
