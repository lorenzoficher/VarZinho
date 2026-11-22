package br.edu.unipampa.varzinho.infrastructure.spectator;

import java.io.IOException;
import java.util.UUID;

interface PixGateway {
    record Charge(String id, String code, String qrImage) { }
    final class ClosedPayment extends IOException { }
    Charge create(UUID reference, PixPayer payer) throws IOException;
    boolean approved(UUID reference, String id) throws IOException;
}
