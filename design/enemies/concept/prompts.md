# Enemies — concept prompts

## lineup-r03-a

**Round-03 lineup** — every round-03 enemy at native scale next to the player ship, on four backgrounds (dark, station hull, lunar regolith, Earth from orbit), then at 2× with names, plus the Brood Carrier at 1/4 scale. A readability and size check, not an asset.

- **Prompt:** not applicable (comparison sheet). For a polished faction sheet: late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background, like Raptor: Call of the Shadows or Tyrian 2000; a lineup of alien biomechanical Vrell creatures (violet chitin, teal and pink glow) next to angular black-and-gold human-built Ascendancy craft and a grey-white UTC fighter, all at the same scale, top-down.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, three-quarter view, side view, text, watermark, busy background, anti-aliased soft edges, motion blur.
- **Mockup generator:** `python3 tools/concept/enemies_r03.py lineup` (models in `tools/concept/render/enemy_models.py`)
