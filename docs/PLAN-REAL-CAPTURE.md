# Plan — real capture from a phone camera

VarZinho 2.0 starts from the delivered 1.0 model (commit `2a01f07`) and
reverses one decision: **clips are real video files**. A phone streams over
Wi-Fi, the last 30 seconds live on disk, and pressing *Capture* writes an
`.mp4` you can open.

Target: one night, one phone, one court. Not a product.

---

## The idea in one paragraph

FFmpeg records the phone stream into **1-second segment files that overwrite
each other in a ring** (`seg-00.ts` … `seg-39.ts`). Each finished segment is one
`Frame` — the domain already counts footage in whole seconds, so the
`CircularBuffer` does not change. On capture, the camera hands the 30 frames of
its window to a `ClipAssembler`, which concatenates the matching segments into
one `.mp4`. The domain never learns FFmpeg exists: it talks to an interface,
and the FFmpeg code lives in a new outer package, exactly like `repository/`.

```
phone ──HTTP──▶ ffmpeg (recorder) ──▶ buffer/seg-NN.ts + buffer/segments.csv
                                               │
                         SegmentFeed polls ────┘  one new line = one Frame
                                │
                   court.record(frame) ──▶ StreamCamera ──▶ CircularBuffer (30)
                                                 │
                     Capture button ──▶ triggerCapture()
                                                 │
                         ClipAssembler ◀─────────┘  (interface, domain)
                                │
             FfmpegClipAssembler ──▶ ffmpeg -f concat ──▶ clips/<id>.mp4
```

---

## Phase 0 — Setup (≈ 30 min, you, no code)

1. **FFmpeg:** `winget install Gyan.FFmpeg`, then open a *new* terminal and
   check `ffmpeg -version`.
2. **Phone:** install *IP Webcam* (Android) or *DroidCam* (Android/iOS). Start
   the server; note the URL, e.g. `http://192.168.0.42:8080/video`.
3. **Same Wi-Fi** for the phone and the PC. Test in the browser, then
   `ffplay http://192.168.0.42:8080/video`. If it does not open, check the
   Windows firewall and the network (university Wi-Fi often isolates clients;
   use the phone's hotspot instead).
4. **Maven on PATH** for this terminal:
   `$env:Path = "$env:LOCALAPPDATA\Programs\apache-maven-3.9.9\bin;$env:Path"`
5. Baseline: `mvn test` must be green before anything changes.

**Done when:** `ffplay` shows the phone's image on the PC.

---

## Phase 1 — Spike, FFmpeg only (≈ 45 min, no Java)

Prove the whole trick on the command line before writing any class. Run from
the project root, with `buffer/` and `clips/` created.

**Recorder** (leave it running):

```powershell
ffmpeg -hide_banner -loglevel warning `
  -i http://192.168.0.42:8080/video `
  -an -r 30 -c:v libx264 -preset veryfast -tune zerolatency -pix_fmt yuv420p `
  -force_key_frames "expr:gte(t,n_forced*1)" `
  -f segment -segment_time 1 -segment_wrap 40 -reset_timestamps 1 `
  -segment_list buffer/segments.csv -segment_list_type csv -segment_list_size 0 `
  buffer/seg-%02d.ts
```

Why each part matters:

- `-force_key_frames …*1` — a keyframe every second, so every segment starts
  on one and the cuts join cleanly. Without it the clip stutters or goes grey.
- `-segment_wrap 40` — the ring holds 40 files while the buffer keeps 30. The
  10-file margin is what stops FFmpeg from overwriting a segment while it is
  being copied into a clip.
- `-segment_list … csv` — FFmpeg appends one line per **finished** segment.
  That line is our clock tick; Java never has to guess when a second ended.

**Clip** (in a second terminal, after ≥ 30 s): take the last 30 names from
`buffer/segments.csv`, write them to `buffer/list.txt` as `file 'seg-07.ts'`
lines, then:

```powershell
ffmpeg -hide_banner -f concat -safe 0 -i buffer/list.txt -c copy clips/test.mp4
```

**Check in the spike, write down the answers:**

- [ ] Does `segments.csv` grow by one line per second? (With
  `-segment_list_size 0` it should keep every entry.)
- [ ] Does `clips/test.mp4` play for ~30 s without a frozen start?
- [ ] How long does the concat take? (Expect well under a second with
  `-c copy`.)
- [ ] What happens to the recorder when the phone app is closed? (It should
  exit — that is what Phase 4 reacts to.)

**Done when:** a 30-second `.mp4` of your phone plays in the Windows player.
If this phase fails, stop and fix it here — Java will not make it work.

---

## Phase 2 — Domain, TDD (≈ 1 h)

Domain changes are small on purpose. `Frame`, `CircularBuffer`, `Court`,
`Highlight` and `VideoClip` do **not** change.

### 2.1 `exception/ClipAssemblyException`

`final class ClipAssemblyException extends DomainException` — same family as
`EmptyBufferException`, so the capture panel can catch it next to the others.
Constructor `(String message, Throwable cause)`: always chain the cause.

### 2.2 `domain/capture/ClipAssembler` (interface)

```java
/**
 * Turns the seconds a camera kept into a playable file.
 *
 * @throws ClipAssemblyException if the file could not be written
 */
void assemble(List<Frame> window, Path target);
```

This is the port. It is the only thing the domain knows about real video.

### 2.3 `domain/capture/StreamCamera extends Camera`

A third camera kind, alongside `FixedCamera` and `PtzCamera` — polymorphism
doing real work now, not just a different `describe()`.

- Fields: `streamUrl` (validated non-blank), `assembler` (non-null).
- `captureLastSeconds(seconds)`: `recordedWindow(seconds)` → path
  `clipName(last) + ".mp4"` → `assembler.assemble(window, Path.of(path))` →
  `clipFrom(path, seconds)`.
- `describe()` → `describeAs("stream")`.

**Tests (`StreamCameraTest`, with a hand-written fake assembler):**

- `handsTheRecordedWindowToTheAssembler` — records 30 frames, captures, the
  fake saw those 30 frames in order.
- `returnsAClipAtThePathItAskedTheAssemblerToWrite`
- `propagatesAssemblyFailureWithoutCountingAClip` — fake throws; the exception
  reaches the caller.
- `refusesToCaptureWhenNotRecording` — `EmptyBufferException`, assembler never
  called.
- Constructor rejects blank URL and null assembler.

**Done when:** `mvn test` green, and `domain/` still imports nothing outside
`domain/`, `enums/`, `exception/` and the JDK.

---

## Phase 3 — FFmpeg adapter, TDD where it pays (≈ 1 h 30)

New package `br.edu.unipampa.varzinho.stream` — the outer ring, sibling of
`repository/`. It knows the domain; the domain does not know it.

### 3.1 `SegmentLog`

Reads `buffer/segments.csv` incrementally: remembers how many lines it has
already consumed and returns only the new segment file names.

- `List<Path> readNew()`; a missing file returns an empty list (the recorder
  has not produced anything yet).
- Tests on a `@TempDir`: returns only new lines, ignores a trailing half-written
  line, empty when the file does not exist.

### 3.2 `SegmentIndex`

Maps a frame's `sequence` to its segment file. Filled by the feed, read by the
assembler.

- `register(int sequence, Path segment)`, `Path segmentOf(Frame)`.
- Forgets entries older than the ring size (40) so it does not grow all night.
- Test: an unknown sequence throws `ClipAssemblyException` naming it.

### 3.3 `FfmpegClipAssembler implements ClipAssembler`

1. **Freeze first:** copy each segment of the window into a temp directory
   *before* doing anything slow — this is the other half of the wrap margin.
2. Write `list.txt` with `file '<absolute path>'` lines (forward slashes; quote
   escaping for paths with spaces — your home folder has one).
3. `ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-f",
   "concat", "-safe", "0", "-i", list, "-c", "copy", target)`, stderr merged,
   `waitFor(10, SECONDS)`.
4. Non-zero exit or timeout → `ClipAssemblyException` with the last lines of
   the output. `IOException` / `InterruptedException` → chained, never
   swallowed (restore the interrupt flag).
5. Create `clips/` if missing; delete the temp directory afterwards.

Tests: build the concat list as a pure function and test *that* (order, path
escaping). One integration test that really runs FFmpeg on two tiny
generated segments, guarded with `Assumptions.assumeTrue(ffmpegAvailable())`
so the suite still passes on a machine without it.

### 3.4 `FfmpegRecorder`

Owns the long-running recording process from Phase 1.

- `start()` — clears `buffer/`, launches the Phase 1 command, redirects its
  output to `buffer/recorder.log` (an unread pipe fills up and freezes FFmpeg).
- `stop()` — writes `q` to its stdin for a clean exit, then `destroy()` after
  a short wait. Registered in a shutdown hook.
- `onExit(Runnable)` — so the window hears when the phone drops.

No unit tests beyond argument building; it is verified by hand in Phase 5.

**Done when:** `mvn test` green; the integration test ran (not skipped) on
your machine.

---

## Phase 4 — Wiring the interface (≈ 1 h)

### 4.1 `ui/SegmentFeed`

The real-time twin of `LiveFeed`. A Swing `Timer` at 250 ms calls `poll()`:
for every path from `SegmentLog.readNew()`, `index.register(sequence, path)`
and `court.record(new Frame(Instant.now(), sequence++))`. Same shape as
`LiveFeed.tick`, so `GymSessionTest`-style tests with fixed instants still work.

### 4.2 Startup (`Main` / `VarZinhoWindow.open`)

- Read the URL from `VARZINHO_STREAM_URL`. **Absent → the app behaves exactly
  like 1.0** (synthetic `LiveFeed`). This keeps every existing test and the old
  demo alive.
- Present → build `SegmentIndex`, `FfmpegClipAssembler`, `FfmpegRecorder`;
  install `StreamCamera("phone-1", "Phone", Resolution.HD, 30, url, assembler)`
  on court 1, `startRecording()`, start the recorder, use `SegmentFeed`
  instead of `LiveFeed`.

### 4.3 Persistence — the one trap

`CsvGymRepository.format` throws `IllegalArgumentException` for an unknown
camera kind, so saving the gym would fail as soon as the phone is installed.
Decision: **the phone camera is runtime equipment and is not stored** — skip
`StreamCamera` in `save`, and install it fresh on every start. (Storing it
would force the repository to build an assembler, dragging FFmpeg into
persistence.) Add a test: saving a gym with a `StreamCamera` stores the
others and does not throw.

`CsvHighlightRepository` needs nothing: it already stores `filePath`, and now
that path is real.

### 4.4 Capture and archive panels

- `CapturePanel`: add `ClipAssemblyException` to the existing
  `catch (NoActiveCameraException | EmptyBufferException …)`.
- `ArchivePanel`: an **Open clip** button → `Desktop.getDesktop().open(file)`
  when `clip.exists()`, an error message when it does not.
- Header: an **Open live view** button → `Desktop.getDesktop().browse(url)`.
  This is the whole preview. Showing video inside Swing needs a decoding
  library and is out of tonight's scope.

### 4.5 Phone drops

`recorder.onExit(...)` → on the Swing thread, `camera.stopRecording()` and a
status message. From then on the domain answers by itself: *Capture* throws
`NoActiveCameraException`, the panel shows it. Reconnecting is a restart of
the app — fine for tonight.

---

## Phase 5 — End to end (≈ 45 min)

```powershell
$env:VARZINHO_STREAM_URL = "http://192.168.0.42:8080/video"
mvn exec:java
```

- [ ] Buffer status climbs 0 → 30 and stops at 30.
- [ ] *Capture* before 30 s → the "not enough recorded" message, no file.
- [ ] *Capture* after 30 s → new row in the archive; **Open clip** plays the
      last 30 s, ending at the moment you pressed.
- [ ] Two captures 5 s apart → two different files, both playable.
- [ ] Close the phone app → status says so; *Capture* shows "no active camera".
- [ ] Close the window → no `ffmpeg.exe` left in Task Manager.
- [ ] Restart without the variable → the 1.0 behaviour, `mvn test` green.

Record a short screen capture of this checklist — it is the demo.

---

## Phase 6 — Documentation (≈ 20 min)

The 1.0 documents say real capture is permanently out of scope. In 2.0 that is
no longer true, and the docs must say so rather than contradict the code:

- `CONTEXT.md` — record the reversal and why (FFmpeg as an external process,
  segments as frames, the domain behind `ClipAssembler`).
- `AGENTS.md`, `.claude/rules/project-constraints.md` — FFmpeg is now a
  runtime dependency (external binary, not a Maven library); remove "no real
  video capture" from out of scope.
- `CLAUDE.md` — the "Current state" section still describes 1.0.
- `README.md` (Portuguese) — how to set up the phone and the variable.

---

## Time budget

| Phase | Time | Cumulative |
|---|---|---|
| 0 Setup | 0:30 | 0:30 |
| 1 FFmpeg spike | 0:45 | 1:15 |
| 2 Domain | 1:00 | 2:15 |
| 3 Adapter | 1:30 | 3:45 |
| 4 Interface | 1:00 | 4:45 |
| 5 End to end | 0:45 | 5:30 |
| 6 Docs | 0:20 | 5:50 |

If the night runs short, the cut line is after Phase 5 item 3: one real clip
you can open is the goal; everything after it is polish.

## Risks, in the order they usually bite

1. **Network** — the phone is unreachable from the PC (client isolation,
   firewall). Mitigation: phone hotspot. Found in Phase 0, not Phase 5.
2. **Keyframes** — a clip with a grey or frozen first second means the
   segments do not start on keyframes. Mitigation: the `-force_key_frames`
   flag; verified in Phase 1.
3. **Wrap race** — a clip containing a second from 40 s ago means a segment
   was overwritten mid-copy. Mitigation: ring 40 > buffer 30, and copy first.
4. **Paths with spaces** — `C:\Users\Lorenzo Ficher\…` breaks unquoted
   arguments and concat lists. Mitigation: `ProcessBuilder` with separate
   arguments (never one string), escaped lines in `list.txt`, tested.
5. **Orphan FFmpeg** — an app killed from the IDE leaves the recorder running
   and holding `buffer/`. Mitigation: shutdown hook; `taskkill /IM ffmpeg.exe
   /F` if it happens.
6. **Clock drift** — the phone's frame rate varies, so a "second" is really a
   segment. Accepted: the buffer counts segments, which is what a viewer
   perceives as seconds anyway.

## Out of scope tonight

Video preview inside Swing, more than one phone, reconnecting automatically,
audio, a physical button, disk-space management of `clips/`, storing the
stream camera in `gym.csv`.
