"""Shared helpers for the Earth Collection Blender scripts (headless Blender 5.1.2, bundled Python).

Imported by process_exhibit.py, author_<id>.py, render_thumbnail.py and the room scripts after
`sys.path.insert(0, <art/tools>)`. Nothing here touches files outside the paths a caller passes in.

Conventions (runtime = glTF): +Y up, metres, front of an object = +Z, right = +X.
Blender: +Z up, metres (unit scale 1.0), front = -Y. The exporter maps Blender (x, y, z) -> glTF (x, z, -y).
A yaw of theta degrees about glTF +Y is the same rotation as theta about Blender +Z.
"""
import hashlib
import json
import math
import os
import subprocess
import sys

import bpy
import bmesh
from mathutils import Matrix, Vector

TOOLS_DIR = os.path.dirname(os.path.abspath(__file__))
ART_DIR = os.path.dirname(TOOLS_DIR)
REPO_DIR = os.path.dirname(ART_DIR)

# The one exporter configuration used for every runtime GLB (brief: extension-free, embedded images only).
EXPORT_SETTINGS = dict(
    export_format='GLB', export_yup=True, export_apply=True,
    export_image_format='AUTO', export_jpeg_quality=90, export_draco_mesh_compression_enable=False,
    export_use_gltfpack=False, export_image_add_webp=False, export_texcoords=True, export_normals=True,
    export_tangents=False, export_materials='EXPORT', export_vertex_color='NONE', export_animations=False,
    export_skins=False, export_morph=False, export_cameras=False, export_lights=False, export_extras=False,
    use_selection=True,
)


def export_settings_digest():
    return hashlib.sha256(json.dumps(EXPORT_SETTINGS, sort_keys=True).encode()).hexdigest()


# Accepted exhibit look (established on gascan + smartphone, applied to all later items).
# Values are glTF metallic-roughness factors. Colours are linear RGB unless named *_srgb.
LOOK = {
    "plastic_gloss_roughness": (0.35, 0.45),
    "plastic_matte_roughness": (0.50, 0.62),
    "metal_satin_roughness": (0.25, 0.40),
    "glass_roughness": (0.04, 0.10),
    "paper_fabric_roughness": (0.80, 0.95),
    "saturation_note": "moderate, museum-archival: no neon; hero colours kept but not pushed",
}


def srgb_to_linear(c):
    c = c / 255.0 if c > 1.0 else c
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def srgb(r, g, b, a=1.0):
    """sRGB 0-255 (or 0-1) triple -> linear RGBA tuple for Principled BSDF / glTF factors."""
    return (srgb_to_linear(r), srgb_to_linear(g), srgb_to_linear(b), a)


def versions():
    import addon_utils
    gltf_version = None
    for mod in addon_utils.modules():
        if mod.__name__ == "io_scene_gltf2":
            gltf_version = ".".join(str(v) for v in mod.bl_info.get("version", ()))
    return {"blender": bpy.app.version_string, "blenderHash": bpy.app.build_hash.decode() if isinstance(bpy.app.build_hash, bytes) else str(bpy.app.build_hash),
            "io_scene_gltf2": gltf_version, "python": sys.version.split()[0]}


def magick_version():
    try:
        out = subprocess.run(["magick", "-version"], capture_output=True, text=True, check=True).stdout
        return out.splitlines()[0].replace("Version: ", "").strip()
    except Exception:
        return None


LABEL_FONT_CANDIDATES = [os.environ.get("EC_LABEL_FONT", ""), "/System/Library/Fonts/Supplemental/Arial.ttf",
                         "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]


def label_font():
    """A TrueType font for ImageMagick labels on inspection sheets / contact sheets (host font, not shipped)."""
    for f in LABEL_FONT_CANDIDATES:
        if f and os.path.isfile(f):
            return f
    return None


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def rel(path):
    """Repo-relative path for records (never a personal absolute path)."""
    p = os.path.abspath(path)
    if p.startswith(REPO_DIR + os.sep):
        return os.path.relpath(p, REPO_DIR)
    home = os.path.expanduser("~")
    if p.startswith(home + os.sep):
        return "<HOME>/" + os.path.relpath(p, home)
    return p


def script_args():
    return sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scn = bpy.context.scene
    scn.unit_settings.system = 'METRIC'
    scn.unit_settings.scale_length = 1.0
    return scn


def link(obj, collection=None):
    (collection or bpy.context.scene.collection).objects.link(obj)
    return obj


def select_only(objs):
    bpy.ops.object.select_all(action='DESELECT')
    objs = objs if isinstance(objs, (list, tuple)) else [objs]
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]


def world_bounds(objs):
    """Exact bounds of evaluated mesh vertices in Blender world space."""
    dg = bpy.context.evaluated_depsgraph_get()
    lo = Vector((math.inf,) * 3)
    hi = Vector((-math.inf,) * 3)
    for o in objs:
        if o.type != 'MESH':
            continue
        ev = o.evaluated_get(dg)
        me = ev.to_mesh()
        mw = o.matrix_world
        for v in me.vertices:
            w = mw @ v.co
            lo = Vector((min(lo.x, w.x), min(lo.y, w.y), min(lo.z, w.z)))
            hi = Vector((max(hi.x, w.x), max(hi.y, w.y), max(hi.z, w.z)))
        ev.to_mesh_clear()
    return lo, hi


def ground_and_centre(objs, origin="footprint-centre", offset=(0.0, 0.0)):
    """Translate objects so the lowest vertex sits on Blender z = 0 (glTF y = 0) and the footprint bbox
    centre is at x = y = 0 (glTF x = z = 0), then apply the translation into the mesh data."""
    lo, hi = world_bounds(objs)
    shift = Vector((-(lo.x + hi.x) / 2 + offset[0], -(lo.y + hi.y) / 2 + offset[1], -lo.z))
    for o in objs:
        o.location += shift
    bpy.context.view_layer.update()
    select_only(objs)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    return shift


def triangle_count(objs):
    dg = bpy.context.evaluated_depsgraph_get()
    n = 0
    for o in objs:
        if o.type != 'MESH':
            continue
        ev = o.evaluated_get(dg)
        me = ev.to_mesh()
        n += sum(len(p.vertices) - 2 for p in me.polygons)
        ev.to_mesh_clear()
    return n


def load_image(path, colorspace='sRGB', name=None):
    img = bpy.data.images.load(os.path.abspath(path), check_existing=False)
    img.name = name or os.path.basename(path)
    img.colorspace_settings.name = colorspace
    return img


def pbr_material(name, base_color=(0.8, 0.8, 0.8, 1.0), metallic=0.0, roughness=0.5, base_image=None,
                 normal_image=None, normal_strength=1.0, mr_image=None, emissive=None, emissive_image=None,
                 emissive_strength=1.0, uv_map=None):
    """A Principled BSDF material the glTF exporter maps 1:1 onto metallic-roughness PBR.
    Factors are always set explicitly (the museum runtime's default is dark rough metal)."""
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    bsdf.inputs["Base Color"].default_value = base_color
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    uv = None
    if uv_map:
        uv = nt.nodes.new("ShaderNodeUVMap")
        uv.uv_map = uv_map

    def tex(img, noncolor=False):
        t = nt.nodes.new("ShaderNodeTexImage")
        t.image = img
        t.interpolation = 'Linear'
        if uv is not None:
            nt.links.new(uv.outputs["UV"], t.inputs["Vector"])
        return t

    if base_image is not None:
        t = tex(base_image)
        if tuple(base_color[:3]) != (1.0, 1.0, 1.0):
            mix = nt.nodes.new("ShaderNodeMix")
            mix.data_type = 'RGBA'
            mix.blend_type = 'MULTIPLY'
            mix.inputs["Factor"].default_value = 1.0
            nt.links.new(t.outputs["Color"], mix.inputs["A"])
            mix.inputs["B"].default_value = base_color
            nt.links.new(mix.outputs["Result"], bsdf.inputs["Base Color"])
        else:
            nt.links.new(t.outputs["Color"], bsdf.inputs["Base Color"])
    if normal_image is not None:
        normal_image.colorspace_settings.name = 'Non-Color'
        t = tex(normal_image)
        nm = nt.nodes.new("ShaderNodeNormalMap")
        nm.inputs["Strength"].default_value = normal_strength
        if uv_map:
            nm.uv_map = uv_map
        nt.links.new(t.outputs["Color"], nm.inputs["Color"])
        nt.links.new(nm.outputs["Normal"], bsdf.inputs["Normal"])
    if mr_image is not None:
        mr_image.colorspace_settings.name = 'Non-Color'
        t = tex(mr_image)
        sep = nt.nodes.new("ShaderNodeSeparateColor")
        nt.links.new(t.outputs["Color"], sep.inputs["Color"])
        nt.links.new(sep.outputs["Green"], bsdf.inputs["Roughness"])
        nt.links.new(sep.outputs["Blue"], bsdf.inputs["Metallic"])
    if emissive is not None or emissive_image is not None:
        bsdf.inputs["Emission Strength"].default_value = emissive_strength
        if emissive_image is not None:
            t = tex(emissive_image)
            nt.links.new(t.outputs["Color"], bsdf.inputs["Emission Color"])
        else:
            bsdf.inputs["Emission Color"].default_value = emissive
    return m


def export_glb(objs, out_path):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    select_only(objs)
    bpy.ops.export_scene.gltf(filepath=os.path.abspath(out_path), **EXPORT_SETTINGS)
    return sha256_file(out_path)


def inspect_glb(path):
    sys.path.insert(0, TOOLS_DIR)
    import inspect_glb as ig
    return ig.inspect(path)


def write_json(path, data):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=1, sort_keys=False)
        fh.write("\n")


# ---------------------------------------------------------------- bmesh building helpers (for authored items)

def new_mesh_object(name, bm, materials=(), smooth_angle_deg=None):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    obj = bpy.data.objects.new(name, me)
    link(obj)
    for m in materials:
        me.materials.append(m)
    if smooth_angle_deg is not None:
        select_only(obj)
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(smooth_angle_deg), keep_sharp_edges=True)
    return obj


def rounded_rect(hw, hh, r, seg):
    """Counter-clockwise outline of a rounded rectangle (half sizes hw, hh; corner radius r) in the XY plane.
    Returns 4*(seg+1) points starting at the right edge."""
    r = max(min(r, hw, hh), 0.0)
    pts = []
    corners = [(hw - r, hh - r, 0.0), (-hw + r, hh - r, 90.0), (-hw + r, -hh + r, 180.0), (hw - r, -hh + r, 270.0)]
    for cx, cy, a0 in corners:
        for i in range(seg + 1):
            a = math.radians(a0 + 90.0 * i / seg)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def stadium(hw, hh, seg):
    """Counter-clockwise outline of a stadium (pill): two semicircles of seg+1 points joined by straight sides.
    Long axis on Y when hh > hw, else on X. hw == hh gives a circle of 2*seg points."""
    pts = []
    if hh >= hw:
        r, c = hw, hh - hw
        for i in range(seg + 1):
            a = math.pi * i / seg
            pts.append((r * math.cos(a), c + r * math.sin(a)))
        for i in range(seg + 1):
            a = math.pi + math.pi * i / seg
            pts.append((r * math.cos(a), -c + r * math.sin(a)))
    else:
        r, c = hh, hw - hh
        for i in range(seg + 1):
            a = -math.pi / 2 + math.pi * i / seg
            pts.append((c + r * math.cos(a), r * math.sin(a)))
        for i in range(seg + 1):
            a = math.pi / 2 + math.pi * i / seg
            pts.append((-c + r * math.cos(a), r * math.sin(a)))
    out = []
    for p in pts:  # drop coincident neighbours (circle case)
        if not out or (abs(p[0] - out[-1][0]) > 1e-9 or abs(p[1] - out[-1][1]) > 1e-9):
            out.append(p)
    if abs(out[0][0] - out[-1][0]) < 1e-9 and abs(out[0][1] - out[-1][1]) < 1e-9:
        out.pop()
    return out


def circle(r, n):
    return [(r * math.cos(2 * math.pi * i / n), r * math.sin(2 * math.pi * i / n)) for i in range(n)]


def write_png_rgb(path, width, height, rows):
    """Deterministic 8-bit RGB PNG writer (stdlib zlib). rows: list of `height` bytes objects, top row first,
    each 3*width long."""
    import struct
    import zlib
    raw = b"".join(b"\x00" + bytes(r) for r in rows)

    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)


def loft_rings(bm, rings, cap_start=True, cap_end=True, mat_for_band=None, cap_mats=(0, 0)):
    """rings: list of lists of 3D points with equal counts. Creates quads between consecutive rings and
    optional ngon caps. Returns (bands_faces, cap_start_face, cap_end_face). Winding: rings ordered from the
    front cap (first) to the back cap (last); each ring counter-clockwise when viewed from the first cap side."""
    vrings = [[bm.verts.new(p) for p in ring] for ring in rings]
    faces = []
    n = len(rings[0])
    for k in range(len(vrings) - 1):
        a, b = vrings[k], vrings[k + 1]
        for i in range(n):
            j = (i + 1) % n
            f = bm.faces.new((a[i], b[i], b[j], a[j]))
            if mat_for_band is not None:
                f.material_index = mat_for_band(k)
            faces.append(f)
    cs = ce = None
    if cap_start:
        cs = bm.faces.new(list(reversed(vrings[0])))
        cs.material_index = cap_mats[0]
    if cap_end:
        ce = bm.faces.new(vrings[-1])
        ce.material_index = cap_mats[1]
    return faces, cs, ce
