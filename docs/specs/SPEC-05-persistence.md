# SPEC-05 — Persistence

**Aggregate:** highlight archive and gym structure · **Milestone:** Phase 2 —
Persistence

Highlights and the gym structure survive between executions. Both storage
boundaries are interfaces so callers do not depend on CSV.

## Contracts

```java
public interface HighlightRepository {
    void save(Highlight highlight) throws RepositoryException;
    Optional<Highlight> findById(String id) throws RepositoryException;
    List<Highlight> findAll() throws RepositoryException;
    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}

public interface GymRepository {
    void save(Gym gym) throws RepositoryException;
    Optional<Gym> load() throws RepositoryException;
}
```

`CsvHighlightRepository` and `CsvGymRepository` implement the production
storage. The in-memory implementations satisfy the same interfaces and keep UI
and contract tests independent of the filesystem.

## Highlight archive

```csv
id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb
h-001,2026-09-12T20:14:33Z,3,cam-a1,clips/h-001.mp4,30,FULL_HD,42.5
```

- Saving creates the file and writes the header once.
- Saving an existing id replaces the value without changing its position.
- `findAll()` preserves first-save order; `findByCourt()` returns only the
  requested court; unknown and null ids return `Optional.empty()`.
- Null highlights and text containing a comma or line break are rejected
  before storage changes.
- A malformed record raises `CorruptedRecordException`, names its line and
  carries every valid record recovered before and after it. Filtered queries
  carry only recovered records for the requested court.
- Writes use a temporary file and replace the archive only after the complete
  content has been written.

## Gym structure

```csv
kind,values
gym,VarZinho Arena,100 Sports Avenue
court,1
fixed,1,fixed-1,Fixed Pro,FULL_HD,30,ACTIVE,45
ptz,1,ptz-1,PTZ Pro,HD,30,ACTIVE,0,0,1
```

- `load()` returns empty when no file exists.
- Saving again replaces the previous snapshot.
- Loading restores the gym name and address, court numbers, both camera kinds,
  model, resolution, buffer length, status, fixed angle and PTZ aim.
- Loading rebuilds the model through domain methods. An invalid line, missing
  header, undeclared court or duplicate camera fails the whole load with the
  offending line number; half a gym is never returned.
- Unsupported text is rejected before the existing file changes.
- People are intentionally absent because the Swing interface does not
  register them. Highlights remain in their separate archive.

## Error policy

| Condition | Result |
|---|---|
| Missing highlight or gym file | Empty result |
| Unreadable or unwritable storage | `RepositoryException` |
| Malformed highlight line | `CorruptedRecordException` with recovered highlights |
| Malformed gym structure | `RepositoryException`; no partial gym |
| Null value or unrepresentable text passed to save | `IllegalArgumentException`; stored content unchanged |

## Dependency direction

The repository package depends on domain objects. No class under `domain/`
imports `repository/`. The UI depends on the two interfaces and constructs
the CSV implementations only at application startup.

## Verification

- `HighlightRepositoryContractTest` runs against both highlight
  implementations.
- `CsvHighlightRepositoryTest` covers filesystem round trips, replacement,
  ordering and atomic writes.
- `CsvHighlightRepositoryDamageTest` covers damaged-record recovery and
  filtered recovery.
- `CsvGymRepositoryTest` covers complete gym round trips, replacement,
  invalid files and non-destructive refusal.
- `GymSessionTest` covers loading the saved gym on the next execution and
  falling back without overwriting a damaged file.

## Out of scope

Databases, JDBC, ORM, migrations, concurrent writers and real video files.
