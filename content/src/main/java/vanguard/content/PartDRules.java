package vanguard.content;

import java.util.List;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;

/**
 * M5 part D's level rules for the simulation (design/campaign, Level 10 Evacuation Corridor; user
 * decisions D1, D4 and D5 = a of 2026-10-08), used by {@link SimSpecs}: an air escort's stations,
 * liftoff, climb-out and scripted loss, turned from the data's screen points (px below the top
 * edge, units from 1) into the simulation's (y up from the bottom edge, units from 0).
 */
final class PartDRules {
    private PartDRules() {}

    /** The air escort of {@code escort} (its {@code stations} present). */
    static LevelScript.Air air(LevelData.Escort escort) {
        List<LevelScript.Station> stations = escort.stations().orElseThrow().stream()
                .map(PartDRules::station)
                .toList();
        return new LevelScript.Air(
                stations,
                escort.liftoff()
                        .map(liftoff -> new LevelScript.Liftoff(
                                liftoff.t(),
                                liftoff.seconds(),
                                liftoff.pads().stream()
                                        .map(pad -> new LevelScript.Pad(pad.x(), PlayField.HEIGHT - pad.y()))
                                        .toList())),
                escort.climb().map(climb -> new LevelScript.Climb(climb.t(), climb.seconds())),
                escort.scriptedLoss()
                        .map(loss -> new LevelScript.ScriptedLoss(loss.unit() - 1, loss.t(), loss.glow())));
    }

    /** A station: its point turned y up; the sway as the data gives it (down the screen, see {@link LevelScript.Station}). */
    static LevelScript.Station station(LevelData.Station station) {
        return new LevelScript.Station(
                station.at().x(),
                PlayField.HEIGHT - station.at().y(),
                station.sway().x(),
                station.sway().y(),
                station.period(),
                station.phase());
    }
}
