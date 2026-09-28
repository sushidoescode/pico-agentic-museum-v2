"""Author the Earth Collection QR menu stand (exhibit 006): Blender original.

Usage: Blender -b --factory-startup --python art/exhibits/qrcode/author_qrcode.py -- --out <glb>

A small oak block (100 x 60 x 22 mm) holding a printed card (94 x 140 mm, 4 mm corner radius, leaning back 8 degrees) with a
QR-style pattern: three finder squares, timing rows, one alignment square and a dark module in the positions of a
version-2 (25 x 25) symbol, but the data AND format-information modules are seeded random, so the pattern is
deliberately NON-DECODING (not a URL, not any text). Slate band at the top of the card, thin brass-coloured rule,
plain slate back. No text anywhere. Materials: card (1024 px atlas: front + back), wood (512 px tiling grain).
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Matrix, Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

CARD, WOOD = 0, 1
CW, CH, CT = 0.094, 0.140, 0.0016
BW, BD, BH = 0.100, 0.060, 0.022
TILT = math.radians(8)
SLOT_Z = 0.010   # card bottom sits 12 mm deep in the slot
ATLAS = 1024


def qr_modules(seed=6):
    n = 25
    m = np.zeros((n, n), int)
    fixed = np.zeros((n, n), bool)

    def finder(r0, c0):
        for r in range(-1, 8):
            for c in range(-1, 8):
                rr, cc = r0 + r, c0 + c
                if 0 <= rr < n and 0 <= cc < n:
                    fixed[rr, cc] = True
                    on = (0 <= r <= 6 and 0 <= c <= 6) and (r in (0, 6) or c in (0, 6) or (2 <= r <= 4 and 2 <= c <= 4))
                    m[rr, cc] = 1 if on else 0
    finder(0, 0); finder(0, n - 7); finder(n - 7, 0)
    for i in range(8, n - 8):  # timing
        m[6, i] = m[i, 6] = 1 - (i % 2)
        fixed[6, i] = fixed[i, 6] = True
    for r in range(-2, 3):  # alignment at (18, 18)
        for c in range(-2, 3):
            m[18 + r, 18 + c] = 1 if (max(abs(r), abs(c)) != 1) else 0
            fixed[18 + r, 18 + c] = True
    m[4 * 2 + 9, 8] = 1
    fixed[4 * 2 + 9, 8] = True
    rng = np.random.default_rng(seed)
    rnd = rng.random((n, n)) < 0.5
    m[~fixed] = rnd[~fixed]  # data + format bits: random -> no valid format/RS codewords
    return m


def card_atlas(path):
    """Left half (512 x 697 px) = front, right half = back. Row 0 = top of the card."""
    img = np.ones((ATLAS, ATLAS, 3)) * np.array([236, 234, 228]) / 255.0
    fw = ATLAS // 2
    fh = int(round(fw * CH / CW))  # 697
    white = np.array([244, 242, 236]) / 255.0
    slate = np.array([70, 86, 98]) / 255.0
    brass = np.array([176, 136, 74]) / 255.0
    ink = np.array([22, 22, 24]) / 255.0
    front = np.ones((fh, fw, 3)) * white
    front[: int(fh * 0.16)] = slate
    front[int(fh * 0.16): int(fh * 0.16) + 4] = brass
    q = qr_modules()
    quiet = 2
    px = (fw - 80) // (25 + 2 * quiet)          # module size in px
    size = px * (25 + 2 * quiet)
    x0 = (fw - size) // 2
    y0 = int(fh * 0.16) + 40
    front[y0:y0 + size, x0:x0 + size] = white
    for r in range(25):
        for c in range(25):
            if q[r, c]:
                yy, xx = y0 + (quiet + r) * px, x0 + (quiet + c) * px
                front[yy:yy + px, xx:xx + px] = ink
    front[fh - 60: fh - 56, fw // 2 - 60: fw // 2 + 60] = brass
    back = np.ones((fh, fw, 3)) * slate
    back[8:-8, 8:12] = back[8:-8, -12:-8] = white * 0.9
    back[8:12, 8:-8] = back[-12:-8, 8:-8] = white * 0.9
    img[:fh, :fw] = front
    img[:fh, fw:] = back
    return S.save_png(path, S.to_u8(img)), fh, (px, x0, y0)


def wood_texture(path):
    n = 512
    y = (np.arange(n) + 0.5) / n
    warp = S.fbm(n, n, 3, 3, seed=21)
    rings = 0.5 + 0.5 * np.sin(2 * np.pi * (y[:, None] * 14 + 0.9 * warp))
    fine = S.value_noise(n, n, 256, 8, seed=22)
    base = np.array([180, 138, 94]) / 255.0
    dark = np.array([150, 108, 70]) / 255.0
    t = 0.35 * rings ** 4 + 0.30 * fine
    col = base[None, None, :] * (1 - t[..., None]) + dark[None, None, :] * t[..., None]
    return S.save_jpg(path, S.to_u8(col), 90)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    tex = os.path.join(HERE, "textures")
    card_png, fh, qinfo = card_atlas(os.path.join(tex, "qrcode_card.png"))
    wood_jpg = wood_texture(os.path.join(tex, "qrcode_wood.jpg"))
    mats = [ec.pbr_material("qrcode_card", (1, 1, 1, 1), metallic=0.0, roughness=0.55,
                            base_image=ec.load_image(card_png, 'sRGB', "qrcode_card")),
            ec.pbr_material("qrcode_wood", (1, 1, 1, 1), metallic=0.0, roughness=0.62,
                            base_image=ec.load_image(wood_jpg, 'sRGB', "qrcode_wood"))]
    parts = []
    # oak block with rounded plan corners and softened top edge, centred under the card
    bm = bmesh.new()
    wf = S.plan_loft(bm, S.rrect_fn(BW / 2, BD / 2, 0.006, 6), S.edge_profile(0.0, BH, 0.0015, 0.004, 2, 4), WOOD)
    S.uv_box(bm, wf, scale=1 / 0.12)
    parts.append(S.part("block", bm, mats))
    # card: thin slab, leaning back (+Y) by TILT about its bottom edge
    bm = bmesh.new()
    # rounded-corner card: outline in (x, up), extruded through its thickness, then stood up (local up -> +Z,
    # local thickness -> -Y so the printed front faces -Y = glTF +Z)
    outline = [(x, y + CH / 2) for x, y in ec.rounded_rect(CW / 2, CH / 2, 0.004, 6)]
    cf = S.prism(bm, outline, -CT / 2, CT / 2, CARD, xform=Matrix.Rotation(math.radians(90), 4, 'X'))
    uvl = bm.loops.layers.uv.verify()
    fu = 0.5
    fv = fh / ATLAS
    for f in cf:
        f.normal_update()
        for loop in f.loops:
            c = loop.vert.co
            x01 = (c.x + CW / 2) / CW
            z01 = c.z / CH
            if f.normal.y < -0.5:        # front (faces -Y = glTF +Z)
                uv = (x01 * fu, 1 - (1 - z01) * fv)
            elif f.normal.y > 0.5:       # back (mirrored so it reads from behind)
                uv = (fu + (1 - x01) * fu, 1 - (1 - z01) * fv)
            else:                        # edges: white card margin
                uv = (0.75, 0.02)
            loop[uvl].uv = uv
    xf = Matrix.Translation((0, 0.004, SLOT_Z)) @ Matrix.Rotation(-TILT, 4, 'X')
    bmesh.ops.transform(bm, matrix=xf, verts=list(bm.verts))
    parts.append(S.part("card", bm, mats, smooth_angle=20))
    S.finish_exhibit("qrcode", parts, a.out, a.record, {
        "script": "art/exhibits/qrcode/author_qrcode.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "qrPattern": {"decision": "non-decoding pattern with finder squares",
                      "detail": "version-2 layout (25 x 25): finder, timing, alignment and dark module placed; data and format-information modules seeded random (numpy default_rng(6)), so no valid format/Reed-Solomon codewords; not a URL, not text",
                      "modulePx": qinfo[0]},
        "materials": {"qrcode_card": "printed card atlas r0.55", "qrcode_wood": "oak grain texture r0.62"},
        "textures": {os.path.basename(p): {"path": ec.rel(p), "sha256": ec.sha256_file(p), "bytes": os.path.getsize(p)}
                     for p in (card_png, wood_jpg)}})


if __name__ == "__main__":
    main()
