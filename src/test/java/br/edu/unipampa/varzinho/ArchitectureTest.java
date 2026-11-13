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
    private static final Path TEST_SOURCES = Path.of("src/test/java/br/edu/unipampa/varzinho");
    // Only infrastructure talks to the database; the inner layers never see JDBC.
    private static final List<String> DATABASE_ACCESS = List.of("java.sql", "javax.sql", "org.postgresql");
    private static final Pattern IMPORT = Pattern.compile(
            "(?m)^\\h*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    @Test
    void domainDoesNotImportOuterLayersOrDesktopLibraries() throws IOException {
        assertNoForbiddenImports("domain", List.of(PROJECT_PACKAGE + ".application",
                PROJECT_PACKAGE + ".infrastructure", PROJECT_PACKAGE + ".ui",
                PROJECT_PACKAGE + ".bootstrap", "javax.swing", "java.awt"));
    }

    @Test
    void domainDoesNotReachTheDatabase() throws IOException {
        assertNoForbiddenImports("domain", DATABASE_ACCESS);
    }

    @Test
    void applicationDoesNotImportAdaptersOrDesktopLibraries() throws IOException {
        assertNoForbiddenImports("application", List.of(PROJECT_PACKAGE + ".infrastructure",
                PROJECT_PACKAGE + ".ui", PROJECT_PACKAGE + ".bootstrap", "javax.swing", "java.awt"));
    }

    @Test
    void applicationDoesNotReachTheDatabase() throws IOException {
        assertNoForbiddenImports("application", DATABASE_ACCESS);
    }

    @Test
    void infrastructureDoesNotImportUiOrBootstrap() throws IOException {
        assertNoForbiddenImports("infrastructure", List.of(PROJECT_PACKAGE + ".ui",
                PROJECT_PACKAGE + ".bootstrap"));
    }

    @Test
    void uiDoesNotImportTheDomainOrAdapters() throws IOException {
        assertNoForbiddenImports("ui", List.of(PROJECT_PACKAGE + ".domain",
                PROJECT_PACKAGE + ".infrastructure"));
    }

    @Test
    void exceptionsDoNotImportOtherProjectPackages() throws IOException {
        assertNoForbiddenImports("exception", List.of(PROJECT_PACKAGE));
    }

    @Test
    void testsDoNotImportJUnit4() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(TEST_SOURCES)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java"))
                    .sorted().toList()) {
                String code = Files.readString(source).replaceAll("(?s)/\\*.*?\\*/", "");
                Matcher imports = IMPORT.matcher(code);
                while (imports.find()) {
                    String imported = imports.group(1);
                    if (belongsTo(imported, "org.junit")
                            && !belongsTo(imported, "org.junit.jupiter")
                            && !belongsTo(imported, "org.junit.platform")) {
                        violations.add(TEST_SOURCES.relativize(source) + " imports " + imported);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "JUnit 4 imports are forbidden:\n"
                + String.join("\n", violations));
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
