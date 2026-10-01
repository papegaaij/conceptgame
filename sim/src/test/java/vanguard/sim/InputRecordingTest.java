package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class InputRecordingTest {
    @Test
    void writesRunLengthEncodedTextAndReadsItBack() throws IOException {
        int fire = 16;
        int fireLeft = 20;
        var recording = new InputRecording(7, new int[] {fire, fire, fire, fireLeft, 0});

        var text = new StringWriter();
        recording.write(text);

        assertEquals("seed 7\n3 16\n1 20\n1 0\n", text.toString());
        InputRecording read = InputRecording.read(new StringReader(text.toString()));
        assertEquals(7, read.seed());
        assertArrayEquals(recording.commands(), read.commands());
    }

    @Test
    void recorderCollectsTheCommandsOfEveryStep() {
        var recorder = new InputRecording.Recorder(3);
        recorder.record(1);
        recorder.record(2);

        InputRecording recording = recorder.toRecording();

        assertEquals(3, recording.seed());
        assertArrayEquals(new int[] {1, 2}, recording.commands());
    }
}
