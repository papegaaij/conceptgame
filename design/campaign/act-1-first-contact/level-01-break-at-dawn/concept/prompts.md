# Level 01 – concept prompts

## backdrop-fixes-r11-a

Round 11. Before/after board made by
[tools/concept/backdrop_fixes_r11.py](../../../../../tools/concept/backdrop_fixes_r11.py)
(`python3 tools/concept/backdrop_fixes_r11.py`): "before" is read from git at commit `acd4d42`
(via `git lfs smudge`), "after" from `assets/backdrop/level-01/`, which
[tools/concept/backdrop_l01.py](../../../../../tools/concept/backdrop_l01.py) renders
(`python3 tools/concept/backdrop_l01.py earth-dawn platform-burning`).

- `earth-dawn`: the night shading, the dawn band along the terminator (now wider, σ 55 px, with
  a faint brighter strip on the day side) and the top fade go through `finish_dithered()`: a 4×4
  Bayer threshold steps the alpha into 16 levels and offsets the colour by ±5 before a 32-colour
  median cut (was 10 alpha levels and 24 colours without dithering, which left hard bands).
- `platform-burning`: each breach vents a plume pointing out of the platform and a little
  down-right; its heat falls off along and across the plume and is torn into tongues by
  turbulence (two periodic noise fields mixed by cos/sin of the frame phase, so the 4-frame loop
  is seamless and nothing jumps), cut into a 6-step dull fire ramp (`FIRE`, below the bullets'
  orange in saturation and kept matte, readability rules 1 and 7), with a grey smoke trail and a
  few ember pixels, faded out before the sprite edge.

**Prompt:** pre-rendered late-90s CGI top-down view of a damaged orbital defence platform, fire
venting from three hull breaches as ragged flame plumes with drifting grey smoke and embers, dull
red-orange flames, 140×140 pixel sprite, palette-limited; and the dawn terminator on Earth seen
from orbit, a soft warm band between the day side and the city-lit night side, ordered
dithering instead of colour banding.

**Negative prompt:** round glowing blobs, neon orange, soft airbrushed glow, colour banding,
photographic textures.

## loot-targets-final-r12-a

Round 12, production art (Level 01 batch). Review sheet (`.png`) and loop (`.gif`) made from the final files in `assets/` by `tools/art/loot_targets.py --review`; the frames themselves are rendered by `tools/art/loot_targets.py` (see `tools/art/README.md`). Shows the container states, the break-apart (part P3: the pieces crumble away from their edges, a ragged pixel layer at a time, instead of dissolving in a 50 % dither checker), the beacon frames and the glint; check rule 7 (warm matte markings, light rim, damage from the first hit, clear break-apart). Not an image-generator prompt: the look is the chosen concept's, whose prompt stays valid; this is the brief for reviewing the production frames.

## backdrop-final-r12-a

Generated, not prompted: `python3 tools/art/backdrop_l01.py` renders every tile set and set piece
of the level's `backdrop` data into `assets/backdrop/level-01/` (with a `Source` chunk) and then
this review sheet and GIF from those files (`--review` rebuilds only the review). The look is the
placeholder's from `tools/concept/backdrop_l01.py` (the chosen Earth orbit scene of parallax r03 A,
its station kit with the muted ground accent of readability rule 7), brought to the production
bar: posterized to 12–32 colours on the opaque pixels, one palette per frame set, ordered
dithering on the limb haze and the Vrell glow, stepped smoke and spark translucency, the Moon at
4×, Aegis Two through the sprite render path, and `-mirrored` renders instead of runtime mirroring.

## game-capture-final-r12-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 80 --settings <file> --invulnerable --debug-speed 3` under `xvfb-run -s "-screen 0 960x540x24"`
(settings: `display.mode=window`, a 960×540 window at 0,0, `audio.master=0`), recorded with
`ffmpeg -f x11grab -framerate 4`; one frame per section at game time 12, 24.75, 78, 114.75 and
168.75 s (part P3: chosen where Skitters turn along their paths), cropped to the 480×540 play field.

## north-arm-final-r13-a

Generated, not prompted: `python3 tools/art/backdrop_l01.py north-arm` re-renders only the north arm
into `assets/backdrop/level-01/north-arm.png` and writes this sheet from it (`--review north-arm`
rebuilds only the sheet). The placeholder's layout, kit and far scale (0.45) with production solar
wings: 118 px instead of 140 px, so they end inside the 180 px piece, panel blankets in steel frames
with end rails and cell ribs, a capped hub with the red tip light; posterized to 24 colours.
