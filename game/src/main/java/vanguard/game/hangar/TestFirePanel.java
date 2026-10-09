package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.Choice;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.Refusal;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.TestFireView;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;
import vanguard.sim.FixedStepClock;
import vanguard.sim.SimStep;

/**
 * The shop drawer's test-fire box (design/ui/hangar, Test fire), below the selected item: while a
 * weapon is selected it loops a {@link TestFire} of it in the slot being shopped, at the level the
 * detail shows, or at the next one while its UPGRADE choice is highlighted (what the purchase would
 * give), and starts over whenever that changes. The box is the play field turned a quarter
 * ({@link TestFireView}): the ship faces right; one of Rook's guns fires from his craft. Other items
 * show what the box is for.
 */
final class TestFirePanel {
    /** The box: its top-left on the 960x540 screen and its size, under the shop's message lines. */
    static final int X = 26;

    static final int Y = 422;
    static final int WIDTH = 264;
    static final int HEIGHT = 64;
    /** The play-field point at the box's centre: the ship's centre line at the height it holds. */
    static final double CENTRE_X = TestFire.SHIP_X;

    static final double CENTRE_Y = TestFire.SHIP_Y;
    /** The label's place in the box. */
    static final int LABEL_X = X + 4;

    static final int LABEL_Y = Y + 3;
    /** The label's height with its backing, px. */
    static final int LABEL_HEIGHT = 12;
    /**
     * The label for any item without a loop (M5 part E: every weapon flies now, the torpedo over the
     * range's water, so the "not yet in flight" label is gone).
     */
    static final String WEAPONS_ONLY = "TEST FIRE - WEAPONS ONLY";
    /** The most steps one frame catches up on (a stall drops the rest). */
    private static final int MAX_STEPS_PER_FRAME = 4;

    private final Glass glass;
    private final Content content;
    private final TestFireView view;
    private final FixedStepClock clock = new FixedStepClock(SimStep.SECONDS, MAX_STEPS_PER_FRAME);
    /** What the box shows; null while it shows no weapon. */
    private TestFire fire;
    /** The selection the box was made for (a weapon, or the reason there is none). */
    private Optional<TestFire.Shown> shown = Optional.empty();

    private String selectedItem = "";
    private String label = "TEST FIRE";
    private Color labelColour = Glass.DIM;

    /** The label's dark backing over the loop. */
    private static final Color BACKING = new Color(0, 0, 0, 0.55f);

    private final TextureRegion pixel;

    TestFirePanel(Glass glass, Content content, TestFireView view, TextureRegion pixel) {
        this.pixel = pixel;
        this.glass = glass;
        this.content = content;
        this.view = view;
    }

    /** Follows the selection, then runs the loop on the simulation's clock. */
    void update(float seconds, HangarState state) {
        Optional<Offer> offer = state.selected();
        Optional<TestFire.Shown> wanted = offer.flatMap(
                selected -> TestFire.of(content, state.slot(), selected.item().id(), level(state, selected)));
        String item = offer.map(selected -> selected.item().id()).orElse("");
        if (!wanted.equals(shown) || !item.equals(selectedItem)) {
            select(wanted, upgrading(state));
            selectedItem = item;
        }
        if (fire == null) {
            return;
        }
        int steps = clock.advance(seconds);
        for (int i = 0; i < steps; i++) {
            if (fire.step()) {
                view.restart();
            }
            view.stepped(fire.sortie());
        }
    }

    private void select(Optional<TestFire.Shown> wanted, boolean upgrade) {
        shown = wanted;
        if (wanted.isPresent()) {
            TestFire.Shown weapon = wanted.get();
            fire = new TestFire(content, weapon);
            view.show(fire.sortie(), weapon.level(), weapon.escortGun().isPresent());
            label = label(weapon.level(), upgrade);
            labelColour = Glass.AMBER;
            return;
        }
        fire = null;
        labelColour = Glass.DIM;
        label = WEAPONS_ONLY;
    }

    /**
     * The label over a weapon's loop, short (it hides the rear of the ship's left): "TEST FIRE L2",
     * or "TEST FIRE L1>L2" while the upgrade is highlighted.
     */
    static String label(int level, boolean upgrade) {
        return "TEST FIRE " + (upgrade ? "L" + (level - 1) + ">" : "") + "L" + level;
    }

    /**
     * The level the box shows for a row: the level the detail shows, or the next one while the row's
     * UPGRADE choice is highlighted (and not refused for the top level).
     */
    static int level(HangarState state, Offer offer) {
        return offer.level() + (upgrading(state) ? 1 : 0);
    }

    /** Whether the highlighted choice is an upgrade that has a next level. */
    static boolean upgrading(HangarState state) {
        if (state.focus() != HangarState.Focus.SHOP) {
            return false;
        }
        Optional<Choice> choice = state.choice();
        return choice.isPresent()
                && choice.get().action() == Action.UPGRADE
                && !choice.get().refusal().filter(Refusal.MAX_LEVEL::equals).isPresent();
    }

    void draw(SpriteBatch batch) {
        glass.inset(batch, X, Y, WIDTH, HEIGHT);
        if (fire != null) {
            view.draw(batch, fire.sortie(), clock.alpha(), X, Y, WIDTH, HEIGHT, CENTRE_X, CENTRE_Y);
        }
        if (fire != null) {
            batch.setColor(BACKING);
            batch.draw(
                    pixel,
                    X + 1,
                    PixelScreen.HEIGHT - LABEL_Y - LABEL_HEIGHT,
                    LABEL_X - X + Fonts.width(glass.fonts.label, label) + 2,
                    LABEL_HEIGHT + 1);
            batch.setColor(Color.WHITE);
        }
        glass.shadowed(batch, glass.fonts.label, label, labelColour, LABEL_X, LABEL_Y);
    }
}
