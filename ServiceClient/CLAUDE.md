@AGENTS.md

# Claude-specific notes

- `AGENTS.md` (imported above) is the single source of truth; update it, not this file, when a rule changes.
- Folder rules: `backend/CLAUDE.md`, `frontend/CLAUDE.md`, `assistant/CLAUDE.md`, `documentation/CLAUDE.md`
  import the `AGENTS.md` next to them.
- Prefer the Docker commands of AGENTS.md §3 when Java, Node or Python are not installed.
- Values for Docker Compose come from `.env` only; when you add one, update `.env.example` and the guide.
- Keep disk use low: no new images or containers without need, bounded caches, `scripts/clean` documented.
- Design mockups live in the Claude Design canvas (see `docs/ux-ui/screens.md`), not in Figma.
- Ports used by the owner: keep the docs on `DOCS_HOST_PORT=8006`; never default anything to port 8000.
- When Docker fails, ask for the output of `scripts\doctor.ps1` before guessing.
