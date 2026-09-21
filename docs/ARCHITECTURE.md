# Architecture

How the code is organised, and the two structural decisions that matter:
persistence behind an abstraction, and errors as domain exceptions.

## Layers

```
┌──────────────────────────────────────┐
│  ui/                                 │
│  Swing windows · Main                │
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

**Dependencies point inward.** The interface knows the repository; the
repository knows the domain; the domain knows nothing about either. No class
under `domain/` imports from `repository/`, and no class under `domain/`
imports anything from `javax.swing`.

This is what allows the storage mechanism — or the interface — to change
without touching the model.

Section 2.4 of the brief asks the packages to distinguish three
responsibilities, and these three layers are that split: `domain/` is the
model, `repository/` is the service that stores and retrieves it, and `ui/` is
the interaction. The business rules live in the domain classes rather than in a
separate service layer, which is where a rich object model puts them.

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

### Where they are caught

In `ui/`, and nowhere below it. The domain throws, the repository lets domain
exceptions through, and the window that started the operation catches the
specific type and shows the message — typically with `JOptionPane`.

Section 2.8 of the brief requires exactly this: domain exceptions handled in
the layer that talks to the user, reported as a clear message. A stack trace on
the console is not a message, and a swallowed exception is worse than either.

Triggering a court whose cameras are all inactive is the case to get right.
`NoActiveCameraException` reaches the screen and the user is told why nothing
was captured — which is the whole reason the exception exists instead of a
`null` return.

## The interface

`Main` starts the application and hands control to Swing. The windows exist to
exercise the model end to end, not to be a product: build a gym, register its
people, install cameras, record, trigger a capture, persist, reload from disk,
and list the archive as the operator would.

They are deliberately thin. Every rule lives in the domain and the interface
only calls it. If logic starts accumulating in a window class, it belongs in a
domain class instead — a screen that decides whether a court can be captured
has taken a rule away from `Court`.

The brief asks for a minimal interface, and minimal is the target: fields to
type into, a button that runs a real operation, and somewhere the result and
the error messages appear. Nothing here is graded on looking good.

That the domain stays independent of the screen is the part worth defending. A
`Court` that compiles without `javax.swing` on the classpath is the proof, and
it is why this layer sits on top of the other two rather than inside them.
