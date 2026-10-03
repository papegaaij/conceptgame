# Whirl Seed – generator notes and prompts

## whirl-seed-final-r16-a

Round 16, production art (M4 part C batch). Review sheet (`.png`) and loop (`.gif`) made from the final files in `assets/` by `tools/art/vrell_l03.py --review`; the frames themselves are rendered by `tools/art/vrell_l03.py` (see `tools/art/README.md`). Shows the 8 spin frames (26×26, 24 colours, `orientation: radial`): frame k is the seed turned k × 7.5° clockwise, each its own render under the fixed key light, so the 8 frames cover 60° and the loop repeats seamlessly. The model is the chosen round-05 seed (its petal blade, core and eye, plum chitin, teal glow) with six blades instead of five, as the approved six-fold design requires; all six blades are the body chitin (the concept's alternating dark blade would repeat only every 120°), the material patterns turn with the seed, and the core's teal seam runs along each blade's axis. The loop shows the spin enlarged next to a cluster of six spiralling out by the data's numbers (1.5 s at one turn per second, radius +90 px/s, release point drifting down 60 px/s, then 160 px/s outward and downward with side ricochets, spin 1.5 turns/s). Not an image-generator prompt: the look is the chosen concept's, whose prompt stays valid; this is the brief for reviewing the production frames.

## whirl-seed-death-final-r16-a

Round 16, production art (M4 part C batch): the death effect beyond the `tiny` pop. Review sheet (`.png`) and loop (`.gif`) made from the final files in `assets/` by `tools/art/vrell_fx.py --review seed`; the frames are rendered by `tools/art/vrell_fx.py` (see `tools/art/README.md`). Two sets at 2 game steps per frame (30 fps), centred on the seed:

- `whirl-seed-husk_0..11` (40×40, solid, 1-bit alpha, 24 colours, 0.40 s, started with the pop): the husk splitting. The six blades of the production seed break off and fly out along their axes, each tumbling about its own long axis, the spin running down; the core husk splits into two hollow halves that fall open; the eye is gone into the glint. Ray-marched per frame from `tools/art/vrell_l03.py`'s six-blade model and materials (the patterns turn with each piece), under the fixed key light; the pieces shrink in the last frames.
- `whirl-seed-death_0..7` (32×32, additive, 16 colours, 0.27 s, started 4 game steps after the pop, as its white flash fades): the teal glint, a four-point star flare with shorter diagonals over a teal halo, flaring and closing, six teal sparks flying out. No ring: with the star it read as a targeting reticle.

The sheet shows both sets at 6×, the two with the tiny pop (every second game step) and at 1×; the loop has three spinning seeds popping in turn. Not an image-generator prompt: the brief for reviewing the production frames. AI prompt for the look: "late-90s pre-rendered CGI sprite effect, top-down, a tiny plum alien seed pod with six blades popping: the blades breaking off and tumbling away, the round husk splitting in two, a sharp teal star glint, on black, additive glow, no text".

## whirl-seed-dive-capture-r16-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard --bench <s> --settings <file> --invulnerable --level 3` under `xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`), recorded with `ffmpeg -f x11grab -draw_mouse 0` at 20 fps and encoded with ffmpeg's `palettegen` (128 colours, `stats_mode=diff`) and `paletteuse` (Bayer dither, scale 5); level time ≈ the recording's time − 1.5 s. Run with `--bench 136` and `controls.auto-fire=false` (the ship never fires, so the seeds
live out their paths), no keys sent. Crop: the whole play field (480×540 at x 240), recording
68–74.5 s (t≈66.5–73), 15 fps: the second and third clusters spiralling out and diving.
