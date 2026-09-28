"""Author the Earth Collection coffee pod (exhibit 004): Blender original, generic single-serve capsule.

Usage: Blender -b --factory-startup --python art/exhibits/coffeepod/author_coffeepod.py -- --out <glb>

A 40 mm x 30 mm aluminium capsule standing on its sealed foil face: rolled flange, conical body with a stepped
shoulder ring and a small flat crown, copper anodised finish; the silver foil shows as a thin band under the flange.
Generic capsule form (conical body + flange); no roundel, logo, text or embossed mark (the reference pod's
roundel is not used). Two materials, no textures.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

CUP, FOIL = 0, 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    mats = [ec.pbr_material("coffeepod_anodised", ec.srgb(176, 98, 62), metallic=1.0, roughness=0.30),
            ec.pbr_material("coffeepod_foil", ec.srgb(206, 204, 198), metallic=1.0, roughness=0.36)]
    # profile CCW: foil face (bottom) -> flange -> conical body -> shoulder step -> crown
    prof = [(0, 0.0), (0.0192, 0.0), (0.0199, 0.0003), (0.0200, 0.0007),       # foil disc and its edge
            (0.0200, 0.0011), (0.0197, 0.0017), (0.0190, 0.0019),               # rolled flange rim
            (0.0158, 0.0019), (0.0153, 0.0025), (0.0150, 0.0040),               # flange -> body fillet
            (0.0140, 0.0090), (0.0129, 0.0140), (0.0118, 0.0190), (0.0110, 0.0222),
            (0.0107, 0.0228), (0.0100, 0.0230), (0.0097, 0.0236),               # shoulder step ring
            (0.0086, 0.0262), (0.0075, 0.0283), (0.0066, 0.0294), (0.0058, 0.0299),
            (0.0040, 0.0300), (0, 0.0300)]
    em = [FOIL] * 3 + [CUP] * (len(prof) - 1 - 3)
    bm = bmesh.new()
    S.lathe(bm, prof, 128, em, sharp_edges=(3,))
    parts = [S.part("capsule", bm, mats, smooth_angle=40)]
    S.finish_exhibit("coffeepod", parts, a.out, a.record, {
        "script": "art/exhibits/coffeepod/author_coffeepod.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "materials": {"coffeepod_anodised": "copper anodised aluminium metallic 1.0 sRGB (176,98,62) r0.30",
                      "coffeepod_foil": "silver foil metallic 1.0 sRGB (206,204,198) r0.36"},
        "textures": {}})


if __name__ == "__main__":
    main()
