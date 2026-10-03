---
title: Concept round 20 — M4 briefing images
design: approved
implementation: n/a
art: chosen
depends-on: [../../ui/briefing]
updated: 2026-10-03
---

# Concept round 20 — M4 briefing images

## Summary

The briefing images of Levels 03 and 04, one per briefing page, rendered as **final** art by
[tools/art/briefing_images.py](../../../tools/art/README.md) into `assets/ui/briefing/` in the
style of the Act 1 intro and Levels 01–02 images approved in
[round 13](../round-13/README.md), with the production sprites of the levels' enemies, the
crawler and the Airstrike bomber. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 20`). The choice is *approve as final* or *redo* (say what to
change, per image if needed); the levels' `art` stays `chosen` until it is approved. The images
show in the game with `./gradlew :desktop:run --args="--level 3"` (or `--level 4`).

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Briefing images](../../ui/briefing/README.md) of [Level 03](../../campaign/act-1-first-contact/level-03-spore-drift/README.md) and [Level 04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | `level-03-spore-lanes` (Okafor: the high lanes over last week's battle site and its debris field, the *Kestrel* wreck, four spore carriers dropping spores toward Earth, Lancer's lane, "10 carriers"), `level-03-spore-echo` (Varga: the Spore Bomber scan, its spore mines rising into a wide spread, the long-range echo on a radar as a noisy violet silhouette of the Leviathan), `level-04-convoy-road` (Okafor: Tranquility Base with the heritage site, the brood pods around it, the five crawlers on the road across the rille's bridge to the mass-driver terminal, walkers in the craters, Hammer flight with the Airstrike bomber), `level-04-walker-scan` (Varga: the Scuttler walking right with its claws' arc in front and its glowing back behind, and a small "be patient" diagram: let it turn away, get behind it, fire) | `briefing-images-final-r20-a` | the Leviathan silhouette may give the surprise away (the sensor suite's "unknown huge contact" silhouette does the same); the crawlers are the HUD pip turned sideways, small; the convoy map is busy; "400 civilians" and "10 carriers" are the only numbers, both from the level's text and objective || approved — final |

## Decisions

- 2026-10-03: Opened with the briefing images of Levels 03 and 04.
- 2026-10-03: Closed (user decision, "both accepted"): the four briefing images of Levels 03 and 04 approved as final. With them the art of Levels 03 and 04 is final.
