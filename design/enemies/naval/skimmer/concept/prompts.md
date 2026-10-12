# Skimmer – generator notes and prompts

The chosen concept is [skimmer-r06-a](../../concept/prompts.md#skimmer-r06-a) (round 06, in the naval units'
`concept/`); its prompt stays valid for the look.

## skimmer-final-r35-a

Round 35, production art (M5 part G batch, step G2; decision G9 = a, pending the user's review:
straight to production from the chosen concept; water drawn as part E's hybrid, E4 = c). Review sheet
(`.png`) and loop (`.gif`) rendered by `python3 tools/art/skimmer.py` from in-memory frames (see
`tools/art/README.md`); the frames go to `assets/` only with `--assets`, in the content step's time (no
data claims the Skimmer yet, and `packAtlases` fails on sprites no data claims). The ground is the
halted frame's stand-in (`tools/art/l13_props.py`'s `HaltPlate`: the chosen arctic scene's ice, water
and waterline foam re-cut round the relay's floe, the leads cut along the review's lanes), and the
game's generic `sub` pass is the fixed tint/blur/sway stand-in of `tools/art/driftjelly.py`.

- `skimmer_0..31` (40×40, 32 colours shared with the sub frames): the round-06 model
  (`tools/concept/render/r06_models.skimmer`, imported unchanged) above the waterline (cut at z = 0 by
  `render/r07_models.clipped`: only the keel is under water, the fan fins and the tail skim above it),
  16 headings × 2 tail frames (heading k × 22.5° clockwise from straight down, each its own render
  under the fixed key light), a broken foam collar along the keel's waterline. The concept's two tail
  frames (phase 0 and π) were the same pose; production swings the tail to ±π/2.
- `skimmer-sub_0..15` (40×40): the whole plain model per heading, for the `sub` pass.
- `skimmer-wake_0..127` (64×64, stepped translucency): the wake per heading, an 8-frame loop: two V
  arms of bow spray peeling back from the snout, a churned trail from the stern and a faint wash
  (`tools/art/water_fx.py`'s foam with the concept streams of `scenes_r08.py`, turned to the heading).
- `skimmer-sink_0..79` (48×48, 8 headings × 10 steps, 6 steps a frame): the death's flip and sink —
  the hull rolls onto its back, fills and goes under (seen through the water below the surface), a
  collar along the shrinking waterline and `water_fx.sink_foam`'s spreading foam ring; the burst is the
  shared `explosion-small` with `explosion-water-small` over the first steps (the water splash by unit,
  G5 = a).

The loop: 240×270 of the halted frame at 2×, Skimmers on the west lead and the south crossing at
180 px/s with the 40 px / 1.2 s weave (the stat block's lane walker), one shot and sinking.
