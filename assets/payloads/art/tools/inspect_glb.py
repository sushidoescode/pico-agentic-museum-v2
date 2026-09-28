#!/usr/bin/env python3
"""Stdlib-only GLB inspector for the Earth Collection runtime assets.

Usage:
  inspect_glb.py <file.glb> [--dump-images DIR] [--compact]

Prints one JSON object: file identity (sha256, bytes), generator, extensionsUsed/Required, node/mesh/primitive
counts, triangles, vertices, attributes, materials (factors and texture slots), images (mime, width x height,
bytes, sha256, the slots that use them), and the exact bounds of every POSITION vertex transformed by its node's
world matrix (glTF frame: +Y up, +Z front, metres). Only glTF 2.0 binaries with one JSON chunk and one BIN chunk
and embedded (bufferView) images are accepted, which is what the museum ships.

Importable: `inspect(path) -> dict`.
"""
import argparse
import hashlib
import json
import math
import struct
import sys
from pathlib import Path

COMPONENT = {5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2), 5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4)}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}


def read_glb(data):
    magic, version, length = struct.unpack_from("<4sII", data, 0)
    if magic != b"glTF" or version != 2:
        raise ValueError("not a glTF 2.0 binary")
    if length != len(data):
        raise ValueError(f"header length {length} != file length {len(data)}")
    off, chunks = 12, []
    while off < len(data):
        clen, ctype = struct.unpack_from("<I4s", data, off)
        chunks.append((ctype, data[off + 8:off + 8 + clen]))
        off += 8 + clen
    types = [c[0] for c in chunks]
    if types[0] != b"JSON" or (len(types) > 1 and types[1] != b"BIN\x00") or len(types) > 2:
        raise ValueError(f"unexpected chunk layout {types}")
    gltf = json.loads(chunks[0][1])
    binary = chunks[1][1] if len(chunks) > 1 else b""
    return gltf, binary


def view_bytes(gltf, binary, index):
    bv = gltf["bufferViews"][index]
    if bv.get("buffer", 0) != 0:
        raise ValueError("only buffer 0 (the GLB BIN chunk) is supported")
    start = bv.get("byteOffset", 0)
    return binary[start:start + bv["byteLength"]], bv.get("byteStride")


def read_accessor(gltf, binary, index):
    acc = gltf["accessors"][index]
    fmt, size = COMPONENT[acc["componentType"]]
    n = NCOMP[acc["type"]]
    count = acc["count"]
    if "bufferView" not in acc:
        return [tuple([0] * n)] * count
    raw, stride = view_bytes(gltf, binary, acc["bufferView"])
    base = acc.get("byteOffset", 0)
    elem = size * n
    stride = stride or elem
    out = []
    unpack = struct.Struct("<" + fmt * n).unpack_from
    for i in range(count):
        out.append(unpack(raw, base + i * stride))
    return out


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def node_matrix(node):
    if "matrix" in node:
        m = node["matrix"]  # column-major
        return [[m[c * 4 + r] for c in range(4)] for r in range(4)]
    tx, ty, tz = node.get("translation", [0, 0, 0])
    qx, qy, qz, qw = node.get("rotation", [0, 0, 0, 1])
    sx, sy, sz = node.get("scale", [1, 1, 1])
    r = [
        [1 - 2 * (qy * qy + qz * qz), 2 * (qx * qy - qz * qw), 2 * (qx * qz + qy * qw)],
        [2 * (qx * qy + qz * qw), 1 - 2 * (qx * qx + qz * qz), 2 * (qy * qz - qx * qw)],
        [2 * (qx * qz - qy * qw), 2 * (qy * qz + qx * qw), 1 - 2 * (qx * qx + qy * qy)],
    ]
    return [
        [r[0][0] * sx, r[0][1] * sy, r[0][2] * sz, tx],
        [r[1][0] * sx, r[1][1] * sy, r[1][2] * sz, ty],
        [r[2][0] * sx, r[2][1] * sy, r[2][2] * sz, tz],
        [0, 0, 0, 1],
    ]


def image_dims(blob, mime):
    if blob[:8] == b"\x89PNG\r\n\x1a\n":
        w, h, depth, ctype = struct.unpack(">IIBB", blob[16:26])
        return w, h, {0: "grey", 2: "RGB", 3: "palette", 4: "grey+alpha", 6: "RGBA"}.get(ctype, str(ctype)), depth
    if blob[:3] == b"\xff\xd8\xff":
        i = 2
        while i < len(blob):
            if blob[i] != 0xFF:
                i += 1
                continue
            marker = blob[i + 1]
            if marker in (0xD8, 0x01) or 0xD0 <= marker <= 0xD7:
                i += 2
                continue
            seglen = struct.unpack(">H", blob[i + 2:i + 4])[0]
            if marker in (0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF):
                depth, h, w, ncomp = struct.unpack(">BHHB", blob[i + 4:i + 10])
                return w, h, {1: "grey", 3: "YCbCr", 4: "CMYK"}.get(ncomp, str(ncomp)), depth
            i += 2 + seglen
    return None, None, "unknown", None


def inspect(path, dump_images=None):
    data = Path(path).read_bytes()
    gltf, binary = read_glb(data)
    result = {
        "file": Path(path).name,
        "sha256": hashlib.sha256(data).hexdigest(),
        "bytes": len(data),
        "generator": gltf.get("asset", {}).get("generator"),
        "extensionsUsed": gltf.get("extensionsUsed", []),
        "extensionsRequired": gltf.get("extensionsRequired", []),
        "nodes": len(gltf.get("nodes", [])),
        "meshes": len(gltf.get("meshes", [])),
        "buffers": len(gltf.get("buffers", [])),
    }
    # which texture slots use which image
    usage = {}
    tex = gltf.get("textures", [])
    materials = []
    for mi, m in enumerate(gltf.get("materials", [])):
        pbr = m.get("pbrMetallicRoughness", {})
        slots = {}
        for slot, holder in (("baseColor", pbr.get("baseColorTexture")),
                             ("metallicRoughness", pbr.get("metallicRoughnessTexture")),
                             ("normal", m.get("normalTexture")), ("occlusion", m.get("occlusionTexture")),
                             ("emissive", m.get("emissiveTexture"))):
            if holder is not None:
                src = tex[holder["index"]].get("source")
                slots[slot] = {"image": src, "texCoord": holder.get("texCoord", 0)}
                usage.setdefault(src, []).append(f"{m.get('name', mi)}.{slot}")
        materials.append({
            "name": m.get("name"),
            "baseColorFactor": pbr.get("baseColorFactor", [1, 1, 1, 1]),
            "metallicFactor": pbr.get("metallicFactor", 1.0),
            "roughnessFactor": pbr.get("roughnessFactor", 1.0),
            "metallicFactorExplicit": "metallicFactor" in pbr,
            "roughnessFactorExplicit": "roughnessFactor" in pbr,
            "emissiveFactor": m.get("emissiveFactor", [0, 0, 0]),
            "alphaMode": m.get("alphaMode", "OPAQUE"),
            "doubleSided": m.get("doubleSided", False),
            "textures": slots,
            "extensions": sorted(m.get("extensions", {}).keys()),
        })
    result["materials"] = materials
    images = []
    for ii, im in enumerate(gltf.get("images", [])):
        if "bufferView" not in im:
            raise ValueError(f"image {ii} is not embedded (uri={im.get('uri')!r})")
        blob, _ = view_bytes(gltf, binary, im["bufferView"])
        w, h, mode, depth = image_dims(blob, im.get("mimeType"))
        entry = {"index": ii, "name": im.get("name"), "mimeType": im.get("mimeType"), "width": w, "height": h,
                 "mode": mode, "bitDepth": depth, "bytes": len(blob), "sha256": hashlib.sha256(blob).hexdigest(),
                 "usedBy": usage.get(ii, [])}
        images.append(entry)
        if dump_images:
            ext = "png" if im.get("mimeType") == "image/png" else "jpg"
            out = Path(dump_images) / f"{ii:02d}_{(im.get('name') or 'image')}.{ext}"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(blob)
            entry["dumpedTo"] = str(out)
    result["images"] = images

    # geometry: walk the default scene with world matrices
    ident = [[1 if i == j else 0 for j in range(4)] for i in range(4)]
    scenes = gltf.get("scenes", [{"nodes": list(range(len(gltf.get("nodes", []))))}])
    scene = scenes[gltf.get("scene", 0)]
    tris = verts = prims = 0
    attrs = set()
    lo = [math.inf] * 3
    hi = [-math.inf] * 3
    mesh_nodes = []
    stack = [(n, ident) for n in scene.get("nodes", [])]
    while stack:
        ni, parent = stack.pop()
        node = gltf["nodes"][ni]
        world = mat_mul(parent, node_matrix(node))
        for c in node.get("children", []):
            stack.append((c, world))
        if "mesh" not in node:
            continue
        mesh_nodes.append(node.get("name", ni))
        for p in gltf["meshes"][node["mesh"]]["primitives"]:
            prims += 1
            attrs.update(p["attributes"].keys())
            mode = p.get("mode", 4)
            pos_acc = gltf["accessors"][p["attributes"]["POSITION"]]
            n = gltf["accessors"][p["indices"]]["count"] if "indices" in p else pos_acc["count"]
            tris += n // 3 if mode == 4 else max(n - 2, 0) if mode in (5, 6) else 0
            verts += pos_acc["count"]
            if pos_acc["componentType"] != 5126:
                raise ValueError("POSITION is not float32 (quantized meshes are not allowed)")
            for x, y, z in read_accessor(gltf, binary, p["attributes"]["POSITION"]):
                wx = world[0][0] * x + world[0][1] * y + world[0][2] * z + world[0][3]
                wy = world[1][0] * x + world[1][1] * y + world[1][2] * z + world[1][3]
                wz = world[2][0] * x + world[2][1] * y + world[2][2] * z + world[2][3]
                if wx < lo[0]: lo[0] = wx
                if wy < lo[1]: lo[1] = wy
                if wz < lo[2]: lo[2] = wz
                if wx > hi[0]: hi[0] = wx
                if wy > hi[1]: hi[1] = wy
                if wz > hi[2]: hi[2] = wz
    result["primitives"] = prims
    result["meshNodes"] = mesh_nodes
    result["triangles"] = tris
    result["vertices"] = verts
    result["attributes"] = sorted(attrs)
    r = lambda v: round(v, 5)
    result["boundsMetres"] = {"min": [r(v) for v in lo], "max": [r(v) for v in hi],
                              "size": [r(hi[i] - lo[i]) for i in range(3)]}
    # layout checks
    problems = []
    if struct.unpack_from("<I", data, 8)[0] != len(data):
        problems.append("header length mismatch")
    for i, bv in enumerate(gltf.get("bufferViews", [])):
        if bv.get("byteOffset", 0) % 4:
            problems.append(f"bufferView {i} not 4-byte aligned")
    if gltf.get("buffers") and gltf["buffers"][0].get("byteLength") != len(binary):
        # the BIN chunk may carry up to 3 bytes of padding
        if not (0 <= len(binary) - gltf["buffers"][0]["byteLength"] < 4):
            problems.append("buffer length mismatch")
    result["layoutProblems"] = problems
    return result


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("glb")
    ap.add_argument("--dump-images")
    ap.add_argument("--compact", action="store_true")
    a = ap.parse_args()
    res = inspect(a.glb, a.dump_images)
    print(json.dumps(res, indent=None if a.compact else 1, sort_keys=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
