#!/usr/bin/env python3
"""Read-only verification that a private observation-photo backup matches its source."""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path


def snapshot(root: Path) -> dict[str, str]:
    if root.is_symlink() or not root.is_dir():
        raise ValueError(f"Not a real directory: {root}")
    files: dict[str, str] = {}
    for item in sorted(root.rglob("*")):
        if item.is_symlink():
            raise ValueError(f"Symlink is not allowed in photo backup: {item}")
        if item.is_dir():
            continue
        if not item.is_file():
            raise ValueError(f"Unexpected filesystem entry: {item}")
        checksum = hashlib.sha256()
        with item.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                checksum.update(chunk)
        files[item.relative_to(root).as_posix()] = checksum.hexdigest()
    return files


def verify(live_dir: Path, backup_dir: Path) -> tuple[int, int]:
    live = live_dir.resolve(strict=True)
    backup = backup_dir.resolve(strict=True)
    if live == backup or live in backup.parents or backup in live.parents:
        raise ValueError("Live and backup directories must be separate, not nested.")
    originals = snapshot(live_dir)
    copies = snapshot(backup_dir)
    missing = sorted(originals.keys() - copies.keys())
    extra = sorted(copies.keys() - originals.keys())
    changed = sorted(k for k in originals.keys() & copies.keys() if originals[k] != copies[k])
    if missing or extra or changed:
        raise ValueError(
            f"Backup mismatch: {len(missing)} missing, {len(extra)} unexpected, "
            f"{len(changed)} different files. "
            f"Examples: {', '.join((missing + extra + changed)[:4])}"
        )
    if not originals:
        raise ValueError("Both directories are empty; no photo recovery is demonstrated.")
    return len(originals), sum(p.stat().st_size for p in live_dir.rglob("*") if p.is_file())


def main(argv: list[str]) -> int:
    if len(argv) != 3 or argv[1] in {"-h", "--help"}:
        print("Usage: python3 scripts/verify_photo_backup.py LIVE_PHOTO_DIR BACKUP_PHOTO_DIR")
        return 2 if len(argv) != 3 else 0
    try:
        count, size = verify(Path(argv[1]), Path(argv[2]))
    except (OSError, ValueError) as error:
        print(f"FAIL: private photo backup: {error}", file=sys.stderr)
        return 1
    print(f"PASS: {count} private photo files match ({size} bytes; SHA-256 per file).")
    print("Verification is read-only and does not prove the backed-up SQL photo keys match.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
