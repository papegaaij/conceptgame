# Level 03 – generator notes and prompts

## backdrop-final-r16-a

Round 16, production art (M4 part C). Review sheet made from the final files in
`assets/backdrop/level-03/` by `tools/art/backdrop_l03.py --review`; the pieces are rendered by
`tools/art/backdrop_l03.py` from the level's `backdrop` block (until the level has a data.yaml,
from `../backdrop-proposal.yaml`). It reuses Level 01's production pieces
(`tools/art/backdrop_l01.py`: the earth tiles, thin cloud decks, wisps, the defence platform and
crossbeam) and Level 02's frost streaks (`tools/art/backdrop_l02.py`, here the ice streaks), and adds
two cloud fronts over the Earth (r03's weather as translucent overlays), the lane beacons and the
orbital defence ring at the far scale, lane-marker buoys, the broken frigate *Kestrel* with its
lifeboat rack, halves of CDF platforms, wreck plates and a burnt-out tug (turned in the model,
scorched and hazed back under the air layer's debris), the olive-grey spore haze and the dense
spore banks of the heavy peak, pale spore streaks, and the rack's amber light (additive, 12×12),
which the game draws over the *Kestrel*'s four dark lenses for the secret's triggers.

## level-03-capture-final-r16-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench <s> --settings <file> --invulnerable --level 3 --loadout front=scatter-vulcan:3` under
`xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`,
`controls.auto-fire=true`), recorded with `ffmpeg -f x11grab -draw_mouse 0`, six whole-window
frames (the layer prompts and the bomber tracker are in the HUD's side panels) in a 2 × 3 grid,
8 px apart on `#0b0e14`. Three runs: the ship left at its start (`--bench 200`, 2 fps: the debris
field, the Spore Bloom, the second pass, the clear lane); the arrow key Left held 1.2 s from 10.5 s
after the start (an XTEST key event, `--bench 30`, 4 fps), so the first Spore Bomber is not shot at
once and drops its spores; Right held 1.2 s from 57 s (`--bench 82`, 4 fps), so the seed clusters
under the Leviathan's first pass live long enough to be seen.

## level-03-rendering-capture-r16-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard --bench <s> --settings <file> --invulnerable --level 3` under `xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`), recorded with `ffmpeg -f x11grab -draw_mouse 0` at 20 fps and encoded with ffmpeg's `palettegen` (128 colours, `stats_mode=diff`) and `paletteuse` (Bayer dither, scale 5); level time ≈ the recording's time − 1.5 s. Run with `--bench 136` and `controls.auto-fire=false` (the ship never fires, so every unit
lives out its path), no keys sent. Crop: the play field's lower 480×360 (x 240, y 180), recording
101.5–107 s (t≈100–105.5), 15 fps: the bombers veiled by the heavy banks and their spore mines
rising.

## level-03-rendering-capture-r16-b

The same run as [level-03-rendering-capture-r16-a](#level-03-rendering-capture-r16-a). Crop: the
whole play field (480×540 at x 240), recording 126.2–132.5 s (t≈124.7–131), 15 fps: the
Leviathan's second pass descending to the play plane.
