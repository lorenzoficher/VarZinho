package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.PendingClipWrites;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneConnectionTest {
    private static final int BUFFER_SECONDS = 30;

    @TempDir
    Path segments;

    private final List<String> reasons = new ArrayList<>();
    private final PendingClipWrites pendingWrites = new PendingClipWrites();

    @Test
    void noAddressMeansNoPhoneAndNothingToReport() {
        assertTrue(PhoneConnection.open(null, segments, BUFFER_SECONDS, pendingWrites, reasons::add).isEmpty());
        assertTrue(PhoneConnection.open("  ", segments, BUFFER_SECONDS, pendingWrites, reasons::add).isEmpty());
        assertTrue(reasons.isEmpty());
    }

    @Test
    void badAddressMeansNoPhoneAndSaysWhy() {
        assertTrue(PhoneConnection.open("not-a-stream", segments, BUFFER_SECONDS, pendingWrites, reasons::add)
                .isEmpty());

        assertEquals(1, reasons.size());
        assertTrue(reasons.get(0).contains("not-a-stream"), reasons.get(0));
    }
}
