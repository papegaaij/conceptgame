# Space enemies — concept prompts

Prompts for polished versions of the concept mockups. The mockups are rendered by code; see the generator per item.

## leviathan-r05-a

**Leviathan (Vrell whale)** — a huge articulated creature, about 480 px long including the tail and 300 px across the fins. Files: `leviathan-r05-a.png` (sheet) and `leviathan-r05-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. A colossal whale-like alien creature seen from directly above: smooth bone/ivory hide (#dccfb4, shadows #5c4a5c), a row of lighter dorsal plates, four dark vents along the back with violet (#9a4dff) glowing turrets, a glowing blowhole, small eyes near a broad head, long pectoral fins and a tail ending in a wide fluke. Deliver body, three tail segments, fluke and both fins as separate sprites with pivots at the joints.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, motion blur, magenta or hot-pink glow, yellow glow, blue or cyan body colour.
- **Animation brief:** A travelling wave runs down the three tail segments and the fluke; the pectoral fins flap slowly; every part is pre-rendered at 32 headings (nearest frame). It drifts across the screen over Earth orbit while the dorsal turrets fire aimed orbs; the blowhole is the weak point.
- **Mockup generator:** `python3 tools/concept/enemies_r05.py leviathan` (models `tools/concept/render/archetype_models.py`, rigs `tools/concept/render/enemy_rigs.py`)
