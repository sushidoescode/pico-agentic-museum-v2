"""Author the Earth Collection doorbell camera (exhibit 015): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/doorbell/author_doorbell.py -- --out <glb>

Upright rounded-rectangle unit 130 x 46 x 25 mm (standing on its lower end; the room supplies the small wall-mount
stand), satin graphite body with softly rounded front edges, a black glass upper panel holding a round camera lens
(ringed) and a small sensor dot, and a round press button with a pale (unlit, not blue) ring near the bottom.
No wordmark, logo or text; not the Ring faceplate. Three materials, no textures.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
from mathutils import Matrix  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

BODY, GLASS, ACCENT = 0, 1, 2
HW, HH, D = 0.023, 0.065, 0.025

# local build frame: x across, y up the unit, z out of the front face; UP maps local -> Blender (x, -z, y)
UP = Matrix.Rotation(math.radians(90), 4, 'X')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("doorbell_body", ec.srgb(72, 74, 78), metallic=0.0, roughness=0.40),
            ec.pbr_material("doorbell_glass", ec.srgb(10, 11, 13), metallic=0.0, roughness=0.06),
            ec.pbr_material("doorbell_accent", ec.srgb(206, 206, 202), metallic=0.0, roughness=0.34)]
    parts = []
    bm = bmesh.new()
    S.plan_loft(bm, S.rrect_fn(HW, HH, 0.015, 12), S.edge_profile(0.0, D, 0.0015, 0.0065, 2, 6), BODY,
                xform=UP @ Matrix.Translation((0, HH, 0)))
    parts.append(S.part("body", bm, mats))
    bm = bmesh.new()
    front = D
    # black glass upper panel (inset from the rounded front edge), camera lens assembly, sensor dot
    pc = HH + 0.030
    S.plan_loft(bm, S.rrect_fn(HW - 0.0068, 0.026, 0.009, 10), [(0.0, front - 0.0012), (0.0, front + 0.0004)], GLASS,
                xform=UP @ Matrix.Translation((0, pc, 0)))
    lens_y = HH + 0.042
    S.lathe(bm, [(0, front - 0.0010), (0.0092, front - 0.0010), (0.0092, front + 0.0012), (0.0086, front + 0.0016),
                 (0.0068, front + 0.0016), (0.0066, front + 0.0008), (0, front + 0.0006)], 48,
            [ACCENT, ACCENT, ACCENT, ACCENT, ACCENT, GLASS], xform=UP @ Matrix.Translation((0, lens_y, 0)))
    S.lathe(bm, [(0, front - 0.0005), (0.0014, front - 0.0005), (0.0014, front + 0.0007), (0, front + 0.0008)], 16,
            ACCENT, xform=UP @ Matrix.Translation((0, HH + 0.018, 0)))
    # press button near the bottom: pale ring + graphite centre
    by = HH - 0.040
    S.lathe(bm, [(0.0080, front - 0.0010), (0.0118, front - 0.0010), (0.0118, front + 0.0008), (0.0112, front + 0.0012),
                 (0.0086, front + 0.0012), (0.0080, front + 0.0008)], 64, ACCENT, xform=UP @ Matrix.Translation((0, by, 0)))
    S.lathe(bm, [(0, front - 0.0010), (0.0078, front - 0.0010), (0.0078, front + 0.0014), (0.0070, front + 0.0021),
                 (0, front + 0.0024)], 48, BODY, xform=UP @ Matrix.Translation((0, by, 0)))
    parts.append(S.part("front", bm, mats))
    S.finish_exhibit("doorbell", parts, a.out, a.record, {
        "script": "art/exhibits/doorbell/author_doorbell.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; the reference ('ring' wordmark, smeary bezel) was not used"},
        "materials": {"doorbell_body": "satin graphite plastic sRGB (72,74,78) r0.40",
                      "doorbell_glass": "black glass panel / lens sRGB (10,11,13) r0.06",
                      "doorbell_accent": "pale satin ring, lens bezel, sensor sRGB (206,206,202) r0.34"},
        "textures": {}})


if __name__ == "__main__":
    main()
