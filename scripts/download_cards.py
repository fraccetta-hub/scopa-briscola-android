#!/usr/bin/env python3
"""
download_cards.py - Utility script for obtaining and naming Sicilian/Neapolitan card assets.

USAGE
-----
  python scripts/download_cards.py --source wikimedia   # Try to download from Wikimedia Commons
  python scripts/download_cards.py --source placeholder # Generate colored placeholder PNGs (default)

ABOUT THE ASSETS
----------------
This project uses 40-card Italian deck images matching the Sicilian/Neapolitan structure:
  Ranks : asso, 2, 3, 4, 5, 6, 7, fante, cavallo, re
  Suits : denari, coppe, spade, bastoni
  Format: {rank}_{suit}.png  → e.g. asso_denari.png, re_bastoni.png

Output directories:
  app/src/main/assets/cards/   – 40 card images
  app/src/main/assets/backs/   – card back (default_back.png)

CURRENT STATE
-------------
The bundled assets were generated as colored placeholder PNGs (64x96 pixels).
Each suit has a distinct background color; the rank label is encoded in the filename only.

  denari  → gold    (#D4A017)
  coppe   → red     (#C0392B)
  spade   → blue    (#2980B9)
  bastoni → green   (#27AE60)

REAL CARD IMAGES
----------------
Free-license Neapolitan card scans are available on Wikimedia Commons:
  https://commons.wikimedia.org/wiki/Category:Neapolitan_playing_cards

After downloading and renaming to match the convention above, replace the placeholder
PNGs in app/src/main/assets/cards/ and re-run the app.

Recommended sizes: 240x360 px (2:3 aspect ratio) for good quality on HDPI screens.
"""

import os
import struct
import zlib
import argparse

RANKS = ["asso", "2", "3", "4", "5", "6", "7", "fante", "cavallo", "re"]
SUITS = ["denari", "coppe", "spade", "bastoni"]

# Background RGB colors per suit
SUIT_COLORS = {
    "denari":  (212, 160,  23),
    "coppe":   (192,  57,  43),
    "spade":   ( 41, 128, 185),
    "bastoni": ( 39, 174,  96),
}

CARD_WIDTH  = 64
CARD_HEIGHT = 96
BACK_WIDTH  = 64
BACK_HEIGHT = 96
BACK_COLOR  = (60, 60, 180)   # dark blue back


def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    c = chunk_type + data
    return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)


def make_png(width: int, height: int, r: int, g: int, b: int) -> bytes:
    """Create a minimal valid RGB PNG filled with a solid color."""
    signature = b"\x89PNG\r\n\x1a\n"
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    ihdr = _png_chunk(b"IHDR", ihdr_data)

    raw_rows = []
    for _ in range(height):
        row = bytes([0])  # filter type None
        row += bytes([r, g, b] * width)
        raw_rows.append(row)
    compressed = zlib.compress(b"".join(raw_rows))
    idat = _png_chunk(b"IDAT", compressed)
    iend = _png_chunk(b"IEND", b"")
    return signature + ihdr + idat + iend


def generate_placeholders(cards_dir: str, backs_dir: str):
    os.makedirs(cards_dir, exist_ok=True)
    os.makedirs(backs_dir, exist_ok=True)

    for suit in SUITS:
        r, g, b = SUIT_COLORS[suit]
        for rank in RANKS:
            fname = f"{rank}_{suit}.png"
            path  = os.path.join(cards_dir, fname)
            with open(path, "wb") as f:
                f.write(make_png(CARD_WIDTH, CARD_HEIGHT, r, g, b))
            print(f"  created {fname}")

    back_path = os.path.join(backs_dir, "default_back.png")
    with open(back_path, "wb") as f:
        f.write(make_png(BACK_WIDTH, BACK_HEIGHT, *BACK_COLOR))
    print(f"  created default_back.png")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Obtain card assets for Scopa & Briscola")
    parser.add_argument("--source", choices=["placeholder", "wikimedia"],
                        default="placeholder",
                        help="'placeholder' generates colored PNGs; 'wikimedia' attempts a live download")
    parser.add_argument("--root", default=".",
                        help="Project root directory (default: current dir)")
    args = parser.parse_args()

    cards_dir = os.path.join(args.root, "app", "src", "main", "assets", "cards")
    backs_dir = os.path.join(args.root, "app", "src", "main", "assets", "backs")

    if args.source == "placeholder":
        print("Generating placeholder PNGs …")
        generate_placeholders(cards_dir, backs_dir)
        print("Done. Replace with real card images before release.")
    else:
        print("Wikimedia download not yet implemented in this script.")
        print("Visit: https://commons.wikimedia.org/wiki/Category:Neapolitan_playing_cards")
        print("Download each image, rename to {rank}_{suit}.png, place in app/src/main/assets/cards/")
