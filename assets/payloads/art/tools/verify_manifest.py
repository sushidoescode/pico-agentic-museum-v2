#!/usr/bin/env python3
"""Verify assets/exhibit-assets.json against the files (stdlib only). Exit 0 = all checks pass.

Usage: verify_manifest.py [--require-complete] [--quiet]

For every item: the runtime GLB and thumbnail exist in native-staging AND byte-identical in native; SHA-256 and
bytes match the manifest; the GLB re-parses with a valid layout, no extensions, embedded images only, and its
triangles / materials / textures / bounds match the manifest; budgets hold (triangles 5k-20k ordinary, scooter
<= 30k, <= 3 materials, textures <= 1024 on each edge, GLB <= 3 MB hard / 1.5 MB soft, total <= 25 MB); base on
y = 0 and footprint centred for `footprint-centre` items; every material states roughness explicitly (factor or
texture); thumbnails are 256x256 8-bit RGBA PNGs. Soft limits print WARN and do not fail.
--require-complete additionally requires all eighteen ids. --staging-only skips the native/ mirror comparison.
"""
import argparse
import hashlib
import json
import os
import struct
import sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(TOOLS))
sys.path.insert(0, TOOLS)
import inspect_glb  # noqa: E402
from build_manifest import IDS  # noqa: E402

STAGING = os.path.join(REPO, "native-staging/app/src/main/assets")
NATIVE = os.path.join(REPO, "native/app/src/main/assets")


def sha(path):
    with open(path, "rb") as fh:
        return hashlib.sha256(fh.read()).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--require-complete", action="store_true")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--staging-only", action="store_true",
                    help="skip the native/ mirror comparison (while root holds native/ for a build window)")
    a = ap.parse_args()
    with open(os.path.join(REPO, "assets/exhibit-assets.json")) as fh:
        man = json.load(fh)
    fails, warns = [], []

    def fail(msg):
        fails.append(msg)

    def warn(msg):
        warns.append(msg)

    for k in ("schemaVersion", "generatedAt", "tools", "items"):
        if k not in man:
            fail(f"manifest missing top-level {k}")
    ids = [it["id"] for it in man["items"]]
    if len(set(ids)) != len(ids):
        fail("duplicate ids")
    for i in ids:
        if i not in IDS:
            fail(f"unknown id {i}")
    if a.require_complete and sorted(ids) != sorted(IDS):
        fail(f"incomplete: missing {sorted(set(IDS) - set(ids))}")
    total = 0
    for it in man["items"]:
        i = it["id"]
        for key in ("runtimePath", "runtimeSha256", "runtimeBytes", "triangles", "materials", "textures",
                    "boundsMetres", "realSizeMetres", "origin", "front", "thumbnail", "source", "processing",
                    "inspection"):
            if key not in it:
                fail(f"{i}: missing {key}")
        glb_s = os.path.join(STAGING, it["runtimePath"])
        glb_n = os.path.join(NATIVE, it["runtimePath"])
        if not os.path.exists(glb_s):
            fail(f"{i}: missing {glb_s}")
            continue
        if not a.staging_only and (not os.path.exists(glb_n) or sha(glb_n) != sha(glb_s)):
            fail(f"{i}: native mirror missing or different: {it['runtimePath']}")
        info = inspect_glb.inspect(glb_s)
        total += info["bytes"]
        if info["sha256"] != it["runtimeSha256"] or info["bytes"] != it["runtimeBytes"]:
            fail(f"{i}: runtime sha/bytes mismatch")
        if info["layoutProblems"]:
            fail(f"{i}: layout {info['layoutProblems']}")
        if info["extensionsUsed"] or info["extensionsRequired"]:
            fail(f"{i}: extensions {info['extensionsUsed']} {info['extensionsRequired']}")
        if info["triangles"] != it["triangles"]:
            fail(f"{i}: triangles {info['triangles']} != manifest {it['triangles']}")
        lo, hi = (5000, 30000) if i == "scooter" else (5000, 20000)
        if info["triangles"] > hi:
            fail(f"{i}: {info['triangles']} triangles over budget {hi}")
        if info["triangles"] < lo:
            warn(f"{i}: {info['triangles']} triangles under the {lo} guide")
        if len(info["materials"]) > 3:
            fail(f"{i}: {len(info['materials'])} materials > 3")
        if len(info["materials"]) != len(it["materials"]):
            fail(f"{i}: material count differs from manifest")
        for m in info["materials"]:
            if not (m["roughnessFactorExplicit"] or "metallicRoughness" in m["textures"]):
                fail(f"{i}: material {m['name']} has no explicit roughness")
        if len(info["images"]) != len(it["textures"]):
            fail(f"{i}: texture count differs from manifest")
        for im, t in zip(info["images"], it["textures"]):
            if f"{im['width']}x{im['height']}" != t["size"] or im["bytes"] != t["bytes"]:
                fail(f"{i}: texture {im['name']} differs from manifest")
            if max(im["width"], im["height"]) > 1024:
                fail(f"{i}: texture {im['name']} {im['width']}x{im['height']} > 1024")
        b = info["boundsMetres"]
        if any(abs(x - y) > 1e-5 for x, y in zip(b["min"] + b["max"], it["boundsMetres"]["min"] + it["boundsMetres"]["max"])):
            fail(f"{i}: bounds differ from manifest")
        if it["origin"] == "footprint-centre":
            if abs(b["min"][1]) > 1e-4:
                fail(f"{i}: base not on y = 0 ({b['min'][1]})")
            if abs(b["min"][0] + b["max"][0]) > 1e-3 or abs(b["min"][2] + b["max"][2]) > 1e-3:
                fail(f"{i}: footprint not centred")
        if info["bytes"] > 3_000_000:
            fail(f"{i}: {info['bytes']} B > 3 MB")
        elif info["bytes"] > 1_500_000:
            warn(f"{i}: {info['bytes']} B > 1.5 MB soft limit")
        th = it["thumbnail"]
        ts, tn = os.path.join(STAGING, th["path"]), os.path.join(NATIVE, th["path"])
        if not os.path.exists(ts):
            fail(f"{i}: missing thumbnail {th['path']}")
            continue
        if sha(ts) != th["sha256"] or os.path.getsize(ts) != th["bytes"]:
            fail(f"{i}: thumbnail sha/bytes mismatch")
        if not a.staging_only and (not os.path.exists(tn) or sha(tn) != sha(ts)):
            fail(f"{i}: thumbnail native mirror missing or different")
        with open(ts, "rb") as fh:
            head = fh.read(26)
        w, h, depth, ctype = struct.unpack(">IIBB", head[16:26])
        if head[:8] != b"\x89PNG\r\n\x1a\n" or (w, h, depth, ctype) != (256, 256, 8, 6):
            fail(f"{i}: thumbnail is not a 256x256 8-bit RGBA PNG ({w}x{h}, depth {depth}, type {ctype})")
        if not a.quiet:
            print(f"ok  {i:13s} {info['triangles']:6d} tris  {info['bytes']:8d} B  {len(info['images'])} tex  "
                  f"size {b['max'][0]-b['min'][0]:.3f}x{b['max'][1]-b['min'][1]:.3f}x{b['max'][2]-b['min'][2]:.3f} m")
    if total > 25_000_000:
        fail(f"total exhibit bytes {total} > 25 MB")
    for w_ in warns:
        print("WARN", w_)
    for f_ in fails:
        print("FAIL", f_)
    print(f"{len(man['items'])} items, total {total} B, {len(fails)} failures, {len(warns)} warnings"
          + (" (staging only: native mirror not checked)" if a.staging_only else ""))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
