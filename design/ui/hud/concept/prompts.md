# HUD – concept prompts

Prompts and briefs for polished versions of the round 01 HUD mockups. The mockups are generated
by [tools/concept/hud.py](../../../../tools/concept/hud.py), which reuses parallax scene A from
[tools/concept/parallax.py](../../../../tools/concept/parallax.py) as the play field.

Shared layout (both variants): 16:9 screen, 640x360 native. Portrait play field 320x360 in the
centre; side panels of 160x360. **Left panel**: score, credits, act/level with progress to the
boss, radio portrait with caller name and subtitle, wingman status. **Right panel**: armour,
shield, generator load (MW used / available), the four weapon slots (front, rear, left wing,
right wing) with upgrade level pips, special ability with charges.

Common negative prompt: `modern flat UI, material design, thin sans-serif, mobile game UI,
photo, watermark, blurry text, cluttered, 3D perspective panels, curved screen`

## hud-r01-a

Variant A, **classic metallic bevelled panels**.

Prompt: `1990s PC shoot-em-up game screen, 16:9, central vertical play field showing a space
station above Earth, left and right side panels made of brushed gunmetal with bevelled edges,
rivets and yellow-black hazard stripes, recessed green LCD readouts for score and credits,
amber text, segmented red armour bar, blue shield bar, yellow generator bar, weapon slot boxes
with level pips, radio video feed with a green-tinted officer portrait and scanlines, crisp
pixel font, late-90s DOS/Windows 95 game UI, pre-rendered metal textures`

Artist notes: think Raptor / Tyrian side panels. Everything is a physical object: plates, wells,
screws. Text uses a chunky bitmap font. Readability first; the play field is never covered.

## hud-r01-b

Variant B, **dark glass neon cockpit**.

Prompt: `late 1990s sci-fi shoot-em-up game screen, 16:9, central vertical play field, side
panels of dark translucent blue glass with chamfered corners and glowing cyan neon outlines,
magenta and amber accents, large white score digits, gradient armour/shield/generator gauges
with tick marks, weapon systems list with chamfered level pips, incoming transmission box with
cyan-tinted pilot portrait and voice waveform, pixel font, subtle scanlines, bloom glow,
Wing Commander / Colony Wars cockpit display aesthetic`

Artist notes: a cockpit display rather than a machine casing. Neon lines glow; labels sit in the
frame gaps. Keep glow on the panels only – nothing bleeds into the play field.
