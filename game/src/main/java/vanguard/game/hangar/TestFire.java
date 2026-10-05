package vanguard.game.hangar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.content.WeaponData;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.sim.Armament;
import vanguard.sim.Command;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PlayField;
import vanguard.sim.Rules;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * The hangar's test fire (design/ui/hangar, Test fire): a looping mini-sortie of one weapon at one
 * upgrade level in the real simulation. The ship, with only that weapon fitted in its slot, climbs to
 * the middle of the play field and holds fire while three dummy targets on the ground drift down past
 * it on the scroll: one on the weapon's line and one either side of it, so a forward gun meets them
 * ahead, a side gun beside the ship, a rear gun behind it, a bomb under its pod, a mortar shell on
 * its snap and a homing missile on its turn. The dummies take {@value #HITS_TO_KILL} of the weapon's
 * level-1 hits (one level-1 blast of a bomb or shell), so a higher level kills them sooner. When all three are gone the loop starts over
 * from the same state (the sortie's own retry), so every loop is the same. Nothing hits the ship.
 * Stepping allocates nothing beyond the simulation's own rules.
 */
public final class TestFire {
    /** The weapon shown: its slug, the slot it fires from and its upgrade level (1-5). */
    public record Shown(String weapon, Armament.Slot slot, int level) {}

    /** The ground scrolls at this speed, px/s: the dummies drift down past the ship. */
    static final double SCROLL = 90;
    /** The ship holds this height, the middle of the field, so the dummies pass ahead, beside and behind it. */
    static final double SHIP_Y = PlayField.HEIGHT / 2.0;
    /** The ship's centre line (its start x). */
    static final double SHIP_X = PlayField.WIDTH / 2.0;
    /** Before a loop shows, the ship climbs from its start to {@link #SHIP_Y} and settles, unseen. */
    static final int CLIMB_TICKS = SimStep.ticks(1.5);
    /** The ship stops climbing this far below its height: it brakes the rest. */
    private static final double CLIMB_BRAKE = 6;
    /** The dummies, px right of the weapon's line, in the order they enter. */
    static final double[] LANES = {0, -36, 36};
    /** When each dummy enters at the top edge, s after the climb. */
    static final double[] ENTRIES = {0, 0.6, 1.2};
    /** A wing weapon's line is this far out from the centre line, towards its pod (the pods sit at ±16 px). */
    static final double WING_LINE = 12;
    /** A dummy's hit box edge, px. */
    static final double TARGET_SIZE = 20;
    /** How many of the weapon's level-1 hits destroy a dummy. */
    static final double HITS_TO_KILL = 2;
    /** A bomb or shell blast at level 1 destroys a dummy outright: a pass under the pod gives only one. */
    static final double BLASTS_TO_KILL = 1;
    /** After the last dummy is gone, the loop shows this long more (its explosion) before it starts over. */
    static final int TAIL_TICKS = SimStep.ticks(0.6);
    /** A loop never runs longer than this, ticks after the climb (the last dummy is off the field by then). */
    static final int MAX_LOOP_TICKS =
            SimStep.ticks(ENTRIES[ENTRIES.length - 1] + (PlayField.HEIGHT + TARGET_SIZE) / SCROLL) + TAIL_TICKS;
    /** The dummies' slug: the game draws them as targets, not as an enemy of the design. */
    public static final String DUMMY = "test-fire-dummy";

    private static final int FIRE = Command.FIRE.bit();
    private static final int CLIMB = Command.UP.bit();
    private static final double LEVEL_SECONDS = 3600;
    private static final long SEED = 1;

    private final Shown shown;
    private final Sortie sortie;
    private final double armour;
    private final double line;
    /** Steps of the loop shown so far, from the end of the climb. */
    private int loopTicks;
    /** Steps since the field emptied; -1 while dummies are on it or still to come. */
    private int emptyTicks = -1;

    public TestFire(Content content, Shown shown) {
        this.shown = shown;
        Loadout loadout = SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(new SimSpecs.FittedWeapon(shown.slot(), shown.weapon(), shown.level())),
                content.shields().models().getFirst().name(),
                content.armour().plating().getFirst().name(),
                0,
                Difficulty.MEDIUM);
        armour = loadout.plating().maxArmour();
        line = line(shown.slot());
        Rules rules = SimSpecs.rules(content, content.levelKey(1).orElseThrow(), Difficulty.MEDIUM)
                .withInvulnerableShip();
        sortie = new Sortie(SEED, loadout, script(content.weapon(shown.weapon()), line), rules, armour);
        climb();
    }

    /**
     * What the shop's selection shows: a weapon the simulation flies, in the selected slot, at
     * {@code level}; empty for any other item.
     */
    public static Optional<Shown> of(Content content, LoadoutSlot slot, String item, int level) {
        Armament.Slot weaponSlot =
                switch (slot) {
                    case FRONT -> Armament.Slot.FRONT;
                    case REAR -> Armament.Slot.REAR;
                    case LEFT_WING -> Armament.Slot.LEFT_WING;
                    case RIGHT_WING -> Armament.Slot.RIGHT_WING;
                    default -> null;
                };
        if (weaponSlot == null || !content.weapons().containsKey(item) || !SimSpecs.flies(content, item)) {
            return Optional.empty();
        }
        int levels = content.weapon(item).levels().size();
        return Optional.of(new Shown(item, weaponSlot, Math.clamp(level, 1, levels)));
    }

    /** The weapon's line: the centre line, or out towards a wing weapon's pod. */
    static double line(Armament.Slot slot) {
        return switch (slot) {
            case FRONT, REAR -> 0;
            case LEFT_WING -> -WING_LINE;
            case RIGHT_WING -> WING_LINE;
        };
    }

    /** The test range: one endless section on the scroll and the three dummies, entering after the climb. */
    static LevelScript script(WeaponData weapon, double line) {
        boolean landing = weapon.hits().equals("ground-only");
        double hp = (landing ? BLASTS_TO_KILL : HITS_TO_KILL)
                * weapon.levels().getFirst().damage();
        EnemySpec dummy = new EnemySpec(
                DUMMY,
                hp,
                new Hitbox(TARGET_SIZE, TARGET_SIZE),
                Layer.GROUND,
                0,
                false,
                0,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
        double climb = CLIMB_TICKS * SimStep.SECONDS;
        List<LevelScript.GroundUnit> dummies = new ArrayList<>();
        for (int i = 0; i < LANES.length; i++) {
            dummies.add(new LevelScript.GroundUnit(climb + ENTRIES[i], SHIP_X + line + LANES[i], dummy, -1));
        }
        return new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(LEVEL_SECONDS, SCROLL)),
                List.of(),
                List.of(),
                dummies,
                0,
                List.of(),
                new LevelScript.Secondary(1, 0),
                List.of());
    }

    /** The ship climbs to its height without firing; the dummies enter right after. */
    private void climb() {
        for (int i = 0; i < CLIMB_TICKS; i++) {
            sortie.step(sortie.ship().y() < SHIP_Y - CLIMB_BRAKE ? CLIMB : Command.NONE);
        }
        loopTicks = 0;
        emptyTicks = -1;
    }

    /**
     * One step of the loop with fire held; once the dummies are gone (and their explosions shown)
     * the loop starts over, climb included, in this step.
     *
     * @return whether the loop started over: what was drawn of the last one is gone
     */
    public boolean step() {
        if (emptyTicks >= TAIL_TICKS || loopTicks >= MAX_LOOP_TICKS) {
            sortie.retry(armour);
            climb();
            return true;
        }
        sortie.step(FIRE);
        loopTicks++;
        boolean entered = loopTicks > SimStep.ticks(ENTRIES[ENTRIES.length - 1]);
        if (entered && sortie.enemyCount() == 0) {
            emptyTicks++;
        }
        return false;
    }

    public Shown shown() {
        return shown;
    }

    /** The mini-sortie, for the drawing. */
    public Sortie sortie() {
        return sortie;
    }

    /** Steps of the current loop since the climb. */
    public int loopTicks() {
        return loopTicks;
    }

    /** The weapon's line: px right of the ship's centre line, where the middle dummy drifts down. */
    public double line() {
        return line;
    }
}
