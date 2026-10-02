package vanguard.content;

/** One value per difficulty, written {@code {easy: …, medium: …, hard: …}}. */
public record PerDifficulty<T>(T easy, T medium, T hard) {
    /** The value for {@code difficulty}. */
    public T of(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easy;
            case MEDIUM -> medium;
            case HARD -> hard;
        };
    }
}
