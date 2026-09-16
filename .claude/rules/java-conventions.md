# Code conventions — Java

> Complements the ARK's generic `code-conventions.md`, which is imported from the
> clone by the global installation. Applies only to Java projects.
>
> Naming, encapsulation and the exception policy are **not** repeated here: they live
> in [`AGENTS.md`](../../AGENTS.md), and that file is their single source of truth.
> What follows is the Java-specific detail that `AGENTS.md` does not spell out.

---

## Javadoc

Write it where a reader would otherwise have to guess, and nowhere else.

**1. Class** — one on every public class, stating what it is in domain terms and what
it does not do:

```java
/**
 * Fixed-size memory holding the most recent seconds of footage from one camera.
 *
 * <p>Writing past the end wraps around and overwrites the oldest frame. Nothing is
 * ever explicitly removed — this is not a queue.
 */
public final class CircularBuffer { ... }
```

**2. Method** — required on public methods whose contract is not obvious from the
signature: anything that throws, that may return empty, or that mutates state.

```java
/**
 * Freezes the current buffer content into a clip.
 *
 * @return the highlight produced by this trigger
 * @throws NoActiveCameraException if no camera on the court is active
 */
public Highlight trigger() { ... }
```

**3. Never** document a getter, a constructor that only assigns fields, or an
overridden method whose parent Javadoc already says it. A Javadoc that restates the
method name is noise.

Javadoc says *why* and *what is guaranteed*. Inline comments say *why*, never *what* —
if a line needs a comment to be read, rename something first.

---

## Types

- **`final` on every field that does not change.** With no setters in this model,
  that is nearly all of them; the compiler then enforces the immutability that
  `AGENTS.md` asks for.
- **`final` on the class** unless it is designed to be extended. `Camera` is
  extended by `FixedCamera` and `PtzCamera` — it is not final. `CircularBuffer` is.
- **`Optional<T>` in return types only.** Never as a field, never as a parameter. A
  lookup that may find nothing says so in its type — `Optional<Person>
  findPerson(String)`, `Optional<Highlight> findById(String)` — while the field
  behind it stays a plain reference.
- **Declare the interface, not the implementation:** `List<Frame>`, never
  `ArrayList<Frame>`; `HighlightRepository`, never `CsvHighlightRepository`.
- **No raw types and no wildcard imports.** `List<Frame>`, not `List`; import each
  class by name.
- **Prefer an enum over a boolean or a string constant.** `CameraStatus.ACTIVE`, not
  `isActive` and not `"active"`.

---

## Equality and printing

- `equals` and `hashCode` are overridden together or not at all, and over the fields
  that define identity — not over every field.
- `toString` exists on entities the console prints, and returns something a human
  reads. It is not a debugging dump.
- Collections returned from a getter are wrapped in
  `Collections.unmodifiableList(...)`, or copied. Handing out the internal list
  undoes the encapsulation the rest of the model pays for.

---

## Exceptions

`AGENTS.md` sets the policy — own exceptions from `exception/`, never a raw
`RuntimeException`, never an empty `catch`. Two Java specifics:

- **Chain the cause.** When wrapping, pass it:
  `throw new ArchiveReadException("row " + line, cause);` — an exception that drops
  its cause deletes the stack trace that explains it.
- **Unchecked for programming errors, checked for recoverable ones.** An invalid
  constructor argument is unchecked; a corrupted archive file the caller can skip
  past is checked.
