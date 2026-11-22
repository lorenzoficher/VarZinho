package br.edu.unipampa.varzinho.infrastructure.spectator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpectatorTest {
    @TempDir Path folder;

    @Test
    void reservationSurvivesRestartAndOnlyAnApprovedPaymentUnlocksItsOwnClip() throws Exception {
        FakePix pix = new FakePix();
        ClipOrders orders = new ClipOrders(folder, pix);
        UUID secret = orders.reserve(file -> Files.writeString(file, "reserved footage"));
        assertThrows(IllegalStateException.class, () -> orders.paidClip(secret));
        var payer = new PixPayer("buyer@example.com", "12345678909");
        assertEquals("copy-paste", orders.checkout(secret, payer));
        assertEquals("copy-paste", orders.checkout(secret, payer));
        assertEquals(1, pix.created);
        ClipOrders reopened = new ClipOrders(folder, pix);
        assertThrows(IllegalStateException.class, () -> reopened.paidClip(secret));
        pix.approved = true;
        assertEquals("reserved footage", Files.readString(reopened.paidClip(secret)));
        assertThrows(IOException.class, () -> reopened.paidClip(UUID.randomUUID()));
    }

    @Test
    void missingFootageCannotCreateAPayment() throws Exception {
        FakePix pix = new FakePix();
        ClipOrders orders = new ClipOrders(folder, pix);
        assertThrows(IOException.class, () -> orders.reserve(file -> { throw new IOException("expired"); }));
        assertEquals(0, pix.created);
        try (var files = Files.list(folder)) { assertEquals(0, files.count()); }
    }

    private static final class FakePix implements PixGateway {
        private int created;
        private boolean approved;
        public Charge create(UUID reference, PixPayer payer) {
            created++;
            return new Charge("123", "copy-paste", "");
        }
        public boolean approved(UUID reference, String id) { return approved; }
    }
}
