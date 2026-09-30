# CLAUDE.md

VarZinho — a highlight capture system for sports gyms. Cameras record continuously
into a 30-second circular buffer; pressing a button preserves that window as a
permanent clip. Academic project for AL0330 (Object-Oriented Programming), Unipampa
Alegrete, 2026/2. The goal is domain modelling, not video processing.

Conventions — build commands, the English-only rule, package layout, code
conventions, testing, exceptions, dependencies and git — live in `AGENTS.md`, which
is their single source of truth. Read it; do not restate it here.

@AGENTS.md

## Documentation

| Document | Contents |
|---|---|
| [CONTEXT.md](CONTEXT.md) | Domain glossary, the decisions and their reasoning, open questions |
| [docs/PRD.md](docs/PRD.md) | What the system does, and what it deliberately does not |
| [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md) | Domain model and class diagram |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, persistence, exception hierarchy |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | Branches, commits, pull requests, division of work |
| [docs/TESTING.md](docs/TESTING.md) | How tests are written (TDD) |
| [docs/specs/](docs/specs/) | Per-aggregate specifications — the contract agreed before the code exists |
| [.claude/rules/](.claude/rules/) | Project constraints, work calibration, Java conventions |

## Current state

`src/` does not exist yet: the repository holds documentation, specs and the Maven
build only. Every class named in `AGENTS.md` and `docs/specs/` is still to be
written, tracked as 23 open issues across three milestones.

Write against the interface described in another aggregate's spec rather than
waiting for its classes to exist — that is what the specs are for.

## Agent skills

### Issue tracker

GitHub Issues on `lorenzoficher/VarZinho-2.0`, via the `gh` CLI. See
`docs/agents/issue-tracker.md`.

### Triage labels

The canonical triage vocabulary, alongside this repo's own aggregate labels. See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` at the repo root. See `docs/agents/domain.md`.
