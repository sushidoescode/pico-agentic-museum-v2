"""Single source of truth for the Earth Collection archive room layout (runtime coordinates).

Runtime frame (glTF): +Y up, metres. Origin = the arrival point on the floor, at the centre of the brass circular
motif. At yaw 0 the visitor faces -Z (toward the rear aperture wall); +X is the visitor's right.
  Room 10 m wide (x -5..+5), 9 m deep (z +4.5 entrance wall .. -4.5 aperture wall), 4 m high (vaulted coves from
  2.5 m), plus a closed entrance vestibule behind the arrival point (z +4.5..+5.7).
Yaw convention (anchors, plaques, signs, viewpoints): degrees about +Y, right-handed; 0 = facing -Z, +90 = facing
-X (turn left). A viewpoint's forward vector is (-sin yaw, 0, -cos yaw). Anchors / plaques / signs rotate an
object's local frame whose front is +Z, so their front points along (sin yaw, 0, cos yaw) (toward the visitor).
Anchor position = where the exhibit's base-centre origin goes (y = plinthTopY); displayScale 1.0 = real size.
Plaque = centre of the blank brass plate's front face; pitchDeg = tilt of the face from vertical, + = facing up.
Sign = centre of a clean wall area for the app's bay title label (front faces the bay viewpoint).
Blender mapping used by the build: runtime (x, y, z) -> Blender (x, -z, y).

Bay geometry (native finding 2026-09-27: ~72 deg horizontal view, eye ~1.75 m): each bay's furniture follows an arc
centred on its viewpoint, so every exhibit is ~3.1-3.4 m from the viewpoint and within about +-30 deg of its view.
"""
import math

ROOM = {"widthX": [-5.0, 5.0], "depthZ": [-4.5, 4.5], "height": 4.0, "wallTop": 2.5, "coveInnerX": 3.6,
        "vestibule": {"x": [-1.1, 1.1], "z": [4.5, 5.7], "height": 2.6}}
EYE_HEIGHT = 1.75            # renders (native finding: eye ~1.7-1.8 m)
HFOV_DEG = 72.0              # renders (native finding: ~72 deg horizontal)
BENCH_TOP = 0.80
BENCH_DEPTH = 0.70
PLATFORM_TOP = 0.10          # domestic floor platform
PLATFORM_DEPTH = 0.90
PLAQUE = {"w": 0.20, "h": 0.075, "t": 0.006, "pitchDeg": 25.0, "topY": 0.72}
APERTURE = {"centre": [0.0, 2.85, -4.5], "w": 4.0, "h": 1.5, "depth": 0.36, "bezel": 0.20}
REAR_DADO_TOP = 1.45         # slate dado behind the Connected lives bench; the bay sign sits between it and the bezel
PLINTH_SIZE = {"rocket": 0.36, "router": 0.46, "gascan": 0.42, "doorbell": 0.40}
LECTERN_ITEMS = ("vacuum", "scooter")   # low supports: plaque on a lectern at the platform's front edge
LECTERN_TOP_Y = 0.48         # lower plate so a low exhibit behind it stays in view from the bay viewpoint

VIEWPOINTS = [
    {"name": "arrival", "position": [0.0, 0.0, 0.0], "yawDeg": 0.0,
     "note": "arrival point at the centre of the floor motif, facing the rear bay and the aperture; the entrance vestibule is behind"},
    {"name": "daily", "position": [-0.8, 0.0, 0.0], "yawDeg": 90.0, "note": "facing -X, the Daily rituals bay (arc centre)"},
    {"name": "connected", "position": [0.0, 0.0, -0.2], "yawDeg": 0.0, "note": "facing -Z, the Connected lives bay under the aperture (arc centre)"},
    {"name": "domestic", "position": [0.8, 0.0, 0.0], "yawDeg": -90.0, "note": "facing +X, the Domestic systems and movement bay (arc centre)"},
]

# Bays: arc centre (viewpoint), radius of the anchor/furniture centreline, the viewpoint's facing direction as an
# angle in the x-z plane, item list (id, angle offset deg from the facing direction, plinth top y, support kind).
# Angle offsets are positive toward the viewer's LEFT.
BAYS = [
    {"id": "daily", "section": "DAILY_RITUALS", "title": "Daily rituals", "viewpoint": "daily", "facing": "+X",
     "centre": (-0.8, 0.0), "radius": 3.2, "span": 33.0, "kind": "bench", "riser": 0.42,
     "segments": [(33.0, 11.0), (11.0, -11.0), (-11.0, -33.0)],
     "items": [("quencher", 27.5, 0.85), ("coffeepod", 16.5, 1.04), ("deliverybox", 5.5, 0.88),
               ("qrcode", -5.5, 0.95), ("fidget", -16.5, 1.02), ("facemask", -27.5, 0.98)]},
    {"id": "connected", "section": "CONNECTED_LIVES", "title": "Connected lives", "viewpoint": "connected", "facing": "-Z",
     "centre": (0.0, -0.2), "radius": 3.1, "span": 31.0, "kind": "bench", "riser": 0.36,
     "segments": [(31.0, 10.0), (10.0, -10.0), (-10.0, -31.0)],
     "items": [("smartspeaker", 25.0, 0.90), ("smartphone", 15.0, 0.95), ("earbuds", 5.0, 1.02),
               ("smartwatch", -5.0, 1.00), ("cryptowallet", -15.0, 1.04), ("remote", -25.0, 1.03)]},
    {"id": "domestic", "section": "DOMESTIC_SYSTEMS", "title": "Domestic systems and movement", "viewpoint": "domestic",
     "facing": "+X-wall", "centre": (0.8, 0.0), "radius": 3.4, "span": 35.0, "kind": "platform",
     # slot widths along the arc (m) back-to-front; the scooter needs the long slot on the bare platform
     "slots": [("vacuum", 0.60, 0.30, "round pedestal"), ("router", 0.50, 0.90, "plinth"),
               ("doorbell", 0.45, 0.95, "plinth with wall-mount backboard"), ("scooter", 1.50, 0.10, "floor platform"),
               ("gascan", 0.50, 0.75, "plinth"), ("rocket", 0.50, 0.90, "plinth")]},
]


RADIAL_OFFSET = {"doorbell": 0.12}   # m outward (toward the wall) from the bay radius: doorbell sits against its backboard
BACKBOARD = {"w": 0.22, "h": 0.40, "t": 0.03}


def bay(bid):
    return next(b for b in BAYS if b["id"] == bid)


def facing_angle(b):
    """Direction (radians, in the x-z plane measured from +X toward +Z) the viewpoint faces."""
    vp = next(v for v in VIEWPOINTS if v["name"] == b["viewpoint"])
    yaw = math.radians(vp["yawDeg"])
    fx, fz = -math.sin(yaw), -math.cos(yaw)
    return math.atan2(fz, fx)


def arc_point(b, off_deg, radius=None):
    """Point on the bay arc at an angular offset (positive = viewer's left) and the unit normal toward the centre."""
    base = facing_angle(b)
    # viewer's left is a positive rotation about +Y; in (x, z) with angle measured x->z that is a negative angle step
    a = base - math.radians(off_deg)
    r = b["radius"] if radius is None else radius
    cx, cz = b["centre"]
    x, z = cx + r * math.cos(a), cz + r * math.sin(a)
    nx, nz = -math.cos(a), -math.sin(a)
    return (x, z), (nx, nz)


def yaw_facing(nx, nz):
    """Yaw whose front (sin yaw, 0, cos yaw) equals (nx, nz)."""
    return math.degrees(math.atan2(nx, nz))


def item_offsets(b):
    """[(id, offset_deg, plinth_top, support)] for any bay kind."""
    if b["kind"] == "bench":
        return [(i, o, t, "riser on the bench") for i, o, t in b["items"]]
    total = sum(w for _, w, _, _ in b["slots"])
    ang_total = math.degrees(total / b["radius"])
    out = []
    acc = 0.0
    for ex_id, w, top, support in b["slots"]:  # back-to-front = viewer's LEFT to RIGHT for the domestic bay
        mid = acc + w / 2
        off = ang_total / 2 - math.degrees(mid / b["radius"])
        out.append((ex_id, off, top, support))
        acc += w
    return out


def plaque_for(b, off_deg, radius_face, top_y=None):
    """Blank brass plate hanging from its top edge on a vertical face at radius_face, tilted up PLAQUE pitch."""
    (x, z), (nx, nz) = arc_point(b, off_deg, radius_face)
    p = math.radians(PLAQUE["pitchDeg"])
    h, t = PLAQUE["h"], PLAQUE["t"]
    top = (x, PLAQUE["topY"] if top_y is None else top_y, z)
    # in-plane "up" of the tilted plate points up and back into the face: (cos p) up - (sin p) n
    cx = top[0] + (math.sin(p) * nx) * h / 2
    cy = top[1] - math.cos(p) * h / 2
    cz = top[2] + (math.sin(p) * nz) * h / 2
    # front face = centre + tilted normal * thickness
    fnx, fny, fnz = math.cos(p) * nx, math.sin(p), math.cos(p) * nz
    face = (cx + fnx * t, cy + fny * t, cz + fnz * t)
    return {"position": [round(v, 4) for v in face], "yawDeg": round(yaw_facing(nx, nz), 3),
            "pitchDeg": PLAQUE["pitchDeg"], "sizeMetres": [PLAQUE["w"], PLAQUE["h"]]}, top, (nx, nz)


def anchors():
    out = []
    for b in BAYS:
        for ex_id, off, top, support in item_offsets(b):
            (x, z), (nx, nz) = arc_point(b, off, b["radius"] + RADIAL_OFFSET.get(ex_id, 0.0))
            if b["kind"] == "bench":
                face_r = b["radius"] - BENCH_DEPTH / 2          # bench front face (toward the viewpoint)
            elif ex_id in LECTERN_ITEMS:
                face_r = b["radius"] - 0.33 - 0.03             # lectern plate on the platform's front edge
            else:
                face_r = b["radius"] - PLINTH_SIZE[ex_id] / 2   # plinth front face
            plaque, _, _ = plaque_for(b, off, face_r, LECTERN_TOP_Y if (b["kind"] != "bench" and ex_id in LECTERN_ITEMS) else None)
            out.append({"name": f"anchor_{ex_id}", "id": ex_id, "bay": b["id"],
                        "position": [round(x, 4), top, round(z, 4)], "yawDeg": round(yaw_facing(nx, nz), 3),
                        "plinthTopY": top, "displayScale": 1.0, "support": support,
                        "angleFromViewpointDeg": round(off, 2),
                        "distanceFromViewpointM": round(b["radius"] + RADIAL_OFFSET.get(ex_id, 0.0), 3),
                        "plaque": plaque})
    return out


def signs():
    out = []
    for b in BAYS:
        if b["id"] == "daily":
            s = {"position": [-4.96, 2.05, 0.0], "yawDeg": 90.0, "surface": "left wall, centre slate panel (upper row)"}
        elif b["id"] == "domestic":
            s = {"position": [4.96, 2.05, 0.0], "yawDeg": -90.0, "surface": "right wall, centre slate panel (upper row)"}
        else:
            s = {"position": [0.0, 1.675, -4.48], "yawDeg": 0.0,
                 "surface": "rear wall, clean pale band between the slate dado (top 1.45 m) and the aperture bezel (from 1.90 m)"}
        s["sizeMetres"] = [1.6, 0.30] if b["id"] != "connected" else [1.6, 0.36]
        out.append(dict(s, bay=b["id"]))
    return out


def viewpoints():
    return VIEWPOINTS
