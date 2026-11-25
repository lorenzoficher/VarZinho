# Changes — SOLID and Clean Architecture

What the second assignment has changed in the code so far, issue by issue. The
plan and its reasoning are in [PLAN-CLEAN-ARCHITECTURE.md](PLAN-CLEAN-ARCHITECTURE.md);
this document records what actually landed, what each change fixed, and what is
still open.

State as of 2026-11-24, on `development` at `bf28bed`.

---

## At a glance

| # | Work item | Issue | Pull request | Author | Status | Principle |
|---|---|---|---|---|---|---|
| 0 | Record the plan | #118 | #125 | lorenzoficher | merged 2026-11-08 | — |
| 1 | Ports in `application/`, adapters in `infrastructure/`, `ArchitectureTest` | #119 | #133 | lararios27 | merged 2026-11-08 | DIP, dependency rule |
| 2 | One CSV format per camera kind | #120 | #185 | lararios27 | merged 2026-11-19 | OCP |
| 3 | `Gym.openNextCourt()` | #128 | #135 | lararios27 | merged 2026-11-10 | SRP (rule back in the domain) |
| 4 | `Aimable` capability | #129 | #138 | ArturKraemer | merged 2026-11-10 | ISP, LSP |
| 5 | `ManageGym` and the read models | #121 | #141 | Inaurrara | merged 2026-11-10 | Use case layer, SRP |
| 6 | `CaptureHighlight` | #130 | #137 | rjnlopes03 | merged 2026-11-09 | Use case layer, SRP |
| 7 | `BrowseArchive` | #131 | #134 | Inaurrara | merged 2026-11-09 | Use case layer, DIP |
| 8 | `GymSession` and `ConnectStreamCamera` | #132 | #142 | rjnlopes03 | merged 2026-11-11 | Use case layer, SRP |
| 9 | The window reaches the model only through use cases | #122 | #143 | rjnlopes03 | merged 2026-11-13 | Dependency rule, DIP |
| 10 | Split `CourtPanel` by responsibility | #123 | #177 | Inaurrara | merged 2026-11-17 | SRP |
| 11 | Document SOLID and the layers | #124 | — | — | **open** | — |

Follow-ups opened from the review of #138, both merged: #139 (the court panel
checks the capability where it aims, PR #175, 2026-11-15) and #140 (the
`Aimable` Javadoc names interface segregation and does not tie aiming to
recording, PR #181, 2026-11-18).

Pull requests #126 and #127 were a first draft of items 1 and 2 and were closed
unmerged on purpose: the code is written by the team, one issue each.

**Tests:** 289 test runs when item 1 merged, 321 after item 8, 549 on
2026-11-24 — more runs than test methods, because the repository contract tests
run once per adapter. Every pull request kept the whole suite green; the FFmpeg
tests are skipped where FFmpeg is absent, and the PostgreSQL tests where Docker
is.

---

## The gaps, and where each one stands

The plan opened with seven gaps. All seven are closed.

| Gap | Principle | Status |
|---|---|---|
| The window orchestrates capture, clip write and archive itself (`CapturePanel`) | Clean Architecture | **Fixed** — the panel presses `CaptureHighlight` (#143) |
| Application services live in `ui/` (`GymSession`, `givePhoneItsCourt`, `PendingClipWrites`) | Clean Architecture, SRP | **Fixed** — moved to `application/` (#130, #132); `PendingClipWrites` later removed (#113) |
| The window holds entities and changes them directly (`CourtPanel`) | Clean Architecture | **Fixed** — `ui/` sees only views, and `ArchitectureTest` forbids `domain/` imports (#143) |
| Ports and adapters share one package; the window catches an adapter's exception | DIP | **Fixed** — packages (#119), the window stops catching (#143) |
| A new camera kind means editing `CsvGymRepository` (`instanceof`) | OCP | **Fixed** — one `CameraCsvFormat` per kind (#120) |
| Aiming casts to `PtzCamera`; the camera kind is chosen by comparing strings | OCP, LSP, ISP | **Fixed** — `Aimable` (#129, #139), `CameraKind` (#143) |
| One panel does five jobs (`CourtPanel`) | SRP | **Fixed** — four panels and a layout (#123) |

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

### 2. A new camera kind is one new format — #120, PR #185

**Before.** `CsvGymRepository` branched on `instanceof PtzCamera` and
`instanceof FixedCamera` to write a camera, and on the kind column to read it
back. A third kind of camera meant editing the repository.

**After.** `CameraCsvFormat` describes one kind: its `kind` column, its class,
its own columns, and how to build the camera from them. `FixedCameraCsvFormat`
and `PtzCameraCsvFormat` are listed in `CameraCsvFormats.standard()`.
`CsvGymRepository` keeps its old constructor and gains one taking a list of
formats; it refuses two formats for one kind or class and a kind named `gym` or
`court`. The file format did not change, and old files still read.

**Why it matters.** Open/closed, shown by a test: a camera class that exists
only in the tests round-trips through the repository with its own format, and
the repository is not edited.

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

**Follow-ups.** #139 (PR #175): `ManageGym.aimCamera` binds `Aimable` with a
pattern variable at its `instanceof` check, so no cast remains on the aiming
path, and a test forces the Move button on for a fixed camera to show the
refusal does not depend on the button being disabled. #140 (PR #181): the
`Aimable` Javadoc names interface segregation as its reason, and a test pins
that a stopped PTZ camera accepts an aim.

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

**Follow-up #113.** The camera now receives the real assembler and completes
the file before `CaptureHighlight.capture` returns. The window runs that whole
operation on a worker, pausing feeds and camera changes until it finishes.
`PendingCapture` only archives an already completed capture; `writeClip()` and
`PendingClipWrites` have been removed. Direct callers no longer depend on the
panel to collect and execute a write.

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

### 9. The window reaches the model only through use cases — #122, PR #143

**Before.** The panels held entities, called the repositories, and caught a
persistence exception. `Launcher`, `SampleGym`, `LiveFeed`, `SegmentFeed` and
`PhoneConnection` all lived in `ui/`.

**After.**

- `CourtPanel` changes the gym through `ManageGym` and shows `CourtView` and
  `CameraView`; the camera type is a `JComboBox<CameraKind>`. `CapturePanel`
  presses `CaptureHighlight`. `ArchivePanel` browses through `BrowseArchive`.
- `Feed` is in `application/`; `LiveFeed` in `infrastructure/simulation/`;
  `SegmentFeed` and `PhoneConnection` in `infrastructure/stream/`.
- `Launcher` and `SampleGym` are in `bootstrap/`, the composition root and the
  one place that names every concrete adapter.
- `ArchitectureTest` gains its `ui` row: `ui/` imports nothing from `domain/`
  or `infrastructure/`. Run against the old `ui/` it reported 26 violations.

**Why it matters.** The boundary between the window and the model is checked by
the build, not by a reviewer, and the business rules run without opening the
window — criterion 4 of the rubric.

### 10. `CourtPanel` is split by responsibility — #123, PR #177

**Before.** One panel selected courts, listed cameras, installed them, moved
them and aimed them.

**After.** `CourtSelectionPanel` (court selection and creation),
`CameraListPanel` (the camera list and its state actions), `CameraInstallPanel`
(installation) and `CameraPositionPanel` (moving and aiming). `CourtPanel` only
lays them out and coordinates them. The original panel tests pass unchanged,
and a new test checks the composition.

**Why it matters.** Single responsibility in the window, where it was most
visibly missing.

---

## After the plan

The final brief added a database, a careful interface and JavaDoc to what the
plan covered. What landed for them, grouped by subject; the reasoning for the
database and the operator accounts is in `CONTEXT.md`.

| Subject | Issues | What changed |
|---|---|---|
| Repository contract | #144 | Every adapter of a port passes one shared contract test |
| PostgreSQL | #145, #147, #148, #152 | PostgreSQL in Docker; highlights, the gym's structure and operators stored there through plain JDBC and parameterised queries |
| Choice of storage | #150 | `bootstrap/Storage` picks CSV or PostgreSQL once; an unreachable database stops the application instead of falling back |
| Operators and sign-in | #146, #149, #151, #153, #154, #155, #195 | People leave the gym; an operator is an account with a hashed password, signs in before the window opens, registers others, and recovers a forgotten password through a simulated link |
| Interface | #158, #159, #161, #162, #163, #188, #191, #193, #197 | Own light and dark theme; a sidebar by subject; court cards with camera states; F2 capture; archive search by court and day; the arena dashboard and spectator view; removing an empty court |
| Responsiveness | #205 | Every repository call from the window — gym changes, archive reads, rename and delete, sign-in and registration — runs on a worker through `RepositoryTask`; its button stays disabled and the message bar shows progress until it ends. A gym change borrows the model through `CaptureActivity`, as a capture does |
| Keyboard and focus | #206 | Every README flow runs from the keyboard alone, with a focus ring of at least 3:1 in both themes. Esc closes every dialog through `EscapeKey`; Enter submits each form from its own fields through `EnterKey`, so one page's Enter never fires another's form; ↑ and ↓ walk the sidebar; Tab leaves the tables through `TabKey`; a recovery step and the rename prompt put the focus on the control to type into |
| Capture | #113, #194 | A phone capture completes its file before it is archived; the live view plays from a phone app that serves one client |
| Tests | #167 | JUnit 4 imports are refused in test sources |
| Documentation | #156 | The documents state that a database and operator sign-in are in scope |

---

## What is still open

Where each one lands in the rubric is in
[PLAN-CLEAN-ARCHITECTURE.md](PLAN-CLEAN-ARCHITECTURE.md#rubric-coverage).

| Issue | What is left |
|---|---|
| #124 | `README.md` maps each SOLID principle and each layer to classes; `ARCHITECTURE.md`, `AGENTS.md`, `CLAUDE.md` and the rules describe the packages as they are |
| #164 | The documentation and screenshots show the new interface |
| #200 | `mvn javadoc:javadoc` generates navigable HTML |
| #201–#204 | JavaDoc with its contract on the domain, use cases and ports, adapters, and the window |
| #207 | No screen is cut off or overlapping at any supported window size |
| #208 | Buttons, fields and headings look the same on every screen |
| #209 | The README's first-assignment checklist names only code that exists |
| #210 | The archive page is split by responsibility |
| #211 | The launcher's phone address lookup returns `Optional` instead of `null` |
| #212 | A grader sees data survive a restart in PostgreSQL by following the README |
