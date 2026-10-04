# Level 05 – Crater Nest: concept prompts and captures

## level-05-capture-final-r21-b

Round 21 item 12, the redo of `level-05-capture-final-r21-a` (now in `rejected/`, taken before the
frigate's production sprites were wired in). A capture of the game, not generated art:
`desktop/build/install/terran-vanguard/bin/terran-vanguard --bench 125 --settings <file> --level 5
--invulnerable --loadout front=pulse-cannon:4 --debug-speed 2` under `xvfb-run -s "-screen 0
960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`, `controls.auto-fire=true`),
recorded with `ffmpeg -f x11grab -draw_mouse 0 -framerate 8`, eight whole-window frames in a 2 × 4
grid, 8 px apart on `#0b0e14` (composed with PIL). One run, the ship left at its start. Top row: a
sled racing up the rail on its streak; the Gorgon Frigate descending with its bar and name (Driver
Control's warning on the radio); the three necks swinging in phase 1; the crown open over the lit
core. Bottom row: the core phase's ring and spiral; the death — the blast cluster over the bell and
necks, its peak at the swap, then the wedges and necks drifting apart with trailing blasts.

## level-05-capture-final-r21-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 150 --settings <file> --level 5 --invulnerable --loadout front=pulse-cannon:4 --debug-speed 2`
under `xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`,
`controls.auto-fire=true`), recorded with `ffmpeg -f x11grab -draw_mouse 0` at 4 fps (2 frames per
level second at double speed), eight whole-window frames in a 2 × 4 grid, 8 px apart on `#0b0e14`
(composed with PIL). One run, the ship left at its start; `--invulnerable` also keeps a lost battery
from failing the level, so the capture reaches the frigate. Level times (the arena clock after
150 s): the rail as the first sled runs, its streak only a faint band at the top (t≈16); a Polyp
Mortar's lob, the lime marker ring around the ship and the acid blob in flight (t≈29); the blob
landed, its 8-bullet ring around the ship, the stuck ore canister (cargo-container placeholder) on
the rail (t≈30); battery A with its units outlined in the objective colour, the tracker
`BATTERIES A B C D` over `NEST` (t≈55.5); the Gorgon Frigate descending with its bar and name
(t≈151); one head left and a head bursting (t≈163); the crown open, the core lit, the spiral
(t≈172); its death burst and the credit shower (t≈184.5). Backdrop: Level 04's Luna tile sets and
pieces (`backdrop.images: level-04`); the frigate is still drawn plainly (production art pending).

## l05-props-final-r21-c

Round 21, production art (M4 part E batch): Level 05's props from the concepts chosen in this round (rocks a, ore canister a, acid splash b; the models of `tools/concept/props_r21.py`, imported unchanged). Review sheet only (`.png`), made from the final files in `assets/` by `tools/art/l05_props.py --review`; the frames by `tools/art/l05_props.py` (see `tools/art/README.md`).

- `rock`: 16×16, air layer: three plain regolith shapes with a bright rim, 4 tumble frames each (ray-marched at 8×, 16 colours); the game picks a shape per rock and tumbles it.
- `ore-canister`: 40×28 on the rail: the steel ore cylinder with hazard bands in its clamp yoke: beacon dark, beacon lit (the clamp hittable), clamp shot open and scorched (8×, 32 colours).
- `polyp-mortar-splash`: 40×40, ground layer, soft alpha: the etched burn with its teal pool: fresh, after 1 s, fading (the concept's 2D field at native size, 24 colours); the mortar's remains for 10 s.

Not an image-generator prompt: the brief for reviewing the production frames.

## sled-final-r21-a

Round 21, production art (M4 part E batch): the mass-driver sled hazard. Review sheet (`.png`) and loop (`.gif`) made from the final files in `assets/` by `tools/art/l05_hazards.py --review`; the frames by `tools/art/l05_hazards.py` (see `tools/art/README.md`). The placeholder's streak barely read (a faint band) and its lamp chase was Level 04's fast blink, hard to tell from the rail tiles.

- `sled`: 24×48, a lit sled facing up the rail: a grey hull with a wedge nose and an amber cargo plate, dark clamp rails at its sides, a glowing amber thrust collar and three white plasma vents at its tail (ray-marched at 8×, 32 colours).
- `sled-streak`: 32×200, additive: a white-hot core and an amber glow trailing 176 px below the sled, fading out, with a bloom where the sled sits.
- `sled-lamp`: 16×16, additive, one lit rail lamp (amber halo, white core), drawn over Level 04's `sled-run` lamp pixels (two columns, every 60 px with the pylons): during the 1.5 s telegraph a lit band sweeps up the rail three times over all lamps at a quarter strength, and every lamp burns at full while the sled runs.

The loop plays one cycle on Level 04's rail piece: the lamps chasing upwards, then the sled racing up the rail on its streak with every lamp lit. Not an image-generator prompt: the brief for reviewing the production frames.

## rocks-r21

Concept round 21 (user decision D8): the low-gravity rocks a destroyed ground unit throws onto the
air layer (16×16; the data's `rocks`). Generator: `tools/concept/props_r21.py` (top-down SDF
renders of displaced spheres with one flat broken face, ground_targets.py's light rim; on a strip of
Luna regolith with a long air-layer shadow). Each sheet: one rock tumbling in 4 frames, two more shapes.
- **a**: plain regolith chunks, light grey with a bright rim.
- **b**: dark scorched basalt torn from the nest floor, a crust of glowing lime creep on one face.
AI prompt: "top-down pre-rendered late-90s game sprite, 16x16 px, a small chunk of lunar rock
tumbling, {a: pale grey regolith, bright rim light | b: dark scorched basalt with a glowing lime-green
alien crust on one face}, key light from the top left, drop shadow far below (it is airborne)"

## ore-canister-r21

Concept round 21 (user decision D8): the stuck sled's ore canister jammed on the rail (40×28, the
data's `stuck sled` target; its clamp is the trigger, hittable while the rail is dark). Generator:
`tools/concept/props_r21.py` (SDF, ground_targets.py's materials, hazard stripes, light rim and
scorch), drawn over Level 04's rail frame (`assets/backdrop/level-04/sled-run_0.png`). Frames: beacon
dark, beacon lit (hittable), clamp shot.
- **a**: a steel ore cylinder with amber/black hazard bands, held by a dark clamp yoke with the beacon.
- **b**: an open ore skip heaped with grey-brown ore, a hazard rim, clamp jaws at both ends.
AI prompt: "top-down pre-rendered late-90s game sprite, 40x28 px, {a: a steel ore cylinder with
amber and black hazard bands clamped by a dark yoke with a red beacon | b: an open steel ore skip full
of grey-brown ore lumps with a hazard-striped rim and clamp jaws at both ends}, jammed on a mass-driver
rail on the Moon, key light from the top left"

## acid-splash-r21

Concept round 21 (user decision D8): the acid splash decal on the ground layer (40×40), the Polyp
Mortar's death per its spec ("acid splash decal"). Generator: `tools/concept/props_r21.py` (2D numpy
fields, drawn flat without a shadow). Frames: fresh, after 1 s, fading.
- **a**: a wet lime splatter: a lobed pool, flung droplets and streaks, wet glints.
- **b**: an etched burn: a dark scorched pit, a teal-green bubbling pool, a pale etched ring.
AI prompt: "top-down late-90s game decal, 40x40 px, on grey lunar regolith, {a: a splash of glowing
lime-green alien acid with droplets and wet highlights | b: a dark acid-etched burn with a small
teal-green bubbling pool and a pale etched ring}, fading out over three frames"
