package br.edu.unipampa.varzinho.infrastructure.spectator;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PublicMatchTest {
    @TempDir Path folder;
    private final HttpClient browser = HttpClient.newHttpClient();
    private static final String ORIGIN = "https://arena.example";
    private URI base;

    @Test
    void demoPurchaseRequiresConfirmationAndCannotUnlockARealOrder() throws Exception {
        UUID secret = UUID.randomUUID();
        var demo = ClipOrders.demo(folder.resolve("demo-orders"));
        demo.reserve(secret, file -> Files.writeString(file, "sample footage"));
        try (var app = new SpectatorServer(new InetSocketAddress("127.0.0.1", 0), ORIGIN, null, demo, 3)) {
            base = URI.create("http://127.0.0.1:" + app.port());
            String order = "/purchase/" + secret;
            assertEquals(402, get(order + "/clip.mp4").statusCode());
            assertEquals(403, post(order + "/demo", "", "https://other.example").statusCode());
            assertEquals(200, post(order + "/demo", "", ORIGIN).statusCode());
            assertEquals("sample footage", get(order + "/clip.mp4").body());
        }
        try (var app = SpectatorServer.open(null, "unused", folder)) {
            base = URI.create("http://127.0.0.1:" + app.port());
            assertEquals(404, post("/purchase/" + secret + "/demo", "", base.toString()).statusCode());
            assertEquals(404, get("/purchase/" + secret + "/clip.mp4").statusCode());
        }
    }

    @Test
    void localPreviewCanOpenBeforePixIsConfigured() throws Exception {
        try (var app = SpectatorServer.open(null, null, folder)) {
            base = URI.create("http://127.0.0.1:" + app.port());
            assertEquals(200, get("/").statusCode());
            var response = post("/reserve/match", "test", base.toString());
            assertEquals(503, response.statusCode());
            assertTrue(response.body().contains("Pix"));
        }
    }

    @Test
    void delayedSpectatorCanBuyOnlyTheReservedThirtySeconds() throws Exception {
        try { new ProcessBuilder("ffmpeg", "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor(); }
        catch (java.io.IOException absent) { Assumptions.abort("FFmpeg unavailable"); }
        var reference = new AtomicReference<String>();
        var paymentStatus = new AtomicReference<>("pending");
        var amount = new AtomicReference<>("5.00");
        var refund = new AtomicReference<>("0");
        var provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/payments", ex -> {
            try (ex) {
                String json;
                if ("POST".equals(ex.getRequestMethod())) {
                    reference.set(ex.getRequestHeaders().getFirst("X-Idempotency-Key"));
                    json = "{\"id\":123,\"point_of_interaction\":{\"transaction_data\":{\"qr_code\":\"pix-copia\",\"qr_code_base64\":\"iVBORw0KGgo=\"}}}";
                } else {
                    json = """
                        {"id":123,"external_reference":"%s","status":"%s","transaction_amount":%s,
                         "currency_id":"BRL","payment_method_id":"pix","transaction_amount_refunded":%s}
                        """.formatted(reference.get(), paymentStatus.get(), amount.get(), refund.get());
                }
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
            }
        });
        provider.start();
        try {
            var gateway = new MercadoPagoPix("test", URI.create("http://127.0.0.1:" + provider.getAddress().getPort() + "/payments"));
            var orders = new ClipOrders(folder.resolve("orders"), gateway);
            var match = new MatchBroadcast(folder, List.of("-readrate", "5", "-f", "lavfi", "-i",
                    "color=red:s=96x54:r=25,drawbox=color=blue:t=fill:enable='gte(t,9.5)'"));
            try (var app = new SpectatorServer(new InetSocketAddress("127.0.0.1", 0), ORIGIN, match, orders, 3)) {
                base = URI.create("http://127.0.0.1:" + app.port());
                assertEquals(200, get("/").statusCode());
                long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
                while (System.nanoTime() < deadline) {
                    var state = get("/match");
                    if (state.statusCode() == 200 && Integer.parseInt(state.body().split(" ")[1]) >= 40000 / MatchBroadcast.SEGMENT_MS) break;
                    Thread.sleep(200);
                }
                String reserve = "/reserve/" + match.session();
                UUID secret = UUID.randomUUID();
                assertEquals(400, post(reserve, secret + " 500", ORIGIN).statusCode());
                assertEquals(403, post(reserve, secret + " 39000", "https://other.example").statusCode());
                assertEquals(201, post(reserve, secret + " 39000", ORIGIN).statusCode());
                String order = "/purchase/" + secret;
                assertEquals(402, get(order + "/clip.mp4").statusCode());
                assertEquals(400, post(order + "/pix", "buyer@example.com\n00000000000", ORIGIN).statusCode());
                assertEquals("pix-copia", post(order + "/pix", "buyer@example.com\n12345678909", ORIGIN).body());
                assertEquals(200, get(order + "/qr.png").statusCode());
                assertEquals(402, get(order + "/status").statusCode());
                paymentStatus.set("approved");
                amount.set("0.50");
                assertEquals(402, get(order + "/status").statusCode());
                amount.set("5.00");
                assertEquals(200, get(order + "/status").statusCode());
                var part = browser.send(HttpRequest.newBuilder(base.resolve(order + "/clip.mp4"))
                        .header("Range", "bytes=0-31").build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(206, part.statusCode());
                assertEquals(32, part.body().length);
                var video = browser.send(HttpRequest.newBuilder(base.resolve(order + "/clip.mp4")).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, video.statusCode());
                assertEquals("video/mp4", video.headers().firstValue("Content-Type").orElseThrow());
                Path file = Files.write(folder.resolve("download.mp4"), video.body());
                assertEquals(30, Double.parseDouble(probe(file)), 0.06);
                byte[] first = pixel(file, "0.04");
                byte[] next = pixel(file, "0.8");
                assertTrue(Byte.toUnsignedInt(first[0]) > 180, "starts during the red half-second before the transition");
                assertTrue(Byte.toUnsignedInt(next[2]) > 180, "turns blue at the expected playback time");
                refund.set("5");
                assertEquals(409, get(order + "/clip.mp4").statusCode());
                assertEquals(404, get("/purchase/" + UUID.randomUUID() + "/clip.mp4").statusCode());
            }
        } finally { provider.stop(0); }
    }

    private static String probe(Path file) throws Exception {
        var p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "csv=p=0", file.toString()).start();
        String data = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor());
        return data.trim();
    }
    private static byte[] pixel(Path file, String at) throws Exception {
        var p = new ProcessBuilder("ffmpeg", "-v", "error", "-ss", at, "-i", file.toString(), "-vframes", "1",
                "-vf", "scale=1:1", "-pix_fmt", "rgb24", "-f", "rawvideo", "pipe:1").start();
        byte[] data = p.getInputStream().readAllBytes();
        assertEquals(0, p.waitFor());
        return data;
    }
    private HttpResponse<String> get(String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(base.resolve(path)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(String path, String body, String origin) throws Exception {
        var req = HttpRequest.newBuilder(base.resolve(path)).header("Origin", origin).header("X-VarZinho","spectator")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return browser.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
