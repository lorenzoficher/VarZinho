# SPEC-02 — Capture

**Aggregate:** `Camera`, `FixedCamera`, `PtzCamera`, `CircularBuffer`, `Frame`
**Milestone:** Phase 1 — Domain

The heart of the system. A camera records continuously into a circular buffer
that keeps only the most recent window of footage. This is what makes it
possible to save a play that has already ended.

## Contract

```java
public abstract class Camera {
    protected Camera(String id, String model, Resolution resolution, int bufferSeconds);
    public void startRecording();
    public void stopRecording();
    public boolean isRecording();
    public void sendToMaintenance();
    public CameraStatus getStatus();
    public void record(Frame frame);
    public abstract VideoClip captureLastSeconds(int seconds);
}

public class FixedCamera extends Camera {
    public FixedCamera(String id, String model, Resolution resolution, int bufferSeconds, int angle);
}

public class PtzCamera extends Camera {
    public PtzCamera(String id, String model, Resolution resolution, int bufferSeconds);
    public void moveTo(int pan, int tilt, int zoom);
}

public class CircularBuffer {
    public CircularBuffer(int capacitySeconds);
    public void record(Frame frame);
    public List<Frame> extractLastSeconds(int seconds);
    public boolean isFull();
    public boolean isEmpty();
    public int size();
}

public class Frame {
    public Frame(Instant timestamp, int sequence);
}
```

## Behaviours

### B-1 — The buffer keeps only the most recent window

The defining rule of the project. Nothing is deleted; the oldest content is
overwritten.

- **AC-1.1** A new buffer is empty
- **AC-1.2** Recording into a buffer below capacity increases its size
- **AC-1.3** A buffer at capacity reports itself full
- **AC-1.4** Recording into a full buffer keeps the size at capacity
- **AC-1.5** Recording into a full buffer discards the oldest frame
- **AC-1.6** After overwriting, the frames present are the most recent ones,
  in order
- **AC-1.7** A buffer of capacity 3 fed frames 1..10 contains exactly 8, 9, 10

### B-2 — Extracting a window

- **AC-2.1** Extracting from an empty buffer throws `EmptyBufferException`
- **AC-2.2** Extracting more seconds than recorded throws `EmptyBufferException`
- **AC-2.3** Extracting exactly what is available returns all frames
- **AC-2.4** Extracting fewer seconds returns only the most recent ones
- **AC-2.5** Extraction does not empty the buffer — recording continues

### B-3 — Camera status governs recording

- **AC-3.1** A new camera is `INACTIVE`
- **AC-3.2** Starting recording sets the status to `ACTIVE`
- **AC-3.3** Stopping recording sets the status to `INACTIVE`
- **AC-3.4** A camera in `MAINTENANCE` cannot start recording
- **AC-3.5** An inactive camera ignores frames sent to it
- **AC-3.6** An active camera stores frames in its buffer

### B-4 — Cameras capture polymorphically

`Camera` is abstract; each subclass produces its clip its own way. A court
calls the same method on every camera with no type checks.

- **AC-4.1** `FixedCamera` produces a clip from its buffer window
- **AC-4.2** `PtzCamera` produces a clip from its buffer window
- **AC-4.3** Both are usable through a `Camera` reference
- **AC-4.4** The produced clip carries the camera's resolution and the
  requested duration
- **AC-4.5** Capturing from a camera that is not recording throws

### B-5 — A PTZ camera can be aimed

- **AC-5.1** Moving sets pan, tilt and zoom
- **AC-5.2** Values outside the accepted range are rejected

## Errors

| Condition | Exception |
|---|---|
| Extracting from an empty buffer | `EmptyBufferException` |
| Extracting a window longer than recorded | `EmptyBufferException` |
| Capturing from a camera that is not recording | `EmptyBufferException` |
| Buffer capacity not positive | `IllegalArgumentException` |
| Blank camera id | `IllegalArgumentException` |
| PTZ values out of range | `IllegalArgumentException` |

## Dependencies

Needs `VideoClip` (SPEC-03), `Resolution`, `CameraStatus`,
`EmptyBufferException`.

## Notes

The buffer stores `Frame` objects as a stand-in for footage. A `Frame` carries
a timestamp and a sequence number — no image data. This is enough to prove the
overwrite rule works, which is the part being graded.

Implementation hint: a fixed-size array with a write position that wraps using
modulo is simpler and more faithful to the concept than a list you trim.

## Out of scope

Real video encoding, frame rates, codecs, audio, streaming.
