"""Author the Earth Collection wireless earbuds and case (exhibit 010): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/earbuds/author_earbuds.py -- --out <glb>

A terracotta pebble case (60 x 36 mm, 30 mm closed) lying flat with its lid hinged at the back and opened ~105
degrees, a charcoal interior insert holding two stemless charcoal buds with pale touch surfaces, a satin hinge
barrel and a small front status dot. Deliberately not the white flip-top case with white stem buds (AirPods trade
dress). Three materials, no textures.
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

CASE, INSIDE, ACCENT = 0, 1, 2
HW, HD, RC = 0.030, 0.018, 0.016
SPLIT = 0.018
OPEN_DEG = 105.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("earbuds_case", ec.srgb(184, 104, 86), metallic=0.0, roughness=0.42),
            ec.pbr_material("earbuds_charcoal", ec.srgb(38, 40, 44), metallic=0.0, roughness=0.30),
            ec.pbr_material("earbuds_accent", ec.srgb(200, 200, 198), metallic=0.0, roughness=0.38)]
    outline = S.rrect_fn(HW, HD, RC, 20)
    parts = []
    # tray: pebble-rounded bottom, straight wall, case-coloured rim, recessed charcoal insert
    bm = bmesh.new()
    prof = [(-0.0060, 0.0), (-0.0035, 0.0006), (-0.0018, 0.0022), (-0.0007, 0.0045), (-0.0001, 0.0070),
            (0.0, 0.0090), (0.0, SPLIT), (-0.0015, SPLIT), (-0.0015, SPLIT - 0.0008)]
    em = [CASE] * 7 + [INSIDE]
    S.plan_loft(bm, outline, prof, em, cap_mats=(CASE, INSIDE))
    parts.append(S.part("tray", bm, mats))
    # lid (built closed, then opened about the hinge line at the back edge of the split plane)
    bm = bmesh.new()
    lprof = [(0.0, SPLIT + 0.0003), (0.0, 0.0220), (-0.0006, 0.0245), (-0.0020, 0.0270), (-0.0045, 0.0290),
             (-0.0080, 0.0302), (-0.0110, 0.0306)]
    S.plan_loft(bm, outline, lprof, CASE, cap_mats=(INSIDE, CASE))
    hinge = Vector((0, HD - 0.0005, SPLIT + 0.0002))
    rot = Matrix.Translation(hinge) @ Matrix.Rotation(math.radians(-OPEN_DEG), 4, 'X') @ Matrix.Translation(-hinge)
    bmesh.ops.transform(bm, matrix=rot, verts=list(bm.verts))
    parts.append(S.part("lid", bm, mats))
    # hinge barrel
    bm = bmesh.new()
    S.lathe(bm, [(0, -0.009), (0.0022, -0.009), (0.0022, 0.009), (0, 0.009)], 16, ACCENT,
            xform=Matrix.Translation(hinge + Vector((0, 0.0012, 0))) @ Matrix.Rotation(math.radians(90), 4, 'Y'))
    parts.append(S.part("hinge", bm, mats))
    # buds: stemless beans half-seated in the insert, pale touch surface on top, tilted slightly outward
    bm = bmesh.new()
    n = 20
    bprof = []
    for i in range(n + 1):
        ph = math.pi * i / n
        bprof.append((math.sin(ph), -math.cos(ph)))  # unit sphere bottom -> top (CCW)
    em_b = [INSIDE if (math.pi * (i + 0.5) / n) < math.radians(150) else ACCENT for i in range(n)]
    for sx in (-1, 1):
        xf = (Matrix.Translation((sx * 0.0135, -0.0010, SPLIT - 0.0006)) @
              Matrix.Rotation(math.radians(-sx * 14), 4, 'Y') @ Matrix.Rotation(math.radians(sx * 12), 4, 'Z') @
              Matrix.Diagonal((0.0100, 0.0085, 0.0074, 1.0)))
        S.lathe(bm, bprof, 48, em_b, xform=xf)
    parts.append(S.part("buds", bm, mats))
    # front status dot
    bm = bmesh.new()
    S.lathe(bm, [(0, 0.0), (0.0011, 0.0), (0.0011, 0.0004), (0, 0.0005)], 16, ACCENT,
            xform=Matrix.Translation((0, -HD + 0.0001, 0.0125)) @ Matrix.Rotation(math.radians(90), 4, 'X'))
    parts.append(S.part("status", bm, mats))
    S.finish_exhibit("earbuds", parts, a.out, a.record, {
        "script": "art/exhibits/earbuds/author_earbuds.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "lidOpenDeg": OPEN_DEG,
        "materials": {"earbuds_case": "terracotta satin plastic sRGB (184,104,86) r0.42",
                      "earbuds_charcoal": "charcoal gloss sRGB (38,40,44) r0.30 (insert, lid underside, buds)",
                      "earbuds_accent": "pale satin sRGB (200,200,198) r0.38 (touch surfaces, hinge, status dot)"},
        "textures": {}})


if __name__ == "__main__":
    main()
