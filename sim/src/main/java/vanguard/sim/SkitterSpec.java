package vanguard.sim;

/**
 * The Skitter's stat block, from design/enemies/air/skitter/data.yaml, with its contact damage
 * from the balancing basis in design/enemies/data.yaml (built by {@code vanguard.content.SimSpecs}).
 *
 * @param hp hit points in damage units
 * @param hitbox the hit box (the sprite is larger)
 * @param snakeSpeed speed along a snake path in px/s
 * @param snakeSpacingSeconds delay between two Skitters of a snake
 * @param contactDamage damage of ramming the player
 * @param layer the layer it flies on
 */
public record SkitterSpec(
        double hp, Hitbox hitbox, double snakeSpeed, double snakeSpacingSeconds, double contactDamage, Layer layer) {}
