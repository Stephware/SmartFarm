# Reviewed public UI baselines

`visual-parity.cjs` checks landing, login and registration in both themes at
320, 375, 768, 1024 and 1440 pixels. The source hashes also detect presentation
changes that require a new review.

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
