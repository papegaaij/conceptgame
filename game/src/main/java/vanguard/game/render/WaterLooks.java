package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import java.nio.IntBuffer;
import vanguard.sim.PlayField;

/**
 * M5 part E: the generic {@code sub} pass (design/art-direction, Water; user decision E4 = c, the
 * hybrid): everything on the {@code sub} layer (submerged jellies, a raft's and a jelly's under-water
 * body, the Kraken's head, mantle and arms under the surface, the sunken pod, torpedoes and their
 * bubbles, under-water bursts) is drawn plain between {@link #begin} and {@link #end}, into a frame
 * buffer the size of the play field, which is then laid over the deep layer through one shader:
 * tinted toward the water, darker, a little fainter, softened by a small blur and swaying row by row
 * like the refraction of a gently moving surface. Foam collars, ripples and the surfacing steps are
 * pre-rendered per unit (tools/art/driftjelly.py, water_fx.py, harbour_kraken.py) and drawn on the
 * surface after it. The look is the art scripts' stand-in ({@code driftjelly.sub_pass}): rgb × (1 −
 * k) × 0.85 + tint × k with k = 0.45, alpha × 0.85, about a 0.7 px blur and a 0.7 px sway.
 *
 * <p>Both shaders are GLSL 1.10 / GLSL ES 1.00 like every shader of the game (no {@code #version}),
 * so the pass runs on the GL 2.1 contexts of all three desktop systems. Without a frame buffer (the
 * driver refuses one) it falls back to drawing each sprite straight through a tint shader with the
 * same colour sums: no blur and no sway, but the same water colour. One frame buffer clear and one
 * full-field draw per frame; nothing is allocated while drawing.
 */
public final class WaterLooks implements Disposable {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The water's colour the submerged parts are tinted toward (the art scripts' {@code WATER_TINT}). */
    static final Color TINT = new Color(6 / 255f, 40 / 255f, 78 / 255f, 1);
    /** How strongly a submerged part is tinted toward the water. */
    static final float TINT_SHARE = 0.45f;
    /** How much darker it is, before the tint. */
    static final float DARKEN = 0.85f;
    /** How opaque it is. */
    static final float OPACITY = 0.85f;
    /** The row-wise sway's amplitude, px, its spatial frequency, radians a row, and its speed, radians a second. */
    static final float SWAY_PX = 0.7f;

    static final float SWAY_FREQUENCY = 0.5f;
    static final float SWAY_SPEED = 5f;

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
    /**
     * Lays the frame buffer (premultiplied: drawn into with a separate alpha blend) over the field: a
     * row-wise sway, a 3×3 blur (centre 0.4, edges 0.1, corners 0.05), then the water's colour sums.
     */
    private static final String COMPOSITE = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec2 u_texel;
            uniform float u_time;
            uniform vec3 u_tint;
            uniform vec4 u_look;
            uniform vec2 u_wave;
            void main() {
                float row = floor(v_texCoords.y / u_texel.y);
                float shift = u_look.w * sin(u_wave.x * row + u_wave.y * u_time);
                vec2 uv = v_texCoords + vec2(shift * u_texel.x, 0.0);
                vec2 dx = vec2(u_texel.x, 0.0);
                vec2 dy = vec2(0.0, u_texel.y);
                vec4 c = texture2D(u_texture, uv) * 0.4;
                c += (texture2D(u_texture, uv + dx) + texture2D(u_texture, uv - dx)
                        + texture2D(u_texture, uv + dy) + texture2D(u_texture, uv - dy)) * 0.1;
                c += (texture2D(u_texture, uv + dx + dy) + texture2D(u_texture, uv + dx - dy)
                        + texture2D(u_texture, uv - dx + dy) + texture2D(u_texture, uv - dx - dy)) * 0.05;
                vec3 rgb = c.rgb * (1.0 - u_look.x) * u_look.y + u_tint * u_look.x * c.a;
                gl_FragColor = vec4(rgb, c.a) * u_look.z;
            }
            """;
    /** The fallback: each sprite through the same colour sums, straight onto the screen. */
    private static final String TINTED = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec3 u_tint;
            uniform vec4 u_look;
            void main() {
                vec4 texel = v_color * texture2D(u_texture, v_texCoords);
                vec3 rgb = texel.rgb * (1.0 - u_look.x) * u_look.y + u_tint * u_look.x;
                gl_FragColor = vec4(rgb, texel.a * u_look.z);
            }
            """;

    private final FrameBuffer buffer;
    private final ShaderProgram composite;
    private final ShaderProgram tinted;
    private final Matrix4 fieldProjection = new Matrix4();
    private final Matrix4 saved = new Matrix4();
    private final IntBuffer boundFramebuffer = BufferUtils.newIntBuffer(16);
    private final IntBuffer boundViewport = BufferUtils.newIntBuffer(16);
    private boolean drawing;

    /** The pass for a level over water: its frame buffer (or the fallback without one) and shaders. */
    public WaterLooks() {
        tinted = compiled(new ShaderProgram(VERTEX, TINTED), "sub tint");
        ShaderProgram program = new ShaderProgram(VERTEX, COMPOSITE);
        FrameBuffer made = null;
        if (program.isCompiled()) {
            try {
                made = new FrameBuffer(Pixmap.Format.RGBA8888, PlayField.WIDTH, PlayField.HEIGHT, false);
                made.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                made.getColorBufferTexture().setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            } catch (RuntimeException refused) {
                // A driver without this frame buffer: the per-sprite fallback.
                Gdx.app.log("WaterLooks", "no frame buffer for the sub pass, tinting per sprite: " + refused);
            }
        } else {
            Gdx.app.log("WaterLooks", "sub composite shader: " + program.getLog());
        }
        if (made == null) {
            program.dispose();
            program = null;
        }
        buffer = made;
        composite = program;
        fieldProjection.setToOrtho2D(X0, 0, PlayField.WIDTH, PlayField.HEIGHT);
    }

    private static ShaderProgram compiled(ShaderProgram program, String name) {
        if (!program.isCompiled()) {
            throw new IllegalStateException(name + " shader: " + program.getLog());
        }
        return program;
    }

    /** Whether the pass blurs and sways (a frame buffer exists), not the per-sprite fallback. */
    public boolean buffered() {
        return buffer != null;
    }

    /**
     * What is drawn until {@link #end} is on the {@code sub} layer: plain sprites at their play-field
     * places (the batch's usual coordinates), alpha-blended. {@code batch} must be drawing.
     */
    public void begin(SpriteBatch batch) {
        drawing = true;
        if (buffer == null) {
            batch.setShader(tinted);
            look(tinted);
            return;
        }
        batch.end();
        saved.set(batch.getProjectionMatrix());
        // FrameBuffer.end() binds the window, not the frame buffer bound before (the pixel screen's).
        Gdx.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, boundFramebuffer);
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, boundViewport);
        buffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(fieldProjection);
        batch.begin();
        // Colour blended as usual, alpha accumulated as coverage: the buffer holds premultiplied colour.
        batch.setBlendFunctionSeparate(
                GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Lays the {@code sub} layer over what is below it at {@code seconds} (real time: the sway). */
    public void end(SpriteBatch batch, double seconds) {
        if (!drawing) {
            return;
        }
        drawing = false;
        if (buffer == null) {
            batch.setShader(null);
            return;
        }
        batch.end();
        buffer.end();
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, boundFramebuffer.get(0));
        Gdx.gl.glViewport(boundViewport.get(0), boundViewport.get(1), boundViewport.get(2), boundViewport.get(3));
        batch.setProjectionMatrix(saved);
        batch.setShader(composite);
        batch.begin();
        look(composite);
        composite.setUniformf("u_texel", 1f / PlayField.WIDTH, 1f / PlayField.HEIGHT);
        composite.setUniformf("u_time", (float) (seconds % (2 * Math.PI * 100)));
        composite.setUniformf("u_wave", SWAY_FREQUENCY, SWAY_SPEED);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(
                buffer.getColorBufferTexture(),
                X0,
                0,
                PlayField.WIDTH,
                PlayField.HEIGHT,
                0,
                0,
                PlayField.WIDTH,
                PlayField.HEIGHT,
                false,
                true);
        batch.setShader(null);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void look(ShaderProgram program) {
        program.setUniformf("u_tint", TINT.r, TINT.g, TINT.b);
        program.setUniformf("u_look", TINT_SHARE, DARKEN, OPACITY, SWAY_PX);
    }

    /**
     * The colour a plain texel (straight alpha, 0..1) shows through the pass, before the blur: the
     * shaders' sums, for the tests and as their reference.
     */
    static float[] tinted(float r, float g, float b, float a) {
        float keep = (1 - TINT_SHARE) * DARKEN;
        return new float[] {
            r * keep + TINT.r * TINT_SHARE, g * keep + TINT.g * TINT_SHARE, b * keep + TINT.b * TINT_SHARE, a * OPACITY
        };
    }

    /** The row-wise sway at a play-field pixel row and {@code seconds}, px (the composite shader's). */
    static double sway(int row, double seconds) {
        return SWAY_PX * Math.sin(SWAY_FREQUENCY * row + SWAY_SPEED * seconds);
    }

    @Override
    public void dispose() {
        tinted.dispose();
        if (composite != null) {
            composite.dispose();
        }
        if (buffer != null) {
            buffer.dispose();
        }
    }
}
