# SPEC-01 — Structure

**Aggregate:** `Gym`, `Court` · **Milestone:** Phase 1 — Domain

The physical layout of the venue: a gym containing courts and holding the
register of the people around it, each court holding the cameras installed on
it and exposing the capture trigger.

## Contract

```java
public class Gym {
    public Gym(String name, String address);
    public void addCourt(Court court);
    public Court findCourt(int number);
    public List<Court> getCourts();             // unmodifiable
    public void registerPerson(Person person);
    public Optional<Person> findPerson(String document);
    public List<Person> getPeople();            // unmodifiable
}

public class Court {
    public Court(int number);
    public void installCamera(Camera camera);
    public void removeCamera(Camera camera);
    public boolean hasActiveCamera();
    public Highlight triggerCapture();
    public List<Camera> getCameras();        // unmodifiable
}
```

A court has no sport. The system serves whatever the gym plays — see
[CONTEXT.md](../../CONTEXT.md), *The system does not classify the play*.

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
- **AC-4.3** The highlight records nothing about who played or who pressed
- **AC-4.4** Triggering on a court with no active camera throws
  `NoActiveCameraException`

The trigger takes no argument. Everything the highlight knows comes from the
court and the camera.

### B-5 — A gym registers the people around it

Athletes who play there and operators who work there. Registration is a
membership list and says nothing about any highlight.

- **AC-5.1** A newly created gym has nobody registered
- **AC-5.2** A registered person is retrievable by document
- **AC-5.3** Looking up a document that is nobody's returns empty, never null
- **AC-5.4** Registering a document that is already registered is rejected
- **AC-5.5** An athlete and an operator are registered through the same call
  and come back as `Person`
- **AC-5.6** The returned people list cannot be modified from outside

AC-5.5 is the polymorphism here: `Gym` stores `Person` and never asks which
kind it is holding.

This is aggregation, like the cameras and unlike the courts: a person exists
before being registered and after being removed.

## Errors

| Condition | Exception |
|---|---|
| Capture triggered with no active camera | `NoActiveCameraException` |
| Duplicate court number | `IllegalArgumentException` |
| Duplicate person document | `IllegalArgumentException` |
| Null person passed to `registerPerson` | `IllegalArgumentException` |
| Removing a camera that is not installed | `IllegalArgumentException` |
| Null or blank gym name | `IllegalArgumentException` |
| Court number not positive | `IllegalArgumentException` |

## Dependencies

Needs `Camera` (SPEC-02), `Highlight` (SPEC-03), `Person` (SPEC-04),
`CameraStatus`, `NoActiveCameraException`.

Write against the contracts above; do not wait for those classes to be
finished, and do not edit them.

## Out of scope

Scheduling, court availability, bookings, matches. A court is a place with
cameras, nothing more.

Memberships, fees, attendance and anything else about the registered people.
The register is a list of who is known to the gym, not a management module.
