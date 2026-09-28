"""Author the Earth Collection smartphone (exhibit 009) from scratch in Blender and export the runtime GLB.

Usage:
  Blender -b --factory-startup --python art/exhibits/smartphone/author_smartphone.py -- \
      --out native-staging/app/src/main/assets/models/exhibits/smartphone.glb [--record art/exhibits/smartphone/build-record.json]

Original, generic design (no brand trade dress): rounded slab 150 x 72 x 8 mm standing upright on its bottom edge,
screen toward +Z (glTF), back toward -Z; dark neutral screen with a centred punch-hole camera and an earpiece slit
(texture), 2.5D front glass edge, satin aluminium frame, frosted sage back, a vertical pill camera island (two
lenses) at the upper corner and a flash dot, volume rocker and power key on the right side. Three materials; the
only texture is the 512 x 1024 front-glass map written by this script (art/exhibits/smartphone/textures/).
Everything is parametric and deterministic.
"""
import argparse
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools"))
import bpy  # noqa: E402
import bmesh  # noqa: E402
import numpy as np  # noqa: E402

import ec_blender as ec  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- dimensions (metres) ----
W, H, D = 0.0720, 0.1500, 0.0080
HW, HH = W / 2, H / 2
R_CORNER = 0.0095          # plan corner radius
R_EDGE = 0.0024            # rounded front/back edge radius
SEG = 32                   # segments per plan corner
NF = 6                     # segments per rounded edge
BEZEL = 0.0013
ISLAND = dict(u=0.0205, v=0.0505, hw=0.0060, hh=0.0135, h=0.0011, rb=0.0005)
LENS = dict(r_out=0.0042, r_in=0.0033, dv=0.0064, n=48)
FLASH = dict(u=0.0100, v=0.0569, r=0.0016)
BUTTONS = [dict(v=0.030, hl=0.011), dict(v=0.004, hl=0.0055)]  # volume rocker, power key (right side, +X)
TEX_W, TEX_H = 512, 1024

MAT_FRAME, MAT_GLASS, MAT_BACK = 0, 1, 2


def P(u, v, w):
    """phone-local (u across, v up from centre, w toward the screen) -> Blender (x, y, z), standing on z = 0."""
    return (u, -w, v + HH)


def body(bm):
    rings, ring_angles = [], []
    for i in range(NF + 1):
        a = (math.pi / 2) * i / NF
        inset, w = R_EDGE * (1 - math.sin(a)), D / 2 - R_EDGE * (1 - math.cos(a))
        rings.append([P(x, y, w) for x, y in ec.rounded_rect(HW - inset, HH - inset, R_CORNER - inset, SEG)])
    for i in range(NF + 1):
        a = (math.pi / 2) * (NF - i) / NF
        inset, w = R_EDGE * (1 - math.sin(a)), -(D / 2 - R_EDGE * (1 - math.cos(a)))
        rings.append([P(x, y, w) for x, y in ec.rounded_rect(HW - inset, HH - inset, R_CORNER - inset, SEG)])

    def band_mat(k):
        if k < NF:   # front rounded edge: glass while the edge is within 50 deg of the face
            return MAT_GLASS if 90.0 * (k + 0.5) / NF < 50 else MAT_FRAME
        if k == NF:
            return MAT_FRAME
        j = k - NF - 1
        return MAT_BACK if 90.0 * (NF - j - 0.5) / NF < 50 else MAT_FRAME
    faces, cs, ce = ec.loft_rings(bm, rings, mat_for_band=band_mat, cap_mats=(MAT_GLASS, MAT_BACK))
    for e in cs.edges:
        e.smooth = False
    for e in ce.edges:
        e.smooth = False


def island(bm):
    I = ISLAND
    w0 = -D / 2
    rings = [[P(I["u"] + x, I["v"] + y, w0 + 0.0003) for x, y in ec.stadium(I["hw"], I["hh"], 16)]]
    nb = 3
    for j in range(nb + 1):
        a = (math.pi / 2) * j / nb
        inset = I["rb"] * (1 - math.cos(a))
        w = w0 - I["h"] + I["rb"] * (1 - math.sin(a))
        rings.append([P(I["u"] + x, I["v"] + y, w) for x, y in ec.stadium(I["hw"] - inset, I["hh"] - inset, 16)])
    _, cs, ce = ec.loft_rings(bm, rings, mat_for_band=lambda k: MAT_FRAME, cap_mats=(MAT_FRAME, MAT_FRAME))
    for e in ce.edges:
        e.smooth = False


def lens(bm, u, v):
    L = LENS
    w_top = -D / 2 - ISLAND["h"]
    prof = [(L["r_out"], w_top + 0.0002), (L["r_out"], w_top - 0.0004), (L["r_out"] - 0.00015, w_top - 0.00065),
            (L["r_out"] - 0.0004, w_top - 0.0007), (L["r_in"], w_top - 0.0007), (L["r_in"], w_top - 0.0003)]
    rings = [[P(u + x, v + y, w) for x, y in ec.circle(r, L["n"])] for r, w in prof]
    _, cs, ce = ec.loft_rings(bm, rings, mat_for_band=lambda k: MAT_FRAME, cap_mats=(MAT_FRAME, MAT_GLASS))
    for e in ce.edges:
        e.smooth = False
    return ce


def flash(bm):
    F = FLASH
    w0 = -D / 2
    prof = [(F["r"], w0 + 0.0002), (F["r"], w0 - 0.0001), (F["r"] - 0.0002, w0 - 0.0002)]
    rings = [[P(F["u"] + x, F["v"] + y, w) for x, y in ec.circle(r, 24)] for r, w in prof]
    _, cs, ce = ec.loft_rings(bm, rings, mat_for_band=lambda k: MAT_FRAME, cap_mats=(MAT_FRAME, MAT_FRAME))
    for e in ce.edges:
        e.smooth = False


def button(bm, v, hl):
    hw_w = 0.0010  # half thickness along w
    prof = [(HW - 0.0003, 0.0), (HW + 0.00045, 0.0), (HW + 0.0006, 0.00015)]
    rings = []
    for u, inset in prof:
        rings.append([(u, -(a), v + b + HH) for a, b in ec.stadium(hw_w - inset, hl - inset, 12)])
    _, cs, ce = ec.loft_rings(bm, rings, mat_for_band=lambda k: MAT_FRAME, cap_mats=(MAT_FRAME, MAT_FRAME))
    for e in ce.edges:
        e.smooth = False


def rr_sdf(x, y, hw, hh, r):
    qx = np.abs(x) - (hw - r)
    qy = np.abs(y) - (hh - r)
    outside = np.hypot(np.maximum(qx, 0), np.maximum(qy, 0))
    inside = np.minimum(np.maximum(qx, qy), 0)
    return outside + inside - r


def screen_texture(path):
    px = (np.arange(TEX_W) + 0.5) / TEX_W
    py = (np.arange(TEX_H) + 0.5) / TEX_H
    X = (px[None, :] * 2 - 1) * HW                    # metres across
    Y = (1 - py[:, None] * 2) * HH                    # metres up; row 0 = top of the phone
    X = np.broadcast_to(X, (TEX_H, TEX_W))
    Y = np.broadcast_to(Y, (TEX_H, TEX_W))
    pix = W / TEX_W
    bezel = np.array([7, 7, 8], float)
    top_c = np.array([38, 42, 50], float)
    bot_c = np.array([17, 19, 23], float)
    t = ((Y / HH) + 1) / 2
    screen = bot_c[None, None, :] * (1 - t[..., None]) + top_c[None, None, :] * t[..., None]
    d = rr_sdf(X, Y, HW - BEZEL, HH - BEZEL, R_CORNER - BEZEL)
    a = np.clip(0.5 - d / pix, 0, 1)[..., None]
    img = bezel * (1 - a) + screen * a
    # punch-hole camera: dark disc with a faint rim
    cy = HH - 0.0052
    rr = np.hypot(X, Y - cy)
    rim = np.clip(0.5 - (rr - 0.0019) / pix, 0, 1)[..., None]
    img = img * (1 - rim) + np.array([30, 32, 36], float) * rim
    hole = np.clip(0.5 - (rr - 0.0015) / pix, 0, 1)[..., None]
    img = img * (1 - hole) + np.array([3, 3, 4], float) * hole
    # earpiece slit in the top bezel
    slit = rr_sdf(X, Y - (HH - BEZEL / 2), 0.005, 0.00028, 0.00028)
    s = np.clip(0.5 - slit / pix, 0, 1)[..., None]
    img = img * (1 - s) + np.array([20, 20, 22], float) * s
    # 4x4 ordered dither against banding in the dark gradient
    bayer = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], float) + 0.5) / 16 - 0.5
    dither = np.tile(bayer, (TEX_H // 4, TEX_W // 4))[..., None]
    img = np.clip(np.floor(img + 0.5 + dither), 0, 255).astype(np.uint8)
    ec.write_png_rgb(path, TEX_W, TEX_H, [img[r].tobytes() for r in range(TEX_H)])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--record", default=os.path.join(HERE, "build-record.json"))
    a = ap.parse_args(ec.script_args())
    ec.reset_scene()

    tex_path = os.path.join(HERE, "textures", "smartphone_front_glass.png")
    screen_texture(tex_path)
    glass_img = ec.load_image(tex_path, 'sRGB', name="smartphone_front_glass")
    mats = [
        ec.pbr_material("smartphone_frame", ec.srgb(178, 182, 186), metallic=1.0, roughness=0.32),
        ec.pbr_material("smartphone_glass", (1, 1, 1, 1), metallic=0.0, roughness=0.06, base_image=glass_img),
        ec.pbr_material("smartphone_back", ec.srgb(118, 138, 130), metallic=0.0, roughness=0.45),
    ]

    parts = []
    for build in ("body", "island", "lenses", "flash", "buttons"):
        bm = bmesh.new()
        if build == "body":
            body(bm)
        elif build == "island":
            island(bm)
        elif build == "lenses":
            for dv in (-LENS["dv"], LENS["dv"]):
                lens(bm, ISLAND["u"], ISLAND["v"] + dv)
        elif build == "flash":
            flash(bm)
        else:
            for b in BUTTONS:
                button(bm, b["v"], b["hl"])
        bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
        for f in bm.faces:
            f.smooth = True
        # UVs: planar front projection (the glass texture covers the front); lens glass maps to the black bezel
        uv = bm.loops.layers.uv.new("UVMap")
        for f in bm.faces:
            for loop in f.loops:
                co = loop.vert.co
                if build == "lenses" and f.material_index == MAT_GLASS:
                    loop[uv].uv = (0.004, 0.004)
                else:
                    loop[uv].uv = ((co.x + HW) / W, (co.z) / H)
        obj = ec.new_mesh_object(f"smartphone_{build}", bm, mats)
        ec.select_only(obj)
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(35), keep_sharp_edges=True)
        parts.append(obj)

    ec.select_only(parts)
    bpy.ops.object.join()
    obj = bpy.context.view_layer.objects.active
    obj.name = obj.data.name = "smartphone"
    bm = bmesh.new(); bm.from_mesh(obj.data)
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 3])
    bm.to_mesh(obj.data); bm.free()
    ec.ground_and_centre([obj])
    out = os.path.abspath(a.out)
    ec.export_glb([obj], out)
    info = ec.inspect_glb(out)
    ec.write_json(a.record, {
        "id": "smartphone",
        "script": "art/exhibits/smartphone/author_smartphone.py",
        "args": {"out": ec.rel(out)},
        "source": {"kind": "blender-original", "note": "authored from scratch from the written brief; no reference model, image or provider"},
        "parameters": {"W": W, "H": H, "D": D, "cornerRadius": R_CORNER, "edgeRadius": R_EDGE, "bezel": BEZEL,
                       "island": ISLAND, "lens": LENS, "flash": FLASH, "buttons": BUTTONS},
        "materials": {"smartphone_frame": "metallic 1.0, roughness 0.32, sRGB (178,182,186) satin aluminium",
                      "smartphone_glass": "metallic 0.0, roughness 0.06, base colour = front-glass texture (dark neutral screen)",
                      "smartphone_back": "metallic 0.0, roughness 0.45, sRGB (118,138,130) frosted sage"},
        "textures": {"smartphone_front_glass.png": {"path": ec.rel(tex_path), "size": [TEX_W, TEX_H],
                                                    "sha256": ec.sha256_file(tex_path), "bytes": os.path.getsize(tex_path)}},
        "versions": ec.versions(),
        "exporterSettings": ec.EXPORT_SETTINGS,
        "exporterSettingsSha256": ec.export_settings_digest(),
        "output": {"path": ec.rel(out), "sha256": info["sha256"], "bytes": info["bytes"],
                   "triangles": info["triangles"], "boundsMetres": info["boundsMetres"],
                   "extensionsUsed": info["extensionsUsed"]},
    })
    print("AUTHOR_RESULT", info["sha256"], info["bytes"], info["triangles"], info["boundsMetres"])


if __name__ == "__main__":
    main()
