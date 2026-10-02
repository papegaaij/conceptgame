package vanguard.content;

/**
 * design/systems/retry/data.yaml.
 *
 * @param armourFloor a retried level starts with its level-start armour, but at least this share of the maximum
 */
public record RetryData(double armourFloor) {
    public RetryData {
        Check.share("armour_floor", armourFloor);
    }
}
