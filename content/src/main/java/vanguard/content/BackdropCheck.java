package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;

/**
 * The checks of a level's backdrop (design/art-direction): the ids resolve, at most one tile set
 * per layer and section and one in every section on an opaque layer, every section's atmosphere
 * has a look, and, frame by frame over the whole level and its outro, no more mid-size set pieces
 * and strongly animated elements on screen than the art direction allows and nothing moving faster
 * than about 2 px per frame on its own.
 */
final class BackdropCheck {
    /** Two or three mid-size set pieces per screen (Density). */
    static final int MAX_MID_SIZE = 3;
    /** At most two strongly animated background elements at once (Motion budget). */
    static final int MAX_ANIMATED = 2;
    /** No element may jump by more than ~2 px between frames (Motion budget), at 60 frames/s. */
    static final double MAX_OWN_SPEED = 2.0 * SimStep.PER_SECOND;

    /** The cloud banks of every atmosphere intensity are one element, however dense. */
    private static final String BANKS = "atmosphere banks";

    private final LevelData level;
    private final BackdropData backdrop;
    private final BiConsumer<String, String> problem;

    BackdropCheck(LevelData level, BiConsumer<String, String> problem) {
        this.level = level;
        this.backdrop = level.backdrop();
        this.problem = problem;
    }

    void run() {
        boolean resolved = checkSections() & checkAtmosphere() & checkPieces();
        if (resolved) {
            checkScreens();
        }
    }

    private boolean checkSections() {
        boolean ok = true;
        for (int i = 0; i < level.sections().size(); i++) {
            List<String> tiles = level.sections().get(i).tiles();
            Set<BackdropLayer> layers = new TreeSet<>();
            boolean resolved = true;
            for (int j = 0; j < tiles.size(); j++) {
                String field = "sections[" + i + "].tiles[" + j + "]";
                BackdropData.TileSet tileSet = backdrop.tileSets().get(tiles.get(j));
                if (tileSet == null) {
                    resolved = unknown(field, "tile set", tiles.get(j), backdrop.tileSets());
                } else if (!layers.add(tileSet.layer())) {
                    problem.accept(
                            field, "a second tile set on " + tileSet.layer().key());
                    resolved = false;
                }
            }
            if (resolved) {
                for (BackdropLayer layer : BackdropLayer.values()) {
                    if (layer.opaque() && !layers.contains(layer)) {
                        problem.accept(
                                "sections[" + i + "].tiles",
                                "no tile set on " + layer.key() + ", which has to cover the whole screen");
                    }
                }
            }
            ok &= resolved;
        }
        return ok;
    }

    private boolean checkAtmosphere() {
        boolean ok = true;
        for (int i = 0; i < level.sections().size(); i++) {
            LevelData.Section section = level.sections().get(i);
            ok &= checkLook("sections[" + i + "].atmosphere", section.atmosphere());
            if (section.peak().isPresent()) {
                LevelData.Peak peak = section.peak().get();
                ok &= checkLook("sections[" + i + "].peak.atmosphere", peak.atmosphere());
                if (peak.from() < level.sectionStart(i) || peak.to() > section.end()) {
                    problem.accept("sections[" + i + "].peak", "the peak lies outside its section");
                    ok = false;
                }
            }
        }
        return ok;
    }

    /** An intensity a section or its peak asks for needs its look, with tile sets on the right layers. */
    private boolean checkLook(String sectionField, LevelData.Atmosphere atmosphere) {
        Optional<BackdropData.Look> look = backdrop.atmosphere().of(atmosphere);
        String key = atmosphere.name().toLowerCase(Locale.ROOT);
        if (look.isEmpty()) {
            problem.accept(sectionField, "no backdrop.atmosphere." + key);
            return false;
        }
        String field = "backdrop.atmosphere." + key;
        boolean banks = checkLookTiles(field + ".banks", look.get().banks(), BackdropLayer.LOW_AIR);
        return checkLookTiles(field + ".wisps", look.get().wisps(), BackdropLayer.HIGH_AIR) && banks;
    }

    private boolean checkLookTiles(String field, Optional<String> id, BackdropLayer layer) {
        if (id.isEmpty()) {
            return true;
        }
        BackdropData.TileSet tileSet = backdrop.tileSets().get(id.get());
        if (tileSet == null) {
            return unknown(field, "tile set", id.get(), backdrop.tileSets());
        }
        if (tileSet.layer() != layer) {
            problem.accept(field, "'" + id.get() + "' is on " + tileSet.layer().key() + ", not " + layer.key());
            return false;
        }
        return true;
    }

    private boolean checkPieces() {
        boolean ok = true;
        for (Map.Entry<String, BackdropData.TileSet> tileSet :
                backdrop.tileSets().entrySet()) {
            ok &= checkDrift("backdrop.tile_sets." + tileSet.getKey(), tileSet.getValue());
        }
        for (int i = 0; i < backdrop.placed().size(); i++) {
            BackdropData.PlacedPiece placed = backdrop.placed().get(i);
            String field = "backdrop.placed[" + i + "]";
            BackdropData.Piece piece = backdrop.pieces().get(placed.piece());
            if (piece == null) {
                ok = unknown(field + ".piece", "set piece", placed.piece(), backdrop.pieces());
                continue;
            }
            if (piece.headings().isPresent() != placed.path().isPresent()) {
                problem.accept(field, "a piece with headings follows a path, and only such a piece");
            }
            if (piece.headings().isPresent() && placed.mirrored()) {
                problem.accept(field + ".mirror", "a piece with headings is not mirrored");
            }
            placed.path().ifPresent(points -> {
                for (int p = 1; p < points.size(); p++) {
                    BackdropData.Waypoint a = points.get(p - 1);
                    BackdropData.Waypoint b = points.get(p);
                    double speed = Math.hypot(b.dx() - a.dx(), b.dy() - a.dy()) / (b.t() - a.t());
                    if (speed > MAX_OWN_SPEED) {
                        problem.accept(
                                field + ".path[" + p + "]",
                                String.format(
                                        Locale.ROOT,
                                        "moves %.0f px/s, more than %.0f (about 2 px per frame)",
                                        speed,
                                        MAX_OWN_SPEED));
                    }
                }
            });
        }
        return ok;
    }

    private boolean checkDrift(String field, BackdropData.TileSet tileSet) {
        if (Math.abs(tileSet.drift().orElse(0.0)) > MAX_OWN_SPEED) {
            problem.accept(field + ".drift", "drifts faster than " + MAX_OWN_SPEED + " px/s (about 2 px per frame)");
            return false;
        }
        return true;
    }

    /** Samples every simulation step of the level and its outro for the density and the motion budget. */
    private void checkScreens() {
        List<BackdropData.PlacedPiece> placed = backdrop.placed();
        boolean[] seen = new boolean[placed.size()];
        boolean densityReported = false;
        boolean motionReported = false;
        int steps = SimStep.ticks(level.outroEnd());
        for (int step = 0; step <= steps; step++) {
            double t = step * SimStep.SECONDS;
            List<String> midSize = new ArrayList<>();
            Set<String> animated = new TreeSet<>();
            for (int i = 0; i < placed.size(); i++) {
                BackdropData.PlacedPiece p = placed.get(i);
                if (onScreen(p, t)) {
                    seen[i] = true;
                    BackdropData.Piece piece = backdrop.pieces().get(p.piece());
                    if (piece.countsAsMidSize()) {
                        midSize.add(p.piece());
                    }
                    if (piece.animated() || p.path().isPresent()) {
                        animated.add(p.piece());
                    }
                }
            }
            addDriftingTiles(t, animated);
            if (!densityReported && midSize.size() > MAX_MID_SIZE) {
                problem.accept(
                        "backdrop.placed",
                        at(t) + midSize.size() + " mid-size set pieces on screen (at most " + MAX_MID_SIZE + "): "
                                + String.join(", ", midSize));
                densityReported = true;
            }
            if (!motionReported && animated.size() > MAX_ANIMATED) {
                problem.accept(
                        "backdrop",
                        at(t) + animated.size() + " strongly animated elements on screen (at most " + MAX_ANIMATED
                                + "): " + String.join(", ", animated));
                motionReported = true;
            }
        }
        for (int i = 0; i < placed.size(); i++) {
            if (!seen[i]) {
                problem.accept("backdrop.placed[" + i + "]", "'" + placed.get(i).piece() + "' is never on screen");
            }
        }
    }

    private static String at(double t) {
        return String.format(Locale.ROOT, "t=%.2f: ", t);
    }

    private boolean onScreen(BackdropData.PlacedPiece placed, double t) {
        BackdropData.Piece piece = backdrop.pieces().get(placed.piece());
        double y = level.pieceCentre(placed) - level.scrollAt(t) * backdrop.factor(piece.layer()) + placed.offsetY(t);
        double x = placed.x() + placed.offsetX(t);
        double halfWidth = piece.size().width() / 2;
        double halfHeight = piece.size().height() / 2;
        return x + halfWidth > 0
                && x - halfWidth < PlayField.WIDTH
                && y + halfHeight > 0
                && y - halfHeight < PlayField.HEIGHT;
    }

    /** The drifting section tile sets with a part on screen, and the atmosphere's banks if they drift. */
    private void addDriftingTiles(double t, Set<String> animated) {
        for (int i = 0; i < level.sections().size(); i++) {
            for (String id : level.sections().get(i).tiles()) {
                BackdropData.TileSet tileSet = backdrop.tileSets().get(id);
                if (tileSet.drifts() && sectionOnScreen(tileSet.layer(), i, t)) {
                    animated.add(id);
                }
            }
        }
        for (LevelData.Atmosphere atmosphere : atmospheresAt(t)) {
            backdrop.atmosphere()
                    .of(atmosphere)
                    .flatMap(BackdropData.Look::banks)
                    .map(backdrop.tileSets()::get)
                    .filter(BackdropData.TileSet::drifts)
                    .ifPresent(banks -> animated.add(BANKS));
        }
    }

    private boolean sectionOnScreen(BackdropLayer layer, int index, double t) {
        double bottom = level.scrollAt(t) * backdrop.factor(layer);
        double end = index + 1 < level.sections().size() ? level.seam(layer, index + 1) : Double.POSITIVE_INFINITY;
        return level.seam(layer, index) < bottom + PlayField.HEIGHT && end > bottom;
    }

    /** The stretch's intensity, or both while an atmosphere change ramps across a stretch boundary. */
    private List<LevelData.Atmosphere> atmospheresAt(double t) {
        List<LevelData.Atmosphere> out = new ArrayList<>();
        List<LevelData.Stretch> stretches = level.atmosphereStretches();
        LevelData.Stretch current = stretches.getLast();
        for (LevelData.Stretch stretch : stretches) {
            if (t < stretch.end()) {
                current = stretch;
                break;
            }
        }
        out.add(current.atmosphere());
        for (int i = 1; i < stretches.size(); i++) {
            if (Math.abs(t - stretches.get(i).start()) < backdrop.ramp() / 2) {
                out.add(stretches.get(i - 1).atmosphere());
                out.add(stretches.get(i).atmosphere());
            }
        }
        return out;
    }

    private boolean unknown(String field, String what, String id, Map<String, ?> known) {
        problem.accept(
                field,
                "unknown " + what + " '" + id + "' (known: " + String.join(", ", new TreeSet<>(known.keySet())) + ")");
        return false;
    }
}
