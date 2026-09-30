# Sound effect briefs and prompts — concept round 01

All round-01 SFX are code-generated mockups, reproducible with:

```
python3 tools/concept/audio/sfx.py            # writes all nine files into this directory
python3 tools/concept/audio/analyze.py design/audio/sfx/concept/*.ogg   # objective checks
```

Generator: `tools/concept/audio/sfx.py` (synth library `tools/concept/audio/synth.py`).
Format: OGG Vorbis q6, 44.1 kHz stereo, fixed seeds (byte-identical on re-run).

**Level rules** (apply to every future SFX, not just these):
player shots peak at −10 dBFS (they repeat several times per second and must never fatigue),
pickups peak at −4 dBFS (sustained ones −7), explosions peak at −1.5 dBFS. Every file starts
and ends at silence (0.5 ms / 5 ms fades), no DC offset, sample peak ≤ −1 dBFS after encoding.

---

## player-shot

The sound heard most in the game, several times per second for minutes on end. It must be
short (< 0.25 s), punchy at the start and gone immediately, with no harsh 2–4 kHz content that
tires the ear. It shouldn't mask enemy shots or the music's lead.

### player-shot-r01-a — classic laser "pew" (0.14 s)
Brief: square wave sweeping down 1900 → 380 Hz with the pulse width narrowing, through a
closing low-pass. Pure late-80s/90s arcade laser.
AI prompt (ElevenLabs SFX style): *"Retro arcade spaceship laser shot, short descending
square-wave 'pew', 0.15 seconds, clean, no reverb, 1990s DOS shoot'em up, dry, punchy start,
fast decay."*

### player-shot-r01-b — pulse cannon (0.12 s)
Brief: sine thump 240 → 70 Hz plus a band-passed noise snap and a short triangle tick. It
sounds more like a projectile cannon than a laser. Heavy, but the least tiring of the three.
AI prompt: *"Short sci-fi pulse cannon shot, compact low thump with a crisp mechanical snap,
0.12 seconds, dry, suitable for rapid fire, 1990s top-down shooter."*

### player-shot-r01-c — plasma bolt "zwip" (0.23 s)
Brief: two saws a fifth apart (1 : 1.5) that rise quickly and then droop, with fast vibrato, a
resonant filter sweep and a tiny ping-pong echo. The most sci-fi and "energy weapon"-like;
also the most distinctive, which suits a signature weapon better than the default gun.
AI prompt: *"Sci-fi plasma blaster shot, resonant filter 'zwip', rising then falling pitch,
slight stereo echo, 0.2 seconds, energetic but soft attack, 90s pre-rendered space shooter."*

---

## explosion

Enemy destruction. Size variants (small / large / boss) will be derived later from the chosen
style. Must feel rewarding and cut through the music without masking the next shot.

### explosion-r01-a — small, crunchy 8-bit pop (0.62 s)
Brief: noise with a low-pass sweeping down 7 kHz → 250 Hz, a short sine thump, saturated,
then bit-crushed to 6 bits at ¼ sample rate. The crush makes the end gate abruptly, like
tracker-era samples. Best for small drones and swarms, where many play at once.
AI prompt: *"Small retro explosion, crunchy bit-crushed 8-bit noise burst, 0.6 seconds, lo-fi,
Amiga / DOS game sample, punchy, no long tail."*

### explosion-r01-b — big boomy blast with debris (1.45 s)
Brief: a sub sine dropping 95 → 28 Hz, a brown-noise body behind a closing low-pass, a
high-passed crack at the start, 38 random filtered debris crackles thinning out over time, a
Haas stereo spread and a short dark room reverb. The "cinematic" option, for large ships and
ground structures.
AI prompt: *"Large spaceship explosion, deep sub-bass boom, rumbling body, crackling metal
debris falling away, 1.5 seconds, wide stereo, punchy transient, 90s sci-fi game."*

### explosion-r01-c — sci-fi plasma detonation (1.25 s)
Brief: noise through a resonant (Q 6) filter that opens and then closes ("whoom"), ring-modulated
falling sine tones, a tremolo saw shimmer, a sub thump, a slow left-right swirl and an echo.
Alien and energetic; a candidate for Vrell biomechanical enemies.
AI prompt: *"Sci-fi plasma explosion, resonant whooshing energy burst, ring-modulated falling
tones, swirling stereo, shimmering tail, 1.2 seconds, alien technology, 1990s space shooter."*

---

## pickup

Collecting credits and power-ups. Bright and positive, clearly different from anything
hostile, and short enough to chain when several pickups are collected in a row.

### pickup-r01-a — square arpeggio power-up (0.57 s)
Brief: a C major arpeggio C6-E6-G6-C7 in 25 % pulse waves, 55 ms per note, the last note held,
with a subtle echo. The iconic arcade "power-up".
AI prompt: *"Classic arcade power-up pickup sound, fast rising major arpeggio, square wave
chiptune, bright and cheerful, 0.5 seconds, light echo."*

### pickup-r01-b — bell chime pair (0.75 s)
Brief: two inharmonic bell tones (partials 1, 2.76, 5.4, 8.93) a fifth apart (E6 then B6),
70 ms apart, with a stereo echo. Cleaner and more "premium"; could suit rare pickups (weapon
upgrades) rather than common credits.
AI prompt: *"Bright crystal bell chime pickup, two quick ascending tones, shimmering stereo
echo, 0.7 seconds, clean digital FM bell, sci-fi UI."*

### pickup-r01-c — rising PWM sweep with sparkles (0.5 s)
Brief: a pulse wave sweeping two octaves up (330 → 1320 Hz) with pulse-width modulation,
vibrato and an opening filter, plus nine random sine sparkles. The "credits collected" candidate.
AI prompt: *"Sci-fi credits pickup, rising synth sweep with pulse-width modulation, sparkling
glitter on top, 0.5 seconds, bright, rewarding, 1990s arcade shooter."*

# Concept round 02 — recorded sound effects (third-party)

The round 01 synthesized shots and explosions were rejected. Round 02 uses recorded sounds from
[Freesound](https://freesound.org), CC0 or CC-BY only (see [CREDITS.md](../../../../CREDITS.md)).
All files are imported by `python3 tools/concept/audio/import_sfx.py`, which downloads the public
HQ previews (~192 kbps OGG) into a cache outside the repository, trims the leading silence, cuts,
fades, peak-normalises (shots −10 dBFS, explosions −1.5 dBFS) and encodes to OGG Vorbis.
**For the production asset, download the original file from the source page (Freesound login)
and rebuild with the same settings.** Selection was based on download counts, descriptions,
licence and measured envelope/spectrum (the files cannot be auditioned by the generator).

### player-shot-r02-a — "Projectile Shoot" by unfa
Source: <https://freesound.org/people/unfa/sounds/193427/> — CC0 1.0. Suggested use: Pulse Cannon (starter front gun).
Why: Sharp broadband transient with a short low thump; very fast attack and decay, so it stays clear under rapid fire. Well used (4k+ downloads).
Edit: 0.25 s, 0.12 s fade-out.

### player-shot-r02-b — "Sci Fi Gun Shot" by Bird_man
Source: <https://freesound.org/people/Bird_man/sounds/317136/> — CC0 1.0. Suggested use: heavy front gun / Hammer Mortar.
Why: Punchy sci-fi gun with a lot of low-end weight (~70% of energy below 200 Hz); reads as a heavy weapon.
Edit: 0.28 s, 0.14 s fade-out.

### player-shot-r02-c — "laser3" by nsstudios
Source: <https://freesound.org/people/nsstudios/sounds/344276/> — CC-BY 4.0. Suggested use: Lance Laser / light laser weapons.
Why: Clean descending laser sweep, naturally 0.3 s long; the most "energy weapon" of the set without sounding 8-bit.
Edit: 0.27 s, 0.10 s fade-out.

### player-shot-r02-d — "Machine Gun 001 - single shot" by pgi
Source: <https://freesound.org/people/pgi/sounds/212601/> — CC0 1.0. Suggested use: Autocannon Pod / ballistic guns.
Why: Real recorded single machine-gun shot: realistic crack and body; 10k+ downloads.
Edit: 0.28 s, 0.16 s fade-out.

### player-shot-r02-e — "Autocannon Three Shot Burst" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/854186/> — CC0 1.0. Suggested use: Scatter Vulcan / heavy ballistic front gun.
Why: Recorded autocannon; the first shot of the three-shot burst is cut out. Deep, mechanical, weighty.
Edit: first shot of the burst, 0.26 s, 0.12 s fade-out.

### explosion-r02-a — "small explosion" by bevibeldesign
Source: <https://freesound.org/people/bevibeldesign/sounds/315826/> — CC0 1.0. Suggested use: small enemy destroyed (fighters, drones).
Why: Short, natural decay within 0.4 s, not bass-heavy; fits frequent small kills without masking the mix.
Edit: 0.70 s, 0.30 s fade-out.

### explosion-r02-b — "Explosion 1" by magnuswaker
Source: <https://freesound.org/people/magnuswaker/sounds/523089/> — CC0 1.0. Suggested use: small-to-medium enemy (heavy fighters, turrets).
Why: Dense, bass-heavy (85% below 200 Hz) short blast; a heavier alternative for small kills.
Edit: 0.90 s, 0.40 s fade-out.

### explosion-r02-c — "Explosion" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/182429/> — CC0 1.0. Suggested use: medium enemy (gunships, ground vehicles, buildings).
Why: Well-known game explosion (14k downloads): deep boom with crackle, clean decay by 1.6 s.
Edit: 1.70 s, 0.50 s fade-out.

### explosion-r02-d — "Nearby explosion with debris" by juskiddink
Source: <https://freesound.org/people/juskiddink/sounds/108641/> — CC-BY 4.0. Suggested use: large enemy / mid-boss / building collapse.
Why: Real recorded nearby explosion with falling debris: the most realistic of the set.
Edit: 2.80 s, 0.90 s fade-out.

### explosion-r02-e — "explosion_big_01" by derplayer
Source: <https://freesound.org/people/derplayer/sounds/587194/> — CC0 1.0. Suggested use: boss destroyed / capital ship.
Why: Big, dense blast with rolling secondary crackle and debris over ~2.8 s.
Edit: 2.86 s, 0.80 s fade-out.

