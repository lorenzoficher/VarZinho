package br.edu.unipampa.varzinho.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    // Neither helper declares `throws`: that is what proves the family is unchecked,
    // and it stops compiling the day somebody reparents DomainException.
    private void triggerCaptureOnADarkCourt() {
        throw new NoActiveCameraException("court 3 has no active camera");
    }

    private void clipACameraThatJustWokeUp() {
        throw new EmptyBufferException("cam-a1 has recorded nothing yet");
    }
}
