package vanguard.game;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.Disposable;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.audio.Mixer;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.SfxBank;
import vanguard.game.display.DisplayModes;
import vanguard.game.input.ActionInput;
import vanguard.game.input.GdxDevices;
import vanguard.game.input.MenuInput;
import vanguard.game.render.FlashShader;
import vanguard.game.render.Sprites;
import vanguard.game.render.TransmissionStatic;
import vanguard.game.settings.Settings;
import vanguard.game.settings.SettingsStore;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;
import vanguard.game.ui.TitleScene;

/**
 * What every screen shares for the whole run: files and audio, the input devices and actions, the
 * settings, the display switcher, the launch difficulty, the game's content (the design data) with
 * the campaign's rules, the save slots, the sprite atlases, the mixer and the sound effects, the UI
 * kit with its fonts and the title scene, the flash shader and the portraits' transmission static.
 */
public final class GameServices implements Disposable {
    private static final Logger LOG = Logger.getLogger(GameServices.class.getName());

    /** The menu sounds' level. */
    private static final float MENU_VOLUME = 0.5f;

    public final Files files;
    public final Audio audio;
    public final GdxDevices devices;
    public final ActionInput input;
    public final MenuInput menu;
    public final DisplayModes display;
    /** The difficulty of {@code --difficulty}: the bench flies it, the difficulty select starts on it. */
    public final Difficulty difficulty;
    /** A debug option for testing ({@code --invulnerable}): nothing hits the ship. */
    public final boolean invulnerable;

    public final Content content;
    public final CampaignRules campaignRules;
    /** What the hangar shop sells, with its sell-back and repair rules. */
    public final Catalogue catalogue;
    /** The save slots in the saves directory next to the settings file. */
    public final SaveSlots saves;

    public final Sprites sprites;
    public final Mixer mixer;
    public final SfxBank sfx;
    public final Fonts fonts;
    public final Glass glass;
    public final TitleScene titleScene;

    public final FlashShader flash;
    public final TransmissionStatic transmissionStatic;

    private final SettingsStore store;
    private Settings settings;

    GameServices(
            Files files,
            Audio audio,
            DisplayModes display,
            Settings settings,
            SettingsStore store,
            Difficulty difficulty,
            boolean invulnerable,
            SaveSlots saves) {
        this.files = files;
        this.audio = audio;
        this.display = display;
        this.settings = settings;
        this.store = store;
        this.difficulty = difficulty;
        this.invulnerable = invulnerable;
        devices = new GdxDevices();
        devices.deadZone(settings.controls().deadZone());
        input = new ActionInput(settings.controls().bindings());
        menu = new MenuInput(input);
        content = ContentLoader.fromClasspath();
        campaignRules = CampaignRules.of(content);
        catalogue = Catalogue.of(content);
        this.saves = saves;
        sprites = new Sprites(files);
        mixer = new Mixer(settings.audio());
        sfx = new SfxBank(audio, files, mixer);
        fonts = new Fonts(files);
        glass = new Glass(fonts, sprites);
        titleScene = new TitleScene(files);
        flash = new FlashShader();
        transmissionStatic = new TransmissionStatic();
    }

    public Settings settings() {
        return settings;
    }

    /** Applies changed settings at once: bindings, dead zone and volumes; the screens read the rest each frame. */
    public void change(Settings changed) {
        settings = changed;
        input.rebind(changed.controls().bindings());
        devices.deadZone(changed.controls().deadZone());
        mixer.set(changed.audio());
    }

    /** Writes the settings to the settings file. */
    public void saveSettings() {
        store.save(settings);
    }

    /**
     * Writes the campaign into a save slot; a failure is logged, since it must not stop the game.
     *
     * @return whether the save was written
     */
    public boolean save(SaveSlots.Slot slot, Campaign campaign) {
        try {
            saves.write(slot, campaign.save(Instant.now()));
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "could not write save slot " + slot.index() + " in " + saves.directory(), e);
            return false;
        }
    }

    /** Plays a menu sound. */
    public void play(Sfx sound) {
        sfx.play(sound, MENU_VOLUME, 1, 0);
    }

    @Override
    public void dispose() {
        transmissionStatic.dispose();
        flash.dispose();
        titleScene.dispose();
        fonts.dispose();
        sfx.dispose();
        sprites.dispose();
    }
}
