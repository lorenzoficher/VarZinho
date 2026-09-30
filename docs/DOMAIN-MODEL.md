# Domain Model

The classes, their relationships and the reasons those relationships exist in
the delivered application.

## Class diagram

```mermaid
classDiagram
    class Gym {
        -String name
        -String address
        -List~Court~ courts
        -List~Person~ people
        +addCourt(Court)
        +installCamera(int, Camera)
        +moveCamera(Camera, int)
        +findCourt(int) Court
        +getCourts() List~Court~
        +registerPerson(Person)
        +findPerson(String) Optional~Person~
    }

    class Court {
        -int number
        -List~Camera~ cameras
        +installCamera(Camera)
        +removeCamera(Camera)
        +record(Frame)
        +triggerCapture() Highlight
        +hasCamera(String) boolean
        +hasActiveCamera() boolean
        +secondsRecorded() int
    }

    class Camera {
        <<abstract>>
        -String id
        -String model
        -Resolution resolution
        -CameraStatus status
        -CircularBuffer buffer
        +startRecording()
        +stopRecording()
        +sendToMaintenance()
        +record(Frame)
        +captureLastSeconds(int) VideoClip*
        +describe() String*
    }

    class FixedCamera {
        -int angle
        +captureLastSeconds(int) VideoClip
        +describe() String
    }

    class PtzCamera {
        -int pan
        -int tilt
        -int zoom
        +moveTo(int, int, int)
        +captureLastSeconds(int) VideoClip
        +describe() String
    }

    class CircularBuffer {
        -Frame[] frames
        -int writePosition
        -int size
        +record(Frame)
        +extractLastSeconds(int) List~Frame~
        +capacity() int
        +size() int
    }

    class Frame {
        -Instant timestamp
        -int sequence
    }

    class Highlight {
        -String id
        -Instant capturedAt
        -int courtNumber
        -String cameraId
        -VideoClip clip
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
        <<abstract>>
        -String name
        -String document
        -LocalDate birthDate
        +age() int
        +identify() String*
    }

    class Athlete
    class Operator

    class HighlightRepository {
        <<interface>>
        +save(Highlight)
        +findById(String) Optional~Highlight~
        +findAll() List~Highlight~
        +findByCourt(int) List~Highlight~
    }

    class GymRepository {
        <<interface>>
        +save(Gym)
        +load() Optional~Gym~
    }

    class CsvHighlightRepository
    class InMemoryHighlightRepository
    class CsvGymRepository
    class InMemoryGymRepository

    Gym "1" *-- "0..*" Court
    Gym "1" o-- "0..*" Person
    Court "1" o-- "0..*" Camera
    Camera "1" *-- "1" CircularBuffer
    CircularBuffer "1" *-- "0..*" Frame
    Camera <|-- FixedCamera
    Camera <|-- PtzCamera
    Highlight "1" *-- "1" VideoClip
    Person <|-- Athlete
    Person <|-- Operator
    HighlightRepository <|.. CsvHighlightRepository
    HighlightRepository <|.. InMemoryHighlightRepository
    GymRepository <|.. CsvGymRepository
    GymRepository <|.. InMemoryGymRepository
    HighlightRepository ..> Highlight
    GymRepository ..> Gym
```

## Relationships

| Relationship | Cardinality | Type | Reason |
|---|---:|---|---|
| Gym → Court | 1 : 0..* | Composition | Courts belong to one gym; a new gym may start empty. |
| Gym → Person | 1 : 0..* | Aggregation | Athletes and operators exist independently of registration. |
| Court → Camera | 1 : 0..* | Aggregation | Cameras can be removed and moved between courts. |
| Camera → CircularBuffer | 1 : 1 | Composition | The buffer is private memory owned by one camera. |
| CircularBuffer → Frame | 1 : 0..* | Composition | Frames exist only as entries in a buffer. |
| Highlight → VideoClip | 1 : 1 | Composition | A highlight cannot exist without its clip metadata. |

Camera identifiers are unique across the whole gym. Installation therefore
goes through `Gym.installCamera(...)`; `Gym.moveCamera(...)` validates the
destination before removing the camera from its current court. A moved camera
arrives inactive and starts with an empty buffer when recording resumes.

A highlight stores the court number and camera identifier as values, rather
than keeping mutable references. It can therefore outlive both objects and be
rebuilt from CSV without reconstructing the capture equipment.

## Object-oriented concepts

- **Encapsulation:** every field is private. Collections are returned as
  unmodifiable copies, and `CircularBuffer` never exposes its frame array.
- **Inheritance:** `FixedCamera` and `PtzCamera` extend `Camera`;
  `Athlete` and `Operator` extend `Person`.
- **Polymorphism:** cameras implement `captureLastSeconds()` and
  `describe()` differently; people implement `identify()` differently.
- **Abstraction:** `HighlightRepository` and `GymRepository` are contracts
  with CSV and in-memory implementations.
- **Domain independence:** no class under `domain/` imports `repository/`
  or Swing.

## Persistence boundaries

`HighlightRepository` stores the permanent highlight archive.
`GymRepository` stores the gym, courts, cameras, their status and PTZ
position. `CsvHighlightRepository` can recover valid highlights around a
damaged line; `CsvGymRepository` rejects a damaged structure as a whole so
the application never runs with half a gym.

## Deliberate omissions

There is no play type, sport, match or highlight author. The physical capture
button carries no identity, so the saved highlight contains only the instant,
court, camera and clip. Real video processing, authentication, scheduling and
a database are outside the project scope.
