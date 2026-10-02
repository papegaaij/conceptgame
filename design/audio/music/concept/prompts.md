# Music briefs and prompts — concept round 01

Two short sketches (~40 s each) of the same job, an in-level action theme, in the two
candidate styles for the game's soundtrack. They are code-generated mockups that show style,
structure and melody. They are not production quality. Reproduce with:

```
python3 tools/concept/audio/music.py          # writes both files into this directory
python3 tools/concept/audio/music.py a        # only sketch A (b for sketch B)
python3 tools/concept/audio/analyze.py design/audio/music/concept/*.ogg
```

Generator: `tools/concept/audio/music.py` (synth library `tools/concept/audio/synth.py`).
Both pieces are written as tracker-style patterns: one token per 16th step, `A4` starts a note,
`-` holds it, `.` is a rest. The melodies can be read and edited directly in the script.
Mastering: −14 LUFS integrated, sample-peak ceiling −2 dBFS (true peak ≈ −1 dBTP). Format:
OGG Vorbis q6, 44.1 kHz stereo, fixed seeds.

---

## music-r01-a — "Afterburner" (tracker-style trance/techno, 37.5 s)

**Style**: late-90s tracker/demoscene trance-techno, as in the Tyrian / Raptor / Amiga
module soundtracks: four-on-the-floor drive, a rolling off-beat bass, a 16th arpeggio, a
detuned supersaw lead with a dotted-eighth echo, and sidechain "pump" on the bass and pads.

**Tempo / key**: 140 BPM, 4/4, A minor (natural minor with a harmonic-minor E major dominant).

**Structure** (bars of 1.71 s):

| Bars | Section | What happens |
|---|---|---|
| 1–4 | Intro | Pad + 16th arpeggio with its low-pass opening from 500 Hz to ~3 kHz; hats from bar 2; kick and rolling bass enter at bar 3; breakbeat fill (snare/tom) on the last two beats of bar 4 |
| 5–12 | A | Crash; full groove (kick, clap on 2 and 4, 16th hats, open hats on the off-beats); lead melody, theme statement; breakbeat fill in bar 12 |
| 13–20 | B | Crash; the melody climbs into a higher, more heroic answer over new chords; noise riser and a 16th snare roll crescendo in bars 19–20; one beat of silence |
| 21 | Ending | Kick, crash, A minor chord, lead holds A5, reverb tail |

**Chord progression**:
- Intro: `Am | F | C | G`
- A: `Am | F | C | G | Am | F | G | E`
- B: `Dm | Am | F | E | Dm | Am | F | E` → final `Am`

**Melody** (lead, A section): starts A4-C5-E5, then falls D5-C5-B4-C5. The main hook is the
dotted rhythm (3+3+2 feel) over the first bar. B answers from F5 up to A5 / C6 / D6, with the
leading tone G♯ over each E chord.

**AI prompt** (Suno/Udio style): *"1990s tracker module trance techno, video game shoot'em up
level music, 140 BPM, A minor, driving four-on-the-floor kick, rolling off-beat bass,
16th-note square arpeggios, detuned supersaw lead with dotted-eighth delay, sidechain pumping
pads, breakbeat drum fills, heroic melodic hook, Amiga demoscene, Tyrian, Raptor: Call of the
Shadows, instrumental, loopable."*

---

## music-r01-b — "Coalition Rising" (synth-orchestral action, 40.7 s)

**Style**: synth-orchestral, as in the late-90s action scores made on hardware synths and
samplers (Colony Wars, Wing Commander, Einhänder). A galloping string ostinato, a heroic horn
motif, off-beat brass stabs, taiko-style big drums, timpani and cymbal crashes.

**Tempo / key**: 132 BPM, 4/4, D minor (the A major dominant uses C♯).

**Structure** (bars of 1.82 s):

| Bars | Section | What happens |
|---|---|---|
| 1–4 | Intro | Galloping low string ostinato on D (1-2-3 accent pattern), string pad swelling in, timpani on each downbeat; horn call D4 → A4 → C♯5–E5 in bars 3–4; timpani roll and riser into A |
| 5–12 | A | Crash + timpani; big drums in a 3-3-2 pattern (steps 1, 4, 7, 9, 12, 15) with snare on 2 and 4; the heroic horn motif in brass, doubled an octave lower; snare roll in bar 12 |
| 13–20 | B | The theme lifts an octave into high strings with brass underneath; ostinato doubles at the octave; brass stabs on the off-beats of 2 and 4; hats; snare roll into the end |
| 21 | Ending | Tutti D minor hit (strings, brass, high D6, low D2), taiko, timpani, crash, long reverb tail |

**Chord progression**:
- Intro: `Dm | Dm | B♭ | A`
- A: `Dm | B♭ | C | Dm | Dm | B♭ | Gm | A`
- B: `B♭ | C | F | Dm | Gm | A | B♭ | A` → final `Dm`

**Melody** (horn motif): D4 held, then A4 A4 (dotted rhythm), leaping to D5. This is the
"call to arms" rising fourth and fifth, and it works as a leitmotif for the Terran Coalition.
The second phrase falls D5-C5-B♭4-C5. B restates it higher (F5-D5-F5-B♭5, …) and climbs through
Gm and A to C♯6 before resolving to the final D.

**AI prompt** (Suno/Udio style): *"Late 1990s synth-orchestral video game action score,
132 BPM, D minor, galloping staccato string ostinato, heroic French horn and brass motif,
off-beat brass stabs, taiko war drums, timpani rolls, cymbal crashes, military snare, epic
space battle, Colony Wars, Wing Commander, hardware synth orchestra, instrumental, loopable."*

---

## Notes for choosing

- A leans into the retro-arcade identity. B leans into the serious war story. A hybrid is
  also possible: electronic drums and bass under orchestral strings and brass, a common late-90s
  approach (e.g. for boss and story tracks while levels stay electronic).
- Both sketches are written so the hook can become the game's main theme. Whichever style is
  chosen, the other's melody can be reused as a leitmotif.

---

# Round 02 — five loopable themes

Round 01 outcome: both sketches were chosen and became the Act 1 level themes A ("Afterburner")
and B ("Coalition Rising"). Round 02 composes five more themes in the same two idioms,
tracker-era electronic and synth-orchestral, and blends them where the brief asks for it.

Reproduce with:

```
python3 tools/concept/audio/music_r02.py                 # all five (~3.5 min)
python3 tools/concept/audio/music_r02.py title boss      # or any of: title hangar boss mars europa
python3 tools/concept/audio/analyze.py design/audio/music/concept/*-r02-a.ogg
```

Generator: `tools/concept/audio/music_r02.py` (reuses the instruments of `music.py` and the
synth in `synth.py`). Mastering is the same as round 01: −14 LUFS integrated, −2 dBFS sample
ceiling, OGG Vorbis q6, fixed seeds.

**Loop format.** Each file is *intro → loop → 2-bar fade-out tail*. The loop points are
stored in the file as Vorbis comments `LOOPSTART` and `LOOPLENGTH` (in samples at 44.1 kHz).
A looping player jumps from `LOOPSTART + LOOPLENGTH` back to `LOOPSTART` and never plays the
tail. The tail exists only so the file ends cleanly in an ordinary player. The loop was rendered
twice and the second pass kept, so reverb and echo tails carry across the seam. Tempos were
picked so a 16th note is a whole number of samples, which makes the loop exact. The loop starts
one beat after the intro ends, because the intro joins the loop through a one-beat crossfade.

**Motifs** (defined in the [music README](../README.md#motifs)):
- humanity's **main motif** is degrees 1-5-8-9-♭10 (D minor: D A D E F);
- the Vrell **Choir motif** is 8-♭7-♭6-5, a tritone drop to ♭2, then 1 (E: E D C B F E).

---

## title-theme-r02-a — "Terran Vanguard" (title / main menu, 57.6 s)

**Brief**: the game's calling card. Heroic and memorable, and the first statement of
humanity's motif. It blends the two chosen styles: an orchestral core (gallop strings, horns,
timpani, taiko) on a tracker backbone (four-on-the-floor kick, clap, 16th hats, pulse
arpeggio, rolling bass, supersaw lead).

**Tempo / key**: 126 BPM, D minor → F major lift.

**Loop**: `LOOPSTART` 357000 (8.095 s), `LOOPLENGTH` 2016000 (45.714 s = 24 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Call | Timpani and swelling strings; horns call the motif slowly (D… A… D-E-F); filtered arp enters; snare roll and riser |
| L1–8 | A | Crash; the motif in brass (with a lower octave) over gallop strings, taiko hits, four-on-the-floor beat, quiet arp |
| L9–16 | B | The motif in **F major** (F-C-F-G-A) in supersaw lead + strings, brass underneath; rolling tracker bass, 16th hats, taiko on 1 and 3 |
| L17–20 | C: breakdown | Drums out; strings, arp opening up, the motif at half speed in low brass |
| L21–24 | C: build | Beat back; brass stabs from quarters to 8ths; gallop; snare roll into the loop start |

**Chords**:
- Intro: `Dm | Dm | B♭ | A`
- A: `Dm | B♭ | F | C | Dm | B♭ | Gm | A`
- B: `F | C | Dm | B♭ | F | C | B♭ | A`
- C: `Dm | B♭ | F | A | Dm | B♭ | C | A` → loop

**AI prompt**: *"Heroic 1990s video game title theme, 126 BPM, D minor with a lift to F major,
synth-orchestral brass fanfare motif over galloping staccato strings, taiko and timpani,
combined with a tracker-style four-on-the-floor beat, rolling bass and square-wave arpeggios,
detuned supersaw lead in the second half, epic space war, Wing Commander meets Tyrian,
instrumental, seamless loop."*

---

## hangar-theme-r02-a — "Dry Dock" (hangar / shop, 64.7 s)

**Brief**: music to shop to between missions. Calm, groovy and warm, and built to survive
dozens of repeats: no drops, no harsh highs (the top octaves sit 15–25 dB below the mids),
narrow dynamics and a melody that rests more than it plays. The feel is mid-90s downtempo
tracker (swung 16ths), with faint metallic hangar clanks as the "mechanical ambience".

**Tempo / key**: 90 BPM, 16ths swung by 30 %, D dorian.

**Loop**: `LOOPSTART` 264600 (6.000 s), `LOOPLENGTH` 2352000 (53.333 s = 20 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–2 (intro) | Doors open | Electric-piano chords and soft pad, distant clanks, shaker fading in |
| L1–8 | A | Groove: soft kick (1, 2a, 3&), laid-back snare on 2 and 4, swung hats and shaker, round sub bass, FM electric piano comping on 1 and the "and" of 3 with suitcase tremolo |
| L9–16 | B | Flute melody. Its first phrase is a relaxed fragment of humanity's motif (A-D-E-F = 5-8-9-♭10) |
| L17–20 | C | Breakdown: rim clicks instead of drums, long bass notes, electric-piano "vibes" answer |

**Chords** (rootless electric-piano voicings): `Dm9 | G13 | Dm9 | G13 | B♭maj9 | Am7 | Gm9 | A7`
(A and B); C = the second half `B♭maj9 | Am7 | Gm9 | A7` → loop.

**AI prompt**: *"Relaxed downtempo lounge groove, 90 BPM swung 16ths, D dorian, warm FM
electric piano chords with tremolo, round sub bass, soft brushed-sounding drums and shaker,
gentle flute melody, faint distant metallic workshop clanks, 1990s tracker module chill-out,
spaceship hangar shop music, unobtrusive, not fatiguing on repeat, instrumental, seamless
loop."*

---

## boss-theme-r02-a — "The Choir Descends" (Vrell boss, 61.2 s)

**Brief**: intense, fast and dark: the Vrell act bosses. The Choir motif is the core, sung by
a wordless formant choir (over an "oo" drone chord in the intro) and doubled by low brass.
Pounding drums, a distorted 16th bass ostinato with a ♭2 bite, orchestral stabs, and a
tritone-laden synth riff.

**Tempo / key**: 150 BPM, E minor / E phrygian (♭2 = F), with B major as the dominant.

**Loop**: `LOOPSTART` 299880 (6.800 s), `LOOPLENGTH` 2257920 (51.200 s = 32 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | The Choir | Low E drone, the Choir motif alone over taiko; distorted bass and a snare roll build over bars 3–4 |
| L1–8 | A | Full fight: kick with pushes, snare on 2 and 4, taiko, tom fills; Choir motif (bars 1–2 and 5–6) with brass; orchestral stabs on a 3-3-2 grid; choir chords in between |
| L9–16 | B | Synth riff (E-G-E-F-B♭-A…, tritone E–B♭) over the ostinato; high string tremolo; 16th hats |
| L17–24 | A′ | Choir motif returns, with the riff two octaves down as a counterline |
| L25–28 | C: half-time | Kick/snare in half time, taiko 3-3-2, the Choir motif at half speed in low brass (E D C B → F … E) |
| L29–32 | C: build | Four-on-the-floor, snare 8ths → 16ths, riser into the loop start |

**Chords** (two bars each):
- A / A′: `Em | C | F | B`
- B: `Em | C | Am | B`
- C: `Em | F | C | B` → loop

**AI prompt**: *"Intense dark boss battle music, 150 BPM, E phrygian, eerie wordless choir
singing a descending phrase with a tritone drop, pounding taiko and electronic drums,
distorted 16th-note bass ostinato, orchestral brass stabs, aggressive detuned synth riff,
alien hive mind, 1990s synth-orchestral and tracker hybrid, instrumental, seamless loop."*

---

## mars-theme-r02-a — "Red Dust Run" (Act 3 level theme A, 53.8 s)

**Brief**: Mars: driving, dusty, dry heat. Breakbeat energy under a desert scale: E phrygian
dominant (E F G♯ A B C D, "Hijaz"). The world doc's mood is carried by a desert-wind drone
(band-passed noise that wanders), hand drums (doum/tek pattern and shaker), and a lonely
nasal reed lead that scoops into its notes. A detuned reese bass drives it. The dust-storm
breakdown muffles the drums and swells the wind, mirroring the level hazard.

**Tempo / key**: 135 BPM, E phrygian dominant; the B section uses the Andalusian cadence
(Am–G–F–E).

**Loop**: `LOOPSTART` 333200 (7.556 s), `LOOPLENGTH` 1881600 (42.667 s = 24 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Desert | Wind drone, hand drums, low E drone, the reed's lonely call; snare pickup |
| L1–8 | A | Breakbeat (kick on 1, 1&, 3&, 3a; snares on 2, 2a, 3e, 4, 4a), reese bass, reed melody climbing E-F-G♯, tom fills every 4 bars |
| L9–16 | B | Andalusian cadence; 16th arpeggio in the scale; the melody an octave higher |
| L17–20 | C: dust storm | Drums low-passed to 700 Hz, hand drums louder, wind swells, long bass notes, the reed call |
| L21–24 | C: build | Drum filter opens back up, arp returns, snare roll; F → E (♭II–I) back into the loop |

**Chords**:
- A: `E | F | E | Dm | Am | F | Dm | E`
- B: `Am | G | F | E | Am | G | F | E`
- C: `E | E | F | F | Dm | Dm | F | F` → loop (E)

**AI prompt**: *"Driving desert breakbeat, 135 BPM, E phrygian dominant scale, dusty
amen-style breakbeat, darbuka and shaker hand percussion, detuned reese bass, nasal
mizmar-like reed lead melody with pitch scoops, howling desert wind drone, dust storm
breakdown with muffled drums, Mars colony, 1990s tracker module, Red Faction meets Tyrian,
instrumental, seamless loop."*

---

## europa-theme-r02-a — "Thera Deep" (Act 4 level theme A, 58.1 s)

**Brief**: under the ice of Europa. Ambient trance heard through water: the whole band runs
through a low-pass (1.8 kHz in the A section) that opens to 4.8 kHz in the B section as
if "surfacing", then sinks to 1.3 kHz in the deep breakdown. Sonar pings, whale-like glides and
bubbles stay outside the filter with long echoes, so they sound "in the water" around the
listener. Mysterious, but kept propulsive for action by a steady kick and a sidechain-pumped
rolling bass.

**Tempo / key**: 125 BPM, F minor (C major as the dominant, D♭maj7 with a ♯11 colour).

**Loop**: `LOOPSTART` 359856 (8.160 s), `LOOPLENGTH` 2032128 (46.080 s = 24 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Descent | Pads behind a closed filter, sonar pings, a distant whale, the bass stirring in bars 3–4 |
| L1–8 | A | Muffled four-on-the-floor, open hats on the off-beats, pumping rolling bass, pluck arpeggio in long ping-pong echoes, sonar every 2 bars |
| L9–16 | B | Filter opens; the lead melody (supersaw with a flute an octave below) enters, soft claps |
| L17–20 | C: the deep | No drums; sub bass notes, whale song, echoing flute fragments, filter at its darkest |
| L21–24 | C: build | Drums return, riser, the filter comes back to its A-section setting at the loop point |

**Chords** (voicings: Fm9 = F-A♭-C-G, D♭maj7, B♭m9, Csus4 → C):
- A: `Fm9 | Fm9 | D♭ | D♭ | B♭m9 | B♭m9 | Csus4 | C`
- B: `D♭ | E♭ | Fm9 | Fm9 | D♭ | E♭ | Csus4 | C`
- C: as A → loop

**AI prompt**: *"Underwater ambient trance, 125 BPM, F minor, muffled low-passed mix as if
heard through water, steady soft four-on-the-floor kick, sidechain-pumping rolling bass,
plucked synth arpeggio drowning in ping-pong echoes, sonar pings, whale song, bubbles, lush
pads, filter slowly opening for a mysterious lead melody, deep sea city, The Abyss, 1990s
trance, instrumental, seamless loop."*

---

# Round 03 — five more loopable themes

Round 02 outcome: all five themes were chosen. Round 03 covers the rest of the campaign's
emotional arc: the fight for Earth, the twist in the belt, the hunt through Jupiter's storms,
the Ascendancy's own boss music, and the finale.

Reproduce with:

```
python3 tools/concept/audio/music_r03.py        # all five (~5 min)
python3 tools/concept/audio/music_r03.py belt   # or any of: earth belt jovian ascendancy final
python3 tools/concept/audio/analyze.py design/audio/music/concept/*-r03-a.ogg
```

Generator: `tools/concept/audio/music_r03.py` (builds on `music_r02.py`). The loop format
and mastering are identical to round 02: intro → loop → 2-bar fade-out tail,
`LOOPSTART`/`LOOPLENGTH` Vorbis comments in samples, −14 LUFS, −2 dBFS ceiling.

**Vorne's motif** is new in this round. It is degrees **1 – ♭3 – 5 – 7**, a minor-major-seventh
arpeggio, in a stately rhythm (dotted quarter, eighth, half, whole). In B minor:
B D F♯ A♯. The unresolved major 7th over a minor chord is the "cold" in it: it sounds
ordered, correct and wrong at the same time.

---

## earth-theme-r03-a — "Homefront" (Act 2 level theme A, 55.9 s)

**Brief**: the fight for home. Urgent and heroic but embattled: megacities under siege,
stormy seas, the arctic. Humanity's motif in a new guise: diminished into a relentless
16th-note figure (1-5-8-9-♭3-9-8-5) that the strings hammer under everything, re-rooted on each
chord (minor or major third as the chord demands). The world doc asks for heavier drums, choir
pads and the main theme in a minor key: urgent breakbeat, orchestral hits, a human choir, an
air-raid siren in the intro.

**Tempo / key**: 147 BPM, C minor (G major as the dominant).

**Loop**: `LOOPSTART` 306000 (6.939 s), `LOOPLENGTH` 2016000 (45.714 s = 28 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Air raid | Siren rising and falling, choir swelling, timpani, the motif called by a far horn; the riff starts as a filtered pluck in bars 3–4; snare roll |
| L1–8 | A | Breakbeat (kick on 1, 1a, 3, 3&; ghost snares), the riff in strings with a pulse double, low brass pushing 8ths, orchestral hits every 4 bars, choir |
| L9–16 | B | Horns state the motif in full (C-G-C-D-E♭), trumpets an octave up; the riff drops an octave |
| L17–20 | C | Embattled: half-time drums, taiko, big choir, a supersaw lament; the riff as a quiet pluck |
| L21–28 | D | Everything: horns and supersaw lead on the motif, riff, breakbeat; snare roll and riser into the loop |

**Chords**:
- Intro: `Cm | Cm | A♭ | G`
- A: `Cm | Cm | A♭ | A♭ | Fm | Fm | G | G`
- B and D: `Cm | A♭ | Fm | G | Cm | A♭ | B♭ | G`
- C: `A♭ | Fm | Cm | G`

**AI prompt**: *"Urgent heroic war theme, 147 BPM, C minor, relentless 16th-note string
ostinato built from a rising heroic motif, driving breakbeat with orchestral hits, big choir
pads, French horns stating the theme, air raid siren intro, embattled half-time lament
section, cities under siege, 1990s synth-orchestral video game music, instrumental, seamless
loop."*

---

## belt-theme-r03-a — "Hollow Rock" (Act 5 level theme A, 60.9 s)

**Brief**: the asteroid belt and its mining stations: industrial, metallic and cold. This is
where the twist lands. Act 5 opens with Vorne's broadcast, so the intro is radio static,
two broadcast beeps, and his motif played by cold brass and organ. After that the machines play
his arpeggio: every sequencer line in the groove is 1-♭3-5-7. Metallic percussion (tuned anvil
hits on a 16th grid, steam bursts, a conveyor tick) and a distorted syncopated bass. In the
breakdown, a single distant horn plays humanity's motif, as the stations choose sides.

**Tempo / key**: 135 BPM, B minor, with a cold chromatic shift to G minor under the motif's
A♯ (= B♭, the third of G minor).

**Loop**: `LOOPSTART` 333200 (7.556 s), `LOOPLENGTH` 2195200 (49.778 s = 28 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | The broadcast | Static and beeps, low drone, Vorne's motif in brass with organ below; anvils start in bars 3–4 |
| L1–8 | A | Industrial four-on-the-floor, metal-layered snare, anvil pattern, distorted bass, pluck arp and distorted 8th line both playing Vorne's arpeggio |
| L9–16 | B | Vorne's motif in full brass (doubled low, with organ), answered by its own inversion; timpani |
| L17–20 | C | Conveyor breakdown: no kick, anvils and 16th metal ticks, long bass, a lone horn with humanity's motif in long echoes |
| L21–28 | D | Riser and build, then Vorne's motif as a tutti (brass in three octaves); snare roll into the loop |

**Chords**:
- A: `Bm | Bm | G | G | Em | Em | F♯ | F♯`
- B: `Bm | Gm | Bm | F♯ | Bm | Gm | Em | F♯`
- C: `Bm` pedal
- D: `G | G | F♯ | F♯ | Bm | Gm | Em | F♯`

**AI prompt**: *"Cold industrial techno, 135 BPM, B minor, metallic anvil percussion on a 16th
grid, steam hiss, distorted syncopated bass, arpeggio sequencer playing a minor-major-seventh
chord, stately cold brass and organ villain leitmotif, radio broadcast static intro, asteroid
mining station, 1990s tracker module industrial, Descent, instrumental, seamless loop."*

---

## jovian-theme-r03-a — "Eye of the Storm" (Act 6 level theme A, 65.6 s)

**Brief**: Jupiter's cloud cities and Callisto: vast, stormy and grand, the most bombastic act,
and the hunt for Vorne. The two motifs duel. Vorne's motif in low brass and choir (on G, then
on C) is answered each time by humanity's motif in *major* in the trumpets (on E♭, then on D).
In the final section both play at once, over a Gm/F♯ bass that makes Vorne's major 7th the
bass note. Thunder, wind and 32nd-note storm strings frame it.

**Tempo / key**: 140 BPM, G minor (D major as the dominant; a G–A♭ phrygian swing in the storm).

**Loop**: `LOOPSTART` 321300 (7.286 s), `LOOPLENGTH` 2419200 (54.857 s = 32 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Storm front | Thunder, wind, tremolo strings; Vorne's motif far off, a trumpet answers; timpani roll |
| L1–8 | A | The hunt: humanity's motif in horns over gallop strings, four-on-the-floor, taiko and big pads |
| L9–16 | B | Vorne's motif (low brass + choir) vs. humanity's motif in major (trumpets), two bars each |
| L17–20 | C: the storm | No kick; thunder, tremolo strings, the choir swinging between G and A♭ |
| L21–24 | C: build | Gallop and drums return, snare roll, riser |
| L25–32 | D | Both motifs at once: humanity's in trumpets and supersaw, Vorne's in the bass brass |

**Chords**:
- Intro: `Gm | Gm | E♭ | D`
- A: `Gm | E♭ | B♭ | F | Gm | E♭ | Cm | D`
- B: `Gm(maj7) | Gm(maj7) | E♭ | E♭ | Cm(maj7) | Cm(maj7) | D | D`
- C: `Gm | A♭ | Gm | A♭ | E♭ | Cm | D | D`
- D: `Gm | Gm/F♯ | B♭ | F | Gm | Gm/F♯ | Cm | D`

**AI prompt**: *"Grand stormy orchestral-electronic battle theme, 140 BPM, G minor, thunder
and wind, tremolo storm strings, galloping string ostinato, taiko and four-on-the-floor,
choir, a cold villain brass motif answered by a heroic trumpet fanfare in major, both themes
colliding at the climax, gas giant cloud cities, Bespin, Einhänder, 1990s video game
music, instrumental, seamless loop."*

---

## ascendancy-boss-r03-a — "Iron Sovereign" (Ascendancy boss theme, 62.4 s)

**Brief**: for the Iron Sovereign battle station and Vorne's flagship Ascendant. Martial,
cold, human-military menace, deliberately the opposite of the organic Vrell boss theme: no
choir, strict rhythm, military snare rudiments (accents on 2 and 4 with drags in between),
timpani, low-brass 8th-note ostinato, trumpets on Vorne's motif, and a machine section where
a distorted 16th riff runs the motif's arpeggio shape on every chord (mM7 on minor chords,
maj7 on D, dominant 7th on C♯).

**Tempo / key**: 147 BPM, F♯ minor (C♯ major as the dominant; G major as a cold Neapolitan
in the half-time section).

**Loop**: `LOOPSTART` 306000 (6.939 s), `LOOPLENGTH` 2304000 (52.245 s = 32 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | Parade | Snare rudiments growing, timpani, Vorne's motif in low brass on F♯ then C♯, organ below |
| L1–8 | A | March: kick 1, 3, 3&; rudiment snare; low-brass 8ths; trumpets on the motif (F♯, then B); short-short-long brass stab figures |
| L9–16 | B | Machine: four-on-the-floor, metal hits, 16th riff on the motif shape, off-beat trumpet stabs |
| L17–20 | C: menace | Half time; the motif at one note per bar in low brass and organ (F♯-A-C♯-E♯) |
| L21–24 | C: build | Snare roll, riser |
| L25–32 | D | Tutti: trumpets on the motif with a low-brass canon a bar behind, two octaves down; riff, march, snare roll into the loop |

**Chords**:
- A and D: `F♯m(maj7) | F♯m | D | D | Bm(maj7) | Bm | C♯ | C♯`
- B: `F♯m | D | Bm | C♯` × 2
- C: `F♯m | G | F♯m | G | D | Bm | C♯ | C♯`

**AI prompt**: *"Cold martial boss battle theme, 147 BPM, F sharp minor, military snare
drum rudiments, timpani, low brass staccato ostinato, trumpets playing a stately
minor-major-seventh villain motif, distorted 16th-note synth riff, metallic industrial hits,
brass canon, human military menace, no choir, 1990s synth-orchestral and industrial hybrid,
instrumental, seamless loop."*

---

## final-boss-r03-a — "Choir Heart" (final boss, 67.6 s)

**Brief**: the final battle against the Choir Heart, with everything at stake and the biggest
arrangement so far. The Choir motif (descending) and humanity's motif (rising) are written to
be played *at the same time*: in section C they run in counterpoint (E5 over E4, D5 over B4,
C5 over E5, B4 over F♯5 …). The Heart beats through the intro and the breakdown. In the
breakdown the Choir sings Vorne's motif, since he was absorbed at L48. The loop ends with
humanity's motif in **E major**: hope, before the fight goes round again.

**Tempo / key**: 150 BPM, E minor / phrygian (F as ♭II); E major climax.

**Loop**: `LOOPSTART` 299880 (6.800 s), `LOOPLENGTH` 2540160 (57.600 s = 36 bars).

| Bars | Section | What happens |
|---|---|---|
| 1–4 (intro) | The Heart | Heartbeat, organ, the Choir motif huge (choir + low brass); humanity's motif answers far off; snare roll |
| L1–8 | A | Vrell fight: the Choir motif twice, distorted 16th bass, pounding drums, orchestral stabs, the heartbeat underneath |
| L9–16 | B | Humanity's fanfare (trumpets + supersaw), gallop strings, rolling bass, four-on-the-floor; the choir holds chords |
| L17–24 | C | Both motifs at once, in counterpoint; 16th string figures, timpani on every other bar |
| L25–28 | D: the Heart | Only the heartbeat, a drone and the choir ("oo"): Vorne's motif, then a whispered Choir motif |
| L29–32 | D: build | Taiko 3-3-2, snare roll, riser |
| L33–36 | E | Humanity's motif in E major: trumpets, brass, supersaw, choir and organ; then B major back into the loop |

**Chords**:
- Intro: `Em | F | Em | B`
- A: `Em | Em | C | C | F | F | B | B`
- B: `Em | C | Am | B | Em | C | D | B`
- C: `Em | F | C | B | Em | F | Am | B`
- D: `Em` × 4 `| C | C | B | B`
- E: `E | E | C | B`

**AI prompt**: *"Epic final boss battle, 150 BPM, E minor resolving to E major, a huge alien
choir singing a descending motif in counterpoint against a heroic rising brass fanfare,
pulsing heartbeat, organ, distorted bass ostinato, pounding taiko and electronic drums,
galloping strings, orchestral stabs, breakdown with only a heartbeat and whispering choir,
triumphant major-key climax, 1990s synth-orchestral video game finale, instrumental,
seamless loop."*

---

# Round 08 — cues for Acts 1–2 and full-length versions

Generator: `python3 tools/concept/audio/music_r08.py [name ...]` (names: `briefing act2b
miniboss warning complete actcomplete failed gameover title hangar afterburner coalition
homefront choir`). Deterministic; loops use the round-02 format (intro + loop + 2-bar fade
tail, `LOOPSTART` / `LOOPLENGTH` in samples). Stings are one-shot files mastered to −14 LUFS
with a −2 dBFS ceiling (peak after encoding ≤ −1 dBFS).

**Verification and mix pass (2026-10-01)**: all 14 files measured (`analyze.py`, spectrograms,
octave-band balance against the chosen r01–r03 tracks, loudness per 4 bars, seam jump) and
re-rendered where needed. All are at −14.0 LUFS (−13.9 for "Game Over"), peak ≤ −1.6 dBFS
after encoding, no DC, clean starts and ends; every loop's seam jump is below the local
sample-to-sample change (ratio ≤ 0.65), and re-rendering gives bit-identical files. Fixes:
"Dry Dock" full length −4 dB below 60 Hz (its sub was ~3 dB above the other tracks);
"Situation Room" −4.5 dB around 240 Hz (150–400 Hz was its loudest band) and its radio static
made loop-periodic (it jumped slightly at the seam); "Coalition Rising" back at 132 BPM and
its lyrical interlude +3 dB (it sat 5–6 LU under the rest); "Homefront" turnaround now carries
humanity's motif (it dipped 4 LU). The EQ is a zero-phase corrective EQ on the whole mix
(`MIX_EQ` in the generator). Remaining dips of ~3–5 LU are the intended breakdowns
("Afterburner" pluck breakdown, title and boss breakdowns, the "Firestorm" lament).

## miniboss-sting-r08-a — "Contact Heavy" (#21 mini-boss stinger, 4.6 s)

**Brief**: a riser into three orchestral hits on a 3-3 rhythm (A minor brass stabs, timpani,
taiko, distorted low A), then a sustained A minor chord with a dissonant ♭9 in the choir
(B♭ over A) and a high string E. Key-neutral enough to cut in over any level track, which
resumes after it.

**Tempo / key**: 120 BPM feel, A minor (♭9 colour).

**AI prompt**: *"4-second orchestral stinger for a video game mini-boss entrance, short
riser into three heavy brass and timpani hits, then a dissonant sustained choir and string
chord, 1990s synth-orchestral, punchy, instrumental."*

## boss-warning-r08-a — "Red Alert" (#22 boss warning, 3 bars at 150 BPM = 4.8 s + tail)

**Brief**: two-tone klaxon on every beat, a 3-bar riser, an E drone in choir ("oo"), strings
and distorted bass, taiko getting denser (half notes → quarters → eighths), snare and timpani
roll in the last bar. The final beat is silent so the Vrell boss theme (150 BPM, E minor)
starts on the downbeat; only a short reverb tail remains.

**Tempo / key**: 150 BPM, E (matches "The Choir Descends").

**AI prompt**: *"5-second boss warning cue, alternating two-tone alarm klaxon, rising noise
sweep, low choir drone in E, accelerating taiko drums and snare roll, stops dead on the last
beat before the boss music, 1990s arcade shoot 'em up, instrumental."*

## mission-complete-r08-a — "Mission Complete" (#23 victory jingle, 6.4 s)

**Brief**: humanity's motif in **D major** (D-A-D-E-F♯) in horns, trumpets and low brass over
D | G – A, a snare build, then a ringing D major tutti (strings, choir, brass, timpani,
crash).

**Tempo / key**: 126 BPM (the title tempo), D major.

**AI prompt**: *"6-second victory jingle, heroic brass fanfare on a rising five-note motif in
D major, timpani and snare roll, final big orchestral D major chord with cymbal, 1990s
synth-orchestral game music, instrumental."*

## act-complete-r08-a — "Act Complete" (#24 fanfare, 16.3 s)

**Brief**: the longer fanfare after an act boss. Horns state humanity's motif over
D | B♭ | G | A (the ♭10 on B♭ keeps a hint of the minor), then trumpets take it up from B♭
over B♭ | C with choir, taiko and an arpeggio shimmer, a snare roll and riser, and the final
D major chord.

**Tempo / key**: 108 BPM, D major with ♭VI–♭VII.

**AI prompt**: *"15-second triumphant act-complete fanfare, horns then trumpets on a heroic
rising motif, choir, timpani, taiko, shimmering synth arpeggio, snare roll into a huge D major
chord, 1990s synth-orchestral video game, instrumental."*

## mission-failed-r08-a — "Mission Failed" (#25 downbeat sting, 5.8 s)

**Brief**: low brass descending D-C-B♭-A in two octaves over D minor, then a dark D minor
chord with timpani and a low "oo" choir; the whole mix sinks through a low-pass sweep (7 kHz
→ 350 Hz), like the ship's systems powering down.

**Tempo / key**: 80 BPM, D minor.

**AI prompt**: *"5-second mission failed sting, slow descending low brass line in D minor,
timpani hit, dark low choir chord, sound filtering down as if powering off, somber, 1990s
synth-orchestral game, instrumental."*

## game-over-r08-a — "Game Over" (#26 somber, 20.7 s)

**Brief**: humanity's motif slowly and in minor on a lone horn over Dm | B♭ | Gm | A | Dm,
soft strings and "oo" choir, a flute echoing the last phrase an octave up, ending on a low D
minor chord.

**Tempo / key**: 72 BPM, D minor.

**AI prompt**: *"20-second somber game over theme, lone French horn playing a slow heroic
motif in D minor, soft strings and distant wordless choir, flute echo, quiet timpani,
melancholic ending, 1990s synth-orchestral, instrumental."*

## briefing-theme-r08-a — "Situation Room" (#3 briefing, 96.6 s)

**Brief**: tense but quiet, under Okafor's and Varga's briefing text. Dark low-passed pads,
a clock-tick rim on every beat, telemetry blips (short plucks on chord tones through a
ping-pong delay), a sonar ping every four bars and faint radio static (loop-periodic, so it does not jump at the seam). Then pulse bass and a
soft kick, muted-horn fragments of humanity's motif (D-A-D, B♭-F-B♭), a D minor / E♭
(Neapolitan) tension section with tremolo strings and timpani, and a thin bar back to the
loop start.

**Tempo / key**: 100 BPM, D minor. **Loop**: `LOOPSTART` 238140 (5.400 s), `LOOPLENGTH`
3810240 (86.400 s = 36 bars). **Chords**: `Dm Dm B♭ B♭ Gm Gm A A` × 2 | `B♭ C Dm Dm B♭ C A A`
| `Dm E♭ Dm E♭ Gm Gm A A` | `Dm Dm B♭ A`.

**AI prompt**: *"Tense military briefing room music, 100 BPM, D minor, dark ambient synth
pads, ticking clock percussion, sparse telemetry beeps and sonar pings, quiet radio static,
soft pulsing bass, distant muted horn motif, 1990s sci-fi game, understated, instrumental,
seamless loop."*

## act2-b-theme-r08-a — "Firestorm" (#7 Act 2 B, 120.4 s)

**Brief**: the second Earth-surface theme, contrasting with "Homefront": an amen-style
breakbeat, reese bass, orchestral hits and a syncopated brass-and-string stab riff. A string
and choir melody in A; humanity's motif in brass (G-D-G-A-B♭) in B; a 16th pluck figure on
the motif's shape; a half-time lament; a storm section (toms, thunder, tremolo strings,
choir stabs); and an acid-arp tracker break.

**Tempo / key**: 140 BPM, G minor. **Loop**: `LOOPSTART` 321300 (7.286 s), `LOOPLENGTH`
4838400 (109.714 s = 64 bars). **Chords**: A `Gm Gm E♭ E♭ Cm Cm D D`; B `E♭ B♭ F Gm E♭ B♭ Cm D`;
C `Cm Cm E♭ E♭ F F D D`; storm `Cm E♭ B♭ F Cm E♭ D D`; break `Gm E♭ Cm D` × 2.

**AI prompt**: *"Urgent 1990s breakbeat and orchestral hybrid level theme, 140 BPM, G
minor, amen break drums, growling reese bass, orchestral brass stabs and hits, heroic brass
motif, choir, storm section with toms, thunder and tremolo strings, acid synth arpeggio
break, video game action, instrumental, seamless loop."*

## Full-length versions (round 08)

Each is the chosen sketch extended to production length with the same instruments, motifs
and mix. New sections are added for variation; the chosen sketch remains the reference for
style.

| File | Tempo / key | Loop (samples) | Sections (loop) |
|---|---|---|---|
| title-theme-full-r08-a — "Terran Vanguard" | 126 BPM, D minor | 357000 + 5376000 (64 bars) | A motif · B major lift · A2 motif in strings + brass counter-line · new G minor bridge (`Gm E♭ B♭ F Gm E♭ C A`) · breakdown/build · B2 + trumpets · A3 tutti · horn-call turnaround |
| hangar-theme-full-r08-a — "Dry Dock" | 90 BPM swing, D dorian | 264600 + 6585600 (56 bars) | groove · flute · vibes counter-line · new e-piano solo (`B♭maj9 Am7 Gm9 A7 B♭maj9 C9 Dm9 A7`) · breakdown · flute + vibes · half-time walking bass · breakdown |
| afterburner-full-r08-a — "Afterburner" | 140 BPM, A minor | 623700 + 6048000 (80 bars) | A · B · pluck breakdown · build · A2 · B2 + counter-arp · breakbeat/acid tracker break · new C melody (`C G Am F C G F E`) · A3 · turnaround |
| coalition-rising-full-r08-a — "Coalition Rising" | 132 BPM, D minor | 340772 + 5773091 (72 bars) | horn theme · string theme · battle (trumpet counter-melody, 3-3-2 stabs) · lyrical interlude (strings, flute, harp; +3 dB in the mix pass) · A2/B2 + choir, trumpets · humanity's motif · build · horn-call reprise |
| homefront-full-r08-a — "Homefront" | 147 BPM, C minor | 306000 + 5760000 (80 bars) | A + string/choir counter-melody · horns · lead variation · lament · siege (augmented motif in choir) · counterattack in E♭ major · lament in strings · horns · tutti · A3 · turnaround with humanity's motif in brass and siren |
| choir-descends-full-r08-a — "The Choir Descends" | 150 BPM, E minor | 299880 + 5080320 (72 bars) | A · riff · A2 · half-time/build · phase 2 tritone grind · chase with the Choir motif in canon · riff + brass · breakdown, motif augmented in the choir · tutti |

**132 BPM with a sample-exact loop**: a 16th at 132 BPM is 5011.36 samples, so the round-02
integer-sample grid cannot hold it (the first render moved to 135 BPM). `FracLoopSong` rounds
the *loop length* instead: 72 bars = 5773091 samples (131.99999 BPM), the step is derived from
it, and every event is placed on the nearest sample of that exact grid (at most ½ sample =
11 µs early or late, inaudible). Each loop pass is then the previous one shifted by exactly
`LOOPLENGTH` samples, so the seam stays sample-exact.

**AI prompt** (per track): use the round-01/02/03 prompt of the original sketch and add
*"full-length 2–3 minute arrangement with varied sections, bridge and breakdown, seamless
loop"*.

## Round 11 — stems (`tools/concept/audio/music_r11.py`)

### coalition-rising-base-r11-a
Base stem of "Coalition Rising" full length (track 5), for Level 01 until section 4. One render
of `music_r08.coalition_full()`: the timpani hits are tagged on their way into the percussion
group and collected in a separate group, and the base stem is mixed from the ostinato, string
pad, timpani, choir, harp, flute and riser groups with the full mix's own effect chains,
coefficients and interlude lift. Mastering is linked (each loudness pass measures the full mix
and gives both the same gain and ceiling; the encoder's peak correction is shared), so base and
full have the same length (142.3 s), the same `LOOPSTART` / `LOOPLENGTH` (340772 + 5773091) and
the same gain: a linear crossfade from base to full adds exactly the brass, lead strings and
drums. The full mix of that pass is byte-identical to `coalition-rising-full-r08-a.ogg`, so the
chosen file is the pair's other half (the script checks this; `--full` writes it as r11 if it
ever differs). The base stem sits 6.4 LU under the full mix; it is not re-mastered on its own,
since that would break the crossfade.

**AI prompt:** use the "Coalition Rising" prompt and add *"underscore stem only: string ostinato,
pads, timpani, harp and choir, no brass melody, no drums, same tempo and length as the full mix"*.

## themes-final-r13-a — title, hangar and briefing themes (production art)

Not a proposal of new music: the production files of the three chosen themes, for the UI batch
review (round 13). `python3 tools/art/themes.py` renders `title-theme-full-r08-a`,
`hangar-theme-full-r08-a` and `briefing-theme-r08-a` again with their generator
(`tools/concept/audio/music_r08.py`, `render_loop`: same composition, seeds, mix EQ and master at
−14 LUFS, OGG Vorbis q6, `LOOPSTART` / `LOOPLENGTH` in samples), remuxes the stream with a
`SOURCE` comment into `assets/music/{title,hangar,briefing}-theme.ogg` and checks them
(`--check`: loudness within 0.5 LU, true peak below −1 dBTP, nominal 192 kbit/s, seam jump no
larger than the nearby sample-to-sample change, and whether the decoded audio equals the chosen
file). The sheet `themes-final-r13-a.png` (`--review`) draws each file's waveform from
`assets/music/` with the intro (grey), loop (blue) and fade tail (dark) and the loop points
(amber), with the measured numbers.

