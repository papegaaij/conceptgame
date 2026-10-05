package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import vanguard.game.settings.Scaling;
import vanguard.game.settings.VideoSettings;

/**
 * The 960x540 internal screen: scenes draw into an off-screen frame buffer, which is then shown in
 * the window by the Video tab's scaling (design/art-direction, Internal resolution): the largest
 * integer scale that fits (nearest neighbour, letterboxed), or sharp-bilinear, which fills the
 * window at any scale (a shader that samples as an integer pre-scale followed by bilinear
 * filtering would), with black bars only where the window's aspect ratio is not 16:9. Optional
 * scanlines darken the lower half of every pixel row once the scale is 2 or more.
 */
public final class PixelScreen implements Disposable {
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    /** Left edge of the 480x540 play field; the HUD panels fill the 240 px on either side. */
    public static final int PLAY_FIELD_X = 240;

    private static final float SCANLINE_DARKNESS = 0.35f;

    private static final String VERTEX = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """;
    /** Sharp-bilinear: within a source pixel the colour is flat, only its outer edge blends. */
    private static final String SHARP_BILINEAR = """
            #ifdef GL_ES
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec2 u_textureSize;
            uniform float u_scale;
            void main() {
                vec2 texel = v_texCoords * u_textureSize;
                vec2 centreDistance = fract(texel) - 0.5;
                float plateau = 0.5 - 0.5 / u_scale;
                vec2 f = (centreDistance - clamp(centreDistance, -plateau, plateau)) * u_scale + 0.5;
                gl_FragColor = v_color * texture2D(u_texture, (floor(texel) + f) / u_textureSize);
            }
            """;

    /** With a stencil buffer (and depth, for the packed depth-stencil fallback): the level's shadow mask. */
    private final FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, WIDTH, HEIGHT, true, true);

    private final OrthographicCamera internalCamera = new OrthographicCamera();
    private final OrthographicCamera windowCamera = new OrthographicCamera();
    private final ShaderProgram sharpBilinear = new ShaderProgram(VERTEX, SHARP_BILINEAR);
    private final Texture scanlines = scanlineTexture();
    private Scaling filtered;

    public PixelScreen() {
        if (!sharpBilinear.isCompiled()) {
            throw new IllegalStateException("sharp-bilinear shader: " + sharpBilinear.getLog());
        }
        filter(Scaling.INTEGER);
        internalCamera.setToOrtho(false, WIDTH, HEIGHT);
    }

    /** One clear and one dark row, repeated once per pixel row of the internal screen. */
    private static Texture scanlineTexture() {
        var pixmap = new Pixmap(1, 2, Pixmap.Format.RGBA8888);
        pixmap.drawPixel(0, 0, Color.rgba8888(0, 0, 0, 0));
        pixmap.drawPixel(0, 1, Color.rgba8888(0, 0, 0, SCANLINE_DARKNESS));
        var texture = new Texture(pixmap);
        texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        pixmap.dispose();
        return texture;
    }

    private void filter(Scaling scaling) {
        if (scaling != filtered) {
            TextureFilter filter = scaling == Scaling.INTEGER ? TextureFilter.Nearest : TextureFilter.Linear;
            frameBuffer.getColorBufferTexture().setFilter(filter, filter);
            filtered = scaling;
        }
    }

    /** Starts drawing into the internal screen; {@code batch} must not be drawing. */
    public void begin(SpriteBatch batch) {
        frameBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(internalCamera.combined);
        batch.begin();
    }

    /**
     * Reads the internal screen as drawn so far, bottom row first (as frame buffers store it), RGBA
     * with the alpha the blending left; call between {@link #begin} and {@link #end}, {@code batch}
     * drawing. The caller disposes the pixmap.
     */
    public Pixmap read(SpriteBatch batch) {
        batch.flush();
        frameBuffer.bind();
        var pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(0, 0, WIDTH, HEIGHT, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixmap.getPixels());
        return pixmap;
    }

    /** Ends the internal screen and draws it into the window with the video settings. */
    public void end(SpriteBatch batch, VideoSettings video) {
        batch.end();
        frameBuffer.end();

        int windowWidth = Gdx.graphics.getBackBufferWidth();
        int windowHeight = Gdx.graphics.getBackBufferHeight();
        float fit = Math.min((float) windowWidth / WIDTH, (float) windowHeight / HEIGHT);
        float scale = video.scaling() == Scaling.INTEGER ? Math.max(1, (int) fit) : fit;
        int width = Math.round(WIDTH * scale);
        int height = Math.round(HEIGHT * scale);
        int x = (windowWidth - width) / 2;
        int y = (windowHeight - height) / 2;

        Gdx.gl.glViewport(0, 0, windowWidth, windowHeight);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        windowCamera.setToOrtho(false, windowWidth, windowHeight);
        batch.setProjectionMatrix(windowCamera.combined);
        filter(video.scaling());
        batch.disableBlending();
        if (video.scaling() == Scaling.SHARP_BILINEAR) {
            batch.setShader(sharpBilinear);
        }
        batch.begin();
        if (video.scaling() == Scaling.SHARP_BILINEAR) {
            sharpBilinear.setUniformf("u_textureSize", WIDTH, HEIGHT);
            sharpBilinear.setUniformf("u_scale", Math.max(1, scale));
        }
        Texture texture = frameBuffer.getColorBufferTexture();
        // Frame buffer textures are stored bottom-up: flip v.
        batch.draw(texture, x, y, width, height, 0, 0, WIDTH, HEIGHT, false, true);
        batch.end();
        batch.setShader(null);
        batch.enableBlending();
        if (video.scanlines() && scale >= 2) {
            batch.begin();
            batch.draw(scanlines, x, y, width, height, 0, 0, 1, HEIGHT);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        scanlines.dispose();
        sharpBilinear.dispose();
        frameBuffer.dispose();
    }
}
