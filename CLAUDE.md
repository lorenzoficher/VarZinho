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

This is VarZinho 2.0, continuing the delivered 1.0 (tag `VarZinho1.0.0`) in this
same repository. It starts from that release — the full model, the Swing window
and both CSV repositories — and adds real capture from a phone, following
[docs/PLAN-REAL-CAPTURE.md](docs/PLAN-REAL-CAPTURE.md): `StreamCamera` and the
`ClipAssembler` interface in the domain, the FFmpeg adapter in `stream/`, and the
window wiring in `ui/`.

The phone path runs only when `VARZINHO_STREAM_URL` is set; without it the
application is 1.0. It was run end to end on 2026-10-01 with a phone streaming
through DroidCam: the ring kept one segment per second, every capture wrote a
playable 30-second `.mp4`, and closing the window left no FFmpeg process behind.
The phone dropping mid-session has not been exercised yet.

## Agent skills

### Issue tracker

GitHub Issues on `lorenzoficher/VarZinho`, via the `gh` CLI. See
`docs/agents/issue-tracker.md`.

### Triage labels

The canonical triage vocabulary, alongside this repo's own aggregate labels. See
`docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` at the repo root. See `docs/agents/domain.md`.
