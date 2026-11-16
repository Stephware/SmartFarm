# Reviewed public UI baselines

`visual-parity.cjs` checks landing, login and registration in both themes at
320, 375, 768, 1024 and 1440 pixels. The source hashes also detect presentation
changes that require a new review.

Goldens use Playwright 1.56.1 Chromium on Ubuntu 24.04, matching CI. On macOS,
use `mcr.microsoft.com/playwright:v1.56.1-noble` with the same runner packages
and access the local site through `host.docker.internal`. Keep native Chrome
screenshots as additional appearance evidence; its font/control rasterization
differs from Linux and must not replace the CI goldens.

For an intentional redesign:

1. Build and start the candidate MVC site.
2. Run `ANIGO_UPDATE_VISUAL_BASELINES=1 node tests/browser/visual-parity.cjs`
   (set `ANIGO_CANDIDATE_URL` when not using port 5082).
3. Inspect all generated PNGs for layout, contrast, overflow and form usability;
   run the browser smoke and accessibility checks as well.
4. Set `reviewed` to `true` in `review.json` only after review, and record the
   reviewer and scope in its note. Commit the PNGs and manifest together.
5. Run the script again without the update flag. CI uses this comparison mode.

These anonymous pages contain no account or farm records. Authenticated role,
data and validation checks remain separate; public goldens do not prove those
workflows.


Phase 6 (2026-11-16): the public landing has an intentionally new approved
photography-led composition. The 10 landing PNGs were regenerated from the
real MVC site at all five widths and both color modes, then representative
1440px/375px full-page screenshots reviewed. The previous landing PNGs are
not valid comparison baselines. Login and registration routes remain included
in the same 30-image review set. The independent Phase 6 Chromium test also
checks image decoding, farm-route destinations, overflow and accessibility.

Phase 7 (2026-11-16): Codex inspected all 30 actual MVC captures from the pinned
Linux runner. Fifteen dark images adopt the charcoal/pine/gold palette; five light
landing images include targeted photo/CTA contrast fixes. All image dimensions
match the previous set, and ten light login/registration images are byte-identical.
The Phase 6 composition is retained. Comparison mode passed all 30 after review;
no tolerance or test changes were used to approve the new images. Theme persistence,
form drafts, enlarged text and A/AA checks remain separate browser assertions.
