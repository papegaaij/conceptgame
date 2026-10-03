# Leviathan – generator notes and prompts

## leviathan-final-r16-a

Round 16, production art (M4 part C, the Level 03 batch). Review sheet (`.png`) and loop (`.gif`) are made from the final files in `assets/` by `tools/art/leviathan.py --review`. The frames are rendered by `tools/art/leviathan.py` (see `tools/art/README.md`). The model is the chosen round-05 Leviathan as re-rendered in round 08 ([leviathan-r08-a](../../concept/leviathan-r08-a.png), models in `tools/concept/render/archetype_models.py`). It keeps the bone/violet materials, hide mottle, dorsal plates, sacs, turret vents, tail segments, fluke and fins, but is laid out to the part offsets in the data: a long head, the fins near the middle, the vents and blowhole on the back half and a short three-segment tail.

The unit has one fixed heading per pass, each its own ray-march under the fixed key light:

- **Second pass** (facing down): the body in three tail-sway frames. The shootable parts (4 vents, 2 fins, fluke) are separate sprites, each centred on its data offset. Each part also has a wrecked version.
- **First pass** (the high-air crossing): the whole unit at heading −58.01° (clockwise from straight down; negative turns the head to the screen's right), drawn 1.25×. That is the course of Level 03's `cross` pass: its centre runs from (−265, −150) at t = 55 to (846, 544) at t = 80 (y below the top edge), atan2(1111, 694) = 58.01° down and to the right, so the head points the way it flies. `python3 tools/art/leviathan.py --cross` renders these three frames alone (about 4 min).

The parts are cut from whole-unit renders by an ID pass. The second-pass set shares one palette, so a part drawn over the body shows no seam. The blowhole glow is a 2D light field (vfx_r08), drawn additively. The sheet's hit-box row draws the data's boxes over the sprites, and its last row draws the level's `cross` path, read from the level's `data.yaml`, as a green arrow through the unit centre of a first-pass frame (the script refuses to build the sheet when the art's heading is more than 0.5° off that course).

This is not an image-generator prompt. The look is the chosen concept's, and its prompt stays valid. This entry is the brief for reviewing the production frames.

## leviathan-death-final-r16-a

Round 16, production art (M4 part C batch): the ichor cloud of the Leviathan's death, beyond the chained bursts. Review sheet (`.png`) and loop (`.gif`) made from the final files in `assets/` by `tools/art/vrell_fx.py --review ichor`; the frames are rendered by `tools/art/vrell_fx.py` (see `tools/art/README.md`). `leviathan-ichor_0..15`: 160×160, additive (premultiplied on black, 40 colours), 16 frames at 6 game steps each (10 fps, 1.60 s); a 2D light field like the blowhole glow. A wound bursts open in a pale violet flash, glowing violet globules are thrown out with a short smear while they are fast, and a plum-and-violet mist billows after them, spreads to about 70 px and thins into wisps. One set for every wound, so it works for either pass and any heading: one at each part with that part's chained medium burst (6 steps apart, the game's chain) and one at the centre with the large burst; its moderate brightness keeps the overlapping clouds over the vents from blowing out.

The sheet shows every second frame at 1× and every third at 2×. The loop plays the effect over the `leviathan-down` sprite (with its parts and the pulsing blowhole glow, as the review of `tools/art/leviathan.py` composes it): the chain of medium bursts with their ichor along the parts, then the large burst and the centre's ichor, the body taken away when the large burst starts (a review choice; when the game removes the body is its own). Not an image-generator prompt: the brief for reviewing the production frames. AI prompt for the look: "late-90s pre-rendered CGI sprite effect, top-down, glowing violet alien ichor gushing from a wound in space: bright violet droplets flung outwards and a soft plum-violet mist billowing and thinning into wisps, on black, additive glow, no text".

The death's whale-song cry is a sound: [enemy-leviathan-cry-r16-a](../../../../audio/sfx/concept/enemy-leviathan-cry-r16-a.ogg), brief in the [SFX prompts](../../../../audio/sfx/concept/prompts.md#enemy-leviathan-cry-r16-a-synthesized).
