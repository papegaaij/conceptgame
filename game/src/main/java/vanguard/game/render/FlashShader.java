package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Draws a sprite blended towards a flat colour by an amount, keeping its silhouette: the white
 * hit flash of design/art-direction (Animation rules) and the ship's blue shield shimmer. GLES 2
 * compatible, like every shader of the game.
 */
public final class FlashShader implements Disposable {
    private static final String VERTEX = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_color.a = v_color.a * (255.0 / 254.0);
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """;
    private static final String FRAGMENT = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec4 u_flash;
            void main() {
                vec4 texel = v_color * texture2D(u_texture, v_texCoords);
                gl_FragColor = vec4(mix(texel.rgb, u_flash.rgb, u_flash.a), texel.a);
            }
            """;

    private final ShaderProgram program;

    public FlashShader() {
        program = new ShaderProgram(VERTEX, FRAGMENT);
        if (!program.isCompiled()) {
            throw new IllegalStateException("flash shader: " + program.getLog());
        }
    }

    /** Draws {@code region} centred on (x, y), blended by {@code amount} (0..1) towards {@code colour}. */
    public void draw(SpriteBatch batch, TextureRegion region, float x, float y, Color colour, float amount) {
        batch.setShader(program);
        program.setUniformf("u_flash", colour.r, colour.g, colour.b, amount);
        batch.draw(region, x - region.getRegionWidth() / 2f, y - region.getRegionHeight() / 2f);
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
