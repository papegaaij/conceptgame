package vanguard.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies the <em>chosen</em> concept sounds the game plays into {@code assets/} (the sound effects
 * into {@code sfx} under their concept names, the music into {@code music} under the names the game
 * loads), except where the final sound is already there: the recorded effects rebuilt from the
 * Freesound originals by tools/art/sfx_originals.py and the themes rendered by tools/art/themes.py
 * carry a {@code SOURCE} Vorbis comment ({@link FinalArt#isFinalSound}).
 *
 * <p>Usage: {@code PlaceholderSounds <conceptDir> <targetDir> <file>...}, where a file is a concept
 * name, copied under that name, or {@code <concept name>=<target name>}.
 */
public final class PlaceholderSounds {
    private PlaceholderSounds() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: PlaceholderSounds <conceptDir> <targetDir> <file>...");
        }
        List<String> files = Arrays.asList(args).subList(2, args.length);
        List<String> copied = copy(Path.of(args[0]), Path.of(args[1]), files);
        System.out.println("copied " + copied.size() + " placeholder sounds, " + (files.size() - copied.size())
                + " final ones kept");
    }

    /**
     * Copies each file ({@code name} or {@code concept=target}) from {@code concept} to {@code target}
     * unless the copy there is final; returns the target names of the copied ones.
     */
    static List<String> copy(Path concept, Path target, List<String> files) throws IOException {
        Files.createDirectories(target);
        List<String> copied = new ArrayList<>();
        for (String file : files) {
            int split = file.indexOf('=');
            String source = split < 0 ? file : file.substring(0, split);
            String name = split < 0 ? file : file.substring(split + 1);
            Path destination = target.resolve(name);
            if (FinalArt.isFinalSound(destination)) {
                continue;
            }
            Files.copy(concept.resolve(source), destination, StandardCopyOption.REPLACE_EXISTING);
            copied.add(name);
        }
        return copied;
    }
}
