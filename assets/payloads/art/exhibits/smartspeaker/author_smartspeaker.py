"""Author the Earth Collection smart speaker (exhibit 003): Blender original, generic design.

Usage: Blender -b --factory-startup --python art/exhibits/smartspeaker/author_smartspeaker.py -- --out <glb>

Straight fabric-wrapped cylinder 140 mm across, 169 mm tall, rounded shoulders, a solid graphite top cap with a
concentric dial groove and a single status dot at the front, black rubber foot. Deliberately not the HomePod pill
or the Echo light-ring/four-button top. Fabric = tiling 256 px plain-weave base colour + normal map written by this
script (art/exhibits/smartspeaker/textures/). Three materials.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "tools"))
import bmesh  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Matrix  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as S  # noqa: E402

FABRIC, CAP, FOOT = 0, 1, 2
TILE_M = 0.02  # one texture tile covers 20 mm of fabric
N = 256


def fabric_textures(tex_dir):
    u = (np.arange(N) + 0.5) / N
    U, V = np.meshgrid(u, u)
    threads = 20
    iu, iv = np.floor(U * threads), np.floor(V * threads)
    fu, fv = U * threads - iu, V * threads - iv
    warp_on_top = ((iu + iv) % 2) == 0
    warp = np.cos(np.pi * (fu - 0.5)) ** 0.6 * (0.55 + 0.45 * np.cos(np.pi * (fv - 0.5)))
    weft = np.cos(np.pi * (fv - 0.5)) ** 0.6 * (0.55 + 0.45 * np.cos(np.pi * (fu - 0.5)))
    h = np.where(warp_on_top, np.maximum(warp, 0.6 * weft), np.maximum(weft, 0.6 * warp))
    h = h + 0.15 * S.fbm(N, N, 4, 3, seed=31)
    normal = S.height_to_normal(h, 3.0)
    mel = S.fbm(N, N, 8, 3, seed=7)
    base = np.array([150, 145, 138], float) / 255.0
    shade = 0.80 + 0.20 * h + 0.10 * (mel - 0.5)
    col = base[None, None, :] * shade[..., None]
    bc = S.save_jpg(os.path.join(tex_dir, "smartspeaker_fabric_basecolor.jpg"), S.to_u8(col), 90)
    nm = S.save_png(os.path.join(tex_dir, "smartspeaker_fabric_normal.png"), normal)
    return bc, nm


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()
    tex_dir = os.path.join(HERE, "textures")
    bc_path, nm_path = fabric_textures(tex_dir)
    mats = [ec.pbr_material("smartspeaker_fabric", (1, 1, 1, 1), metallic=0.0, roughness=0.88,
                            base_image=ec.load_image(bc_path, 'sRGB', "smartspeaker_fabric_basecolor"),
                            normal_image=ec.load_image(nm_path, 'Non-Color', "smartspeaker_fabric_normal"),
                            normal_strength=0.8),
            ec.pbr_material("smartspeaker_cap", ec.srgb(60, 62, 66), metallic=0.0, roughness=0.45),
            ec.pbr_material("smartspeaker_foot", ec.srgb(22, 22, 23), metallic=0.0, roughness=0.75)]
    prof = [(0, 0.0), (0.056, 0.0), (0.058, 0.002), (0.058, 0.006), (0.062, 0.007), (0.066, 0.010),
            (0.069, 0.016), (0.070, 0.024), (0.070, 0.148), (0.0695, 0.153), (0.068, 0.157), (0.0655, 0.160),
            (0.064, 0.1615), (0.062, 0.1645), (0.058, 0.1665), (0.050, 0.1675), (0.042, 0.1678),
            (0.0405, 0.1668), (0.0385, 0.1668), (0.037, 0.1680), (0.012, 0.1685), (0, 0.1686)]
    em = []
    for i in range(len(prof) - 1):
        z = max(prof[i][1], prof[i + 1][1])
        em.append(FOOT if z <= 0.0065 else FABRIC if prof[i + 1][1] <= 0.1601 else CAP)
    bm = bmesh.new()
    circ = 2 * math.pi * 0.070
    S.lathe(bm, prof, 128, em, uv={"mode": "cyl", "u_scale": round(circ / TILE_M), "v": "z", "z0": 0.0,
                                   "z1": TILE_M, "v_scale": 1.0}, sharp_edges=(12,))
    parts = [S.part("body", bm, mats)]
    bm = bmesh.new()
    S.lathe(bm, [(0, 0.1680), (0.0028, 0.1680), (0.0028, 0.1692), (0, 0.1694)], 20, FOOT,
            xform=Matrix.Translation((0, -0.026, 0)))
    parts.append(S.part("status_dot", bm, mats))
    S.finish_exhibit("smartspeaker", parts, a.out, a.record, {
        "script": "art/exhibits/smartspeaker/author_smartspeaker.py", "args": {"out": ec.rel(os.path.abspath(a.out))},
        "source": {"kind": "blender-original", "note": "authored from the written brief; no reference model, image or provider"},
        "materials": {"smartspeaker_fabric": "plain-weave fabric texture (stone grey sRGB 150,145,138), r0.88, normal 0.8",
                      "smartspeaker_cap": "graphite matte plastic sRGB (60,62,66) r0.45",
                      "smartspeaker_foot": "black rubber sRGB (22,22,23) r0.75"},
        "textures": {os.path.basename(p): {"path": ec.rel(p), "sha256": ec.sha256_file(p), "bytes": os.path.getsize(p)}
                     for p in (bc_path, nm_path)},
        "uv": f"fabric tiles every {TILE_M} m (repeat sampler)"})


if __name__ == "__main__":
    main()
