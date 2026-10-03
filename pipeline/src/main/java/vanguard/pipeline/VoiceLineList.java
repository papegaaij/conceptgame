package vanguard.pipeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.VoiceData;
import vanguard.content.voice.VoiceLines;

/**
 * Writes every spoken line with its key and Chatterbox settings as JSON, for the offline voice
 * renderer (tools/art/voice.py; design/audio/voice). Arguments: the output file.
 */
public final class VoiceLineList {
    private VoiceLineList() {}

    public static void main(String[] args) throws IOException {
        Content content = ContentLoader.fromClasspath();
        List<VoiceLines.VoiceLine> lines = VoiceLines.all(content);
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < lines.size(); i++) {
            VoiceLines.VoiceLine line = lines.get(i);
            VoiceData.Speaker speaker = content.voices().speakers().get(line.voice());
            Integer pin = speaker.pins().orElse(Map.of()).get(line.key());
            json.append("  {")
                    .append(field("key", line.key()))
                    .append(", ")
                    .append(field("path", line.path()))
                    .append(", ")
                    .append(field("voice", line.voice()))
                    .append(", ")
                    .append(field("speaker", line.speaker()))
                    .append(", ")
                    .append(field("ref", speaker.ref()))
                    .append(", ")
                    .append(field("layering", speaker.layering().orElse("")))
                    .append(", ")
                    .append(field("expression", line.expression().slug()))
                    .append(", \"shout\": ")
                    .append(line.shout())
                    .append(", ")
                    .append(field("filter", line.filter().slug()))
                    .append(String.format(
                            Locale.ROOT,
                            ", \"exaggeration\": %.3f, \"cfg_weight\": %.3f, \"temperature\": %.3f",
                            line.settings().exaggeration(),
                            line.settings().cfgWeight(),
                            line.settings().temperature()))
                    .append(", \"pin\": ")
                    .append(pin == null ? "null" : pin.toString())
                    .append(", ")
                    .append(field("source", line.source()))
                    .append(", ")
                    .append(field("spoken", line.spoken()))
                    .append(", ")
                    .append(field("text", line.text()))
                    .append(i < lines.size() - 1 ? "},\n" : "}\n");
        }
        json.append("]\n");
        Path out = Path.of(args[0]);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, json, StandardCharsets.UTF_8);
        System.out.println(lines.size() + " voice lines -> " + out);
    }

    private static String field(String name, String value) {
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return "\"" + name + "\": \"" + escaped + "\"";
    }
}
