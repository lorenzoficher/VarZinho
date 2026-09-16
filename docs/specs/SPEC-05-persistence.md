# SPEC-05 — Persistence

**Aggregate:** `HighlightRepository`, `CsvHighlightRepository`
**Milestone:** Phase 2 — Persistence

Highlights must survive between runs. Storage is CSV today and might be a
database later, so everything goes through an interface and nothing else in the
system knows the difference.

This is also what the operator uses: when an athlete asks for a clip, the
operator searches the archive by court and time.

## Contract

```java
public interface HighlightRepository {
    void save(Highlight highlight);
    Optional<Highlight> findById(String id);
    List<Highlight> findAll();
    List<Highlight> findByCourt(int courtNumber);
}

public class CsvHighlightRepository implements HighlightRepository {
    public CsvHighlightRepository(Path file);
}
```

The repository needs no directory of people: a highlight refers to nobody, so
there is nothing to resolve on load.

## File format

```csv
id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb
h-001,2026-09-12T20:14:33Z,3,cam-a1,/clips/h-001.mp4,30,FULL_HD,42.5
h-002,2026-09-12T20:41:02Z,3,cam-a1,/clips/h-002.mp4,30,FULL_HD,41.8
```

Every column after `clipPath` belongs to the `VideoClip`, which is rebuilt on
load. This format is private to `CsvHighlightRepository`; no other class parses
or writes it.

## Behaviours

### B-1 — Saving and reloading

- **AC-1.1** A saved highlight is found by its id
- **AC-1.2** A highlight saved in one repository instance is found by a new
  instance reading the same file — this is the point of the whole aggregate
- **AC-1.3** A reloaded highlight equals the original in timestamp, court,
  camera and clip
- **AC-1.4** A reloaded clip equals the original in path, duration, resolution
  and size
- **AC-1.5** Saving creates the file if it does not exist
- **AC-1.6** Saving writes the header exactly once
- **AC-1.7** Saving a highlight whose id already exists replaces it rather
  than duplicating

### B-2 — Reading the archive

- **AC-2.1** `findAll()` on a missing file returns an empty list, not an error
- **AC-2.2** `findAll()` returns every saved highlight
- **AC-2.3** `findById()` on an unknown id returns empty, never null
- **AC-2.4** `findByCourt()` returns only that court's highlights
- **AC-2.5** Filters return an empty list when nothing matches

There is no `findByType()`: the system does not classify the play. Court and
time are what the operator searches by.

### B-3 — A damaged file does not destroy the archive

One bad line must not cost the whole season. This is the recoverable error in
the system, and the reason `CorruptedRecordException` is checked.

- **AC-3.1** A malformed line raises `CorruptedRecordException`
- **AC-3.2** The exception names the offending line number
- **AC-3.3** Valid records before and after a malformed line are still loaded
- **AC-3.4** A file containing only a header loads as empty
- **AC-3.5** A blank line is skipped without error

### B-4 — The abstraction holds

- **AC-4.1** Every caller depends on `HighlightRepository`, never on the CSV
  class
- **AC-4.2** A second implementation — an in-memory one written for tests —
  satisfies the same tests
- **AC-4.3** No class under `domain/` imports anything from `repository/`

AC-4.2 is worth doing: an `InMemoryHighlightRepository` proves the abstraction
is real and makes every other aggregate's tests faster.

AC-4.3 is why `Operator` has no repository and no `listHighlights()`. The
operator's access to the archive is wired in `Main`, not in the domain.

## Errors

| Condition | Exception |
|---|---|
| Malformed CSV line | `CorruptedRecordException` (checked) |
| Unwritable path | `RepositoryException` |
| Null highlight passed to `save()` | `IllegalArgumentException` |

## Dependencies

Needs `Highlight` (SPEC-03), `VideoClip`, `Resolution`.

Fewer than before: with no author to resolve, this aggregate no longer depends
on `people/` at all. Start with the interface and an in-memory implementation —
those need nobody — and write the CSV version as `Highlight` lands.

## Notes

Use `java.nio.file.Files` and `Instant.parse` / `Instant.toString`, which round
trip ISO-8601 exactly. Do not write a date formatter by hand.

`Resolution` round trips through `Resolution.valueOf(...)` — the enum's own
name, not `label()`, which is for humans.

Tests must use JUnit's `@TempDir`. A test that writes into the project
directory will eventually be committed by somebody.

## Out of scope

Databases, JDBC, ORM, concurrency, transactions, migrations, caching.
