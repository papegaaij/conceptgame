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
        var settings = ControlSettings.defaults();
        devices.keys.add(Keys.LEFT);
        devices.keys.add(Keys.SHIFT_LEFT);
        input.update(devices);
        assertEquals(Command.of(Command.LEFT, Command.PRECISION), FlightCommands.of(input, settings));

        devices.keys.add(Keys.SPACE);
        input.update(devices);

        assertEquals(Command.of(Command.LEFT, Command.PRECISION, Command.FIRE), FlightCommands.of(input, settings));
    }

    @Test
    void autoFireFiresWithoutHolding() {
        var settings = ControlSettings.defaults().withAutoFire(true);
        input.update(devices);
        assertEquals(Command.FIRE.bit(), FlightCommands.of(input, settings));

        devices.keys.add(Keys.SPACE);
        input.update(devices);

        assertEquals(Command.FIRE.bit(), FlightCommands.of(input, settings), "holding does nothing extra");
    }
}
