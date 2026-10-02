package vanguard.game.level;

import com.badlogic.gdx.Input.Keys;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import vanguard.game.input.Action;
import vanguard.game.input.Bindings;

/** The words of the control prompts, naming the primary keys of the current bindings. */
public final class PromptTexts {
    /** A prompt's line in the HUD: what to do and the keys that do it. */
    public record Text(String action, String keys) {}

    private static final List<Integer> ARROWS = List.of(Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT);

    private final Text move;
    private final Text fire;
    private final Text precision;

    public PromptTexts(Bindings bindings) {
        move = new Text(
                "MOVE", keys(bindings, List.of(Action.MOVE_UP, Action.MOVE_DOWN, Action.MOVE_LEFT, Action.MOVE_RIGHT)));
        fire = new Text("FIRE", keys(bindings, List.of(Action.FIRE)));
        precision = new Text("PRECISION", keys(bindings, List.of(Action.PRECISION)));
    }

    /** The actions' primary keys; the four arrows are "ARROW KEYS", which fits the prompt box. */
    private static String keys(Bindings bindings, List<Action> actions) {
        List<Integer> keys = actions.stream()
                .map(action -> bindings.get(action).primaryKey())
                .toList();
        if (keys.equals(ARROWS)) {
            return "ARROW KEYS";
        }
        return keys.stream()
                .map(key -> Keys.toString(key).toUpperCase(Locale.ROOT))
                .collect(Collectors.joining("/"));
    }

    public List<Text> of(List<ControlPrompts.Prompt> prompts) {
        return prompts.stream()
                .map(prompt -> switch (prompt) {
                    case MOVE -> move;
                    case FIRE -> fire;
                    case PRECISION -> precision;
                })
                .toList();
    }
}
