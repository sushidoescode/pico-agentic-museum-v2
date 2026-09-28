"""Author the Earth Collection robot vacuum (exhibit 002): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/vacuum/author_vacuum.py -- --out <glb> [--record <json>]

Disc robot 340 mm across, ~92 mm tall with its laser turret; front (+Z glTF / -Y Blender) carries a matte black
bumper arc; pale gloss top shell inside a graphite rim and groove, graphite turret with a dark window band and a pale ring,
two graphite buttons ahead of the turret, two pale three-arm side brushes peeking out at the front corners.
No marks, text or brand trade dress. Three materials, no textures.
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

SHELL, GRAPHITE, BLACK = 0, 1, 2
R = 0.170


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("vacuum_shell", ec.srgb(222, 222, 218), metallic=0.0, roughness=0.30),
            ec.pbr_material("vacuum_graphite", ec.srgb(62, 64, 68), metallic=0.0, roughness=0.36),
            ec.pbr_material("vacuum_black", ec.srgb(24, 25, 27), metallic=0.0, roughness=0.55)]
    parts = []

    # body
    bm = bmesh.new()
    prof = [(0, 0.004), (0.150, 0.004), (0.160, 0.006), (0.166, 0.011), (0.169, 0.018), (R, 0.024),
            (R, 0.060), (0.169, 0.066), (0.166, 0.071), (0.160, 0.075), (0.150, 0.077), (0.132, 0.0775),
            (0.130, 0.0765), (0.126, 0.0765), (0.124, 0.0778), (0.080, 0.0782), (0, 0.0785)]
    em = [BLACK] * 5 + [GRAPHITE] * 6 + [BLACK] * 3 + [SHELL] * 2
    S.lathe(bm, prof, 128, em, sharp_edges=(1,))
    parts.append(S.part("body", bm, mats))

    # front bumper arc (200 degrees centred on -Y), slightly proud of the body side
    bm = bmesh.new()
    prof2 = [(-0.004, 0.016), (0.0015, 0.016), (0.0035, 0.018), (0.0035, 0.060), (0.0015, 0.062), (-0.004, 0.062)]
    frames = []
    n = 72
    for i in range(n + 1):
        th = math.radians(-190 + 200 * i / n)
        radial = Vector((math.cos(th), math.sin(th), 0))
        frames.append((radial * R, radial, Vector((0, 0, 1))))
    S.sweep(bm, prof2, frames, BLACK)
    parts.append(S.part("bumper", bm, mats))

    # laser turret, 25 mm behind centre
    bm = bmesh.new()
    tprof = [(0, 0.0775), (0.043, 0.0775), (0.043, 0.080), (0.042, 0.0805), (0.042, 0.0875), (0.0425, 0.088),
             (0.0425, 0.0905), (0.041, 0.0918), (0.036, 0.0925), (0.030, 0.0927), (0, 0.0928)]
    tm = [GRAPHITE, GRAPHITE, GRAPHITE, BLACK, GRAPHITE, GRAPHITE, GRAPHITE, GRAPHITE, SHELL, GRAPHITE]
    S.lathe(bm, tprof, 96, tm, xform=Matrix.Translation((0, 0.025, 0)), sharp_edges=(3, 4))
    parts.append(S.part("turret", bm, mats))

    # two buttons ahead of the turret
    bm = bmesh.new()
    for x in (-0.016, 0.016):
        S.plan_loft(bm, S.stadium_fn(0.0065, 0.0035, 10), S.edge_profile(0.0772, 0.0796, 0.0, 0.0008, 0, 2),
                    GRAPHITE, xform=Matrix.Translation((x, -0.088, 0)))
    parts.append(S.part("buttons", bm, mats))

    # side brushes at the front corners: hub + three slightly swept arms
    bm = bmesh.new()
    for sx in (-1, 1):
        c = Vector((sx * 0.108, -0.106, 0.0))
        S.lathe(bm, [(0, 0.0045), (0.009, 0.0045), (0.009, 0.0085), (0, 0.0085)], 24, BLACK,
                xform=Matrix.Translation(c))
        for k in range(3):
            base = math.radians(k * 120 + (20 if sx > 0 else -20))
            pts = []
            for j in range(5):
                t = j / 4
                ang = base + sx * 0.35 * t
                rr = 0.004 + 0.058 * t
                pts.append(c + Vector((rr * math.cos(ang), rr * math.sin(ang), 0.0065 - 0.0035 * t)))
            S.tube(bm, pts, 0.0019, 6, SHELL)
    parts.append(S.part("brushes", bm, mats))

    S.finish_exhibit("vacuum", parts, a.out, a.record, {
        "script": "art/exhibits/vacuum/author_vacuum.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "materials": {"vacuum_shell": "gloss pale plastic sRGB (222,222,218) r0.30 (top shell, turret ring, brush arms)",
                      "vacuum_graphite": "gloss plastic sRGB (62,64,68) r0.36 (rim, turret, buttons)",
                      "vacuum_black": "matte sRGB (24,25,27) r0.55 (bumper, underside, window, hubs)"},
        "textures": {}})


if __name__ == "__main__":
    main()
