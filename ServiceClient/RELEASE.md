# Release Process

## Purpose

This document defines how a Maplenou Customer Service release is prepared, validated, documented, and published.

## Release checklist

- [ ] Development matrix contains no untracked release-blocking work.
- [ ] Required tests pass.
- [ ] Documentation builds with `python documentation/build.py --strict` (six sites, FR and EN).
- [ ] French and English texts are both updated (app strings, documentation pages, assistant texts).
- [ ] CHANGELOG contains the release changes.
- [ ] Version follows Semantic Versioning.
- [ ] Release notes are prepared.
- [ ] CI is green.
- [ ] Security-sensitive changes have been reviewed.
- [ ] Git tag is created using the project version.

## Versioning

Use Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

- MAJOR: incompatible/breaking changes
- MINOR: backward-compatible functionality
- PATCH: backward-compatible fixes

## Release principle

A release must describe what was actually implemented. Planned work must not be presented as released functionality.
