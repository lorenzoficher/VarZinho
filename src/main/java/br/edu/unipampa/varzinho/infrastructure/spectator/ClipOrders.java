package br.edu.unipampa.varzinho.infrastructure.spectator;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

final class ClipOrders {
    @FunctionalInterface interface Capture { void write(Path clip) throws IOException; }
    private final Path root;
    private final PixGateway gateway;

    ClipOrders(Path root, PixGateway gateway) throws IOException {
        this.root = Files.createDirectories(root);
        this.gateway = gateway;
    }

    static ClipOrders demo(Path root) throws IOException {
        return new ClipOrders(root, null);
    }

    boolean isDemo() { return gateway == null; }

    synchronized void confirmDemo(UUID secret) throws IOException {
        if (!isDemo()) throw new IllegalStateException("not a demo order");
        Path order = root.resolve(secret.toString());
        Properties state = read(order);
        state.setProperty("demoConfirmed", "true");
        write(order, state);
    }

    UUID reserve(Capture capture) throws IOException {
        return reserve(UUID.randomUUID(), capture);
    }

    synchronized UUID reserve(UUID secret, Capture capture) throws IOException {
        Path order = root.resolve(secret.toString());
        if (Files.exists(order.resolve("order.properties"))) return secret;
        int outstanding = 0;
        try (var entries = Files.list(root)) {
            for (Path entry : entries.toList()) {
                Path file = entry.resolve("order.properties");
                if (!Files.isRegularFile(file)) continue;
                Properties state = read(entry);
                if (!state.containsKey("checkoutStarted") && Files.getLastModifiedTime(file).toInstant()
                        .isBefore(Instant.now().minus(Duration.ofHours(1)))) {
                    removeReservation(entry);
                } else if (isDemo() || !state.containsKey("delivered")) outstanding++;
            }
        }
        if (outstanding >= 200 || Files.getFileStore(root).getUsableSpace() < 256L * 1024 * 1024)
            throw new IOException("reservation storage full");
        Files.createDirectory(order);
        try {
            capture.write(order.resolve("clip.mp4"));
            if (Files.size(order.resolve("clip.mp4")) == 0) throw new IOException("empty clip");
            Properties state = new Properties();
            state.setProperty("reference", UUID.randomUUID().toString());
            write(order, state);
            return secret;
        } catch (IOException | RuntimeException failure) {
            removeReservation(order);
            throw failure;
        }
    }

    private static void removeReservation(Path order) throws IOException {
        Files.deleteIfExists(order.resolve("order.tmp"));
        Files.deleteIfExists(order.resolve("clip.mp4"));
        Files.deleteIfExists(order.resolve("order.properties"));
        Files.delete(order);
    }

    synchronized String checkout(UUID secret, PixPayer payer) throws IOException {
        Path order = root.resolve(secret.toString());
        Properties state = read(order);
        if (state.containsKey("code")) return state.getProperty("code");
        if (!state.containsKey("checkoutStarted")) {
            state.setProperty("checkoutStarted", "true");
            write(order, state);
        }
        PixGateway.Charge charge = gateway.create(UUID.fromString(state.getProperty("reference")), payer);
        state.setProperty("payment", charge.id());
        state.setProperty("code", charge.code());
        state.setProperty("qr", charge.qrImage());
        write(order, state);
        return charge.code();
    }

    byte[] qrImage(UUID secret) throws IOException {
        String image = read(root.resolve(secret.toString())).getProperty("qr", "");
        if (image.isEmpty()) throw new NoSuchFileException("no QR image");
        return java.util.Base64.getDecoder().decode(image);
    }

    String pixCode(UUID secret) throws IOException {
        return read(root.resolve(secret.toString())).getProperty("code", "");
    }

    Path paidClip(UUID secret) throws IOException {
        Path order = root.resolve(secret.toString());
        Properties state = read(order);
        if (isDemo()) {
            if (!state.containsKey("demoConfirmed")) throw new IllegalStateException("demo pending");
            return order.resolve("clip.mp4");
        }
        String payment = state.getProperty("payment");
        if (payment == null || !gateway.approved(UUID.fromString(state.getProperty("reference")), payment))
            throw new IllegalStateException("payment pending");
        if (!state.containsKey("delivered")) markDelivered(order);
        return order.resolve("clip.mp4");
    }

    private synchronized void markDelivered(Path order) throws IOException {
        Properties current = read(order);
        current.setProperty("delivered", "true");
        write(order, current);
    }

    private static Properties read(Path order) throws IOException {
        Properties state = new Properties();
        try (var in = Files.newInputStream(order.resolve("order.properties"))) { state.load(in); }
        return state;
    }

    private static void write(Path order, Properties state) throws IOException {
        Path temp = order.resolve("order.tmp");
        try (var out = Files.newOutputStream(temp)) { state.store(out, null); }
        Files.move(temp, order.resolve("order.properties"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
