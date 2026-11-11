# Changes — SOLID and Clean Architecture

What the second assignment has changed in the code so far, issue by issue. The
plan and its reasoning are in [PLAN-CLEAN-ARCHITECTURE.md](PLAN-CLEAN-ARCHITECTURE.md);
this document records what actually landed, what each change fixed, and what is
still open.

State as of 2026-11-11, on `development` at `7706b5f`.

---

## At a glance

| # | Work item | Issue | Pull request | Author | Status | Principle |
|---|---|---|---|---|---|---|
| 0 | Record the plan | #118 | #125 | lorenzoficher | merged 2026-11-08 | — |
| 1 | Ports in `application/`, adapters in `infrastructure/`, `ArchitectureTest` | #119 | #133 | lararios27 | merged 2026-11-08 | DIP, dependency rule |
| 2 | One CSV format per camera kind | #120 | — | — | **open** | OCP |
| 3 | `Gym.openNextCourt()` | #128 | #135 | lararios27 | merged 2026-11-10 | SRP (rule back in the domain) |
| 4 | `Aimable` capability | #129 | #138 | ArturKraemer | merged 2026-11-10 | ISP, LSP |
| 5 | `ManageGym` and the read models | #121 | #141 | Inaurrara | merged 2026-11-10 | Use case layer, SRP |
| 6 | `CaptureHighlight` | #130 | #137 | rjnlopes03 | merged 2026-11-09 | Use case layer, SRP |
| 7 | `BrowseArchive` | #131 | #134 | Inaurrara | merged 2026-11-09 | Use case layer, DIP |
| 8 | `GymSession` and `ConnectStreamCamera` | #132 | #142 | rjnlopes03 | merged 2026-11-11 | Use case layer, SRP |
| 9 | The window reaches the model only through use cases | #122 | #143 | rjnlopes03 | **in review** | Dependency rule, DIP |
| 10 | Split `CourtPanel` by responsibility | #123 | — | — | **open** | SRP |
| 11 | Document SOLID and the layers | #124 | — | — | **open** | — |

Follow-ups opened from the review of #138: #139 (the court panel checks the
capability where it aims) and #140 (the `Aimable` Javadoc names interface
segregation and does not tie aiming to recording). Both are open.

Pull requests #126 and #127 were a first draft of items 1 and 2 and were closed
unmerged on purpose: the code is written by the team, one issue each.

**Tests:** 289 test runs when item 1 merged, 321 after item 8 — more runs than
test methods, because the repository contract tests run once per adapter.
Every pull request kept the whole suite green, with only the FFmpeg tests
skipped where FFmpeg is absent.

---

## The gaps, and where each one stands

The plan opened with seven gaps. This is each one today.

| Gap | Principle | Status |
|---|---|---|
| The window orchestrates capture, clip write and archive itself (`CapturePanel`) | Clean Architecture | Use case exists (`CaptureHighlight`); the window adopts it in #143 |
| Application services live in `ui/` (`GymSession`, `givePhoneItsCourt`, `PendingClipWrites`) | Clean Architecture, SRP | **Fixed** — all three are in `application/` |
| The window holds entities and changes them directly (`CourtPanel`) | Clean Architecture | Use case exists (`ManageGym`); the window adopts it in #143 |
| Ports and adapters share one package; the window catches an adapter's exception | DIP | Packages **fixed**; the window stops catching in #143 |
| A new camera kind means editing `CsvGymRepository` (`instanceof`) | OCP | **Open** — #120 |
| Aiming casts to `PtzCamera`; the camera kind is chosen by comparing strings | OCP, LSP, ISP | Cast **fixed** (`Aimable`); string comparison ends in #143 via `CameraKind` |
| One panel does five jobs (`CourtPanel`) | SRP | **Open** — #123 |

---

## What each merged change did

### 1. Ports and adapters move to their layers — #119, PR #133

**Before.** `repository/` held both the contracts (`HighlightRepository`,
`GymRepository`) and their CSV and in-memory implementations; `stream/` sat at
the root. The only architectural check was `ExceptionPackageDependencyTest`.

**After.**

- `application/port/`: `HighlightRepository`, `GymRepository`,
  `RepositoryException`, `CorruptedRecordException`, `StorableHighlights` (now
  `public`, since two adapters in another package call it).
- `infrastructure/persistence/`: `CsvGymRepository`, `CsvHighlightRepository`,
  `InMemoryGymRepository`, `InMemoryHighlightRepository`.
- `infrastructure/stream/`: `FfmpegClipAssembler`, `FfmpegRecorder`,
  `SegmentIndex`, `SegmentLog`.
- `ArchitectureTest` reads every `import` under `src/main/java` and fails when a
  layer reaches outward:

| Package | May not import |
|---|---|
| `domain/` | `application`, `infrastructure`, `ui`, `bootstrap`, Swing/AWT |
| `application/` | `infrastructure`, `ui`, `bootstrap`, Swing/AWT |
| `infrastructure/` | `ui`, `bootstrap` |
| `exception/` | any project package |

**Why it matters.** The dependency rule stops being a convention a reviewer has
to remember: breaking it breaks the build. The interfaces belong to whoever uses
them (the application), which is dependency inversion in its literal form.

No behaviour changed — `git mv` plus imports.

### 3. A gym opens its next court — #128, PR #135

**Before.** `Launcher.givePhoneItsCourt` computed "highest court number plus
one" itself, a gym rule living in the window's package.

**After.** `Gym.openNextCourt()` creates, registers and returns the court one
past the highest (court 1 on an empty gym). The rule is back in the entity that
owns the courts.

### 4. Aiming is a capability — #129, PR #138

**Before.** The court panel cast the selected camera to `PtzCamera` to aim it.
Any caller wanting to aim had to know the concrete class.

**After.** `domain/capture/Aimable` declares `moveTo(pan, tilt, zoom)` and only
`PtzCamera` implements it. Callers ask for the capability, not the class.
`AimableTest` pins that a `PtzCamera` is aimable and a `FixedCamera` and a
`StreamCamera` are not.

**Why it matters.** Interface segregation: `Camera` is not given a `moveTo` that
two of its three subclasses would have to refuse — which would also have broken
Liskov substitution.

### 5. The gym's structure changes through `ManageGym` — #121, PR #141

**Before.** `CourtPanel` held `Court` and `Camera` entities and called
`startRecording()`, `installCamera(...)` and the rest on them directly, then
saved the gym itself.

**After.** `application/ManageGym` takes court numbers and camera ids:
`addCourt`, `installCamera(court, CameraSpec)`, `startRecording`,
`stopRecording`, `sendToMaintenance`, `removeCamera`, `moveCamera`,
`aimCamera`. It saves the gym after each accepted change and never after a
refused one. Reads return immutable views — `CourtView`, `CameraView` (id, the
`describe()` line, `aimable`). `CameraKind` (`FIXED`, `PTZ`) builds each camera
from a `CameraSpec`, replacing the string comparison in `CourtPanel.buildCamera`.

**Why it matters.** The window will see read models, never entities, which is
what makes "`ui/` imports nothing from `domain/`" enforceable. The rules stay in
`Court`, `Camera` and `Gym`; the use case only sequences them with the port.

### 6. A capture is archived only after its file exists — #130, PR #137

**Before.** `CapturePanel` triggered the court, ran the deferred clip write and
saved the highlight — the business order lived in a Swing class.

**After.** `CaptureHighlight.capture(courtNumber)` returns a `PendingCapture`:
`writeClip()` does the deferred write (the window runs it on a worker) and
`archive()` saves the highlight and returns its `HighlightView`, refusing with
`IllegalStateException` while the file does not exist. `PendingClipWrites`
moved from `ui/` to `application/`.

**Why it matters.** The rule "never archive a clip that is not on disk" is now
the use case's and is tested without Swing; which thread runs each step stays
the window's decision, as fixed in #115.

### 7. The archive is listed through `BrowseArchive` — #131, PR #134

**Before.** `ArchivePanel` called the repository and caught
`CorruptedRecordException`, an exception from the persistence port.

**After.** `BrowseArchive.all()` and `byCourt(number)` return an
`ArchiveListing`: the readable highlights newest first, as `HighlightView`s, and
an optional warning naming the damaged line. Any other `RepositoryException`
still propagates.

**Why it matters.** A damaged archive becomes a result the window can show, not
an exception it must know about.

### 8. The session and the phone's court are use cases — #132, PR #142

**Before.** `GymSession` lived in `ui/` and called `SampleGym` itself;
`Launcher.givePhoneItsCourt` installed the phone, and
`VarZinhoWindow.phoneDropped` stopped it.

**After.** `GymSession` is in `application/` and receives the fallback gym as an
argument. `ConnectStreamCamera` opens the next court (through item 3), installs
the phone, starts it, and stops it if it is still recording when the phone
drops. After a damaged gym file the session keeps the gym in a private
repository that stores nothing — the application may not import the in-memory
adapter — so the damaged file is still never overwritten.

---

## What is still open

| Issue | What is left |
|---|---|
| #122 (PR #143, in review) | Panels use `ManageGym`, `CaptureHighlight` and `BrowseArchive`; `Launcher` and `SampleGym` move to `bootstrap/`; `LiveFeed` to `infrastructure/simulation/`; `SegmentFeed` and `PhoneConnection` to `infrastructure/stream/`; `ArchitectureTest` gains the `ui` row. Until it merges, `ui/` still holds those classes and still touches entities. |
| #120 | `CsvGymRepository` still branches on `instanceof PtzCamera` / `FixedCamera`. |
| #123 | `CourtPanel` still selects courts, lists cameras, installs, moves and aims. Needs #122. |
| #124 | `README.md`, `ARCHITECTURE.md`, `AGENTS.md`, `CLAUDE.md` and the rules still describe the code before this work. Needs 1–10. |
| #139, #140 | Follow-ups to `Aimable`, from the review of #138. |
| #118 | The plan merged in #125 but the issue was not closed. |
