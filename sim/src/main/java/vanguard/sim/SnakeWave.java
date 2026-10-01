package vanguard.sim;

/**
 * A {@code snake} formation of Skitters entering along one path, one every snake spacing.
 *
 * @param startSeconds when the first Skitter enters, from the start of the wave cycle
 * @param path the path the snake flies
 * @param count Skitters in the snake
 * @param mirrored whether the snake flies the path flipped left to right
 */
public record SnakeWave(double startSeconds, SnakePath path, int count, boolean mirrored) {}
