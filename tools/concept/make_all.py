#!/usr/bin/env python3
"""Regenerate the visual concept mockups of one round.

Run: python3 tools/concept/make_all.py            # round 02 (current), about 3 minutes
     python3 tools/concept/make_all.py --round 01  # round 01 (640x360), about 2 minutes

Rejected files are rewritten in place in concept/rejected/ (see render.config.out_path).
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--round", default="02", choices=["01", "02"])
    rnd = ap.parse_args().round
    sys.argv = sys.argv[:1]
    if rnd == "01":
        import hud, logos, palettes, parallax, player_ship  # noqa: E401
        modules = (player_ship, palettes, parallax, hud, logos)
    else:
        import hud_r02, parallax_r02, ships_r02  # noqa: E401
        modules = (ships_r02, parallax_r02, hud_r02)
    for module in modules:
        module.main()   # parallax and hud modules share the cached scene renders
