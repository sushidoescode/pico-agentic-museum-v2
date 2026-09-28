"""Author the Earth Collection disposable face mask (exhibit 008): Blender original.

Usage: Blender -b --factory-startup --python art/exhibits/facemask/author_facemask.py -- --out <glb>

Three-ply pleated mask 175 x 95 mm standing on its lower edge, outer face toward +Z (glTF): horizontally curved
(28 mm bulge), three downward-facing pleats (proud lower fold edges, sharp fold lines), 6 mm sealed side bindings, a slight nose-wire
ridge along the top edge, white inner face, and two white elastic ear loops curving back from the corners.
Closed shell (front/back surfaces + rim). Materials: outer nonwoven (tiling speckle texture), inner/elastic white.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

OUTER, WHITE = 0, 1
W, H = 0.175, 0.095
BULGE = 0.028
THICK = 0.0012
PLEATS = 3
PLEAT_DEPTH = 0.0032
NU = 64


def nonwoven(path):
    n = 256
    v = S.fbm(n, n, 16, 3, seed=41)
    sp = S.value_noise(n, n, 128, 128, seed=42)
    base = np.array([128, 176, 206]) / 255.0
    shade = 0.93 + 0.10 * (v - 0.5) + 0.06 * (sp - 0.5)
    return S.save_jpg(path, S.to_u8(base[None, None, :] * shade[..., None]), 90)


def z_rows():
    """Rows (z, pleat offset) bottom->top: three pleats; each fold is a vertical step (two rows at one z)."""
    rows = []
    margin = 0.012
    period = (H - 2 * margin) / PLEATS
    rows.append((0.0, 0.0))
    rows.append((margin * 0.5, 0.0))
    for k in range(PLEATS):
        z0 = margin + k * period
        n = 8
        for i in range(n + 1):
            t = i / n
            # pleats face downward: each panel's lower fold edge stands proud (-Y) and overhangs the panel below;
            # the panel recedes going up, then the next fold steps forward again (near-vertical step face)
            rows.append((z0 + t * period * 0.999, -PLEAT_DEPTH * (1 - t)))
    rows.append((H - margin * 0.5, 0.0))
    rows.append((H - 0.002, -0.0008))   # nose-wire ridge
    rows.append((H, 0.0))
    return rows


def surface_point(x, z, pleat):
    """Front surface: bulge toward -Y (glTF +Z) in the middle, pleat offsets forward (-Y)."""
    xn = x / (W / 2)
    edge = min(1.0, (1 - abs(xn)) / 0.10)  # pleats flatten into the side bindings
    y = -BULGE * (1 - xn * xn) + pleat * edge
    # slight vertical curvature: top and bottom edges pull back
    zn = (z / H) * 2 - 1
    y += 0.006 * zn * zn
    return Vector((x, y, z))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    tex = os.path.join(HERE, "textures")
    nw = nonwoven(os.path.join(tex, "facemask_nonwoven.jpg"))
    mats = [ec.pbr_material("facemask_outer", (1, 1, 1, 1), metallic=0.0, roughness=0.92,
                            base_image=ec.load_image(nw, 'sRGB', "facemask_nonwoven")),
            ec.pbr_material("facemask_white", ec.srgb(236, 236, 232), metallic=0.0, roughness=0.85)]
    rows = z_rows()
    xs = [(-W / 2) + W * i / NU for i in range(NU + 1)]
    bm = bmesh.new()
    F = [[bm.verts.new(surface_point(x, z, p)) for x in xs] for z, p in rows]
    Bk = []
    for (z, p), rowf in zip(rows, F):
        Bk.append([bm.verts.new(v.co + Vector((0, THICK, 0))) for v in rowf])
    faces_front, faces_back, rim = [], [], []
    nr, nc = len(rows), len(xs)
    for r in range(nr - 1):
        for c in range(nc - 1):
            f = bm.faces.new((F[r][c], F[r][c + 1], F[r + 1][c + 1], F[r + 1][c]))
            f.material_index = OUTER
            faces_front.append(f)
            b = bm.faces.new((Bk[r][c], Bk[r + 1][c], Bk[r + 1][c + 1], Bk[r][c + 1]))
            b.material_index = WHITE
            faces_back.append(b)
    # rim around the perimeter
    loop_idx = [(0, c) for c in range(nc)] + [(r, nc - 1) for r in range(1, nr)] + \
               [(nr - 1, c) for c in range(nc - 2, -1, -1)] + [(r, 0) for r in range(nr - 2, 0, -1)]
    for i in range(len(loop_idx)):
        r0, c0 = loop_idx[i]
        r1, c1 = loop_idx[(i + 1) % len(loop_idx)]
        f = bm.faces.new((F[r0][c0], Bk[r0][c0], Bk[r1][c1], F[r1][c1]))
        f.material_index = OUTER
        rim.append(f)
    bmesh.ops.recalc_face_normals(bm, faces=faces_front + faces_back + rim)
    # sharp fold lines: edges along rows that are fold steps (same z, different pleat offset)
    for r in range(nr - 1):
        if abs(rows[r][0] - rows[r + 1][0]) < 2e-4:
            for rr in (r, r + 1):
                for c in range(nc - 1):
                    e = bm.edges.get((F[rr][c], F[rr][c + 1]))
                    if e:
                        e.smooth = False
    S.uv_planar(bm, faces_front + rim, (0, 0, 0), (1, 0, 0), (0, 0, 1), scale=1 / 0.04)
    S.uv_const(bm, faces_back, 0.5, 0.5)
    parts = [S.part("mask", bm, mats, smooth_angle=50)]
    # side bindings: slightly thicker sealed strips over the outer face edges
    bm = bmesh.new()
    for sx in (-1, 1):
        x0 = sx * (W / 2 - 0.003)
        pts = [surface_point(x0, z, 0.0) + Vector((0, -0.0006, 0)) for z in np.linspace(0.001, H - 0.001, 24)]
        prof = [(-0.003, -0.0005), (0.003, -0.0005), (0.003, 0.0007), (-0.003, 0.0007)]
        S.tube(bm, pts, 0.0, profile=prof, mat=OUTER, up=(0, -1, 0))
    S.uv_planar(bm, list(bm.faces), (0, 0, 0), (1, 0, 0), (0, 0, 1), scale=1 / 0.04)
    parts.append(S.part("bindings", bm, mats, smooth_angle=40))
    # ear loops: elastic cords from top to bottom corner on each side, curving back (+Y) and slightly out
    bm = bmesh.new()
    for sx in (-1, 1):
        top = surface_point(sx * (W / 2 - 0.004), H - 0.010, 0.0) + Vector((0, THICK / 2, 0))
        bot = surface_point(sx * (W / 2 - 0.004), 0.010, 0.0) + Vector((0, THICK / 2, 0))
        c1 = top + Vector((sx * 0.018, 0.070, 0.004))
        c2 = bot + Vector((sx * 0.018, 0.070, -0.004))
        pts = []
        for i in range(33):
            t = i / 32
            p = (1 - t) ** 3 * top + 3 * (1 - t) ** 2 * t * c1 + 3 * (1 - t) * t * t * c2 + t ** 3 * bot
            pts.append(p)
        S.tube(bm, pts, 0.0013, 8, WHITE)
    S.uv_const(bm, list(bm.faces), 0.5, 0.5)
    parts.append(S.part("loops", bm, mats, smooth_angle=60))
    S.finish_exhibit("facemask", parts, a.out, a.record, {
        "script": "art/exhibits/facemask/author_facemask.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "materials": {"facemask_outer": "pale blue nonwoven speckle texture (tiles every 40 mm) r0.92",
                      "facemask_white": "white inner layer / elastic sRGB (236,236,232) r0.85"},
        "textures": {os.path.basename(nw): {"path": ec.rel(nw), "sha256": ec.sha256_file(nw), "bytes": os.path.getsize(nw)}}})


if __name__ == "__main__":
    main()
