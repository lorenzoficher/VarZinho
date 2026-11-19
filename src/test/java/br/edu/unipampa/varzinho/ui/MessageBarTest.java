package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageBarTest {
    @Test
    void combinesStartupWarningsWithAnArchiveWarningUntilTheNextAction() {
        MessageBar bar = new MessageBar();
        bar.show("Acervo danificado.");
        bar.showStartupWarnings(java.util.List.of("Academia indisponível.", "Celular desconectado."));
        assertEquals("Academia indisponível. Celular desconectado. Acervo danificado.", bar.message());
        bar.show("Destaque salvo.");
        assertEquals("Destaque salvo.", bar.message());
    }

    @Test
    void keepsTheLatestMessageUntilAnotherOneArrives() {
        MessageBar bar = new MessageBar();

        bar.show("Destaque salvo.");
        assertEquals("Destaque salvo.", bar.message());

        bar.show("Selecione uma câmera.");
        assertEquals("Selecione uma câmera.", bar.message());
    }
}
