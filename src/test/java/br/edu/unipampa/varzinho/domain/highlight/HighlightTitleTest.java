package br.edu.unipampa.varzinho.domain.highlight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HighlightTitleTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "Gol\nde placa", "Gol\rde placa", "Gol\tde placa", "Gol\u0000"})
    void rejectsANameThatIsMissingBlankOrNotOnOneLine(String text) {
        assertThrows(IllegalArgumentException.class, () -> new HighlightTitle(text));
    }

    @Test
    void stripsLeadingAndTrailingSpaces() {
        assertEquals("Gol de placa", new HighlightTitle("  Gol de placa  ").text());
    }

    @Test
    void twoTitlesWithTheSameTextAreEqual() {
        assertEquals(new HighlightTitle("Gol de placa"), new HighlightTitle(" Gol de placa"));
    }
}
