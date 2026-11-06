# Plan — SOLID and Clean Architecture

The second assignment ([docs/Trabalho 2.md](Trabalho%202.md)) keeps the theme and
asks for two things on top of the first: the SOLID principles, and Clean
Architecture with Clean Code. The first assignment's checklist is complete in
`README.md`, so nothing is pending from it.

The model already does most of the work: entities are immutable or change only
through validated behaviour, and the domain imports neither storage, FFmpeg nor
Swing. What is missing is the layer between the window and the model, and the
package split that makes the dependency rule visible.

---

## What was wrong, and where

| Gap | Where | Principle |
|---|---|---|
| The window orchestrates the model and the archive itself — capture, clip write, archive | `ui/CapturePanel` | Clean Architecture: no use case layer |
| Application services and business rules live in the window's package | `ui/GymSession`, `ui/Launcher.givePhoneItsCourt`, `ui/PendingClipWrites` | Clean Architecture, SRP |
| The window holds entities and changes them directly — `camera.startRecording()` | `ui/CourtPanel` | Clean Architecture: the boundary is a convention, not a rule |
| Ports and their adapters share one package; the window catches an adapter's exception | `repository/`, `ui/ArchivePanel` | DIP |
| A new kind of camera means editing the repository | `CsvGymRepository` (`instanceof`) | OCP |
| Aiming casts to `PtzCamera`; the camera kind is chosen by comparing strings | `ui/CourtPanel` | OCP, LSP, ISP |
| One panel selects courts, lists cameras, installs, moves and aims | `ui/CourtPanel` | SRP |

---

## Target structure

```
br.edu.unipampa.varzinho
├── domain/                     entities — unchanged in spirit; knows nothing outside itself
├── enums/, exception/          shared by every layer; exception/ holds domain exceptions only
├── application/                use cases: CaptureHighlight, ManageGym, BrowseArchive,
│   │                           GymSession, ConnectStreamCamera; read models (views)
│   └── port/                   HighlightRepository, GymRepository, RepositoryException,
│                               CorruptedRecordException
├── infrastructure/
│   ├── persistence/            Csv* and InMemory* repositories, one CSV format per camera kind
│   ├── stream/                 FFmpeg adapter, PhoneConnection, SegmentFeed
│   └── simulation/             LiveFeed — the simulated cameras' clock
├── ui/                         Swing only: talks to use cases, imports nothing from domain/
├── bootstrap/                  Launcher (composition root), SampleGym
└── Main, ConsoleDemo
```

**The dependency rule, enforced by a test:**

| Package | May not import |
|---|---|
| `domain/` | `application`, `infrastructure`, `ui`, `bootstrap`, Swing/AWT |
| `application/` | `infrastructure`, `ui`, `bootstrap`, Swing/AWT |
| `infrastructure/` | `ui`, `bootstrap` |
| `ui/` | `domain`, `infrastructure` |
| `exception/` | `domain` |

---

## Decisions

1. **Thin use cases, rich domain.** The rules stay in `Court`, `Camera` and `Gym`;
   a use case only sequences them with the ports. This reverses "no service
   layer" in `ARCHITECTURE.md` — see `CONTEXT.md`.
2. **The window sees views, never entities.** Use cases take numbers and ids and
   return immutable read models (`CourtView`, `CameraView`, `HighlightView`). This
   is what makes "`ui/` imports nothing from `domain/`" checkable.
3. **Ports belong to the layer that uses them.** The repository interfaces and
   their exceptions move to `application/port/`. `ClipAssembler` stays in the
   domain, because `StreamCamera`, an entity, depends on it.
4. **One CSV format per camera kind.** `CsvGymRepository` looks a camera's format
   up by its class when writing and by its `kind` column when reading; a new kind
   is one new format class and one registration in the composition root.
5. **Aiming is a capability.** `PtzCamera` implements an `Aimable` interface in
   the domain, so the use case asks for the capability instead of the class, and
   no other camera is forced to answer an aim it cannot take (ISP).
6. **A capture is two steps.** `CaptureHighlight.capture` returns a
   `PendingCapture`: `writeClip()` runs on a worker, `archive()` back on the Swing
   thread. The order — the file exists before the highlight is archived — is the
   use case's; which thread runs each step stays the window's, as fixed in #115.
7. **A damaged archive is a result, not an exception, for the window.**
   `BrowseArchive` returns an `ArchiveListing`: the highlights it could read and
   an optional warning.
8. **The phone's court comes from the domain.** `Gym.openNextCourt()` adds the
   court after the highest-numbered one; `ConnectStreamCamera` installs the phone
   there and starts it.
9. **The launcher is the composition root,** outside `ui/`, the one class that
   knows every concrete adapter.

---

## Steps

One issue and one pull request each, into `development`, every test passing at
every step.

1. Record this plan and the reversal in `CONTEXT.md`.
2. Move ports to `application/port/` and adapters to `infrastructure/`; add the
   architecture test.
3. One CSV format per camera kind (OCP).
4. Add the use cases and read models, tested against the in-memory repositories.
5. The window talks to the use cases only; the launcher becomes the composition
   root.
6. Split `CourtPanel` by responsibility.
7. Document SOLID and the layers: `README.md` (in Portuguese), `ARCHITECTURE.md`,
   `AGENTS.md`, `CLAUDE.md`, the project rules.
