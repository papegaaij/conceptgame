package vanguard.game.ui;

import java.util.ArrayList;
import java.util.List;

/** Word wrapping for the fixed-width fonts. */
public final class Words {
    private Words() {}

    /** Word-wraps a text into lines of at most {@code width} characters; longer words are cut. */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
