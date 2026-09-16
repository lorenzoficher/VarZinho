# Workflow

How five people work on this repository without blocking each other.

## Branches

| Branch | Purpose | Protected | Approval to merge |
|---|---|---|---|
| `main` | Delivered, presentable state | Yes | One |
| `development` | Integration of finished work | Yes | None |
| `feature/*` | One issue's worth of work | No | — |
| `docs/*` | Documentation only | No | — |
| `fix/*` | Corrections | No | — |

Both `main` and `development` require a **pull request**: direct pushes are
rejected for everyone, repository owner included, and force pushes and branch
deletion are blocked. Every commit reaches `development` through a pull
request, always.

The two branches differ in what it takes to merge. `development` needs no
approval — open the pull request and merge it yourself once the tests pass.
`main` needs **one approval**, because that is the branch that gets
delivered.

`main` only ever receives pull requests from `development`.

## The cycle

```bash
git checkout development
git pull

git checkout -b feature/circular-buffer-overwrite

# work, committing as you go

git push -u origin feature/circular-buffer-overwrite
gh pr create --base development --fill
```

Then squash-merge and delete the branch — no approval is needed to merge
into `development`:

```bash
gh pr merge --squash --delete-branch
```

## Division of work

Work is split **by aggregate**, not by layer. Each person owns a set of
classes and works in files nobody else touches.

| Aggregate | Classes | Spec |
|---|---|---|
| Structure | `Gym`, `Court` | [SPEC-01](specs/SPEC-01-structure.md) |
| Capture | `Camera`, `FixedCamera`, `PtzCamera`, `CircularBuffer`, `Frame` | [SPEC-02](specs/SPEC-02-capture.md) |
| Highlight | `Highlight`, `VideoClip` | [SPEC-03](specs/SPEC-03-highlight.md) |
| People | `Person`, `Athlete`, `Operator` | [SPEC-04](specs/SPEC-04-people.md) |
| Persistence | `HighlightRepository`, `CsvHighlightRepository` | [SPEC-05](specs/SPEC-05-persistence.md) |

Splitting by layer was rejected: whoever owned the console would sit idle until
the domain existed, and with eighteen days that is fatal. Splitting by
aggregate lets all five start on day one, in different files, with almost no
merge conflicts.

Shared work — enums, exceptions, `Main`, final integration — is picked up by
whoever finishes their aggregate first.

### Working against classes you do not own

Your aggregate will need types someone else is still writing. Do not wait and
do not edit their files. Write against the interface described in their spec —
the specs exist precisely so that the contract is agreed before the code is.

If a spec turns out to be wrong, open an issue rather than changing someone
else's class in your branch.

## Commits

English, imperative mood, lowercase:

```
add circular buffer overwrite rule
fix camera removal on a court that never had it
document exception hierarchy
```

Not `added...`, not `Adicionei...`, not `update files`.

Commit when something works, not at the end of the day. A pull request with
one enormous commit cannot be reviewed.

## Pull requests

One issue per pull request. If it grows beyond its issue, split it.

The description says what changed and how it was verified. The template
prompts for both.

Before merging — or before requesting review, when the target is `main`:

```bash
mvn test
```

A pull request with failing tests wastes everyone's time, and on `development`
nobody else is there to catch it.

## Review

A pull request into `development` merges without approval. `CODEOWNERS` still
requests a review automatically, so the work is visible and anyone can comment
after the fact; nobody has to wait for it. Ask for a review when you actually
want a second opinion.

A pull request into `main` needs **one approval**. Nobody approves their own —
GitHub does not allow it, which is deliberate.

Reviewing means reading the code and running it, not clicking approve. Say
what is wrong and why; a review that only says "ok" adds nothing.

## Issues

Issues are written as behaviours, not tasks: *"Camera keeps the last 30
seconds available"*, not *"create Camera class"*. What matters is what the
system does when the issue is closed.

Each issue belongs to one aggregate, so it can be worked without waiting for
anyone. Milestones track phases; labels mark aggregate and kind.

| Milestone | Contains |
|---|---|
| Phase 1 — Domain | Structure, capture, highlight, people |
| Phase 2 — Persistence | Repository, CSV, exceptions |
| Phase 3 — Console | `Main`, integration, final documentation |

## Definition of done

An issue is done when:

- [ ] The behaviour it describes works
- [ ] Tests cover its acceptance criteria and pass
- [ ] The code follows [AGENTS.md](../AGENTS.md)
- [ ] Documentation affected by the change was updated
- [ ] The pull request was merged into `development`
