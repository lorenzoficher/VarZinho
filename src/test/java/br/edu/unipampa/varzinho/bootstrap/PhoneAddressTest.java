package br.edu.unipampa.varzinho.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class PhoneAddressTest {
    @TempDir Path folder;

    @Test
    void startsWithTheLocalAddressUnlessTheOperatorProvidedAnOverride() throws Exception {
        Path file = folder.resolve("stream-url.txt");
        assertEquals(Optional.empty(), Launcher.phoneAddress(null, file));
        Files.writeString(file, "  ");
        assertEquals(Optional.empty(), Launcher.phoneAddress(null, file));
        Files.writeString(file, "http://phone.local:8080/video\n");
        assertEquals(Optional.of("http://phone.local:8080/video"), Launcher.phoneAddress(" ", file));
        assertEquals(Optional.of("http://other.local/video"), Launcher.phoneAddress("http://other.local/video", file));
    }
}
