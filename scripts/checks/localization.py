#!/usr/bin/env python3
"""Audit resource coverage/format contracts without translating application data."""
from pathlib import Path
import json
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
WEB = ROOT / "src/SmartFarm.Web"
MOBILE = ROOT / "mobile/SmartFarm.Android/app/src/main"
errors = []


def resx(path):
    values = {}
    folded = set()
    for node in ET.parse(path).getroot().findall("data"):
        key = node.attrib["name"]
        if key.casefold() in folded:
            errors.append(f"Duplicate RESX key: {key}")
        folded.add(key.casefold())
        values[key] = node.findtext("value") or ""
    return values


def android(folder):
    values = {}
    for path in folder.glob("*.xml"):
        for node in ET.parse(path).getroot().findall("string"):
            value = node.text or ""
            values[node.attrib["name"]] = json.loads(value) if value.startswith('"') else value
    return values


web_en = resx(WEB / "Resources/SharedResource.resx")
web_fil = resx(WEB / "Resources/SharedResource.fil.resx")
mobile_en = android(MOBILE / "res/values")
mobile_fil = android(MOBILE / "res/values-fil")
for platform, base, translated, placeholder in [
    ("Web", web_en, web_fil, r"\{(\d+)(?:[^}]*)\}"),
    ("Android", mobile_en, mobile_fil, r"%(\d+)\$[sd]"),
]:
    for key, english in base.items():
        if key not in translated:
            errors.append(f"{platform} Filipino resource missing: {key}")
        elif set(re.findall(placeholder, english)) != set(re.findall(placeholder, translated[key])):
            errors.append(f"{platform} placeholder mismatch: {key}")
    for key in translated.keys() - base.keys():
        errors.append(f"{platform} has no English fallback: {key}")

for folder in [WEB / "Views", WEB / "Areas/Admin/Views"]:
    for path in folder.rglob("*.cshtml"):
        source = path.read_text()
        for match in re.finditer(r'\bT\[("(?:\\.|[^"\\])*")', source):
            key = json.loads(match[1])
            if key not in web_en:
                errors.append(f"Missing Web resource in {path.relative_to(ROOT)}: {key}")
        if re.search(r"ViewData\[T\[|(?:==|!=)\s*T\[", source):
            errors.append(f"Localized metadata/comparison in {path.relative_to(ROOT)}")

for path in (WEB / "Controllers").rglob("*.cs"):
    source = path.read_text()
    for match in re.finditer(r'TempData\["([^"]+)"\]\s*=\s*SmartFarm.Web.Presentation.UiMessages.Text', source):
        if match[1] not in {"Message", "Notice", "Error", "HarvestNotice", "HarvestError"}:
            errors.append(f"Localized TempData marker: {path.name} {match[1]}")

for path in (MOBILE / "java").rglob("*.kt"):
    for match in re.finditer(r'uiText\(R.string.(\w+),\s*("(?:\\.|[^"\\])*")', path.read_text()):
        key = match[1]
        fallback = json.loads(match[2].replace(r"\$", "$"))
        if key not in mobile_en:
            errors.append(f"Missing Android resource: {key}")
        elif mobile_en[key] != fallback:
            errors.append(f"Android English fallback differs: {path.name} {key}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"PASS: {len(web_en)} Web and {len(mobile_en)} Android resources; Filipino coverage, fallback, format and identifier guards")
