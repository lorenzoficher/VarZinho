# Project constraints

> Seeded once by the ARK and filled in by `adopt-repo`; the ARK never touches it
> again. The generic conventions (language, clean code) come from the ARK clone via
> the global installation — this file is only what holds in **this** repository.
>
> Domain model, decisions and their reasoning: [`CONTEXT.md`](../../CONTEXT.md).

---

## Platform

- **Java 17 (LTS), and no language feature above it.** Team machines and the
  presentation machine may run older JDKs; code that only compiles on 21 cannot be
  demonstrated.
- **Maven, no wrapper, no CI.** `mvn compile`, `mvn test`, `mvn exec:java`. The build
  is verified on each person's machine, so it must depend on nothing but a JDK and
  Maven.

## Dependencies

- **The dependencies are FlatLaf, FlatLaf Extras and MigLayout Swing (compile),
  agreed in #158; the PostgreSQL JDBC driver (runtime) and Testcontainers (test),
  agreed in #145; and JUnit 5 (test). That is a decision, not an accident.** The
  list and its scopes match `pom.xml` and the table in `AGENTS.md`. Adding a
  third-party library requires an issue and the team's agreement first.
- **The window's icons are bundled files, not a dependency.** They come from the
  MIT-licensed Tabler Icons set, live in `src/main/resources/icons/tabler/` with
  their `LICENSE`, and are drawn by FlatLaf Extras through `VarZinhoIcons`.
- **The dependency arrow points inward.** `domain/` never imports from `application/`
  or `infrastructure/`. Persistence knows the domain; the domain knows nothing about
  persistence. The layers are `domain/` (entities), `application/` (use cases, with
  the ports in `application/port/`), `ui/` (presentation, which reaches the model
  only through `application/`) and `infrastructure/` (adapters); `bootstrap/` is the
  composition root, the one place that names a concrete adapter. `ArchitectureTest`
  fails on any import that points outward.
- **All persistence goes through the ports in `application/port/`:**
  `HighlightRepository`, `GymRepository` and `OperatorStore`. Each has a CSV and a
  PostgreSQL adapter (`CsvHighlightRepository` and `PostgresHighlightRepository`,
  and so on); neither is the contract. `bootstrap/Storage` picks one per run:
  PostgreSQL by default (the `docker-compose.yml` database unless
  `VARZINHO_DATABASE_URL` names another), CSV under `data/` only when
  `VARZINHO_STORAGE=csv`, and an unreachable database stops the application rather
  than falling back to CSV.
- **FFmpeg is a runtime dependency, never a build one.** It is an external program
  on the `PATH`, used only when `VARZINHO_STREAM_URL` names a phone stream. `mvn
  compile` and `mvn test` must keep passing on a machine without it.
- **Docker is a runtime dependency, never a build one.** It runs the local
  PostgreSQL and the Testcontainers tests. `mvn test` must keep passing on a machine
  without it; the database tests are skipped there.
- **All video work goes through the `ClipAssembler` interface.** `domain/` never
  imports from `stream/`, exactly as it never imports from
  `infrastructure/persistence/`.

## Model invariants

- Every field is `private`. Expose behaviour, not state.
- No setters. Constructors validate — an object that exists is an object that is
  valid.
- **`Highlight` never mutates.** It is immutable and records no person: the button
  carries no identity, so there is no author to assign later. Equipment does change
  state — a camera starts and stops recording, a buffer is written to — and that
  state changes only through named behaviour that validates first, never a setter.
- **The system classifies neither the sport nor the kind of play.** The focus is
  football, but the model has to serve whatever a gym plays.
- Domain errors use this project's own exceptions in `exception/`. Never a raw
  `RuntimeException`, never an empty `catch`.

## Permanently out of scope

Stated so nobody mistakes them for oversights — each is argued in `CONTEXT.md`:

- No frame decoded in Java, and no video library on the classpath.

**Real video capture was on this list until 2026-09-30.** VarZinho 2.0 records a
phone through FFmpeg and writes real `.mp4` clips for it; fixed and PTZ cameras stay
metadata only. The reversal and its reasoning are in `CONTEXT.md`.

**A graphical interface was on this list until 2026-09-18.** Section 2.9 of
[`docs/ASSIGNMENT-BRIEF.md`](../../docs/ASSIGNMENT-BRIEF.md) requires one, so it is
now required here too: in Swing, confined to `ui/`, owning no rule. It started
minimal, as the brief's word allowed; since the second assignment it is a sidebar
workspace with light and dark themes, and it still owns no rule. Both reversals
and their reasoning are in `CONTEXT.md`.

**A real database was on this list until 2026-11-11.** It came in for the
second assignment, and the code now has it: PostgreSQL in Docker, reached through plain JDBC,
alongside CSV. Operator accounts and sign-in arrived with it, stored the same way.
The reversal and its reasoning are in `CONTEXT.md`.

## Language

All code, comments, commit messages, branch names, issues and technical
documentation are in English, identifiers included (`Highlight`, never `Lance`).
There are two exceptions, both in Portuguese: `README.md`, and the text the window
shows the operator. Identifiers and log messages in `ui/` stay in English; see
`AGENTS.md`.

**This overrides the ARK's generic `code-conventions.md`**, which asks for
documentation in Portuguese and `snake_case` identifiers. In this repository
`AGENTS.md` wins on both counts.

## Delivery

| | |
|---|---|
| Course | AL0330 — Object-Oriented Programming, Unipampa Alegrete, 2026/2 |
| Deadline | 2026-09-30 |
| Team | 5 people, one aggregate each |
| Brief | [`docs/ASSIGNMENT-BRIEF.md`](../../docs/ASSIGNMENT-BRIEF.md) — free theme, 18-item checklist |
| Must be demonstrated | Abstraction, associations, encapsulation, inheritance, polymorphism, exception handling, a Swing interface (the brief's minimum, now exceeded), `java.time` |
| Must be delivered | Source in packages, `README.md` with the checklist filled in, screenshots of the interface, commit history |

`main` and `development` are protected: no direct pushes, no force pushes, every
commit through a pull request. Merging into `development` needs no approval;
merging into `main` needs one. See [`docs/WORKFLOW.md`](../../docs/WORKFLOW.md).

Because the work is graded on OOP concepts, a change that simplifies the model by
removing one of the required topics is not an improvement here.

## Open questions

Tracked in [`CONTEXT.md`](../../CONTEXT.md) and as issues #21–#23. Not repeated here.
