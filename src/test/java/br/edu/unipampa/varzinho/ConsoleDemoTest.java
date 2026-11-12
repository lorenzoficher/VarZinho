package br.edu.unipampa.varzinho;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleDemoTest {
    @TempDir
    Path directory;

    @Test
    void demonstratesTheCompleteCaptureFlow() throws Exception {
        Path archive = directory.resolve("archive.csv");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            ConsoleDemo.run(archive);
        } finally {
            System.setOut(originalOutput);
        }

        String demonstration = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, new CsvHighlightRepository(archive).findByCourt(1).size());
        assertFalse(demonstration.contains("Registered:"));
        assertTrue(demonstration.contains("Reloaded highlight:"));
        assertTrue(demonstration.contains("Existing archive for court 1 ("));
        assertTrue(demonstration.contains("Expected error:"));
    }
}
