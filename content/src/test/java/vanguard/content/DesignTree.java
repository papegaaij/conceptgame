package vanguard.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** The data files read straight from the design tree (the build passes its path). */
final class DesignTree {
    static final Path ROOT = Path.of(System.getProperty("vanguard.designDir"));

    private DesignTree() {}

    static List<DataFile> dataFiles() {
        try (Stream<Path> files = Files.walk(ROOT)) {
            return files.filter(p -> p.getFileName().toString().equals("data.yaml"))
                    .sorted()
                    .map(DesignTree::read)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DataFile read(Path file) {
        try {
            String path = ROOT.relativize(file).toString().replace('\\', '/');
            return new DataFile(path, Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
