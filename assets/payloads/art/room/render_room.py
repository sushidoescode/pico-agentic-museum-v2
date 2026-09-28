"""Render the archive room from named viewpoints (headless Blender 5.1.2). Inspection only, not shipped.

Usage:
  Blender -b --factory-startup --python art/room/render_room.py -- --blend art/room/earth_archive.blend \
      --out DIR [--viewpoints arrival,daily,connected,domestic] [--size 1600x900] [--hfov 72] [--samples 256]
  Blender -b --factory-startup --python art/room/render_room.py -- --glb native-staging/.../room/<file>.glb \
      --out DIR [...]

--blend renders the authoring scene with its lights (Cycles, AgX: the look the bake captured).
--glb imports the exported runtime GLB into an empty, unlit scene (black world, no lights) and renders emission
only with the Standard view transform, i.e. the baked display colours as they would appear on a renderer that
shows emissive texels 1:1 without tone mapping. Camera: viewpoint floor position + eye height 1.75 m, yaw per
art/room/room_layout.py (0 = facing -Z, +90 = facing -X), level horizon.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "tools"))
import bpy  # noqa: E402
from mathutils import Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import room_layout as RL  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--blend")
    g.add_argument("--glb")
    ap.add_argument("--out", required=True)
    ap.add_argument("--viewpoints", default="arrival,daily,connected,domestic")
    ap.add_argument("--size", default="1600x900")
    ap.add_argument("--hfov", type=float, default=RL.HFOV_DEG)
    ap.add_argument("--samples", type=int, default=256)
    ap.add_argument("--pitch", type=float, default=-6.0, help="camera pitch in degrees (negative looks down)")
    ap.add_argument("--tag", default="")
    ap.add_argument("--camera", action="append", default=[],
                    help="extra named view name:x,y,z,yawDeg,pitchDeg in runtime coords (y = camera height)")
    a = ap.parse_args(ec.script_args())
    if a.blend:
        bpy.ops.wm.open_mainfile(filepath=os.path.abspath(a.blend))
        scn = bpy.context.scene
        mode = "blend"
    else:
        scn = ec.reset_scene()
        bpy.ops.import_scene.gltf(filepath=os.path.abspath(a.glb))
        w = bpy.data.worlds.new("black")
        w.use_nodes = True
        w.node_tree.nodes["Background"].inputs[1].default_value = 0.0
        scn.world = w
        scn.render.engine = 'CYCLES'
        mode = "glb"
    prefs = bpy.context.preferences.addons['cycles'].preferences
    prefs.compute_device_type = 'METAL'
    prefs.get_devices()
    for d in prefs.devices:
        d.use = d.type == 'METAL'
    scn.render.engine = 'CYCLES'
    scn.cycles.device = 'GPU'
    scn.cycles.samples = a.samples if mode == "blend" else 16
    scn.cycles.use_denoising = mode == "blend"
    scn.cycles.seed = 0
    if mode == "glb":
        scn.view_settings.view_transform = 'Standard'
        scn.view_settings.look = 'None'
        scn.view_settings.exposure = 0.0
    w_, h_ = (int(v) for v in a.size.split("x"))
    scn.render.resolution_x, scn.render.resolution_y = w_, h_
    scn.render.resolution_percentage = 100
    scn.render.film_transparent = False
    scn.render.image_settings.file_format = 'PNG'
    scn.render.image_settings.color_mode = 'RGB'
    cam_data = bpy.data.cameras.new("viewcam")
    cam_data.sensor_fit = 'HORIZONTAL'
    cam_data.angle = math.radians(a.hfov)
    cam_data.clip_start = 0.05
    cam_data.clip_end = 100
    cam = bpy.data.objects.new("viewcam", cam_data)
    scn.collection.objects.link(cam)
    scn.camera = cam
    os.makedirs(a.out, exist_ok=True)
    vps = {v["name"]: dict(v, eye=RL.EYE_HEIGHT, pitch=a.pitch) for v in RL.VIEWPOINTS}
    views = [n for n in a.viewpoints.split(",") if n]
    for spec in a.camera:
        nm, vals = spec.split(":")
        x, y, z, yw, pt = (float(v) for v in vals.split(","))
        vps[nm] = {"name": nm, "position": [x, 0.0, z], "yawDeg": yw, "eye": y, "pitch": pt}
        views.append(nm)
    for name in views:
        vp = vps[name]
        x, y, z = vp["position"]
        yaw = math.radians(vp["yawDeg"])
        pitch = math.radians(vp["pitch"])
        cam.location = Vector((x, -z, y + vp["eye"]))
        fwd_rt = Vector((-math.sin(yaw) * math.cos(pitch), math.sin(pitch), -math.cos(yaw) * math.cos(pitch)))
        fwd = Vector((fwd_rt.x, -fwd_rt.z, fwd_rt.y))
        cam.rotation_euler = fwd.to_track_quat('-Z', 'Y').to_euler()
        out = os.path.join(a.out, f"{mode}{a.tag}-{name}.png")
        scn.render.filepath = out
        bpy.ops.render.render(write_still=True)
        print("ROOM_RENDER", out)


if __name__ == "__main__":
    main()
