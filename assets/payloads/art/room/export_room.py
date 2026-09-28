"""Export the archive room runtime GLB from the built .blend and write the layout + provenance records.

Usage:
  Blender -b --factory-startup --python art/room/export_room.py -- [--generated-at ISO8601]

Reads  art/room/earth_archive.blend (from build_room.py), its baked atlases and starfield in art/room/textures/.
Writes native-staging/app/src/main/assets/room/earth_archive.glb
       native-staging/app/src/main/assets/room/room-layout.json   (schema 1: room GLBs, bays + signs, 18 anchors
                                                                   with plaques, 4 viewpoints)
       assets/room-assets.json                                     (room provenance + measured identities)
Nodes: floor, structure_shell, structure_walls, displays (each: baseColor black, metallic 0, roughness 0.95,
emissive = its baked atlas, emissiveFactor 1) and aperture (the starfield card: the same material form with the
packaged starfield as emissive texture). No lights, cameras, empties, extras or extensions are exported.
Mirror to native/ with art/tools/sync_native_assets.py (root runs it while holding native/).
"""
import argparse
import datetime
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "tools"))
import bpy  # noqa: E402

import ec_blender as ec  # noqa: E402
import room_layout as RL  # noqa: E402

GROUP_ORDER = ["floor", "structure_shell", "structure_walls", "displays", "aperture"]
ASSETS = os.path.join(ec.REPO_DIR, "native-staging/app/src/main/assets")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default=os.path.join(ASSETS, "room"))
    ap.add_argument("--generated-at", default=None)
    a = ap.parse_args(ec.script_args())
    blend = os.path.join(HERE, "earth_archive.blend")
    bpy.ops.wm.open_mainfile(filepath=blend)
    groups = [bpy.data.objects[n] for n in GROUP_ORDER if n in bpy.data.objects]
    for obj in groups:
        img = bpy.data.images[obj["bakedImage"]]
        img.colorspace_settings.name = 'sRGB'
        m = ec.pbr_material(f"{obj.name}_baked" if obj.name != "aperture" else "aperture_card",
                            (0.0, 0.0, 0.0, 1.0), metallic=0.0, roughness=0.95, emissive_image=img, emissive_strength=1.0)
        obj.data.materials.clear()
        obj.data.materials.append(m)
        for p in obj.data.polygons:
            p.material_index = 0
    out = os.path.join(os.path.abspath(a.out_dir), "earth_archive.glb")
    ec.export_glb(groups, out)
    info = ec.inspect_glb(out)
    gen = a.generated_at or datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    glb_entry = {
        "path": os.path.relpath(out, ASSETS), "sha256": info["sha256"], "bytes": info["bytes"],
        "triangles": info["triangles"], "nodes": info["meshNodes"], "materials": [m["name"] for m in info["materials"]],
        "textures": [{"name": im["name"], "size": f"{im['width']}x{im['height']}", "format": im["mimeType"],
                      "bytes": im["bytes"]} for im in info["images"]],
        "boundsMetres": {"min": info["boundsMetres"]["min"], "max": info["boundsMetres"]["max"]},
        "extensionsUsed": info["extensionsUsed"],
    }
    anchors = RL.anchors()
    signs = {s["bay"]: {k: v for k, v in s.items() if k != "bay"} for s in RL.signs()}
    bays = []
    for b in RL.BAYS:
        bays.append({"id": b["id"], "section": b["section"], "title": b["title"], "viewpoint": b["viewpoint"],
                     "anchors": [x["name"] for x in anchors if x["bay"] == b["id"]],
                     "furniture": "curved segmented bench with risers" if b["kind"] == "bench" else
                     "low curved floor platform with plinths, a round pedestal and lecterns",
                     "arcCentre": [b["centre"][0], 0.0, b["centre"][1]], "arcRadiusM": b["radius"],
                     "sign": signs[b["id"]]})
    layout = {
        "schemaVersion": 1,
        "generatedAt": gen,
        "scope": "room",
        "frame": {"up": "+Y", "units": "metres", "visitorForwardAtYaw0": "-Z",
                  "origin": "arrival point on the floor, centre of the brass circular motif"},
        "yawConvention": "degrees about +Y, right-handed; 0 = facing -Z, +90 = facing -X (turn left). Viewpoint forward = (-sin yaw, 0, -cos yaw). Anchor / plaque / sign yaw rotates the object's local frame (front +Z), so its front = (sin yaw, 0, cos yaw) (toward the visitor).",
        "anchorConvention": "position = where the exhibit's base-centre origin goes (y = plinthTopY); displayScale 1.0 = real size (multiply by the exhibit's scaleToRealSize from assets/exhibit-assets.json).",
        "plaqueConvention": "position = centre of the blank brass plate's front face; yawDeg as anchors; pitchDeg = tilt of the face from vertical, positive = facing up; sizeMetres = plate width x height (a ~0.16 x 0.05 m label fits). No text in any texture.",
        "signConvention": "position = centre of a clean wall area for the bay title app label; yawDeg as anchors (front faces the bay viewpoint); sizeMetres = clean area width x height.",
        "room": {"glb": [glb_entry],
                 "lighting": "baked: baseColor black + emissive atlas (display-referred sRGB through AgX); the aperture card is emissive starfield; no runtime lights needed for the architecture",
                 "enclosure": "closed on all sides: floor, walls, vaulted ceiling, rear wall with the aperture tunnel ending at the starfield card, front wall with a closed entrance vestibule; large surfaces overlap on the outside",
                 "entrance": {"threshold": [0.0, 0.0, 4.5], "marker": [0.55, 1.45, RL.ROOM["vestibule"]["z"][1] - 0.02],
                              "note": "unlabelled illuminated marker on the vestibule's closed door; the real exit is an app control"},
                 "aperture": {"centre": RL.APERTURE["centre"], "sizeMetres": [RL.APERTURE["w"], RL.APERTURE["h"]],
                              "note": "static packaged starfield + small planetary crescent (emissive card); no live sky"}},
        "bays": bays,
        "anchors": anchors,
        "viewpoints": [{k: v for k, v in vp.items() if k != "note"} | {"eyeHeightForRenders": RL.EYE_HEIGHT}
                       for vp in RL.viewpoints()],
    }
    lay_path = os.path.join(os.path.abspath(a.out_dir), "room-layout.json")
    ec.write_json(lay_path, layout)
    rec = json.load(open(os.path.join(HERE, "room-build-record.json")))
    assets = {
        "schemaVersion": 1,
        "generatedAt": gen,
        "purpose": "Earth Collection archive room runtime assets: identities, measured geometry, provenance",
        "scope": "room",
        "runtime": {"glb": [glb_entry], "layout": {"path": os.path.relpath(lay_path, ASSETS),
                                                   "sha256": ec.sha256_file(lay_path), "bytes": os.path.getsize(lay_path)}},
        "source": {"kind": "blender-original",
                   "note": "authored from the written brief and the project's own generated concept image (viewed only, never uploaded); no reference geometry, image or provider"},
        "build": {"script": "art/room/build_room.py", "layout": "art/room/room_layout.py", "args": rec["args"],
                  "blend": rec["blend"], "groups": rec["groups"], "bakes": rec["bakes"], "starfield": rec["starfield"],
                  "bake": rec["bake"], "buildRecord": "art/room/room-build-record.json",
                  "reproducibility": "regenerable from build_room.py + export_room.py; the Metal GPU bake is not byte-identical run to run (texel differences <= a few /255), geometry and UVs are; each export records the atlases it used"},
        "export": {"script": "art/room/export_room.py", "exporterSettings": ec.EXPORT_SETTINGS,
                   "exporterSettingsSha256": ec.export_settings_digest(),
                   "material": "per node: baseColorFactor [0,0,0,1], metallic 0, roughness 0.95, emissiveTexture = baked atlas (aperture: starfield), emissiveFactor [1,1,1]"},
        "axisConversion": "authored in runtime coordinates, stored in Blender as (x, -z, y); exported with export_yup=True back to runtime (x, y, z)",
        "supersedes": {"room/earth_archive_segment_v1.glb": "Phase 1 segment (native-checked); replaced by room/earth_archive.glb"},
        "versions": ec.versions(),
    }
    ec.write_json(os.path.join(ec.REPO_DIR, "assets", "room-assets.json"), assets)
    print("EXPORT_RESULT", json.dumps(glb_entry))


if __name__ == "__main__":
    main()
