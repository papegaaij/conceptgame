# Ship systems: prompts and capture notes

## targeting-computer-capture-r28-a

A capture of the game, not generated art (M5 part A, concept round 28). `desktop:installDist`'s
start script on an own `Xvfb :57 -screen 0 1920x1080x24` display, frames grabbed with
`ffmpeg -f x11grab -draw_mouse 0` at 8 fps (the play field cropped, 960×1080 at 2×) and assembled
with PIL: the two left panels at 1× (480×540), the right column two 2× zooms (240×270 each).

- Level 07: `--bench 62 --start level --level 7 --debug-speed 2 --invulnerable --loadout
  "left=micro-missile-pod:2,utility=targeting-computer,utility2=salvage-scanner"` with a fresh
  settings file holding `controls.auto-fire=true`.
- Left: the frame at about t = 90 s, a Brood Pod hit by the Pulse Cannon with its HP bar.
- Middle: the frame at about t = 110 s, the Brood Carrier with the brackets on its open bay-1 sacs.
- Right, top: the Brood Pod's bar zoomed; bottom: the Salvage scanner's glint on the lifeboat's tow
  cable at about t = 25 s (it plays the loot targets' glint frames until the glint's own sprite).

What to judge: whether the bar (2 px, readout green, amber below 30 %, fading 0.3 s after 1.5 s)
and the lime brackets read clearly at 1× without adding noise, and whether the glint needs a larger
or brighter sprite of its own.

## secret-glint-r28-a

Production art, not a prompt (M5 part A, concept round 28): `tools/art/secret_glint.py` (default
`--variant a`), a 2D light field evaluated at 4× and box-downsampled, additive (premultiplied on
black), one median-cut palette of 24 colours. Brief: the Salvage scanner's glint read small and
subtle as the loot targets' 9×9 sparkle (see the capture above); make it bigger and brighter while
keeping it a glint, not a pickup. Option a: a 23×23 four-point star in the loot glint's warm white
(`FFF4D6`), its long arms tapering out to 10 px, short diagonal arms, a white core, over a soft halo
in the salvage pickups' gold; four frames as the game plays them (0.3 s every 1.5 s): it rises,
flares, twinkles (the diagonals longest) and sinks back into the fading halo. Played in the game
until the round closes.

## secret-glint-r28-b

Production art, not a prompt: `tools/art/secret_glint.py --variant b` writes it to the game's
sprites instead of a. Option b, "scanner ping": a 35×35 thin gold ring that expands from the
object's centre (radius 3, 7, 11, 15 px over the four frames) and fades as it grows, four brighter
lock marks on it at the diagonals (a sensor's lock), a faint wash inside the ring and a white flash
at the centre in the first two frames: it reads as the scanner finding the object rather than the
object shining.
