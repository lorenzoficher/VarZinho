package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingClipWritesTest {
    private static final String FIRST = "clips/phone-1-1000-1.mp4";
    private static final String SECOND = "clips/phone-1-1001-2.mp4";

    private final List<Path> written = new ArrayList<>();
    private final PendingClipWrites pending = new PendingClipWrites();
    private final ClipAssembler deferred = pending.deferring((window, target) -> written.add(target));

    @Test
    void twoCapturesBeforeAnyTakeEachKeepTheirOwnWrite() {
        deferred.assemble(List.of(), Path.of(FIRST));
        deferred.assemble(List.of(), Path.of(SECOND));

        pending.take(clipAt(SECOND)).orElseThrow().run();
        pending.take(clipAt(FIRST)).orElseThrow().run();

        assertEquals(List.of(Path.of(SECOND), Path.of(FIRST)), written);
    }

    @Test
    void clipNobodyAskedToWriteTakesNothingWhileAnotherWriteWaits() {
        deferred.assemble(List.of(), Path.of(FIRST));

        assertTrue(pending.take(clipAt("clips/fixed-1-1000-1.mp4")).isEmpty());
        assertTrue(pending.take(clipAt(FIRST)).isPresent());
    }

    @Test
    void writeIsGoneOnceTaken() {
        deferred.assemble(List.of(), Path.of(FIRST));
        pending.take(clipAt(FIRST));

        assertTrue(pending.take(clipAt(FIRST)).isEmpty());
        assertTrue(written.isEmpty());
    }

    private static VideoClip clipAt(String path) {
        return new VideoClip(path, 30, Resolution.HD, 1.0);
    }
}
