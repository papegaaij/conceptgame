"""Models for concept round 08 (Earth scenes for Acts 1-2). Units = native px at 960x540; the
bow of every vessel points up the screen (+Y), the direction the convoy steams.

- ``freighter``: UTC convoy container ship (L11): dark hull, rows of muted containers, bridge
  and funnel at the stern.
- ``frigate``: CDF escort frigate: grey hull, bow gun, missile cells, radar mast, helipad.
- ``fusion_platform``: offshore fusion platform on four pontoon legs (L12 storm).
- ``rotunda``: the Geneva Concord assembly rotunda with a verdigris dome and colonnade (L14).
"""
import numpy as np

from .sdf import (Material, mirror_x, panel_lines, sd_box, sd_capsule, sd_cylinder_z, sd_plate,
                  sd_sphere, union)

# shared materials (indices into MATS)
HULL, DECK, WHITE, WINDOW, FUNNEL, GREY, DARK, RED_L, GREEN_L, PAD, GLOW, FLAME = range(12)
CONTAINERS = [(0.55, 0.24, 0.18), (0.22, 0.34, 0.44), (0.40, 0.44, 0.30), (0.66, 0.60, 0.48),
              (0.30, 0.28, 0.32), (0.50, 0.36, 0.22)]


def _mats(pad_c=(0.0, 7.0)):
    def windows(p, n):
        return np.where(np.abs(np.sin(p[:, 0] * 1.4)) > 0.45, 1.0, 0.0)

    def vls(p, n):
        return np.where(((p[:, 0] * 0.6) % 1 < 0.18) | ((p[:, 1] * 0.6) % 1 < 0.18), 0.55, 1.0)

    def pad(p, n):
        cx, cy, rr = pad_c if len(pad_c) == 3 else (0.0,) + tuple(pad_c)
        r = np.hypot(p[:, 0] - cx, p[:, 1] - cy)
        ring = np.abs(r - rr) < 0.9
        hbar = (np.abs(p[:, 0] - cx) < 4.5) & (np.abs(p[:, 1] - cy) < 0.9)
        vbar = (np.abs(np.abs(p[:, 0] - cx) - 4.0) < 0.9) & (np.abs(p[:, 1] - cy) < 5.0)
        return np.where(ring | hbar | vbar, 2.6, 1.0)
    mats = [
        Material((0.10, 0.12, 0.16), metal=0.3, shininess=40, spec=0.4),                  # HULL
        Material((0.36, 0.22, 0.17), metal=0.15, shininess=20, spec=0.25,
                 pattern=panel_lines(0.12, 0.05, 0.85)),                                     # DECK
        Material((0.86, 0.86, 0.84), metal=0.2, shininess=60, spec=0.6),                  # WHITE
        Material((0.12, 0.14, 0.18), emission=(0.9, 0.7, 0.35), emission_pattern=windows),  # WINDOW
        Material((0.62, 0.16, 0.12), metal=0.3, shininess=50, spec=0.5),                  # FUNNEL
        Material((0.50, 0.53, 0.57), metal=0.45, shininess=60, spec=0.6,
                 pattern=panel_lines(0.1, 0.05, 0.82)),                                      # GREY
        Material((0.16, 0.17, 0.20), metal=0.5, shininess=50, spec=0.5, pattern=vls),     # DARK
        Material((0.1, 0.05, 0.05), emission=(1.8, 0.25, 0.2)),                           # RED_L
        Material((0.05, 0.1, 0.05), emission=(0.3, 1.6, 0.4)),                            # GREEN_L
        Material((0.34, 0.36, 0.38), metal=0.3, shininess=30, spec=0.3, pattern=pad),     # PAD
        Material((0.2, 0.3, 0.4), emission=(0.9, 1.5, 2.0)),                              # GLOW
        Material((0.3, 0.1, 0.0), emission=(2.2, 1.0, 0.25)),                             # FLAME
    ]
    mats += [Material(c, metal=0.25, shininess=35, spec=0.35,
                      pattern=lambda p, n: np.where((p[:, 0] * 0.8) % 1 < 0.2, 0.8, 1.0))
             for c in CONTAINERS]
    return mats


def _hull_poly(length, beam, bow=0.2, stern=0.06):
    hl, hb = length / 2, beam / 2
    return [(0, hl), (hb * 0.55, hl - length * bow * 0.45), (hb, hl - length * bow),
            (hb, -hl + length * stern), (hb * 0.82, -hl), (-hb * 0.82, -hl),
            (-hb, -hl + length * stern), (-hb, hl - length * bow),
            (-hb * 0.55, hl - length * bow * 0.45)]


def freighter(length=180, beam=34, seed=3):
    rng = np.random.default_rng(seed)
    hl = length / 2
    hull = _hull_poly(length, beam)
    deck = _hull_poly(length - 6, beam - 5)
    stacks = []
    y = hl - 34
    while y > -hl + 44:
        for cx in (-beam * 0.3, 0.0, beam * 0.3):
            h = int(rng.integers(1, 4))
            stacks.append((cx, y, h, 12 + int(rng.integers(len(CONTAINERS)))))
        y -= 13.5

    def scene(p):
        items = [(sd_plate(p, hull, 0.0, 5.0, 1.5), HULL),
                 (sd_plate(p, deck, 4.5, 1.0, 0.5), DECK)]
        for cx, cy, h, m in stacks:
            items.append((sd_box(p, (cx, cy, 5 + 2.6 * h), (beam * 0.14, 6.0, 2.6 * h), 0.4), m))
        by = -hl + 22
        items += [(sd_box(p, (0, by, 12), (beam * 0.42, 8, 8), 1.0), WHITE),
                  (sd_box(p, (0, by + 7.6, 16), (beam * 0.4, 0.8, 2.0), 0.3), WINDOW),
                  (sd_box(p, (0, by + 2, 21), (beam * 0.5, 2.5, 0.8), 0.4), WHITE),
                  (sd_cylinder_z(p, (0, by - 9, 16), 4.5, 6), FUNNEL),
                  (sd_cylinder_z(p, (0, by - 9, 22.2), 4.6, 0.4), DARK),
                  (sd_capsule(p, (0, hl - 12, 5), (0, hl - 12, 16), 0.9), GREY),
                  (sd_sphere(p, (0, hl - 12, 16.5), 1.4), RED_L),
                  (sd_sphere(p, (beam * 0.5, by + 2, 21), 1.2), GREEN_L),
                  (sd_sphere(p, (-beam * 0.5, by + 2, 21), 1.2), RED_L)]
        return union(*items)
    return scene, _mats(), (beam + 14, length + 10)


def frigate(length=140, beam=22):
    hl = length / 2
    hull = _hull_poly(length, beam, bow=0.32, stern=0.03)
    deck = _hull_poly(length - 5, beam - 4, bow=0.32, stern=0.03)

    def scene(p):
        q = mirror_x(p)
        items = [(sd_plate(p, hull, 0.0, 4.5, 1.2), GREY),
                 (sd_plate(p, deck, 4.2, 0.8, 0.4), GREY),
                 (sd_cylinder_z(p, (0, hl - 30, 7), 5.0, 2.4), GREY),                 # bow gun
                 (sd_capsule(p, (0, hl - 28, 8), (0, hl - 14, 8), 1.1), DARK),
                 (sd_box(p, (0, hl - 44, 5.5), (beam * 0.3, 6, 1.2), 0.4), DARK),     # missile cells
                 (sd_box(p, (0, 2, 11), (beam * 0.38, 18, 6), 1.5), GREY),            # superstructure
                 (sd_box(p, (0, 14, 15), (beam * 0.32, 4, 2.4), 0.8), WINDOW),
                 (sd_sphere(p, (0, -2, 20), 4.0), WHITE),                             # radar dome
                 (sd_capsule(p, (0, -10, 14), (0, -10, 26), 0.9), DARK),
                 (sd_box(q, (beam * 0.25, -14, 13), (1.5, 3, 3), 0.5), DARK),
                 (sd_plate(p, [(-beam * 0.42, -hl + 4), (beam * 0.42, -hl + 4), (beam * 0.42, -hl + 30),
                               (-beam * 0.42, -hl + 30)], 5.0, 0.6, 0.3), PAD),
                 (sd_sphere(p, (0, hl - 3, 6), 1.0), RED_L)]
        return union(*items)
    return scene, _mats((0.0, -hl + 17, 7.0)), (beam + 12, length + 10)


def fusion_platform(size=112):
    hs = size / 2

    def scene(p):
        q = mirror_x(p)
        r = np.hypot(p[:, 0], p[:, 1] + 6)
        torus = np.hypot(r - 22, p[:, 2] - 12) - 6.0
        items = [
            (sd_box(p, (0, 0, 6), (hs, hs, 3), 2), GREY),                           # main deck
            (sd_box(p, (0, 0, 9.4), (hs - 6, hs - 6, 0.6), 0.4), DECK),
            (torus, WHITE),                                                         # reactor ring
            (sd_cylinder_z(p, (0, -6, 12), 14, 4), GLOW),                           # core glow
            (np.maximum(sd_cylinder_z(p, (0, -6, 16.5), 15, 0.8),
                        -sd_cylinder_z(p, (0, -6, 16.5), 10.5, 2.0)), DARK),
            (sd_box(p, (hs - 20, hs - 18, 14), (14, 12, 5), 1), WHITE),             # living block
            (sd_box(p, (hs - 20, hs - 9, 18), (13, 1, 1.5), 0.3), WINDOW),
            (sd_plate(p, [(-hs + 4, hs - 4), (-hs + 34, hs - 4), (-hs + 34, hs - 34),
                          (-hs + 4, hs - 34)], 10.5, 0.7, 0.3), PAD),               # helipad
            (sd_capsule(p, (hs - 10, -hs + 10, 10), (hs + 12, -hs - 12, 30), 1.6), DARK),   # flare boom
            (sd_sphere(p, (hs + 13, -hs - 13, 31), 3.2), FLAME),
            (sd_capsule(p, (-hs + 16, -hs + 16, 10), (-hs + 40, -hs + 30, 26), 1.4), FUNNEL),  # crane
            (sd_sphere(q, (hs, hs, 11), 1.4), RED_L),
            (sd_sphere(q, (hs, -hs, 11), 1.4), RED_L),
        ]
        return union(*items)
    return scene, _mats((-hs + 19, hs - 19, 9.0)), (size + 40, size + 40)


def rotunda(radius=46):
    def scene(p):
        r = np.hypot(p[:, 0], p[:, 1])
        ang = np.arctan2(p[:, 1], p[:, 0])
        cols = np.abs(np.sin(ang * 12)) < 0.35
        ring = np.maximum(np.abs(r - radius * 0.86) - 2.2, np.abs(p[:, 2] - 8) - 8)
        ring = np.where(cols, ring, ring + 2.0)
        items = [
            (sd_cylinder_z(p, (0, 0, 2), radius + 6, 2), PAD),                      # plaza step
            (sd_cylinder_z(p, (0, 0, 8), radius * 0.78, 8), WHITE),                 # drum
            (ring, WHITE),                                                          # colonnade
            (sd_cylinder_z(p, (0, 0, 16.5), radius * 0.92, 1.0), GREY),             # cornice
            (np.maximum(sd_sphere(p, (0, 0, 10), radius * 0.66), -(p[:, 2] - 16)), 12 + 2),  # dome
            (sd_cylinder_z(p, (0, 0, 10 + radius * 0.66), 4, 3), WHITE),            # lantern
        ]
        return union(*items)
    mats = _mats()
    mats[14] = Material((0.30, 0.52, 0.46), metal=0.55, shininess=70, spec=0.7,
                        pattern=lambda p, n: np.where(np.abs(np.sin(np.arctan2(p[:, 1], p[:, 0]) * 16))
                                                      < 0.12, 0.7, 1.0))    # verdigris dome
    return scene, mats, (2 * radius + 20, 2 * radius + 20)
