# CONTEXT

The shared vocabulary of this project, and the reasoning behind the decisions
that shaped it. When a term in this glossary appears in code, it means exactly
what it means here.

## The problem

A sports gym has courts. Courts have cameras. A good play happens — a goal, a
save, a dunk — and by the time anyone reacts, it is over. Nobody can start
recording a moment that already ended.

So the cameras never stop recording. Each one keeps the most recent 30 seconds
in memory and throws away everything older. When someone presses the button,
those 30 seconds are frozen and written to disk as a permanent clip.

## Glossary

**Highlight** — a play worth keeping, captured and persisted. The central
entity of the system. Carries when it happened, what kind of play it was,
which court and camera produced it, the resulting clip, and optionally the
athlete who performed it.

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

**Court** — a playing surface inside a gym, dedicated to one sport. Owns its
cameras.

**Camera** — equipment mounted on a court, continuously recording into its own
buffer. Has status (active, inactive, maintenance).

**Operator** — the person who presses the button. Known to the system as a
registered person, but *not* recorded on the highlight, because the button
carries no identity.

**Athlete** — the player who performed the highlight. Optional, and filled in
later by a human who watched the clip.

## Decisions

Each decision records what was chosen and, more importantly, why — so that a
future reader does not undo it by accident.

### The trigger preserves the past, it does not start the future

Every alternative design fails the same way: if the button starts a recording,
the play is already gone. The circular buffer exists because of this, and it
is the reason `CircularBuffer` holds real behaviour rather than being a list.

### The highlight's author is optional (`0..1`)

At capture time the system genuinely does not know who made the play. There is
no login, no identification, no user session — just a button being pressed.
Requiring an author would force the code to invent data it does not have.

An author may be assigned later, manually, by someone who watches the clip and
recognises the player. This is the only mutation allowed on a `Highlight`.

This decision also settles a scope question: no GUI is needed for capture,
because there is nobody to interact with at that moment.

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

### No graphical interface

The focus is object-oriented modelling: classes, relationships, business
rules. The real-world flow reinforces this — the camera records on a button
press with no interaction layer, and clips end up in a folder. A GUI would add
work in Swing without demonstrating a single additional OOP concept.

The domain is nonetheless kept independent of any interface, so a GUI could be
added later without changing the model.

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
| Team | 5 people |
| Required syllabus topics | Abstraction, associations, encapsulation, inheritance, polymorphism, exception handling |

## Open questions

These are unresolved and marked deliberately. Do not silently assume an answer.

- **The official assignment brief has not been read.** It lives on the
  Codefólio platform (`codefolio.com.br/cursos/poo`) and no local copy exists.
  If it imposes requirements that contradict anything here — a mandatory GUI,
  a required diagram format, a different deliverable — this document loses.
- **GitHub usernames of the other four members** are unknown. `CODEOWNERS`
  and issue assignment are incomplete until they are filled in.
- **Whether the instructor expects a UML diagram** in a specific tool. The
  Mermaid diagram in `docs/DOMAIN-MODEL.md` may need to be rebuilt in
  draw.io or Astah.
