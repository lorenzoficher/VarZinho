package br.edu.unipampa.varzinho;

import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleDemoTest {
    @TempDir
    Path directory;

    @Test
    void demonstratesTheCompleteCaptureFlow() throws Exception {
        Path archive = directory.resolve("archive.csv");
        CsvHighlightRepository repository = new CsvHighlightRepository(archive);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            ConsoleDemo.run(repository);
        } finally {
            System.setOut(originalOutput);
        }

        String demonstration = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, new CsvHighlightRepository(archive).findByCourt(1).size());
        assertTrue(demonstration.contains("Registered:"));
        assertTrue(demonstration.contains("Reloaded highlight:"));
        assertTrue(demonstration.contains("filtered by court 1:"));
        assertTrue(demonstration.contains("Expected error:"));
    }
}
