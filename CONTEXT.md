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
30 seconds are frozen and written to disk as a permanent clip.

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

**Video clip** — the persisted file produced by a trigger. In this project it
is metadata only: path, duration, resolution, size. No bytes are read or
written.

**Court** — a playing surface inside a gym. Owns its cameras.

**Camera** — equipment mounted on a court, continuously recording into its own
buffer. Has status (active, inactive, maintenance).

**Athlete** — the player. Presses the button when something worth keeping
happens, and asks the operator for the clip afterwards. Registered at the gym,
never recorded on a highlight.

**Operator** — gym staff, and the person who holds the archive. When an
athlete asks for a clip, the operator is who searches for it. Registered at
the gym, never recorded on a highlight either.

## Decisions

Each decision records what was chosen and, more importantly, why — so that a
future reader does not undo it by accident.

### The trigger preserves the past, it does not start the future

Every alternative design fails the same way: if the button starts a recording,
the play is already gone. The circular buffer exists because of this, and it
is the reason `CircularBuffer` holds real behaviour rather than being a list.

### A highlight has no author

The button carries no identity. There is no login, no badge reader, no
session — somebody pressed a button on a wall. Recording who performed the
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

### The gym keeps the register of people

Athletes and operators are registered at the gym — not at a court, and not on
a highlight. A person exists before the gym registers them and goes on
existing afterwards, which makes this aggregation, like the gym's cameras and
unlike its courts.

### A court aggregates its cameras, it does not compose them

A camera is movable equipment. It can be uninstalled from court 1 and mounted
on court 2, it can be sent for repair, it can be replaced. It outlives the
court's ownership of it.

That makes the relationship **aggregation**, not composition. Contrast with
the gym and its courts, which is composition: demolish the gym and the court
ceases to exist.

### A camera does not own its highlights

Removing a broken camera must not delete the goals it recorded last month.
Once persisted, a highlight has its own life. The camera is recorded on the
highlight as its origin — an association, not ownership.

### Persistence hides behind an interface

Highlights are stored in a CSV file today. A database would be better later.
Rather than couple the whole system to CSV, all persistence goes through
`HighlightRepository`, an interface. `CsvHighlightRepository` implements it now;
a `DatabaseHighlightRepository` could replace it without touching any other
class.

This is dependency inversion, and it is the project's strongest justification
for using an interface — the domain depends on an abstraction, never on a
concrete storage mechanism.

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

### No real video capture

`VideoClip` stores metadata. Integrating a camera library would consume the
entire schedule and demonstrate nothing about OOP. This is stated explicitly
so nobody mistakes it for an oversight.

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

## Open questions

These are unresolved and marked deliberately. Do not silently assume an answer.

- **GitHub usernames of the four other members** are unknown. `CODEOWNERS`
  and issue assignment are incomplete until they are filled in.
- **How far the interface has to reach.** The brief asks for a minimal one:
  enter data, run at least one operation, see the result. Whether the archive
  listing also deserves a screen, or the capture flow alone is enough, is a
  judgement nobody has made yet.

Answered by the brief. Kept so nobody reopens them:

- ~~The official assignment brief has not been read.~~ It is now
  [docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md), and it required a
  graphical interface — which reversed the decision above.
- ~~Whether the instructor expects a UML diagram in a specific tool.~~ The
  deliverables in section 4 are source code, a `README.md`, screenshots and
  commit history. No diagram is asked for, so the Mermaid one in
  `docs/DOMAIN-MODEL.md` stays as it is.
