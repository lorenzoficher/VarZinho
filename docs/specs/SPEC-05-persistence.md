# SPEC-05 — Persistence

**Aggregate:** `HighlightRepository`, `CsvHighlightRepository`
**Milestone:** Phase 2 — Persistence

Highlights must survive between runs. Storage is CSV today and might be a
database later, so everything goes through an interface and nothing else in the
system knows the difference.

## Contract

```java
public interface HighlightRepository {
    void save(Highlight highlight);
    Optional<Highlight> findById(String id);
    List<Highlight> findAll();
    List<Highlight> findByCourt(int courtNumber);
    List<Highlight> findByType(HighlightType type);
}

public class CsvHighlightRepository implements HighlightRepository {
    public CsvHighlightRepository(Path file, AthleteDirectory athletes);
}
```

`AthleteDirectory` resolves a stored document string back into an `Athlete`.
A `Map<String, Athlete>` wrapper is enough.

## File format

```csv
id,capturedAt,type,durationSeconds,courtNumber,cameraId,clipPath,authorDocument
h-001,2026-09-12T20:14:33Z,GOAL,30,3,cam-a1,/clips/h-001.mp4,
h-002,2026-09-12T20:41:02Z,SAVE,30,3,cam-a1,/clips/h-002.mp4,01234567890
```

An empty `authorDocument` means no author — the normal state after capture.

This format is private to `CsvHighlightRepository`. No other class parses or
writes it.

## Behaviours

### B-1 — Saving and reloading

- **AC-1.1** A saved highlight is found by its id
- **AC-1.2** A highlight saved in one repository instance is found by a new
  instance reading the same file — this is the point of the whole aggregate
- **AC-1.3** A reloaded highlight equals the original in timestamp, type,
  court, camera, clip path and duration
- **AC-1.4** Saving creates the file if it does not exist
- **AC-1.5** Saving writes the header exactly once
- **AC-1.6** Saving a highlight whose id already exists replaces it rather
  than duplicating

### B-2 — Authorship round-trips

- **AC-2.1** A highlight with no author reloads with no author
- **AC-2.2** A highlight with an author reloads with an equal athlete
- **AC-2.3** An author document not present in the directory reloads as no
  author, and does not fail the load

### B-3 — Reading the archive

- **AC-3.1** `findAll()` on a missing file returns an empty list, not an error
- **AC-3.2** `findAll()` returns every saved highlight
- **AC-3.3** `findById()` on an unknown id returns empty, never null
- **AC-3.4** `findByCourt()` returns only that court's highlights
- **AC-3.5** `findByType()` returns only that type
- **AC-3.6** Filters return an empty list when nothing matches

### B-4 — A damaged file does not destroy the archive

One bad line must not cost the whole season. This is the recoverable error in
the system, and the reason `CorruptedRecordException` is checked.

- **AC-4.1** A malformed line raises `CorruptedRecordException`
- **AC-4.2** The exception names the offending line number
- **AC-4.3** Valid records before and after a malformed line are still loaded
- **AC-4.4** A file containing only a header loads as empty
- **AC-4.5** A blank line is skipped without error

### B-5 — The abstraction holds

- **AC-5.1** Every caller depends on `HighlightRepository`, never on the CSV
  class
- **AC-5.2** A second implementation — an in-memory one written for tests —
  satisfies the same tests
- **AC-5.3** No class under `domain/` imports anything from `repository/`

AC-5.2 is worth doing: an `InMemoryHighlightRepository` proves the abstraction
is real and makes every other aggregate's tests faster.

## Errors

| Condition | Exception |
|---|---|
| Malformed CSV line | `CorruptedRecordException` (checked) |
| Unwritable path | `RepositoryException` |
| Null highlight passed to `save()` | `IllegalArgumentException` |

## Dependencies

Needs `Highlight` (SPEC-03), `Athlete` (SPEC-04), `VideoClip`,
`HighlightType`, `Resolution`.

Depends on the most other aggregates, so start with the interface and an
in-memory implementation — those need nobody — and write the CSV version as
the others land.

## Notes

Use `java.nio.file.Files` and `Instant.parse` / `Instant.toString`, which round
trip ISO-8601 exactly. Do not write a date formatter by hand.

Tests must use JUnit's `@TempDir`. A test that writes into the project
directory will eventually be committed by somebody.

## Out of scope

Databases, JDBC, ORM, concurrency, transactions, migrations, caching.
