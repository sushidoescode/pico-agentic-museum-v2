"""Inspection sheet for a runtime GLB: six views on a grey background (headless Blender 5.1.2, Cycles CPU).

Usage:
  Blender -b --factory-startup --python art/tools/render_inspect.py -- <in.glb> <out.png> [--size 384] [--title T]

Views (glTF frame, +Y up, +Z front): front (+Z), right (+X), back (-Z), left (-X), three-quarter (thumbnail
angle) and top (front at the bottom of the image). Same studio lights as the thumbnails. The per-view PNGs and a
labelled montage (ImageMagick) are written next to <out.png>. For human/agent inspection only, not shipped.
"""
import argparse
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402

import ec_blender as ec  # noqa: E402
import render_thumbnail as rt  # noqa: E402

VIEWS = [("front +Z", 0, 4), ("right +X", 90, 4), ("back -Z", 180, 4), ("left -X", -90, 4),
         ("three-quarter", 35, 22), ("top (front down)", 0, 89.5)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("glb")
    ap.add_argument("out")
    ap.add_argument("--size", type=int, default=384)
    ap.add_argument("--samples", type=int, default=48)
    ap.add_argument("--title", default=None)
    a = ap.parse_args(ec.script_args())
    scn = ec.reset_scene()
    bpy.ops.import_scene.gltf(filepath=os.path.abspath(a.glb))
    objs = [o for o in scn.objects if o.type == 'MESH']
    pts = rt.all_world_verts(objs)
    rt.setup_render(scn, a.size, a.samples, transparent=True)
    rt.studio_world(scn)  # same ambient as the thumbnails; a grey backdrop is composited below
    stem = os.path.splitext(os.path.abspath(a.out))[0]
    files = []
    for i, (label, az, el) in enumerate(VIEWS):
        for o in [o for o in scn.objects if o.type in ('CAMERA', 'LIGHT')]:
            bpy.data.objects.remove(o, do_unlink=True)
        target, radius, _ = rt.fit_camera(scn, pts, az, el, 0.86, 70.0)
        rt.studio(scn, target, radius)
        f = f"{stem}-v{i}.png"
        scn.render.filepath = f
        bpy.ops.render.render(write_still=True)
        files.append((label, f))
    for label, f in files:  # flatten each view onto a mid-grey backdrop so silhouettes read
        subprocess.run(["magick", f, "-background", "#5c5c5e", "-flatten", f], check=True)
    args = ["magick", "montage"]
    for label, f in files:
        args += ["-label", label, f]
    args += ["-tile", "6x1", "-geometry", "+2+2", "-background", "#202020", "-fill", "white", "-pointsize", "16"]
    if ec.label_font():
        args += ["-font", ec.label_font()]
    if a.title:
        args += ["-title", a.title]
    subprocess.run(args + [a.out], check=True)
    print("INSPECT_RESULT", a.out)


if __name__ == "__main__":
    main()
