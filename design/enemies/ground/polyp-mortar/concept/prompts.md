# Polyp Mortar – generator notes and prompts

## polyp-mortar-final-r21-a

Round 21, production art (M4 part E, the Level 05 batch). Review sheet (`.png`) and loop (`.gif`) are made from the final files in `assets/` by `tools/art/l05_hazards.py --review`; the frames are rendered by `tools/art/l05_hazards.py` (see `tools/art/README.md`).

- `polyp-mortar_0..7`: 44×44, the stat-block size (the placeholder was cut from the concept at 36 px), `orientation: fixed`: the chosen round-04 model ([polyp-mortar-r04-a](../../concept/polyp-mortar-r04-a.png), `render/enemy_models.polyp_mortar_a`) in an 8-frame idle pulse loop at 10 fps, the acid mouth contracting and swelling (the model's `anim`) and its glow breathing with it; 32 colours over the set. The game has no fire state per unit, so there is no separate firing frame: the lob itself is the shot.
- `mortar-blob_0..3`: 16×16, the acid blob in flight, a wobbling lime glob shaded as a sphere with a 1-bit body and a stepped halo, drawn solid and scaled up to 1.6× at the top of its arc.
- `mortar-marker`: 44×44, additive, the impact marker: a lime ring of radius 16 px (the data's 32 px impact circle is a diameter), eight inward ticks and a centre dot, pulsing faster as the blob nears.

The loop plays a lob over a plain regolith plate: the mortar pulsing, the marker at the target and the blob arcing over to it. Not an image-generator prompt: the brief for reviewing the production frames. The concept's prompt stays valid for the look.

## polyp-mortar-death-final-r21-a

Production art (M4 part E, round 21), not a mockup: the Polyp Mortar's death effects in `assets/sprites/`,
rendered by [tools/art/l05_hazards.py](../../../../../tools/art/l05_hazards.py) (`python3 tools/art/l05_hazards.py death`,
which writes this sheet and GIF). `polyp-mortar-death` is a 2D light field like the explosions (vfx_r08's Canvas and
gauss): a lime flash in the mouth and 44 glowing acid droplets flung out in low arcs on a thinning haze;
`polyp-mortar-tatters` is ray-marched from the chosen round-04 model's own pieces and materials (its nine tentacles
torn off along their axes and tumbling, five dark shell shards), posed per frame under the fixed key light, as
tools/art/vrell_fx.py does for the Spore Bomber. Both 12 frames at 64×64, 4 game steps a frame, played by name with
the small burst. Brief: `late-90s pre-rendered sprite effect, top-down, a squat alien acid-mortar polyp bursting:
a lime-green flash and a spray of glowing acid droplets, its tentacles torn off and tumbling away with dark shell shards`.
