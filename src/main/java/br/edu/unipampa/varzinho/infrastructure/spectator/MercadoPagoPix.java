package br.edu.unipampa.varzinho.infrastructure.spectator;

import com.formdev.flatlaf.json.Json;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

final class MercadoPagoPix implements PixGateway {
    private final String token;
    private final URI payments;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    MercadoPagoPix(String token, URI payments) {
        this.token = token;
        this.payments = payments;
    }

    public Charge create(UUID reference, PixPayer payer) throws IOException {
        String body = """
                {"transaction_amount":5.00,"description":"Melhor momento - VarZinho Arena",
                 "payment_method_id":"pix","external_reference":"%s","payer":{"email":"%s",
                 "identification":{"type":"CPF","number":"%s"}}}
                """.formatted(reference, payer.email(), payer.cpf());
        var request = request(payments).header("X-Idempotency-Key", reference.toString())
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        Map<?, ?> payment = send(request);
        String id = String.valueOf(payment.get("id"));
        if (id.matches("[0-9]+") && payment.get("point_of_interaction") instanceof Map<?, ?> interaction
                && interaction.get("transaction_data") instanceof Map<?, ?> data
                && data.get("qr_code") instanceof String code && !code.isBlank()) {
            return new Charge(id, code, data.get("qr_code_base64") instanceof String image ? image : "");
        }
        throw new IOException("Pix code unavailable");
    }

    public boolean approved(UUID reference, String id) throws IOException {
        if (!id.matches("[0-9]+")) throw new IOException("invalid payment identifier");
        Map<?, ?> payment = send(request(URI.create(payments + "/" + id)).GET());
        if (java.util.Set.of("rejected", "cancelled", "refunded", "charged_back").contains(String.valueOf(payment.get("status")))
                || payment.get("transaction_amount_refunded") != null && !amountEquals(payment.get("transaction_amount_refunded"), 0))
            throw new PixGateway.ClosedPayment();
        return id.equals(String.valueOf(payment.get("id"))) && reference.toString().equals(payment.get("external_reference"))
                && "approved".equals(payment.get("status")) && "pix".equals(payment.get("payment_method_id"))
                && "BRL".equals(payment.get("currency_id")) && amountEquals(payment.get("transaction_amount"), 5)
                && amountEquals(payment.get("transaction_amount_refunded"), 0);
    }

    private static boolean amountEquals(Object value, int expected) {
        if (value == null) return false;
        try { return new BigDecimal(value.toString()).compareTo(BigDecimal.valueOf(expected)) == 0; }
        catch (NumberFormatException invalid) { return false; }
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).header("Authorization", "Bearer " + token);
    }

    private Map<?, ?> send(HttpRequest.Builder request) throws IOException {
        try {
            var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IOException("payment provider unavailable");
            if (Json.parse(new StringReader(response.body())) instanceof Map<?, ?> data) return data;
            throw new IOException("invalid payment response");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("payment request interrupted", interrupted);
        }
    }
}
