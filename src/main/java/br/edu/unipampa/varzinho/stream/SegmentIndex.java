package br.edu.unipampa.varzinho.stream;

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
 */
public final class SegmentIndex {

    private final Map<Integer, Path> segments;

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

    public void register(int sequence, Path segment) {
        if (segment == null) {
            throw new IllegalArgumentException("second " + sequence + " needs a segment file");
        }
        segments.put(sequence, segment);
    }

    /**
     * @throws ClipAssemblyException if the second was never registered or the ring has
     *         already overwritten it
     */
    public Path segmentOf(Frame frame) {
        Path segment = segments.get(frame.getSequence());
        if (segment == null) {
            throw new ClipAssemblyException("no segment on disk for second " + frame.getSequence());
        }
        return segment;
    }
}
