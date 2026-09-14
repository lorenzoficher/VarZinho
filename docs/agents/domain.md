# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root, or
- **`CONTEXT-MAP.md`** at the repo root if it exists — it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The producer skill (`/grill-with-docs`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo (most repos):

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

Multi-context repo (presence of `CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← system-wide decisions
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← context-specific decisions
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/grill-with-docs`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_

## This repository

**Single-context.** One `CONTEXT.md` at the root; no `CONTEXT-MAP.md`.

**`docs/adr/` does not exist.** The decisions that would live there are already
recorded in prose in `CONTEXT.md`, under *Decisions* — the trigger preserving the
past, the optional author, aggregation versus composition, persistence behind an
interface, no GUI, no real video capture. Read that section where these instructions
say to read ADRs, and flag a contradiction against it the same way.

Alongside `CONTEXT.md`, three project files carry the rest of the domain:

| File | What it settles |
|---|---|
| `.claude/rules/project-constraints.md` | What cannot change: platform, dependencies, model invariants, permanent non-goals |
| `docs/PRD.md` | Scope — what the system does and what it deliberately does not |
| `docs/specs/` | The contract of each aggregate, agreed before the code exists |

**The glossary is bilingual on purpose.** Domain terms are discussed in Portuguese and
written in English: *lance* is `Highlight`, and only `Highlight` appears in code,
issue titles, test names and branch names. When your output names a concept, use the
English identifier from `CONTEXT.md` — never invent a synonym the glossary avoids.
