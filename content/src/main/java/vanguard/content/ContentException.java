package vanguard.content;

import java.util.List;

/** The data files do not load or do not validate; one line per problem, naming the file and the field. */
public final class ContentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    ContentException(List<String> problems) {
        super("invalid design data:\n  " + String.join("\n  ", problems));
        this.problems = List.copyOf(problems);
    }

    /** Each problem as "design/<file>: <field path>: <what is wrong>". */
    public List<String> problems() {
        return problems;
    }
}
