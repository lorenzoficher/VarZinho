# Testing

This project is developed test-first. A specification describes a behaviour,
its acceptance criteria become tests, and the tests are written before the
code that satisfies them.

## The cycle

1. **Red** — write a test for one acceptance criterion. Run it. It must fail,
   and it must fail for the right reason. A test that passes before the code
   exists is testing nothing.
2. **Green** — write the least code that makes it pass. Not the elegant
   version, the passing version.
3. **Refactor** — clean up with the test as a safety net. Run again.

One criterion at a time. Resist writing five tests before any code.

## Running

```bash
mvn test                              # everything
mvn test -Dtest=CircularBufferTest    # one class
mvn test -Dtest=CircularBufferTest#overwritesOldestFrameWhenFull
```

## What to test

Test behaviour that can break:

- **Rules** — the buffer overwrites when full; an author cannot be assigned
  twice; a court with no active camera refuses to capture
- **Boundaries** — empty buffer, exactly full buffer, one frame past full
- **Exceptions** — the error path is thrown, with the right type
- **Round-trips** — a highlight written to CSV and read back is equal to the
  original

Do not test:

- Getters that return a field
- Constructors that only assign
- Enum values
- `toString()`, unless its format is a contract someone depends on

A test that cannot fail is maintenance cost with no benefit. Ten tests on real
rules beat fifty on accessors.

## Naming

The method name states the behaviour, so a failure report reads as a sentence:

```java
@Test
void overwritesOldestFrameWhenBufferIsFull() { }

@Test
void throwsWhenCourtHasNoActiveCamera() { }

@Test
void keepsOriginalAuthorWhenAssignedTwice() { }
```

Never `test1()`, `testBuffer()`, or `shouldWork()`.

## Structure

Arrange, act, assert — separated by blank lines:

```java
@Test
void overwritesOldestFrameWhenBufferIsFull() {
    CircularBuffer buffer = new CircularBuffer(3);
    buffer.record(frameAt(1));
    buffer.record(frameAt(2));
    buffer.record(frameAt(3));

    buffer.record(frameAt(4));

    List<Frame> frames = buffer.extractLastSeconds(3);
    assertEquals(List.of(frameAt(2), frameAt(3), frameAt(4)), frames);
}
```

One behaviour per test. If asserting two unrelated things, write two tests.

## Exceptions

Assert the type, and assert it comes from the right call:

```java
@Test
void throwsWhenCourtHasNoActiveCamera() {
    Court court = new Court(1, Sport.FUTSAL);
    court.installCamera(inactiveCamera());

    assertThrows(NoActiveCameraException.class,
                 () -> court.triggerCapture(HighlightType.GOAL));
}
```

Never assert on a message string — messages change, behaviour does not.

## Test data

Build objects through small helpers rather than repeating constructor calls:

```java
private Camera activeCamera() {
    Camera camera = new FixedCamera("cam-test", "model", Resolution.HD, 0);
    camera.startRecording();
    return camera;
}
```

When a test needs a file, use JUnit's `@TempDir`. Never write into the project
directory, and never depend on a file another test created.

## Coverage

We do not chase a percentage. The target is that **every acceptance criterion
in `docs/specs/` has a test**, and that the rules listed above are covered.

A spec criterion with no test is an incomplete issue.

## Where tests live

`src/test/java/`, mirroring the main package structure. A test class sits in
the same package as its subject, named `<Class>Test`.
