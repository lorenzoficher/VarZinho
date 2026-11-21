package br.edu.unipampa.varzinho.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PhoneAddressTest {
    @TempDir Path folder;

    @Test
    void startsWithTheLocalAddressUnlessTheOperatorProvidedAnOverride() throws Exception {
        Path file = folder.resolve("stream-url.txt");
        assertNull(Launcher.phoneAddress(null, file));
        Files.writeString(file, "http://phone.local:8080/video\n");
        assertEquals("http://phone.local:8080/video", Launcher.phoneAddress(" ", file));
        assertEquals("http://other.local/video", Launcher.phoneAddress("http://other.local/video", file));
    }
}
