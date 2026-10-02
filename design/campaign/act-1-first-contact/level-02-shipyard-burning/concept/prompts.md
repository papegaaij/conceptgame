# Level 02 – generator notes and prompts

## backdrop-final-r15-a

Round 15, production art (M4 part B). Review sheet made from the final files in
`assets/backdrop/level-02/` by `tools/art/backdrop_l02.py --review`; the pieces are rendered by
`tools/art/backdrop_l02.py`, which reuses Level 01's production pieces (`tools/art/backdrop_l01.py`)
for the shared station kit and adds the burning north arm, the docks with their half-built
frigates and teal Vrell growth (the Skitter's role colours), the *Resolute* venting coolant, grey
smoke and white coolant banks and frost streaks; Earth's limb (Level 01's piece) falls behind at the end.

## crane-four-final-r15-a

Round 15, production art (M4 part B). Review sheet and loop made from the final files by
`tools/art/crane_four.py --review`: the lattice jib of the station kit's look rendered at 61 angles
(3° apart, from straight down, positive to the right) under the fixed key light, with its pivot per
frame in `assets/pivots/crane-four.json`; the warning lights, the clamp light (additive) and the CDF
canister as their own sprites.

## level-02-capture-final-r15-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 95 --debug-speed 2 --settings <file> --invulnerable --level 2` under
`xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`,
`controls.auto-fire=true`), recorded with `ffmpeg -f x11grab -framerate 2`, six frames cropped to
the play field. The ship stays at its start, so the docks are lost; the × is the X server's pointer.

