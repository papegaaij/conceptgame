package vanguard.content;

/**
 * One data file of the design tree.
 *
 * @param path the path relative to {@code design/}, e.g. {@code enemies/air/skitter/data.yaml}
 * @param text the file's YAML
 */
public record DataFile(String path, String text) {}
