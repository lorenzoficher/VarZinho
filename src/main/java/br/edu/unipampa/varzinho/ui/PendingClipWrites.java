package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The clip files presses have asked for and the capture panel has yet to write.
 *
 * <p>A capture reads the camera — its buffer, its count of clips — and that must happen
 * on the Swing thread, where the feeds keep writing to it. Writing the file takes
 * seconds and must not. So a stream camera is handed an assembler that only notes what
 * it was asked to write, and the capture panel takes that note after the capture and
 * writes it on a worker.
 *
 * <p>Each note is filed under the file it writes, and every capture names a file of its
 * own, so a highlight can only ever take the write that makes its clip — however many
 * captures, cameras or courts came in between.
 */
final class PendingClipWrites {
    private final Map<Path, Runnable> byTarget = new HashMap<>();

    /**
     * @param writer what really writes the file, once the panel runs the note
     * @return the assembler to give the stream camera in place of the writer
     */
    ClipAssembler deferring(ClipAssembler writer) {
        if (writer == null) throw new IllegalArgumentException("a deferred assembler needs something to write with");
        return (window, target) -> byTarget.put(target, () -> writer.assemble(window, target));
    }

    /**
     * @param clip the clip a capture has just handed back
     * @return the write that makes that clip's file, gone from here once taken; empty
     *     when the camera that captured it writes no file
     */
    Optional<Runnable> take(VideoClip clip) {
        return Optional.ofNullable(byTarget.remove(Path.of(clip.getFilePath())));
    }
}
