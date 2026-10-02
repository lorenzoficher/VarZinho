# AGENTS.md

Conventions for anyone — human or agent — writing code in this repository.
Read this before your first commit.

## Project

VarZinho: a highlight capture system for sports gyms. Cameras record
continuously into a 30-second circular buffer; pressing a button persists that
window as a permanent clip.

Academic project for AL0330 (Object-Oriented Programming). The goal is
**domain modelling**, not video processing. Since 2.0 a phone's clips are real
`.mp4` files, but Java still decodes no frame: FFmpeg, an external program,
does all the video work behind the `ClipAssembler` interface.

## Build and run

```bash
mvn compile          # compile
mvn test             # run all tests
mvn test -Dtest=CircularBufferTest   # run one test class
mvn exec:java        # open the interface
```

Java 17. Do not use language features above 17 — several team machines and the
presentation machine may run older JDKs.

## Language

**All code, comments, commit messages, branch names, issues and technical
documentation are written in English.** The only exception is `README.md`,
which is in Portuguese.

This includes identifiers: `Highlight`, not `Lance`. `saveHighlight()`, not
`salvarLance()`.

## Package layout

```
src/main/java/br/edu/unipampa/varzinho/
├── domain/          entities and value objects
│   ├── structure/   Gym, Court
│   ├── capture/     Camera, FixedCamera, PtzCamera, StreamCamera, CircularBuffer,
│   │                Frame, ClipAssembler
│   ├── highlight/   Highlight, VideoClip
│   └── people/      Person, Athlete, Operator
├── repository/      HighlightRepository, CsvHighlightRepository
├── stream/          FFmpeg adapter: SegmentLog, SegmentIndex, FfmpegRecorder,
│                    FfmpegClipAssembler — knows the domain, never the reverse
├── exception/       domain exceptions
├── enums/           CameraStatus, Resolution
├── ui/              Swing windows — the only package that imports javax.swing
└── Main.java        entry point, opens the interface

src/test/java/br/edu/unipampa/varzinho/   mirrors the same structure
```

A test lives in the same package as the class it tests, under `src/test`.

## Code conventions

**Encapsulation is not optional.** Every field is `private`. Expose behaviour,
not state: prefer `court.triggerCapture()` over `court.getCameras().get(0)...`.

**No setters, and in this model nothing mutable at all.** A `Highlight`
receives everything at construction and changes nothing afterwards. Objects are
built valid and stay that way; the collections a `Gym` or a `Court` owns grow
through named behaviour (`addCourt`, `installCamera`, `registerPerson`), never
through a setter handing out the list.

**Constructors validate.** If an argument would produce an invalid object,
throw. An object that exists is an object that is valid.

**Naming:**

| Kind | Convention | Example |
|---|---|---|
| Class | `PascalCase`, singular noun | `CircularBuffer` |
| Interface | `PascalCase`, no `I` prefix | `HighlightRepository` |
| Method | `camelCase`, verb phrase | `triggerCapture()` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_BUFFER_SECONDS` |
| Test | `<Class>Test` | `CircularBufferTest` |

**Comments explain why, never what.** The code already says what it does. If a
line needs a comment to be understood, first try renaming something.

## Testing

We follow TDD: write the failing test first, then the code that passes it.
See [docs/TESTING.md](docs/TESTING.md).

Test behaviour that can break — buffer overwriting, repository round-trips,
capture rules, exception paths. Do not write tests for getters and
constructors that only assign fields.

Test method names describe the behaviour:

```java
@Test
void overwritesOldestFrameWhenBufferIsFull() { ... }
```

Not `testBuffer1()`.

## Exceptions

Capture failures use the domain exceptions defined in `exception/`. Invalid
arguments use `IllegalArgumentException`, and invalid camera state transitions
use `IllegalStateException`. Persistence failures use `RepositoryException`.
Never throw a raw `RuntimeException` or `Exception`, and never swallow an
exception with an empty `catch`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Dependencies

The domain must not depend on persistence. `Highlight` never imports anything
from `repository/`. The dependency points inward: repository knows domain,
domain knows nothing.

Adding a third-party library requires agreement from the team — open an issue
first. Right now the only dependency is JUnit 5.

FFmpeg is a **runtime** dependency, not a library: an external program on the
`PATH`, needed only when `VARZINHO_STREAM_URL` is set. The build and every test
pass without it; the one test that runs it is skipped where it is missing.

## Git

Branches: `feature/<short-description>`, `docs/<short-description>`,
`fix/<short-description>`. In English, kebab-case.

Commits: imperative mood, English. `add circular buffer overwrite rule`, not
`added` or `adicionei buffer`.

Never push to `main` or `development` — both are protected and every commit
arrives through a pull request. A pull request into `development` merges
without approval; one into `main` needs one approval. See
[docs/WORKFLOW.md](docs/WORKFLOW.md).

## Scope discipline

The deadline is 2026-09-30 and the team has five people. Before adding
anything not in [docs/PRD.md](docs/PRD.md), open an issue and get agreement.
Features listed as out of scope stay out of scope — including a real database.

Real video capture used to be on that list too. VarZinho 2.0 took it off, for
one phone and through FFmpeg only; the reasoning is in `CONTEXT.md`.

The graphical interface used to be on that list and no longer is:
[docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md) requires one. It stays
**minimal** — the brief's word — and it owns no business rule.

The brief is the contract. Where it and these conventions disagree, the brief
wins, and the disagreement gets fixed here rather than worked around in a
branch.
