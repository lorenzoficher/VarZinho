package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageBarTest {
    @Test
    void keepsTheLatestMessageUntilAnotherOneArrives() {
        MessageBar bar = new MessageBar();

        bar.show("Destaque salvo.");
        assertEquals("Destaque salvo.", bar.message());

        bar.show("Selecione uma câmera.");
        assertEquals("Selecione uma câmera.", bar.message());
    }
}
