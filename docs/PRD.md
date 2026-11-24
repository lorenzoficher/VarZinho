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

The result is an archive containing only the moments somebody cared about,
each one catalogued by the time it happened and the court and camera it came
from. Nothing is catalogued by who was involved: the button carries no
identity.

## Goals

1. Model the domain in Java with correct object-oriented design
2. Demonstrate encapsulation, inheritance, polymorphism, abstraction,
   associations and exception handling
3. Produce a working, testable system, driven from a Swing interface and
   exercised by automated tests
4. Keep persistence swappable behind an abstraction

## Non-goals

Stated explicitly so they are not mistaken for omissions:

| Not doing | Why |
|---|---|
| Video decoding or encoding in Java | No video library on the classpath. Since 2.0 a phone's clips are real files, but FFmpeg — an external program — writes them; see `CONTEXT.md` |
| Business rules in the interface | The window grew past the brief's minimum — a sidebar workspace with light and dark themes — but every rule stays in the domain, never on the screen |
| Sponsors and multi-angle | The public spectator view in #188 covers a single phone court |
| Automatic play detection | Listed as future work |
| Classifying the kind of play | Per-sport by nature; the system must serve any sport |
| Recording who played or who pressed | No identity exists at the button |

## Users

The extension agreed in [#188](https://github.com/lorenzoficher/VarZinho/issues/188)
lets a remote spectator watch the phone court through a public HTTPS link and reserve
the 30 seconds ending at the displayed playback time. A verified BRL 5.00 Pix
payment grants access to the reserved clip. The operator's local capture remains independent.

**Athlete** — plays the match and presses the button when something worth
keeping happens. Afterwards asks the operator for the clip. Needs capture to be
a single button press, with nothing to fill in.

**Operator** — works at the gym and holds the archive. Retrieves clips for the
athletes who ask, searching by court and time. Needs retrieval to be
searchable.

**Gym manager** — registers courts and cameras, wants to know the equipment is
working.

## Functional requirements

### FR-1 — Continuous recording

Each active camera records continuously into its own circular buffer. The
buffer holds a fixed window (default 30 seconds) and overwrites the oldest
content when full. An inactive camera does not record.

### FR-2 — Highlight capture

Triggering a capture on a court produces a 30-second highlight from its first
active camera's buffer (`Court.DEFAULT_CAPTURE_SECONDS`). The highlight records the moment of the trigger, the
originating court and camera, and the resulting clip. It records nothing
about who played or who pressed.

Triggering a court with no active camera is an error, not a silent no-op.

### FR-3 — Operator accounts

The window opens only after an operator signs in with a username and a
password. On the first run, with no account stored, it asks for the first
operator instead. A signed-in operator registers further operators, and a
forgotten password is reset through a simulated recovery: no e-mail is sent.
Only a salted hash of each password is stored.

Accounts are independent of capture — no operator is ever recorded on a
highlight, because no identity exists at the moment the button is pressed.

### FR-4 — Persistence

Highlights, the gym's structure and the operator accounts survive between
executions. Each goes through its own abstraction — `HighlightRepository`,
`GymRepository` and `OperatorStore` — and is written to CSV files under
`data/` by default, or to PostgreSQL when `VARZINHO_DATABASE_URL` names a
database. No other class knows the storage format.

### FR-5 — Retrieval

The archive can be listed in full, filtered by court, and a single highlight
can be fetched by its identifier. This is what the operator uses when an
athlete asks for a clip.

### FR-6 — Equipment management

A gym registers courts; a court has cameras installed and removed. Camera
status (active, inactive, maintenance) determines whether it records.

### FR-7 — Graphical interface

A Swing interface drives the system: the user supplies data, runs at least
one real operation, and sees the result. It goes beyond the brief's minimum: a
sidebar workspace with light and dark themes, pages for the dashboard, courts,
cameras, highlights, archive, operators and settings, the capture on F2, and
an archive filtered by court and by day whose clips open in the system's
video player. It is the layer that catches
domain exceptions and turns them into a message a person can read — triggering
a court with no active camera reports why nothing was captured, rather than
failing silently or printing a stack trace.

The interface owns no rule. It calls the domain and displays what comes back;
if logic starts accumulating in a window class, it belongs in a domain class
instead.

## Non-functional requirements

- **Java 17.** No language feature above 17, so the project compiles on every
  team machine and on the presentation machine.
- **Tested.** Behaviour is developed test-first. See `TESTING.md`.
- **Domain independent of infrastructure.** `domain/` imports nothing from
  `application/` or `infrastructure/`.
- **English throughout**, except `README.md`.

## Acceptance criteria

The project is done when:

- [x] `mvn test` passes with every spec's criteria covered
- [x] `mvn exec:java` opens the interface and demonstrates the full flow:
      load the gym, create courts, install and control cameras, record, trigger,
      persist, reload and list, after an operator signs in
- [x] A domain exception raised by the model reaches the user as a readable
      message, never as a stack trace
- [x] Highlights written in one run are readable in the next
- [x] Every syllabus topic appears in the code with a defensible reason
- [x] `README.md` carries the brief's checklist, filled in, pointing at where
      each concept was used or saying why it does not apply
- [x] Screenshots of the interface are committed
- [x] Every document in `docs/` reflects the code as shipped

## Future work

Deliberately deferred, recorded so the scope boundary is visible:

- Multi-angle capture (many cameras per highlight, N:N)
- `Match` grouping highlights, with teams and score
- Automatic play detection
- Sponsor features
- Archive search beyond the delivered court, day and text filters

## Risks

| Risk | Mitigation |
|---|---|
| ~~Official brief contradicts these decisions~~ — it did, and required a GUI | Reconciled in #30; the brief now lives in `docs/ASSIGNMENT-BRIEF.md` |
| Swing is new to the team and was never planned for | Started minimal, with its own issue and its own owner; it grew only after the model was done, and never took a rule |
| TDD unfamiliar to the team — time lost to tooling | Maven preconfigured; test only real logic, never accessors |
| Five people editing the same files | Work split by aggregate, no cross-dependency |
| Scope creep near the deadline | Non-goals are explicit; new work needs an issue |
