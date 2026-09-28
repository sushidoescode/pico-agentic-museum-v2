"""Anchor convention check (Blender render, inspection only): place exhibit GLBs on the room anchors exactly as
room-layout.json describes, and render from viewpoints.

Usage:
  Blender -b --factory-startup --python art/room/check_anchors.py -- --out DIR [--viewpoints arrival,daily,...]
      [--stand-in quencher,gascan,...]   # ids to borrow for empty anchors (scale check only)

For each anchor: exhibit = assets/exhibit-assets.json item with that id (or a borrowed stand-in, labelled in the
log), scale = displayScale * scaleToRealSize, yaw about +Y, translation = anchor position + the lift that puts the
exhibit's lowest point on the plinth top (0 for footprint-centre items). Anchors without any GLB get a small
cone pointing along the exhibit front (sin yaw, 0, cos yaw). Room = the runtime room GLB (emissive only);
exhibits are lit by one soft warm area light per anchor, standing in for the app's runtime lights.
"""
import argparse
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "tools"))
import bpy  # noqa: E402
from mathutils import Matrix, Vector  # noqa: E402

import ec_blender as ec  # noqa: E402
import room_layout as RL  # noqa: E402

ASSETS = os.path.join(ec.REPO_DIR, "native-staging/app/src/main/assets")


def rt(x, y, z):
    return Vector((x, -z, y))


def import_glb(path):
    before = set(bpy.data.objects)
    bpy.ops.import_scene.gltf(filepath=path)
    new = [o for o in bpy.data.objects if o not in before]
    root = bpy.data.objects.new(os.path.basename(path), None)
    bpy.context.scene.collection.objects.link(root)
    for o in new:
        if o.parent is None:
            o.parent = root
    return root, new


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--viewpoints", default="arrival,daily,connected,domestic")
    ap.add_argument("--stand-in", default="")
    ap.add_argument("--size", default="1600x900")
    ap.add_argument("--pitch", type=float, default=-10.0)
    ap.add_argument("--camera", action="append", default=[], help="name:x,eyeY,z,yawDeg,pitchDeg (runtime)")
    a = ap.parse_args(ec.script_args())
    layout = json.load(open(os.path.join(ASSETS, "room", "room-layout.json")))
    man = {it["id"]: it for it in json.load(open(os.path.join(ec.REPO_DIR, "assets/exhibit-assets.json")))["items"]}
    stand = [s for s in a.stand_in.split(",") if s]
    scn = ec.reset_scene()
    for g in layout["room"]["glb"]:
        import_glb(os.path.join(ASSETS, g["path"]))
    log = []
    k = 0
    for anc in layout["anchors"]:
        ex = man.get(anc["id"])
        label = anc["id"]
        if ex is None and stand:
            ex = man[stand[k % len(stand)]]
            label = f"{anc['id']} <- stand-in {ex['id']}"
            k += 1
        x, y, z = anc["position"]
        yaw = math.radians(anc["yawDeg"])
        if ex is None:
            bpy.ops.mesh.primitive_cone_add(radius1=0.03, depth=0.08, location=rt(x, y + 0.03, z))
            c = bpy.context.active_object
            c.rotation_euler = rt(math.sin(yaw), 0, math.cos(yaw)).to_track_quat('Z', 'Y').to_euler()
            log.append(f"{anc['name']}: marker only")
            continue
        root, objs = import_glb(os.path.join(ASSETS, ex["runtimePath"]))
        s = anc["displayScale"] * ex.get("scaleToRealSize", 1.0)
        lift = -ex["boundsMetres"]["min"][1] * s
        root.matrix_world = Matrix.Translation(rt(x, y + lift, z)) @ Matrix.Rotation(yaw, 4, 'Z') @ Matrix.Scale(s, 4)
        L = bpy.data.lights.new(f"key_{anc['id']}", 'AREA')
        L.energy, L.size, L.color = 25.0, 0.6, (1.0, 0.86, 0.7)
        lo = bpy.data.objects.new(L.name, L)
        scn.collection.objects.link(lo)
        lo.location = rt(x + 1.2 * math.sin(yaw), y + 1.3, z + 1.2 * math.cos(yaw))
        lo.rotation_euler = (rt(x, y + 0.1, z) - lo.location).to_track_quat('-Z', 'Y').to_euler()
        log.append(f"{anc['name']}: {label}, scale {s:.4f}, lift {lift:.4f}, yaw {anc['yawDeg']}")
    w = bpy.data.worlds.new("dim")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs[0].default_value = (0.05, 0.05, 0.055, 1)
    w.node_tree.nodes["Background"].inputs[1].default_value = 1.0
    scn.world = w
    prefs = bpy.context.preferences.addons['cycles'].preferences
    prefs.compute_device_type = 'METAL'
    prefs.get_devices()
    for d in prefs.devices:
        d.use = d.type == 'METAL'
    scn.render.engine = 'CYCLES'
    scn.cycles.device = 'GPU'
    scn.cycles.samples = 128
    scn.view_settings.view_transform = 'Standard'
    wd, ht = (int(v) for v in a.size.split("x"))
    scn.render.resolution_x, scn.render.resolution_y = wd, ht
    cam_data = bpy.data.cameras.new("cam")
    cam_data.sensor_fit = 'HORIZONTAL'
    cam_data.angle = math.radians(RL.HFOV_DEG)
    cam = bpy.data.objects.new("cam", cam_data)
    scn.collection.objects.link(cam)
    scn.camera = cam
    vps = {v["name"]: dict(v, eye=RL.EYE_HEIGHT, pitch=a.pitch) for v in layout["viewpoints"]}
    names = [n for n in a.viewpoints.split(",") if n]
    for spec in a.camera:
        nm, vals = spec.split(":")
        cx, cy, cz, cyaw, cp = (float(v) for v in vals.split(","))
        vps[nm] = {"position": [cx, 0.0, cz], "yawDeg": cyaw, "eye": cy, "pitch": cp}
        names.append(nm)
    os.makedirs(a.out, exist_ok=True)
    for name in names:
        vp = vps[name]
        x, y, z = vp["position"]
        yaw = math.radians(vp["yawDeg"])
        pitch = math.radians(vp["pitch"])
        cam.location = rt(x, vp["eye"], z)
        fwd = rt(-math.sin(yaw) * math.cos(pitch), math.sin(pitch), -math.cos(yaw) * math.cos(pitch))
        cam.rotation_euler = fwd.to_track_quat('-Z', 'Y').to_euler()
        scn.render.filepath = os.path.join(a.out, f"anchors-{name}.png")
        bpy.ops.render.render(write_still=True)
        print("ANCHOR_RENDER", scn.render.filepath)
    for line in log:
        print("ANCHOR", line)


if __name__ == "__main__":
    main()
