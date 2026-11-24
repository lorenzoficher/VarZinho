package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size memory holding the most recent seconds of footage from one camera.
 *
 * <p>Writing past the end wraps around and overwrites the oldest frame. Nothing is
 * ever explicitly removed — this is not a queue.
 */
public final class CircularBuffer {

    private final Frame[] frames;
    private int writePosition;
    private int size;

    /**
     * Builds an empty buffer.
     *
     * @param capacitySeconds how many seconds the buffer holds before it starts
     *        overwriting the oldest
     * @throws IllegalArgumentException if the capacity is not at least one second
     */
    public CircularBuffer(int capacitySeconds) {
        if (capacitySeconds <= 0) {
            throw new IllegalArgumentException(
                    "a buffer holds at least one second, not " + capacitySeconds);
        }
        this.frames = new Frame[capacitySeconds];
    }

    /**
     * Keeps one more second. Once the buffer is full, the new second takes the place of
     * the oldest one, which is lost.
     *
     * @param frame the second to keep
     */
    public void record(Frame frame) {
        frames[writePosition] = frame;
        writePosition = (writePosition + 1) % frames.length;
        if (size < frames.length) {
            size++;
        }
    }

    /**
     * Reads the most recent window without consuming it. A camera keeps recording
     * over the frames just handed out, which is what lets the same seconds be
     * captured twice.
     *
     * @param seconds how many of the most recent frames to return, oldest first
     * @return the window, unmodifiable
     * @throws IllegalArgumentException if the window is not at least one second
     * @throws EmptyBufferException if that many seconds have not been recorded yet
     */
    public List<Frame> extractLastSeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException(
                    "a window covers at least one second, not " + seconds);
        }
        if (isEmpty()) {
            throw new EmptyBufferException("nothing has been recorded yet");
        }
        if (seconds > size) {
            throw new EmptyBufferException(
                    "a window of " + seconds + "s was asked of the " + size + "s recorded");
        }
        List<Frame> window = new ArrayList<>(seconds);
        int oldest = Math.floorMod(writePosition - seconds, frames.length);
        for (int offset = 0; offset < seconds; offset++) {
            window.add(frames[(oldest + offset) % frames.length]);
        }
        return List.copyOf(window);
    }

    /** {@return how many seconds the buffer can hold, fixed when it was built} */
    public int capacity() {
        return frames.length;
    }

    /** {@return how many seconds the buffer holds now, never more than its capacity} */
    public int size() {
        return size;
    }

    /** {@return whether the next second recorded will overwrite the oldest} */
    public boolean isFull() {
        return size == frames.length;
    }

    /** {@return whether nothing has been recorded yet} */
    public boolean isEmpty() {
        return size == 0;
    }
}
