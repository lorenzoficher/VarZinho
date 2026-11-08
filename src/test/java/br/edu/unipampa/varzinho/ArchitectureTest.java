package br.edu.unipampa.varzinho;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String PROJECT_PACKAGE = "br.edu.unipampa.varzinho";
    private static final Path SOURCES = Path.of("src/main/java/br/edu/unipampa/varzinho");
    private static final Pattern IMPORT = Pattern.compile(
            "(?m)^\\h*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    @Test
    void domainDoesNotImportOuterLayersOrDesktopLibraries() throws IOException {
        assertNoForbiddenImports("domain", List.of(PROJECT_PACKAGE + ".application",
                PROJECT_PACKAGE + ".infrastructure", PROJECT_PACKAGE + ".ui",
                PROJECT_PACKAGE + ".bootstrap", "javax.swing", "java.awt"));
    }

    @Test
    void applicationDoesNotImportAdaptersOrDesktopLibraries() throws IOException {
        assertNoForbiddenImports("application", List.of(PROJECT_PACKAGE + ".infrastructure",
                PROJECT_PACKAGE + ".ui", PROJECT_PACKAGE + ".bootstrap", "javax.swing", "java.awt"));
    }

    @Test
    void infrastructureDoesNotImportUiOrBootstrap() throws IOException {
        assertNoForbiddenImports("infrastructure", List.of(PROJECT_PACKAGE + ".ui",
                PROJECT_PACKAGE + ".bootstrap"));
    }

    @Test
    void exceptionsDoNotImportOtherProjectPackages() throws IOException {
        assertNoForbiddenImports("exception", List.of(PROJECT_PACKAGE));
    }

    private static void assertNoForbiddenImports(String layer, List<String> forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(SOURCES.resolve(layer))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java"))
                    .sorted().toList()) {
                String code = Files.readString(source).replaceAll("(?s)/\\*.*?\\*/", "");
                Matcher imports = IMPORT.matcher(code);
                while (imports.find()) {
                    String imported = imports.group(1);
                    if (belongsTo(imported, PROJECT_PACKAGE + "." + layer)) {
                        continue;
                    }
                    if (forbidden.stream().anyMatch(prefix -> belongsTo(imported, prefix))) {
                        violations.add(SOURCES.relativize(source) + " imports " + imported);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "Forbidden layer dependencies:\n"
                + String.join("\n", violations));
    }

    private static boolean belongsTo(String imported, String prefix) {
        return imported.equals(prefix) || imported.startsWith(prefix + ".");
    }
}
