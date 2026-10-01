package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.EnumMap;
import java.util.Map;
import vanguard.sim.Bullet;
import vanguard.sim.Enemy;
import vanguard.sim.Faction;
import vanguard.sim.Layer;
import vanguard.sim.Player;
import vanguard.sim.World;

/**
 * Draws the world layer by layer, back to front, interpolating positions between the last two
 * simulation steps. Bullets and sparks are drawn with additive blending for their glow.
 */
public final class WorldRenderer {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final Color PLAYER_SHOT = Color.valueOf("00C0FF");
    private static final Color ENEMY_SHOT = Color.valueOf("FF8020");

    private final ProceduralArt art;
    private final ParallaxLayers parallax;
    private final HaloPlatform halo;
    private final SparkField sparks;
    private final Map<Layer, Color> enemyTints = new EnumMap<>(Map.of(
            Layer.GROUND, Color.valueOf("B08A60"),
            Layer.SUB, Color.valueOf("3C8C8C80"),
            Layer.LOW_AIR, Color.valueOf("D0A040"),
            Layer.AIR, Color.valueOf("E05080")));

    public WorldRenderer(ProceduralArt art, HaloPlatform halo, SparkField sparks) {
        this.art = art;
        this.parallax = new ParallaxLayers(art);
        this.halo = halo;
        this.sparks = sparks;
    }

    /**
     * @param alpha     interpolation between the previous and the current step
     * @param haloAngle current angle of the Halo Platform ring
     */
    public void draw(SpriteBatch batch, World world, float alpha, float haloAngle) {
        double groundScroll = world.groundScroll() - World.GROUND_SCROLL * (1 - alpha);
        for (Layer layer : Layer.values()) {
            parallax.draw(batch, layer, groundScroll);
            if (layer == Layer.GROUND) {
                halo.draw(batch, haloAngle, X0 + World.WIDTH / 2f, 380);
            }
            drawEnemies(batch, world, layer, alpha);
            if (layer == Layer.AIR) {
                drawPlayer(batch, world.player(), alpha);
                drawGlowing(batch, world, alpha);
            }
        }
    }

    private void drawEnemies(SpriteBatch batch, World world, Layer layer, float alpha) {
        Color tint = enemyTints.get(layer);
        if (tint == null) {
            return;
        }
        batch.setColor(tint);
        for (int i = 0; i < world.enemyCount(); i++) {
            Enemy enemy = world.enemy(i);
            if (enemy.layer() == layer) {
                drawCentred(batch, art.enemies[enemy.variant()], enemy.renderX(alpha), enemy.renderY(alpha));
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPlayer(SpriteBatch batch, Player player, float alpha) {
        drawCentred(batch, art.player, player.renderX(alpha), player.renderY(alpha));
    }

    private void drawGlowing(SpriteBatch batch, World world, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < world.bulletCount(); i++) {
            Bullet bullet = world.bullet(i);
            double x = bullet.renderX(alpha);
            double y = bullet.renderY(alpha);
            Color colour = bullet.faction() == Faction.PLAYER ? PLAYER_SHOT : ENEMY_SHOT;
            batch.setColor(colour.r, colour.g, colour.b, 0.55f);
            drawCentred(batch, art.glow, x, y);
            batch.setColor(Color.WHITE);
            drawCentred(batch, art.bulletCore, x, y);
        }
        sparks.draw(batch, art.spark, X0);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(region, X0 + (float) x - region.getRegionWidth() / 2f, (float) y - region.getRegionHeight() / 2f);
    }
}
