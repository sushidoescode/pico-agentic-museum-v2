"""Author the Earth Collection hardware crypto wallet (exhibit 012): Blender original, generic USB-stick form.

Usage: Blender -b --factory-startup --python art/exhibits/cryptowallet/author_cryptowallet.py -- --out <glb>

A matte black rounded stick 60 x 18 x 9 mm lying flat, long axis across the viewer (X): a small dark glass
screen (off, no text) and two round buttons on top, a brushed-steel collar and USB-C plug at the right end, a
lanyard hole at the left end. No swivel cover (not the Ledger Nano trade dress), no text or logo.
Three materials, no textures.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
from mathutils import Matrix  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

BODY, STEEL, GLASS = 0, 1, 2
L, W, T = 0.052, 0.018, 0.009   # body without the plug


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("cryptowallet_body", ec.srgb(30, 31, 33), metallic=0.0, roughness=0.58),
            ec.pbr_material("cryptowallet_steel", ec.srgb(190, 192, 195), metallic=1.0, roughness=0.32),
            ec.pbr_material("cryptowallet_glass", ec.srgb(8, 9, 11), metallic=0.0, roughness=0.06)]
    parts = []
    bm = bmesh.new()
    # body: rounded plan, rounded top and bottom edges
    S.plan_loft(bm, S.rrect_fn(L / 2, W / 2, 0.0045, 16), S.edge_profile(0.0, T, 0.0022, 0.0028, 6, 6), BODY,
                xform=Matrix.Translation((-0.004, 0, 0)))
    # steel eyelet ring on top at the left end (lanyard point)
    S.lathe(bm, [(0.0012, T - 0.0003), (0.0024, T - 0.0003), (0.0024, T + 0.0004), (0.0019, T + 0.0006),
                 (0.0012, T + 0.0004)], 24, STEEL, xform=Matrix.Translation((-L / 2 - 0.0005, 0, 0)))
    parts.append(S.part("body", bm, mats))
    # screen window and two buttons on top
    bm = bmesh.new()
    S.plan_loft(bm, S.rrect_fn(0.0110, 0.0045, 0.0012, 4), [(0.0, T - 0.0006), (0.0, T + 0.00015)], GLASS,
                xform=Matrix.Translation((-0.004, 0, 0)))
    for x in (-0.0192, 0.0112):
        S.lathe(bm, [(0, T - 0.0004), (0.0026, T - 0.0004), (0.0026, T + 0.0003), (0.0020, T + 0.0006), (0, T + 0.0007)],
                24, STEEL, xform=Matrix.Translation((x, 0, 0)))
    parts.append(S.part("top", bm, mats))
    # steel collar + USB-C plug at +X
    bm = bmesh.new()
    S.plan_loft(bm, S.rrect_fn(0.0030, W / 2 + 0.0002, 0.0030, 6), S.edge_profile(-0.0002, T + 0.0002, 0.0025, 0.0028, 3, 3),
                STEEL, xform=Matrix.Translation((L / 2 - 0.004 - 0.0025, 0, 0)))
    S.plan_loft(bm, S.stadium_fn(0.0036, 0.00125, 8), [(0.0, 0.0), (0.0, 0.0065)], STEEL,
                xform=Matrix.Translation((L / 2 - 0.0035, 0, T / 2)) @ Matrix.Rotation(1.5707963, 4, 'Y') @
                Matrix.Rotation(1.5707963, 4, 'Z'))
    parts.append(S.part("plug", bm, mats))
    S.finish_exhibit("cryptowallet", parts, a.out, a.record, {
        "script": "art/exhibits/cryptowallet/author_cryptowallet.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; the reference (garbled screen text, noisy bezel) was not used"},
        "materials": {"cryptowallet_body": "matte black plastic sRGB (30,31,33) r0.58",
                      "cryptowallet_steel": "brushed steel metallic 1.0 sRGB (190,192,195) r0.32",
                      "cryptowallet_glass": "dark screen glass (off) sRGB (8,9,11) r0.06"},
        "textures": {}})


if __name__ == "__main__":
    main()
