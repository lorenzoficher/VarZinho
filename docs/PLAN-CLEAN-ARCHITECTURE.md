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
   is one new format class and one line in `CameraCsvFormats.standard()`.
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

## Work items

One issue and one pull request each, into `development`, every test passing at
every step. Each item belongs to one aggregate, as
`.claude/rules/work-calibration.md` asks; the order below is the order of the
dependencies, and items at the same level can run in parallel.

Until item 1 lands, this plan describes a target, not the code:
`ARCHITECTURE.md`, `AGENTS.md` and `CLAUDE.md` keep describing the code as it
is, and are brought up to date in item 11.

### Level 1 — the layers exist

**1. Move ports and adapters, and enforce the dependency rule** (#119, `shared`).
A `git mv` plus imports; no behaviour changes.

- `repository/HighlightRepository`, `GymRepository`, `CorruptedRecordException`,
  `StorableHighlights` and `exception/RepositoryException` move to
  `application/port/`. `StorableHighlights` and `requireStorable` become `public`,
  since both persistence adapters call it from another package.
- `repository/Csv*` and `InMemory*` move to `infrastructure/persistence/`;
  `stream/*` to `infrastructure/stream/`. Their tests move with them.
- `ArchitectureTest` (in the root test package) replaces
  `ExceptionPackageDependencyTest`: it reads the `import` lines under
  `src/main/java` and fails when a layer imports one the table above forbids.
  It starts with `domain`, `application`, `infrastructure` and `exception`; the
  `ui` row is added by item 9.
- Check: add an `application.port` import to `Gym` and the test must fail.

Everyone else rebases on this one, so it goes first and alone.

### Level 2 — independent of each other

**2. One CSV format per camera kind** (#120, `persistence`, open/closed).

```java
public interface CameraCsvFormat<C extends Camera> {
    String kind();                 // "fixed", "ptz" — the first column
    Class<C> cameraClass();        // looked up by camera.getClass() on save
    int ownColumns();              // columns after the 7 every camera shares
    List<String> ownFields(C camera);
    C build(String id, String model, Resolution resolution, int bufferSeconds,
            List<String> ownFields);
}
```

`FixedCameraCsvFormat` (angle) and `PtzCameraCsvFormat` (pan, tilt, zoom, applied
through `moveTo`); `CameraCsvFormats.standard()` lists them. `CsvGymRepository`
keeps `CsvGymRepository(Path)` (using the standard list) and gains
`CsvGymRepository(Path, List<CameraCsvFormat<?>>)`, which refuses two formats for
one kind or class and a kind named `gym` or `court`. The file format does not
change. Test it with a test-only camera subclass and its format: it must round-trip
without the repository being edited.

**3. `Gym.openNextCourt()`** (#128, `structure`). Adds the court numbered one past the
highest (1 on an empty gym) and returns it. It replaces the arithmetic in
`Launcher.givePhoneItsCourt`.

**4. Aiming is a capability** (#129, `capture`, interface segregation). An `Aimable`
interface in `domain/capture/` with `moveTo(pan, tilt, zoom)`, implemented by
`PtzCamera`. Callers ask for the capability (`camera instanceof Aimable`), never
for the class, and fixed or stream cameras are not made to answer an aim.

### Level 3 — the use cases (all in `application/`, tested with the in-memory repositories)

**5. `ManageGym` and the read models** (#121, `structure`, needs 1 and 4). Every change
the court panel makes today, taken by number and id instead of by entity:
`addCourt`, `installCamera(court, CameraSpec)`, `startRecording`, `stopRecording`,
`sendToMaintenance`, `removeCamera`, `moveCamera`, `aimCamera`. Each saves the gym
after an accepted change, as `CourtPanel.onGymChanged` does now. Reads return
immutable views: `CourtView` (number, cameras, seconds recorded) and `CameraView`
(id, the `describe()` line, `aimable`). A `CameraKind` enum (`FIXED`, `PTZ`) builds
the camera from a `CameraSpec`, each constant with its own construction, which
ends the string comparison in `CourtPanel.buildCamera`.

**6. `CaptureHighlight`** (#130, `highlight`, needs 1). `capture(courtNumber)` triggers the
court and returns a `PendingCapture` with `writeClip()` — run by the window on a
worker, it does the deferred file write if the camera has one — and `archive()`,
run back on the Swing thread, which saves the highlight and returns its
`HighlightView`. The rule "a highlight is archived only once its file exists"
moves here from `CapturePanel`; which thread runs each step stays the window's,
as fixed in #115. `PendingClipWrites` moves from `ui/` to `application/`.

**7. `BrowseArchive`** (#131, `persistence`, needs 1). `all()` and `byCourt(number)`
return an `ArchiveListing`: the readable highlights newest first, as
`HighlightView`s, and an optional warning when the file was damaged. The window
then stops catching `CorruptedRecordException`; any other `RepositoryException`
still propagates.

**8. `GymSession` and `ConnectStreamCamera`** (#132, `structure`, needs 1 and 3).
`GymSession` moves from `ui/` to `application/` unchanged in behaviour, taking the
fallback gym as an argument instead of calling `SampleGym`. `ConnectStreamCamera`
opens the next court, installs the phone and starts it (today's
`givePhoneItsCourt`), and stops it if still recording when the phone drops (today
in `VarZinhoWindow.phoneDropped`).

### Level 4 — the window

**9. The window reaches the model only through use cases** (#122, `console`, needs 5–8).

- The panels take use cases and views; `ui/` imports nothing from `domain/` or
  `infrastructure/`. It may import `application/`, `enums/` and `exception/`.
- `Feed` moves to `application/`; `LiveFeed` to `infrastructure/simulation/`;
  `SegmentFeed` and `PhoneConnection` to `infrastructure/stream/`.
- `Launcher` and `SampleGym` move to `bootstrap/`: the composition root, the one
  place that names every concrete adapter. `VarZinhoWindow` becomes `public` so
  the launcher can build it.
- `ArchitectureTest` gains its `ui` row.

**10. Split `CourtPanel` by responsibility** (#123, `console`, single responsibility,
needs 9). Court selection and the new-court field; the camera list with its
state buttons; the install form; the move and aim row. `CourtPanel` only lays
them out.

### Level 5 — the delivery

**11. Document SOLID and the layers** (#124, `docs`, needs 1–10). `README.md` (in
Portuguese) gets a section for the second assignment: a table from each SOLID
principle and each Clean Architecture circle to the classes that show it.
`ARCHITECTURE.md`, `AGENTS.md` (package layout), `CLAUDE.md`,
`.claude/rules/project-constraints.md` and `work-calibration.md` describe the new
packages. `docs/DOMAIN-MODEL.md` gains `Aimable` and `openNextCourt`.
