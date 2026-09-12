# SPEC-03 — Highlight

**Aggregate:** `Highlight`, `VideoClip` · **Milestone:** Phase 1 — Domain

What the system exists to produce: a preserved play, catalogued, with the clip
that proves it.

## Contract

```java
public class Highlight {
    public Highlight(String id, Instant capturedAt, HighlightType type,
                     int courtNumber, String cameraId, VideoClip clip);
    public void assignAuthor(Athlete athlete);
    public boolean hasAuthor();
    public Optional<Athlete> getAuthor();
    public String describe();
}

public class VideoClip {
    public VideoClip(String filePath, int durationSeconds,
                     Resolution resolution, double sizeMb);
    public boolean exists();
}
```

## Behaviours

### B-1 — A highlight records the moment

Created at the instant of the trigger, carrying everything the system knows at
that point.

- **AC-1.1** A highlight exposes the timestamp it was captured at
- **AC-1.2** A highlight exposes its play type
- **AC-1.3** A highlight exposes the court number and camera id it came from
- **AC-1.4** A highlight exposes its clip
- **AC-1.5** Two highlights never share an id
- **AC-1.6** A highlight created with a null clip is rejected
- **AC-1.7** A highlight's timestamp, type, court, camera and clip never change
  after construction

### B-2 — The author is optional and assigned later

The system has no identity at capture time — a button was pressed, nothing
more. Authorship comes later, from a human who watched the clip.

- **AC-2.1** A newly created highlight has no author
- **AC-2.2** `getAuthor()` on a fresh highlight returns empty, never null
- **AC-2.3** Assigning an author makes `hasAuthor()` true
- **AC-2.4** The assigned athlete is the one returned
- **AC-2.5** Assigning an author to a highlight that already has one throws
  `AuthorAlreadyAssignedException`
- **AC-2.6** A failed assignment leaves the original author untouched
- **AC-2.7** Assigning null is rejected

Authorship is the only mutable state on a highlight, and it changes through
`assignAuthor()` — never a setter.

### B-3 — A highlight describes itself

Used by the console listing and by the CSV writer's caller.

- **AC-3.1** The description includes the timestamp, type and court
- **AC-3.2** The description of an unauthored highlight says so, rather than
  showing an empty field
- **AC-3.3** The description of an authored highlight names the athlete

### B-4 — A clip carries its metadata

No bytes are read. The clip is a record of a file, not the file.

- **AC-4.1** A clip exposes path, duration, resolution and size
- **AC-4.2** A clip with a blank path is rejected
- **AC-4.3** A clip with non-positive duration is rejected
- **AC-4.4** A clip with negative size is rejected
- **AC-4.5** `exists()` reports whether the path is present on disk

## Errors

| Condition | Exception |
|---|---|
| Assigning an author twice | `AuthorAlreadyAssignedException` |
| Null clip, type or timestamp | `IllegalArgumentException` |
| Blank id or blank clip path | `IllegalArgumentException` |
| Non-positive duration | `IllegalArgumentException` |

## Dependencies

Needs `Athlete` (SPEC-04), `HighlightType`, `Resolution`,
`AuthorAlreadyAssignedException`.

## Notes

Return `Optional<Athlete>` rather than a nullable athlete. The optional author
is a domain decision, and the type should say so — a caller that forgets to
check gets a compile-time nudge instead of a `NullPointerException` during the
presentation.

## Out of scope

Playback, editing, trimming, sharing, export formats. A highlight knows what it
is and where its file lives; it does not open it.
