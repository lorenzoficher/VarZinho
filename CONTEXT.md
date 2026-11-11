# CONTEXT

The shared vocabulary of this project, and the reasoning behind the decisions
that shaped it. When a term in this glossary appears in code, it means exactly
what it means here.

## The problem

A sports gym has courts. Courts have cameras. A good play happens and, by the
time anyone reacts, it is over. Nobody can start recording a moment that
already ended.

So the cameras never stop recording. Each one keeps the most recent 30 seconds
in memory and throws away everything older. When the button is pressed, those
30 seconds are frozen and written to disk as a permanent clip, so every clip
lasts 30 seconds. A camera switched on less than 30 seconds ago has no clip to
give yet.

The athlete who just played is the one who presses the button. Later they ask
the operator for the clip, and the operator — who holds the whole archive —
looks it up by court and time.

## Glossary

**Highlight** — a play worth keeping, captured and persisted. The central
entity of the system. Carries when it happened, which court and camera
produced it, and the resulting clip.

Portuguese speakers say *lance*. In code it is always `Highlight`.

**Circular buffer** — fixed-size memory that holds the most recent N seconds
of footage. Writing past the end wraps around and overwrites the oldest
content. Not a queue: nothing is ever explicitly removed, it is simply
overwritten.

**Trigger** — the act of pressing the button. Freezes the buffer content into
a `VideoClip` and produces a `Highlight`. The system has no concept of who
triggered it.

**Video clip** — the persisted file produced by a trigger: path, duration,
resolution, size. A clip from a stream camera is a real `.mp4` on disk; a clip
from a fixed or PTZ camera is metadata only, naming a file nothing writes.
Java reads and writes no video bytes either way.

**Stream camera** — a camera that is not mounted on the court but streams to
it over the network: a phone on a tripod. It records like any other camera;
the difference is that its trigger writes a real file.

**Segment** — one second of a stream camera's footage, as a file FFmpeg
writes into a ring that overwrites itself. A finished segment is one `Frame`,
so the circular buffer counts segments exactly as it counted simulated
seconds.

**Court** — a playing surface inside a gym. Owns its cameras.

**Camera** — equipment mounted on a court, continuously recording into its own
buffer. Has status (active, inactive, maintenance).

**Athlete** — the player. Presses the button when something worth keeping
happens, and asks the operator for the clip afterwards. A role, not a record:
the system registers no athlete and never records one on a highlight.

**Operator** — gym staff, and the person who holds the archive. When an
athlete asks for a clip, the operator is who searches for it. The only person
the system knows: an account with a name, a username and a password, which
must sign in before the window opens. Never recorded on a highlight.

**Sign in** — an operator proving who they are with their username and
password. Opens the window; closing the window ends it. It says nothing about
who pressed the button.

## Decisions

Each decision records what was chosen and, more importantly, why — so that a
future reader does not undo it by accident.

### The trigger preserves the past, it does not start the future

Every alternative design fails the same way: if the button starts a recording,
the play is already gone. The circular buffer exists because of this, and it
is the reason `CircularBuffer` holds real behaviour rather than being a list.

### A highlight has no author

The button carries no identity. There is no badge reader and no session at
the button — somebody pressed a button on a wall. The operator signing in to
the window changes nothing here: whoever is signed in did not press the button. Recording who performed the
play, or even who pressed it, would mean inventing data the system never had.

It is also unnecessary. An athlete who wants their clip asks the operator, who
searches the archive by court and time. That is how the gym already works, and
no name has to be stored on the highlight for it to work.

The consequence is that `Highlight` is entirely immutable: every field is set
at construction, and nothing in the model changes after the object exists.

### The system does not classify the play

An earlier draft gave every highlight a type — goal, save, dunk, block — and
every court a sport. Both are gone.

The focus is football, but the system has to serve whatever a gym plays. A
closed vocabulary of plays is per-sport by definition: it would grow with every
sport added, and every class holding it would change along. A highlight is a
preserved window of footage; what happened inside it is for whoever watches.

`CameraStatus` and `Resolution` stay, because those are properties of the
equipment and do not vary by sport.

### Operators are accounts, apart from the gym

**Reversed on 2026-11-11, for the second assignment.** The gym used to keep a
register of people — athletes and operators — as an aggregation: a person
exists before the gym registers them and goes on existing afterwards.

Nothing ever read that register. Athletes were registered but never used, and
cannot be: the button carries no identity, so no athlete can be tied to a play.
The athlete stays in this glossary as a role and leaves the code.

The operator becomes the one person the system knows, because the window now
opens only for an operator who signs in. Credentials change for different
reasons than courts and cameras, so operators are kept apart from the gym, in
a store of their own, rather than saved with its structure every time a camera
is switched on. Aggregation is still shown where it was always clearest: a
court and its movable cameras.

The first operator is created on the first run, when none exists, so no
password lives in the code. Any signed-in operator can register another; there
are no roles. A username is unique regardless of case; a password has at least
eight characters, with an uppercase letter, a lowercase letter, a digit and a
symbol, and is stored only as a hash. A failed sign-in never says whether the
username or the password was wrong.

### A court aggregates its cameras, it does not compose them

A camera is movable equipment. It can be uninstalled from court 1 and mounted
on court 2, it can be sent for repair, it can be replaced. It outlives the
court's ownership of it.

That makes the relationship **aggregation**, not composition. Contrast with
the gym and its courts, which is composition: demolish the gym and the court
ceases to exist.

`Gym.moveCamera()` is that move. The camera arrives switched off, and a camera's
buffer begins when it is switched on: whatever it held belongs to a moment, or
a court, it is no longer watching. So a highlight on the new court never holds
the old court's footage, and a camera stopped and started again never passes
the seconds before the stop off as the present.

### A camera does not own its highlights

Removing a broken camera must not delete the goals it recorded last month.
Once persisted, a highlight has its own life. The camera is recorded on the
highlight as its origin — an association, not ownership.

### Persistence hides behind an interface

Highlights and the gym structure are stored in CSV files today. A database
would be better later. Rather than couple the whole system to CSV, persistence
goes through the `HighlightRepository` and `GymRepository` interfaces. Their
CSV implementations can be replaced without touching the domain.

This is dependency inversion, and it is the project's strongest justification
for using interfaces — the user interface depends on abstractions rather than
concrete storage mechanisms, while the domain stays independent of persistence.

### A minimal Swing interface, because the brief requires one

**Reversed on 2026-09-18.** This project decided against a graphical interface
and argued it at length: the focus is object-oriented modelling, the real-world
flow has no interaction layer — a button is pressed and clips end up in a
folder — and Swing work demonstrates no additional OOP concept.

The brief overrules it. Section 2.9 of
[docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md) requires a minimal
graphical interface in Java, preferably Swing. It is not one checklist line
either: section 1 asks which operations the interface offers, 2.4 asks for a
package of its own, 2.8 requires domain exceptions to be caught and reported
there, and 4.3 makes screenshots a deliverable.

The old reasoning is kept above instead of deleted, because it was not wrong
about the domain — it was wrong about what was being asked. What survives from
it is the part that still holds: the domain stays independent of the interface,
and the screen calls the model without owning a single rule.

### Real video capture, from a phone

**Reversed on 2026-09-30, in VarZinho 2.0.** The delivered 1.0 said:

> `VideoClip` stores metadata. Integrating a camera library would consume the
> entire schedule and demonstrate nothing about OOP.

That held for the graded delivery, and 1.0 stays that way in its own
repository. 2.0 starts from it with one goal: a clip you can open. It gets
there without the cost the old reasoning feared, because Java still touches no
video:

- **FFmpeg does all the video work, as an external program.** One process
  records the phone into a ring of one-second segment files; another joins a
  window of them into an `.mp4` without re-encoding. No Java library is added.
- **A segment is a `Frame`.** The buffer already counted footage in whole
  seconds, so `CircularBuffer`, `Court`, `Highlight` and `VideoClip` did not
  change.
- **The domain sees only an interface.** `StreamCamera` hands its window to a
  `ClipAssembler`; the FFmpeg implementation lives in `stream/`, outside the
  domain, the same way persistence lives behind a repository.
- **The phone is runtime equipment.** It is installed on a court of its own at
  every start and never stored in `gym.csv`, so a court captures from it and
  nothing else, and persistence never has to build an FFmpeg process.

Without `VARZINHO_STREAM_URL` the application behaves exactly like 1.0.

### Use cases between the window and the model

**Reversed on 2026-11-06, for the second assignment.** `ARCHITECTURE.md` said
the business rules live in the domain classes "rather than in a separate
service layer", and the window called the model and the repositories
directly.

The second assignment ([docs/Trabalho 2.md](docs/Trabalho%202.md)) asks for
Clean Architecture, and its central circle is the one this project skipped: the
application's use cases. Without it the window decided the order of a capture —
trigger, write the file, archive — and held entities it could change behind
any rule's back.

What changes is where the *sequence* lives, not where the *rules* live. The
domain stays rich: a court still picks its camera, a camera still guards its
transitions. A use case only orders those calls with the ports, and hands the
window read models instead of entities, so the boundary is something a test can
check rather than something everyone has to remember. The plan is in
[docs/PLAN-CLEAN-ARCHITECTURE.md](docs/PLAN-CLEAN-ARCHITECTURE.md).

### A real database, alongside CSV

**Reversed on 2026-11-11, for the second assignment.** `PRD.md` kept the
database out of scope — "CSV is sufficient at this scale; the interface allows
a later swap" — and the project rules listed it as permanently out.

The second assignment will ask for a database, and the later swap the old
reasoning promised is exactly what it gets: PostgreSQL, run in Docker, reached
through plain JDBC behind the same `HighlightRepository` and `GymRepository`
ports. The CSV adapters stay, and the launcher picks one at start. Two real
adapters for one port is the strongest evidence of dependency inversion the
project can show, and CSV keeps the application opening on a machine with no
Docker.

Plain JDBC rather than an object-relational mapper, because a mapper wants
entities that can be built empty and filled in afterwards, and nothing in this
model may exist invalid or change after construction. The tables are created
by the adapter itself, so the same schema serves Docker and the tests.

When the database is chosen but unreachable, the application refuses to open
instead of falling back to CSV: highlights quietly written to a file would
look lost from the database.

## Constraints

| | |
|---|---|
| Course | AL0330 — Object-Oriented Programming |
| Institution | Unipampa, Alegrete — Software Engineering, 2026/2 |
| Instructor | Silvio Ereno Quincozes |
| Deadline | 2026-09-30 |
| Team | 5 people — Lorenzo Ficher, Lara Rios, Rafael Lopes, Artur Kraemer, Inaurrara Flores |
| Brief | [docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md) — free theme, judged against an 18-item checklist |
| Required syllabus topics | Abstraction, associations, encapsulation, inheritance, polymorphism, exception handling, a minimal Swing interface and `java.time` |

## Resolved questions

No product question remains open for this delivery. The decisions below are
recorded so they are not silently reopened.

- **GitHub ownership.** Every team member's username is known, and
  `.github/CODEOWNERS` identifies the owners of each area.
- **Interface scope.** The delivered window manages courts and cameras,
  captures highlights and lists the persisted archive. This is enough to
  exercise the required flow without moving business rules into Swing.

- ~~The official assignment brief has not been read.~~ It is now
  [docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md), and it required a
  graphical interface — which reversed the decision above.
- ~~Whether the instructor expects a UML diagram in a specific tool.~~ The
  deliverables in section 4 are source code, a `README.md`, screenshots and
  commit history. No diagram is asked for, so the Mermaid one in
  `docs/DOMAIN-MODEL.md` stays as it is.
