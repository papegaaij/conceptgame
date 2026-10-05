#!/usr/bin/env python3
"""Import third-party sound effects (CC0 / CC-BY) into the concept directories.

Every entry in SOURCES maps one Freesound sound to one concept file: the public HQ preview is
downloaded into a cache directory outside the repository, the leading silence is trimmed, the
sound is cut to `length` seconds with a `fade` second fade-out, peak-normalised to `peak` dBFS
and written as 44.1 kHz OGG Vorbis. Licences and credits are recorded in CREDITS.md.

Optional treatments:
  loop=(start, length, xfade[, curve])
                               cut a seamless loop (for continuous beams): the `xfade` seconds
                               after the loop end are cross-faded into its start; no fades are
                               applied, so the file can be played looped. curve "power"
                               (default, equal-power, for uncorrelated noise) or "auto" (linear
                               when the two overlapping parts correlate > 0.5, e.g. tonal hums,
                               which would otherwise bulge by up to +3 dB at the seam).
  band_rms=dB                  normalise the 200 Hz-5 kHz band RMS to this level instead of the
                               peak, with `peak` as ceiling; used for sustained sounds so their
                               audible loudness matches the shots (round 03 beams were
                               peak-normalised but almost all sub-bass, hence inaudible).
  rejected=True                the user rejected the file; it is written to concept/rejected/.
  lowpass=Hz                   4-pole low-pass (two cascaded 2-pole SVFs) applied after the
                               cut, e.g. to derive a muffled under-water variant.
  highpass=Hz                  4-pole high-pass (two cascaded 2-pole SVFs) applied after the
                               cut and before the fades: removes sub-sonic drift (which leaves
                               a DC offset after the fade-out) or tames a sub-heavy source so
                               its audible band can be levelled up without hitting the ceiling.
  fadein=sec                   fade-in length (default 2 ms), for cuts that start in the middle
                               of a sound, e.g. a jet flyby.

The previews are lossy (~192 kbps); the production asset should be rebuilt from the original
file (Freesound login required) with the same settings.

Usage: python3 tools/concept/audio/import_sfx.py [--cache DIR] [name ...]
       (default cache: $TMPDIR/conceptgame-sfx-cache; names select entries, e.g. explosion-r02-d)
"""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import SR, db, decode, normalize_peak, svf, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
SFX = ROOT / "design" / "audio" / "sfx" / "concept"

# name: source page, preview url, licence, offset into the (silence-trimmed) sound, length,
# fade-out, peak dBFS. Shots peak at -10 dBFS (they repeat constantly), explosions at -1.5.
SOURCES = {
    "player-shot-r02-a": dict(
        page="https://freesound.org/people/unfa/sounds/193427/",
        preview="https://cdn.freesound.org/previews/193/193427_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.25, fade=0.12, peak=-10.0),
    "player-shot-r02-b": dict(
        page="https://freesound.org/people/Bird_man/sounds/317136/",
        preview="https://cdn.freesound.org/previews/317/317136_4745081-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.28, fade=0.14, peak=-10.0),
    "player-shot-r02-c": dict(
        page="https://freesound.org/people/nsstudios/sounds/344276/",
        preview="https://cdn.freesound.org/previews/344/344276_2776777-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.27, fade=0.10, peak=-10.0),
    "player-shot-r02-d": dict(
        page="https://freesound.org/people/pgi/sounds/212601/",
        preview="https://cdn.freesound.org/previews/212/212601_1654571-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.28, fade=0.16, peak=-10.0),
    "player-shot-r02-e": dict(
        page="https://freesound.org/people/qubodup/sounds/854186/",
        preview="https://cdn.freesound.org/previews/854/854186_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.26, fade=0.12, peak=-10.0),  # first shot of the burst
    "explosion-r02-a": dict(
        page="https://freesound.org/people/bevibeldesign/sounds/315826/",
        preview="https://cdn.freesound.org/previews/315/315826_4557960-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.70, fade=0.30, peak=-1.5),
    "explosion-r02-b": dict(
        page="https://freesound.org/people/magnuswaker/sounds/523089/",
        preview="https://cdn.freesound.org/previews/523/523089_11537497-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.40, peak=-1.5),
    "explosion-r02-c": dict(
        page="https://freesound.org/people/qubodup/sounds/182429/",
        preview="https://cdn.freesound.org/previews/182/182429_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.70, fade=0.50, peak=-1.5),
    "explosion-r02-d": dict(
        page="https://freesound.org/people/juskiddink/sounds/108641/",
        preview="https://cdn.freesound.org/previews/108/108641_649468-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=2.80, fade=0.90, peak=-1.5),
    "explosion-r02-e": dict(
        page="https://freesound.org/people/derplayer/sounds/587194/",
        preview="https://cdn.freesound.org/previews/587/587194_13123807-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.86, fade=0.80, peak=-1.5),
    # ---- concept round 03: per-weapon shot families (shot-<family>) and the explosion ladder
    # (explosion-<size>); see design/audio/sfx/README.md for the weapon/enemy mapping.
    "shot-vulcan-r03-b": dict(
        page="https://freesound.org/people/pgi/sounds/98331/",
        preview="https://cdn.freesound.org/previews/98/98331_1654571-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.30, fade=0.12, peak=-10.0),
    "shot-laser-r03-b": dict(
        page="https://freesound.org/people/michael_grinnell/sounds/512469/",
        preview="https://cdn.freesound.org/previews/512/512469_7372230-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.30, fade=0.14, peak=-10.0),
    "shot-beam-r03-a": dict(
        page="https://freesound.org/people/deleted_user_1941307/sounds/152322/",
        preview="https://cdn.freesound.org/previews/152/152322_1941307-hq.ogg",
        licence="CC0 1.0", loop=(0.20, 1.40, 0.10), peak=-12.0, rejected=True),
    "shot-beam-r03-b": dict(
        page="https://freesound.org/people/bolkmar/sounds/420364/",
        preview="https://cdn.freesound.org/previews/420/420364_2927958-hq.ogg",
        licence="CC-BY 4.0", loop=(0.40, 2.62, 0.08), peak=-12.0, rejected=True),
    "shot-missile-r03-a": dict(
        page="https://freesound.org/people/Jarusca/sounds/521377/",
        preview="https://cdn.freesound.org/previews/521/521377_10847299-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.40, peak=-8.0),
    "shot-micromissile-r03-a": dict(
        page="https://freesound.org/people/Audionautics/sounds/171655/",
        preview="https://cdn.freesound.org/previews/171/171655_2451120-hq.ogg",
        licence="CC-BY 3.0", offset=0.0, length=0.45, fade=0.20, peak=-10.0),
    "shot-mortar-r03-a": dict(
        page="https://freesound.org/people/qubodup/sounds/184382/",
        preview="https://cdn.freesound.org/previews/184/184382_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.00, fade=0.50, peak=-8.0),
    "shot-bomb-r03-a": dict(
        page="https://freesound.org/people/Daleonfire/sounds/506313/",
        preview="https://cdn.freesound.org/previews/506/506313_150886-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.20, fade=0.60, peak=-10.0),
    "shot-torpedo-r03-a": dict(
        page="https://freesound.org/people/jobro/sounds/35530/",
        preview="https://cdn.freesound.org/previews/35/35530_35187-hq.ogg",
        licence="CC-BY 3.0", offset=0.0, length=1.00, fade=0.50, peak=-8.0),
    "shot-mine-r03-a": dict(
        page="https://freesound.org/people/nicktermer/sounds/259553/",
        preview="https://cdn.freesound.org/previews/259/259553_2316086-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.30, peak=-8.0),
    "shot-tesla-r03-a": dict(
        page="https://freesound.org/people/michael_grinnell/sounds/512471/",
        preview="https://cdn.freesound.org/previews/512/512471_7372230-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.22, fade=0.06, peak=-10.0),
    "shot-resonator-r03-a": dict(
        page="https://freesound.org/people/humanoide9000/sounds/422440/",
        preview="https://cdn.freesound.org/previews/422/422440_4361321-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.60, fade=0.30, peak=-8.0),
    "explosion-tiny-r03-a": dict(
        page="https://freesound.org/people/Cyberios/sounds/145788/",
        preview="https://cdn.freesound.org/previews/145/145788_2483826-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.45, fade=0.20, peak=-4.0),
    "explosion-tiny-r03-b": dict(
        page="https://freesound.org/people/dinodilopho/sounds/328833/",
        preview="https://cdn.freesound.org/previews/328/328833_4732572-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.60, fade=0.30, peak=-4.0),
    "explosion-tiny-r03-c": dict(
        page="https://freesound.org/people/unfa/sounds/609588/",
        preview="https://cdn.freesound.org/previews/609/609588_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.60, fade=0.30, peak=-4.0, rejected=True),
    "explosion-small-r03-a": dict(
        page="https://freesound.org/people/lorenzgillner/sounds/271979/",
        preview="https://cdn.freesound.org/previews/271/271979_5169846-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.10, fade=0.40, peak=-1.5),
    "explosion-medium-r03-a": dict(
        page="https://freesound.org/people/1histori/sounds/401609/",
        preview="https://cdn.freesound.org/previews/401/401609_3767503-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.60, fade=0.60, peak=-1.5, rejected=True),
    "explosion-medium-r03-b": dict(
        page="https://freesound.org/people/mitchelk/sounds/136765/",
        preview="https://cdn.freesound.org/previews/136/136765_2482480-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.00, fade=0.80, peak=-1.5),
    "explosion-large-r03-a": dict(
        page="https://freesound.org/people/derplayer/sounds/587193/",
        preview="https://cdn.freesound.org/previews/587/587193_13123807-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.10, fade=1.00, peak=-1.5),
    "explosion-huge-r03-a": dict(
        page="https://freesound.org/people/tommccann/sounds/235968/",
        preview="https://cdn.freesound.org/previews/235/235968_4265427-hq.ogg",
        licence="CC0 1.0", offset=0.36, length=5.00, fade=2.00, peak=-1.0, rejected=True),
    "explosion-huge-r03-b": dict(
        page="https://freesound.org/people/unfa/sounds/189779/",
        preview="https://cdn.freesound.org/previews/189/189779_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=6.00, fade=2.50, peak=-1.0),
    "explosion-underwater-r03-a": dict(
        page="https://freesound.org/people/cubix/sounds/124544/",
        preview="https://cdn.freesound.org/previews/124/124544_276157-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.00, fade=1.20, peak=-1.5),
    "explosion-underwater-r03-b": dict(
        page="https://freesound.org/people/qubodup/sounds/182429/",
        preview="https://cdn.freesound.org/previews/182/182429_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.70, fade=0.60, peak=-1.5, lowpass=500, rejected=True),
    "explosion-water-r03-a": dict(
        page="https://freesound.org/people/Sheyvan/sounds/519008/",
        preview="https://cdn.freesound.org/previews/519/519008_3248005-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.90, fade=0.70, peak=-1.5),
    # ---- concept round 04: audible beam loops (+ start/stop), Sonar Pulse, extra huge and
    # under-water explosions. Beams are normalised on their 200 Hz-5 kHz band RMS (see band_rms).
    "shot-beam-r04-a": dict(
        page="https://freesound.org/people/peepholecircus/sounds/169991/",
        preview="https://cdn.freesound.org/previews/169/169991_2747497-hq.ogg",
        licence="CC0 1.0", loop=(5.60, 2.60, 0.12, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-r04-b": dict(
        page="https://freesound.org/people/unfa/sounds/584191/",
        preview="https://cdn.freesound.org/previews/584/584191_1038806-hq.ogg",
        licence="CC0 1.0", loop=(0.00, 1.90, 0.10, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-r04-c": dict(
        page="https://freesound.org/people/zimbot/sounds/177100/",
        preview="https://cdn.freesound.org/previews/177/177100_1449999-hq.ogg",
        licence="CC-BY 4.0", loop=(1.00, 2.00, 0.10, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-start-r04-a": dict(
        page="https://freesound.org/people/Glitchedtones/sounds/375925/",
        preview="https://cdn.freesound.org/previews/375/375925_3294528-hq.ogg",
        licence="CC0 1.0", offset=1.10, length=0.90, fade=0.15, band_rms=-30.0, peak=-3.0),
    "shot-beam-stop-r04-a": dict(
        page="https://freesound.org/people/noirenex/sounds/159399/",
        preview="https://cdn.freesound.org/previews/159/159399_1656228-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.30, fade=0.60, band_rms=-30.0, peak=-3.0),
    "special-sonar-r04-a": dict(
        page="https://freesound.org/people/SamsterBirdies/sounds/539957/",
        preview="https://cdn.freesound.org/previews/539/539957_5487341-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.00, fade=1.00, peak=-4.0),
    "special-sonar-r04-b": dict(
        page="https://freesound.org/people/unfa/sounds/215415/",
        preview="https://cdn.freesound.org/previews/215/215415_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.50, fade=1.50, peak=-4.0),
    "explosion-huge-r04-a": dict(
        page="https://freesound.org/people/sidohzen/sounds/165808/",
        preview="https://cdn.freesound.org/previews/165/165808_2872744-hq.ogg",
        licence="CC0 1.0", offset=1.63, length=6.00, fade=2.50, peak=-1.0),
    "explosion-underwater-r04-a": dict(
        page="https://freesound.org/people/mokasza/sounds/810765/",
        preview="https://cdn.freesound.org/previews/810/810765_17437502-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=3.50, fade=1.20, peak=-1.5),
    # ---- concept round 08: remaining Acts 1-2 sounds. One-shots are levelled on their
    # 200 Hz-5 kHz band (band_rms) with a peak ceiling, so sub-heavy sources stay audible;
    # explosion-like ones are peak-normalised like the explosion ladder. Ambience = loops.
    "hit-metal-r08-a": dict(
        page="https://freesound.org/people/wilhellboy/sounds/351371/",
        preview="https://cdn.freesound.org/previews/351/351371_4603244-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.3, fade=0.12, band_rms=-30.0, peak=-8.0),
    "hit-metal-r08-b": dict(
        page="https://freesound.org/people/coolguy244e/sounds/267893/",
        preview="https://cdn.freesound.org/previews/267/267893_4657534-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.45, fade=0.2, band_rms=-30.0, peak=-8.0),
    "hit-organic-r08-a": dict(
        page="https://freesound.org/people/gprosser/sounds/360942/",
        preview="https://cdn.freesound.org/previews/360/360942_5406151-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.3, fade=0.12, band_rms=-30.0, peak=-8.0),
    "hit-organic-r08-b": dict(
        page="https://freesound.org/people/smidoid/sounds/49139/",
        preview="https://cdn.freesound.org/previews/49/49139_485393-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.4, fade=0.15, band_rms=-30.0, peak=-8.0),
    "hit-crumble-r08-a": dict(
        page="https://freesound.org/people/onteca/sounds/197772/",
        preview="https://cdn.freesound.org/previews/197/197772_1011133-hq.ogg",
        licence="CC-BY 3.0", offset=0.0, length=2.4, fade=0.8, peak=-1.5),
    # replaced the first b (NeoSpica "Rock Smash", 512243): almost all sub-bass with one spike,
    # so even high-passed it stayed peak-limited ~8 dB below a on its audible band.
    "hit-crumble-r08-b": dict(
        page="https://freesound.org/people/iwanPlays/sounds/567249/",
        preview="https://cdn.freesound.org/previews/567/567249_7108319-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.6, fade=0.6, band_rms=-25.0, peak=-1.5),
    "player-shield-hit-r08-a": dict(
        page="https://freesound.org/people/JoelAudio/sounds/136542/",
        preview="https://cdn.freesound.org/previews/136/136542_1206321-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.5, fade=0.2, band_rms=-24.0, peak=-2.0,
        lowpass=7000),  # 83 % of its energy was above 5 kHz: harsh for a frequent sound
    "player-shield-hit-r08-b": dict(
        page="https://freesound.org/people/StormwaveAudio/sounds/330629/",
        preview="https://cdn.freesound.org/previews/330/330629_3594951-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.9, fade=0.35, band_rms=-24.0, peak=-2.0),
    "player-shield-break-r08-a": dict(
        page="https://freesound.org/people/joe_bou_khalil/sounds/861848/",
        preview="https://cdn.freesound.org/previews/861/861848_19038210-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=1.4, fade=0.5, band_rms=-24.0, peak=-2.0),
    "player-shield-restore-r08-a": dict(
        page="https://freesound.org/people/qubodup/sounds/172631/",
        preview="https://cdn.freesound.org/previews/172/172631_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.8, fade=0.25, band_rms=-24.0, peak=-2.0),
    "player-shield-restore-r08-b": dict(
        page="https://freesound.org/people/Bychop/sounds/136881/",
        preview="https://cdn.freesound.org/previews/136/136881_2139644-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.6, fade=0.6, band_rms=-24.0, peak=-2.0),
    "player-armour-hit-r08-a": dict(
        page="https://freesound.org/people/JoMungus/sounds/726486/",
        preview="https://cdn.freesound.org/previews/726/726486_11865776-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.6, fade=0.3, band_rms=-24.0, peak=-2.0),
    "player-low-armour-r08-a": dict(
        page="https://freesound.org/people/magnuswaker/sounds/522162/",
        preview="https://cdn.freesound.org/previews/522/522162_11537497-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.4, fade=0.08, band_rms=-24.0, peak=-2.0),
    "player-destroyed-r08-a": dict(
        page="https://freesound.org/people/phantastonia/sounds/270616/",
        preview="https://cdn.freesound.org/previews/270/270616_5137631-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=3.5, fade=1.5, peak=-1.0,
        highpass=20),  # sub-sonic drift left a 0.008 DC offset after the fade-out
    "overdrive-start-r08-a": dict(
        page="https://freesound.org/people/GameAudio/sounds/220173/",
        preview="https://cdn.freesound.org/previews/220/220173_4100837-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.6, fade=0.15, band_rms=-24.0, peak=-2.0),
    "overdrive-end-r08-a": dict(
        page="https://freesound.org/people/Jerimee/sounds/521776/",
        preview="https://cdn.freesound.org/previews/521/521776_3202600-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.1, fade=0.4, band_rms=-24.0, peak=-2.0),
    "enemy-shot-small-r08-a": dict(
        page="https://freesound.org/people/humanoide9000/sounds/330293/",
        preview="https://cdn.freesound.org/previews/330/330293_4361321-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.45, fade=0.2, band_rms=-30.0, peak=-8.0),
    "enemy-shot-small-r08-b": dict(
        page="https://freesound.org/people/JavierZumer/sounds/257232/",
        preview="https://cdn.freesound.org/previews/257/257232_2836758-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.32, fade=0.12, band_rms=-30.0, peak=-8.0),
    "enemy-shot-heavy-r08-a": dict(
        page="https://freesound.org/people/SuperPhat/sounds/531861/",
        preview="https://cdn.freesound.org/previews/531/531861_7542558-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.75, fade=0.3, band_rms=-30.0, peak=-8.0),
    "enemy-shot-heavy-r08-b": dict(
        page="https://freesound.org/people/xkeril/sounds/702000/",
        preview="https://cdn.freesound.org/previews/702/702000_13504080-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.9, fade=0.4, band_rms=-30.0, peak=-8.0),
    "enemy-laser-warning-r08-a": dict(
        page="https://freesound.org/people/plasterbrain/sounds/351807/",
        preview="https://cdn.freesound.org/previews/351/351807_4284968-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.2, fade=0.3, band_rms=-30.0, peak=-8.0),
    "enemy-missile-r08-a": dict(
        page="https://freesound.org/people/NHMWretched/sounds/151858/",
        preview="https://cdn.freesound.org/previews/151/151858_2754532-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.6, fade=0.6, band_rms=-30.0, peak=-8.0),
    "enemy-screech-r08-a": dict(
        page="https://freesound.org/people/Khrinx/sounds/565024/",
        preview="https://cdn.freesound.org/previews/565/565024_1187042-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.2, fade=0.5, band_rms=-30.0, peak=-8.0),
    "enemy-screech-r08-b": dict(
        page="https://freesound.org/people/Wolfsinger/sounds/25713/",
        preview="https://cdn.freesound.org/previews/25/25713_176969-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=1.6, fade=0.5, band_rms=-30.0, peak=-8.0),
    "enemy-screech-r08-c": dict(
        page="https://freesound.org/people/AlienXXX/sounds/78539/",
        preview="https://cdn.freesound.org/previews/78/78539_97763-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=1.6, fade=0.5, band_rms=-30.0, peak=-8.0),
    "enemy-screech-r08-d": dict(
        page="https://freesound.org/people/jvmyka@gmail.com/sounds/556535/",
        preview="https://cdn.freesound.org/previews/556/556535_7802703-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.0, fade=0.5, band_rms=-30.0, peak=-8.0),
    # Vrell warp-in / spawn (Brood Pod bursting, Hive Node and Brood Carrier spawns)
    "enemy-spawn-r08-a": dict(
        page="https://freesound.org/people/darcyadam/sounds/651487/",
        preview="https://cdn.freesound.org/previews/651/651487_3379512-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.6, fade=0.6, band_rms=-30.0, peak=-6.0),
    "enemy-spawn-r08-b": dict(
        page="https://freesound.org/people/ThefitzyG/sounds/414296/",
        preview="https://cdn.freesound.org/previews/414/414296_6629239-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.7, fade=0.5, band_rms=-30.0, peak=-6.0),
    "enemy-lock-r08-a": dict(
        page="https://freesound.org/people/SamsterBirdies/sounds/467881/",
        preview="https://cdn.freesound.org/previews/467/467881_5487341-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.06, fade=0.08, band_rms=-30.0, peak=-8.0),
    "special-airstrike-jets-r08-a": dict(
        page="https://freesound.org/people/qubodup/sounds/189446/",
        preview="https://cdn.freesound.org/previews/189/189446_71257-hq.ogg",
        licence="CC0 1.0", offset=3.4, length=4.5, fade=1.5, fadein=0.6, band_rms=-24.0, peak=-1.5),
    "special-airstrike-bombs-r08-a": dict(
        page="https://freesound.org/people/craigsmith/sounds/483284/",
        preview="https://cdn.freesound.org/previews/483/483284_2524442-hq.ogg",
        licence="CC0 1.0", offset=0.95, length=5.0, fade=1.5, peak=-1.5),
    "special-smartbomb-r08-a": dict(
        page="https://freesound.org/people/Kinoton/sounds/369516/",
        preview="https://cdn.freesound.org/previews/369/369516_2247456-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=4.2, fade=1.5, peak=-1.5),
    "special-flares-r08-a": dict(
        page="https://freesound.org/people/OGsoundFX/sounds/423109/",
        preview="https://cdn.freesound.org/previews/423/423109_3325582-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=1.4, fade=0.6, band_rms=-24.0, peak=-1.5),
    "special-denied-r08-a": dict(
        page="https://freesound.org/people/Jacco18/sounds/419023/",
        preview="https://cdn.freesound.org/previews/419/419023_215268-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.31, fade=0.05, band_rms=-27.0, peak=-4.0),
    "ui-radio-open-r08-a": dict(
        page="https://freesound.org/people/JustinBW/sounds/70107/",
        preview="https://cdn.freesound.org/previews/70/70107_1022651-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.3, fade=0.05, band_rms=-27.0, peak=-4.0),
    "ui-radio-close-r08-a": dict(
        page="https://freesound.org/people/JovianSounds/sounds/524205/",
        preview="https://cdn.freesound.org/previews/524/524205_9561949-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.5, fade=0.2, band_rms=-27.0, peak=-4.0),
    "ui-klaxon-r08-a": dict(
        page="https://freesound.org/people/noirenex/sounds/159453/",
        preview="https://cdn.freesound.org/previews/159/159453_1656228-hq.ogg",
        licence="CC0 1.0", loop=(0.0, 4.76, 0.08, 'auto'), band_rms=-27.0, peak=-4.0),
    "ui-klaxon-r08-b": dict(
        page="https://freesound.org/people/zimbot/sounds/178032/",
        preview="https://cdn.freesound.org/previews/178/178032_1449999-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.95, fade=0.1, band_rms=-27.0, peak=-4.0),
    "ambience-orbit-r08-a": dict(
        page="https://freesound.org/people/Elektrocell/sounds/20705/",
        preview="https://cdn.freesound.org/previews/20/20705_53896-hq.ogg",
        licence="CC0 1.0", loop=(20.0, 16.0, 1.5), band_rms=-36.0, peak=-10.0),
    "ambience-luna-r08-a": dict(
        page="https://freesound.org/people/ztitchez/sounds/370754/",
        preview="https://cdn.freesound.org/previews/370/370754_3104030-hq.ogg",
        licence="CC-BY 4.0", loop=(10.0, 16.0, 1.5), band_rms=-36.0, peak=-10.0),
    "ambience-city-r08-a": dict(
        page="https://freesound.org/people/TRP/sounds/568975/",
        preview="https://cdn.freesound.org/previews/568/568975_97550-hq.ogg",
        licence="CC0 1.0", loop=(30.0, 20.0, 2.0), band_rms=-36.0, peak=-10.0),
    "ambience-ocean-r08-a": dict(
        page="https://freesound.org/people/byjoshberry/sounds/435668/",
        preview="https://cdn.freesound.org/previews/435/435668_5409980-hq.ogg",
        licence="CC-BY 4.0", loop=(2.0, 16.0, 2.0), band_rms=-36.0, peak=-10.0),
    "ambience-storm-r08-a": dict(
        page="https://freesound.org/people/FlatHill/sounds/237729/",
        preview="https://cdn.freesound.org/previews/237/237729_3839718-hq.ogg",
        licence="CC0 1.0", loop=(1.0, 24.0, 2.0), band_rms=-36.0, peak=-10.0),
    "ambience-arctic-r08-a": dict(
        page="https://freesound.org/people/cobratronik/sounds/117136/",
        preview="https://cdn.freesound.org/previews/117/117136_732072-hq.ogg",
        licence="CC0 1.0", loop=(30.0, 16.0, 2.0), band_rms=-36.0, peak=-10.0),
    # Round 21 (Level 05, M4 part E, user decision D8): the mass-driver sled's whine (played
    # while the rail lights chase, 1.5 s) and its pass (the 0.4 s run), the Polyp Mortar's lob
    # and its impact; a/b per sound.
    "hazard-sled-whine-r21-a": dict(          # a rail-gun charge hum, cut as a loop
        page="https://freesound.org/people/BaggoNotes/sounds/785400/",
        preview="https://cdn.freesound.org/previews/785/785400_15107322-hq.ogg",
        licence="CC0 1.0", loop=(0.0, 0.78, 0.04, 'auto'), band_rms=-30.0, peak=-8.0),
    "hazard-sled-whine-r21-b": dict(          # an electrical machine charging up: the last 1.6 s of its rise
        page="https://freesound.org/people/JavierZumer/sounds/257229/",
        preview="https://cdn.freesound.org/previews/257/257229_2836758-hq.ogg",
        licence="CC-BY 4.0", offset=2.0, length=1.6, fade=0.1, fadein=0.3, band_rms=-30.0, peak=-8.0),
    "hazard-sled-pass-r21-a": dict(           # a shipboard rail-gun crack with its tail
        page="https://freesound.org/people/deleted_user_1941307/sounds/155790/",
        preview="https://cdn.freesound.org/previews/155/155790_1941307-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.8, fade=0.7, band_rms=-24.0, peak=-3.0),
    "hazard-sled-pass-r21-b": dict(           # a large object rushing past, low rumble
        page="https://freesound.org/people/mattpavone/sounds/76175/",
        preview="https://cdn.freesound.org/previews/76/76175_1163073-hq.ogg",
        licence="CC0 1.0", offset=0.3, length=1.6, fade=0.7, fadein=0.15, band_rms=-24.0, peak=-3.0),
    "enemy-mortar-lob-r21-a": dict(           # a real mortar fired, with reverb
        page="https://freesound.org/people/Mozfoo/sounds/529239/",
        preview="https://cdn.freesound.org/previews/529/529239_8708205-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.2, fade=0.6, band_rms=-27.0, peak=-6.0),
    "enemy-mortar-lob-r21-b": dict(           # an organic spit
        page="https://freesound.org/people/noahpardo/sounds/352404/",
        preview="https://cdn.freesound.org/previews/352/352404_6246023-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.6, fade=0.15, band_rms=-27.0, peak=-6.0),
    "enemy-mortar-impact-r21-a": dict(        # a moist splat
        page="https://freesound.org/people/JustInvoke/sounds/446115/",
        preview="https://cdn.freesound.org/previews/446/446115_758593-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.69, fade=0.3, band_rms=-27.0, peak=-4.0),
    "enemy-mortar-impact-r21-b": dict(        # acid bubbling up: a sizzle
        page="https://freesound.org/people/spookymodem/sounds/202094/",
        preview="https://cdn.freesound.org/previews/202/202094_3756348-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.6, fade=0.6, band_rms=-30.0, peak=-8.0),
    # Round 23 (Level 06, M4 part F, user decision D8): the Mantis's 0.6 s telegraph and its
    # 1.2 s beam sweep, the perimeter beacon's flare launch and the falling flare's burn (a loop
    # under its 8 s fall), the Coilwyrm's 0.6 s head regrowth; a/b per sound.
    "enemy-mantis-telegraph-r23-a": dict(     # a bright laser charge-up, cut in its rise
        page="https://freesound.org/people/magnuswaker/sounds/588242/",
        preview="https://cdn.freesound.org/previews/588/588242_11537497-hq.ogg",
        licence="CC0 1.0", offset=0.1, length=0.6, fade=0.06, band_rms=-30.0, peak=-8.0),
    "enemy-mantis-telegraph-r23-b": dict(     # a lower, buzzing charge, its loudest 0.6 s
        page="https://freesound.org/people/StavSounds/sounds/701702/",
        preview="https://cdn.freesound.org/previews/701/701702_7862587-hq.ogg",
        licence="CC0 1.0", offset=0.6, length=0.6, fade=0.06, fadein=0.05, band_rms=-30.0, peak=-8.0),
    "enemy-mantis-sweep-r23-a": dict(         # a steady game-style laser beam
        page="https://freesound.org/people/Jofae/sounds/352852/",
        preview="https://cdn.freesound.org/previews/352/352852_6512973-hq.ogg",
        licence="CC0 1.0", offset=0.2, length=1.3, fade=0.25, fadein=0.02, band_rms=-27.0, peak=-6.0),
    "enemy-mantis-sweep-r23-b": dict(         # a 1.2 kHz death-ray tone with crackle
        page="https://freesound.org/people/zimbot/sounds/177099/",
        preview="https://cdn.freesound.org/previews/177/177099_1449999-hq.ogg",
        licence="CC-BY 4.0", offset=1.0, length=1.3, fade=0.25, fadein=0.02, band_rms=-27.0, peak=-6.0),
    "hazard-flare-launch-r23-a": dict(        # a flare pistol shot: pop and a short hiss
        page="https://freesound.org/people/marb7e/sounds/674375/",
        preview="https://cdn.freesound.org/previews/674/674375_13732472-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.8, fade=0.4, band_rms=-27.0, peak=-4.0),
    "hazard-flare-launch-r23-b": dict(        # a firework rocket igniting and climbing away
        page="https://freesound.org/people/derplayer/sounds/587173/",
        preview="https://cdn.freesound.org/previews/587/587173_13123807-hq.ogg",
        licence="CC0 1.0", offset=0.4, length=1.6, fade=0.7, fadein=0.05, band_rms=-37.0, peak=-8.0),
    "hazard-flare-burn-r23-a": dict(          # a road flare's steady sputter, cut as a loop
        page="https://freesound.org/people/frankelmedico/sounds/348766/",
        preview="https://cdn.freesound.org/previews/348/348766_299928-hq.ogg",
        licence="CC0 1.0", loop=(4.0, 3.0, 0.5), band_rms=-34.0, peak=-10.0),
    "hazard-flare-burn-r23-b": dict(          # a road flare burning close by, a hissier loop
        page="https://freesound.org/people/theshaggyfreak/sounds/317834/",
        preview="https://cdn.freesound.org/previews/317/317834_8335-hq.ogg",
        licence="CC-BY 4.0", loop=(3.0, 3.0, 0.5), band_rms=-34.0, peak=-10.0),
    "enemy-coilwyrm-regrow-r23-a": dict(      # wet slime stretching: flesh knitting into a head
        page="https://freesound.org/people/Archos/sounds/433826/",
        preview="https://cdn.freesound.org/previews/433/433826_652422-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.6, fade=0.15, band_rms=-27.0, peak=-6.0),
    "enemy-coilwyrm-regrow-r23-b": dict(      # an insect growl and chitter: the new head waking
        page="https://freesound.org/people/SecureSubset/sounds/800277/",
        preview="https://cdn.freesound.org/previews/800/800277_16752880-hq.ogg",
        licence="CC0 1.0", offset=0.1, length=0.6, fade=0.15, fadein=0.03, band_rms=-27.0, peak=-6.0),
}


def fetch(url, cache):
    out = cache / url.rsplit("/", 1)[1]
    if not out.exists():
        subprocess.run(["curl", "-sS", "-L", "-f", "-A", "Mozilla/5.0", "-o", str(out), url],
                       check=True)
    return out


def trim_leading_silence(x, threshold_db=-40.0, pre=0.003):
    level = db(np.max(np.abs(x), axis=0) + 1e-12) - db(np.max(np.abs(x)))
    first = int(np.argmax(level > threshold_db))
    return x[:, max(0, first - int(pre * SR)):]


def make_loop(x, start, length, xfade, curve="power"):
    a, n, f = int(start * SR), int(length * SR), int(xfade * SR)
    seg = x[:, a:a + n + f].copy()
    head, tail = seg[:, :f].copy(), seg[:, n:n + f]
    linear = curve == "auto" and np.corrcoef(head.ravel(), tail.ravel())[0, 1] > 0.5
    if linear:
        fin = np.linspace(0, 1, f)
        fout = 1 - fin
    else:
        t = np.linspace(0, np.pi / 2, f)
        fin, fout = np.sin(t), np.cos(t)
    seg[:, :f] = head * fin + tail * fout
    return seg[:, :n]


def band_rms_db(x, lo=200.0, hi=5000.0):
    mono = x.mean(axis=0)
    spec = np.fft.rfft(mono)
    freqs = np.fft.rfftfreq(len(mono), 1 / SR)
    spec[(freqs < lo) | (freqs >= hi)] = 0
    return db(np.sqrt(np.mean(np.fft.irfft(spec, len(mono)) ** 2)) + 1e-12)


def normalize(x, src):
    if "band_rms" not in src:
        return normalize_peak(x, src["peak"])
    y = x * 10 ** ((src["band_rms"] - band_rms_db(x)) / 20)
    ceiling = 10 ** (src["peak"] / 20)
    return y * min(1.0, ceiling / np.max(np.abs(y)))


def process(src, raw):
    x = trim_leading_silence(decode(raw))
    if "loop" in src:
        x = make_loop(x, *src["loop"])
        x -= x.mean(axis=1, keepdims=True)
        return normalize(x, src)
    start = int(src["offset"] * SR)
    x = x[:, start:start + int(src["length"] * SR)].copy()
    x -= x.mean(axis=1, keepdims=True)  # remove DC
    if "highpass" in src:
        x = np.array([svf(svf(ch, src["highpass"], mode="hp"), src["highpass"], mode="hp")
                      for ch in x])
    if "lowpass" in src:
        x = np.array([svf(svf(ch, src["lowpass"]), src["lowpass"]) for ch in x])
    n_in = int(src.get("fadein", 0.002) * SR)
    n_out = min(x.shape[1], int(src["fade"] * SR))
    x[:, :n_in] *= np.linspace(0, 1, n_in)
    x[:, -n_out:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n_out))) ** 2
    return normalize(x, src)


def main(argv):
    cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    if "--cache" in argv:
        i = argv.index("--cache")
        cache = Path(argv[i + 1])
        del argv[i:i + 2]
    cache.mkdir(parents=True, exist_ok=True)
    for name, src in SOURCES.items():
        if argv and name not in argv:
            continue
        out_dir = SFX / "rejected" if src.get("rejected") else SFX
        out = write_ogg(out_dir / f"{name}.ogg", process(src, fetch(src["preview"], cache)),
                        max_peak_db=src["peak"])
        print(f"wrote {out.relative_to(ROOT)}  <- {src['page']} ({src['licence']})")


if __name__ == "__main__":
    main(sys.argv[1:])
