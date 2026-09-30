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

- **JUnit 5 is the only dependency, and that is a decision, not an accident.** Adding
  a third-party library requires an issue and the team's agreement first.
- **The dependency arrow points inward.** `domain/` never imports from `repository/`.
  Persistence knows the domain; the domain knows nothing about persistence.
- **All persistence goes through the `HighlightRepository` interface.** CSV is
  today's implementation (`CsvHighlightRepository`), not the contract.

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

- No real database.
- No real video capture: `VideoClip` is metadata only, and no frame is ever decoded.

**A graphical interface was on this list until 2026-09-18.** Section 2.9 of
[`docs/ASSIGNMENT-BRIEF.md`](../../docs/ASSIGNMENT-BRIEF.md) requires one, so it is
now required here too: minimal, in Swing, confined to `ui/`, owning no rule. The
reversal and its reasoning are in `CONTEXT.md`.

## Language

All code, comments, commit messages, branch names, issues and technical
documentation are in English, identifiers included (`Highlight`, never `Lance`).
`README.md` is the only exception.

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
| Must be demonstrated | Abstraction, associations, encapsulation, inheritance, polymorphism, exception handling, a minimal Swing interface, `java.time` |
| Must be delivered | Source in packages, `README.md` with the checklist filled in, screenshots of the interface, commit history |

`main` and `development` are protected: no direct pushes, no force pushes, every
commit through a pull request. Merging into `development` needs no approval;
merging into `main` needs one. See [`docs/WORKFLOW.md`](../../docs/WORKFLOW.md).

Because the work is graded on OOP concepts, a change that simplifies the model by
removing one of the required topics is not an improvement here.

## Open questions

Tracked in [`CONTEXT.md`](../../CONTEXT.md) and as issues #21–#23. Not repeated here.
