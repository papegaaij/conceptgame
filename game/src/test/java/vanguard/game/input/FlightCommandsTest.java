package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import vanguard.sim.Command;

class FlightCommandsTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());

    @Test
    void holdToFireFiresOnlyWhileFireIsHeld() {
        var commands = new FlightCommands(ControlSettings.defaults());
        devices.keys.add(Keys.LEFT);
        devices.keys.add(Keys.SHIFT_LEFT);
        input.update(devices);
        assertEquals(Command.of(Command.LEFT, Command.PRECISION), commands.of(input));

        devices.keys.add(Keys.SPACE);
        input.update(devices);

        assertEquals(Command.of(Command.LEFT, Command.PRECISION, Command.FIRE), commands.of(input));
    }

    @Test
    void autoFireFiresWithoutHolding() {
        var commands = new FlightCommands(new ControlSettings(true));
        input.update(devices);
        assertEquals(Command.FIRE.bit(), commands.of(input));

        devices.keys.add(Keys.SPACE);
        input.update(devices);

        assertEquals(Command.FIRE.bit(), commands.of(input), "holding does nothing extra");
    }
}
