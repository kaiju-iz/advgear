#!/usr/bin/env python3
"""Generates the AdvancedGear resource pack (placeholder pixel art) into ../resource-pack.
Usage: pip install pillow && python tools/generate_art.py
Add a tier: add a palette to PALETTES and an entry to ITEMS_BY_TIER."""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resource-pack")
NS = "advancedgear"

# primary, dark, light, accent(glow), handle
PALETTES = {
    "infernal":  dict(p=(168, 38, 24), d=(86, 16, 12),  l=(236, 96, 40),  a=(255, 190, 50),  h=(60, 40, 34)),
    "astral":    dict(p=(88, 60, 170), d=(40, 26, 92),  l=(160, 130, 245), a=(120, 240, 255), h=(52, 44, 70)),
    "celestial": dict(p=(60, 140, 210), d=(24, 66, 120), l=(150, 215, 255), a=(255, 255, 200), h=(70, 76, 96)),
    "primordial":dict(p=(86, 128, 70),  d=(40, 66, 36),  l=(150, 200, 110), a=(240, 210, 90),  h=(70, 52, 36)),
    "eternal":   dict(p=(226, 190, 70), d=(120, 86, 24), l=(255, 240, 160), a=(255, 255, 255), h=(70, 50, 40)),
}
OUT = (18, 12, 14, 255)

def blank(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
def put(im, pts, c):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def line(x0, y0, x1, y1):
    pts, n = [], max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        t = i / n if n else 0
        pts.append((round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t)))
    return pts

def outline(im):
    src = im.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    nx, ny = x+dx, y+dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0:
                        im.putpixel((x, y), OUT); break
    return im

def handle(im, P, a, b):
    put(im, line(*a, *b), P["h"])

def blade(im, P, a, b, thick=1):
    pts = line(*a, *b)
    put(im, pts, P["p"])
    put(im, [(x, y - 1) for x, y in pts], P["l"])
    put(im, [(x + 1, y) for x, y in pts], P["d"]) if thick > 1 else None
    if thick > 1: put(im, [(x, y + 1) for x, y in pts], P["p"])

def sword(P, greatsword=False, dagger=False):
    im = blank()
    if dagger:
        blade(im, P, (7, 8), (12, 3)); handle(im, P, (6, 9), (4, 11)); put(im, [(5, 8), (6, 7), (8, 9), (7, 10)], P["a"])
        put(im, [(3, 12), (3, 11)], P["d"]); put(im, [(12, 3)], P["a"])
    elif greatsword:
        blade(im, P, (5, 10), (14, 1), 2); handle(im, P, (4, 11), (1, 14))
        put(im, [(3, 9), (4, 8), (6, 12), (7, 13), (5, 11), (4, 10)], P["d"]); put(im, [(3, 10), (6, 11)], P["a"])
        put(im, [(13, 2), (12, 3), (10, 5)], P["a"])
    else:
        blade(im, P, (5, 10), (13, 2)); handle(im, P, (4, 11), (2, 13))
        put(im, [(3, 9), (4, 8), (6, 12), (5, 11)], P["d"]); put(im, [(13, 1)], P["l"])
    return outline(im)

def pickaxe(P):
    im = blank(); handle(im, P, (2, 14), (10, 6))
    put(im, [(4,3),(5,2),(6,2),(7,2),(8,2),(9,3),(10,4),(11,5),(12,6),(12,7)], P["p"])
    put(im, [(5,3),(6,3),(7,3),(8,3),(9,4),(10,5),(11,6)], P["l"]); put(im, [(4,4),(12,8),(10,6)], P["d"])
    put(im, [(7, 2)], P["a"]); return outline(im)

def axe(P):
    im = blank(); handle(im, P, (2, 14), (10, 6))
    put(im, [(8,2),(9,2),(10,2),(7,3),(8,3),(9,3),(10,3),(11,3),(7,4),(8,4),(9,4),(10,4),(11,4),(9,5),(10,5),(11,5),(10,6),(11,6)], P["p"])
    put(im, [(8,2),(9,2),(7,3),(8,3)], P["l"]); put(im, [(11,4),(11,5),(11,6),(10,6)], P["d"]); put(im, [(9, 4)], P["a"]); return outline(im)

def shovel(P):
    im = blank(); handle(im, P, (2, 14), (10, 6))
    put(im, [(11,1),(12,1),(10,2),(11,2),(12,2),(13,2),(10,3),(11,3),(12,3),(13,3),(11,4),(12,4),(11,5)], P["p"])
    put(im, [(11,1),(10,2),(11,2),(10,3)], P["l"]); put(im, [(13,3),(12,4),(12,3)], P["d"]); put(im, [(12, 2)], P["a"]); return outline(im)

def hoe(P):
    im = blank(); handle(im, P, (2, 14), (11, 5))
    put(im, [(8,2),(9,2),(10,2),(11,2),(12,2),(13,3),(8,3),(9,3)], P["p"])
    put(im, [(8,2),(9,2),(10,2)], P["l"]); put(im, [(13,3),(9,3),(8,3)], P["d"]); put(im, [(11, 2)], P["a"]); return outline(im)

def warhammer(P):
    im = blank(); handle(im, P, (2, 14), (10, 6)); handle(im, P, (3, 14), (11, 6))
    for x in range(7, 14):
        for y in range(1, 7):
            put(im, [(x, y)], P["p"])
    put(im, [(x, 1) for x in range(7, 14)] + [(7, y) for y in range(1, 7)], P["l"])
    put(im, [(x, 6) for x in range(7, 14)] + [(13, y) for y in range(1, 7)], P["d"])
    put(im, [(9,3),(10,3),(11,3),(10,4)], P["a"]); put(im, [(14, 3), (6, 3)], P["d"]); return outline(im)

def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): put(im, [(x, y)], c)

def shade(im, x0, y0, x1, y1, P):
    for y in range(y0, y1 + 1): put(im, [(x0, y)], P["l"]); put(im, [(x1, y)], P["d"])
    for x in range(x0, x1 + 1): put(im, [(x, y0)], P["l"]); put(im, [(x, y1)], P["d"])

def helmet(P):
    im = blank(); rect(im, 3, 3, 12, 9, P["p"]); shade(im, 3, 3, 12, 9, P)
    rect(im, 3, 10, 5, 12, P["p"]); rect(im, 10, 10, 12, 12, P["p"]); rect(im, 5, 7, 10, 8, P["d"])
    put(im, [(6, 7), (9, 7)], P["a"]); put(im, [(7, 3), (8, 3)], P["a"]); return outline(im)

def chestplate(P):
    im = blank(); rect(im, 4, 3, 11, 13, P["p"]); shade(im, 4, 3, 11, 13, P)
    rect(im, 1, 3, 4, 8, P["p"]); rect(im, 11, 3, 14, 8, P["p"]); shade(im, 1, 3, 4, 8, P); shade(im, 11, 3, 14, 8, P)
    rect(im, 6, 3, 9, 4, (0, 0, 0, 0)); rect(im, 7, 7, 8, 9, P["a"]); put(im, [(7, 6), (8, 10)], P["l"]); return outline(im)

def leggings(P):
    im = blank(); rect(im, 4, 2, 11, 5, P["p"]); rect(im, 4, 6, 7, 13, P["p"]); rect(im, 8, 6, 11, 13, P["p"])
    shade(im, 4, 2, 11, 5, P); shade(im, 4, 6, 7, 13, P); shade(im, 8, 6, 11, 13, P)
    put(im, [(7, 3), (8, 3)], P["a"]); return outline(im)

def boots(P):
    im = blank(); rect(im, 2, 7, 6, 12, P["p"]); rect(im, 9, 7, 13, 12, P["p"])
    shade(im, 2, 7, 6, 12, P); shade(im, 9, 7, 13, 12, P)
    rect(im, 1, 12, 6, 13, P["d"]); rect(im, 9, 12, 14, 13, P["d"]); put(im, [(4, 8), (11, 8)], P["a"]); return outline(im)

def ingot(P):
    im = blank()
    for i, y in enumerate(range(5, 11)):
        rect(im, 2 + (5 - i) // 2, y, 10 + (5 - i) // 2, y, P["p"])
    put(im, [(x, 5) for x in range(5, 14)], P["l"]); put(im, [(x, 10) for x in range(2, 11)], P["d"]); put(im, [(7, 7), (8, 7)], P["a"])
    return outline(im)

def shard(P):
    im = blank()
    for (x, y) in [(8,1),(7,2),(8,2),(9,2),(7,3),(8,3),(9,3),(6,4),(7,4),(8,4),(9,4),(10,4),(6,5),(7,5),(8,5),(9,5),(10,5),(6,6),(7,6),(8,6),(9,6),(7,7),(8,7),(8,8),(8,9),(8,10),(9,11)][:27]:
        put(im, [(x, y)], P["p"])
    put(im, [(8,1),(7,2),(7,3),(6,4),(6,5)], P["l"]); put(im, [(10,4),(10,5),(9,6),(8,8)], P["d"]); put(im, [(8, 4), (8, 5)], P["a"])
    return outline(im)

def template(P):
    im = blank(); rect(im, 2, 2, 13, 13, P["d"]); rect(im, 3, 3, 12, 12, P["p"]); shade(im, 2, 2, 13, 13, P)
    for (x, y) in [(8,4),(7,5),(9,5),(6,6),(10,6),(7,7),(9,7),(8,8),(7,9),(9,9),(8,10)]: put(im, [(x, y)], P["a"])
    put(im, [(4, 4), (11, 4), (4, 11), (11, 11)], P["l"]); return outline(im)

ICONS = dict(helmet=helmet, chestplate=chestplate, leggings=leggings, boots=boots,
             sword=sword, pickaxe=pickaxe, axe=axe, shovel=shovel, hoe=hoe,
             ingot=ingot, shard=shard, upgrade_template=template)
WEAPONS = dict(greatsword=lambda P: sword(P, greatsword=True), ember_dagger=lambda P: sword(P, dagger=True), magma_warhammer=warhammer)
WEAPON_NAMES = {"infernal": {"greatsword": "greatsword", "ember_dagger": "ember_dagger", "magma_warhammer": "warhammer"}}

def write(path, data, mode="w"):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, mode) as f: f.write(data)

def item_asset(item_id, handheld=True):
    write(f"{ROOT}/assets/{NS}/items/{item_id}.json", json.dumps({"model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"}}, indent=2))
    write(f"{ROOT}/assets/{NS}/models/item/{item_id}.json", json.dumps(
        {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item_id}"}}, indent=2))

def armor_layers(tier, P):
    random.seed(tier)
    for layer in ("humanoid", "humanoid_leggings"):
        im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        regions = [(0, 0, 31, 15), (16, 16, 39, 31), (40, 16, 55, 31), (0, 16, 15, 31)]
        for (x0, y0, x1, y1) in regions:
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    c = P["p"]
                    if (x + y) % 7 == 0: c = P["d"]
                    elif random.random() < 0.08: c = P["l"]
                    if x in (x0, x1) or y in (y0, y1): c = P["d"]
                    im.putpixel((x, y), c + (255,))
        # accent studs
        for x in range(2, 30, 6): im.putpixel((x, 8), P["a"] + (255,))
        for x in range(18, 38, 5): im.putpixel((x, 22), P["a"] + (255,))
        path = f"{ROOT}/assets/{NS}/textures/entity/equipment/{layer}/{tier}.png"
        os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
    write(f"{ROOT}/assets/{NS}/equipment/{tier}.json", json.dumps(
        {"layers": {"humanoid": [{"texture": f"{NS}:{tier}"}], "humanoid_leggings": [{"texture": f"{NS}:{tier}"}]}}, indent=2))

def build(tiers):
    write(f"{ROOT}/pack.mcmeta", json.dumps({"pack": {"description": "AdvancedGear textures", "min_format": 88, "max_format": 999}}, indent=2))
    sheet_items = []
    for tier in tiers:
        P = PALETTES[tier]
        for name, fn in ICONS.items():
            item_id = f"{tier}_{name}"
            img = fn(P); path = f"{ROOT}/assets/{NS}/textures/item/{item_id}.png"
            os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
            item_asset(item_id, handheld=name in ("sword","pickaxe","axe","shovel","hoe")); sheet_items.append(img)
        for wid, fn in WEAPONS.items():
            item_id = f"{tier}_{wid}" if tier != "infernal" else wid  # infernal weapons use their own names
            img = fn(P); path = f"{ROOT}/assets/{NS}/textures/item/{item_id}.png"
            os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path); item_asset(item_id); sheet_items.append(img)
        armor_layers(tier, P)
    cols = 9
    rows = (len(sheet_items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 72, rows * 72), (38, 38, 46, 255))
    for i, im in enumerate(sheet_items):
        t = im.resize((64, 64), Image.NEAREST); sheet.paste(t, ((i % cols) * 72 + 4, (i // cols) * 72 + 4), t)
    sheet.save(os.path.join(ROOT, "..", "tools", "preview.png"))

if __name__ == "__main__":
    build(["infernal"])   # milestone 1; later: build(list(PALETTES))
    print("pack generated in", os.path.abspath(ROOT))
