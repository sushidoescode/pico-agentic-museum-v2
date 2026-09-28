"""Author the Earth Collection Wi-Fi router (exhibit 014): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/router/author_router.py -- --out <glb>

Matte charcoal body 230 x 140 x 36 mm on four small feet, a field of recessed vent slots on the top, a glossy
front strip with six small pale status lights (unlit), and four flat paddle antennas on hinge knuckles along the
back edge, splayed slightly (overall height 0.18 m). No text, labels or logo. Three materials, no textures.
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

BODY, DARK, LIGHT = 0, 1, 2
HW, HD, H = 0.115, 0.070, 0.036
FEET = 0.004


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("router_body", ec.srgb(50, 52, 56), metallic=0.0, roughness=0.56),
            ec.pbr_material("router_dark", ec.srgb(18, 18, 20), metallic=0.0, roughness=0.30),
            ec.pbr_material("router_light", ec.srgb(170, 210, 196), metallic=0.0, roughness=0.30)]
    parts = []
    bm = bmesh.new()
    top = FEET + H
    S.plan_loft(bm, S.rrect_fn(HW, HD, 0.012, 8), S.edge_profile(FEET, top, 0.004, 0.007, 3, 4, taper_top=0.002), BODY)
    for sx in (-1, 1):
        for sy in (-1, 1):
            S.lathe(bm, [(0, 0.0), (0.009, 0.0), (0.010, 0.001), (0.010, FEET + 0.001), (0, FEET + 0.001)], 20, DARK,
                    xform=Matrix.Translation((sx * (HW - 0.022), sy * (HD - 0.018), 0)))
    parts.append(S.part("body", bm, mats))
    # vent slots on top (shallow dark pills, flush-proud 0.3 mm), 5 rows x 9
    bm = bmesh.new()
    zt = top - 0.0003
    for r in range(5):
        for c in range(9):
            x = -0.072 + c * 0.018
            y = -0.030 + r * 0.013
            S.plan_loft(bm, S.stadium_fn(0.0060, 0.0018, 6), [(0.0, zt - 0.0005), (0.0, zt + 0.0006)], DARK,
                        xform=Matrix.Translation((x, y, 0)))
    # front glossy strip with six status lights
    S.plan_loft(bm, S.rrect_fn(0.060, 0.0006, 0.0005, 2), [(0.0, FEET + 0.016), (0.0, FEET + 0.024)], DARK,
                xform=Matrix.Translation((0, -HD - 0.0002, 0)))
    for i in range(6):
        x = -0.045 + i * 0.018
        S.lathe(bm, [(0, 0.0), (0.0016, 0.0), (0.0016, 0.0006), (0, 0.0008)], 16, LIGHT,
                xform=Matrix.Translation((x, -HD - 0.0007, FEET + 0.020)) @ Matrix.Rotation(math.radians(90), 4, 'X'))
    parts.append(S.part("details", bm, mats))
    # four paddle antennas on knuckles along the back edge
    bm = bmesh.new()
    for x, splay in ((-0.085, -10), (-0.030, -3), (0.030, 3), (0.085, 10)):
        base = Vector((x, HD - 0.004, top - 0.006))
        S.lathe(bm, [(0, -0.009), (0.0055, -0.009), (0.0055, 0.009), (0, 0.009)], 20, DARK,
                xform=Matrix.Translation(base) @ Matrix.Rotation(math.radians(90), 4, 'Y'))
        paddle = S.plan_loft(bm, S.rrect_fn(0.0085, 0.0028, 0.0026, 5), S.edge_profile(0.0, 0.138, 0.0, 0.006, 0, 5), BODY)
        xf = (Matrix.Translation(base) @ Matrix.Rotation(math.radians(splay), 4, 'Y') @
              Matrix.Rotation(math.radians(-6), 4, 'X'))
        bmesh.ops.transform(bm, matrix=xf, verts=list({v for f in paddle for v in f.verts}))
    parts.append(S.part("antennas", bm, mats))
    S.finish_exhibit("router", parts, a.out, a.record, {
        "script": "art/exhibits/router/author_router.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; the reference (lumpy, '5G' label) was not used"},
        "materials": {"router_body": "matte charcoal plastic sRGB (50,52,56) r0.56",
                      "router_dark": "gloss black (vents, strip, feet, knuckles) sRGB (18,18,20) r0.30",
                      "router_light": "pale status lights, unlit sRGB (170,210,196) r0.30"},
        "textures": {}})


if __name__ == "__main__":
    main()
