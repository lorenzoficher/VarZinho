# Architecture

How the code is organised, and the two structural decisions that matter:
persistence behind an abstraction, and errors as domain exceptions.

## Layers

```
┌──────────────────────────────────────┐
│  Main — console demo                 │
├──────────────────────────────────────┤
│  repository/                         │
│  HighlightRepository (interface)     │
│  CsvHighlightRepository              │
├──────────────────────────────────────┤
│  domain/                             │
│  structure · capture · highlight ·   │
│  people                              │
└──────────────────────────────────────┘
```

**Dependencies point inward.** The console knows the repository; the
repository knows the domain; the domain knows nothing about either. No class
under `domain/` imports from `repository/`.

This is what allows the storage mechanism — or the interface — to change
without touching the model.

## Persistence

Highlights are stored in CSV today. A database would be better eventually. To
avoid coupling the system to either, all persistence goes through an interface:

```java
public interface HighlightRepository {
    void save(Highlight highlight);
    Optional<Highlight> findById(String id);
    List<Highlight> findAll();
    List<Highlight> findByCourt(int courtNumber);
}
```

`CsvHighlightRepository` implements it against a file. A future
`DatabaseHighlightRepository` would implement the same interface, and nothing
else in the system would change — because nothing else in the system mentions
CSV.

This is **dependency inversion**: the high-level policy depends on an
abstraction, not on the low-level detail. It is also the project's clearest
justification for declaring an interface at all.

### The CSV format

```csv
id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb
h-001,2026-09-12T20:14:33Z,3,cam-a1,/clips/h-001.mp4,30,FULL_HD,42.5
h-002,2026-09-12T20:41:02Z,3,cam-a1,/clips/h-002.mp4,30,FULL_HD,41.8
```

No column names a person: a highlight records nobody. The columns after
`clipPath` are the `VideoClip`, rebuilt on load.

The format is an implementation detail of `CsvHighlightRepository`. No other
class parses or produces it.

## Exceptions

Domain errors are signalled with our own exception types, never with raw
`RuntimeException`, and never swallowed.

```
DomainException (abstract, unchecked)
├── NoActiveCameraException
└── EmptyBufferException

RepositoryException (checked)
└── CorruptedRecordException
```

### When each is thrown

**`NoActiveCameraException`** — a capture is triggered on a court whose cameras
are all inactive or in maintenance. Returning `null` or an empty highlight
would hide a real operational failure: somebody pressed the button and got
nothing, and they need to know why.

**`EmptyBufferException`** — a clip is requested from a camera that has not
recorded enough footage yet, typically right after being switched on. The
window asked for does not exist.

**`CorruptedRecordException`** — a line in the CSV cannot be parsed. Checked,
because the caller can reasonably recover: skip the record, report it, and
carry on loading the rest. The archive should not be lost because one line is
malformed.

### Checked or unchecked?

Unchecked for programming and operational errors the caller cannot sensibly
recover from at the call site. Checked for the one case where recovery is a
real decision: a damaged file the caller may want to partially read.

### Rules

- Never `catch (Exception e)` — catch the specific type
- Never leave a `catch` block empty
- Never throw from a getter
- A constructor that receives invalid arguments throws; an object that exists
  is valid

## Console demo

`Main` exists to exercise the model end to end, not to be a product. It builds
a gym, registers its people, installs cameras, records, triggers captures,
persists, reloads from disk, and lists the archive as the operator would.

It is deliberately thin: every rule lives in the domain, and the console only
calls it. If logic starts accumulating in `Main`, it belongs in a domain class
instead.

## Why no GUI

The capture flow has no interaction layer — a physical button, nothing else.
The retrieval flow could justify a screen, and real products have one, but it
would add Swing work without demonstrating any additional OOP concept.

The domain is kept independent of any interface, so a GUI could be added later
as another consumer of the same model. That independence is the part being
graded; the screen is not.
