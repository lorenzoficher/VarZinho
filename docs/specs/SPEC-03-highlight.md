# SPEC-03 — Highlight

**Aggregate:** `Highlight`, `VideoClip` · **Milestone:** Phase 1 — Domain

What the system exists to produce: a preserved play, catalogued, with the clip
that proves it.

## Contract

```java
public class Highlight {
    public Highlight(String id, Instant capturedAt, int courtNumber,
                     String cameraId, VideoClip clip);
    public String describe();
}

public class VideoClip {
    public VideoClip(String filePath, int durationSeconds,
                     Resolution resolution, double sizeMb);
    public boolean exists();
}
```

`Highlight` is immutable. It has no setter, no `assignAuthor`, and no field
that changes after construction — see [CONTEXT.md](../../CONTEXT.md), *A
highlight has no author*.

## Behaviours

### B-1 — A highlight records the moment

Created at the instant of the trigger, carrying everything the system knows at
that point — which is the time, the place and the equipment, and nothing about
any person.

- **AC-1.1** A highlight exposes the timestamp it was captured at
- **AC-1.2** A highlight exposes the court number and camera id it came from
- **AC-1.3** A highlight exposes its clip
- **AC-1.4** Two highlights never share an id
- **AC-1.5** A highlight created with a null clip is rejected
- **AC-1.6** A highlight created with a blank id is rejected
- **AC-1.7** A highlight created with a null timestamp is rejected
- **AC-1.8** Nothing on a highlight changes after construction

AC-1.8 is not a test of a setter that does not exist — it is the reason every
field is `final` and the clip is never handed out for modification.

### B-2 — A highlight describes itself

Used by the console listing and by the CSV writer's caller.

- **AC-2.1** The description includes the timestamp, the court and the camera
- **AC-2.2** The description includes the clip's duration
- **AC-2.3** The description names no person, because a highlight knows none

### B-3 — A clip carries its metadata

No bytes are read. The clip is a record of a file, not the file.

- **AC-3.1** A clip exposes path, duration, resolution and size
- **AC-3.2** A clip with a blank path is rejected
- **AC-3.3** A clip with non-positive duration is rejected
- **AC-3.4** A clip with negative size is rejected
- **AC-3.5** `exists()` reports whether the path is present on disk

## Errors

| Condition | Exception |
|---|---|
| Null clip or timestamp | `IllegalArgumentException` |
| Blank id or blank clip path | `IllegalArgumentException` |
| Non-positive duration | `IllegalArgumentException` |
| Court number not positive, or blank camera id | `IllegalArgumentException` |

## Dependencies

Needs `Resolution`.

This is the aggregate with the fewest dependencies of the ones that matter —
it needs one enum and nothing else.

## Notes

A highlight does not store its own duration; the clip already has it. Ask
`clip` rather than copying the number onto two objects that can then disagree.

The court and camera are stored as values — `int courtNumber`, `String
cameraId` — not as references to `Court` and `Camera`. A highlight outlives
both, and the CSV round-trip has to reproduce it without rebuilding a camera.

## Out of scope

Playback, editing, trimming, sharing, export formats. A highlight knows what it
is and where its file lives; it does not open it.

Authorship. Nobody is recorded on a highlight — not the player, not whoever
pressed the button. The button carries no identity, and an athlete who wants a
clip asks the operator, who searches by court and time.
