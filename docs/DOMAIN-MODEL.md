# Domain Model

The classes, their relationships, and the reasoning behind each one. This is
the English, corrected successor of the original `MODELO.md` draft.

## Class diagram

```mermaid
classDiagram
    class Gym {
        -String name
        -String address
        -List~Court~ courts
        +addCourt(Court)
        +findCourt(int) Court
        +listHighlights() List~Highlight~
    }

    class Court {
        -int number
        -Sport sport
        -List~Camera~ cameras
        +installCamera(Camera)
        +removeCamera(Camera)
        +triggerCapture(HighlightType) Highlight
        +hasActiveCamera() boolean
    }

    class Camera {
        -String id
        -String model
        -Resolution resolution
        -CameraStatus status
        -CircularBuffer buffer
        +startRecording()
        +stopRecording()
        +isRecording() boolean
        +captureLastSeconds(int) VideoClip
    }

    class FixedCamera {
        -int angle
        +captureLastSeconds(int) VideoClip
    }

    class PtzCamera {
        -int pan
        -int tilt
        -int zoom
        +moveTo(int, int, int)
        +captureLastSeconds(int) VideoClip
    }

    class CircularBuffer {
        -int capacitySeconds
        -Frame[] frames
        -int writePosition
        +record(Frame)
        +extractLastSeconds(int) List~Frame~
        +isFull() boolean
        +isEmpty() boolean
    }

    class Frame {
        -Instant timestamp
        -int sequence
    }

    class Highlight {
        -String id
        -Instant capturedAt
        -HighlightType type
        -int durationSeconds
        -Athlete author
        -VideoClip clip
        -Camera origin
        +assignAuthor(Athlete)
        +hasAuthor() boolean
        +describe() String
    }

    class VideoClip {
        -String filePath
        -int durationSeconds
        -Resolution resolution
        -double sizeMb
        +exists() boolean
    }

    class Person {
        -String name
        -String document
        -LocalDate birthDate
        +age() int
        +identify() String
    }

    class Athlete {
        -int shirtNumber
        -String position
        +identify() String
    }

    class Operator {
        -String badge
        -String shift
        +identify() String
    }

    class Triggerable {
        +trigger() void
    }

    class PhysicalButton {
        -int pin
        +trigger()
    }

    class HighlightRepository {
        +save(Highlight) void
        +findById(String) Optional~Highlight~
        +findAll() List~Highlight~
        +findByCourt(int) List~Highlight~
    }

    class CsvHighlightRepository {
        -Path file
        +save(Highlight) void
        +findById(String) Optional~Highlight~
        +findAll() List~Highlight~
        +findByCourt(int) List~Highlight~
    }

    Gym "1" *-- "1..*" Court
    Court "1" o-- "0..*" Camera
    Camera "1" *-- "1" CircularBuffer
    CircularBuffer "1" *-- "0..*" Frame
    Camera <|-- FixedCamera
    Camera <|-- PtzCamera
    Camera "1" --> "0..*" Highlight
    Highlight "1" *-- "1" VideoClip
    Highlight "0..*" --> "0..1" Athlete
    Person <|-- Athlete
    Person <|-- Operator
    Triggerable <|.. PhysicalButton
    Court "1" --> "1..*" Triggerable
    HighlightRepository <|.. CsvHighlightRepository
    HighlightRepository ..> Highlight
```

`Camera` and `Person` are **abstract**. `Triggerable` and `HighlightRepository`
are **interfaces**.

## Relationships

| Relationship | Cardinality | Type | Why |
|---|---|---|---|
| Gym → Court | 1 : 1..* | Composition ◆ | A court cannot exist outside its gym |
| Court → Camera | 1 : 0..* | **Aggregation ◇** | Cameras are movable equipment |
| Camera → CircularBuffer | 1 : 1 | Composition ◆ | Internal memory of the device |
| CircularBuffer → Frame | 1 : 0..* | Composition ◆ | Frames exist only inside the buffer |
| Camera → Highlight | 1 : 0..* | Association → | Highlights outlive the camera |
| Highlight → VideoClip | 1 : 1 | Composition ◆ | Without a clip there is no highlight |
| Highlight → Athlete | 0..* : **0..1** | Association → | Author is optional and assigned later |

### Composition or aggregation?

The test: *if the whole is removed, does the part still make sense alone?*

**Gym and court** — demolish the building and the court is gone. Composition.

**Court and camera** — uninstall the camera and it goes to the storeroom, gets
repaired, gets mounted on another court. It survives. **Aggregation**, drawn
with a hollow diamond.

Having both in the same model is deliberate: it shows the distinction is
understood rather than applied by habit.

### Why `0..*` cameras on a court

A court that was just built has no cameras yet. `1..*` would make that state
unrepresentable and force the code to lie.

### Why `0..1` author

At the moment of capture the system does not know who made the play. There is
no login and no identification — a button was pressed, nothing more. Requiring
an author would mean inventing data.

An author is assigned later by a human who watched the clip. This is the only
mutable field on `Highlight`, and it changes through `assignAuthor()`, not a
setter. Assigning twice is an error.

## Enumerations

| Enum | Values |
|---|---|
| `HighlightType` | `GOAL`, `SAVE`, `DUNK`, `SPIKE`, `BLOCK`, `FOUL`, `OTHER` |
| `Sport` | `FUTSAL`, `BASKETBALL`, `VOLLEYBALL`, `HANDBALL` |
| `CameraStatus` | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |
| `Resolution` | `HD`, `FULL_HD`, `ULTRA_HD` |

## Where the syllabus topics appear

**Encapsulation** — every field is private. The strongest case is
`CircularBuffer`: nothing outside it touches the frame array, because the
overwrite policy must hold. Exposing the array would let a caller break the
invariant.

**Inheritance** — `Person` → `Athlete`, `Operator`. Shared state (name,
document, birth date) and shared behaviour (`age()`) live in the superclass.

**Polymorphism** — `Camera` is abstract and declares `captureLastSeconds()`.
`FixedCamera` and `PtzCamera` implement it differently; a PTZ camera must
settle its position first. `Court` triggers every camera through the same call
with no type checks. Adding a `Camera360` later changes nothing in `Court`.

**Abstraction** — two interfaces, each with a real reason to exist.
`HighlightRepository` inverts the dependency on storage. `Triggerable`
abstracts the capture trigger, which real products implement as both a button
and a remote control.

**Associations** — all four kinds appear: composition, aggregation, plain
association and inheritance. See the table above.

**Exception handling** — see [ARCHITECTURE.md](ARCHITECTURE.md).

## Implementation order

Start with what depends on nothing:

1. Enums
2. `Frame`, `VideoClip`
3. `Person` → `Athlete`, `Operator`
4. `CircularBuffer`
5. `Camera` → `FixedCamera`, `PtzCamera`
6. `Highlight`
7. `HighlightRepository` → `CsvHighlightRepository`
8. `Court`, `Gym`
9. `Main` console demo
