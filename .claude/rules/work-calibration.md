# Work calibration

> How **this project** divides work: when an issue is too big for one go, and what
> counts as a layer when slicing commits.
>
> Seeded by the ARK and filled in by `adopt-repo`. An empty section means the ARK's
> generic defaults apply. Nothing in the ARK overwrites what is written here.

---

## Work breakdown

> Consumed by `complexity-guide.md` in `/start-issue`, which decides whether an issue
> becomes a direct implementation or a plan of sub-tasks.

**The unit here is the aggregate, not the module or the directory.** There are five:

| Aggregate | Classes |
|---|---|
| Structure | `Gym`, `Court` |
| Capture | `Camera`, `FixedCamera`, `PtzCamera`, `StreamCamera`, `CircularBuffer`, `Frame`, `ClipAssembler`, `Aimable` |
| Highlight | `Highlight`, `VideoClip` |
| People | `Operator` |
| Persistence | The ports in `application/port/` and their adapters in `infrastructure/persistence/` |

**An aggregate cuts across the layers.** Its entities live in `domain/`, the use
case that drives them in `application/`, and its adapter in `infrastructure/`: the
second assignment gave `ManageGym`, `GymSession` and `ConnectStreamCamera` to
structure, `CaptureHighlight` to highlight, `BrowseArchive` and `CameraCsvFormat`
to persistence, and `Aimable` to capture (see
[`docs/PLAN-CLEAN-ARCHITECTURE.md`](../../docs/PLAN-CLEAN-ARCHITECTURE.md)). The
layers themselves — `ArchitectureTest`, and `bootstrap/` as the composition root —
are shared code.

**One aggregate per issue. An issue that touches two is already too big — split it
into two.** The threshold is this tight on purpose: five people work in parallel, each
owning one aggregate, and the split exists so nobody waits and nobody edits someone
else's files. An issue that spans aggregates hands one person work in files another
person owns, which is exactly what `docs/WORKFLOW.md` is designed to prevent.

Shared code — enums, exceptions, `Main`, `bootstrap/`, final integration — is the one area with no
single owner. Work there is picked up by whoever finishes their aggregate first, and
still follows one issue per pull request.

When you need a type someone else is still writing, code against the interface in
their spec under `docs/specs/`. Do not wait for it, and do not edit their files. If
the spec turns out to be wrong, open an issue instead of changing their class in your
branch.

---

## Layers of this project

> Consumed by `atomicity-rules.md` in `/commit`, which groups changes into atomic
> commits.

**Deliberately empty — the generic defaults apply.** This project is not sliced by
layer, it is sliced by aggregate, and the aggregate boundary is already enforced by
the work breakdown above. One domain per commit is enough here.

Splitting commits by package (`domain/` against `infrastructure/`) would also fight TDD:
a test and the code that makes it pass belong in the same commit.
