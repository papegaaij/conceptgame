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

# Concept round 03 — weapon sound families and the explosion ladder (third-party)

The weapon → family mapping and the size ladder are in the [README](../README.md). All files are
imported by `python3 tools/concept/audio/import_sfx.py` from Freesound HQ previews (production:
rebuild from the original files with the same settings). Beam files are seamless loops
(`loop=` in the importer: equal-power cross-fade of the loop end into its start, no fades).

### shot-vulcan-r03-b — "minigun.wav" by pgi
Source: <https://freesound.org/people/pgi/sounds/98331/> — CC0 1.0. Use: `vulcan` b, rotary chatter.
Why: Recorded minigun: continuous rotary chatter, so any 0.3 s slice works as one volley; gives the vulcan a second, rougher texture next to r02-e.
Edit: leading silence trimmed, cut to 0.30 s, 0.12 s fade-out; peak -10.0 dBFS.

### shot-laser-r03-b — "Laser shot.wav" by michael_grinnell
Source: <https://freesound.org/people/michael_grinnell/sounds/512469/> — CC0 1.0. Use: `laser` b.
Why: Clean, bright laser with a fast attack (4.6 kHz centroid); a second laser flavour for Rear Lance vs Lance Laser.
Edit: leading silence trimmed, cut to 0.30 s, 0.14 s fade-out; peak -10.0 dBFS.

### shot-beam-r03-a — "heavy beam weapon" by deleted Freesound user (deleted_user_1941307)
Source: <https://freesound.org/people/deleted_user_1941307/sounds/152322/> — CC0 1.0. Use: `beam` a, 1.4 s seamless loop (Ion Beam).
Why: Steady for its first 1.7 s (flat envelope), ideal for a seamless loop; 8.5k downloads.
Edit: seamless 1.40 s loop from 0.20 s with 0.10 s cross-fade; peak -12.0 dBFS.

### shot-beam-r03-b — "SFX Oscilating Laser Beam" by bolkmar
Source: <https://freesound.org/people/bolkmar/sounds/420364/> — CC-BY 4.0. Use: `beam` b, 2.62 s seamless loop (Orbital Lance).
Why: Oscillating beam with a clean 1.31 s period; the loop covers exactly two periods.
Edit: seamless 2.62 s loop from 0.40 s with 0.08 s cross-fade; peak -12.0 dBFS.

### shot-missile-r03-a — "Rocket Launch" by Jarusca
Source: <https://freesound.org/people/Jarusca/sounds/521377/> — CC0 1.0. Use: `missile` (Hornet Launcher).
Why: Short rocket launch with a strong ignition transient and ~0.6 s whoosh; 5.6k downloads.
Edit: leading silence trimmed, cut to 0.90 s, 0.40 s fade-out; peak -8.0 dBFS.

### shot-micromissile-r03-a — "Rocket Shots" by Audionautics
Source: <https://freesound.org/people/Audionautics/sounds/171655/> — CC-BY 3.0. Use: `micromissile`, first shot.
Why: Isolated small rocket shots in a longer take; the first one is cut out.
Edit: leading silence trimmed, cut to 0.45 s, 0.20 s fade-out; peak -10.0 dBFS.

### shot-mortar-r03-a — "Mortar Shots.flac" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/184382/> — CC0 1.0. Use: `mortar`, first shot.
Why: Recorded mortar shots; the first thump decays cleanly within 1 s.
Edit: leading silence trimmed, cut to 1.00 s, 0.50 s fade-out; peak -8.0 dBFS.

### shot-bomb-r03-a — "Falling Bomb.wav" by Daleonfire
Source: <https://freesound.org/people/Daleonfire/sounds/506313/> — CC0 1.0. Use: `bomb`, whistle cut to 1.2 s.
Why: Classic falling-bomb whistle (2.9 s); cut short for the frequent Bomb Rack, full length for the Airstrike special.
Edit: leading silence trimmed, cut to 1.20 s, 0.60 s fade-out; peak -10.0 dBFS.

### shot-torpedo-r03-a — "Torpedo launch underwater.wav" by jobro
Source: <https://freesound.org/people/jobro/sounds/35530/> — CC-BY 3.0. Use: `torpedo`.
Why: Real underwater launch with bubbles; the only good torpedo recording with a usable licence.
Edit: leading silence trimmed, cut to 1.00 s, 0.50 s fade-out; peak -8.0 dBFS.

### shot-mine-r03-a — "small metal object fall " by nicktermer
Source: <https://freesound.org/people/nicktermer/sounds/259553/> — CC0 1.0. Use: `mine` drop-and-bounce.
Why: Metallic object dropping and bouncing: two clunks read as 'dropped and landed'.
Edit: leading silence trimmed, cut to 0.90 s, 0.30 s fade-out; peak -8.0 dBFS.

### shot-tesla-r03-a — "Electric zap.wav" by michael_grinnell
Source: <https://freesound.org/people/michael_grinnell/sounds/512471/> — CC0 1.0. Use: `tesla`.
Why: Tight 0.22 s electric zap, 12.6k downloads; short enough to chain for Plasma Arc.
Edit: leading silence trimmed, cut to 0.22 s, 0.06 s fade-out; peak -10.0 dBFS.

### shot-resonator-r03-a — "sci-fi cannon" by humanoide9000
Source: <https://freesound.org/people/humanoide9000/sounds/422440/> — CC-BY 4.0. Use: `resonator` (Choir Resonator).
Why: Heavy sci-fi cannon with a sustained energy body; alien enough for captured Vrell tech.
Edit: leading silence trimmed, cut to 0.60 s, 0.30 s fade-out; peak -8.0 dBFS.

### explosion-tiny-r03-a — "Small Explosion" by Cyberios
Source: <https://freesound.org/people/Cyberios/sounds/145788/> — CC0 1.0. Use: `tiny` a.
Why: Very short, bass-heavy pop that decays within 0.45 s.
Edit: leading silence trimmed, cut to 0.45 s, 0.20 s fade-out; peak -4.0 dBFS.

### explosion-tiny-r03-b — "Small explosion" by dinodilopho
Source: <https://freesound.org/people/dinodilopho/sounds/328833/> — CC0 1.0. Use: `tiny` b.
Why: Single small blast with quick decay; thinner than a, so the two alternate well.
Edit: leading silence trimmed, cut to 0.60 s, 0.30 s fade-out; peak -4.0 dBFS.

### explosion-tiny-r03-c — "Firecracker Explosion" by unfa
Source: <https://freesound.org/people/unfa/sounds/609588/> — CC0 1.0. Use: `tiny` c, sharp crack.
Why: Firecracker crack: the sharpest transient of the set, decays in ~0.35 s.
Edit: leading silence trimmed, cut to 0.60 s, 0.30 s fade-out; peak -4.0 dBFS.

### explosion-small-r03-a — "Small Explosion" by lorenzgillner
Source: <https://freesound.org/people/lorenzgillner/sounds/271979/> — CC0 1.0. Use: `small` c.
Why: Small explosion with some crackle, a bit longer than the r02 small ones.
Edit: leading silence trimmed, cut to 1.10 s, 0.40 s fade-out; peak -1.5 dBFS.

### explosion-medium-r03-a — "Air Explosion.wav" by 1histori
Source: <https://freesound.org/people/1histori/sounds/401609/> — CC0 1.0. Use: `medium` b.
Why: Sharp airburst with a clean tail; lighter than r02-c.
Edit: leading silence trimmed, cut to 1.60 s, 0.60 s fade-out; peak -1.5 dBFS.

### explosion-medium-r03-b — "Explode001" by mitchelk
Source: <https://freesound.org/people/mitchelk/sounds/136765/> — CC0 1.0. Use: `medium` c.
Why: Dense, rumbling medium blast (10k downloads), cut to 2 s.
Edit: leading silence trimmed, cut to 2.00 s, 0.80 s fade-out; peak -1.5 dBFS.

### explosion-large-r03-a — "explosion_big_02.ogg" by derplayer
Source: <https://freesound.org/people/derplayer/sounds/587193/> — CC0 1.0. Use: `large` c.
Why: Sister file of r02-e: big blast with secondary crackle and debris.
Edit: leading silence trimmed, cut to 3.10 s, 1.00 s fade-out; peak -1.5 dBFS.

### explosion-huge-r03-a — "Explosion_01.wav" by tommccann
Source: <https://freesound.org/people/tommccann/sounds/235968/> — CC0 1.0. Use: `huge` a, 5 s.
Why: Most-downloaded CC0 explosion on Freesound (138k); long natural decay, cut to 5 s.
Edit: leading silence trimmed, offset 0.36 s, cut to 5.00 s, 2.00 s fade-out; peak -1.0 dBFS.

### explosion-huge-r03-b — "Big Boom" by unfa
Source: <https://freesound.org/people/unfa/sounds/189779/> — CC0 1.0. Use: `huge` b, 6 s, deep sub-bass.
Why: Deep cinematic boom dominated by sub-bass (94% of energy below 200 Hz), cut to 6 s.
Edit: leading silence trimmed, cut to 6.00 s, 2.50 s fade-out; peak -1.0 dBFS.

### explosion-underwater-r03-a — "underwater explosion.wav" by cubix
Source: <https://freesound.org/people/cubix/sounds/124544/> — CC0 1.0. Use: `underwater` a, recorded.
Why: Recorded underwater explosion: slow, muffled 'whump' with a long rumble.
Edit: leading silence trimmed, cut to 3.00 s, 1.20 s fade-out; peak -1.5 dBFS.

### explosion-underwater-r03-b — "Explosion" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/182429/> — CC0 1.0. Use: `underwater` b, derived.
Why: Shows the derivation recipe: any explosion through a 4-pole 500 Hz low-pass (import_sfx.py `lowpass=500`).
Edit: leading silence trimmed, cut to 1.70 s, 0.60 s fade-out, 4-pole 500 Hz low-pass; peak -1.5 dBFS.

### explosion-water-r03-a — "Water Explosion" by Sheyvan
Source: <https://freesound.org/people/Sheyvan/sounds/519008/> — CC0 1.0. Use: `water`, surface naval kills.
Why: Explosion with water spray, sharp attack; for kills on the water surface.
Edit: leading silence trimmed, cut to 1.90 s, 0.70 s fade-out; peak -1.5 dBFS.

# Concept round 04 — audible beams, Sonar Pulse, extra huge and under-water explosions

The round 03 beam loops were rejected ("I can't even hear B"): both had ~100% of their energy
below 200 Hz, so they were loud on a meter but inaudible on small speakers. Round 04 candidates
were measured for their 200 Hz–5 kHz share, and beams (plus start/stop) are normalised on that
band's RMS (−30 dB, `band_rms=` in `tools/concept/audio/import_sfx.py`), the level the chosen
shots reach. Loop seams use a linear cross-fade when the overlapping parts correlate (> 0.5),
equal-power otherwise (`loop=(…, "auto")`).

### shot-beam-r04-a — "laser beam" by peepholecircus
Source: <https://freesound.org/people/peepholecircus/sounds/169991/> — CC0 1.0. Use: `beam` loop a, 2.6 s, smooth ~850 Hz hum (Ion Beam).
Why: 100% of its energy in 200 Hz–5 kHz (median 845 Hz); a steady 2.6 s stretch between its slow swells gives a smooth loop. 6k downloads.
Edit: seamless 2.60 s loop from 5.60 s with 0.12 s cross-fade; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -3.0 dBFS).

### shot-beam-r04-b — "Weapons Beam Loop" by unfa
Source: <https://freesound.org/people/unfa/sounds/584191/> — CC0 1.0. Use: `beam` loop b, 1.9 s, gritty pulsing texture.
Why: Made as a beam loop by unfa; 72% mid-band with some grit on top, the most 'weapon-like' texture.
Edit: seamless 1.90 s loop from 0.00 s with 0.10 s cross-fade; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -3.0 dBFS).

### shot-beam-r04-c — "SonicDeathRay_1.2KHzNoCrackle.wav" by zimbot
Source: <https://freesound.org/people/zimbot/sounds/177100/> — CC-BY 4.0. Use: `beam` loop c, 2.0 s, piercing ray (Orbital Lance).
Why: Very steady ray tone (~1.65 kHz, 100% mid-band): the most clearly audible option, suited to the Orbital Lance.
Edit: seamless 2.00 s loop from 1.00 s with 0.10 s cross-fade; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -3.0 dBFS).

### shot-beam-start-r04-a — "Machine Charge.wav" by Glitchedtones
Source: <https://freesound.org/people/Glitchedtones/sounds/375925/> — CC0 1.0. Use: `beam` start, 0.9 s rising charge.
Why: Rising machine charge, 71% mid-band; the last 0.9 s of the rise leads into the loop.
Edit: leading silence trimmed, offset 1.10 s, cut to 0.90 s, 0.15 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -3.0 dBFS).

### shot-beam-stop-r04-a — "Power Down" by noirenex
Source: <https://freesound.org/people/noirenex/sounds/159399/> — CC0 1.0. Use: `beam` stop, 1.3 s power-down.
Why: Short power-down whine, 72% mid-band (the peepholecircus 'Power Down' was 100% sub-bass, so rejected).
Edit: leading silence trimmed, cut to 1.30 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -3.0 dBFS).

### special-sonar-r04-a — "Sonar Ping" by SamsterBirdies
Source: <https://freesound.org/people/SamsterBirdies/sounds/539957/> — CC0 1.0. Use: Sonar Pulse, clean single ping.
Why: Classic single submarine ping, all energy in the mid band; decays within ~2.5 s.
Edit: leading silence trimmed, cut to 3.00 s, 1.00 s fade-out; peak-normalised to -4.0 dBFS.

### special-sonar-r04-b — "Ping!" by unfa
Source: <https://freesound.org/people/unfa/sounds/215415/> — CC0 1.0. Use: Sonar Pulse, long ringing ping.
Why: unfa's ping (13k downloads): longer, ringing tail for a more dramatic reveal.
Edit: leading silence trimmed, cut to 3.50 s, 1.50 s fade-out; peak-normalised to -4.0 dBFS.

### explosion-huge-r04-a — "Explosion with debris - authentic. 4kg TNT" by sidohzen
Source: <https://freesound.org/people/sidohzen/sounds/165808/> — CC0 1.0. Use: `huge` b, 6 s real blast.
Why: Authentic recorded 4 kg TNT blast with debris; 65% of its energy in the mid band, so it complements the sub-heavy huge a instead of doubling it.
Edit: leading silence trimmed, offset 1.63 s, cut to 6.00 s, 2.50 s fade-out; peak-normalised to -1.0 dBFS.

### explosion-underwater-r04-a — "underwater explosion" by mokasza
Source: <https://freesound.org/people/mokasza/sounds/810765/> — CC-BY 4.0. Use: `underwater` b.
Why: Muffled under-water blast with rumble; second recorded under-water variant for Act 4.
Edit: leading silence trimmed, cut to 3.50 s, 1.20 s fade-out; peak-normalised to -1.5 dBFS.

## Round 08 — remaining Acts 1–2 sounds

Sourced with the same rules as rounds 02–04 (CC0/CC-BY, licence checked on each page, Freesound HQ previews for concept; rebuild from the originals for production). One-shots are levelled on the 200 Hz–5 kHz band (`band_rms`) so sub-heavy sources stay audible; candidates with more than ~85 % of their energy below 200 Hz were rejected (Movie Trailer Boom, PlasmaCannon, Heavy Blast, Big Sci-Fi Cinematic Explosion, ATP2 power-down). Ambience files are seamless loops. Regenerate: `python3 tools/concept/audio/import_sfx.py <name ...>`.

Pickups and UI blips stay **synthesized** (see Sourcing in the README), extending the family of the three chosen round-01 pickups: `python3 tools/concept/audio/sfx_r08.py`.

### hit-metal-r08-a — "HeavyBulletPing.mp3" by wilhellboy
Source: <https://freesound.org/people/wilhellboy/sounds/351371/> — CC0 1.0. Use: Hit: metal — bullet ping on Ascendancy hulls and machines.
Why: Short heavy bullet ping, 77% mid-band; reads as 'shot hits armour' at rapid fire.
Edit: leading silence trimmed, cut to 0.30 s, 0.12 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### hit-metal-r08-b — "Bullet Hit Metal" by coolguy244e
Source: <https://freesound.org/people/coolguy244e/sounds/267893/> — CC0 1.0. Use: Hit: metal, variant.
Why: Clean metallic bullet impact, 86% mid-band; alternates with a.
Edit: leading silence trimmed, cut to 0.45 s, 0.20 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### hit-organic-r08-a — "splat.ogg" by gprosser
Source: <https://freesound.org/people/gprosser/sounds/360942/> — CC0 1.0. Use: Hit: organic — wet hit on Vrell chitin.
Why: Short wet splat; gives the Vrell a fleshy hit distinct from metal.
Edit: leading silence trimmed, cut to 0.30 s, 0.12 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### hit-organic-r08-b — "cracking.wav" by smidoid
Source: <https://freesound.org/people/smidoid/sounds/49139/> — CC-BY 4.0. Use: Hit: organic, chitin crunch.
Why: Dry cracking crunch (64% above 5 kHz) for armoured Vrell plates.
Edit: leading silence trimmed, cut to 0.40 s, 0.15 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### hit-crumble-r08-a — "building_collapse02_close.wav" by onteca
Source: <https://freesound.org/people/onteca/sounds/197772/> — CC-BY 3.0. Use: Ground target destroyed — crumbling structure.
Why: Close building collapse with debris clatter; 2.4 s.
Edit: leading silence trimmed, cut to 2.40 s, 0.80 s fade-out; peak-normalised to -1.5 dBFS.

### hit-crumble-r08-b — "Bricks/Stones/Rocks/Gravel Falling" by iwanPlays
Source: <https://freesound.org/people/iwanPlays/sounds/567249/> — CC0 1.0 (a mix of three CC0 rock sounds). Use: Ground target destroyed, small (rubble burst).
Why: Dense, crunchy rubble burst that starts at full strength, 65% mid-band. Replaces "Rock Smash" by NeoSpica (512243): 77% below 200 Hz with one sharp spike, so even high-passed it stayed peak-limited ~8 dB below a on the 200 Hz–5 kHz band.
Edit: leading silence trimmed, cut to 1.60 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -25.0 dB (peak ceiling -1.5 dBFS). The band level matches crumble a (-25.3 dB).

### player-shield-hit-r08-a — "ELECTRIC_ZAP_001.wav" by JoelAudio
Source: <https://freesound.org/people/JoelAudio/sounds/136542/> — CC0 1.0. Use: Shield hit — electric fizz.
Why: 37k-download electric zap, mostly above 5 kHz: a crisp fizz that never masks the music's low end.
Edit: leading silence trimmed, cut to 0.50 s, 0.20 s fade-out; 4-pole low-pass at 7 kHz (83% of the energy was above 5 kHz, harsh for a sound that plays often; now 56%, the fizz character kept); 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-shield-hit-r08-b — "Sci-Fi Force Field Impact 15.wav" by StormwaveAudio
Source: <https://freesound.org/people/StormwaveAudio/sounds/330629/> — CC-BY 4.0. Use: Shield hit, force-field variant.
Why: Sci-fi force-field impact with a glassy tail.
Edit: leading silence trimmed, cut to 0.90 s, 0.35 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-shield-break-r08-a — "Synthesized_Pitch-Down_Zap" by joe_bou_khalil
Source: <https://freesound.org/people/joe_bou_khalil/sounds/861848/> — CC-BY 4.0. Use: Shield break — descending zap.
Why: Synthesized pitch-down zap, 100% mid-band; reads as 'shield collapsed'.
Edit: leading silence trimmed, cut to 1.40 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-shield-restore-r08-a — "Power Up Charge [Remix of LegoLunatic's Charged laser 151243]" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/172631/> — CC0 1.0. Use: Shield restored — rising charge.
Why: Short rising power-up charge (remix of LegoLunatic's charged laser).
Edit: leading silence trimmed, cut to 0.80 s, 0.25 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-shield-restore-r08-b — "Shield recharging" by Bychop
Source: <https://freesound.org/people/Bychop/sounds/136881/> — CC0 1.0. Use: Shield restored, longer recharge.
Why: Shield recharge hum rising over 1.6 s; for a full restore.
Edit: leading silence trimmed, cut to 1.60 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-armour-hit-r08-a — "Impact on metal" by JoMungus
Source: <https://freesound.org/people/JoMungus/sounds/726486/> — CC0 1.0. Use: Armour hit — metallic crunch.
Why: Heavy impact on metal; distinct from the shield fizz so the player hears armour damage.
Edit: leading silence trimmed, cut to 0.60 s, 0.30 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-low-armour-r08-a — "Bleeper 1" by magnuswaker
Source: <https://freesound.org/people/magnuswaker/sounds/522162/> — CC0 1.0. Use: Low armour warning — one beep.
Why: Single clean bleep (88% mid-band); the game repeats it slowly at 30 % and fast at 15 % armour.
Edit: leading silence trimmed, cut to 0.40 s, 0.08 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### player-destroyed-r08-a — "spaceship explosion9.WAV" by phantastonia
Source: <https://freesound.org/people/phantastonia/sounds/270616/> — CC-BY 4.0. Use: Ship destroyed.
Why: Spaceship explosion with a long rumble; the music sting follows it.
Edit: leading silence trimmed, cut to 3.50 s, 4-pole high-pass at 20 Hz (sub-sonic drift left a 0.008 DC offset after the fade-out), 1.50 s fade-out; peak-normalised to -1.0 dBFS.

### overdrive-start-r08-a — "Spacey 1up/Power up" by GameAudio
Source: <https://freesound.org/people/GameAudio/sounds/220173/> — CC0 1.0. Use: Overdrive start.
Why: Spacey 1-up / power-up sweep, 99% mid-band.
Edit: leading silence trimmed, cut to 0.60 s, 0.15 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### overdrive-end-r08-a — "Teleport Error" by Jerimee
Source: <https://freesound.org/people/Jerimee/sounds/521776/> — CC0 1.0. Use: Overdrive end.
Why: Falling 'teleport error' wind-down; the ATP2 power-down candidate was 89% sub-bass, so rejected.
Edit: leading silence trimmed, cut to 1.10 s, 0.40 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -2.0 dBFS).

### enemy-shot-small-r08-a — "Sci-fi gun shot x6" by humanoide9000
Source: <https://freesound.org/people/humanoide9000/sounds/330293/> — CC0 1.0. Use: Enemy shot, small.
Why: First shot of a six-shot sci-fi gun take; light and short.
Edit: leading silence trimmed, cut to 0.45 s, 0.20 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-shot-small-r08-b — "retro shot blaster" by JavierZumer
Source: <https://freesound.org/people/JavierZumer/sounds/257232/> — CC-BY 4.0. Use: Enemy shot, small (retro blaster).
Why: Retro blaster zap; alternates with a.
Edit: leading silence trimmed, cut to 0.32 s, 0.12 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-shot-heavy-r08-a — "ScifiHeavyBlasterShot.wav" by SuperPhat
Source: <https://freesound.org/people/SuperPhat/sounds/531861/> — CC0 1.0. Use: Enemy shot, heavy.
Why: Heavy sci-fi blaster shot, 47% mid-band (PlasmaCannon and Heavy Blast candidates were >85% sub-bass, rejected).
Edit: leading silence trimmed, cut to 0.75 s, 0.30 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-shot-heavy-r08-b — "Heavy blaster shot 05" by xkeril
Source: <https://freesound.org/people/xkeril/sounds/702000/> — CC0 1.0. Use: Enemy shot, heavy b.
Why: Bassy blaster made from a recorded hit, with a falling sweep; 50% mid-band, 40% sub, so it reads on small speakers and differs from a.
Edit: leading silence trimmed, cut to 0.90 s, 0.40 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-laser-warning-r08-a — "Laser Charging" by plasterbrain
Source: <https://freesound.org/people/plasterbrain/sounds/351807/> — CC0 1.0. Use: Enemy laser charge warning.
Why: First 1.2 s of a laser charge-up: the telegraph before a laser sweep or rail shot.
Edit: leading silence trimmed, cut to 1.20 s, 0.30 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-missile-r08-a — "Missile firing fl.mp3" by NHMWretched
Source: <https://freesound.org/people/NHMWretched/sounds/151858/> — CC0 1.0. Use: Enemy missile launch.
Why: Missile firing with a short exhaust tail (Jarusca's rocket is the player missile, so not reused).
Edit: leading silence trimmed, cut to 1.60 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-screech-r08-a — "Monster screech" by Khrinx
Source: <https://freesound.org/people/Khrinx/sounds/565024/> — CC0 1.0. Use: Vrell screech (spawn/attack cue) a.
Why: Monster screech, 90% mid-band.
Edit: leading silence trimmed, cut to 2.20 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-screech-r08-b — "inhuman screech.wav" by Wolfsinger
Source: <https://freesound.org/people/Wolfsinger/sounds/25713/> — CC-BY 4.0. Use: Vrell screech b.
Why: Inhuman screech, cut to 1.6 s.
Edit: leading silence trimmed, cut to 1.60 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-screech-r08-c — "alien4.wav" by AlienXXX
Source: <https://freesound.org/people/AlienXXX/sounds/78539/> — CC-BY 4.0. Use: Vrell screech c.
Why: Alien call, cut to 1.6 s.
Edit: leading silence trimmed, cut to 1.60 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-screech-r08-d — "alien screech.wav" by jvmyka@gmail.com
Source: <https://freesound.org/people/jvmyka@gmail.com/sounds/556535/> — CC0 1.0. Use: Vrell screech d.
Why: Tonal screech made from reversed, processed horse sounds (no human voice); first of two takes, 74% mid-band.
Edit: leading silence trimmed, cut to 2.00 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### enemy-spawn-r08-a — "DCA Alien Spawning Birth.aif" by darcyadam
Source: <https://freesound.org/people/darcyadam/sounds/651487/> — CC0 1.0. Use: Vrell spawn a (Hive Node and Brood Carrier spawns, portal warp-in).
Why: Wet creature swell built from sponge, glassware, pans and cats; first of three takes.
Edit: leading silence trimmed, cut to 1.60 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -6.0 dBFS).

### enemy-spawn-r08-b — "goreSplat.wav" by ThefitzyG
Source: <https://freesound.org/people/ThefitzyG/sounds/414296/> — CC0 1.0. Use: Vrell spawn b (Brood Pod bursting).
Why: "Something exploded into tiny fleshy pieces": a wet burst with splatter. (Artninja's "morphing burst" was rejected: its description says it uses Warner Bros and Zapsplat library sounds, which cannot be relicensed as CC-BY.)
Edit: leading silence trimmed, cut to 1.70 s, 0.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -6.0 dBFS).

### enemy-lock-r08-a — "lock on" by SamsterBirdies
Source: <https://freesound.org/people/SamsterBirdies/sounds/467881/> — CC0 1.0. Use: Turret lock-on beep.
Why: Rising lock-on beep sequence, first 1.06 s.
Edit: leading silence trimmed, cut to 1.06 s, 0.08 s fade-out; 200 Hz–5 kHz band RMS normalised to -30.0 dB (peak ceiling -8.0 dBFS).

### special-airstrike-jets-r08-a — "Jet Plane Flyby.flac" by qubodup
Source: <https://freesound.org/people/qubodup/sounds/189446/> — CC0 1.0. Use: Airstrike: jets flyby.
Why: Jet plane flyby, cut around its loudest pass (4.5 s with 0.6 s fade-in).
Edit: leading silence trimmed, offset 3.40 s, cut to 4.50 s, 0.60 s fade-in, 1.50 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -1.5 dBFS).

### special-airstrike-bombs-r08-a — "R11-55-Large Blasts.wav" by craigsmith
Source: <https://freesound.org/people/craigsmith/sounds/483284/> — CC0 1.0. Use: Airstrike: bomb carpet.
Why: A series of large blasts recorded in sequence: a natural carpet-bombing run.
Edit: leading silence trimmed, offset 0.95 s, cut to 5.00 s, 1.50 s fade-out; peak-normalised to -1.5 dBFS.

### special-smartbomb-r08-a — "Energy Blast" by Kinoton
Source: <https://freesound.org/people/Kinoton/sounds/369516/> — CC0 1.0. Use: Smart bomb: charge-up + white-out boom.
Why: Energy blast with build and long tail, 62% mid-band (the Movie Trailer Boom candidate was 100% sub-bass, rejected).
Edit: leading silence trimmed, cut to 4.20 s, 1.50 s fade-out; peak-normalised to -1.5 dBFS.

### special-flares-r08-a — "Guns & Explosions Album - Flare gun 5-2.wav" by OGsoundFX
Source: <https://freesound.org/people/OGsoundFX/sounds/423109/> — CC-BY 4.0. Use: Decoy flares.
Why: Flare-gun launch with fizz.
Edit: leading silence trimmed, cut to 1.40 s, 0.60 s fade-out; 200 Hz–5 kHz band RMS normalised to -24.0 dB (peak ceiling -1.5 dBFS).

### special-denied-r08-a — "acess denied buzz" by Jacco18
Source: <https://freesound.org/people/Jacco18/sounds/419023/> — CC0 1.0. Use: Special unavailable (denied buzz).
Why: Short access-denied buzz.
Edit: leading silence trimmed, cut to 0.31 s, 0.05 s fade-out; 200 Hz–5 kHz band RMS normalised to -27.0 dB (peak ceiling -4.0 dBFS).

### ui-radio-open-r08-a — "Power On.wav" by JustinBW
Source: <https://freesound.org/people/JustinBW/sounds/70107/> — CC-BY 4.0. Use: Radio squelch open.
Why: Walkie-talkie power-on click before a radio line.
Edit: leading silence trimmed, cut to 0.30 s, 0.05 s fade-out; 200 Hz–5 kHz band RMS normalised to -27.0 dB (peak ceiling -4.0 dBFS).

### ui-radio-close-r08-a — "Radio Sign Off / Squelch" by JovianSounds
Source: <https://freesound.org/people/JovianSounds/sounds/524205/> — CC0 1.0. Use: Radio squelch close.
Why: Radio sign-off squelch after a radio line.
Edit: leading silence trimmed, cut to 0.50 s, 0.20 s fade-out; 200 Hz–5 kHz band RMS normalised to -27.0 dB (peak ceiling -4.0 dBFS).

### ui-klaxon-r08-a — "Sci-Fi Alarm" by noirenex
Source: <https://freesound.org/people/noirenex/sounds/159453/> — CC0 1.0. Use: Warning klaxon (boss, rear attack) — loop.
Why: Sci-fi alarm cut to two cycles (2 × 2.38 s) as a seamless loop.
Edit: leading silence trimmed, seamless 4.76 s loop from 0.00 s with a 0.08 s cross-fade; 200 Hz–5 kHz band RMS normalised to -27.0 dB (peak ceiling -4.0 dBFS).

### ui-klaxon-r08-b — "RedAlert_Klaxon_STTOS_recreated.wav" by zimbot
Source: <https://freesound.org/people/zimbot/sounds/178032/> — CC-BY 4.0. Use: Warning klaxon, single blast.
Why: Red-alert klaxon recreation, one blast.
Edit: leading silence trimmed, cut to 0.95 s, 0.10 s fade-out; 200 Hz–5 kHz band RMS normalised to -27.0 dB (peak ceiling -4.0 dBFS).

### ambience-orbit-r08-a — "spacedrone3.wav" by Elektrocell
Source: <https://freesound.org/people/Elektrocell/sounds/20705/> — CC0 1.0. Use: Ambience: Earth orbit (space hum).
Why: Slow evolving space drone; 16 s loop.
Edit: leading silence trimmed, seamless 16.00 s loop from 20.00 s with a 1.50 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### ambience-luna-r08-a — "drone Space wind scifi.wav" by ztitchez
Source: <https://freesound.org/people/ztitchez/sounds/370754/> — CC-BY 4.0. Use: Ambience: Luna.
Why: Desolate sci-fi space-wind drone; 16 s loop.
Edit: leading silence trimmed, seamless 16.00 s loop from 10.00 s with a 1.50 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### ambience-city-r08-a — "201110 Distant sirens, urban, night, quiet, roof 11pm.flac" by TRP
Source: <https://freesound.org/people/TRP/sounds/568975/> — CC0 1.0. Use: Ambience: megacity.
Why: Night city from a rooftop with distant sirens; 20 s loop.
Edit: leading silence trimmed, seamless 20.00 s loop from 30.00 s with a 2.00 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### ambience-ocean-r08-a — "Ocean waves hitting bow of moving boat." by byjoshberry
Source: <https://freesound.org/people/byjoshberry/sounds/435668/> — CC-BY 4.0. Use: Ambience: ocean.
Why: Waves against a moving bow: open sea at speed; 16 s loop.
Edit: leading silence trimmed, seamless 16.00 s loop from 2.00 s with a 2.00 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### ambience-storm-r08-a — "Rain and Thunder 4" by FlatHill
Source: <https://freesound.org/people/FlatHill/sounds/237729/> — CC0 1.0. Use: Ambience: ocean storm (rain + thunder).
Why: Rain and thunder (132k downloads); 24 s loop with one thunder roll near the start.
Edit: leading silence trimmed, seamless 24.00 s loop from 1.00 s with a 2.00 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### ambience-arctic-r08-a — "Wind__Artic__Cold.wav" by cobratronik
Source: <https://freesound.org/people/cobratronik/sounds/117136/> — CC0 1.0. Use: Ambience: arctic wind.
Why: Cold arctic wind; 16 s loop.
Edit: leading silence trimmed, seamless 16.00 s loop from 30.00 s with a 2.00 s cross-fade; 200 Hz–5 kHz band RMS normalised to -36.0 dB (peak ceiling -10.0 dBFS).

### pickup-salvage-small-r08-a (synthesized)
Salvage small — short, high PWM blip with one sparkle (pickup-r01-c family, which itself is salvage medium). Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, short, high PWM blip with one sparkle (pickup-r01-c family, which itself is salvage medium), clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### pickup-salvage-large-r08-a (synthesized)
Salvage large — long PWM sweep two octaves up with an octave layer and a sparkle shower. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, long PWM sweep two octaves up with an octave layer and a sparkle shower, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### pickup-shield-cell-r08-a (synthesized)
Shield cell — cool rising triangle arpeggio (G major) with a chorus shimmer. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, cool rising triangle arpeggio (G major) with a chorus shimmer, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### pickup-armour-patch-r08-a (synthesized)
Armour patch — low square 'clunk' plus a metallic ding: a plate bolted on. Generator: `tools/concept/audio/sfx_r08.py`.
The ding rings out over 0.7 s (it was cut at 0.45 s).
AI prompt: "late-90s game UI/pickup sound, low square 'clunk' plus a metallic ding: a plate bolted on, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### pickup-special-charge-r08-a (synthesized)
Special charge — three rising square notes ending in a bell. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, three rising square notes ending in a bell, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-menu-move-r08-a (synthesized)
Menu move — soft 50 ms triangle blip. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, soft 50 ms triangle blip, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-menu-confirm-r08-a (synthesized)
Menu confirm — two rising square notes (E6–B6). Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, two rising square notes (E6–B6), clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-menu-back-r08-a (synthesized)
Menu back — two falling square notes (B5–E5). Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, two falling square notes (B5–E5), clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-shop-buy-r08-a (synthesized)
Shop buy — blip plus coin sparkle ('ka-ching'). Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, blip plus coin sparkle ('ka-ching'), clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-shop-sell-r08-a (synthesized)
Shop sell — three descending coin blips. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, three descending coin blips, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-shop-denied-r08-a (synthesized)
Shop denied (can't afford / won't fit) — low beating square buzz. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, low beating square buzz, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-typewriter-r08-a (synthesized)
Typewriter blip — 30 ms click-blip, played per character of briefing text. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, 30 ms click-blip, played per character of briefing text, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-tally-tick-r08-a (synthesized)
Debrief tally tick — 30 ms high sine tick. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, 30 ms high sine tick, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-tally-total-r08-a (synthesized)
Debrief total — C-major bell chord stinger. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI/pickup sound, C-major bell chord stinger, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-equip-r08-a (synthesized)
Hangar equip — square clunk (220 → 110 Hz), latch click and a quiet rising two-note blip (G5–D6, the ui-menu-confirm shape). Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI sound, hangar equip: a short low square clunk and a latch click, then a quiet rising two-note blip, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-upgrade-r08-a (synthesized)
Hangar upgrade — short PWM rise (pickup-r01-c family) landing on a G6 bell with a D7 overtone and a few sparkles. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI sound, hangar upgrade: a quick rising synth sweep that lands on a bright bell, a few sparkles, clean synthesized chiptune-meets-FM timbre, short, no reverb wash, no voice"

### ui-grade-stamp-r08-a (synthesized)
Debrief grade stamp — punchy thump with a 300 → 140 Hz square body (audible on small speakers), a band-passed paper slap and a low C5 bell. Generator: `tools/concept/audio/sfx_r08.py`.
AI prompt: "late-90s game UI sound, a grade being stamped onto a debrief report: a punchy rubber-stamp thump with a paper slap and a short low bell, clean synthesized timbre, short, no reverb wash, no voice"
