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
