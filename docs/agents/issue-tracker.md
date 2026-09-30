# Issue tracker: GitHub

Issues and PRDs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v` — `gh` does this automatically when run inside a clone.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## This repository

Issues live at `lorenzoficher/VarZinho-2.0`. `gh` is authenticated; the repo is inferred
from `git remote -v`.

**Labels mark the aggregate**, which is how work is divided — see
`.claude/rules/work-calibration.md`:

| Label | Aggregate |
|---|---|
| `structure` | `Gym`, `Court` |
| `capture` | `Camera`, `CircularBuffer`, `Frame` |
| `highlight` | `Highlight`, `VideoClip` |
| `people` | `Person`, `Athlete`, `Operator` |
| `persistence` | `HighlightRepository`, CSV storage |
| `shared` | enums, exceptions, cross-cutting code |
| `console` | `Main` and integration |
| `docs` | documentation |

**Milestones mark the phase:** `Phase 1 - Domain`, `Phase 2 - Persistence`,
`Phase 3 - Console`.

**Issues are written as behaviours, not tasks** — *"Camera keeps the last 30 seconds
available"*, not *"create Camera class"*. What matters is what the system does once
the issue is closed. Each issue belongs to one aggregate, so it can be worked without
waiting for anyone. See `docs/WORKFLOW.md`.

There is no GitHub Projects board configured, and the authenticated token has no
`read:project` scope. The ARK's `BOARD_SYNC` toggle is on globally: without a board
`board-move.sh` warns on stderr and carries on, which breaks nothing.
