package vanguard.game.input;

import vanguard.sim.Command;

/** Turns the held actions into the simulation's command set for one step. */
public final class FlightCommands {
    private final ControlSettings settings;

    public FlightCommands(ControlSettings settings) {
        this.settings = settings;
    }

    public int of(ActionInput input) {
        int commands = Command.NONE;
        commands |= bit(input, Action.MOVE_UP, Command.UP);
        commands |= bit(input, Action.MOVE_DOWN, Command.DOWN);
        commands |= bit(input, Action.MOVE_LEFT, Command.LEFT);
        commands |= bit(input, Action.MOVE_RIGHT, Command.RIGHT);
        commands |= bit(input, Action.PRECISION, Command.PRECISION);
        // With auto-fire on, holding fire does nothing extra (design/ui/controls).
        if (settings.autoFire() || input.held(Action.FIRE)) {
            commands |= Command.FIRE.bit();
        }
        return commands;
    }

    private static int bit(ActionInput input, Action action, Command command) {
        return input.held(action) ? command.bit() : Command.NONE;
    }
}
