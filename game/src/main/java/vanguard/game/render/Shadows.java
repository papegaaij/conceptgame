package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * The flyers' drop shadows (design/art-direction, render pipeline step 6 and the Shadows rule):
 * computed at runtime from the sprite's alpha, never baked into it. A flyer's frame is drawn once
 * more as a flat dark silhouette at 85 % scale, offset down-right by its layer's depth, and only
 * where the ground layer lies under it: the ground's tiles and pieces mark the stencil buffer while
 * they are drawn ({@link #beginGround}), and the shadows draw only on marked pixels, so they fall
 * on stations, wrecks and terrain but never on open space (Levels 03 and 07) or the far layer seen
 * through gaps. A perspective tower's walls and roof are ground too ({@link TowerProjection}, drawn
 * with the ground's pieces): a flyer's shadow falls on them at the ground's offset (the art
 * direction's rule for scenery towers), not nearer for a roof. Each pixel is shadowed once (drawing clears its mark), so overlapping flyers do not
 * stack into a darker blot. Cost: one stencil clear per frame and one extra draw per flyer.
 */
public final class Shadows implements Disposable {
    /** The shadow's opacity (the rule's 45-55 %). */
    static final float OPACITY = 0.5f;
    /** The shadow is the flyer's silhouette at this scale (it is farther from the camera). */
    static final float SCALE = 0.85f;
    /** An air flyer's shadow offset down-right, px (the layer table's {@code air} row). */
    static final int AIR_DX = 21;

    static final int AIR_DY = 30;
    /** A low flyer's shadow offset down-right, px (the layer table's {@code low-air} row). */
    static final int LOW_AIR_DX = 9;

    static final int LOW_AIR_DY = 13;

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
    /** Draws as the default shader, but discards texels below the threshold so they leave the stencil alone. */
    private static final String FRAGMENT = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform float u_threshold;
            void main() {
                vec4 texel = texture2D(u_texture, v_texCoords);
                if (texel.a < u_threshold) {
                    discard;
                }
                gl_FragColor = v_color * texel;
            }
            """;
    /** The ground marks every texel it draws (its alpha 0 margins none). */
    private static final float GROUND_THRESHOLD = 1 / 512f;
    /** A shadow is the sprite's solid silhouette: its 1-bit alpha body, not its soft glow. */
    private static final float SILHOUETTE_THRESHOLD = 0.5f;

    private final ShaderProgram program;

    public Shadows() {
        program = new ShaderProgram(VERTEX, FRAGMENT);
        if (!program.isCompiled()) {
            throw new IllegalStateException("shadow shader: " + program.getLog());
        }
    }

    /** Forgets last frame's ground; call before the ground layer is drawn. */
    public void clear(SpriteBatch batch) {
        batch.flush();
        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glClearStencil(0);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);
    }

    /** What is drawn until {@link #endGround} is ground that catches shadows. */
    public void beginGround(SpriteBatch batch) {
        batch.setShader(program);
        program.setUniformf("u_threshold", GROUND_THRESHOLD);
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
    }

    public void endGround(SpriteBatch batch) {
        batch.setShader(null);
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
    }

    /** The shadows drawn until {@link #end} fall on the marked ground only, each pixel once. */
    public void begin(SpriteBatch batch) {
        batch.setShader(program);
        program.setUniformf("u_threshold", SILHOUETTE_THRESHOLD);
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_ZERO);
        batch.setColor(0, 0, 0, OPACITY);
    }

    public void end(SpriteBatch batch) {
        batch.setShader(null);
        batch.setColor(1, 1, 1, 1);
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
    }

    /**
     * A flyer's shadow: {@code region} centred on the screen point (x, y) plus the layer's offset
     * (dx right, dy down), at the shadow scale times {@code scale}; between {@link #begin} and
     * {@link #end}.
     */
    public static void draw(SpriteBatch batch, TextureRegion region, float x, float y, int dx, int dy, float scale) {
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();
        float s = SCALE * scale;
        batch.draw(
                region,
                Math.round(x + dx - width / 2),
                Math.round(y - dy - height / 2),
                width / 2,
                height / 2,
                width,
                height,
                s,
                s,
                0);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
