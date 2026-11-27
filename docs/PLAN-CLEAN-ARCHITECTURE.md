# Plan — SOLID and Clean Architecture

The second assignment ([docs/Trabalho 2.md](Trabalho%202.md)) keeps the theme and
asks for six things on top of the first: SOLID, Clean Architecture, Clean Code, a
careful graphical interface, a database, and JavaDoc. It is graded on ten
criteria of one point each, three of them on the interface; the first
assignment's checklist is checked again as criterion 1. Where each criterion
stands, and the issues that close it, is in [Rubric coverage](#rubric-coverage).

This plan was written against an early draft of the brief that asked only for
SOLID and Clean Architecture. Levels 1 to 5 below are that part; level 6 adds
what the final brief asks for beyond it.

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

Items 1 to 10 have landed; what each one changed is recorded in
[CHANGES-CLEAN-ARCHITECTURE.md](CHANGES-CLEAN-ARCHITECTURE.md). Item 11 and
level 6 are open.

### Level 1 — the layers exist

**1. Move ports and adapters, and enforce the dependency rule** (#119, `shared`)
— done, PR #133.
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

**2. One CSV format per camera kind** (#120, `persistence`, open/closed) — done,
PR #185.

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

**3. `Gym.openNextCourt()`** (#128, `structure`) — done, PR #135. Adds the court numbered one past the
highest (1 on an empty gym) and returns it. It replaces the arithmetic in
`Launcher.givePhoneItsCourt`.

**4. Aiming is a capability** (#129, `capture`, interface segregation) — done,
PRs #138 and #181. An `Aimable`
interface in `domain/capture/` with `moveTo(pan, tilt, zoom)`, implemented by
`PtzCamera`. Callers ask for the capability (`camera instanceof Aimable`), never
for the class, and fixed or stream cameras are not made to answer an aim.

### Level 3 — the use cases (all in `application/`, tested with the in-memory repositories)

**5. `ManageGym` and the read models** (#121, `structure`, needs 1 and 4) — done,
PR #141. Every change
the court panel makes today, taken by number and id instead of by entity:
`addCourt`, `installCamera(court, CameraSpec)`, `startRecording`, `stopRecording`,
`sendToMaintenance`, `removeCamera`, `moveCamera`, `aimCamera`. Each saves the gym
after an accepted change, as `CourtPanel.onGymChanged` does now. Reads return
immutable views: `CourtView` (number, cameras, seconds recorded) and `CameraView`
(id, the `describe()` line, `aimable`). A `CameraKind` enum (`FIXED`, `PTZ`) builds
the camera from a `CameraSpec`, each constant with its own construction, which
ends the string comparison in `CourtPanel.buildCamera`.

**6. `CaptureHighlight`** (#130, `highlight`, needs 1) — done, PR #137; the
deferred write was later removed in #113. `capture(courtNumber)` triggers the
court and returns a `PendingCapture` with `writeClip()` — run by the window on a
worker, it does the deferred file write if the camera has one — and `archive()`,
run back on the Swing thread, which saves the highlight and returns its
`HighlightView`. The rule "a highlight is archived only once its file exists"
moves here from `CapturePanel`; which thread runs each step stays the window's,
as fixed in #115. `PendingClipWrites` moves from `ui/` to `application/`.

**7. `BrowseArchive`** (#131, `persistence`, needs 1) — done, PR #134. `all()` and `byCourt(number)`
return an `ArchiveListing`: the readable highlights newest first, as
`HighlightView`s, and an optional warning when the file was damaged. The window
then stops catching `CorruptedRecordException`; any other `RepositoryException`
still propagates.

**8. `GymSession` and `ConnectStreamCamera`** (#132, `structure`, needs 1 and 3)
— done, PR #142.
`GymSession` moves from `ui/` to `application/` unchanged in behaviour, taking the
fallback gym as an argument instead of calling `SampleGym`. `ConnectStreamCamera`
opens the next court, installs the phone and starts it (today's
`givePhoneItsCourt`), and stops it if still recording when the phone drops (today
in `VarZinhoWindow.phoneDropped`).

### Level 4 — the window

**9. The window reaches the model only through use cases** (#122, `console`, needs 5–8)
— done, PR #143.

- The panels take use cases and views; `ui/` imports nothing from `domain/` or
  `infrastructure/`. It may import `application/`, `enums/` and `exception/`.
- `Feed` moves to `application/`; `LiveFeed` to `infrastructure/simulation/`;
  `SegmentFeed` and `PhoneConnection` to `infrastructure/stream/`.
- `Launcher` and `SampleGym` move to `bootstrap/`: the composition root, the one
  place that names every concrete adapter. `VarZinhoWindow` becomes `public` so
  the launcher can build it.
- `ArchitectureTest` gains its `ui` row.

**10. Split `CourtPanel` by responsibility** (#123, `console`, single responsibility,
needs 9) — done, PR #177. Court selection and the new-court field; the camera list with its
state buttons; the install form; the move and aim row. `CourtPanel` only lays
them out.

### Level 5 — the delivery

**11. Document SOLID and the layers** (#124, `docs`, needs 1–10) — open. `README.md` (in
Portuguese) gets a section for the second assignment: a table from each SOLID
principle and each Clean Architecture circle to the classes that show it.
`ARCHITECTURE.md`, `AGENTS.md` (package layout), `CLAUDE.md`,
`.claude/rules/project-constraints.md` and `work-calibration.md` describe the new
packages. `docs/DOMAIN-MODEL.md` gains `Aimable` and `openNextCourt`.

### Level 6 — what the final brief adds

The final brief asks for three requirements the draft did not have, and the
interface is named as the main effort. Each is one or more issues; their order
is free unless an issue says otherwise.

**Interface** (requirement 4, criteria 6 to 8). The window already has its own
light and dark theme (#158), a sidebar by subject (#161, #187), court cards and
F2 capture (#162, #188, #191), the archive search (#163) and the arena dashboard
(#193). What is left:

- #208 — buttons, fields and headings look the same on every screen (identity).
- #207 — no screen is cut off or overlapping at any supported window size (layout).
- #206 — every screen can be used from the keyboard, with visible focus (usability).
- #205 — the window stays responsive while a repository operation runs (usability).
- #210 — the archive page is split by responsibility, as `CourtPanel` was in #123.

**Database** (requirement 5, criterion 9). Done: PostgreSQL in Docker (#145),
one adapter per port (#147, #148, #152), one contract test every adapter passes
(#144), the launcher's choice of storage (#150). The tables are created by the
adapters themselves (`CREATE TABLE IF NOT EXISTS`) and the container by
`docker-compose.yml`, which is the brief's "mecanismo automático de criação do
banco". Registration, lookup, change and removal reach the window for courts,
cameras, operators and highlights. The README gives one copy-paste block per
shell that starts the container and opens the window on PostgreSQL, and a
walk-through that shows a court, a camera, an operator and a renamed highlight
surviving a restart of both the application and the database (#212).

**JavaDoc** (requirement 6, criterion 10). Classes, interfaces, constructors and
public methods, with `@param`, `@return` and `@throws`, and the HTML generated.

- #200 — `mvn javadoc:javadoc` generates navigable HTML.
- #201 — the domain.
- #202 — use cases, read models and ports.
- #203 — infrastructure adapters.
- #204 — the window, exceptions, enums and entry points.

**Clean Code and the first assignment** (criteria 1 and 5).

- #209 — the README's first-assignment checklist names only code that exists.
- #211 — the launcher's phone address lookup returns `Optional` instead of `null`.

---

## Rubric coverage

Each criterion of the rubric in [Trabalho 2.md](Trabalho%202.md), what already
answers it, and the open issues that finish it.

| # | Criterion | Already delivered | Open |
|---|---|---|---|
| 1 | First-assignment checklist | `README.md` checklist | #209 |
| 2 | SOLID: SRP and OCP | `CameraCsvFormat` (#120), `CourtPanel` split (#123), use cases (#121, #130–#132) | #210, #124 |
| 3 | SOLID: LSP, ISP and DIP | `Aimable` (#129, #140), ports in `application/port/` (#119), composition root (#143) | #124 |
| 4 | Clean Architecture | layers and `ArchitectureTest` (#119, #143), use cases tested without Swing | #124 |
| 5 | Clean Code | exceptions chained with their cause, no empty `catch` | #211, #210 |
| 6 | Interface: visual identity | own theme in light and dark (#158), sidebar (#187) | #208 |
| 7 | Interface: layout | pages by subject (#161), court cards (#188) | #207 |
| 8 | Interface: usability | F2 capture (#162, #191), archive search (#163), capture on a worker (#113), keyboard use with visible focus (#206) | #205 |
| 9 | Database and persistence | PostgreSQL adapters (#145–#152), storage choice (#150), contract test (#144), restart walk-through in the README (#212) | — |
| 10 | JavaDoc | class Javadoc on most public types | #200–#204 |

---

## Delivery checklist

What the brief asks to be handed in, and where each item comes from.

| Item | Where | Issue |
|---|---|---|
| Complete, runnable source | `development` merged into `main` | — |
| README: members | `README.md`, "Equipe" | done |
| README: how to run | `README.md`, "Como executar" | done |
| README: database configuration | `README.md`, "Como executar" | done, with the restart walk-through (#212) |
| README: where each requirement is met | `README.md`, second-assignment section | #124 |
| Screenshots of the screens | `docs/screenshots/` | #164 |
| JavaDoc generated as HTML | `mvn javadoc:javadoc` | #200 |
| Script or automatic creation of the database | `docker-compose.yml` and the adapters' `CREATE TABLE IF NOT EXISTS` | done |
