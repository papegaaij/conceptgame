package vanguard.sim;

/**
 * The Skitter's stat block, from design/enemies/air/skitter/README.md, with its contact damage
 * from the balancing basis in design/enemies/README.md ({@code tiny} contact).
 *
 * @param hp hit points in damage units
 * @param hitbox the hit box (the sprite is larger)
 * @param snakeSpeed speed along a snake path in px/s
 * @param snakeSpacingSeconds delay between two Skitters of a snake
 * @param contactDamage damage of ramming the player
 * @param layer the layer it flies on
 */
public record SkitterSpec(
        double hp, Hitbox hitbox, double snakeSpeed, double snakeSpacingSeconds, double contactDamage, Layer layer) {
    public static final SkitterSpec SKITTER = new SkitterSpec(1, new Hitbox(16, 16), 190, 0.25, 6, Layer.AIR);
}
