# Mantis: concept prompts and briefs

## mantis-beam-r23-a

**Rejected** (round 23, user picked b); the files are in `rejected/`.
Round 23, variant A "hot wire" (`mantis-beam-r23-a.png`, sweep GIF `mantis-beam-r23-a.gif`).
Generated, not prompted: `python3 tools/concept/props_r23.py mantis-beam` (2D light fields
supersampled 4×, premultiplied on black, additive). Parts: a tileable beam strip 16×12 in 3 shimmer
frames (white-hot 2 px core, 6 px crimson #ff3038 body, stepped halo to 12 px), a 12×12 round tip
flare and a 14×14 eye flare in 3 frames. Telegraph: 19 pulsing 5×5 crimson dots on the 300 px reach
arc and dashed edge lines (6×2 dashes every 10 px), the layout `FarsideLooks.drawSweeps` draws today.
The play-field views and the GIF show the chosen round-04 Mantis turned in the model to face inward
from the left edge, the 0.6 s telegraph and the 1.2 s, 70° sweep over the darkened Level 06 ground.

- **Artist brief:** a thin, searing crimson laser from the Mantis's eye, 300 px long and 6 px wide,
  white-hot at the core, crisp edges with a short stepped halo, a round flare where it ends; the
  telegraph is a dotted arc at the beam's reach with dashed edges, pulsing, never filled.

## mantis-beam-r23-b

**Chosen** (round 23, user). Round 23, variant B "charged lance" (`mantis-beam-r23-b.png`, `mantis-beam-r23-b.gif`), same
generator. Parts: a beam strip 32×10 in 4 frames with energy knots running outwards (4 px per frame,
16 px apart, tileable), a 16×16 impact spark at the tip in 4 frames and a 16×16 eye ring. Telegraph:
one pre-rendered additive fan, a faint crimson fill rising towards the rim, a bright 2 px rim on the
arc and 1 px edges (302×348 px for 70°, 302×428 px for hard's 90°), rotated to the sweep's centre.

- **Artist brief:** a charged crimson lance with bright knots of energy racing out along it and
  sparks spitting where it ends; the telegraph shows the whole danger zone as a faint red wedge with
  a bright edge, so the safe side reads at a glance.

## mantis-beam-final-r23-a

Production art of the chosen variant B (`mantis-beam-final-r23-a.png`, `.gif`): not prompted,
written by `python3 tools/art/mantis_beam.py` into `assets/sprites/` (`mantis-beam_0..3`,
`mantis-beam-tip_0..3`, `mantis-beam-eye`, `mantis-telegraph-70`, `mantis-telegraph-90`) from the
concept's light fields (`tools/concept/props_r23.py` beam_b, spark_b, ring_b, wedge_b, imported
unchanged), additive (premultiplied on black), 24 colours over the beam, tip and eye, 32 per wedge;
the arcs and the 300 px length from `data.yaml`. The sheet shows the parts and two in-play frames;
the GIF a pincer of the production sprites (`tools/art/mantis.py`) telegraphing and sweeping from
the eye at the data's `sweep.origin`, laid out as `FarsideLooks.drawSweeps` draws them.

## mantis-beam-capture-r23-b

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 75 --settings <file> --level 6 --invulnerable` on its own virtual display (`Xvfb :97
-screen 0 960x540x24`; settings: `display.mode=window`, a 960×540 window at 0,0,
`audio.master=0`), recorded from that display only with `ffmpeg -f x11grab -draw_mouse 0
-framerate 10`; composed with PIL from frames of Level 06's t=56 pincer: the play field in the
telegraph and two moments of the sweep, then both heads at 3× in the telegraph and the sweep, the
wedge and the beam starting at the eye on both edges; the ship left at its start.

## mantis-final-r23-a

Production art, round 23 (`mantis-final-r23-a.png`, `.gif`): not prompted, rendered by
`python3 tools/art/mantis.py` from the chosen round-04 model (`render/enemy_models.mantis_a`,
re-posed in the model: wing beat, raptorial arms opening, eye charge), 80×80 at 4×, one 40-colour
palette; the death sets are 2D light fields (flash) and ray-marched pieces. The sheet and GIF are
built from the files in `assets/sprites/`.
