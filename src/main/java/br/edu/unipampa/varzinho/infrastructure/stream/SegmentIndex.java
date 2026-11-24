package br.edu.unipampa.varzinho.infrastructure.stream;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which segment file holds which second, for the seconds the ring still has on disk.
 *
 * <p>The ring reuses file names, so an entry older than the ring points at a file that
 * now holds a different second. Those entries are forgotten rather than trusted.
 *
 * <p>Safe to share between threads: the feed registers seconds while a clip being written
 * elsewhere looks its own seconds up.
 */
public final class SegmentIndex {

    private final Map<Integer, Path> segments;

    /**
     * Remembers at most {@code ringSize} seconds, forgetting the oldest first.
     *
     * @param ringSize how many segment files the recorder's ring holds
     * @throws IllegalArgumentException if the ring holds no segment
     */
    public SegmentIndex(int ringSize) {
        if (ringSize < 1) {
            throw new IllegalArgumentException("a ring must hold at least one segment");
        }
        this.segments = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Path> eldest) {
                return size() > ringSize;
            }
        };
    }

    /**
     * Records that {@code segment} now holds second {@code sequence}. Once the index is full,
     * the oldest second is forgotten, because the ring has reused its file.
     *
     * @param sequence the second's number, as the camera's frames carry it
     * @param segment the file that holds that second
     * @throws IllegalArgumentException if the segment file is missing
     */
    public synchronized void register(int sequence, Path segment) {
        if (segment == null) {
            throw new IllegalArgumentException("second " + sequence + " needs a segment file");
        }
        segments.put(sequence, segment);
    }

    /**
     * The file that holds the frame's second, while the ring still has it.
     *
     * @param frame a frame of the camera this index serves
     * @return the segment file registered for the frame's second
     * @throws ClipAssemblyException if the second was never registered or the ring has
     *         already overwritten it
     */
    public synchronized Path segmentOf(Frame frame) {
        Path segment = segments.get(frame.getSequence());
        if (segment == null) {
            throw new ClipAssemblyException("no segment on disk for second " + frame.getSequence());
        }
        return segment;
    }
}
