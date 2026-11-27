package br.edu.unipampa.varzinho.infrastructure.stream;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PhoneConnectionTest {
    private static final int BUFFER_SECONDS = 30;

    @TempDir
    Path segments;

    private final List<String> reasons = new ArrayList<>();

    @Test
    void reportsAnUnreachablePhoneBeforeAnyClipCanBeRecorded() throws Exception {
        assumeTrue(FfmpegExecutable.find().isPresent(), "FFmpeg unavailable");
        int port;
        try (var unused = new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            port = unused.getLocalPort();
        }
        var phone = PhoneConnection.start("http://127.0.0.1:" + port + "/video", segments, BUFFER_SECONDS);
        try {
            var failure = assertThrows(java.io.UncheckedIOException.class, phone::awaitFootage);
            assertTrue(failure.getCause() instanceof java.net.ConnectException);
            assertEquals("127.0.0.1:" + port, failure.getCause().getMessage());
        } finally {
            phone.disconnect();
        }
    }

    @Test
    void connectingAnHttpCameraWaitsForPlayableFootageAndFeedsTheRealCourt() throws Exception {
        assumeTrue(FfmpegExecutable.find().isPresent(), "FFmpeg unavailable");
        var jpeg = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(128, 96,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", jpeg);
        byte[] frame = jpeg.toByteArray();
        byte[] header = ("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: " + frame.length + "\r\n\r\n")
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var webcam = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        webcam.createContext("/video", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "multipart/x-mixed-replace; boundary=frame");
                exchange.sendResponseHeaders(200, 0);
                for (int i = 0; i < 500; i++) {
                    var out = exchange.getResponseBody();
                    out.write(header);
                    out.write(frame);
                    out.write(new byte[]{13, 10});
                    out.flush();
                    try { Thread.sleep(30); }
                    catch (InterruptedException stop) { Thread.currentThread().interrupt(); return; }
                }
            }
        });
        webcam.start();
        try {
            String address = "http://127.0.0.1:" + webcam.getAddress().getPort() + "/video";
            var phone = PhoneConnection.open(address, segments, BUFFER_SECONDS, reasons::add).orElseThrow();
            try {
                phone.awaitFootage();
                phone.camera().startRecording();
                phone.feed(() -> { throw new AssertionError("camera unexpectedly disconnected"); }).tick(Instant.now());
                assertTrue(phone.camera().getSecondsRecorded() > 0);
            } finally { phone.disconnect(); }
        } finally { webcam.stop(0); }
    }

    @Test
    void noAddressMeansNoPhoneAndNothingToReport() {
        assertTrue(PhoneConnection.open(null, segments, BUFFER_SECONDS, reasons::add).isEmpty());
        assertTrue(PhoneConnection.open("  ", segments, BUFFER_SECONDS, reasons::add).isEmpty());
        assertTrue(reasons.isEmpty());
    }

    @Test
    void badAddressMeansNoPhoneAndSaysWhy() {
        assertTrue(PhoneConnection.open("not-a-stream", segments, BUFFER_SECONDS, reasons::add)
                .isEmpty());

        assertEquals(1, reasons.size());
        assertTrue(reasons.get(0).contains("not-a-stream"), reasons.get(0));
    }

    @Test
    void phoneCaptureFailsAtTheCallerWhenThereIsNoSegmentToWrite() {
        StreamCamera phone = PhoneConnection.cameraFor("http://phone/video", BUFFER_SECONDS, new SegmentIndex(40));
        Court court = new Court(1);
        court.installCamera(phone);
        phone.startRecording();
        for (int second = 0; second < BUFFER_SECONDS; second++) {
            phone.record(new Frame(Instant.ofEpochSecond(second), second));
        }

        assertThrows(ClipAssemblyException.class, court::triggerCapture);
        assertThrows(ClipAssemblyException.class, court::triggerCapture);
    }

    @Test
    void capturesOutsideThePanelLeaveRealFilesWithoutCollectingAnyWrite() throws Exception {
        Process version = null;
        try {
            version = new ProcessBuilder(FfmpegExecutable.command(), "-version").redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            assumeTrue(version.waitFor(10, TimeUnit.SECONDS) && version.exitValue() == 0,
                    "ffmpeg is not available");
        } catch (java.io.IOException missing) {
            assumeTrue(false, "ffmpeg is not on PATH");
        } finally {
            if (version != null && version.isAlive()) version.destroyForcibly().waitFor();
        }
        Path segment = segments.resolve("second.ts");
        Process generate = new ProcessBuilder(FfmpegExecutable.command(), "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=64x64:rate=10", "-c:v", "libx264",
                "-pix_fmt", "yuv420p", "-f", "mpegts", segment.toString())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(generate.waitFor(20, TimeUnit.SECONDS));
            assertEquals(0, generate.exitValue());
        } finally {
            if (generate.isAlive()) generate.destroyForcibly().waitFor();
        }

        String classpath = Arrays.stream(System.getProperty("surefire.test.class.path",
                        System.getProperty("java.class.path")).split(Pattern.quote(File.pathSeparator)))
                .map(entry -> Path.of(entry).toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Path log = segments.resolve("capture.log");
        // Camera paths are relative to clips/, so a separate process keeps every file under @TempDir.
        Process capture = new ProcessBuilder(java, "-cp", classpath, DirectCapture.class.getName(), segment.toString())
                .directory(segments.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(capture.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, capture.exitValue(), () -> readLog(log));
        } finally {
            if (capture.isAlive()) capture.destroyForcibly().waitFor();
        }
    }

    private static String readLog(Path log) {
        try {
            return Files.readString(log);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("cannot read the capture process output", failure);
        }
    }

    public static final class DirectCapture {
        public static void main(String[] arguments) throws Exception {
            SegmentIndex index = new SegmentIndex(40);
            StreamCamera phone = PhoneConnection.cameraFor("http://phone/video", BUFFER_SECONDS, index);
            Court court = new Court(1);
            court.installCamera(phone);
            phone.startRecording();
            for (int second = 0; second < BUFFER_SECONDS; second++) {
                index.register(second, Path.of(arguments[0]));
                phone.record(new Frame(Instant.ofEpochSecond(second), second));
            }
            for (int capture = 0; capture < 2; capture++) {
                Highlight highlight = court.triggerCapture();
                if (Files.size(Path.of(highlight.getClip().getFilePath())) == 0) {
                    throw new IllegalStateException("capture returned an empty clip");
                }
            }
        }
    }
}
