package vanguard.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies the <em>chosen</em> concept sound effects the game plays into {@code assets/sfx} under
 * their concept names, except where the final sound is already there: the recorded effects rebuilt
 * from the Freesound originals by tools/art/sfx_originals.py carry a {@code SOURCE} Vorbis comment
 * ({@link FinalArt#isFinalSound}).
 *
 * <p>Usage: {@code PlaceholderSounds <conceptDir> <sfxDir> <file>...}
 */
public final class PlaceholderSounds {
    private PlaceholderSounds() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: PlaceholderSounds <conceptDir> <sfxDir> <file>...");
        }
        List<String> files = Arrays.asList(args).subList(2, args.length);
        List<String> copied = copy(Path.of(args[0]), Path.of(args[1]), files);
        System.out.println("copied " + copied.size() + " placeholder sounds, " + (files.size() - copied.size())
                + " final ones kept");
    }

    /** Copies each file from {@code concept} to {@code sfx} unless the copy there is final; returns the copied ones. */
    static List<String> copy(Path concept, Path sfx, List<String> files) throws IOException {
        Files.createDirectories(sfx);
        List<String> copied = new ArrayList<>();
        for (String file : files) {
            Path target = sfx.resolve(file);
            if (FinalArt.isFinalSound(target)) {
                continue;
            }
            Files.copy(concept.resolve(file), target, StandardCopyOption.REPLACE_EXISTING);
            copied.add(file);
        }
        return copied;
    }
}
