package vanguard.sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A seed plus the command set of every step: enough to replay a run exactly. The text format is
 * a {@code seed <n>} line followed by run-length encoded {@code <steps> <commands>} lines.
 */
public final class InputRecording {
    private final long seed;
    private final int[] commands;

    public InputRecording(long seed, int[] commands) {
        this.seed = seed;
        this.commands = commands.clone();
    }

    public long seed() {
        return seed;
    }

    /** Replays the recording from a fresh world. */
    public World replay(SimConfig config) {
        if (config.seed() != seed) {
            throw new IllegalArgumentException("config seed " + config.seed() + " != recording seed " + seed);
        }
        World world = new World(config);
        for (int command : commands) {
            world.step(command);
        }
        return world;
    }

    /** A copy of the command sets, one per step. */
    public int[] commands() {
        return commands.clone();
    }

    public void write(Writer out) throws IOException {
        out.write("seed " + seed + "\n");
        int i = 0;
        while (i < commands.length) {
            int run = 1;
            while (i + run < commands.length && commands[i + run] == commands[i]) {
                run++;
            }
            out.write(run + " " + commands[i] + "\n");
            i += run;
        }
    }

    public static InputRecording read(Reader in) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        String header = reader.readLine();
        if (header == null || !header.startsWith("seed ")) {
            throw new IOException("missing seed line");
        }
        long seed = Long.parseLong(header.substring(5).trim());
        int[] commands = new int[1024];
        int size = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.trim().split(" ");
            int run = Integer.parseInt(parts[0]);
            int command = Integer.parseInt(parts[1]);
            if (size + run > commands.length) {
                commands = Arrays.copyOf(commands, Math.max(commands.length * 2, size + run));
            }
            Arrays.fill(commands, size, size + run, command);
            size += run;
        }
        return new InputRecording(seed, Arrays.copyOf(commands, size));
    }

    /** Collects the commands of a live run; allocates only when its buffer grows. */
    public static final class Recorder {
        private final long seed;
        private int[] commands = new int[60 * 60 * 10];
        private int size;

        public Recorder(long seed) {
            this.seed = seed;
        }

        public void record(int command) {
            if (size == commands.length) {
                commands = Arrays.copyOf(commands, size * 2);
            }
            commands[size++] = command;
        }

        public InputRecording toRecording() {
            return new InputRecording(seed, Arrays.copyOf(commands, size));
        }

        /** Writes the recording, wrapping I/O errors for use at shutdown. */
        public void writeTo(Writer out) {
            try (out) {
                toRecording().write(out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
