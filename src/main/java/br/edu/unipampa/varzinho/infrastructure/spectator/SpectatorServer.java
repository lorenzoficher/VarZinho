package br.edu.unipampa.varzinho.infrastructure.spectator;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class SpectatorServer implements AutoCloseable {
    private final HttpServer server;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());
    private final Semaphore exportSlot = new Semaphore(1);
    private volatile MatchBroadcast match;
    private final ClipOrders orders;
    private int court;
    private volatile String origin;
    private QuickTunnel tunnel;

    SpectatorServer(InetSocketAddress bind, String origin, MatchBroadcast match, ClipOrders orders, int court)
            throws IOException {
        this.match = match;
        this.orders = orders;
        this.court = court;
        server = HttpServer.create(bind, 32);
        this.origin = origin == null ? "http://127.0.0.1:" + server.getAddress().getPort() : origin;
        server.setExecutor(workers);
        server.createContext("/", this::handle);
        server.start();
    }

    public static SpectatorServer open(String address, String token, Path store) throws IOException {
        return open(address, token, store, false);
    }

    public static SpectatorServer open(String address, String token, Path store, boolean demo) throws IOException {
        String publicOrigin = null;
        if (address != null && !address.isBlank()) {
            URI url = URI.create(address);
            if (!"https".equals(url.getScheme()) || url.getHost() == null || url.getRawUserInfo() != null
                    || url.getRawQuery() != null || url.getFragment() != null || !Set.of("", "/").contains(url.getPath()))
                throw new IllegalArgumentException("expected a public HTTPS origin");
            publicOrigin = url.getScheme() + "://" + url.getRawAuthority();
        }
        ClipOrders orders = null;
        if (demo) orders = ClipOrders.demo(store.resolve("demo-orders"));
        else if (token != null && !token.isBlank()) orders = new ClipOrders(store.resolve("orders"), new MercadoPagoPix(token,
                URI.create("https://api.mercadopago.com/v1/payments")));
        return new SpectatorServer(new InetSocketAddress("127.0.0.1", publicOrigin == null ? 0 : 8787),
                publicOrigin, null, orders, 0);
    }

    public synchronized String shareDemo() throws IOException {
        if (orders == null || !orders.isDemo()) throw new IllegalStateException("temporary sharing requires demo mode");
        if (!origin.startsWith("https://")) {
            tunnel = QuickTunnel.start(port());
            origin = tunnel.origin();
        }
        return origin;
    }

    public synchronized String broadcast(String stream, int courtNumber, Path folder) throws IOException {
        if (match != null) throw new IllegalStateException("a match is already streaming");
        court = courtNumber;
        match = new MatchBroadcast(folder, List.of("-rw_timeout", "5000000", "-i", stream));
        return origin;
    }

    int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; media-src 'self' blob:; object-src 'none'; frame-ancestors 'none'");
            try { dispatch(ex); }
            catch (IllegalArgumentException invalid) { error(ex, 400, "Confira os dados informados."); }
            catch (NoSuchFileException missing) { error(ex, 404, "Gravação ou trecho indisponível."); }
            catch (IllegalStateException pending) { error(ex, 402, "Aguardando confirmação do Pix."); }
            catch (PixGateway.ClosedPayment closed) { error(ex, 409, "Pagamento encerrado. O acesso ao lance está bloqueado."); }
            catch (IOException unavailable) { error(ex, 503, "Serviço indisponível. Tente novamente em instantes."); }
        }
    }

    private void dispatch(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getRawPath();
        boolean post = "POST".equals(ex.getRequestMethod());
        if (post && (!origin.equals(ex.getRequestHeaders().getFirst("Origin"))
                || !"spectator".equals(ex.getRequestHeaders().getFirst("X-VarZinho")))) {
            error(ex, 403, "Origem inválida."); return;
        }
        if (!post && !"GET".equals(ex.getRequestMethod())) { error(ex, 405, "Método inválido."); return; }
        if (!post && Set.of("/", "/watch.js", "/arena.css").contains(path)) {
            String resource = path.equals("/") ? "/watch.html" : path;
            try (var in = getClass().getResourceAsStream("/spectator" + resource)) {
                if (in == null) throw new IOException("missing viewer");
                send(ex, 200, path.endsWith("js") ? "text/javascript" : path.endsWith("css") ? "text/css" : "text/html", in.readAllBytes());
            }
        } else if (!post && path.equals("/match") && match != null) {
            String sales = orders == null ? "watch" : orders.isDemo() ? "demo" : "buy";
            text(ex, 200, match.session() + " " + match.latest() + " " + court + " " + sales + " " + MatchBroadcast.SEGMENT_MS);
        } else if (orders == null && (path.startsWith("/reserve/") || path.startsWith("/purchase/"))) {
            error(ex, 503, "A arena ainda não habilitou as compras por Pix.");
        } else if (!post && match != null && path.startsWith("/live/" + match.session() + "/")) {
            file(ex, match.media(path.substring(path.lastIndexOf('/') + 1)), "video/mp4");
        } else if (post && match != null && path.equals("/reserve/" + match.session())) {
            String[] request = body(ex).split(" ", 2);
            if (request.length != 2) throw new IllegalArgumentException();
            UUID secret = uuid(request[0]);
            long time = Long.parseLong(request[1]);
            if (!exportSlot.tryAcquire()) { error(ex, 429, "Outro lance está sendo preparado. Tente novamente."); return; }
            try { text(ex, 201, orders.reserve(secret, target -> match.reserve(time, target)).toString()); }
            finally { exportSlot.release(); }
        } else if (path.startsWith("/purchase/")) {
            String[] parts = path.split("/");
            if (parts.length != 4) throw new IllegalArgumentException();
            UUID secret = uuid(parts[2]);
            ex.getResponseHeaders().set("X-VarZinho-Sales", orders.isDemo() ? "demo" : "buy");
            if (post && parts[3].equals("demo") && orders.isDemo()) {
                orders.confirmDemo(secret);
                text(ex, 200, "confirmed");
            } else if (post && parts[3].equals("pix") && !orders.isDemo()) {
                String[] payer = body(ex).split("\n", -1);
                if (payer.length != 2) throw new IllegalArgumentException();
                text(ex, 200, orders.checkout(secret, new PixPayer(payer[0], payer[1])));
            } else if (!post && parts[3].equals("pix")) {
                text(ex, 200, orders.pixCode(secret));
            } else if (!post && parts[3].equals("qr.png")) {
                send(ex, 200, "image/png", orders.qrImage(secret));
            } else if (!post && parts[3].equals("status")) {
                orders.paidClip(secret);
                text(ex, 200, "approved");
            } else if (!post && parts[3].equals("clip.mp4")) {
                file(ex, orders.paidClip(secret), "video/mp4");
            } else error(ex, 404, "Opção indisponível.");
        } else error(ex, 404, "Transmissão indisponível.");
    }

    private static UUID uuid(String value) {
        UUID parsed = UUID.fromString(value);
        if (!parsed.toString().equals(value)) throw new IllegalArgumentException();
        return parsed;
    }
    private static String body(HttpExchange ex) throws IOException {
        byte[] data = ex.getRequestBody().readNBytes(513);
        if (data.length > 512) throw new IllegalArgumentException();
        return new String(data, StandardCharsets.UTF_8);
    }
    private static void error(HttpExchange ex, int status, String message) throws IOException {
        if (ex.getResponseCode() == -1) text(ex, status, message);
    }
    private static void text(HttpExchange ex, int status, String message) throws IOException {
        send(ex, status, "text/plain", message.getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type.startsWith("text/") ? type + "; charset=utf-8" : type);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }
    private static void file(HttpExchange ex, Path file, String type) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            long size = Files.size(file);
            String range = ex.getRequestHeaders().getFirst("Range");
            ex.getResponseHeaders().set("Content-Type", type);
            ex.getResponseHeaders().set("Accept-Ranges", "bytes");
            if (range == null) {
                ex.sendResponseHeaders(200, size);
                in.transferTo(ex.getResponseBody());
                return;
            }
            var bounds = java.util.regex.Pattern.compile("bytes=([0-9]{0,18})-([0-9]{0,18})").matcher(range);
            long start = size, end = size - 1;
            if (bounds.matches()) {
                if (!bounds.group(1).isEmpty()) {
                    start = Long.parseLong(bounds.group(1));
                    if (!bounds.group(2).isEmpty()) end = Math.min(end, Long.parseLong(bounds.group(2)));
                } else if (!bounds.group(2).isEmpty()) {
                    start = Math.max(0, size - Long.parseLong(bounds.group(2)));
                }
            }
            if (start > end) {
                ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
                ex.sendResponseHeaders(416, -1);
                return;
            }
            long remaining = end - start + 1;
            ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + size);
            ex.sendResponseHeaders(206, remaining);
            in.skipNBytes(start);
            byte[] buffer = new byte[8192];
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(remaining, buffer.length));
                if (read < 0) throw new EOFException("video truncated");
                ex.getResponseBody().write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
    public synchronized void close() throws IOException {
        if (tunnel != null) tunnel.close();
        server.stop(0);
        workers.shutdownNow();
        if (match != null) match.close();
    }
}
