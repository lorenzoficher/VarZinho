# Architecture

How the code is organised, and the two structural decisions that matter:
persistence behind an abstraction, and errors as domain exceptions.

## Layers

```
Main, ConsoleDemo                entry points: the window, and the text demonstration
bootstrap/                       Launcher, Storage and SampleGym: the composition root
ui/                              Swing windows and panels
application/                     use cases, the read models the window displays, Feed
application/port/                storage contracts and persistence failures
infrastructure/persistence/      CSV, PostgreSQL and in-memory storage adapters
infrastructure/stream/           FFmpeg adapter behind ClipAssembler, the phone's Feed
infrastructure/simulation/       LiveFeed, the clock that feeds the simulated cameras
infrastructure/spectator/        the public spectator view
domain/                          structure, capture, highlight and the operator account
enums/, exception/               shared values and domain failures
```

These map onto the circles of Clean Architecture: `domain/` holds the
entities, `application/` the use cases, `ui/` the presentation and
`infrastructure/` the external details — files, PostgreSQL, FFmpeg and the
spectators' web page. `bootstrap/` sits outside all four and wires them
together.

**Dependencies point inward.** The ports live in `application/port/`:
`HighlightRepository`, `GymRepository`, `OperatorStore` and `RecordingEditor`.
Their implementations live in `infrastructure/persistence/`. The FFmpeg
adapter lives in `infrastructure/stream/` and implements `ClipAssembler`, which
remains in `domain/capture/` because `StreamCamera`, an entity, depends on it.
`Feed`, the source of the window's seconds of footage, is declared in
`application/` and implemented by `LiveFeed` and the phone's `SegmentFeed`.

The domain imports no application, infrastructure, UI or bootstrap package,
nor Swing or AWT. Application code imports no infrastructure, UI or bootstrap
package, nor Swing or AWT. Neither of the two imports `java.sql`, `javax.sql`
or the PostgreSQL driver. Infrastructure imports no UI or bootstrap package.
The window imports nothing from the domain or infrastructure. Domain
exceptions import no other project package. `ArchitectureTest` reads the
`import` lines under `src/main/java` and fails on any of these.

The window constructs no adapter. `bootstrap/Launcher` is the composition
root: it asks `bootstrap/Storage` for the storage, builds the use cases and,
when there is one, the phone, and hands them to the window, which reaches the
model only through the use cases in `application/`. The use cases take their
ports in the constructor — `ManageGym(Gym, GymRepository)`,
`CaptureHighlight(Gym, HighlightRepository)`, `OperatorAccess(OperatorStore)` —
so their tests run them on the in-memory adapters, with no window open.

The brief's responsibilities remain distinct: `domain/` models the system,
application ports define storage contracts, infrastructure implements storage
and video integration, and `ui/` handles interaction.

## Persistence

Three things are kept between runs: the highlight archive, the gym's structure
and the operator accounts. Each has its own port in `application/port/`, and
each port has two adapters a run can store in — a CSV file and a PostgreSQL
table — plus an in-memory one for tests and for a damaged gym file:

| Port | CSV (default) | PostgreSQL | In memory |
|---|---|---|---|
| `HighlightRepository` | `CsvHighlightRepository`, `data/highlights.csv` | `PostgresHighlightRepository`, table `highlight` | `InMemoryHighlightRepository` |
| `GymRepository` | `CsvGymRepository`, `data/gym.csv` | `PostgresGymRepository`, tables `gym`, `court`, `camera` | `InMemoryGymRepository` |
| `OperatorStore` | `CsvOperatorStore`, `data/operators.csv` | `PostgresOperatorStore`, table `operator_account` | `InMemoryOperatorStore` |

The adapters of one port pass the same contract test
(`HighlightRepositoryContractTest`, `GymRepositoryContractTest`,
`OperatorStoreContractTest`), and nothing else in the system changes between
them — because nothing else in the system mentions CSV or SQL.

```java
public interface HighlightRepository {
    void save(Highlight highlight) throws RepositoryException;
    void rename(String id, String title) throws RepositoryException;
    void delete(String id) throws RepositoryException;
    Optional<Highlight> findById(String id) throws RepositoryException;
    List<Highlight> findAll() throws RepositoryException;
    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}
```

### How the launcher picks one

The choice is made once per run, for all three ports together, in
`bootstrap/Storage.fromEnvironment`, which `bootstrap/Launcher` calls before
anything else:

- `VARZINHO_DATABASE_URL` unset or blank: the three CSV adapters, under `data/`.
- `VARZINHO_DATABASE_URL` set: the three PostgreSQL adapters, opened with
  `VARZINHO_DATABASE_USER` and `VARZINHO_DATABASE_PASSWORD`. A missing user is
  refused before any connection is tried.

A database that is named but unreachable stops the application before any
window opens; it never falls back to CSV, because highlights written to a file
would look lost from the database. The error message names the database but
masks its password.

The launcher then hands the operator store to `OperatorAccess`, and
`ui/SignInDialog` asks for a sign-in — or for the first operator, when none is
stored — before the main window is built. Closing that gate exits without
opening the window.

This is **dependency inversion**: the high-level policy depends on an
abstraction, not on the low-level detail. It is also the project's clearest
justification for declaring an interface at all.

### Browsing by local day

`BrowseArchive.byDay(day)` lists a local calendar day, newest first;
`byDay(day, courtNumber)` narrows it to one court. Each capture instant is
converted to the gym's `ZoneId` before comparing dates, so UTC midnight and
daylight-saving changes cannot move a highlight to the wrong local day.

The constructor accepting a `Clock` and `ZoneId` makes the gym's zone explicit
and lets tests fix the current instant independently of the machine. The
original constructor still uses the system clock and zone, like the current
window. `all()` and `byCourt()` retain their existing behaviour.

`todaySummary()` and `todaySummary(courtNumber)` use the injected clock in the
gym's zone. The immutable `ArchiveSummary` reports a count and the latest
capture as `Optional<Instant>`; an empty day has zero highlights and no last
instant. Both day listings and summaries retain the warning when damaged
records are skipped, and filter only the readable highlights. An unreadable
archive still propagates `RepositoryException`.

### The CSV format

```csv
id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb
h-001,2026-09-12T20:14:33Z,3,cam-a1,/clips/h-001.mp4,30,FULL_HD,42.5
h-002,2026-09-12T20:41:02Z,3,cam-a1,/clips/h-002.mp4,30,FULL_HD,41.8
```

No column names a person: a highlight records nobody. The columns after
`clipPath` are the `VideoClip`, rebuilt on load.

The format is an implementation detail of `CsvHighlightRepository`. No other
class parses or produces it.

### The PostgreSQL table

`PostgresHighlightRepository` creates its `highlight` table when it is missing,
so the Docker database and the tests share one schema with no migration tool.
It holds the same values as the CSV, with two differences: a `position` column
keeps the order first saved, and the capture instant takes two columns —
`captured_epoch_second` and `captured_nano` — because `TIMESTAMPTZ` stops at
microseconds and an `Instant` carries nanoseconds. Every value reaches SQL as a
bound parameter, and every `SQLException` surfaces as a `RepositoryException`
with it as the cause.

### Operator accounts in PostgreSQL

`PostgresOperatorStore` implements `OperatorStore` through plain JDBC. Opening
it creates `operator_account` and a unique index on `lower(username)` in one
transaction. The database arbitrates duplicate inserts, including concurrent
inserts from different windows; a duplicate is reported as
`IllegalArgumentException`, as required by the port. Other SQL failures become
`RepositoryException` with their SQL cause chained.

The table stores only the name, username and encoded password hash. Reading
uses `Operator.restore` to retain the original hash, never rehashing it or
storing a plain password. `bootstrap/Storage` opens it alongside the other
PostgreSQL repositories when a database is named. Its contract and database constraints are tested
against Testcontainers PostgreSQL, with integration tests skipped without Docker.

### The gym's structure

Courts and cameras created in the window are kept by a second interface,
`GymRepository` (`save(Gym)` and `Optional<Gym> load()`), implemented by
`CsvGymRepository` in `data/gym.csv`:

```csv
kind,values
gym,VarZinho Arena,100 Sports Avenue
court,1
fixed,1,fixed-1,Fixed Pro,FULL_HD,30,ACTIVE,45
ptz,1,ptz-1,PTZ Pro,HD,30,ACTIVE,0,0,1
```

A camera line holds its court, id, model, resolution, buffer length and status,
then the angle (fixed) or pan, tilt and zoom (PTZ). Loading rebuilds the gym
only through the domain's own behaviour (`addCourt`, `installCamera`,
`startRecording`, `sendToMaintenance`, `moveTo`), so the file cannot produce a
gym the domain would have refused.

A stream camera is never written to this file. It is runtime equipment,
installed afresh on every start, and a court that holds only stream cameras is
left out with it, or every start would add another.

**One CSV format per camera kind.** `CsvGymRepository` owns the seven shared
columns: kind, court number, id, model, resolution, buffer length and status.
A `CameraCsvFormat<C>` owns only its kind's additional columns and rebuilds
that camera through its domain behaviour. `FixedCameraCsvFormat` stores the
angle; `PtzCameraCsvFormat` stores pan, tilt and zoom and restores them through
`moveTo`. `CameraCsvFormats.standard()` lists both without changing the file
format, so existing gym files still load.

The default constructor uses those standard formats. The constructor accepting
a format list allows a new camera kind to round-trip without editing the
repository: saving selects its exact camera class, and loading selects its
kind. Duplicate kinds or classes and the reserved kinds `gym` and `court` are
refused at construction. Unregistered cameras are refused before a save
changes the file; an unknown kind on load fails with its line number. The
domain remains independent of CSV.

Unlike the highlight archive, one unreadable line fails the whole load: half a
gym would be a gym nobody built. The window then opens on the sample gym and
keeps its changes in memory (`InMemoryGymRepository`), leaving the damaged file
for someone to repair instead of overwriting it.

`PostgresGymRepository` keeps the same structure in three tables it creates
when they are missing: `gym` (one row at most), `court` and `camera`, where a
`position` column keeps the order cameras were installed in and a check lets
each kind fill only its own aim columns. Saving deletes and rewrites the three
tables inside one transaction, so a save that fails part-way rolls back and
leaves the gym stored before untouched. Loading reads the three tables in one
snapshot and rebuilds the gym through the domain, as the CSV adapter does.
Stream cameras, and courts that hold only them, are never saved, for the same
reason as in the CSV file. It passes the same
`GymRepositoryContractTest` as the CSV and in-memory adapters.

## Exceptions

Failures the application can act on use the project's domain or repository
exception types and are never swallowed. Constructors reject invalid values
with `IllegalArgumentException`; a camera rejects an invalid state transition
with `IllegalStateException`.

```
DomainException (abstract, unchecked)
├── NoActiveCameraException
├── EmptyBufferException
├── DuplicateCameraException
└── ClipAssemblyException

RepositoryException (checked, in `application/port/`)
└── CorruptedRecordException (in `application/port/`: it carries `Highlight`s)
```

### When each is thrown

**`NoActiveCameraException`** — a capture is triggered on a court whose cameras
are all inactive or in maintenance. Returning `null` or an empty highlight
would hide a real operational failure: somebody pressed the button and got
nothing, and they need to know why.

**`EmptyBufferException`** — a clip is requested from a camera that has not
recorded enough footage yet, typically right after being switched on. The
window asked for does not exist.

**`DuplicateCameraException`** — a camera is installed under an id that a court
of the gym already uses. Clips are named after their camera and the archive
tells cameras apart by id alone, so ignoring the second camera in silence would
leave the caller believing it was installed.

**`ClipAssemblyException`** — a stream camera's clip file could not be written:
a second with no segment on disk, because it was never registered or the ring
has already overwritten it, or FFmpeg failing or timing out. The highlight is
not archived, because a record pointing at a file that was never written would
promise a clip nobody can open. When the failure comes from an I/O error or an
interruption, it carries that cause. When FFmpeg exits with an error, the
message carries the last lines of its log. When FFmpeg times out, the message
says how long it was waited for.

**`CorruptedRecordException`** — a line in the CSV cannot be parsed. Checked,
because the caller can reasonably recover: skip the record, report it, and
carry on loading the rest. The archive should not be lost because one line is
malformed.

### Checked or unchecked?

Unchecked exceptions represent invalid arguments, invalid state transitions
and capture failures. Repository failures are checked because the interface
can report them to the user or recover from a damaged file.

### Rules

- Never `catch (Exception e)` — catch the specific type
- Never leave a `catch` block empty
- Never throw from a getter
- A constructor that receives invalid arguments throws; an object that exists
  is valid

### Where they are caught

In the layer that can act on them. The domain throws, the repository lets
domain exceptions through, and the window that started the operation catches
the specific type and shows the message — typically with `JOptionPane`.

Three failures are recovered from before they reach a window, each because
the recovery is a rule of its own: `BrowseArchive` turns a
`CorruptedRecordException` into an `ArchiveListing` carrying the readable
highlights and a warning; `GymSession` answers an unreadable saved gym by
running on the fallback gym without keeping it; and `bootstrap/Launcher`
refuses to open when `Storage` reports an unreachable database.

Section 2.8 of the brief requires exactly this: domain exceptions handled in
the layer that talks to the user, reported as a clear message. A stack trace on
the console is not a message, and a swallowed exception is worse than either.

Triggering a court whose cameras are all inactive is the case to get right.
`NoActiveCameraException` reaches the screen and the user is told why nothing
was captured — which is the whole reason the exception exists instead of a
`null` return.

## The interface

`ManageGym` returns immutable snapshots through `CameraView` and `CourtView`.
Each camera view carries its id, `CameraKind`, `Resolution`, `CameraStatus` and
aiming capability as data. PTZ pan, tilt and zoom are `OptionalInt` values:
they are present for a PTZ and empty for other kinds. A court view reports
its total camera count and its recording count from the captured statuses,
so a previous view never changes when the gym is modified. The existing
description remains available for the current camera list; status badges can
use `getStatus()` directly without comparing that text or importing domain types.

Stream cameras appear as `CameraKind.STREAM` with their actual status. They
remain outside `CameraKind.installable()`: a manual `CameraSpec` cannot build
a phone camera without its stream connection and assembler.

`Main` calls `bootstrap/Launcher`, which hands control to Swing. `GymSession` loads
the saved gym or creates the sample gym on the first run. The court panel
creates courts, installs and moves cameras, changes camera status and points a
PTZ camera. The capture panel saves a highlight from the selected court, and
the archive panel lists persisted highlights with an optional court filter.
The operators page (`OperatorsPanel`) lets the signed-in operator register
another account through `SignedInOperator`, and the sign-in offers a simulated
password recovery (`PasswordRecoveryDialog`) through `OperatorAccess`.

The stream camera receives the real `FfmpegClipAssembler`: capture returns only
after its clip is written. `CapturePanel` runs the whole capture on a
`SwingWorker`, then archives the completed highlight on the Swing thread.
While the worker uses the model, the window skips feed ticks and disables
court and camera changes. A phone disconnect waits for capture to finish.
Both success and failure restore the controls and feeds. There is no registry
of deferred clip writes; a caller outside the panel gets the same completed
capture as the window.

Every window action that feeds or changes the gym or its cameras must respect
`CaptureActivity`, including actions added by future screens. A worker borrows
the model exclusively until capture finishes; callers outside the window must
provide equivalent exclusion. The current sign-in runs before the window opens.
Expected capture refusals are shown to the operator. Unexpected worker failures
also log their original exception and stack trace, and always show a nonempty
message before the controls and feeds resume.

They are deliberately thin. Every rule lives in the domain and the interface
only calls it. If logic starts accumulating in a window class, it belongs in a
domain class instead — a screen that decides whether a court can be captured
has taken a rule away from `Court`.

The brief asks for a minimal interface, and minimal is the target. The panels
expose the real operations required for the demonstration and report domain or
repository failures through dialogs.

That the domain stays independent of the screen is the part worth defending. A
`Court` that compiles without `javax.swing` on the classpath is the proof, and
it is why this layer sits on top of the other two rather than inside them.
