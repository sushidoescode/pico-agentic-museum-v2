"""Author the Earth Collection streaming remote (exhibit 013): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/remote/author_remote.py -- --out <glb>

Slim charcoal remote 150 x 38 x 16 mm displayed LYING FLAT with its long axis across the viewer (X), the d-pad /
IR end toward -X (root's native study check: an end-on remote reads as a dark blob): rounded plan and a softly rounded underside, a satin d-pad ring with a centre select
button, four round function buttons, a volume rocker pill, a small muted-red power button and a dark IR window
at the far end. No icons, text or logo. Three materials, no textures.
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

BODY, KEYS, ACCENT = 0, 1, 2
HW, HL, T = 0.019, 0.075, 0.016


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("remote_body", ec.srgb(40, 42, 46), metallic=0.0, roughness=0.52),
            ec.pbr_material("remote_keys", ec.srgb(78, 80, 84), metallic=0.0, roughness=0.34),
            ec.pbr_material("remote_power", ec.srgb(150, 62, 54), metallic=0.0, roughness=0.40)]
    parts = []
    bm = bmesh.new()
    # body: long axis on Blender Y (+Y = far end, away from the viewer at -Y); soft underside, crisper top edge
    S.plan_loft(bm, S.rrect_fn(HW, HL, 0.014, 12), S.edge_profile(0.0, T, 0.0060, 0.0032, 5, 4), BODY)
    # IR window cap at the far end (+Y)
    S.plan_loft(bm, S.rrect_fn(0.0080, 0.0020, 0.0018, 4), S.edge_profile(0.0035, T - 0.0035, 0.0012, 0.0012, 2, 2), KEYS,
                xform=Matrix.Translation((0, HL - 0.0002, 0)))
    parts.append(S.part("body", bm, mats))
    top = T
    bm = bmesh.new()
    # d-pad ring and centre select
    dy = 0.040
    S.lathe(bm, [(0.0092, top - 0.0004), (0.0158, top - 0.0004), (0.0160, top + 0.0006), (0.0155, top + 0.0012),
                 (0.0098, top + 0.0012), (0.0092, top + 0.0006)], 64, KEYS, xform=Matrix.Translation((0, dy, 0)), caps=True)
    S.lathe(bm, [(0, top - 0.0004), (0.0078, top - 0.0004), (0.0078, top + 0.0008), (0.0072, top + 0.0014), (0, top + 0.0015)],
            48, KEYS, xform=Matrix.Translation((0, dy, 0)))
    # four round function buttons
    for x, y in ((-0.0085, 0.013), (0.0085, 0.013), (-0.0085, -0.004), (0.0085, -0.004)):
        S.lathe(bm, [(0, top - 0.0004), (0.0046, top - 0.0004), (0.0046, top + 0.0008), (0.0040, top + 0.0013), (0, top + 0.0014)],
                32, KEYS, xform=Matrix.Translation((x, y, 0)))
    # volume rocker pill
    S.plan_loft(bm, S.stadium_fn(0.0048, 0.0140, 10), S.edge_profile(top - 0.0004, top + 0.0013, 0.0, 0.0006, 0, 2), KEYS,
                xform=Matrix.Translation((0, -0.033, 0)))
    parts.append(S.part("keys", bm, mats))
    bm = bmesh.new()
    S.lathe(bm, [(0, top - 0.0004), (0.0030, top - 0.0004), (0.0030, top + 0.0007), (0.0025, top + 0.0011), (0, top + 0.0012)],
            24, ACCENT, xform=Matrix.Translation((0, 0.064, 0)))
    parts.append(S.part("power", bm, mats))
    # display orientation (root, native study check): lying flat with the long axis along X, d-pad end toward -X,
    # so the study/inspector front view (+Z) shows its full length instead of a foreshortened end
    rot = Matrix.Rotation(math.radians(90), 4, 'Z')
    for o in parts:
        o.data.transform(rot)
    S.finish_exhibit("remote", parts, a.out, a.record, {
        "script": "art/exhibits/remote/author_remote.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; the reference (garbled icons, chunky proportions) was not used"},
        "display": "lying flat, long axis along X, d-pad / IR end toward -X",
        "materials": {"remote_body": "charcoal matte plastic sRGB (40,42,46) r0.52",
                      "remote_keys": "dark grey satin keys sRGB (78,80,84) r0.34",
                      "remote_power": "muted red power key sRGB (150,62,54) r0.40"},
        "textures": {}})


if __name__ == "__main__":
    main()
