package vanguard.game.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.SoundTest;
import vanguard.game.input.Action;
import vanguard.game.input.ControlSettings;
import vanguard.game.settings.AudioSettings;
import vanguard.game.settings.GameplaySettings;
import vanguard.game.settings.Scaling;
import vanguard.game.settings.Settings;
import vanguard.game.settings.VideoSettings;

/** The four tabs of design/ui/options and their rows. */
final class OptionTabs {
    /** A tab: its chip's name, its rows and a note under them. */
    record Tab(String name, List<OptionRow> rows, String note) {}

    static final List<String> OFF_ON = List.of("OFF", "ON");

    private OptionTabs() {}

    static List<Tab> all() {
        return List.of(video(), audio(), controls(), gameplay());
    }

    private static Tab video() {
        return new Tab(
                "VIDEO",
                List.of(
                        new OptionRow.Choice(
                                "DISPLAY MODE",
                                List.of("FULL SCREEN", "WINDOW"),
                                target -> target.fullScreen() ? 0 : 1,
                                (target, chip) -> target.toggleFullScreen(),
                                List.of(
                                        "BORDERLESS AT THE DESKTOP RESOLUTION. ALT+ENTER OR F11 TOGGLES.",
                                        "RESIZABLE. ALT+ENTER OR F11 TOGGLES.")),
                        new OptionRow.Choice(
                                "SCALING",
                                List.of("INTEGER + LETTERBOX", "SHARP-BILINEAR"),
                                target -> target.settings().video().scaling().ordinal(),
                                (target, chip) ->
                                        video(target, target.settings().video().withScaling(Scaling.values()[chip])),
                                List.of("CRISP PIXELS, BLACK BORDERS ROUND IT.", "FILLS THE SCREEN, SLIGHTLY SOFT.")),
                        new OptionRow.Choice(
                                "CRT SCANLINES",
                                OFF_ON,
                                target -> target.settings().video().scanlines() ? 1 : 0,
                                (target, chip) ->
                                        video(target, target.settings().video().withScanlines(chip == 1)),
                                List.of("", "FROM A 2X SCALE (1920X1080) UP."))),
                "");
    }

    private static void video(OptionsTarget target, VideoSettings changed) {
        target.change(target.settings().withVideo(changed));
    }

    private static Tab audio() {
        return new Tab(
                "AUDIO",
                List.of(
                        volume("MASTER", AudioSettings::master, AudioSettings::withMaster, Sfx.MENU_MOVE),
                        volume("MUSIC", AudioSettings::music, AudioSettings::withMusic, Sfx.MENU_MOVE),
                        volume("EFFECTS", AudioSettings::effects, AudioSettings::withEffects, Sfx.MENU_MOVE),
                        volume("RADIO BLIPS", AudioSettings::radio, AudioSettings::withRadio, Sfx.TYPEWRITER),
                        volume("VOICE", AudioSettings::voice, AudioSettings::withVoice, Sfx.MENU_MOVE),
                        new OptionRow.Sound("SOUND TEST: MUSIC", SoundTest.Kind.MUSIC),
                        new OptionRow.Sound("SOUND TEST: EFFECTS", SoundTest.Kind.EFFECTS)),
                "SOUND TEST: LEFT/RIGHT PICKS, ENTER PLAYS; ENTER AGAIN STOPS A TRACK. VOICE IS THE SPOKEN RADIO"
                        + " AND BRIEFINGS; THE SUBTITLES STAY.");
    }

    private static OptionRow volume(
            String label,
            ToDoubleFunction<AudioSettings> get,
            BiFunction<AudioSettings, Double, AudioSettings> set,
            Sfx feedback) {
        return new OptionRow.Slider(
                label,
                0,
                1,
                0.05,
                settings -> get.applyAsDouble(settings.audio()),
                (settings, volume) -> settings.withAudio(set.apply(settings.audio(), volume)),
                OptionTabs::percent,
                feedback);
    }

    private static Tab controls() {
        List<OptionRow> rows = new ArrayList<>();
        Action.REMAPPABLE.forEach(action -> rows.add(new OptionRow.Remap(action)));
        rows.add(new OptionRow.Choice(
                "AUTO-FIRE",
                OFF_ON,
                target -> target.settings().controls().autoFire() ? 1 : 0,
                (target, chip) -> controls(target, target.settings().controls().withAutoFire(chip == 1)),
                List.of("OFF: HOLD FIRE TO SHOOT.", "ON: ALWAYS FIRING, NO BUTTON NEEDED.")));
        rows.add(new OptionRow.Slider(
                "STICK DEAD ZONE",
                0.05,
                0.5,
                0.05,
                settings -> settings.controls().deadZone(),
                (settings, share) -> settings.withControls(settings.controls().withDeadZone(share)),
                OptionTabs::percent,
                Sfx.MENU_MOVE));
        rows.add(new OptionRow.Button("RESET TO DEFAULTS", target -> controls(target, ControlSettings.defaults())));
        return new Tab("CONTROLS", rows, "");
    }

    private static void controls(OptionsTarget target, ControlSettings changed) {
        target.change(target.settings().withControls(changed));
    }

    private static Tab gameplay() {
        return new Tab(
                "GAMEPLAY",
                List.of(
                        new OptionRow.Slider(
                                "TEXT SPEED",
                                10,
                                90,
                                10,
                                settings -> settings.gameplay().textSpeed(),
                                (settings, speed) ->
                                        gameplay(settings, settings.gameplay().withTextSpeed(speed.intValue())),
                                speed -> (int) speed + " CPS",
                                Sfx.MENU_MOVE),
                        new OptionRow.Slider(
                                "SCREEN SHAKE",
                                0,
                                1,
                                0.1,
                                settings -> settings.gameplay().screenShake(),
                                (settings, share) ->
                                        gameplay(settings, settings.gameplay().withScreenShake(share)),
                                OptionTabs::percent,
                                Sfx.MENU_MOVE),
                        new OptionRow.Choice(
                                "FLASH REDUCTION",
                                OFF_ON,
                                target -> target.settings().gameplay().flashReduction() ? 1 : 0,
                                (target, chip) -> target.change(gameplay(
                                        target.settings(),
                                        target.settings().gameplay().withFlashReduction(chip == 1))),
                                List.of("", "TONES DOWN THE WHITE HIT AND INVULNERABILITY FLASHES.")),
                        new OptionRow.Choice(
                                "CREDIT NUMBERS",
                                OFF_ON,
                                target -> target.settings().gameplay().creditNumbers() ? 1 : 0,
                                (target, chip) -> target.change(gameplay(
                                        target.settings(),
                                        target.settings().gameplay().withCreditNumbers(chip == 1))),
                                List.of("", "SHOWS THE CREDITS OF EVERY PICKUP WHERE IT WAS TAKEN."))),
                "EDGE WARNINGS FOR SIDE AND REAR WAVES ARE ALWAYS ON (READABILITY RULE). DIFFICULTY IS CHOSEN PER"
                        + " CAMPAIGN WHEN STARTING A NEW GAME.");
    }

    private static Settings gameplay(Settings settings, GameplaySettings changed) {
        return settings.withGameplay(changed);
    }

    private static String percent(double share) {
        return String.format(Locale.ROOT, "%d%%", Math.round(share * 100));
    }
}
