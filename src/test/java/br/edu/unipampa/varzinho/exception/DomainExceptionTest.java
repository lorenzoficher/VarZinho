package br.edu.unipampa.varzinho.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void catchesAnInactiveCourtThroughTheAbstractParent() {
        DomainException thrown =
                assertThrows(DomainException.class, this::triggerCaptureOnADarkCourt);

        assertEquals("court 3 has no active camera", thrown.getMessage());
    }

    @Test
    void catchesAnEmptyBufferThroughTheAbstractParent() {
        DomainException thrown =
                assertThrows(DomainException.class, this::clipACameraThatJustWokeUp);

        assertEquals("cam-a1 has recorded nothing yet", thrown.getMessage());
    }

    @Test
    void catchesAFailedAssemblyThroughTheAbstractParentWithItsCause() {
        IOException cause = new IOException("disk full");

        DomainException thrown =
                assertThrows(DomainException.class, () -> assembleOnAFullDisk(cause));

        assertSame(cause, thrown.getCause());
    }

    // Neither helper declares `throws`: that is what proves the family is unchecked,
    // and it stops compiling the day somebody reparents DomainException.
    private void triggerCaptureOnADarkCourt() {
        throw new NoActiveCameraException("court 3 has no active camera");
    }

    private void clipACameraThatJustWokeUp() {
        throw new EmptyBufferException("cam-a1 has recorded nothing yet");
    }

    private void assembleOnAFullDisk(IOException cause) {
        throw new ClipAssemblyException("could not write clips/phone-1-40-1.mp4", cause);
    }
}
