package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Glass;

/**
 * A placeholder for the credits (design/ui/credits): the scrolling credits with the CC-BY
 * attributions come with M6. Back returns to the menu.
 */
public final class CreditsScreen implements GameScreen {
    private final GameServices services;

    public CreditsScreen(GameServices services) {
        this.services = services;
    }

    @Override
    public Transition update(float seconds) {
        if (services.menu.back() || services.menu.confirm()) {
            services.play(Sfx.MENU_BACK);
            return Transition.BACK;
        }
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.5f);
        glass.panel(batch, 240, 170, 480, 170);
        float centre = PixelScreen.WIDTH / 2f;
        glass.centred(batch, glass.fonts.heading, "CREDITS", Glass.AMBER, centre, 192);
        glass.centred(batch, glass.fonts.body, "TERRAN VANGUARD", Glass.WHITE, centre, 240);
        glass.centred(
                batch, glass.fonts.label, "THE CREDITS ROLL ARRIVES WITH THE RELEASE (M6).", Glass.LABEL, centre, 276);
        glass.centred(batch, glass.fonts.label, "THIRD-PARTY ASSETS: SEE CREDITS.MD", Glass.LABEL, centre, 296);
        glass.hints(batch, "ESC BACK");
    }

    @Override
    public void dispose() {}
}
