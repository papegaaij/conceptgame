package vanguard.game.level;

import com.badlogic.gdx.Input.Keys;
import java.util.List;
import java.util.Locale;
import vanguard.game.input.Action;
import vanguard.game.input.Bindings;

/** The words of the control prompts, naming the primary keys of the current bindings. */
public final class PromptTexts {
    private final String move;
    private final String fire;
    private final String precision;

    public PromptTexts(Bindings bindings) {
        move = "MOVE  " + keys(bindings, Action.MOVE_UP, Action.MOVE_DOWN, Action.MOVE_LEFT, Action.MOVE_RIGHT);
        fire = "FIRE  " + keys(bindings, Action.FIRE);
        precision = "PRECISION  " + keys(bindings, Action.PRECISION);
    }

    private static String keys(Bindings bindings, Action... actions) {
        StringBuilder text = new StringBuilder();
        for (Action action : actions) {
            if (!text.isEmpty()) {
                text.append('/');
            }
            text.append(Keys.toString(bindings.get(action).primaryKey()).toUpperCase(Locale.ROOT));
        }
        return text.toString();
    }

    public List<String> of(List<ControlPrompts.Prompt> prompts) {
        return prompts.stream()
                .map(prompt -> switch (prompt) {
                    case MOVE -> move;
                    case FIRE -> fire;
                    case PRECISION -> precision;
                })
                .toList();
    }
}
