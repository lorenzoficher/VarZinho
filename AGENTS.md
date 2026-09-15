# AGENTS.md

Conventions for anyone — human or agent — writing code in this repository.
Read this before your first commit.

## Project

VarZinho: a highlight capture system for sports gyms. Cameras record
continuously into a 30-second circular buffer; pressing a button persists that
window as a permanent clip.

Academic project for AL0330 (Object-Oriented Programming). The goal is
**domain modelling**, not video processing. No frames are ever decoded.

## Build and run

```bash
mvn compile          # compile
mvn test             # run all tests
mvn test -Dtest=CircularBufferTest   # run one test class
mvn exec:java        # run the console demo
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
│   ├── capture/     Camera, CircularBuffer, Frame, Triggerable
│   ├── highlight/   Highlight, VideoClip
│   └── people/      Person, Athlete, Operator
├── repository/      HighlightRepository, CsvHighlightRepository
├── exception/       domain exceptions
├── enums/           CameraStatus, Resolution
└── Main.java        console entry point

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

Domain errors use our own exceptions, defined in `exception/`. Never throw raw
`RuntimeException` or `Exception`. Never swallow an exception with an empty
`catch`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Dependencies

The domain must not depend on persistence. `Highlight` never imports anything
from `repository/`. The dependency points inward: repository knows domain,
domain knows nothing.

Adding a third-party library requires agreement from the team — open an issue
first. Right now the only dependency is JUnit 5.

## Git

Branches: `feature/<short-description>`, `docs/<short-description>`,
`fix/<short-description>`. In English, kebab-case.

Commits: imperative mood, English. `add circular buffer overwrite rule`, not
`added` or `adicionei buffer`.

Never push to `main` or `development` — both are protected and require a pull
request with one approval. See [docs/WORKFLOW.md](docs/WORKFLOW.md).

## Scope discipline

The deadline is 2026-09-30 and the team has five people. Before adding
anything not in [docs/PRD.md](docs/PRD.md), open an issue and get agreement.
Features listed as out of scope stay out of scope — including a GUI, a real
database, and actual video capture.
