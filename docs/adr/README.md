# Architecture Decision Records

This directory records the significant architectural decisions behind Link
Clear, using a lightweight [MADR](https://adr.github.io/madr/)-style format.

These ADRs were written **retroactively** after the initial v1 implementation,
to document *why* the code looks the way it does for future contributors. They
describe decisions that are already reflected in the codebase.

Each record is immutable once accepted. If a decision changes, add a new ADR
that supersedes the old one rather than editing history.

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-bundle-and-reuse-clearurls-ruleset.md) | Bundle and reuse the ClearURLs ruleset | Accepted |
| [0002](0002-no-background-clipboard-monitor.md) | No background clipboard monitoring | Accepted |
| [0003](0003-opt-in-network-unshortening.md) | Opt-in, isolated network unshortening | Accepted |
| [0004](0004-three-module-split.md) | Three-module split (`:core` / `:unshorten` / `:app`) | Accepted |
| [0005](0005-gpl3-and-fdroid-first.md) | GPL-3.0 license and F-Droid-first distribution | Accepted |
| [0006](0006-tooling.md) | Build tooling and quality gates | Accepted |
| [0007](0007-release-and-packaging.md) | Release and packaging | Accepted |
