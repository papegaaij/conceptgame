"""Colour palettes as 6-step ramps (dark -> highlight), hex codes.

Round 01 proposed three palettes; **B "90s Neon CGI" was chosen** and is the reference palette
for everything from round 02 on (``B`` below). A and C are kept for reproducing round 01.
"""
from .raster import hexrgb

PALETTES = {
    "a": {
        "name": "COLD MILITARY STEEL",
        "mood": ["DESATURATED STEELS AND COOL BLUES, LIKE A 1997 MILITARY SIM RENDER.",
                 "SERIOUS AND GRITTY; COLOUR IS RESERVED FOR ACCENTS, GLOWS AND BULLETS."],
        "factions": [
            ("UTC HULL", "1c2229 353f4a 56626f 7d8a97 aab5bf dde4ea"),
            ("UTC ACCENTS", "1f3f7a 2f6bd8 7fb0ff b85a12 f08c24 ffd08a"),
            ("VRELL CHITIN", "140a18 2e1433 4d2152 6e3a6e 94608a c290ae"),
            ("VRELL GLOW", "0c3f33 1f8f6e 44f0b8 7a1640 ff3f99 ffc0dd"),
            ("ASCENDANCY", "08080a 16161b 26262e 3a3a44 555562 7a7a88"),
            ("ASCEND. GOLD", "4a3208 7d560f b8861e e0b440 f6d77a fff2c4"),
            ("UI PANELS", "0e1116 1b222b 2b3642 3f4d5c 6a7d8f a9bccb"),
            ("UI TEXT", "0f2a16 2f8a46 7cff9a ffd24a ff7a2a ff3b30"),
            ("ENEMY SHOTS", "200018 a0106a ff3fa8 ffb0dc ffffff ff8a00"),
            ("PLAYER SHOTS", "0a2a5a 2f7ff0 8fd0ff e8f8ff 9affd0 ffffff"),
        ],
        "settings": [
            ("EARTH ORBIT", "05070f 0f2344 1f4a7a 3f78a8 8fb4d0 e6eef6"),
            ("EARTH SURFACE", "0e1a12 22402a 3f6a3a 7a8a52 b0a476 d9d2b0"),
            ("MARS", "1a0b08 3d1a10 6e3420 a4583a cf8a5e f0c9a0"),
            ("EUROPA (ICE)", "020a14 06243a 0b4a66 1f7a94 5fb8c8 c8f4f8"),
            ("ASTEROID BELT", "0a0a0c 1f1d1c 3a3532 5e5650 8c8278 c4bab0"),
            ("JOVIAN", "140c08 3a2214 6e4428 a8703c d8a868 f4dcb0"),
            ("ALIEN SPACE", "06020c 1a0826 3a1050 6a2080 a050b0 f0a0f0"),
        ],
    },
    "b": {
        "name": "90S NEON CGI",
        "mood": ["SATURATED, GLOSSY, VIOLET-SHADOWED CHROME. LOUD 1998 CD-ROM COVER ENERGY.",
                 "EVERYTHING GLOWS; NEEDS CAREFUL BULLET CONTRAST RULES."],
        "factions": [
            ("UTC HULL", "121632 2a3068 4e5aa0 8a96d0 c8d0f4 ffffff"),
            ("UTC ACCENTS", "0050ff 00a8ff 7ff0ff ff2a6a ff7a2a ffe04a"),
            ("VRELL CHITIN", "1a0020 40004a 6e0a78 a020a8 d050d0 ff9aff"),
            ("VRELL GLOW", "003a20 00a060 00ff9a 600080 c000ff f0a0ff"),
            ("ASCENDANCY", "000000 100818 201430 34244a 4c3a68 6c5890"),
            ("ASCEND. GOLD", "402000 804000 c07000 ffa800 ffd84a ffffa0"),
            ("UI PANELS", "06061a 101438 1c2460 283890 4058c8 80a0ff"),
            ("UI TEXT", "00ffff 00ff66 ffff00 ff00aa ff4400 ffffff"),
            ("ENEMY SHOTS", "300030 c000c0 ff40ff ffc0ff ffffff ffff40"),
            ("PLAYER SHOTS", "002060 0060ff 00c0ff a0ffff ffffff 40ff80"),
        ],
        "settings": [
            ("EARTH ORBIT", "000010 001050 0030a0 2070e0 60b0ff c0f0ff"),
            ("EARTH SURFACE", "001008 003818 007030 40a040 a0d060 f0ffa0"),
            ("MARS", "200000 600800 a02000 e04800 ff8830 ffd080"),
            ("EUROPA (ICE)", "000818 002050 004890 0080c0 20d0e0 b0ffff"),
            ("ASTEROID BELT", "080410 201830 403850 686080 9890b0 d0d0e8"),
            ("JOVIAN", "200808 602010 a04818 e08020 ffb840 fff0a0"),
            ("ALIEN SPACE", "100010 300040 600080 a000c0 e040ff ffb0ff"),
        ],
    },
    "c": {
        "name": "WARM CINEMATIC",
        "mood": ["TEAL SHADOWS, AMBER LIGHT, WARM GREYS: A 90S SCI-FI FILM TRANSFERRED TO SPRITES.",
                 "MOODY AND COHESIVE; ORANGE/TEAL CONTRAST CARRIES READABILITY."],
        "factions": [
            ("UTC HULL", "1a2224 34403f 5a6560 8a918a bcbdb2 ece6d6"),
            ("UTC ACCENTS", "0f4c5c 1f8a99 7fd0cc 8a3a10 d8762a ffc27a"),
            ("VRELL CHITIN", "140c10 2e1a20 4e2c30 74443e 9c6a58 caa088"),
            ("VRELL GLOW", "083a3a 0f8a86 3ff0e0 6a1a08 ff5a1a ffd0a0"),
            ("ASCENDANCY", "0a0908 1a1714 2c2722 423a32 5e544a 837666"),
            ("ASCEND. GOLD", "3c2606 6e4810 a8721e d8a23c f2cc74 fff0c8"),
            ("UI PANELS", "121414 202424 323838 4a5250 6e7872 a0aaa2"),
            ("UI TEXT", "ffb347 ffd98a fff4dc 6fe0d0 ff6a3a ff3a2a"),
            ("ENEMY SHOTS", "2a0808 b8200a ff5a1a ffb080 fff0e0 ff2a6a"),
            ("PLAYER SHOTS", "06303a 0fa0b0 6ff0e8 d8fffa ffffff ffd84a"),
        ],
        "settings": [
            ("EARTH ORBIT", "04080c 0c1e2a 1c3c4c 3a6670 7aa8a4 e0e8dc"),
            ("EARTH SURFACE", "10140c 283220 4a5634 7a7a4a b0a06a e6d6a8"),
            ("MARS", "1c0c06 42200e 74381a b0602c e09450 ffd6a0"),
            ("EUROPA (ICE)", "040c10 0a2228 144448 266e6c 52a8a0 c0ece0"),
            ("ASTEROID BELT", "0c0a08 221e1a 3e3630 625648 8e7e6a c4b49c"),
            ("JOVIAN", "1a0e06 42260e 744418 b0702a e0a650 fff0c0"),
            ("ALIEN SPACE", "0a0610 221230 402050 6a3070 a45890 f0a8c0"),
        ],
    },
}


class Palette:
    """Convenience access to one palette's ramps as RGB tuples (0..255) or floats (0..1)."""

    def __init__(self, key):
        self.data = PALETTES[key]
        self._ramps = {name: [hexrgb(c) for c in codes.split()]
                       for group in ("factions", "settings") for name, codes in self.data[group]}

    def __getitem__(self, name):
        return self._ramps[name]

    def f(self, name, i):
        return tuple(c / 255 for c in self._ramps[name][i])

    def ship_colors(self):
        """Material overrides for the player ship models (see render.models.UTC)."""
        return {"hull": self.f("UTC HULL", 4), "hull_dark": self.f("UTC HULL", 1),
                "accent": self.f("UTC ACCENTS", 1), "accent2": self.f("UTC ACCENTS", 4),
                "glass": self.f("EARTH ORBIT", 1), "engine": self.f("PLAYER SHOTS", 2)}

    def vrell_colors(self):
        """Material overrides for the Vrell models (see render.models.VRELL)."""
        return {"chitin": self.f("VRELL CHITIN", 3), "chitin2": self.f("VRELL GLOW", 1),
                "glow": self.f("VRELL GLOW", 2), "glow2": self.f("VRELL GLOW", 4)}


B = Palette("b")
