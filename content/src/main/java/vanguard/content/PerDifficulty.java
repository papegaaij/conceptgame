package vanguard.content;

/** One value per difficulty, written {@code {easy: …, medium: …, hard: …}}. */
public record PerDifficulty<T>(T easy, T medium, T hard) {}
