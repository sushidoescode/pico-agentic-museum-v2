"""Author the Earth Collection Saturn V desk replica (exhibit 018): Blender original, built from public proportions.

Usage: Blender -b --factory-startup --python art/exhibits/rocket/author_rocket.py -- --out <glb>

Why original: the reference rocket GLB (Sketchfab export) carries asset.extras license CC-BY-ND-4.0
(NoDerivatives), so nothing is derived from it; this model is authored from general public dimensions only.

Stack (real metres, overall 110.6 m): S-IC 0-42 (F-1 bells 0-5.6, 5.03 m radius, four engine fairings and fins),
S-II 42-66.8, conical interstage to the S-IVB (3.3 m radius) 66.8-72.6, S-IVB to 84.7, IU 84.7-85.6,
SLA cone 85.6-94.1, service module 94.1-97.9, command module + boost cover 97.9-101.1, escape tower and motor
101.1-110.6. Scaled 1:212.7 to 0.52 m and stood on a 25 mm black lacquer stand with a blank brass plate
(total ~0.545 m). Stylised roll pattern (black/white quarter panels on the S-IC and S-IVB), no flags or lettering.
One 512 x 1024 texture: left half = livery strip mapped by angle (u) and height (v); right half = swatches.
Two materials: paint (dielectric) and metal (bells, service module, RCS, brass plate).
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

PAINT, METAL = 0, 1
TOTAL = 110.6
HEIGHT = 0.520
SCALE = HEIGHT / TOTAL
STAND_H = 0.025
TW, TH = 512, 1024

WHITE = (236, 234, 228)
BLACK = (24, 24, 26)
SWATCH = {"white": (0.60, WHITE), "black": (0.70, (20, 20, 22)), "brass": (0.80, (182, 142, 78)),
          "silver": (0.90, (200, 202, 205))}


def livery(a01, z):
    q = int(math.floor(a01 * 4)) % 4
    if z < 5.6:
        return (58, 58, 60)                              # F-1 bells, dark metal
    if z < 15.0:
        return BLACK if q in (0, 2) else WHITE           # S-IC lower roll pattern
    if 36.0 <= z < 42.0:
        return BLACK if q in (1, 3) else WHITE           # S-IC forward roll pattern
    if 72.6 <= z < 77.0:
        return BLACK if q in (0, 2) else WHITE           # S-IVB aft roll pattern
    if 84.7 <= z < 85.6:
        return (192, 192, 190)                           # instrument unit
    if 85.6 <= z < 94.1:
        return (220, 220, 216)                           # spacecraft adapter
    if 94.1 <= z < 97.9:
        return (200, 202, 205)                           # service module (metal)
    if 101.0 <= z < 104.2:
        return (205, 92, 42)                             # escape tower
    if 108.9 <= z:
        return (60, 60, 62)                              # canards / nose
    return WHITE


def texture(path):
    img = np.zeros((TH, TW, 3))
    for r in range(TH):
        z = (1 - (r + 0.5) / TH) * TOTAL
        for c in range(TW // 2):
            img[r, c] = livery((c + 0.5) / (TW // 2), z)
    for name, (u, col) in SWATCH.items():
        c0 = int((u - 0.04) * TW)
        img[:, c0:c0 + int(0.08 * TW)] = col
    return S.save_png(path, img.astype(np.uint8))


def strip_uv(co, a01):
    return (a01 * 0.5, co.z / TOTAL)


def swatch_uv(bm, faces, name):
    S.uv_const(bm, faces, SWATCH[name][0], 0.5)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    tex = texture(os.path.join(HERE, "textures", "rocket_livery.png"))
    img = ec.load_image(tex, 'sRGB', "rocket_livery")
    mats = [ec.pbr_material("rocket_paint", (1, 1, 1, 1), metallic=0.0, roughness=0.42, base_image=img),
            ec.pbr_material("rocket_metal", (1, 1, 1, 1), metallic=1.0, roughness=0.32, base_image=img)]
    parts = []
    bm = bmesh.new()
    # main body (real metres; scaled at the end)
    prof = [(0, 5.6), (4.5, 5.6), (5.03, 5.9), (5.03, 41.9), (4.93, 42.0), (4.93, 42.2), (5.03, 42.3),
            (5.03, 47.4), (4.95, 47.5), (5.03, 47.6), (5.03, 66.8), (4.2, 69.7), (3.30, 72.6), (3.30, 84.6),
            (3.24, 84.7), (3.30, 84.8), (3.30, 85.6), (2.62, 89.85), (1.95, 94.1), (1.95, 97.9), (1.2, 99.5),
            (0.45, 101.1), (0, 101.15)]
    em = []
    for i in range(len(prof) - 1):
        zmid = (prof[i][1] + prof[i + 1][1]) / 2
        em.append(METAL if 94.1 <= zmid < 97.9 else PAINT)
    S.lathe(bm, prof, 64, em, uv=strip_uv)
    # escape tower (frustum) and motor with nose
    S.lathe(bm, [(0, 101.0), (0.62, 101.0), (0.32, 104.25), (0, 104.3)], 16, PAINT, uv=strip_uv)
    S.lathe(bm, [(0, 104.2), (0.33, 104.2), (0.33, 108.8), (0.20, 109.6), (0.12, 110.2), (0.05, 110.55), (0, 110.6)],
            24, PAINT, uv=strip_uv)
    # five F-1 bells
    bell = [(0, 0.8), (1.78, 0.05), (1.88, 0.0), (1.88, 0.15), (1.5, 1.6), (1.1, 3.2), (0.75, 4.4), (0.70, 5.0),
            (0.9, 5.6), (0, 5.7)]
    for x, y in [(0, 0)] + [(3.25 * math.cos(math.radians(45 + 90 * k)), 3.25 * math.sin(math.radians(45 + 90 * k)))
                           for k in range(4)]:
        S.lathe(bm, bell, 32, METAL, uv=strip_uv, xform=Matrix.Translation((x, y, 0)))
    # four engine fairings (white) and fins (white) on the outer-engine lines
    fair = [(0, 3.8), (2.1, 3.8), (2.2, 4.3), (1.6, 8.0), (0.9, 11.0), (0.3, 12.5), (0, 12.8)]
    fin_faces = []
    for k in range(4):
        ang = math.radians(45 + 90 * k)
        f = S.lathe(bm, fair, 32, PAINT, xform=Matrix.Translation((4.25 * math.cos(ang), 4.25 * math.sin(ang), 0)))
        fin_faces += f
        outline = [(5.6, 4.6), (9.0, 3.2), (9.0, 6.0), (5.6, 10.8)]
        ff = S.prism(bm, outline, -0.25, 0.25, PAINT,
                     xform=Matrix.Rotation(ang, 4, 'Z') @ Matrix.Rotation(math.radians(90), 4, 'X'))
        fin_faces += ff
    swatch_uv(bm, fin_faces, "white")
    # S-IVB auxiliary propulsion fairings and SM RCS quads
    extra_white, extra_metal = [], []
    for k in range(2):
        ang = math.radians(90 + 180 * k)
        ff = S.plan_loft(bm, S.rrect_fn(0.3, 0.7, 0.25, 3), [(0.0, 72.8), (0.0, 75.4)], PAINT,
                         xform=Matrix.Rotation(ang, 4, 'Z') @ Matrix.Translation((3.35, 0, 0)))
        extra_white += ff
    for k in range(4):
        ang = math.radians(45 + 90 * k)
        ff = S.plan_loft(bm, S.rrect_fn(0.22, 0.35, 0.08, 2), [(0.0, 95.2), (0.0, 96.4)], METAL,
                         xform=Matrix.Rotation(ang, 4, 'Z') @ Matrix.Translation((2.05, 0, 0)))
        extra_metal += ff
    swatch_uv(bm, extra_white, "white")
    swatch_uv(bm, extra_metal, "silver")
    bmesh.ops.transform(bm, matrix=Matrix.Translation((0, 0, STAND_H)) @ Matrix.Scale(SCALE, 4), verts=list(bm.verts))
    parts.append(S.part("rocket", bm, mats, smooth_angle=35))
    # stand: black lacquer square plinth with a blank brass plate on the front
    bm = bmesh.new()
    sf = S.plan_loft(bm, S.rrect_fn(0.060, 0.060, 0.006, 6), S.edge_profile(0.0, STAND_H, 0.0015, 0.003, 2, 3), PAINT)
    swatch_uv(bm, sf, "black")
    pf = S.plan_loft(bm, S.rrect_fn(0.030, 0.0006, 0.0005, 2), [(0.0, 0.007), (0.0, 0.017)], METAL,
                     xform=Matrix.Translation((0, -0.0603, 0)))
    swatch_uv(bm, pf, "brass")
    parts.append(S.part("stand", bm, mats))
    S.finish_exhibit("rocket", parts, a.out, a.record, {
        "script": "art/exhibits/rocket/author_rocket.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original",
                   "note": "authored from general public Saturn V proportions; the reference rocket GLB (asset.extras license CC-BY-ND-4.0, Sketchfab) was not opened for geometry or texture and nothing is derived from it"},
        "scale": f"1:{1 / SCALE:.1f} (110.6 m -> {HEIGHT} m) on a {STAND_H} m stand",
        "materials": {"rocket_paint": "livery strip + swatches, dielectric r0.42", "rocket_metal": "same texture, metallic 1.0 r0.32 (bells, service module, RCS, brass plate)"},
        "textures": {os.path.basename(tex): {"path": ec.rel(tex), "sha256": ec.sha256_file(tex), "bytes": os.path.getsize(tex)}}})


if __name__ == "__main__":
    main()
