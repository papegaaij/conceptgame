package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.content.campaign.Campaign;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.hangar.Names;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.Glass;

/**
 * The game over screen (design/systems/retry: hard, no retry left), a placeholder after the chosen
 * game-over-r08-a: the game over cue, the campaign's stats, and confirm back to the main menu,
 * where the last save can be loaded. Not built yet: Okafor's last transmission (no text in the
 * design yet) and the top-10 with its name entry.
 */
public final class GameOverScreen implements GameScreen {
    private static final float CUE_VOLUME = 0.8f;
    private static final int PANEL_X = 280;
    private static final int PANEL_Y = 200;
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 196;

    private final GameServices services;
    private final Campaign campaign;
    private final String mission;

    /** @param mission the mission that ended the campaign, "MISSION 01 - BREAK AT DAWN" */
    GameOverScreen(GameServices services, Campaign campaign, String mission) {
        this.services = services;
        this.campaign = campaign;
        this.mission = mission;
        services.sfx.play(Sfx.GAME_OVER, CUE_VOLUME, 1, 0);
    }

    @Override
    public Transition update(float seconds) {
        if (services.menu.confirm() || services.menu.back()) {
            services.play(Sfx.MENU_CONFIRM);
            return Transition.replace(MainMenuScreen.menu(services));
        }
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.25f);
        var heading = glass.fonts.heading;
        heading.getData().setScale(2);
        glass.centred(batch, heading, "GAME OVER", Glass.ALERT, PixelScreen.WIDTH / 2f, 96);
        heading.getData().setScale(1);
        glass.panel(batch, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, 0.85f);
        int x = PANEL_X + 18;
        int right = PANEL_X + PANEL_WIDTH - 18;
        glass.header(batch, "CAMPAIGN", x, right, PANEL_Y + 14);
        String[][] rows = {
            {"DIFFICULTY", campaign.difficulty().name()},
            {"ENDED IN", mission},
            {"SCORE", Names.grouped(campaign.score())},
            {"KILLS", Names.grouped(campaign.kills())},
            {"SHIPS LOST", Integer.toString(campaign.deaths())},
            {"PLAYTIME", SlotsScreen.playtime(campaign.playtime())}
        };
        int y = PANEL_Y + 34;
        for (String[] row : rows) {
            glass.shadowed(batch, glass.fonts.label, row[0], Glass.LABEL, x, y + 4);
            glass.right(batch, glass.fonts.body, row[1], Glass.WHITE, right, y);
            y += 24;
        }
        glass.centred(
                batch,
                glass.fonts.label,
                "NO RETRIES LEFT. LOAD YOUR LAST SAVE FROM THE MAIN MENU.",
                Glass.BODY,
                PixelScreen.WIDTH / 2f,
                PANEL_Y + PANEL_HEIGHT + 18);
        glass.hints(batch, "ENTER MAIN MENU");
    }

    @Override
    public void dispose() {
        services.sfx.stop(Sfx.GAME_OVER);
    }
}
