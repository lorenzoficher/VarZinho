package br.edu.unipampa.varzinho.infrastructure.spectator;

import br.edu.unipampa.varzinho.infrastructure.stream.FfmpegExecutable;
import br.edu.unipampa.varzinho.infrastructure.stream.PhoneConnection;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DroidCam serves its video to one client and answers everyone else with its web page.
 * The recorder and the spectator broadcast must both work from that single connection.
 */
class SingleClientPhoneTest {
    private static final int BUFFER_SECONDS = 30;

    @TempDir Path folder;

    @Test
    void broadcastPlaysWhileThePhoneServesItsVideoToOneClientOnly() throws Exception {
        assumeTrue(FfmpegExecutable.find().isPresent(), "FFmpeg unavailable");
        AtomicInteger videoClients = new AtomicInteger();
        HttpServer phoneApp = singleClientPhone(videoClients);
        phoneApp.start();
        String address = "http://127.0.0.1:" + phoneApp.getAddress().getPort() + "/video";
        PhoneConnection phone = PhoneConnection.start(address, folder.resolve("buffer"), BUFFER_SECONDS);
        try (SpectatorServer spectators = SpectatorServer.open(null, null, folder.resolve("spectator"))) {
            phone.awaitFootage();

            spectators.broadcast(phone.liveRelay(), 1, folder.resolve("spectator/live"));

            assertTrue(matchIsLive(spectators.port()), "the spectator page never received footage");
            assertEquals(1, videoClients.get(), "the phone was asked for its video more than once");
        } finally {
            phone.disconnect();
            phoneApp.stop(0);
        }
    }

    private static boolean matchIsLive(int port) throws Exception {
        HttpClient browser = HttpClient.newHttpClient();
        HttpRequest match = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/match")).build();
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (browser.send(match, HttpResponse.BodyHandlers.ofString()).statusCode() == 200) return true;
            Thread.sleep(200);
        }
        return false;
    }

    private static HttpServer singleClientPhone(AtomicInteger videoClients) throws IOException {
        byte[] frame = jpeg();
        byte[] partHeader = ("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: " + frame.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        byte[] busyPage = "<!doctype html><title>DroidCam</title>".getBytes(StandardCharsets.US_ASCII);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/video", exchange -> {
            try (exchange) {
                if (videoClients.incrementAndGet() > 1) {
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, busyPage.length);
                    exchange.getResponseBody().write(busyPage);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", "multipart/x-mixed-replace; boundary=frame");
                exchange.sendResponseHeaders(200, 0);
                OutputStream out = exchange.getResponseBody();
                for (int i = 0; i < 1500; i++) {
                    out.write(partHeader);
                    out.write(frame);
                    out.write(new byte[]{13, 10});
                    out.flush();
                    Thread.sleep(30);
                }
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
            } catch (IOException clientLeft) {
                // The recorder closing its connection ends this stream; nothing else to do.
            }
        });
        return server;
    }

    private static byte[] jpeg() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(128, 96, BufferedImage.TYPE_INT_RGB), "jpg", bytes);
        return bytes.toByteArray();
    }
}
