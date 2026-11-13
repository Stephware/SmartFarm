# SmartFarm LLM Wiki

> **New planned product target (2026-11-13):** [Farmer-first MVP scope](wiki/project/mvp-simplification.md), [PRODUCT.md](../../PRODUCT.md), and [Codex execution plan](../../docs/superpowers/plans/2026-11-13-mvp-simplification.md). Historical articles and immutable raw captures continue to describe prior implemented states. For routine coding, open only the relevant wiki article rather than ingesting every raw snapshot.


Knowledge root: `.local/karpathy-wiki/`. Start with [the index](wiki/index.md).
Repository source is authoritative; this is maintained development context, not
application configuration or a runtime acceptance certificate.

## Installation and discovery

The reusable `karpathy-llm-wiki` skill is installed at
`/Users/stephen/.codex/skills/karpathy-llm-wiki/`, pinned at initial setup to upstream
`eafcc77001e496cc43499e4923b663aec722c813`. The discovery symlink under
`/Users/stephen/.agents/skills/karpathy-llm-wiki` points to that installation.
No application dependency, automatic watcher or hook was added. On another machine,
install the actual skill and use its instructions; this repository does not vendor it.

Root [AGENTS.md](../../AGENTS.md) selects this knowledge root and the integrated
Karpathy/Superpowers/Ponytail/Impeccable workflow. Existing sessions can retain old
agent guidance; inspect current files or start a fresh session if guidance conflicts.

## Evidence and limits

Initial capture at `850bfc7dab94675d5a70c13a95a9d00c481c1784` on 2026-10-27 is
preserved. The 2026-11-11 audit reviewed `dev` at `ac624be`, all application areas,
SQL, tests and CI. GitHub metadata confirmed PR61–78 merge SHAs are ancestors of
that checkout. Presence/merge history does not prove runtime or deployment success.

`raw/project/` is immutable evidence: original source bundles and dated review
observations. Current articles cite new evidence; old claims/results retain dated
scope or Outdated annotations. `source-manifest.json` preserves initial source
hashes and appends `reviews` for later capture hashes and source scope. Initial
hashes are provenance, not automatic freshness status. Do not rewrite snapshots
or delete initial manifest entries to make freshness look current.

No credentials, environment files, live records, keys, photos, databases or private
logs belong in shared knowledge. The `verification/` directory contains historical
local artifacts and stays ignored; articles use shareable raw evidence and identify
those artifacts as local-only. Links within immutable raw source bundles retain
the original source context and are not rebased as current wiki navigation.

## Working with the wiki

Read index → relevant articles → affected source; use explicit hidden searches:

```sh
rg --hidden --no-ignore -n 'enrollment|approval' .local/karpathy-wiki/wiki
```

After an important completed change, capture new sanitized evidence in a new raw
file, locate load-bearing numbers/dates/quotes there before writing them, update
only affected current sections/cross-references, refresh article/index dates and
append to `wiki/log.md`. Record actual verification separately from implementation.
Append new manifest provenance; leave prior captures intact. Plain queries are
read-only. See installed skill templates for Raw links and status annotations.

## Git visibility and safety

The user's 2026-11-11 instruction permanently supersedes the former local-only
policy. Wiki articles/raw/manifest/runbook, `docs/superpowers/` and `AGENTS.md` are
Git-visible. Do not restore their old `.git/info/exclude` rules or broad `/docs/`
ignore. Other existing development Markdown guides are visible too. Visibility is
not staging: this synchronization stages/commits/pushes nothing.

Keep secrets, generated output and private `verification/` ignored. Never force-add
excluded files. Scope future staging explicitly and inspect staged content first.
Do not edit global skills as a side effect of maintaining project documentation.

## Verify the knowledge

From repository root:

```sh
# No positive ignore match for the shareable documentation:
git check-ignore -v --non-matching AGENTS.md .local/karpathy-wiki/wiki/index.md docs/superpowers/README.md
git ls-files --others --exclude-standard -- AGENTS.md .local/karpathy-wiki docs
# Ignored private/generated files must remain protected:
git check-ignore -v .env .local/karpathy-wiki/verification/
# Upstream checker requires Python 3.10+; default python3 here is 3.9:
/Users/stephen/.local/share/uv/python/cpython-3.10-macos-aarch64-none/bin/python3.10 -B /Users/stephen/.codex/skills/karpathy-llm-wiki/scripts/check_evidence.py .local/karpathy-wiki
git diff --check
git status --short
```

Read the checker report: it returns zero even when reporting suspects. Validate
actual Raw references and review literal mismatches; also check article/index
coverage, dates and relative links. The checker does not validate every semantic
claim, external URL or Markdown heading anchor. No need to run the application for
a prose-only change; never claim unrun SQL/device/provider/deployment checks passed.

## Recovery

Missing article links: locate the intended existing page before changing paths.
Missing evidence: add a new capture rather than altering the old one. Unexpected
Git exposure: inspect only affected paths and narrow generated/private exclusions;
do not re-ignore the requested documentation. Preserve unrelated work and avoid
`git clean -fdx`, which can destroy local runtime evidence and ignored configuration.
Application-data recovery is separate: see [database/RECOVERY.md](../../database/RECOVERY.md).
