# SPEC-01 — Structure

**Aggregate:** `Gym`, `Court` · **Milestone:** Phase 1 — Domain

The physical layout of the venue: a gym containing courts, each court holding
the cameras installed on it and exposing the capture trigger.

## Contract

```java
public class Gym {
    public Gym(String name, String address);
    public void addCourt(Court court);
    public Court findCourt(int number);
    public List<Court> getCourts();          // unmodifiable
}

public class Court {
    public Court(int number, Sport sport);
    public void installCamera(Camera camera);
    public void removeCamera(Camera camera);
    public boolean hasActiveCamera();
    public Highlight triggerCapture(HighlightType type);
    public List<Camera> getCameras();        // unmodifiable
}
```

## Behaviours

### B-1 — A gym holds courts

A gym is created with a name and an address, and courts are added to it.

- **AC-1.1** A newly created gym has no courts
- **AC-1.2** An added court is retrievable by its number
- **AC-1.3** Looking up a number that does not exist returns no court
- **AC-1.4** Adding a court whose number is already taken is rejected
- **AC-1.5** The returned court list cannot be modified from outside

### B-2 — A court has cameras installed and removed

Cameras are equipment. They arrive, they leave, they come back.

- **AC-2.1** A newly created court has no cameras
- **AC-2.2** An installed camera appears in the court's cameras
- **AC-2.3** A removed camera no longer appears
- **AC-2.4** Removing a camera that was never installed is rejected
- **AC-2.5** Installing the same camera twice does not duplicate it

This is aggregation: destroying the court does not destroy its cameras.

### B-3 — A court knows whether it can record

- **AC-3.1** A court with no cameras has no active camera
- **AC-3.2** A court whose only camera is `INACTIVE` has no active camera
- **AC-3.3** A court whose only camera is in `MAINTENANCE` has no active camera
- **AC-3.4** A court with at least one `ACTIVE` camera has an active camera

### B-4 — Triggering a capture produces a highlight

- **AC-4.1** Triggering on a court with an active camera returns a highlight
- **AC-4.2** The highlight records the court's number and the originating camera
- **AC-4.3** The highlight records the play type passed in
- **AC-4.4** The highlight is created without an author
- **AC-4.5** Triggering on a court with no active camera throws
  `NoActiveCameraException`

## Errors

| Condition | Exception |
|---|---|
| Capture triggered with no active camera | `NoActiveCameraException` |
| Duplicate court number | `IllegalArgumentException` |
| Removing a camera that is not installed | `IllegalArgumentException` |
| Null or blank gym name | `IllegalArgumentException` |
| Court number not positive | `IllegalArgumentException` |

## Dependencies

Needs `Camera` (SPEC-02), `Highlight` (SPEC-03), `Sport`, `HighlightType`,
`CameraStatus`, `NoActiveCameraException`.

Write against the contracts above; do not wait for those classes to be
finished, and do not edit them.

## Out of scope

Scheduling, court availability, bookings, matches. A court is a place with
cameras, nothing more.
