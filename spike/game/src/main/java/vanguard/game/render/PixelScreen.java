package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

/**
 * The 960x540 internal screen: scenes draw into an off-screen frame buffer, which is then shown
 * at the largest integer scale that fits the window (nearest neighbour, letterboxed).
 */
public final class PixelScreen implements Disposable {
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    /** Left edge of the 480x540 play field; the HUD panels fill the 240 px on either side. */
    public static final int PLAY_FIELD_X = 240;

    private final FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, WIDTH, HEIGHT, false);
    private final OrthographicCamera internalCamera = new OrthographicCamera();
    private final OrthographicCamera windowCamera = new OrthographicCamera();

    public PixelScreen() {
        frameBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        internalCamera.setToOrtho(false, WIDTH, HEIGHT);
    }

    /** Starts drawing into the internal screen; {@code batch} must not be drawing. */
    public void begin(SpriteBatch batch) {
        frameBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(internalCamera.combined);
        batch.begin();
    }

    /** Ends the internal screen and draws it scaled into the window. */
    public void end(SpriteBatch batch) {
        batch.end();
        frameBuffer.end();

        int windowWidth = Gdx.graphics.getBackBufferWidth();
        int windowHeight = Gdx.graphics.getBackBufferHeight();
        int scale = Math.max(1, Math.min(windowWidth / WIDTH, windowHeight / HEIGHT));
        int x = (windowWidth - WIDTH * scale) / 2;
        int y = (windowHeight - HEIGHT * scale) / 2;

        Gdx.gl.glViewport(0, 0, windowWidth, windowHeight);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        windowCamera.setToOrtho(false, windowWidth, windowHeight);
        batch.setProjectionMatrix(windowCamera.combined);
        batch.disableBlending();
        batch.begin();
        Texture texture = frameBuffer.getColorBufferTexture();
        // Frame buffer textures are stored bottom-up: flip v.
        batch.draw(texture, x, y, WIDTH * scale, HEIGHT * scale, 0, 0, WIDTH, HEIGHT, false, true);
        batch.end();
        batch.enableBlending();
    }

    @Override
    public void dispose() {
        frameBuffer.dispose();
    }
}
