"""Parametric shape + texture kit for the Blender-original exhibits (headless Blender 5.1.2).

All geometry is built with bmesh in Blender coordinates (Z up, front = -Y; the exporter maps to glTF +Y up,
+Z front). Closed parts get consistent outward normals via recalc_face_normals. Every function is
deterministic (no randomness except seeded numpy generators in the texture helpers).

Geometry:
  lathe(bm, profile, seg, mats, ...)          solid of revolution about Z from a CCW (r, z) profile
  plan_loft(bm, outline_fn, profile, mats)    loft of plan outlines (rounded rects etc.) with an edge profile
  sweep(bm, profile2d, frames, edge_mats)     closed 2D profile swept along frames (origin, s_axis, t_axis)
  tube(bm, pts, radius, sides, ...)           circle (or profile) along a polyline, rotation-minimising frames
  prism(bm, outline2d, z0, z1, ...)           straight extrusion of a 2D outline (concave allowed)
UV helpers: uv_planar, uv_box, uv_const, uv_cyl
Finishing: part(), finish_exhibit()
Textures: value_noise, fbm, height_to_normal, save_png, save_jpg
"""
import math
import os
import subprocess
import tempfile

import bpy
import bmesh
import numpy as np
from mathutils import Matrix, Vector

import ec_blender as ec


# ------------------------------------------------------------------------------------------------ geometry

def _apply_mats(faces, mats, idx):
    if mats is None:
        return
    m = mats if isinstance(mats, int) else mats[idx]
    for f in faces:
        f.material_index = m


def lathe(bm, profile, seg, mats=0, uv=None, xform=None, sharp_edges=(), offset_angle=0.0, caps=True):
    """Solid of revolution about the local Z axis.
    profile: [(r, z)], ordered CCW in the (r, z) half-plane (bottom centre -> outside -> top centre) so normals
             come out right; r ~ 0 points become poles. mats: int or per-profile-edge list.
    uv: None | dict(mode='cyl', u_scale=1, v_scale=1, v='arc'|'z', z0=0, z1=1) | callable(co, angle01) -> (u, v)
    xform: Matrix applied to the new vertices (place/rotate the part).
    sharp_edges: profile indices whose rings are marked sharp."""
    rings = []
    for (r, z) in profile:
        if r < 1e-7:
            rings.append([bm.verts.new((0.0, 0.0, z))])
        else:
            rings.append([bm.verts.new((r * math.cos(offset_angle + 2 * math.pi * k / seg),
                                        r * math.sin(offset_angle + 2 * math.pi * k / seg), z)) for k in range(seg)])
    faces = []
    face_info = []
    cap_faces = []
    if caps:
        for ring, rev in ((rings[0], True), (rings[-1], False)):
            if len(ring) > 1:
                f = bm.faces.new(list(reversed(ring)) if rev else ring)
                f.material_index = mats if isinstance(mats, int) else (mats[0] if rev else mats[-1])
                cap_faces.append(f)
                for e in f.edges:
                    e.smooth = False
    for i in range(len(rings) - 1):
        a, b = rings[i], rings[i + 1]
        edge_faces = []
        for k in range(seg):
            k1 = (k + 1) % seg
            if len(a) == 1 and len(b) == 1:
                continue
            if len(a) == 1:
                f = bm.faces.new((a[0], b[k], b[k1]))
            elif len(b) == 1:
                f = bm.faces.new((a[k], b[0], a[k1]))
            else:
                f = bm.faces.new((a[k], b[k], b[k1], a[k1]))
            edge_faces.append(f)
            face_info.append((f, i, k))
        _apply_mats(edge_faces, mats, i)
        faces.extend(edge_faces)
    for i in sharp_edges:
        if len(rings[i]) > 1:
            for k in range(seg):
                e = bm.edges.get((rings[i][k], rings[i][(k + 1) % seg]))
                if e:
                    e.smooth = False
    bmesh.ops.recalc_face_normals(bm, faces=faces + cap_faces)
    if uv is not None:
        uvl = bm.loops.layers.uv.verify()
        for f in cap_faces:  # planar top-down projection for flat caps
            for loop in f.loops:
                loop[uvl].uv = (0.5 + loop.vert.co.x * 0.5, 0.5 + loop.vert.co.y * 0.5) if not callable(uv) else uv(loop.vert.co, 0.0)
        # arc length along the profile for v
        arc = [0.0]
        for i in range(1, len(profile)):
            arc.append(arc[-1] + math.hypot(profile[i][0] - profile[i - 1][0], profile[i][1] - profile[i - 1][1]))
        for f, i, k in face_info:
            for loop in f.loops:
                v = loop.vert
                # which ring/angle index is this vertex
                if v in rings[i] or (len(rings[i]) == 1 and v is rings[i][0]):
                    pi_ = i
                else:
                    pi_ = i + 1
                ring = rings[pi_]
                if len(ring) == 1:
                    kk = k + 0.5
                else:
                    kk = ring.index(v)
                    if kk == 0 and k == seg - 1:
                        kk = seg
                a01 = kk / seg
                if callable(uv):
                    loop[uvl].uv = uv(v.co, a01)
                else:
                    if uv.get("v", "arc") == "arc":
                        vv = arc[pi_] / max(arc[-1], 1e-9)
                    else:
                        vv = (v.co.z - uv.get("z0", 0.0)) / max(uv.get("z1", 1.0) - uv.get("z0", 0.0), 1e-9)
                    loop[uvl].uv = (a01 * uv.get("u_scale", 1.0), vv * uv.get("v_scale", 1.0))
    faces += cap_faces
    if xform is not None:
        bmesh.ops.transform(bm, matrix=xform, verts=list({v for f in faces for v in f.verts}))
    return faces


def plan_loft(bm, outline_fn, profile, mats=0, caps=(True, True), cap_mats=None, xform=None, sharp_caps=True):
    """Loft of plan outlines. outline_fn(o) -> [(x, y)] CCW with the same point count for every o (o = outward
    offset from the nominal outline, negative = inset). profile: [(o, z)] bottom -> top. Closed with ngon caps.
    mats: int or per-profile-edge list; cap_mats: (bottom, top)."""
    rings = [[bm.verts.new((x, y, z)) for x, y in outline_fn(o)] for o, z in profile]
    n = len(rings[0])
    faces = []
    for i in range(len(rings) - 1):
        ef = []
        for k in range(n):
            k1 = (k + 1) % n
            ef.append(bm.faces.new((rings[i][k], rings[i][k1], rings[i + 1][k1], rings[i + 1][k])))
        _apply_mats(ef, mats, i)
        faces.extend(ef)
    cm = cap_mats or ((mats if isinstance(mats, int) else mats[0]), (mats if isinstance(mats, int) else mats[-1]))
    capf = []
    if caps[0]:
        f = bm.faces.new(list(reversed(rings[0])))
        f.material_index = cm[0]
        faces.append(f)
        capf.append(f)
    if caps[1]:
        f = bm.faces.new(rings[-1])
        f.material_index = cm[1]
        faces.append(f)
        capf.append(f)
    bmesh.ops.recalc_face_normals(bm, faces=faces)
    if sharp_caps:
        for f in capf:
            for e in f.edges:
                e.smooth = False
    if xform is not None:
        bmesh.ops.transform(bm, matrix=xform, verts=list({v for f in faces for v in f.verts}))
    return faces


def rrect_fn(hw, hd, r, seg):
    """outline_fn for plan_loft: rounded rectangle half sizes hw (x), hd (y), corner radius r."""
    def fn(o):
        return ec.rounded_rect(hw + o, hd + o, max(r + o, 1e-5), seg)
    return fn


def stadium_fn(hw, hd, seg):
    def fn(o):
        return ec.stadium(hw + o, hd + o, seg)
    return fn


def circle_fn(r, n):
    def fn(o):
        return ec.circle(r + o, n)
    return fn


def edge_profile(z0, z1, r_bot, r_top, n_bot=3, n_top=3, taper_top=0.0):
    """Standard slab edge profile [(o, z)]: rounded bottom edge (radius r_bot), straight side, rounded top edge
    (radius r_top); taper_top insets the whole top by that amount (draft)."""
    # bottom quarter circle from (-r_bot, z0) to (0, z0 + r_bot); top from (-taper, z1 - r_top) to (-taper - r_top, z1)
    prof = [(-r_bot * (1 - math.sin(math.pi / 2 * i / n_bot)), z0 + r_bot * (1 - math.cos(math.pi / 2 * i / n_bot)))
            for i in range(n_bot + 1)] if n_bot else [(0.0, z0)]
    top = [(-taper_top - r_top * (1 - math.cos(math.pi / 2 * i / n_top)), z1 - r_top + r_top * math.sin(math.pi / 2 * i / n_top))
           for i in range(n_top + 1)] if n_top else [(-taper_top, z1)]
    return prof + top


def sweep(bm, profile, frames, edge_mats=0, caps=(True, True), cap_mat=None, closed=True):
    """Closed 2D profile [(s, t)] swept along frames [(origin, s_axis, t_axis)] (Blender Vectors)."""
    rings = [[bm.verts.new(o + s * sa + t * ta) for s, t in profile] for o, sa, ta in frames]
    n = len(profile)
    faces = []
    for k in range(len(rings) - 1):
        for i in range(n if closed else n - 1):
            j = (i + 1) % n
            f = bm.faces.new((rings[k][i], rings[k + 1][i], rings[k + 1][j], rings[k][j]))
            f.material_index = edge_mats if isinstance(edge_mats, int) else edge_mats[i]
            faces.append(f)
    cm = cap_mat if cap_mat is not None else (edge_mats if isinstance(edge_mats, int) else edge_mats[0])
    capf = []
    if caps[0]:
        f = bm.faces.new(list(reversed(rings[0])))
        f.material_index = cm
        faces.append(f)
        capf.append(f)
    if caps[1]:
        f = bm.faces.new(rings[-1])
        f.material_index = cm
        faces.append(f)
        capf.append(f)
    if caps[0] and caps[1]:
        bmesh.ops.recalc_face_normals(bm, faces=faces)
    for f in capf:
        for e in f.edges:
            e.smooth = False
    return faces


def rmf_frames(pts, closed=False, up=None):
    """Rotation-minimising frames along a polyline: [(origin, n, b)] with n, b spanning the normal plane.
    If `up` is given, b is fixed to the component of `up` normal to the tangent (no twist for planar paths)."""
    pts = [Vector(p) for p in pts]
    N = len(pts)
    tans = []
    for i in range(N):
        if closed:
            t = pts[(i + 1) % N] - pts[i - 1]
        else:
            t = pts[min(i + 1, N - 1)] - pts[max(i - 1, 0)]
        tans.append(t.normalized())
    frames = []
    if up is not None:
        upv = Vector(up)
        for p, t in zip(pts, tans):
            b = (upv - t * upv.dot(t)).normalized()
            n = b.cross(t).normalized()
            frames.append((p, n, b))
        return frames
    t0 = tans[0]
    ref = Vector((0, 0, 1)) if abs(t0.z) < 0.9 else Vector((1, 0, 0))
    n = (ref - t0 * ref.dot(t0)).normalized()
    for i, (p, t) in enumerate(zip(pts, tans)):
        n = (n - t * n.dot(t)).normalized()
        b = t.cross(n).normalized()
        frames.append((p, n, b))
    return frames


def tube(bm, pts, radius, sides=12, mat=0, closed=False, caps=True, up=None, profile=None):
    """Sweep a circle (or a closed 2D `profile` [(s, t)]) along pts. closed=True makes a loop (no caps)."""
    prof = profile or ec.circle(radius, sides)
    frames = rmf_frames(pts, closed=closed, up=up)
    rings = [[bm.verts.new(o + n * s + b * t) for s, t in prof] for o, n, b in frames]
    m = len(prof)
    faces = []
    count = len(rings) if closed else len(rings) - 1
    for k in range(count):
        a, bb = rings[k], rings[(k + 1) % len(rings)]
        off = 0
        if closed and k == len(rings) - 1 and profile is None:
            # align the last ring to the first to avoid a twist seam
            best = min(range(m), key=lambda o: (a[0].co - bb[o].co).length)
            off = best
        for i in range(m):
            j = (i + 1) % m
            f = bm.faces.new((a[i], a[j], bb[(j + off) % m], bb[(i + off) % m]))
            f.material_index = mat
            faces.append(f)
    if caps and not closed:
        for ring, rev in ((rings[0], True), (rings[-1], False)):
            f = bm.faces.new(list(reversed(ring)) if rev else ring)
            f.material_index = mat
            faces.append(f)
    if closed or caps:
        bmesh.ops.recalc_face_normals(bm, faces=faces)
    return faces


def prism(bm, outline, z0, z1, mat_side=0, mat_top=None, mat_bot=None, xform=None):
    """Straight extrusion of a 2D outline (CCW, concave allowed) from z0 to z1 with ngon caps."""
    mt = mat_side if mat_top is None else mat_top
    mb = mat_side if mat_bot is None else mat_bot
    bot = [bm.verts.new((x, y, z0)) for x, y in outline]
    top = [bm.verts.new((x, y, z1)) for x, y in outline]
    n = len(outline)
    faces = []
    for i in range(n):
        j = (i + 1) % n
        f = bm.faces.new((bot[i], bot[j], top[j], top[i]))
        f.material_index = mat_side
        faces.append(f)
    fb = bm.faces.new(list(reversed(bot)))
    fb.material_index = mb
    ft = bm.faces.new(top)
    ft.material_index = mt
    faces += [fb, ft]
    bmesh.ops.recalc_face_normals(bm, faces=faces)
    for f in (fb, ft):
        for e in f.edges:
            e.smooth = False
    if xform is not None:
        bmesh.ops.transform(bm, matrix=xform, verts=list({v for f in faces for v in f.verts}))
    return faces


# ------------------------------------------------------------------------------------------------ UVs

def uv_planar(bm, faces, origin, u_axis, v_axis, scale=1.0, offset=(0.0, 0.0)):
    uvl = bm.loops.layers.uv.verify()
    o, ua, va = Vector(origin), Vector(u_axis), Vector(v_axis)
    for f in faces:
        for loop in f.loops:
            d = loop.vert.co - o
            loop[uvl].uv = (d.dot(ua) * scale + offset[0], d.dot(va) * scale + offset[1])


def uv_box(bm, faces, scale=1.0, offset=(0.0, 0.0)):
    """Per-face dominant-axis projection (tileable materials)."""
    uvl = bm.loops.layers.uv.verify()
    for f in faces:
        f.normal_update()
        n = f.normal
        ax = max(range(3), key=lambda i: abs(n[i]))
        for loop in f.loops:
            c = loop.vert.co
            if ax == 0:
                u, v = c.y * (1 if n.x > 0 else -1), c.z
            elif ax == 1:
                u, v = c.x * (-1 if n.y > 0 else 1), c.z
            else:
                u, v = c.x, c.y * (1 if n.z > 0 else -1)
            loop[uvl].uv = (u * scale + offset[0], v * scale + offset[1])


def uv_const(bm, faces, u, v):
    uvl = bm.loops.layers.uv.verify()
    for f in faces:
        for loop in f.loops:
            loop[uvl].uv = (u, v)


# ------------------------------------------------------------------------------------------------ finishing

def part(name, bm, mats, smooth_angle=35.0, all_smooth=True):
    """bmesh -> object with the shared material list; smooth by angle (explicit sharp edges kept)."""
    if not bm.loops.layers.uv:
        bm.loops.layers.uv.new("UVMap")
    if all_smooth:
        for f in bm.faces:
            f.smooth = True
    obj = ec.new_mesh_object(name, bm, mats)
    ec.select_only(obj)
    bpy.ops.object.shade_smooth_by_angle(angle=math.radians(smooth_angle), keep_sharp_edges=True)
    return obj


def finish_exhibit(ex_id, parts, out, record_path, record, ground=True):
    """Join parts, triangulate, ground + centre footprint, export, inspect, write the build record."""
    ec.select_only(parts)
    if len(parts) > 1:
        bpy.ops.object.join()
    obj = bpy.context.view_layer.objects.active
    obj.name = obj.data.name = ex_id
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 3])
    bm.to_mesh(obj.data)
    bm.free()
    if ground:
        ec.ground_and_centre([obj])
    out = os.path.abspath(out)
    ec.export_glb([obj], out)
    info = ec.inspect_glb(out)
    rec = {"id": ex_id}
    rec.update(record)
    rec.update({
        "versions": ec.versions(),
        "imagemagick": ec.magick_version(),
        "exporterSettings": ec.EXPORT_SETTINGS,
        "exporterSettingsSha256": ec.export_settings_digest(),
        "output": {"path": ec.rel(out), "sha256": info["sha256"], "bytes": info["bytes"],
                   "triangles": info["triangles"], "boundsMetres": info["boundsMetres"],
                   "extensionsUsed": info["extensionsUsed"]},
    })
    ec.write_json(record_path, rec)
    print("AUTHOR_RESULT", ex_id, info["sha256"], info["bytes"], info["triangles"], info["boundsMetres"])
    return obj, info


# ------------------------------------------------------------------------------------------------ textures

def value_noise(h, w, cells_y, cells_x, seed):
    """Tileable smooth value noise in [0, 1], shape (h, w)."""
    rng = np.random.default_rng(seed)
    g = rng.random((cells_y, cells_x))
    ys = np.arange(h) / h * cells_y
    xs = np.arange(w) / w * cells_x
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    y1 = (y0 + 1) % cells_y
    x1 = (x0 + 1) % cells_x
    y0 %= cells_y
    x0 %= cells_x
    a = g[np.ix_(y0, x0)]
    b = g[np.ix_(y0, x1)]
    c = g[np.ix_(y1, x0)]
    d = g[np.ix_(y1, x1)]
    top = a * (1 - fx)[None, :] + b * fx[None, :]
    bot = c * (1 - fx)[None, :] + d * fx[None, :]
    return top * (1 - fy)[:, None] + bot * fy[:, None]


def fbm(h, w, base_cells, octaves, seed, gain=0.5):
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = base_cells * (2 ** o)
        out += amp * value_noise(h, w, c, c, seed + 101 * o)
        tot += amp
        amp *= gain
    return out / tot


def height_to_normal(hgt, strength):
    """Tangent-space normal map (glTF/OpenGL: +X right, +Y up the image) from a tileable height field."""
    dx = (np.roll(hgt, -1, axis=1) - np.roll(hgt, 1, axis=1)) * 0.5
    dy_rows = (np.roll(hgt, -1, axis=0) - np.roll(hgt, 1, axis=0)) * 0.5
    nx = -dx * strength
    ny = dy_rows * strength  # +Y is up the image = decreasing row index
    nz = np.ones_like(hgt)
    L = np.sqrt(nx * nx + ny * ny + nz * nz)
    rgb = np.stack([(nx / L) * 0.5 + 0.5, (ny / L) * 0.5 + 0.5, (nz / L) * 0.5 + 0.5], axis=-1)
    return np.clip(np.floor(rgb * 255 + 0.5), 0, 255).astype(np.uint8)


def to_u8(rgb01):
    return np.clip(np.floor(np.asarray(rgb01) * 255 + 0.5), 0, 255).astype(np.uint8)


def save_png(path, img_u8):
    h, w = img_u8.shape[:2]
    ec.write_png_rgb(path, w, h, [img_u8[r].tobytes() for r in range(h)])
    return path


def save_jpg(path, img_u8, quality=90):
    """Deterministic JPEG: stdlib PNG -> ImageMagick (4:2:0, stripped)."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        p = os.path.join(tmp, "src.png")
        save_png(p, img_u8)
        subprocess.run(["magick", p, "-strip", "-sampling-factor", "4:2:0", "-quality", str(quality), path],
                       check=True)
    return path


def srgb_u8(r, g, b):
    return np.array([r, g, b], dtype=float)
