#!/usr/bin/env python3
"""Validate AniGo crop artwork without external dependencies."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "src" / "SmartFarm.Web" / "wwwroot" / "images" / "crops"
NAMES = ("tomato", "eggplant", "rice", "corn", "sprout")
SVG_NS = "http://www.w3.org/2000/svg"


def check_crop(name: str) -> list[str]:
    path = ART / f"{name}.svg"
    errors: list[str] = []
    try:
        content = path.read_text(encoding="utf-8")
        root = ET.fromstring(content)
    except (OSError, UnicodeError, ET.ParseError) as exc:
        return [f"{path}: SVG failed to parse: {exc}"]

    if root.tag != f"{{{SVG_NS}}}svg":
        errors.append(f"{path}: root must be a namespaced svg")
    if root.attrib.get("viewBox") != "0 0 180 180":
        errors.append(f"{path}: expected consistent 180x180 viewBox")
    if "http://" in content.replace(f'xmlns="{SVG_NS}"', "") or "https://" in content:
        errors.append(f"{path}: external network assets are not allowed")
    if re.search(r"<\s*(?:script|foreignObject|image)\b", content, flags=re.I):
        errors.append(f"{path}: must remain a self-contained vector illustration")
    ids = {node.attrib["id"] for node in root.iter() if "id" in node.attrib}
    references = re.findall(r"url\(#([A-Za-z][\w-]*)\)", content)
    for ref in references:
        if ref not in ids:
            errors.append(f"{path}: undefined paint reference #{ref}")
    if root.find(f"{{{SVG_NS}}}title") is None:
        errors.append(f"{path}: missing title")
    return errors


if __name__ == "__main__":
    errors = [error for name in NAMES for error in check_crop(name)]
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print(f"Validated {len(NAMES)} crop illustrations: valid, self-contained SVGs.")
