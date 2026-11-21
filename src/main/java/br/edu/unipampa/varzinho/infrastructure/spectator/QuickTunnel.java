package br.edu.unipampa.varzinho.infrastructure.spectator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/** Exposes only the loopback spectator server for the lifetime of a demo. */
record QuickTunnel(Process process, String origin) implements AutoCloseable {
    private static final Pattern URL = Pattern.compile("https://[a-z0-9-]+\\.trycloudflare\\.com\\b");

    static QuickTunnel start(int port) throws IOException {
        Process process = new ProcessBuilder(executable(), "tunnel", "--no-autoupdate", "--url",
                "http://127.0.0.1:" + port).redirectErrorStream(true).start();
        var ready = new CompletableFuture<String>();
        var reader = new Thread(() -> {
            try (var log = process.inputReader()) {
                String origin = null;
                boolean connected = false;
                for (String line; (line = log.readLine()) != null;) {
                    var url = URL.matcher(line);
                    if (url.find()) origin = url.group();
                    if (line.contains("Registered tunnel connection")) connected = true;
                    if (origin != null && connected) ready.complete(origin);
                }
                ready.completeExceptionally(new IOException("tunnel stopped"));
            } catch (IOException failure) {
                ready.completeExceptionally(failure);
            }
        }, "spectator-tunnel");
        reader.setDaemon(true);
        reader.start();
        try {
            return new QuickTunnel(process, ready.get(45, TimeUnit.SECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("sharing interrupted", interrupted);
        } catch (ExecutionException | TimeoutException unavailable) {
            process.destroyForcibly();
            throw new IOException("temporary tunnel unavailable", unavailable);
        }
    }

    private static String executable() {
        for (String variable : List.of("ProgramFiles(x86)", "ProgramFiles")) {
            String root = System.getenv(variable);
            if (root == null) continue;
            Path exe = Path.of(root, "cloudflared", "cloudflared.exe");
            if (Files.isRegularFile(exe)) return exe.toString();
        }
        return "cloudflared";
    }

    public void close() { process.destroyForcibly(); }
}
