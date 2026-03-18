# Contributing

## Branching and Pull Requests

- Create all changes on a `feature/*` branch.
- Do not commit directly to `main`.
- Merge into `main` only via pull request.
- Ensure all required PR checks pass before merge.

## Release Flow

- Branch from `main` using `feature/*`.
- Make release changes (including `CHANGELOG.md`, `versionName`, and `versionCode`).
- Open a PR to `main` and wait for all required checks to pass.
- Merge PR; GitHub Actions creates tag `vX.Y.Z` and runs release publishing.

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) for all commits.

- Use a conventional type with scope, e.g. `feat(scope): ...`, `fix(scope): ...`
- For release-related commits, use scope `release` or `beta`
- Preferred release titles:
  - `feat(release): v1.x.x`
  - `feat(beta): v1.x.x-beta.x`
- Add a concise body (1-3 short lines) describing key changes

Example:

```text
feat(release): v1.1.12

- Fix interval elapsed/remaining restore drift.
- Move workout hint below formula selector.
```
