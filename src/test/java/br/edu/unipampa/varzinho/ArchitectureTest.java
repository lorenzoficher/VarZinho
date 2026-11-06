package br.edu.unipampa.varzinho;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The dependency rule of Clean Architecture, checked on the sources: every arrow points
 * inward, so an inner package never imports an outer one.
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java/br/edu/unipampa/varzinho");
    private static final String ROOT = "br.edu.unipampa.varzinho.";

    @Test
    void domainImportsNoOuterLayerAndNoWindowToolkit() {
        assertImportsNone("domain", "application", "infrastructure", "ui", "bootstrap", "javax.swing", "java.awt");
    }

    @Test
    void applicationImportsNeitherAdaptersNorTheWindow() {
        assertImportsNone("application", "infrastructure", "ui", "bootstrap", "javax.swing", "java.awt");
    }

    @Test
    void infrastructureImportsNeitherTheWindowNorTheCompositionRoot() {
        assertImportsNone("infrastructure", "ui", "bootstrap");
    }

    @Test
    void domainExceptionsImportNothingElseFromTheProject() {
        assertImportsNone("exception", "domain", "application", "infrastructure", "ui", "bootstrap");
    }

    private static void assertImportsNone(String layer, String... forbidden) {
        List<String> offenders = sourcesOf(layer)
                .flatMap(source -> importsOf(source).stream()
                        .filter(imported -> startsWithAny(imported, forbidden))
                        .map(imported -> SOURCES.relativize(source) + " imports " + imported))
                .collect(Collectors.toList());

        assertTrue(offenders.isEmpty(), layer + "/ breaks the dependency rule: " + offenders);
    }

    private static Stream<Path> sourcesOf(String layer) {
        try (Stream<Path> files = Files.walk(SOURCES.resolve(layer))) {
            return files.filter(file -> file.toString().endsWith(".java")).collect(Collectors.toList()).stream();
        } catch (IOException cause) {
            throw new UncheckedIOException("cannot list " + layer, cause);
        }
    }

    private static List<String> importsOf(Path source) {
        try {
            return Files.readAllLines(source).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line.substring("import ".length()).replace("static ", ""))
                    .collect(Collectors.toList());
        } catch (IOException cause) {
            throw new UncheckedIOException("cannot read " + source, cause);
        }
    }

    // A project layer is named without its root; a library package by its full name.
    private static boolean startsWithAny(String imported, String... forbidden) {
        for (String target : forbidden) {
            String prefix = target.contains(".") ? target + "." : ROOT + target + ".";
            if (imported.startsWith(prefix)) return true;
        }
        return false;
    }
}
