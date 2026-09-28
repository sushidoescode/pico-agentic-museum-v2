"""Author the Earth Collection delivery box (exhibit 005): Blender original, generic opened shipping carton.

Usage: Blender -b --factory-startup --python art/exhibits/deliverybox/author_deliverybox.py -- --out <glb>

Regular slotted corrugated carton 350 x 250 x 200 mm (W x D x H), 4 mm board, opened and empty: four top flaps
folded outward at different angles with a slight sag, cut tape halves on the long flaps' free edges and tape
tails down the two end walls, a blank white shipping label (grey blocks and a non-decoding bar pattern, no text).
No retailer branding or pictograms. Materials: cardboard (tiling kraft texture), tape, label (small texture).
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

BOARD, TAPE, LABEL = 0, 1, 2
W, D, H, T = 0.350, 0.250, 0.200, 0.004
TILE_M = 0.25


def kraft_texture(path):
    n = 512
    base = np.array([178, 138, 96], float) / 255.0
    v = S.fbm(n, n, 4, 5, seed=11)
    fib = S.value_noise(n, n, 256, 32, seed=5)  # streaky fibres along u
    y = (np.arange(n) + 0.5) / n
    flutes = 0.5 + 0.5 * np.cos(2 * np.pi * y * 32)  # faint flute lines every ~8 mm
    shade = 0.90 + 0.12 * (v - 0.5) + 0.05 * (fib - 0.5) - 0.025 * flutes[:, None]
    col = base[None, None, :] * shade[..., None]
    return S.save_jpg(path, S.to_u8(col), 90)


def label_texture(path):
    w, h = 256, 128
    img = np.ones((h, w, 3)) * np.array([240, 238, 232]) / 255.0
    ink = np.array([70, 70, 72]) / 255.0
    grey = np.array([175, 175, 172]) / 255.0

    def rect(x0, y0, x1, y1, c):
        img[int(y0 * h):int(y1 * h), int(x0 * w):int(x1 * w)] = c
    # blank address blocks (solid pale grey bars, not text)
    for i, (x1) in enumerate((0.55, 0.45, 0.50, 0.35)):
        rect(0.06, 0.10 + i * 0.11, x1, 0.16 + i * 0.11, grey)
    rect(0.62, 0.10, 0.94, 0.44, grey)
    # non-decoding bar pattern (random widths, no quiet zone or check digits)
    rng = np.random.default_rng(2026)
    x = 0.06
    while x < 0.94:
        bw = rng.choice([0.004, 0.008, 0.012])
        if rng.random() < 0.55:
            rect(x, 0.60, x + bw, 0.90, ink)
        x += bw
    return S.save_jpg(path, S.to_u8(img), 92)


def flap(bm, hinge_mid, hinge_axis, up0, out_dir, length, width, angle_deg, sag_deg, mat, tape=None):
    """Thin slab hinged at hinge_mid (on the wall's outer top edge). angle 0 = standing straight up, +90 = folded
    down flat against the outside of the wall. The flap bends a further sag_deg along its length."""
    pts = []
    n = 10
    ang0 = math.radians(angle_deg)
    # integrate the curved centreline (flap mid-thickness, continuous with the wall's mid-plane at the hinge)
    p = Vector(hinge_mid)
    pts.append(p.copy())
    for i in range(n):
        t = (i + 0.5) / n
        a = ang0 + math.radians(sag_deg) * t * t
        d = up0 * math.cos(a) + out_dir * math.sin(a)
        p = p + d * (length / n)
        pts.append(p.copy())
    frames = []
    for i, q in enumerate(pts):
        t = i / n
        a = ang0 + math.radians(sag_deg) * t * t
        d = up0 * math.cos(a) + out_dir * math.sin(a)
        nrm = hinge_axis.cross(d).normalized()
        if nrm.dot(out_dir * math.cos(a) - up0 * math.sin(a)) < 0:
            nrm = -nrm
        frames.append((q, hinge_axis, nrm))
    prof = [(-width / 2, -T / 2), (width / 2, -T / 2), (width / 2, T / 2), (-width / 2, T / 2)]
    faces = S.sweep(bm, prof, frames, mat)
    return faces, frames


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    tex = os.path.join(HERE, "textures")
    kraft = kraft_texture(os.path.join(tex, "deliverybox_kraft.jpg"))
    lab = label_texture(os.path.join(tex, "deliverybox_label.jpg"))
    mats = [ec.pbr_material("deliverybox_board", (1, 1, 1, 1), metallic=0.0, roughness=0.86,
                            base_image=ec.load_image(kraft, 'sRGB', "deliverybox_kraft")),
            ec.pbr_material("deliverybox_tape", ec.srgb(196, 160, 112), metallic=0.0, roughness=0.30),
            ec.pbr_material("deliverybox_label", (1, 1, 1, 1), metallic=0.0, roughness=0.70,
                            base_image=ec.load_image(lab, 'sRGB', "deliverybox_label"))]
    parts = []
    hw, hd = W / 2, D / 2
    # walls and bottom (slightly rounded board edges)
    bm = bmesh.new()
    board = []
    walls = [((0, -hd + T / 2), hw, T / 2), ((0, hd - T / 2), hw, T / 2),
             ((-hw + T / 2, 0), T / 2, hd - T), ((hw - T / 2, 0), T / 2, hd - T)]
    for (cx, cy), ww, dd in walls:
        board += S.plan_loft(bm, S.rrect_fn(ww, dd, 0.0012, 2), [(0, 0.0), (0, H)], BOARD,
                             xform=Matrix.Translation((cx, cy, 0)))
    board += S.plan_loft(bm, S.rrect_fn(hw - T, hd - T, 0.001, 2), [(0, 0.0), (0, T)], BOARD)
    # flaps: front/back long flaps (W x D/2), side flaps (D x D/2), 3 mm clearance
    fl = D / 2 - 0.003
    specs = [  # hinge mid, hinge axis, outward dir, width, angle, sag
        (Vector((0, -hd + T / 2, H)), Vector((1, 0, 0)), Vector((0, -1, 0)), W - 0.006, 112, 10),
        (Vector((0, hd - T / 2, H)), Vector((1, 0, 0)), Vector((0, 1, 0)), W - 0.006, 28, 6),
        (Vector((-hw + T / 2, 0, H)), Vector((0, 1, 0)), Vector((-1, 0, 0)), D - 0.010, 58, 8),
        (Vector((hw - T / 2, 0, H)), Vector((0, 1, 0)), Vector((1, 0, 0)), D - 0.010, 46, 8),
    ]
    tape_faces = []
    bm_t = bmesh.new()
    for i, (hm, ax, od, wid, ang, sag) in enumerate(specs):
        f, frames = flap(bm, hm, ax, Vector((0, 0, 1)), od, fl, wid, ang, sag, BOARD)
        board += f
        if i < 2:  # cut tape half along the long flap's free edge (outer surface), 24 mm wide
            k0 = int(len(frames) * 0.78)
            tf = []
            for (o, sa, nrm) in frames[k0:]:
                tf.append((o + nrm * (T / 2 + 0.0006), sa, nrm))
            tw = wid / 2 - 0.002
            prof = [(-tw, -0.0003), (tw, -0.0003), (tw, 0.0003), (-tw, 0.0003)]
            tape_faces += S.sweep(bm_t, prof, tf, TAPE)
    # tape tails where the seam tape wrapped over the ends: down the outside of the left and right walls
    # (48 mm wide, 60 mm long)
    for sx in (-1, 1):
        x = sx * (hw + 0.0006)
        S.plan_loft(bm_t, S.rrect_fn(0.0003, 0.024, 0.0002, 1), [(0, H - 0.060), (0, H - 0.0005)], TAPE,
                    xform=Matrix.Translation((x, 0, 0)))
    # label on the front wall, right of the tape
    bm_l = bmesh.new()
    lf = S.prism(bm_l, [(-0.050, -0.0004), (0.050, -0.0004), (0.050, 0.0004), (-0.050, 0.0004)], 0.0, 0.066,
                 LABEL, xform=Matrix.Translation((0.095, -hd - 0.0006, 0.085)))
    S.uv_planar(bm_l, lf, (0.045, 0, 0.085), (1, 0, 0), (0, 0, 1), scale=1 / 0.100, offset=(0.0, 0.0))
    # fix the label UV to the 100 x 66 mm face: u from x in [0.045, 0.145], v from z in [0.085, 0.151]
    uvl = bm_l.loops.layers.uv.verify()
    for f_ in lf:
        for loop in f_.loops:
            c = loop.vert.co
            loop[uvl].uv = ((c.x - 0.045) / 0.100, (c.z - 0.085) / 0.066)
    S.uv_box(bm, board, scale=1 / TILE_M)
    parts.append(S.part("board", bm, mats, smooth_angle=30))
    S.uv_box(bm_t, list(bm_t.faces), scale=1.0)
    parts.append(S.part("tape", bm_t, mats, smooth_angle=30))
    parts.append(S.part("label", bm_l, mats, smooth_angle=30))
    S.finish_exhibit("deliverybox", parts, a.out, a.record, {
        "script": "art/exhibits/deliverybox/author_deliverybox.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider; no retailer name, smile or pictogram"},
        "materials": {"deliverybox_board": "kraft texture (tiles every 0.25 m) r0.86", "deliverybox_tape": "tan tape sRGB (196,160,112) r0.30",
                      "deliverybox_label": "blank label texture: grey blocks + non-decoding random bars, no text"},
        "textures": {os.path.basename(p): {"path": ec.rel(p), "sha256": ec.sha256_file(p), "bytes": os.path.getsize(p)}
                     for p in (kraft, lab)}})


if __name__ == "__main__":
    main()
