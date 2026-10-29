package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;

import java.util.Optional;

/**
 * The clip file a press has asked for and the capture panel has yet to write.
 *
 * <p>A capture reads the camera — its buffer, its count of clips — and that must happen
 * on the Swing thread, where the feeds keep writing to it. Writing the file takes
 * seconds and must not. So a stream camera is handed an assembler that only notes what
 * it was asked to write, and the capture panel takes that note right after the capture
 * and writes it on a worker.
 */
final class PendingClipWrites {
    private Runnable next;

    /**
     * @param writer what really writes the file, once the panel runs the note
     * @return the assembler to give the stream camera in place of the writer
     */
    ClipAssembler deferring(ClipAssembler writer) {
        if (writer == null) throw new IllegalArgumentException("a deferred assembler needs something to write with");
        return (window, target) -> next = () -> writer.assemble(window, target);
    }

    /**
     * @return the write the last capture asked for, gone from here once taken; empty when
     *     the camera that captured writes no file
     */
    Optional<Runnable> take() {
        Optional<Runnable> taken = Optional.ofNullable(next);
        next = null;
        return taken;
    }
}
