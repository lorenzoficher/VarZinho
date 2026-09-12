# Product Requirements — VarZinho

**Status:** approved · **Deadline:** 2026-09-30 · **Team:** 5

## Problem

A play worth remembering is over before anyone can react to it. In amateur
gyms there is no production crew and no replay operator — if the moment was
not already being recorded, it is lost.

Recording everything is not a solution either. A full evening of footage is
hours of nothing punctuated by a handful of moments people actually want, and
nobody scrubs through six hours of video to find a goal.

## Solution

Cameras record continuously into a circular buffer holding the last 30
seconds. A button on the court freezes that window into a permanent clip.
Everything older is discarded automatically and never reaches disk.

The result is an archive containing only the moments somebody cared about, each
one catalogued by time, court, play type and — when a human fills it in — the
athlete who performed it.

## Goals

1. Model the domain in Java with correct object-oriented design
2. Demonstrate encapsulation, inheritance, polymorphism, abstraction,
   associations and exception handling
3. Produce a working, testable system exercised from the console
4. Keep persistence swappable behind an abstraction

## Non-goals

Stated explicitly so they are not mistaken for omissions:

| Not doing | Why |
|---|---|
| Real video capture or encoding | Would consume the entire schedule and demonstrate no OOP |
| Graphical interface | Not the focus; capture has no interaction layer by nature |
| Database | CSV is sufficient at this scale; the interface allows a later swap |
| User accounts, login, authentication | No identity exists in the capture flow |
| Live streaming, sponsors, multi-angle | Real products do this; out of scope here |
| Automatic play detection | Listed as future work |

## Users

**Operator** — works at the gym, presses the button when something happens,
later retrieves clips for players who ask. Needs capture to be instant and
retrieval to be searchable.

**Athlete** — played the match, wants the clip of their goal. Never touches the
system directly; asks the operator.

**Gym manager** — registers courts and cameras, wants to know the equipment is
working.

## Functional requirements

### FR-1 — Continuous recording

Each active camera records continuously into its own circular buffer. The
buffer holds a fixed window (default 30 seconds) and overwrites the oldest
content when full. An inactive camera does not record.

### FR-2 — Highlight capture

Triggering a capture on a court produces a highlight from the current buffer
contents of its cameras. The highlight records the moment of the trigger, the
play type, the originating court and camera, and the resulting clip.

Triggering a court with no active camera is an error, not a silent no-op.

### FR-3 — Optional authorship

A highlight is created without an author. An author may be assigned afterwards
exactly once. Attempting to capture an author at trigger time is impossible by
design: no identity exists at that moment.

### FR-4 — Persistence

Highlights survive between executions. They are written to a CSV file through
the `HighlightRepository` abstraction. No other class knows the storage format.

### FR-5 — Retrieval

The archive can be listed in full, filtered by court, by date and by play type,
and a single highlight can be fetched by its identifier.

### FR-6 — Equipment management

A gym registers courts; a court has cameras installed and removed. Camera
status (active, inactive, maintenance) determines whether it records.

## Non-functional requirements

- **Java 17.** No language feature above 17, so the project compiles on every
  team machine and on the presentation machine.
- **Tested.** Behaviour is developed test-first. See `TESTING.md`.
- **Domain independent of infrastructure.** `domain/` imports nothing from
  `repository/`.
- **English throughout**, except `README.md`.

## Acceptance criteria

The project is done when:

- [ ] `mvn test` passes with every spec's criteria covered
- [ ] `mvn exec:java` demonstrates the full flow: register gym and court,
      install camera, record, trigger, persist, reload, assign author, list
- [ ] Highlights written in one run are readable in the next
- [ ] Every syllabus topic appears in the code with a defensible reason
- [ ] Every document in `docs/` reflects the code as shipped

## Future work

Deliberately deferred, recorded so the scope boundary is visible:

- Multi-angle capture (many cameras per highlight, N:N)
- `Match` grouping highlights, with teams and score
- Database repository implementation
- Automatic play detection
- Sponsor and live-streaming features
- Graphical archive browser

## Risks

| Risk | Mitigation |
|---|---|
| Official brief contradicts these decisions | Read it on Codefólio and reconcile early |
| TDD unfamiliar to the team — time lost to tooling | Maven preconfigured; test only real logic, never accessors |
| Five people editing the same files | Work split by aggregate, no cross-dependency |
| Scope creep near the deadline | Non-goals are explicit; new work needs an issue |
