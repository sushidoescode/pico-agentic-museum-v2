"""Author the Earth Collection smartwatch (exhibit 011): Blender original, generic round design.

Usage: Blender -b --factory-startup --python art/exhibits/smartwatch/author_smartwatch.py -- --out <glb>

Round brushed-steel case 42 mm across and 11 mm thick with a thin bezel, a dark glass screen (off: dark/neutral,
no UI or text), a crown and a pusher at the right, lug bars top and bottom, and a slate-blue sport strap closed into
a wrist-sized loop (~0.21 m around) standing on its lower curve, face toward the viewer (+Z) and tilted up 10 deg.
Round (not the rectangular Apple Watch case); the reference watch (UI with readable numbers, no strap loop) is not
used. Three materials, no textures.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
from mathutils import Matrix, Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

STEEL, GLASS, STRAP = 0, 1, 2
LOOP_Y, LOOP_Z, LOOP_C = 0.030, 0.036, 0.040   # oval semi-axes (depth, height) and centre height
STRAP_W, STRAP_T = 0.022, 0.0030
TILT = math.radians(10)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("smartwatch_steel", ec.srgb(186, 188, 191), metallic=1.0, roughness=0.30),
            ec.pbr_material("smartwatch_glass", ec.srgb(9, 10, 12), metallic=0.0, roughness=0.05),
            ec.pbr_material("smartwatch_strap", ec.srgb(78, 96, 120), metallic=0.0, roughness=0.68)]
    parts = []
    # strap: flat rounded profile swept around a closed oval in the Y-Z plane (loop axis = X)
    bm = bmesh.new()
    n = 96
    pts = [Vector((0.0, LOOP_Y * math.sin(2 * math.pi * i / n), LOOP_C - LOOP_Z * math.cos(2 * math.pi * i / n)))
           for i in range(n)]
    # profile (s = thickness along the in-plane normal, t = width along the loop axis X)
    prof = ec.rounded_rect(STRAP_T / 2, STRAP_W / 2, 0.0012, 3)
    S.tube(bm, pts, 0.0, mat=STRAP, closed=True, up=(1, 0, 0), profile=prof)
    # keeper band around the strap at the back of the loop
    kp = [Vector((0.0, LOOP_Y * math.sin(2 * math.pi * i / n), LOOP_C - LOOP_Z * math.cos(2 * math.pi * i / n)))
          for i in range(int(n * 0.47), int(n * 0.53) + 1)]
    kprof = [(x, y) for x, y in ec.rounded_rect(STRAP_T / 2 + 0.0012, STRAP_W / 2 + 0.0010, 0.0015, 3)]
    S.tube(bm, kp, 0.0, mat=STRAP, up=(1, 0, 0), profile=kprof)
    parts.append(S.part("strap", bm, mats, smooth_angle=40))
    # case, built along local +Z (face up), then turned to face -Y and set on the front of the loop
    bm = bmesh.new()
    cprof = [(0, -0.0060), (0.0120, -0.0058), (0.0150, -0.0050), (0.0170, -0.0045), (0.0190, -0.0042),
             (0.0205, -0.0035), (0.0210, -0.0020), (0.0210, 0.0020), (0.0205, 0.0035), (0.0198, 0.0045),
             (0.0186, 0.0050), (0.0181, 0.0051), (0.0179, 0.0052), (0.0160, 0.0056), (0.0100, 0.0058), (0, 0.0059)]
    em = [GLASS, GLASS, STEEL, STEEL, STEEL, STEEL, STEEL, STEEL, STEEL, STEEL, STEEL, GLASS, GLASS, GLASS, GLASS]
    S.lathe(bm, cprof, 96, em, sharp_edges=(10, 11))
    # lug bars top and bottom (local +-Y), crown and pusher at 3 o'clock (local +X)
    for sy in (-1, 1):
        S.plan_loft(bm, S.rrect_fn(0.0125, 0.0035, 0.0025, 4), S.edge_profile(-0.0048, 0.0022, 0.0012, 0.0015, 2, 2),
                    STEEL, xform=Matrix.Translation((0, sy * 0.0215, 0)))
    S.lathe(bm, [(0, 0.0205), (0.0032, 0.0205), (0.0034, 0.0215), (0.0034, 0.0238), (0.0030, 0.0242), (0, 0.0243)],
            32, STEEL, xform=Matrix.Rotation(math.radians(90), 4, 'Y'))
    S.lathe(bm, [(0, 0.0200), (0.0018, 0.0200), (0.0018, 0.0228), (0, 0.0229)], 20, STEEL,
            xform=Matrix.Rotation(math.radians(30), 4, 'Z') @ Matrix.Rotation(math.radians(90), 4, 'Y'))
    # local frame -> world: face (+Z) toward -Y, 12 o'clock (+Y) up (+Z), tilted up by TILT
    place = (Matrix.Translation((0, -LOOP_Y - STRAP_T / 2 - 0.0058, LOOP_C)) @ Matrix.Rotation(-TILT, 4, 'X') @
             Matrix.Rotation(math.radians(90), 4, 'X'))
    bmesh.ops.transform(bm, matrix=place, verts=list(bm.verts))
    parts.append(S.part("case", bm, mats))
    S.finish_exhibit("smartwatch", parts, a.out, a.record, {
        "script": "art/exhibits/smartwatch/author_smartwatch.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; the reference watch was not used (readable UI numbers, no strap loop)"},
        "materials": {"smartwatch_steel": "brushed steel metallic 1.0 sRGB (186,188,191) r0.30",
                      "smartwatch_glass": "dark glass (screen off, sensor window) sRGB (9,10,12) r0.05",
                      "smartwatch_strap": "slate-blue sport rubber sRGB (78,96,120) r0.68"},
        "textures": {}})


if __name__ == "__main__":
    main()
