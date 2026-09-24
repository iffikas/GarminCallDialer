"""Regenerate the widget's device list, launcher icons and jungle wiring.

This build is the widget, which exists for devices that cannot run the
glance-based v2 app (see ../../GarminCallDialer2). The split is by API
level: glances for watch-app type apps only arrived in 4.0.0, so anything
below that stays here, and anything at or above it belongs to v2. The lower
bound is the manifest's own minSdkVersion, since the favorites menu needs
WatchUi.Menu2.

Devices whose launcher icon is 62x62 keep using the shared resources/
folder, which is where the original hand-made icon lives; every other size
gets a generated one.

Run from watch-app/:  python tools/generate_device_support.py
"""
import json
import os
import re
import shutil
from collections import defaultdict

from PIL import Image, ImageDraw, ImageFont

DEVICES_DIR = os.path.expandvars(r"$APPDATA\Garmin\ConnectIQ\Devices")
WATCH_APP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT_PATH = r"C:\Windows\Fonts\arialbd.ttf"

EXCLUDED_FAMILY_PREFIXES = ("rectangle",)
MIN_API_LEVEL = (3, 2, 0)   # matches manifest minSdkVersion / Menu2
MAX_API_LEVEL = (4, 0, 0)   # at and above this, v2's glance app takes over
BASE_ICON_SIZE = 62         # served by the existing resources/ folder


def api_level(cfg):
    versions = []
    for part in cfg.get("partNumbers", []):
        raw = part.get("connectIQVersion")
        if raw:
            versions.append(tuple(int(n) for n in raw.split(".")))
    return min(versions) if versions else (0, 0, 0)


def load_devices():
    devices = []
    for device_id in sorted(os.listdir(DEVICES_DIR)):
        path = os.path.join(DEVICES_DIR, device_id, "compiler.json")
        if not os.path.isfile(path):
            continue
        with open(path, encoding="utf-8") as handle:
            cfg = json.load(handle)

        app_types = {entry["type"] for entry in cfg.get("appTypes", [])}
        if "widget" not in app_types:
            continue
        if cfg.get("deviceFamily", "").startswith(EXCLUDED_FAMILY_PREFIXES):
            continue
        if not MIN_API_LEVEL <= api_level(cfg) < MAX_API_LEVEL:
            continue

        icon = cfg.get("launcherIcon", {})
        devices.append({
            "id": cfg.get("deviceId", device_id),
            "width": int(icon["width"]),
            "height": int(icon["height"]),
            "mono": cfg.get("bitsPerPixel") == 1,
        })
    return devices


def draw_icon(width, height, mono):
    """The GD monogram, flat black-and-white on 1-bit displays where a
    gradient would just dither into noise."""
    supersample = 8
    canvas_w, canvas_h = width * supersample, height * supersample
    image = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    if mono:
        background, ring, letters = (0, 0, 0), (255, 255, 255), (255, 255, 255)
    else:
        background, ring, letters = (30, 44, 58), (79, 168, 216), (245, 250, 253)

    draw = ImageDraw.Draw(image)
    draw.ellipse([0, 0, canvas_w - 1, canvas_h - 1], fill=background + (255,))

    inset_x, inset_y = int(canvas_w * 0.06), int(canvas_h * 0.06)
    draw.ellipse(
        [inset_x, inset_y, canvas_w - inset_x, canvas_h - inset_y],
        outline=ring + (255,),
        width=max(1, int(min(canvas_w, canvas_h) * 0.045)),
    )

    font = ImageFont.truetype(FONT_PATH, int(min(canvas_w, canvas_h) * 0.42))
    box = draw.textbbox((0, 0), "GD", font=font)
    draw.text(
        ((canvas_w - (box[2] - box[0])) / 2 - box[0], (canvas_h - (box[3] - box[1])) / 2 - box[1]),
        "GD",
        font=font,
        fill=letters + (255,),
    )

    return image.resize((width, height), Image.LANCZOS)


def icon_folder(width, height, mono):
    return f"resources-icon{width}x{height}{'mono' if mono else ''}"


def needs_generated_icon(device):
    return device["width"] != BASE_ICON_SIZE or device["height"] != BASE_ICON_SIZE


def write_icon_folders(devices):
    for folder in os.listdir(WATCH_APP_DIR):
        if folder.startswith("resources-icon"):
            shutil.rmtree(os.path.join(WATCH_APP_DIR, folder))

    variants = {(d["width"], d["height"], d["mono"]) for d in devices if needs_generated_icon(d)}
    for width, height, mono in sorted(variants):
        folder = os.path.join(WATCH_APP_DIR, icon_folder(width, height, mono), "drawables")
        os.makedirs(folder, exist_ok=True)
        draw_icon(width, height, mono).save(os.path.join(folder, "launcher_icon.png"))
        with open(os.path.join(folder, "drawables.xml"), "w", encoding="utf-8") as handle:
            handle.write('<drawables>\n    <bitmap id="LauncherIcon" filename="launcher_icon.png"/>\n</drawables>\n')
    return variants


def write_jungle(devices):
    lines = ["project.manifest = manifest.xml", ""]
    by_variant = defaultdict(list)
    for device in devices:
        if needs_generated_icon(device):
            by_variant[(device["width"], device["height"], device["mono"])].append(device["id"])

    for (width, height, mono), ids in sorted(by_variant.items()):
        folder = icon_folder(width, height, mono)
        lines.append(f"# {width}x{height}{' monochrome' if mono else ''} launcher icon")
        for device_id in sorted(ids):
            lines.append(f"{device_id}.resourcePath = $({device_id}.resourcePath);{folder}")
        lines.append("")

    lines.append("# Every other device uses the 62x62 icon in resources/")
    with open(os.path.join(WATCH_APP_DIR, "monkey.jungle"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def write_manifest_products(devices):
    path = os.path.join(WATCH_APP_DIR, "manifest.xml")
    with open(path, encoding="utf-8") as handle:
        manifest = handle.read()

    products = "\n".join(f'            <iq:product id="{d["id"]}"/>' for d in sorted(devices, key=lambda d: d["id"]))
    manifest = re.sub(
        r"<iq:products>.*?</iq:products>",
        f"<iq:products>\n{products}\n        </iq:products>",
        manifest,
        flags=re.DOTALL,
    )
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(manifest)


if __name__ == "__main__":
    devices = load_devices()
    variants = write_icon_folders(devices)
    write_jungle(devices)
    write_manifest_products(devices)
    print(f"{len(devices)} devices, {len(variants)} generated icon variants")
