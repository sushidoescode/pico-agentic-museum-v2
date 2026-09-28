"""Render a catalogue thumbnail from a final runtime GLB (headless Blender 5.1.2, Cycles CPU).

Usage:
  Blender -b --factory-startup --python art/tools/render_thumbnail.py -- <in.glb> <out.png>
      [--size 256] [--samples 128] [--azimuth 35] [--elevation 22] [--fill 0.80] [--lens 70]

Look (fixed for all eighteen): 3/4 view from the front-right (+Z/+X, glTF) slightly above; warm key from the
front-left, soft neutral-cool fill from the front-right, subtle rim from behind; dim neutral ambient; transparent
background (PNG RGBA 8-bit); AgX view transform. The camera is fitted so the object's projected extent fills
`--fill` of the frame and is centred. Cycles runs on the CPU with a fixed seed and OIDN denoising so a re-run on
the same host reproduces the image (single render thread; Blender's PNG metadata is stripped by ImageMagick).
"""
import argparse
import math
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
from mathutils import Vector  # noqa: E402

import ec_blender as ec  # noqa: E402


def parse():
    ap = argparse.ArgumentParser()
    ap.add_argument("glb")
    ap.add_argument("out")
    ap.add_argument("--size", type=int, default=256)
    ap.add_argument("--samples", type=int, default=128)
    ap.add_argument("--azimuth", type=float, default=35.0, help="degrees from glTF +Z toward +X")
    ap.add_argument("--elevation", type=float, default=22.0)
    ap.add_argument("--fill", type=float, default=0.80)
    ap.add_argument("--lens", type=float, default=70.0)
    return ap.parse_args(ec.script_args())


def gltf_dir_to_blender(x, y, z):
    return Vector((x, -z, y))


def all_world_verts(objs):
    dg = bpy.context.evaluated_depsgraph_get()
    pts = []
    for o in objs:
        ev = o.evaluated_get(dg)
        me = ev.to_mesh()
        mw = o.matrix_world
        pts.extend(mw @ v.co for v in me.vertices)
        ev.to_mesh_clear()
    return pts


def setup_render(scn, size, samples, transparent=True):
    scn.render.engine = 'CYCLES'
    scn.cycles.device = 'CPU'
    scn.cycles.samples = samples
    scn.cycles.use_adaptive_sampling = False
    scn.cycles.seed = 0
    scn.cycles.use_denoising = True
    scn.cycles.denoiser = 'OPENIMAGEDENOISE'
    scn.cycles.max_bounces = 8
    scn.render.threads_mode = 'FIXED'   # fixed thread count: a re-run on this host reproduces the image bytes
    scn.render.threads = 1
    scn.render.resolution_x = scn.render.resolution_y = size
    scn.render.resolution_percentage = 100
    scn.render.film_transparent = transparent
    scn.render.image_settings.file_format = 'PNG'
    scn.render.image_settings.color_mode = 'RGBA' if transparent else 'RGB'
    scn.render.image_settings.color_depth = '8'
    scn.render.image_settings.compression = 90
    scn.render.use_stamp = False
    scn.view_settings.view_transform = 'AgX'
    scn.view_settings.look = 'None'
    scn.view_settings.exposure = 0.0
    scn.view_settings.gamma = 1.0


def studio_world(scn, rgb=(0.52, 0.52, 0.54), strength=0.45):
    w = bpy.data.worlds.new("studio")
    w.use_nodes = True
    bg = w.node_tree.nodes["Background"]
    bg.inputs[0].default_value = (*rgb, 1.0)
    bg.inputs[1].default_value = strength
    scn.world = w


def studio(scn, centre, radius):
    def area(name, gltf_dir, colour, power, size):
        L = bpy.data.lights.new(name, 'AREA')
        L.shape = 'DISK'
        L.color = colour
        L.energy = power * radius * radius
        L.size = size * radius
        ob = bpy.data.objects.new(name, L)
        scn.collection.objects.link(ob)
        d = gltf_dir_to_blender(*gltf_dir).normalized()
        ob.location = centre + d * radius * 5.0
        ob.rotation_euler = (centre - ob.location).to_track_quat('-Z', 'Y').to_euler()

    area("key", (-1.0, 1.1, 1.0), (1.0, 0.84, 0.68), 260.0, 3.0)    # warm, front-left, above
    area("fill", (1.3, 0.35, 0.9), (0.86, 0.91, 1.0), 90.0, 5.0)     # soft, front-right, low
    area("rim", (0.5, 0.9, -1.3), (1.0, 0.96, 0.9), 160.0, 2.0)      # subtle, behind


def fit_camera(scn, pts, azimuth, elevation, fill, lens):
    cam_data = bpy.data.cameras.new("cam")
    cam_data.lens = lens
    cam_data.sensor_fit = 'HORIZONTAL'
    cam = bpy.data.objects.new("cam", cam_data)
    scn.collection.objects.link(cam)
    scn.camera = cam
    az, el = math.radians(azimuth), math.radians(elevation)
    d = gltf_dir_to_blender(math.sin(az) * math.cos(el), math.sin(el), math.cos(az) * math.cos(el)).normalized()
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    target = (lo + hi) / 2
    radius = max((hi - lo).length / 2, 1e-4)
    rot = (-d).to_track_quat('-Z', 'Y')
    right = rot @ Vector((1, 0, 0))
    up = rot @ Vector((0, 1, 0))
    fwd = -d
    tan_half = (cam_data.sensor_width / 2) / lens
    dist = radius / math.sin(math.atan(tan_half)) * 1.05
    for _ in range(40):
        loc = target - fwd * dist
        xs, ys = [], []
        for p in pts:
            v = p - loc
            z = v.dot(fwd)
            xs.append(v.dot(right) / (z * tan_half))
            ys.append(v.dot(up) / (z * tan_half))
        cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
        ext = max(max(xs) - min(xs), max(ys) - min(ys)) / 2
        target += right * cx * dist * tan_half * 0.9 + up * cy * dist * tan_half * 0.9
        dist *= 1.0 + 0.9 * (ext / fill - 1.0)
    cam.location = target - fwd * dist
    cam.rotation_euler = rot.to_euler()
    cam_data.clip_start = dist * 0.01
    cam_data.clip_end = dist * 10
    return target, radius, {"extent": ext, "centreOffset": (cx, cy), "distance": dist}


def main():
    a = parse()
    scn = ec.reset_scene()
    bpy.ops.import_scene.gltf(filepath=os.path.abspath(a.glb))
    objs = [o for o in scn.objects if o.type == 'MESH']
    pts = all_world_verts(objs)
    setup_render(scn, a.size, a.samples, transparent=True)
    target, radius, fitinfo = fit_camera(scn, pts, a.azimuth, a.elevation, a.fill, a.lens)
    studio_world(scn)
    studio(scn, target, radius)
    out = os.path.abspath(a.out)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    tmp = out + ".render.png"
    scn.render.filepath = tmp
    bpy.ops.render.render(write_still=True)
    # Blender writes render date/time text chunks; re-encode without metadata so the bytes are reproducible
    subprocess.run(["magick", tmp, "-strip", "-define", "png:exclude-chunks=date,time",
                    "-define", "png:compression-level=9", "-define", "png:color-type=6", "PNG32:" + out], check=True)
    os.remove(tmp)
    print("THUMB_RESULT", a.out, ec.sha256_file(out), fitinfo)


if __name__ == "__main__":
    main()
