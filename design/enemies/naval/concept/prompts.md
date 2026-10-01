# Naval enemies — concept prompts

AI image-generator prompts and animation briefs for the naval concept sheets. Palette B, role colours from `design/enemies/README.md`.

# Round 06 — Act 2 introductions and roster additions

## driftjelly-r06-a

**Driftjelly (Vrell floating mine)** — a 40 px radial jellyfish. Files: `driftjelly-r06-a.png` (sheet) and `driftjelly-r06-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. An alien jellyfish seen from directly above, radially symmetric: a glossy translucent-looking olive-green bell (#7c8c3a lightened) with sixteen glowing lime (#a8ff2a) radial veins, a glowing lime knob at the top, a frilled dark rim and eight pale trailing tentacles spreading out under it.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, motion blur, robot, mechanical parts, rivets, metal plating, magenta or hot-pink glow, yellow glow, blue or cyan body colour, insect
- **Animation brief:** Static radial sprite, 4 bell-pulse frames (the bell contracts and the tentacles spread). Drifts with the current at the surface (with a ripple ring) or just below it (sub layer: blue-green tint, softer). When the player comes within 96 px the bell flashes and pulses a ring of shots.
- **Mockup generator:** `python3 tools/concept/enemies_r06.py driftjelly` (models `tools/concept/render/r06_models.py`, rendered with `ModelSpaceAngleSprites` from `tools/concept/render/enemy_rigs.py`)

## reef-spitter-r06-a

**Reef Spitter (Vrell barnacle gun on a raft)** — a 36 px gun on an 84 px raft. Files: `reef-spitter-r06-a.png` (sheet) and `reef-spitter-r06-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. Two sprites seen from directly above: (1) a floating biomass raft, an irregular mat of dark olive kelp lobes (#263010 to #7c8c3a) with glossy air bladders and a seaweed fringe; (2) a plated barnacle gun in mauve-grey slate chitin (#7c6878) with three bone mouths fanned forward, glowing violet (#9a4dff) inside.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, motion blur, robot, mechanical parts, rivets, metal plating, magenta or hot-pink glow, yellow glow, blue or cyan body colour, boat, ship, insect
- **Animation brief:** The raft bobs and drifts with the scroll (static sprite, small vertical bob). The barnacle turns at 32 headings to aim at the player and fires a 3-way fan from its three mouths (recoil frame, violet muzzle flash).
- **Mockup generator:** `python3 tools/concept/enemies_r06.py reef-spitter` (models `tools/concept/render/r06_models.py`, rendered with `ModelSpaceAngleSprites` from `tools/concept/render/enemy_rigs.py`)

## skimmer-r06-a

**Skimmer (Vrell flying-fish skiff)** — a 40 px fast surface skimmer. Files: `skimmer-r06-a.png` (sheet) and `skimmer-r06-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. A sleek alien flying fish seen from directly above: a torpedo body in rust-red chitin (#b04a2c) with a pale bone dorsal ridge, two long outrigger pectoral fins skimming the water, a forked tail, a glowing violet (#9a4dff) gland on the snout.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, motion blur, robot, mechanical parts, rivets, metal plating, magenta or hot-pink glow, yellow glow, blue or cyan body colour, insect, aircraft, boat
- **Animation brief:** 16 headings x 2 tail-beat frames. Skimmers weave between ice floes in sine paths from every edge (left, right, top), leaving a fading foam wake, and fire aimed shots from the snout gland.
- **Mockup generator:** `python3 tools/concept/enemies_r06.py skimmer` (models `tools/concept/render/r06_models.py`, rendered with `ModelSpaceAngleSprites` from `tools/concept/render/enemy_rigs.py`)

## spiral-nautilus-r06-a

**Spiral Nautilus (Vrell shell roller)** — a 56 px coiled-shell cephalopod. Files: `spiral-nautilus-r06-a.png` (sheet) and `spiral-nautilus-r06-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. Two sprites seen from directly above: (1) a coiled nautilus-like shell lying flat, ivory/bone (#dccfb4) with dark tiger stripes and growth ribs following the spiral, a faint violet (#9a4dff) glowing lip at the aperture; (2) a tentacle crown: a hooded mantle with two violet eyes and a fan of eight dark tentacles tipped with violet light.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, motion blur, robot, mechanical parts, rivets, metal plating, magenta or hot-pink glow, yellow glow, blue or cyan body colour, snail, insect
- **Animation brief:** The shell rolls (24 spin frames over 360°, stripes turning with it) along spiral-in paths around the player, pairs orbiting in opposite directions. It uncoils - the tentacle crown appears at 16 headings facing the player - and fires a spiral of shots; the shell is armoured, so it must be shot while uncoiled. Under Europa's ice it is lit only by the player's headlight cone and its own bioluminescence.
- **Mockup generator:** `python3 tools/concept/enemies_r06.py spiral-nautilus` (models `tools/concept/render/r06_models.py`, rendered with `ModelSpaceAngleSprites` from `tools/concept/render/enemy_rigs.py`)


## driftjelly-r07-a

**Driftjelly at the waterline (round 07)**: the chosen round-06 Driftjelly, now meeting the water surface properly. The user had asked whether the plain circle around it was a wave. Files: `driftjelly-r07-a.png` (sheet) and `driftjelly-r07-a.gif` (motion in the 480×540 play field).

- **Prompt:** late-1990s pre-rendered CGI game sprite, straight top-down orthographic view, key light from the top-left, a floating alien jellyfish mine half-submerged in a deep blue-green ocean seen from directly above. The glossy olive bell (#7c8c3a) with glowing lime (#a8ff2a) radial veins rises out of the water. A broken, uneven ring of white sea foam clings to the waterline and sheds small specks. Its pale tentacles and the lower bell are visible just below the surface, tinted blue-green, softened and slightly wavy from refraction. Two or three uneven concentric ripples spread out from it, each a light crest with a darker trough. Crisp pixel-art finish.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, three-quarter view, text, watermark, perfect geometric circles, clean outline ring, cartoon splash, magenta or hot-pink glow, mechanical parts, insect
- **Animation brief:** The bell pulses (4 frames). Each contraction sends out a train of three uneven ripples (light crest and dark trough, wobbling in radius, broken along their length) that stay where they were made while the jelly drifts on with the current and the scroll. The foam collar wobbles, opens and closes in gaps and sheds specks that drift away and fade. The submerged part sways with a sub-pixel row shift. Jellies fully below the surface (sub layer) have no foam. When the player comes within 96 px the bell flashes lime and pulses a ring of 10 shots. The 96 px trigger radius is a diagram on the sheet and is never drawn in game.
- **Mockup generator:** `python3 tools/concept/enemies_r07.py driftjelly`. The model is cut at the water plane with `clipped()` in `tools/concept/render/r07_models.py`; the foam, ripples and refraction are in `enemies_r07.py`. This follows the "Water" rule in design/art-direction.
