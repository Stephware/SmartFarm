"""Phase A photo recovery checks run with Python's standard-library unittest."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).resolve().parents[1] / "verify_photo_backup.py"


class PhotoBackupVerificationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.live = Path(self.tmp.name) / "live"
        self.copy = Path(self.tmp.name) / "copy"
        self.live.mkdir()
        self.copy.mkdir()

    def run_check(self, live=None, backup=None):
        return subprocess.run(
            [sys.executable, str(TOOL), str(live or self.live), str(backup or self.copy)],
            capture_output=True, text=True, check=False
        )

    def test_matching_binary_photos_pass(self):
        for root in (self.live, self.copy):
            (root / "observations").mkdir()
            (root / "observations" / "image.jpg").write_bytes(b"\xff\xd8\xff\x00\x01")
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("1 private photo files match", result.stdout)

    def test_corrupt_backup_is_rejected(self):
        (self.live / "photo.jpg").write_bytes(b"correct bytes")
        (self.copy / "photo.jpg").write_bytes(b"incorrect bytes")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("1 different files", result.stderr)

    def test_missing_backup_file_is_rejected(self):
        (self.live / "photo.jpg").write_bytes(b"photo")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("1 missing", result.stderr)

    def test_unexpected_backup_file_is_rejected(self):
        (self.live / "photo.jpg").write_bytes(b"photo")
        (self.copy / "photo.jpg").write_bytes(b"photo")
        (self.copy / "extra.jpg").write_bytes(b"unexpected")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("1 unexpected", result.stderr)

    def test_empty_backups_cannot_be_claimed_as_verified(self):
        self.assertEqual(self.run_check().returncode, 1)

    def test_same_or_nested_directory_is_rejected(self):
        (self.live / "photo.jpg").write_bytes(b"photo")
        self.assertEqual(self.run_check(self.live, self.live).returncode, 1)
        (self.live / "nested").mkdir()
        self.assertEqual(self.run_check(self.live, self.live / "nested").returncode, 1)

    def test_symlinks_are_not_trusted_as_backup_files(self):
        (self.live / "photo.jpg").write_bytes(b"photo")
        try:
            (self.copy / "photo.jpg").symlink_to(self.live / "photo.jpg")
        except (OSError, NotImplementedError):
            self.skipTest("Symlinks unavailable")
        result = self.run_check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("Symlink", result.stderr)


if __name__ == "__main__":
    unittest.main()
