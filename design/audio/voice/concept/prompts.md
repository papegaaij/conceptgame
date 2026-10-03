# Voice briefs and prompts — concept round 19

Round 19 casts the generic radio speakers of Act 1. Each speaker says one of its real lines with
two candidate reference voices (`-a`, `-b`), cloned zero-shot by Chatterbox and put through
radio filter b (round 18's choice). Reproduce with:

```
python3 tools/concept/audio/tts_r19.py          # generate (Chatterbox venv) + post-process
python3 tools/concept/audio/tts_r19.py --post   # post-process only (raw WAVs in ~/.cache/tv-tts/raw/r19/)
```

Generator: `tools/concept/audio/tts_r19.py` (Chatterbox 0.1.7, model `ResembleAI/chatterbox`,
set up as in `tools/concept/audio/tts_r18.py`; the filter is `radio()` there). Seed 19 + the
take's index, except Tranquility Control a (seed 4019: five other seeds ran on past the line).
Files `voice-<speaker>-r19-<a|b>.ogg`, OGG Vorbis q4, 44.1 kHz mono, −16 LUFS. Settings are
`exaggeration` / `cfg_weight` / `temperature` from the [voice](../README.md#speakers-and-expression)
expression table. Reference clips: [refs](../refs/README.md), sources in
[CREDITS.md](../../../../CREDITS.md). Pitch is the median F0 of the reference clip (female
≈ 165–255 Hz, male ≈ 85–155 Hz).

## voice-yard-control

Yard Control (L02, t 78), fierce (0.8 / 0.4 / 0.8): "Crane Four is still on automatic. Watch the arm, Aegis!"

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Mark Nelson | [A Princess of Mars, foreword (Burroughs)](https://archive.org/details/princess_mars_0810_librivox), cut at 60 s | public domain | 145 Hz |
| b | Elizabeth Klett | [Lady Audley's Secret, ch. 1 (Braddon)](https://archive.org/details/lady_audleys_secret_ek_librivox), cut at 60 s | public domain | 177 Hz |

## voice-dock

Dock One–Four (L02, t 34 (Dock One; the four docks share the lines)), shout (1.2 / 0.3 / 0.8): "Dock One to anyone! They're growing on the hull — burn it off us!"

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Tom Crawford | [The Sea Wolf, ch. 1 (London)](https://archive.org/details/sea_wolf_0907_librivox), cut at 60 s | public domain | 93 Hz |
| b | Kristin Hughes | [The Strange Case of Dr. Jekyll and Mr. Hyde, ch. 1 (Stevenson)](https://archive.org/details/jekyll_and_hyde_klh_0904_librivox), cut at 60 s | public domain | 167 Hz |

## voice-ring-control

Ring Control (L03, t 36.5), neutral (0.5 / 0.5 / 0.7): "Debris field ahead. Big pieces will stop your rounds. And theirs."

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Bob Neufeld | [A Study in Scarlet, part 1 ch. 1 (Doyle)](https://archive.org/details/studyinscarlet_bn_librivox), cut at 60 s | CC0 1.0 | 143 Hz |
| b | Cori Samuel | [Frankenstein (1818), preface (Shelley)](https://archive.org/details/frankenstein_cs_librivox), cut at 60 s | CC0 1.0 | 222 Hz |

## voice-tranquility-control

Tranquility Control (L04, t 86), shout (1.2 / 0.3 / 0.8): "Walkers coming over the crater rims, both sides of the road!"

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | John Greenman | [Adventures of Huckleberry Finn, explanatory note (Twain)](https://archive.org/details/huckleberry_finn_0908_librivox), cut at 60 s | public domain | 131 Hz |
| b | Karen Savage | [The Scarlet Pimpernel, ch. 1 (Orczy)](https://archive.org/details/scarlet_pimpernel_ks_librivox), cut at 60 s | public domain | 198 Hz |

## voice-crawler-one

Crawler One (L04, t 11), neutral (0.5 / 0.5 / 0.7): "Crawler One rolling. We're slow and we're loud, Aegis. Please don't leave us."

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Phil Chenevert | [Pollyanna, ch. 1 (Porter)](https://archive.org/details/pollyanna-1_pc_librivox), cut at 60 s | CC0 1.0 | 164 Hz |
| b | Kara Shallenberg | [Heidi, introduction (Spyri)](https://archive.org/details/heidi_solo_librivox), cut at 60 s | public domain | 197 Hz |

## voice-convoy

Convoy (L04, first crawler hit, {ally} = Three), shout (1.2 / 0.3 / 0.8): "We're taking fire! Crawler {ally} is hit!"

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Adrian Praetzellis | [Treasure Island, front matter (Stevenson)](https://archive.org/details/treasure_island_ap_librivox), cut at 60 s | public domain | 118 Hz |
| b | Judy Bieber | [Dorothy and the Wizard in Oz, "To My Readers" (Baum)](https://archive.org/details/dorothy_wizard_oz_librivox), cut at 60 s | public domain | 211 Hz |

## voice-hammer-lead

Hammer Lead (Airstrike special, the call), fierce (0.8 / 0.4 / 0.8): "Hammer flight, inbound!"

| Variant | Reader | Source | Licence | Clip pitch |
|---|---|---|---|---|
| a | Mark F. Smith | [The Lost World, ch. 1 (Doyle)](https://archive.org/details/lost_world_mfs_librivox), cut at 120 s | public domain | 102 Hz |
| b | Gord Mackenzie | [Scaramouche, book 1 ch. 1 (Sabatini)](https://archive.org/details/scaramouche_gm_librivox), cut at 60 s | public domain | 100 Hz |
