package br.edu.unipampa.varzinho.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ThemeVocabularyTest {

    private static final Path UI_SOURCES = Path.of("src/main/java/br/edu/unipampa/varzinho/ui");
    // The two files that are the vocabulary: every other panel borrows from them.
    private static final Set<String> VOCABULARY = Set.of("VarZinhoTheme.java", "VarZinhoIcons.java");
    private static final Pattern LITERAL_COLOUR = Pattern.compile(
            "new\\s+(?:java\\.awt\\.)?Color\\s*\\(|\\bColor\\.(?:decode\\b|[A-Z_]+\\b)|\"[^\"\\n]*#[0-9A-Fa-f]{6}");
    private static final Pattern LITERAL_FONT = Pattern.compile(
            "new\\s+(?:java\\.awt\\.)?Font\\s*\\(|\\.deriveFont\\s*\\(");

    @Test
    void noPanelBuildsItsOwnColour() throws IOException {
        List<String> violations = findInPanels(LITERAL_COLOUR);
        assertTrue(violations.isEmpty(), () -> "Colours must come from VarZinhoTheme:\n"
                + String.join("\n", violations));
    }

    @Test
    void noPanelBuildsItsOwnFont() throws IOException {
        List<String> violations = findInPanels(LITERAL_FONT);
        assertTrue(violations.isEmpty(), () -> "Fonts must come from VarZinhoTheme:\n"
                + String.join("\n", violations));
    }

    private static List<String> findInPanels(Pattern forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> sources = Files.list(UI_SOURCES)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !VOCABULARY.contains(path.getFileName().toString()))
                    .sorted().toList()) {
                List<String> lines = Files.readAllLines(source);
                for (int index = 0; index < lines.size(); index++) {
                    String code = lines.get(index).replaceAll("//.*$", "");
                    Matcher match = forbidden.matcher(code);
                    if (match.find()) {
                        violations.add(source.getFileName() + ":" + (index + 1) + "  " + code.strip());
                    }
                }
            }
        }
        return violations;
    }
}
