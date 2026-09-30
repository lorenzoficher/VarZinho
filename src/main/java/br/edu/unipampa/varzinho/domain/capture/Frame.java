package br.edu.unipampa.varzinho.domain.capture;

import java.time.Instant;
import java.util.Objects;

/**
 * One second of footage, as far as this model is concerned.
 *
 * <p>It carries when it was captured and where it sits in the camera's sequence, and
 * no image data at all — the project models the capture rules, and never decodes a
 * picture. That is enough to prove the buffer overwrites the oldest content.
 */
public final class Frame {

    private final Instant timestamp;
    private final int sequence;

    public Frame(Instant timestamp, int sequence) {
        if (timestamp == null) {
            throw new IllegalArgumentException("a frame needs the instant it was captured");
        }
        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    /**
     * A frame is the instant it was captured plus its place in the sequence. The
     * buffer hands out windows of frames, and a window is compared by what is in it.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Frame frame)) {
            return false;
        }
        return sequence == frame.sequence && timestamp.equals(frame.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, sequence);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getSequence() {
        return sequence;
    }
}
