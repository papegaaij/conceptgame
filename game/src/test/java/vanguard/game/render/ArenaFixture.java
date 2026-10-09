package vanguard.game.render;

import java.util.List;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.sim.AllySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

/**
 * M5 part E: a fixture of Level 11's arena for the game's tests while the level's own data is being
 * written (step E3a): the Harbour Kraken from its data file on 3 s of sea at 140 px/s, the arena of 1 s
 * at speed 0 where it arrives, and Level 11's convoy (three cargo ships to lanes 1, 2 and 4, the frigate
 * that leaves) with the cue triggers of its arena, over water. The ships' specs are the allies' data's
 * first values (design/allies: two slams to sink a cargo ship; the frigate cannot be damaged, its flak
 * every 2 s). As content's {@code KrakenTest} builds it.
 */
public final class ArenaFixture {
    public static final Content CONTENT = ContentLoader.fromClasspath();
    /** The rules of an Act 2 level. */
    static final String RULES_LEVEL = "act-2-homefront/level-10-evacuation-corridor";

    public static final double ARENA = 3;
    public static final int HEAD = 0;
    public static final int LEFT_ARM = 1;
    public static final int RIGHT_ARM = 2;
    public static final List<String> SHIPS = List.of("Halvorsen", "Mbeki", "Saint-Laurent", "Ruyter");

    private ArenaFixture() {}

    static AllySpec cargo() {
        return new AllySpec(
                "cargo-ship",
                new Hitbox(56, 120),
                new Hitbox(48, 112),
                2,
                false,
                0,
                0.5,
                0,
                false,
                false,
                0,
                0,
                0,
                1,
                0);
    }

    static AllySpec frigate() {
        return new AllySpec(
                "escort-frigate",
                new Hitbox(40, 110),
                new Hitbox(32, 102),
                1,
                false,
                0,
                0,
                0,
                false,
                false,
                0,
                0,
                0,
                0,
                2);
    }

    static LevelScript.Naval convoy() {
        AllySpec cargo = cargo();
        return new LevelScript.Naval(
                List.of(
                        new LevelScript.NavalUnit(cargo, SHIPS.get(0), 130, PlayField.HEIGHT - 380, 1),
                        new LevelScript.NavalUnit(cargo, SHIPS.get(1), 240, PlayField.HEIGHT - 350, 2),
                        new LevelScript.NavalUnit(cargo, SHIPS.get(2), 350, PlayField.HEIGHT - 380, 4),
                        new LevelScript.NavalUnit(frigate(), SHIPS.get(3), 240, PlayField.HEIGHT - 480, 0)),
                3,
                PlayField.HEIGHT - 450,
                4,
                120);
    }

    static LevelScript.RadioCue cue(LevelScript.CueTrigger trigger, String subject, String line) {
        return new LevelScript.RadioCue(trigger, 0, subject, "Atlas Control", line, false, "neutral");
    }

    public static LevelScript level() {
        return new LevelScript(
                        11,
                        2,
                        0,
                        List.of(
                                new LevelScript.Section(ARENA, 140),
                                new LevelScript.Section(ARENA + 1, 0, true),
                                new LevelScript.Section(ARENA + 16, 140)),
                        List.of(),
                        List.of(),
                        List.of(),
                        0,
                        List.of(
                                cue(LevelScript.CueTrigger.FIRST_TELEGRAPH, "", "Lane's boiling!"),
                                cue(LevelScript.CueTrigger.ALLY_HIT, "", "The {ally} is hit!")),
                        LevelScript.Secondary.afloat(100, "CONVOY"),
                        List.of(),
                        List.of(),
                        List.of(SimSpecs.boss(
                                CONTENT,
                                new LevelData.BossPlacement("harbour-kraken", ARENA, PlayField.WIDTH / 2.0, 2),
                                Difficulty.MEDIUM)))
                .withWater(true)
                .withConvoy(convoy());
    }

    /** The arena flown with the starter loadout and the ship invulnerable (it holds still without input). */
    public static Sortie sortie() {
        return new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                level(),
                SimSpecs.rules(CONTENT, RULES_LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
    }
}
