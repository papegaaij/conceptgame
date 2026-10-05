# Coilwyrm: production review files

## coilwyrm-final-r23-a

Production art, round 23 (`coilwyrm-final-r23-a.png`, `.gif`): not prompted, rendered by
`python3 tools/art/coilwyrm.py` from the chosen round-08 models (`render/archetype_models.coil_head`,
`coil_segment`, `coil_tail`, the model-space patterns of `tools/concept/rerender_r08.py`): every part
at 48 headings at 8×, the segments at the 12 sizes of the data's taper, one 40-colour palette over
the chain; the death sets are 2D light fields (flash, glint) and ray-marched pieces of the models'
parts. The sheet and GIF are built from the files in `assets/sprites/`; the image prompt and
animation brief of the concept are in [../../concept/prompts.md](../../concept/prompts.md#coilwyrm-r05-a).

## coilwyrm-death-capture-r24-a

A capture of the game, not generated art, taken at round 24's close (`.gif` silent at 12 fps,
`.mp4` with the game's sound): `desktop/build/install/terran-vanguard/bin/terran-vanguard --bench
100 --settings <file> --level 6 --invulnerable --loadout front=pulse-cannon:3` on a private Xvfb
display (960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`), the ship left at
its start. Video: `ffmpeg -f x11grab -framerate 30 -copyts` (wall-clock timestamps); sound:
OpenAL Soft's `wave` driver (`ALSOFT_CONF` with `drivers = wave`) writing the game's mix to a
file, nothing played aloud; the two aligned by the wave file's creation time against the video's
first timestamp (0.334 s), cut to the 5 s from t≈81.6 s of the run and cropped to the play field.
The first Coilwyrm comes down onto the ship's fire: the head is shot (the flash at 82.78 s, its
burst found in the sound at 82.76 s by a matched filter), the body drifts to rest and its members
burst 0.25 s apart (sound at 83.00, 83.26, 83.52, 83.80, 84.02, 84.26 s …, each with its look in
the same video frame or the next).
