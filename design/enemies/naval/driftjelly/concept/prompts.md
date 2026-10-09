# Driftjelly – generator notes and prompts

The chosen concept is [driftjelly-r07-a](../../concept/prompts.md#driftjelly-r07-a) (round 07, the
waterline look on the round-06 model, in the naval units' `concept/`); its prompt stays valid for the
look.

## driftjelly-final-r33-a

Round 33, production art (M5 part E batch, user decision E9 = a: straight to production from the
chosen concept; water drawn as the hybrid of E4 = c). Review sheet (`.png`) and loop (`.gif`) made
from the files in `assets/` by `python3 tools/art/driftjelly.py --review`; the frames are rendered by
`python3 tools/art/driftjelly.py` (see `tools/art/README.md`). The sea, and the game's generic `sub`
pass (tint toward the water, darker, blurred, swaying), are stand-ins in the review: the concept's
open-ocean ground and a fixed tint/blur/sway in the script.

- `driftjelly-sub_0..3` (40×40, 32 colours shared with the surface frames): the whole plain body —
  the round-06 model (`tools/concept/render/r06_models.driftjelly`, imported unchanged) — at four
  pulse frames (rest, contracting, contracted, relaxing); drawn through the `sub` pass, submerged
  and also under a surfaced jelly (its under-water part).
- `driftjelly_0..3` (40×40): surfaced, the dome above the waterline (the model cut at z = 0 by
  `render/r07_models.clipped`) with a broken, wobbling foam collar that sheds specks, at the four
  pulse frames and four collar phases; drawn on the surface over the matching `-sub` frame.
- `driftjelly-pulse_0..3` (40×40, additive): the lime veins' bloom, brighter as the bell contracts;
  drawn over a submerged jelly so the quickening pulse shows through the water.
- `driftjelly-surface_0..5` (40×40, 6 steps a frame = 0.6 s): the swap's surface layer — a lit swell
  over the bell, the crown breaking first, the dome rising through a growing collar, water streaming
  off; played backward to dive.
- `driftjelly-ripple_0..11` (104×104, 8 steps a frame, stepped translucency): one contraction's
  ripple train, three uneven light-crest/dark-trough rings 0.28 s apart, left on the sea.
- `driftjelly-death_0..9` (64×64, additive) and `driftjelly-tatters_0..9` (64×64, solid), 4 steps a
  frame: the `small` wet pop (white-lime flash, lime droplets, haze) and the bell torn in shreds,
  the tentacles and the knob, flung out and darkening toward the water as they sink.
