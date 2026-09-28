"""Build the Earth Collection archive room in Blender, bake its lighting, save the .blend.

Usage:
  Blender -b --factory-startup --python art/room/build_room.py -- [--samples 1024] [--atlas 2048]
      [--device GPU|CPU] [--no-bake]

Writes:
  art/room/earth_archive.blend              authoring scene: geometry, authoring materials, lights, layout empties,
                                            baked images linked from textures/ (relative paths)
  art/room/textures/room_<group>.jpg        baked lighting atlases (display-referred sRGB through AgX), 2048 px
  art/room/textures/room_starfield.jpg      the aperture's packaged starfield + planetary crescent (procedural)
  art/room/room-build-record.json           parameters, versions, bake settings, texture hashes
Groups (one mesh object = one runtime node each):
  floor            the floor (room + vestibule)
  structure_shell  one connected vault strip (walls behind the panels, coves, ceiling with two recessed light
                   channels), four transverse ribs, the ceiling ring luminaire, downlight discs
  structure_walls  slate side panels, cove light ledges, skirtings, rear wall with the oval aperture frame,
                   front wall with the entrance portal, architrave and closed vestibule with its marker
  displays         three bays of furniture (two curved benches with risers, the domestic platform with plinths,
                   the doorbell backboard, lecterns), blank brass plaques, floor inlays, entrance threshold
  aperture         the starfield card behind the aperture (not baked: its own emissive texture)
Everything comes from art/room/room_layout.py. Geometry is authored in runtime coordinates (+Y up, visitor faces
-Z) and converted with runtime (x, y, z) -> Blender (x, -z, y). The room is closed on all sides (no gaps): every
large surface overlaps its neighbours on the outside by a few centimetres.

Lighting: Cycles bakes COMBINED (direct + indirect diffuse + emission; no glossy) into one atlas per baked group
through the authoring materials and lights; the float bake is saved through AgX to JPEG q92, so atlases hold
final display colours. export_room.py ships each group as baseColor black + emissive = atlas. Brass is authored as
a warm diffuse colour for the bake (metals have no diffuse term to bake).
"""
import argparse
import math
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "tools"))
import bpy  # noqa: E402
import bmesh  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import ec_shapes as ES  # noqa: E402
import room_layout as RL  # noqa: E402

SCOPE = "room"
PALETTE = {
    "mineral_white": ((226, 221, 210), 0.85),
    "slate": ((92, 112, 128), 0.75),
    "slate_seam": ((38, 44, 49), 0.9),
    "vestibule": ((64, 76, 86), 0.8),
    "brass": ((176, 136, 74), 0.4),
    "floor": ((172, 166, 158), 0.6),
}
EMIT = {
    "channel": ((255, 224, 186), 9.0),
    "glow": ((255, 204, 150), 5.0),
    "downlight": ((255, 230, 200), 6.0),
    "wallwash": ((255, 224, 188), 10.0),
    "ring": ((255, 228, 192), 4.0),
    "aperture_glow": ((255, 216, 168), 7.0),
    "marker": ((255, 206, 150), 8.0),
}
SPOT = {"power": 420.0, "color": (1.0, 0.88, 0.74), "size_deg": 20.0, "blend": 0.45, "radius": 0.04, "throw": 0.9}
WORLD_FILL = ((0.02, 0.021, 0.024), 1.0)   # the room is closed; the world only matters through the aperture card
VIEW_TRANSFORM = ("AgX", "None", 0.0)
RIBS_Z = [-3.0, -1.0, 1.0, 3.0]
RIB_HW, RIB_DEPTH = 0.15, 0.20
SLOT = (3.25, 3.45, 0.12)   # recessed ceiling channel: |x| from, |x| to, depth


def RB(x, y, z):
    return Vector((x, -z, y))


def parse():
    ap = argparse.ArgumentParser()
    ap.add_argument("--samples", type=int, default=1024)
    ap.add_argument("--atlas", type=int, default=2048)
    ap.add_argument("--device", default="GPU", choices=["GPU", "CPU"])
    ap.add_argument("--no-bake", action="store_true")
    return ap.parse_args(ec.script_args())


# ------------------------------------------------------------------ materials

def authoring_material(group, key, image=None):
    if key == "starfield":
        return ec.pbr_material(f"{group}_{key}", (0, 0, 0, 1), metallic=0.0, roughness=0.9, emissive_image=image,
                               emissive_strength=1.0)
    if key in PALETTE:
        rgb, rough = PALETTE[key]
        return ec.pbr_material(f"{group}_{key}", ec.srgb(*rgb), metallic=0.0, roughness=rough)
    rgb, strength = EMIT[key]
    return ec.pbr_material(f"{group}_{key}", ec.srgb(20, 20, 20), metallic=0.0, roughness=0.9,
                           emissive=ec.srgb(*rgb), emissive_strength=strength)


class Group:
    def __init__(self, name, keys, image=None):
        self.name = name
        self.keys = keys
        self.mats = [authoring_material(name, k, image) for k in keys]
        self.bm = bmesh.new()

    def mi(self, key):
        return self.keys.index(key)


# ------------------------------------------------------------------ geometry helpers (runtime coordinates)

def orient(faces, want_fn):
    for f in faces:
        f.normal_update()
        c = f.calc_center_median()
        want = want_fn(Vector((c.x, c.z, -c.y)))
        if f.normal.dot(RB(*want)) < 0:
            f.normal_flip()


def quad(g, pts, key, want):
    f = g.bm.faces.new([g.bm.verts.new(RB(*p)) for p in pts])
    f.material_index = g.mi(key)
    orient([f], lambda c: want)
    return f


def sweep(g, profile, frames, edge_keys, caps=(True, True), cap_key=None, closed=True, drop_edges=(), drop_caps=()):
    """Closed 2D profile [(s, t)] along frames [(origin, s_axis, t_axis)] (runtime Vectors); normals made
    consistent/outward on the closed solid, then never-visible faces (drop_edges / drop_caps) are deleted."""
    rings = [[g.bm.verts.new(RB(*(o + s * sa + t * ta))) for s, t in profile] for o, sa, ta in frames]
    n = len(profile)
    faces, doomed = [], []
    for k in range(len(rings) - 1):
        for i in range(n if closed else n - 1):
            j = (i + 1) % n
            f = g.bm.faces.new((rings[k][i], rings[k + 1][i], rings[k + 1][j], rings[k][j]))
            f.material_index = g.mi(edge_keys[i])
            faces.append(f)
            if i in drop_edges:
                doomed.append(f)
    ck = cap_key or edge_keys[0]
    for which, ring in ((0, list(reversed(rings[0]))), (1, rings[-1])):
        if caps[which]:
            f = g.bm.faces.new(ring)
            f.material_index = g.mi(ck)
            faces.append(f)
            if which in drop_caps:
                doomed.append(f)
    if caps[0] and caps[1]:
        bmesh.ops.recalc_face_normals(g.bm, faces=faces)
    if doomed:
        bmesh.ops.delete(g.bm, geom=doomed, context='FACES_ONLY')
        faces = [f for f in faces if f.is_valid]
    return faces


def box_rr(g, centre, yaw_deg, hw, hd, y0, y1, r, key, chamfer=0.006, seg=4, drop_bottom=True):
    cy, sy = math.cos(math.radians(yaw_deg)), math.sin(math.radians(yaw_deg))

    def ring(inset, y):
        pts = []
        for lx, lz in ec.rounded_rect(hw - inset, hd - inset, max(r - inset, 0.001), seg):
            pts.append(RB(centre[0] + lx * cy + lz * sy, y, centre[1] - lx * sy + lz * cy))
        return pts
    rings = [ring(0, y0), ring(0, y1 - chamfer), ring(chamfer, y1)]
    vr = [[g.bm.verts.new(p) for p in rg] for rg in rings]
    faces = []
    n = len(vr[0])
    for k in range(2):
        for i in range(n):
            j = (i + 1) % n
            f = g.bm.faces.new((vr[k][i], vr[k + 1][i], vr[k + 1][j], vr[k][j]))
            f.material_index = g.mi(key)
            faces.append(f)
    bot = g.bm.faces.new(list(reversed(vr[0])))
    top = g.bm.faces.new(vr[-1])
    for f in (bot, top):
        f.material_index = g.mi(key)
    faces += [bot, top]
    bmesh.ops.recalc_face_normals(g.bm, faces=faces)
    if drop_bottom:
        bmesh.ops.delete(g.bm, geom=[bot], context='FACES_ONLY')
    return [f for f in faces if f.is_valid]


def disc(g, centre, radius, key, want, n=24, axis_u=Vector((1, 0, 0)), axis_v=Vector((0, 0, 1))):
    c = Vector(centre)
    vs = [g.bm.verts.new(RB(*(c + axis_u * radius * math.cos(2 * math.pi * i / n) + axis_v * radius * math.sin(2 * math.pi * i / n))))
          for i in range(n)]
    f = g.bm.faces.new(vs)
    f.material_index = g.mi(key)
    orient([f], lambda _c: want)
    return f


def annulus(g, centre, r0, r1, key, want, n=48, axis_u=Vector((1, 0, 0)), axis_v=Vector((0, 0, 1))):
    c = Vector(centre)
    inner, outer = [], []
    for i in range(n):
        a = 2 * math.pi * i / n
        d = axis_u * math.cos(a) + axis_v * math.sin(a)
        inner.append(g.bm.verts.new(RB(*(c + d * r0))))
        outer.append(g.bm.verts.new(RB(*(c + d * r1))))
    faces = []
    for i in range(n):
        j = (i + 1) % n
        f = g.bm.faces.new((inner[i], outer[i], outer[j], inner[j]))
        f.material_index = g.mi(key)
        faces.append(f)
    orient(faces, lambda _c: want)
    return faces


# ------------------------------------------------------------------ cross-sections

def cove_points(side, n=16):
    """Cove from the wall top (|x| = 5, y = 2.5) to the ceiling edge (|x| = 3.6, y = 4.0); side = -1 left, +1 right."""
    pts = []
    for i in range(n + 1):
        th = math.radians(180 - 90 * i / n)
        x = -3.6 + 1.4 * math.cos(th)
        y = 2.5 + 1.5 * math.sin(th)
        pts.append((x if side < 0 else -x, y))
    return pts


def shell_section(with_slots=True):
    """Room cross-section polyline in (x, y) from the left floor edge, over the vault, to the right floor edge.
    Walking along it, the room interior is on the right-hand side."""
    pts = [(-5.0, -0.05)] + [(-5.0, 2.5 * i / 5) for i in range(1, 6)]
    pts += cove_points(-1)[1:]
    x0, x1, dep = SLOT
    if with_slots:
        pts += [(-x1, 4.0), (-x1, 4.0 + dep), (-x0, 4.0 + dep), (-x0, 4.0),
                (x0, 4.0), (x0, 4.0 + dep), (x1, 4.0 + dep), (x1, 4.0)]
    right = list(reversed(cove_points(1)))
    pts += right
    pts += [(5.0, 2.5 * i / 5) for i in range(4, -1, -1)] + [(5.0, -0.05)]
    out = []
    for p in pts:  # drop consecutive duplicates
        if not out or (abs(p[0] - out[-1][0]) > 1e-9 or abs(p[1] - out[-1][1]) > 1e-9):
            out.append(p)
    return out


def right_perp(p0, p1):
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dy)
    return (dy / L, -dx / L)


def station_normals(pts):
    ns = []
    for i in range(len(pts)):
        a = pts[max(i - 1, 0)]
        b = pts[min(i + 1, len(pts) - 1)]
        ns.append(right_perp(a, b))
    return ns


def offset_outline(pts, d):
    """Offset a section polyline outward (away from the interior) by d using station normals."""
    ns = station_normals(pts)
    return [(p[0] - n[0] * d, p[1] - n[1] * d) for p, n in zip(pts, ns)]


# ------------------------------------------------------------------ room pieces

def build_floor(g):
    quad(g, [(-5.05, 0, -4.55), (5.05, 0, -4.55), (5.05, 0, 4.55), (-5.05, 0, 4.55)], "floor", (0, 1, 0))
    v = RL.ROOM["vestibule"]
    quad(g, [(v["x"][0] - 0.05, 0, 4.50), (v["x"][1] + 0.05, 0, 4.50), (v["x"][1] + 0.05, 0, v["z"][1] + 0.05),
             (v["x"][0] - 0.05, 0, v["z"][1] + 0.05)], "floor", (0, 1, 0))


def build_shell(g):
    pts = shell_section(True)
    z0, z1 = -4.52, 4.52
    rows = [(g.bm.verts.new(RB(x, y, z0)), g.bm.verts.new(RB(x, y, z1))) for x, y in pts]
    x0, x1, dep = SLOT
    for k in range(len(pts) - 1):
        (xa, ya), (xb, yb) = pts[k], pts[k + 1]
        if max(ya, yb) <= 2.5 + 1e-6 and abs(xa) > 4.9:
            key = "slate_seam"
        elif abs(ya - (4.0 + dep)) < 1e-6 and abs(yb - (4.0 + dep)) < 1e-6:
            key = "channel"
        else:
            key = "mineral_white"
        f = g.bm.faces.new((rows[k][0], rows[k + 1][0], rows[k + 1][1], rows[k][1]))
        f.material_index = g.mi(key)
        n = right_perp(pts[k], pts[k + 1])
        orient([f], lambda _c, n=n: (n[0], n[1], 0.0))
    # transverse ribs over the vault (no slot detours), rounded inner corners
    rp = shell_section(False)
    rn = station_normals(rp)
    prof = [(0.0, -RIB_HW)]
    rr = 0.05
    for i in range(5):
        a = math.radians(-90 + 90 * i / 4)
        prof.append((RIB_DEPTH - rr + rr * math.cos(a), -RIB_HW + rr + rr * math.sin(a)))
    for i in range(5):
        a = math.radians(90 * i / 4)
        prof.append((RIB_DEPTH - rr + rr * math.cos(a), RIB_HW - rr + rr * math.sin(a)))
    prof.append((0.0, RIB_HW))
    for zr in RIBS_Z:
        frames = [(Vector((p[0], p[1], zr)), Vector((n[0], n[1], 0.0)), Vector((0, 0, 1))) for p, n in zip(rp, rn)]
        sweep(g, prof, frames, ["mineral_white"] * len(prof), drop_caps=(0, 1))
    # ceiling ring luminaire above the arrival motif
    rprof = [(1.36, 3.995), (1.58, 3.995), (1.58, 3.962), (1.53, 3.952), (1.41, 3.952), (1.36, 3.962)]
    keys = ["mineral_white", "mineral_white", "mineral_white", "ring", "mineral_white", "mineral_white"]
    frames = []
    n = 128
    for i in range(n + 1):
        a = 2 * math.pi * i / n
        frames.append((Vector((0, 0, 0)), Vector((math.cos(a), 0, math.sin(a))), Vector((0, 1, 0))))
    fs = sweep(g, [(s, t) for s, t in rprof], frames, keys, caps=(False, False), drop_edges=(0,))
    bmesh.ops.remove_doubles(g.bm, verts=list({v for f in fs if f.is_valid for v in f.verts}), dist=1e-6)
    orient([f for f in fs if f.is_valid], lambda c: (0.0, -1.0, 0.0) if c.y < 3.955 else
           ((c.x, 0.0, c.z) if math.hypot(c.x, c.z) > 1.45 else (-c.x, 0.0, -c.z)))
    # downlight discs above every anchor's spot
    for pos, _ in spot_positions():
        disc(g, (pos[0], 3.999, pos[2]), 0.045, "downlight", (0, -1, 0), n=20)


def spot_positions():
    out = []
    for a in RL.anchors():
        x, y, z = a["position"]
        yaw = math.radians(a["yawDeg"])
        fx, fz = math.sin(yaw), math.cos(yaw)   # exhibit front = toward the visitor
        t = SPOT["throw"]
        out.append(((x + fx * t, 3.97, z + fz * t), (x, y + 0.15, z)))
    return out


def rear_outline():
    """Convex outline of the rear/front wall planes: the room section (no slots) pushed 0.2 m outward, so the
    walls overlap the vault's ends on the outside and close the slot ends."""
    return offset_outline(shell_section(False), 0.20)


def ellipse_r(a, b, th):
    return a * b / math.sqrt((b * math.cos(th)) ** 2 + (a * math.sin(th)) ** 2)


def ray_hit(poly, cx, cy, th):
    """Distance from (cx, cy) along direction th to the closed polygon's boundary (convex/star-shaped)."""
    dx, dy = math.cos(th), math.sin(th)
    best = None
    n = len(poly)
    for i in range(n):
        (x0, y0), (x1, y1) = poly[i], poly[(i + 1) % n]
        ex, ey = x1 - x0, y1 - y0
        den = dx * ey - dy * ex
        if abs(den) < 1e-12:
            continue
        t = ((x0 - cx) * ey - (y0 - cy) * ex) / den
        u = ((x0 - cx) * dy - (y0 - cy) * dx) / den
        if t > 0 and -1e-9 <= u <= 1 + 1e-9:
            best = t if best is None else min(best, t)
    return best


def build_rear_wall(g):
    A = RL.APERTURE
    cx, cy, zw = A["centre"]
    a, b = A["w"] / 2, A["h"] / 2
    outline = rear_outline()
    poly = outline + [(outline[-1][0], -0.25), (outline[0][0], -0.25)]
    n = 160
    ths = [2 * math.pi * i / n for i in range(n)]
    bezel = A["bezel"]

    def oval_pt(th, s):
        r = ellipse_r(a, b, th)
        px, py = r * math.cos(th), r * math.sin(th)
        gx, gy = px / (a * a), py / (b * b)
        L = math.hypot(gx, gy)
        return (cx + px + gx / L * s, cy + py + gy / L * s), (gx / L, gy / L)
    inner = [g.bm.verts.new(RB(*oval_pt(th, bezel)[0], zw)) for th in ths]
    outer = []
    for th in ths:
        d = ray_hit(poly, cx, cy, th)
        outer.append(g.bm.verts.new(RB(cx + d * math.cos(th), cy + d * math.sin(th), zw)))
    faces = []
    for i in range(n):
        j = (i + 1) % n
        f = g.bm.faces.new((inner[i], outer[i], outer[j], inner[j]))
        f.material_index = g.mi("mineral_white")
        faces.append(f)
    orient(faces, lambda c: (0, 0, 1))
    # aperture frame: raised bezel with a warm glow lip, then the tunnel back to the starfield card
    prof = [(bezel, 0.0), (bezel - 0.04, 0.05), (0.08, 0.07), (0.02, 0.06), (0.0, 0.03), (0.0, -A["depth"])]
    keys = ["mineral_white", "mineral_white", "mineral_white", "aperture_glow", "mineral_white"]
    frames = []
    for th in ths + [ths[0]]:
        (px, py), (mx, my) = oval_pt(th, 0.0)
        frames.append((Vector((px, py, zw)), Vector((mx, my, 0.0)), Vector((0, 0, 1))))
    fs = sweep(g, prof, frames, keys + ["mineral_white"], caps=(False, False), closed=False)
    bmesh.ops.remove_doubles(g.bm, verts=list({v for f in fs for v in f.verts}), dist=1e-6)
    fs = [f for f in fs if f.is_valid]

    def want(c):
        # bezel faces toward the room (+Z); the tunnel wall faces the aperture axis
        if c.z > zw + 0.005:
            return (0, 0, 1)
        dx, dy = c.x - cx, c.y - cy
        return (-dx / (a * a), -dy / (b * b), 0.0)
    orient(fs, want)


def portal_path(side_bottom=-0.05):
    """Entrance opening outline (runtime x, y) from the left floor, up the jamb, round top corners, lintel, down."""
    v = RL.ROOM["vestibule"]
    hw, top, r = v["x"][1], v["height"], 0.5
    pts = [(-hw, side_bottom), (-hw, top - r)]
    for i in range(1, 9):
        a = math.radians(180 - 90 * i / 8)
        pts.append((-hw + r + r * math.cos(a), top - r + r * math.sin(a)))
    for i in range(1, 9):
        a = math.radians(90 - 90 * i / 8)
        pts.append((hw - r + r * math.cos(a), top - r + r * math.sin(a)))
    pts.append((hw, side_bottom))
    return pts


def build_front_wall(g):
    zw = 4.5
    outline = rear_outline()                  # left bottom -> over the vault -> right bottom
    door = portal_path(-0.25)
    # polygon: left outline bottom -> ... -> right bottom, then along y = -0.25 to the door, up and over, back out
    poly = [(outline[0][0], -0.25)] + outline + [(outline[-1][0], -0.25)] + list(reversed(door))
    vs = [g.bm.verts.new(RB(x, y, zw)) for x, y in poly]
    f = g.bm.faces.new(vs)
    f.material_index = g.mi("mineral_white")
    orient([f], lambda c: (0, 0, -1))
    # architrave around the portal (room side)
    path = portal_path(-0.02)
    fr = []
    for i, (x, y) in enumerate(path):
        a0 = path[max(i - 1, 0)]
        a1 = path[min(i + 1, len(path) - 1)]
        dx, dy = a1[0] - a0[0], a1[1] - a0[1]
        L = math.hypot(dx, dy)
        out = (dy / L, -dx / L)   # right-perp walking the path = away from the opening
        out = (-out[0], -out[1]) if (out[0] * x + out[1] * (y - 1.3)) < 0 else out
        fr.append((Vector((x, y, zw)), Vector((out[0], out[1], 0.0)), Vector((0, 0, -1))))
    prof = [(-0.01, 0.0), (-0.01, 0.05), (0.03, 0.07), (0.15, 0.07), (0.18, 0.04), (0.18, 0.0)]
    sweep(g, prof, fr, ["mineral_white"] * len(prof), drop_edges=(5,), drop_caps=(0, 1))
    # vestibule: jambs + ceiling swept along z, back wall with a closed double-door seam and the marker
    v = RL.ROOM["vestibule"]
    path = portal_path(-0.05)
    rows = [(g.bm.verts.new(RB(x, y, v["z"][0] - 0.02)), g.bm.verts.new(RB(x, y, v["z"][1] + 0.02))) for x, y in path]
    for k in range(len(path) - 1):
        f = g.bm.faces.new((rows[k][0], rows[k + 1][0], rows[k + 1][1], rows[k][1]))
        f.material_index = g.mi("vestibule")
        mx = (path[k][0] + path[k + 1][0]) / 2
        my = (path[k][1] + path[k + 1][1]) / 2
        orient([f], lambda c, mx=mx, my=my: (-mx, 1.3 - my, 0.0))
    zb = v["z"][1]
    quad(g, [(-1.3, -0.05, zb), (1.3, -0.05, zb), (1.3, 2.8, zb), (-1.3, 2.8, zb)], "vestibule", (0, 0, -1))
    # door seam (dark slate strip) and leaf panels, subtle
    for sx in (-1, 1):
        box = [(sx * 0.03, 0.05), (sx * 0.98, 0.05), (sx * 0.98, 2.30), (sx * 0.03, 2.30)]
        pts = [(x, y, zb - 0.012) for x, y in box]
        quad(g, pts if sx > 0 else list(reversed(pts)), "slate", (0, 0, -1))
    # small unlabelled illuminated marker: warm ring + dot at 1.45 m on the right leaf
    c = (0.55, 1.45, zb - 0.02)
    annulus(g, c, 0.055, 0.07, "marker", (0, 0, -1), n=40, axis_u=Vector((1, 0, 0)), axis_v=Vector((0, 1, 0)))
    disc(g, c, 0.018, "marker", (0, 0, -1), n=20, axis_u=Vector((1, 0, 0)), axis_v=Vector((0, 1, 0)))


def build_side_walls(g):
    zmin, zmax = -4.5, 4.5
    gap = 0.006
    edges = [zmin] + [e for z in RIBS_Z for e in (z - RIB_HW, z + RIB_HW)] + [zmax]
    spans = [(edges[i], edges[i + 1]) for i in range(0, len(edges), 2)]
    rows = [(0.10, 1.30), (1.30, 2.45)]
    for side in (-1, 1):
        xw = side * 5.0
        for za, zb in spans:
            for ya, yb in rows:
                hz, hy = (zb - za) / 2 - gap, (yb - ya) / 2 - gap
                cz, cyy = (za + zb) / 2, (ya + yb) / 2
                prof = ec.rounded_rect(hz, hy, 0.006, 2)
                frames = [(Vector((xw + side * 0.002, cyy, cz)), Vector((0, 0, 1)), Vector((0, 1, 0))),
                          (Vector((xw - side * 0.025, cyy, cz)), Vector((0, 0, 1)), Vector((0, 1, 0)))]
                sweep(g, prof, frames, ["slate"] * len(prof), drop_caps=(0,))
        # cove light ledge (emissive underside grazes the slate; top washes the cove) and skirting
        inward = Vector((-side, 0, 0))
        fr = [(Vector((xw + side * 0.002, 0, zmin - 0.02)), inward, Vector((0, 1, 0))),
              (Vector((xw + side * 0.002, 0, zmax + 0.02)), inward, Vector((0, 1, 0)))]
        sweep(g, [(0.0, 2.50), (0.14, 2.50), (0.14, 2.56), (0.0, 2.56)], fr,
              ["wallwash", "mineral_white", "wallwash", "mineral_white"], cap_key="mineral_white", drop_edges=(3,))
        sweep(g, [(0, 0), (0.03, 0), (0.03, 0.09), (0, 0.09)], fr, ["mineral_white"] * 4, drop_edges=(0, 3))


def end_wall_panel(g, x0, x1, y0, y1, zw, facing):
    """Slate panel (25 mm proud, 6 mm rounded corners) on an end wall at z = zw, front facing +Z (1) or -Z (-1)."""
    hx, hy = (x1 - x0) / 2, (y1 - y0) / 2
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    prof = ec.rounded_rect(hx, hy, 0.006, 2)
    frames = [(Vector((cx, cy, zw - facing * 0.002)), Vector((1, 0, 0)), Vector((0, 1, 0))),
              (Vector((cx, cy, zw + facing * 0.025)), Vector((1, 0, 0)), Vector((0, 1, 0)))]
    sweep(g, prof, frames, ["slate"] * len(prof), drop_caps=(0,))


def build_end_wall_panels(g):
    gap = 0.006
    zr = RL.APERTURE["centre"][2]
    # rear: three dado panels behind the Connected lives bench, two tall panels flanking the aperture
    xs = [-4.6, -1.55, 1.55, 4.6]
    for i in range(3):
        end_wall_panel(g, xs[i] + gap, xs[i + 1] - gap, 0.10, RL.REAR_DADO_TOP, zr, 1)
    for sx in (-1, 1):
        a, b = sorted((sx * 2.55, sx * 4.55))
        end_wall_panel(g, a, b, RL.REAR_DADO_TOP + 0.25, 3.40, zr, 1)
    # front: two rows of panels either side of the entrance portal
    for sx in (-1, 1):
        a, b = sorted((sx * 1.60, sx * 4.60))
        end_wall_panel(g, a, b, 0.10, 1.30 - gap, 4.5, -1)
        end_wall_panel(g, a, b, 1.30 + gap, 2.45, 4.5, -1)


def bench_profile(top, hd):
    prof = [(-hd, 0.0), (hd - 0.08, 0.0), (hd - 0.08, 0.085), (hd, 0.085), (hd, top - 0.02),
            (hd - 0.006, top - 0.006), (hd - 0.02, top), (-hd + 0.02, top), (-hd + 0.006, top - 0.006),
            (-hd, top - 0.02)]
    keys = ["mineral_white", "brass", "glow", "mineral_white", "mineral_white", "mineral_white", "mineral_white",
            "mineral_white", "mineral_white", "mineral_white"]
    return prof, keys


def platform_profile(hd):
    top = RL.PLATFORM_TOP
    prof = [(-hd, 0.0), (hd - 0.06, 0.0), (hd - 0.06, 0.035), (hd, 0.035), (hd, top - 0.012),
            (hd - 0.004, top - 0.004), (hd - 0.012, top), (-hd + 0.012, top), (-hd + 0.004, top - 0.004),
            (-hd, top - 0.012)]
    keys = ["mineral_white", "brass", "glow", "mineral_white", "mineral_white", "mineral_white", "mineral_white",
            "mineral_white", "mineral_white", "mineral_white"]
    return prof, keys


def arc_frames(b, off0, off1, step_m=0.12):
    n = max(4, int(abs(math.radians(off1 - off0)) * b["radius"] / step_m))
    frames = []
    for i in range(n + 1):
        off = off0 + (off1 - off0) * i / n
        (x, z), (nx, nz) = RL.arc_point(b, off)
        frames.append((Vector((x, 0, z)), Vector((nx, 0, nz)), Vector((0, 1, 0))))
    return frames


def plaque_plate(g, b, off, face_r, top_y=None):
    pl, top, (nx, nz) = RL.plaque_for(b, off, face_r, top_y)
    p = math.radians(RL.PLAQUE["pitchDeg"])
    n = Vector((nx, 0, nz))
    up = Vector((0, 1, 0))
    tilted_n = n * math.cos(p) + up * math.sin(p)
    tilted_up = up * math.cos(p) - n * math.sin(p)
    right = tilted_up.cross(tilted_n).normalized()
    top_v = Vector(top)
    centre = top_v - tilted_up * (RL.PLAQUE["h"] / 2)
    w, h, t = RL.PLAQUE["w"], RL.PLAQUE["h"], RL.PLAQUE["t"]
    prof = [(-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2)]
    frames = [(centre - tilted_n * 0.002, right, tilted_up), (centre + tilted_n * t, right, tilted_up)]
    sweep(g, prof, frames, ["brass"] * 4, drop_caps=(0,))


def build_displays(g):
    for b in RL.BAYS:
        if b["kind"] == "bench":
            prof, keys = bench_profile(RL.BENCH_TOP, RL.BENCH_DEPTH / 2)
            for o0, o1 in b["segments"]:
                gap_deg = math.degrees(0.015 / b["radius"])
                a0 = o0 - (gap_deg if o0 != b["segments"][0][0] else 0)
                a1 = o1 + (gap_deg if o1 != b["segments"][-1][1] else 0)
                sweep(g, prof, arc_frames(b, a0, a1), keys, cap_key="mineral_white", drop_edges=(0,))
            for ex_id, off, top, _ in RL.item_offsets(b):
                (x, z), (nx, nz) = RL.arc_point(b, off)
                box_rr(g, (x, z), RL.yaw_facing(nx, nz), b["riser"] / 2, b["riser"] / 2, RL.BENCH_TOP - 0.01, top, 0.03,
                       "mineral_white")
                plaque_plate(g, b, off, b["radius"] - RL.BENCH_DEPTH / 2)
        else:
            hd = RL.PLATFORM_DEPTH / 2
            prof, keys = platform_profile(hd)
            sweep(g, prof, arc_frames(b, b["span"], -b["span"]), keys, cap_key="mineral_white", drop_edges=(0,))
            for ex_id, off, top, support in RL.item_offsets(b):
                rad = b["radius"] + RL.RADIAL_OFFSET.get(ex_id, 0.0)
                (x, z), (nx, nz) = RL.arc_point(b, off, rad)
                yaw = RL.yaw_facing(nx, nz)
                if ex_id == "vacuum":
                    cyl = [(0.26 * math.cos(2 * math.pi * i / 48), 0.26 * math.sin(2 * math.pi * i / 48)) for i in range(48)]
                    rings = []
                    for yy, rr in ((RL.PLATFORM_TOP - 0.01, 1.0), (top - 0.008, 1.0), (top, 0.97)):
                        rings.append([g.bm.verts.new(RB(x + px * rr, yy, z + pz * rr)) for px, pz in cyl])
                    fs = []
                    for k in range(2):
                        for i in range(48):
                            j = (i + 1) % 48
                            f = g.bm.faces.new((rings[k][i], rings[k + 1][i], rings[k + 1][j], rings[k][j]))
                            f.material_index = g.mi("mineral_white")
                            fs.append(f)
                    ft = g.bm.faces.new(rings[-1])
                    ft.material_index = g.mi("mineral_white")
                    fs.append(ft)
                    orient(fs, lambda c, x=x, z=z: (0, 1, 0) if c.y > top - 0.0085 and math.hypot(c.x - x, c.z - z) < 0.2
                           else (c.x - x, 0, c.z - z))
                elif ex_id != "scooter":
                    size = RL.PLINTH_SIZE[ex_id]
                    (px, pz), _ = RL.arc_point(b, off, b["radius"])
                    box_rr(g, (px, pz), yaw, size / 2, size / 2, RL.PLATFORM_TOP - 0.01, top, 0.03, "mineral_white")
                if ex_id == "doorbell":
                    bb = RL.BACKBOARD
                    (bx, bz), _ = RL.arc_point(b, off, rad + 0.014 + bb["t"] / 2)
                    box_rr(g, (bx, bz), yaw, bb["w"] / 2, bb["t"] / 2, top - 0.01, top + bb["h"], 0.01, "mineral_white",
                           chamfer=0.004)
                if ex_id in RL.LECTERN_ITEMS:
                    # lectern with the blank plaque on the platform's front edge (low supports)
                    (lx, lz), _ = RL.arc_point(b, off, b["radius"] - 0.33)
                    box_rr(g, (lx, lz), yaw, 0.06, 0.03, RL.PLATFORM_TOP - 0.01, RL.LECTERN_TOP_Y + 0.012, 0.01,
                           "mineral_white", chamfer=0.004)
                    plaque_plate(g, b, off, b["radius"] - 0.36, RL.LECTERN_TOP_Y)
                else:
                    plaque_plate(g, b, off, b["radius"] - RL.PLINTH_SIZE[ex_id] / 2)
    # floor inlays: arrival motif (outer ring r 1.50, inner ring r 0.50, bar toward -Z), 3 mm proud; threshold strip
    h = 0.003
    for r, w in ((1.50, 0.040), (0.50, 0.025)):
        n = 160 if r > 1 else 96
        ri, ro = r - w / 2, r + w / 2
        prof = [(ri, 0.0), (ri, h), (ro, h), (ro, 0.0)]
        frames = [(Vector((0, 0, 0)), Vector((math.cos(2 * math.pi * i / n), 0, math.sin(2 * math.pi * i / n))),
                   Vector((0, 1, 0))) for i in range(n + 1)]
        faces = sweep(g, prof, frames, ["brass"] * 4, caps=(False, False), closed=False)
        bmesh.ops.remove_doubles(g.bm, verts=list({v for f in faces for v in f.verts}), dist=1e-6)
        faces = [f for f in faces if f.is_valid]
        orient(faces, lambda c, r=r: (0, 1, 0) if abs(c.y - h) < 1e-5 else
               ((c.x, 0, c.z) if math.hypot(c.x, c.z) > r else (-c.x, 0, -c.z)))
    bw = 0.02
    for (za, zb) in ((-0.5125, -1.48),):
        sweep(g, [(-bw / 2, 0.0), (bw / 2, 0.0), (bw / 2, h), (-bw / 2, h)],
              [(Vector((0, 0, za)), Vector((1, 0, 0)), Vector((0, 1, 0))),
               (Vector((0, 0, zb)), Vector((1, 0, 0)), Vector((0, 1, 0)))], ["brass"] * 4, drop_edges=(0,))
    v = RL.ROOM["vestibule"]
    sweep(g, [(-0.03, 0.0), (0.03, 0.0), (0.03, h), (-0.03, h)],
          [(Vector((v["x"][0], 0, 4.5)), Vector((0, 0, 1)), Vector((0, 1, 0))),
           (Vector((v["x"][1], 0, 4.5)), Vector((0, 0, 1)), Vector((0, 1, 0)))], ["brass"] * 4, drop_edges=(0,))


def starfield_texture(path, w=2048, h=1024):
    rng = np.random.default_rng(1969)
    yy, xx = np.mgrid[0:h, 0:w]
    u, v = (xx + 0.5) / w, (yy + 0.5) / h
    top = np.array([3, 5, 12], float)
    bot = np.array([9, 13, 28], float)
    img = top[None, None, :] * (1 - v[..., None]) + bot[None, None, :] * v[..., None]
    band = ES.fbm(h, w, 6, 4, seed=77)
    band_mask = np.exp(-((v - 0.35 - 0.25 * (u - 0.5)) / 0.16) ** 2)
    img += (band * band_mask)[..., None] * np.array([18, 18, 26])
    # stars: power-law brightness, slight colour variety, 2x2 soft cores for the brightest
    nstar = 2600
    sx = rng.integers(0, w, nstar)
    sy = rng.integers(0, h, nstar)
    mag = rng.pareto(2.2, nstar) + 1.0
    lum = np.clip(55 * mag, 45, 255)
    tint = rng.choice(3, nstar, p=[0.7, 0.2, 0.1])
    cols = np.array([[1.0, 1.0, 1.0], [0.85, 0.9, 1.0], [1.0, 0.9, 0.75]])
    for x, y, l, t in zip(sx, sy, lum, tint):
        # 2x2 cores so stars survive mip filtering at viewing distance; bright ones get a soft cross
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            x2, y2 = (x + dx) % w, min(y + dy, h - 1)
            img[y2, x2] = np.maximum(img[y2, x2], cols[t] * l)
        if l > 140:
            for dx, dy in ((2, 0), (-1, 0), (0, 2), (0, -1), (2, 1), (-1, 1), (1, 2), (1, -1)):
                x2, y2 = (x + dx) % w, min(max(y + dy, 0), h - 1)
                img[y2, x2] = np.maximum(img[y2, x2], cols[t] * l * 0.4)
    # small distant planet with a lit crescent (light from the left), right third of the view
    pcx, pcy, pr = 0.70 * w, 0.46 * h, 0.13 * h
    dx, dy = (xx - pcx) / pr, (yy - pcy) / pr
    d2 = dx * dx + dy * dy
    inside = d2 <= 1.0
    nz = np.sqrt(np.clip(1 - d2, 0, 1))
    light = np.array([-0.82, -0.18, 0.40])
    light /= np.linalg.norm(light)
    lam = np.clip(dx * light[0] + dy * light[1] + nz * light[2], 0, 1)
    surf = np.array([120, 150, 185], float) * (0.9 + 0.1 * ES.fbm(h, w, 12, 3, seed=5))[..., None]
    planet = surf * (lam ** 1.3)[..., None] + np.array([2, 3, 6])
    img = np.where(inside[..., None], planet, img)
    rim = np.clip(1 - np.abs(np.sqrt(d2) - 1.0) / 0.03, 0, 1) * np.clip(-dx * 0.8 + 0.2, 0, 1)
    img += rim[..., None] * np.array([60, 90, 140])
    return ES.save_jpg(path, ES.to_u8(np.clip(img, 0, 255) / 255.0), 92)


def build_aperture(g):
    A = RL.APERTURE
    cx, cy, zw = A["centre"]
    a, b = A["w"] / 2 + 0.25, A["h"] / 2 + 0.25
    zc = zw - A["depth"] - 0.005
    n = 96
    c = g.bm.verts.new(RB(cx, cy, zc))
    ring = [g.bm.verts.new(RB(cx + a * math.cos(2 * math.pi * i / n), cy + b * math.sin(2 * math.pi * i / n), zc))
            for i in range(n)]
    faces = []
    for i in range(n):
        f = g.bm.faces.new((c, ring[i], ring[(i + 1) % n]))
        f.material_index = 0
        faces.append(f)
    orient(faces, lambda _c: (0, 0, 1))
    uvl = g.bm.loops.layers.uv.verify()
    # the card shows the texture's central 2:1 window scaled to the oval's bounding box
    for f in faces:
        for loop in f.loops:
            co = loop.vert.co
            x, y = co.x, co.z
            loop[uvl].uv = (0.5 + (x - cx) / (2 * a) * 0.98, 0.5 + (y - cy) / (2 * b) * 0.98)


def finish_group(g, smooth=True):
    obj = ec.new_mesh_object(g.name, g.bm, g.mats)
    for p in obj.data.polygons:
        p.use_smooth = smooth
    ec.select_only(obj)
    bpy.ops.object.shade_smooth_by_angle(angle=math.radians(35), keep_sharp_edges=True)
    return obj


def lightmap_uv(obj, margin):
    ec.select_only(obj)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(72), island_margin=margin, area_weight=0.0,
                             correct_aspect=True, scale_to_bounds=False)
    bpy.ops.uv.select_all(action='SELECT')
    bpy.ops.uv.pack_islands(rotate=True, rotate_method='ANY', scale=True, margin_method='SCALED', margin=margin,
                            shape_method='CONCAVE')
    bpy.ops.object.mode_set(mode='OBJECT')


def lights(scn):
    for i, (pos, target) in enumerate(spot_positions()):
        L = bpy.data.lights.new(f"spot_{i:02d}", 'SPOT')
        L.energy = SPOT["power"]
        L.color = SPOT["color"]
        L.spot_size = math.radians(SPOT["size_deg"])
        L.spot_blend = SPOT["blend"]
        L.shadow_soft_size = SPOT["radius"]
        ob = bpy.data.objects.new(L.name, L)
        scn.collection.objects.link(ob)
        ob.location = RB(*pos)
        ob.rotation_euler = (RB(*target) - ob.location).to_track_quat('-Z', 'Y').to_euler()
    w = bpy.data.worlds.new("fill")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs[0].default_value = (*WORLD_FILL[0], 1.0)
    w.node_tree.nodes["Background"].inputs[1].default_value = WORLD_FILL[1]
    scn.world = w


def layout_empties(scn):
    col = bpy.data.collections.new("layout")
    scn.collection.children.link(col)
    for anc in RL.anchors():
        e = bpy.data.objects.new(anc["name"], None)
        e.empty_display_type = 'ARROWS'
        e.empty_display_size = 0.15
        e.location = RB(*anc["position"])
        e.rotation_euler = (0, 0, math.radians(anc["yawDeg"]))
        e["exhibit"] = anc["id"]
        e["bay"] = anc["bay"]
        col.objects.link(e)
    for vp in RL.viewpoints():
        e = bpy.data.objects.new(f"viewpoint_{vp['name']}", None)
        e.empty_display_type = 'SINGLE_ARROW'
        e.location = RB(*vp["position"])
        yaw = math.radians(vp["yawDeg"])
        e.rotation_euler = RB(-math.sin(yaw), 0, -math.cos(yaw)).to_track_quat('Z', 'Y').to_euler()
        e["yawDeg"] = vp["yawDeg"]
        col.objects.link(e)


def setup_cycles(scn, device, samples):
    scn.render.engine = 'CYCLES'
    if device == "GPU":
        prefs = bpy.context.preferences.addons['cycles'].preferences
        prefs.compute_device_type = 'METAL'
        prefs.get_devices()
        for d in prefs.devices:
            d.use = d.type == 'METAL'
        scn.cycles.device = 'GPU'
    else:
        scn.cycles.device = 'CPU'
    scn.cycles.samples = samples
    scn.cycles.use_adaptive_sampling = False
    scn.cycles.seed = 0
    scn.cycles.max_bounces = 6
    scn.cycles.diffuse_bounces = 4
    scn.cycles.glossy_bounces = 1
    scn.view_settings.view_transform, scn.view_settings.look, scn.view_settings.exposure = VIEW_TRANSFORM


def bake_group(scn, obj, size, samples):
    img = bpy.data.images.new(f"bake_{obj.name}_float", size, size, float_buffer=True, alpha=False)
    img.colorspace_settings.name = 'Linear Rec.709'
    nodes = []
    for m in obj.data.materials:
        nt = m.node_tree
        n = nt.nodes.new("ShaderNodeTexImage")
        n.image = img
        nt.nodes.active = n
        nodes.append((nt, n))
    ec.select_only(obj)
    t0 = time.time()
    bpy.ops.object.bake(type='COMBINED', pass_filter={'DIRECT', 'INDIRECT', 'DIFFUSE', 'EMIT'}, margin=16,
                        margin_type='EXTEND', use_clear=True, target='IMAGE_TEXTURES')
    dt = time.time() - t0
    out = os.path.join(HERE, "textures", f"{SCOPE}_{obj.name}.jpg")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    s = scn.render.image_settings
    s.file_format = 'JPEG'
    s.quality = 92
    s.color_mode = 'RGB'
    img.save_render(out, scene=scn)
    for nt, n in nodes:
        nt.nodes.remove(n)
    bpy.data.images.remove(img)
    baked = ec.load_image(out, 'sRGB', name=f"baked_{obj.name}")
    baked.use_fake_user = True
    obj["bakedImage"] = baked.name
    return {"path": ec.rel(out), "size": [size, size], "sha256": ec.sha256_file(out),
            "bytes": os.path.getsize(out), "bakeSeconds": round(dt, 1), "samples": samples}


def main():
    a = parse()
    scn = ec.reset_scene()
    star_path = starfield_texture(os.path.join(HERE, "textures", "room_starfield.jpg"))
    star_img = ec.load_image(star_path, 'sRGB', "room_starfield")
    star_img.use_fake_user = True
    groups = {
        "floor": Group("floor", ["floor"]),
        "structure_shell": Group("structure_shell", ["mineral_white", "slate_seam", "channel", "downlight", "ring"]),
        "structure_walls": Group("structure_walls", ["mineral_white", "slate", "vestibule", "wallwash",
                                                     "aperture_glow", "marker"]),
        "displays": Group("displays", ["mineral_white", "brass", "glow"]),
        "aperture": Group("aperture", ["starfield"], image=star_img),
    }
    build_floor(groups["floor"])
    build_shell(groups["structure_shell"])
    build_side_walls(groups["structure_walls"])
    build_rear_wall(groups["structure_walls"])
    build_front_wall(groups["structure_walls"])
    build_end_wall_panels(groups["structure_walls"])
    build_displays(groups["displays"])
    build_aperture(groups["aperture"])
    objs = {k: finish_group(g, smooth=(k != "aperture")) for k, g in groups.items()}
    objs["aperture"]["bakedImage"] = star_img.name   # not baked: exported with its own emissive texture
    for k, o in objs.items():
        if k != "aperture":
            lightmap_uv(o, 0.003)
    lights(scn)
    layout_empties(scn)
    setup_cycles(scn, a.device, a.samples)
    tris = {k: ec.triangle_count([o]) for k, o in objs.items()}
    bakes = {}
    if not a.no_bake:
        for k in ("floor", "structure_shell", "structure_walls", "displays"):
            bakes[k] = bake_group(scn, objs[k], a.atlas, a.samples)
            print("BAKED", k, bakes[k])
    blend = os.path.join(HERE, "earth_archive.blend")
    bpy.context.preferences.filepaths.save_version = 0
    for sc in bpy.data.screens:  # factory UI file browsers default to $HOME: never store a personal path
        for area in sc.areas:
            for sp in area.spaces:
                if getattr(sp, "params", None) is not None and hasattr(sp.params, "directory"):
                    sp.params.directory = b"/" * 1100
                    sp.params.directory = b"//"
    scn.render.filepath = "//"
    bpy.ops.wm.save_as_mainfile(filepath=blend, compress=True, relative_remap=True)
    ec.write_json(os.path.join(HERE, "room-build-record.json"), {
        "scope": SCOPE,
        "script": "art/room/build_room.py",
        "args": {"samples": a.samples, "atlas": a.atlas, "device": a.device, "bake": not a.no_bake},
        "blend": {"path": ec.rel(blend), "bytes": os.path.getsize(blend)},
        "groups": {k: {"triangles": tris[k], "materials": [m.name for m in objs[k].data.materials]} for k in objs},
        "bakes": bakes,
        "starfield": {"path": ec.rel(star_path), "size": [2048, 1024], "sha256": ec.sha256_file(star_path),
                      "bytes": os.path.getsize(star_path), "seed": 1969},
        "bake": {"type": "COMBINED", "passes": ["DIRECT", "INDIRECT", "DIFFUSE", "EMIT"], "margin": 16,
                 "viewTransform": VIEW_TRANSFORM, "jpegQuality": 92, "engine": "Cycles", "maxBounces": 6,
                 "diffuseBounces": 4, "seed": 0},
        "palette": PALETTE, "emission": EMIT, "spots": SPOT, "worldFill": WORLD_FILL,
        "versions": ec.versions(),
    })
    print("BUILD_DONE", blend, tris)


if __name__ == "__main__":
    main()
