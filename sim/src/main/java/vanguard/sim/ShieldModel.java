package vanguard.sim;

/**
 * A shield's numbers, from design/player/shields/data.yaml (built by {@code vanguard.content.SimSpecs}).
 *
 * @param capacity shield points when full
 * @param regenPerSecond points regained per second once regenerating
 * @param regenDelaySeconds time without hits before regeneration starts
 * @param breakSeconds extra time the shield stays down after dropping to zero, before the delay
 */
public record ShieldModel(double capacity, double regenPerSecond, double regenDelaySeconds, double breakSeconds) {}
