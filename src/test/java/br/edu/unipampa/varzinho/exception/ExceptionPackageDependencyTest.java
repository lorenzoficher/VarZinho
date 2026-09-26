package br.edu.unipampa.varzinho.exception;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ExceptionPackageDependencyTest {

    private static final Path EXCEPTIONS =
            Path.of("src/main/java/br/edu/unipampa/varzinho/exception");

    @Test
    void noExceptionImportsFromTheDomain() throws IOException {
        List<Path> offenders;
        try (Stream<Path> sources = Files.list(EXCEPTIONS)) {
            offenders = sources.filter(ExceptionPackageDependencyTest::importsTheDomain)
                    .collect(Collectors.toList());
        }

        assertTrue(offenders.isEmpty(), "exception/ must not depend on domain/: " + offenders);
    }

    private static boolean importsTheDomain(Path source) {
        try {
            return Files.readString(source).contains("import br.edu.unipampa.varzinho.domain");
        } catch (IOException cause) {
            throw new IllegalStateException("cannot read " + source, cause);
        }
    }
}
