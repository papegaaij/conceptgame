# Bosses — concept prompts

## brood-carrier-r03-a

**Brood Carrier (Act 1 boss)** — a living carrier: segmented violet chitin hull, armoured dorsal plates, eight launch bays with glowing sac weak points along the flanks, a central core under a six-plate iris, flight membranes, a head with mandible turrets and eye clusters, trailing tendrils.

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background, like Raptor: Call of the Shadows or Tyrian 2000. sleek biomechanical alien, smooth elongated glossy violet chitin (#a020a8 to #d050d0, deep shadows #40004a), thin glowing teal (#00ff9a) seam along the spine, bright pink (#ff40ff) glowing eye as weak point. A colossal living alien carrier seen from directly above, about 2.2 times longer than wide, segmented body with armoured dorsal plates, four launch bays per flank with pulsing pink sacs, a glowing teal core in the centre behind a closing iris of chitin plates, membranous flight fins along the flanks, head with mandibles and pink eyes at the bottom, tendrils trailing at the top. Boss sprite, weak points glow brightest.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, three-quarter view, side view, text, watermark, busy background, anti-aliased soft edges, motion blur, caterpillar, cute, cartoon.
- **Mockup generator:** `python3 tools/concept/enemies_r03.py brood-carrier` (models in `tools/concept/render/enemy_models.py`)

# Round 04 — role colours

## brood-carrier-r04-a

**Brood Carrier, round 04 role colours** — teal-black chitin, teal glow.

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background. Biomechanical alien Vrell creature: colossal living carrier, segmented hull with dorsal plates, launch bays with sacs along the flanks, a core behind a chitin iris, flight membranes, head with mandibles at the bottom. Glossy teal-black chitin (#1e5c5a, shadows #06201e, spikes #8ab8a8), bioluminescent teal glow (#00ff9a) in seams, veins and eyes; weak points glow brightest in #a8ff2a (contrasting). Facing down.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, text, watermark, magenta or hot-pink glow, yellow glow, orange glow, blue or cyan body colour.
- **Mockup generator:** `python3 tools/concept/enemies_r04.py brood-carrier`

# Round 06 — Gorgon Frigate, Harbour Kraken, Siege Spire

All three follow the round 04 role colours and one boss convention: **weak points glow lime
(#a8ff2a)**, like the Brood Carrier. Each item has a sheet (PNG) and a motion study (GIF).

## gorgon-frigate-r06-a

**Gorgon Frigate (Act 1 mid-boss, L05)** — a medusa-bell warship with three serpent necks; each
neck ends in a cobra-like turret head that aims on its own. Turrets die separately, then the
crown petals open over the core.

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy specular highlights, crisp silhouette, isolated on a flat dark background, like Raptor: Call of the Shadows or Tyrian 2000. A biomechanical alien warship shaped like a jellyfish bell seen from above: domed hull of glossy bone-ivory chitin (#dccfb4, shadows #5c4a5c, highlights #f4ecd8) with dark radial ribs, a scalloped rim of glowing bulbs (violet #9a4dff), a five-petal bone crown in the centre hiding a lime (#a8ff2a) core, a short veil of tendrils trailing at the top. Three long segmented serpent necks emerge from sockets (one at the front, two at the rear flanks), each ending in a wedge-shaped cobra head with a low hood, a hinged jaw around a violet glowing gun mouth and small lime eyes. Facing down, about 230 px across plus necks.
- **Negative prompt:** photograph, painterly brush strokes, soft focus, perspective camera, side view, text, watermark, insect wings, moth, beetle, cute, cartoon, magenta or hot-pink glow, yellow glow, orange glow, blue or cyan body colour.
- **Animation brief:** necks are 5-segment chains (16 headings per segment) that bend from their socket toward the player and wave; heads (32 headings) open the jaw 0.3 s before a 3-shot aimed burst. Destroyed turrets leave a torn stump leaking violet ichor; the last turret fires faster; when all three are gone the petals part and the core fires ring bursts. Air layer, shadow (21, 30).
- **Mockup generator:** `python3 tools/concept/bosses_r06.py gorgon` (models in `tools/concept/render/boss_models.py`)

## harbour-kraken-r06-a

**Harbour Kraken (Act 2 mid-boss, L11)** — a real cephalopod wrapped round an offshore UTC
harbour platform; arms rise from the water along lanes to slam, the head surfaces to fire.

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered straight top-down orthographic, key light from the top-left, glossy wet specular highlights, crisp silhouette. A giant alien octopus seen from directly above in dark night-time ocean water: bulbous mottled rust-red mantle (#b04a2c, shadows #3a1008, pale #e0b890 suckers) covered in small papillae, a glowing crimson (#ff3038) seam along the back and a crimson lit beak, two large lime (#a8ff2a) eyes, eight thick sucker-lined arms; four arms coil over the edges of a square blue-grey offshore platform with a helipad, shipping containers, a crane and a torn corner; one arm lies stretched along a straight lane toward the bottom of the image with splash rings. Submerged parts are dark, blue-tinted and blurred. Facing down.
- **Negative prompt:** photograph, painterly brush strokes, perspective camera, side view, text, watermark, squid with fins, cute, cartoon, insect, crab, magenta glow, yellow glow, orange glow, bright turquoise water.
- **Animation brief:** arms are segment chains (13 segments + curled tip, 32 headings). A lane flashes with a red dashed telegraph 0.8 s ahead, the arm's shadow moves under the waves, then the arm rises along the lane (segments surface from base to tip in 0.3 s), slams with splash rings, holds 0.7 s and sinks. Between slams the head surfaces (foam ring), opens its eyes (weak points) and fires 7-orb fans from the beak, then submerges. Submerged sprites are tinted toward the sea colour, faded to ~55 % and softened (sub layer).
- **Mockup generator:** `python3 tools/concept/bosses_r06.py kraken`

## siege-spire-r06-a

**Siege Spire (Act 2 boss, L14)** — a rooted Vrell citadel in the UTC capital: six roots end in
root turrets and acid mortars, the crown's maw launches Wraiths, then the spire tears itself
free and sweeps a laser from the air.

- **Prompt:** late-1990s pre-rendered CGI game sprite, 3D model rendered top-down with a slight camera-height perspective so the tall structure's top is pushed upward, key light from the top-left, glossy organic surfaces, crisp silhouette. A colossal alien bio-tower growing out of a ruined night-time city: six thick mauve-grey (#7c6878, shadows #241a24) roots spreading over the city blocks with dark knots and faint violet (#9a4dff) veins, each ending in a bulbous turret pod with a petal collar; a ribbed tapering trunk with spiral violet ridges and pale bone spines; at the top a crown of six slate petals ending in curved bone claws with crimson (#ff3038) emitter tips around a glowing lime (#a8ff2a) launch maw. Purple-grey alien creep with glowing veins covers the streets around its base.
- **Negative prompt:** photograph, painterly brush strokes, side view, isometric, text, watermark, starfish, flower, cute, cartoon, magenta glow, yellow glow, orange glow, blue body colour.
- **Animation brief:** root turrets (32 headings) aim and fire aimed orbs; mortar pods open and lob acid blobs with marked impacts; the maw opens to launch cloaked Wraiths that rise toward the camera (scale in) and fly off to circle round. When the root pods die (chain explosions) the roots wither, the spire tears itself out (shockwave, debris), rises — drawn 1.0 → 1.3× with its shadow sliding away and blurring — and sweeps a telegraphed laser from its petal tips across the lower screen. The trunk's roof is pushed away from the screen centre (tall-structure perspective rule).
- **Mockup generator:** `python3 tools/concept/bosses_r06.py spire`
