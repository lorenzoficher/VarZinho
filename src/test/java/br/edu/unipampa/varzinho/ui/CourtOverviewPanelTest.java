package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CourtOverviewPanelTest {
    @Test
    void emptyGymAndCourtWithoutCamerasHaveDifferentMessages() throws Exception {
        var gym = new ManageGym(SampleGym.build(), new InMemoryGymRepository());
        gym.addCourt(3);
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> selections = new ArrayList<>();
            var cards = new CourtOverviewPanel(selections::add, n -> null, n -> "Quadra " + n, null);
            cards.refresh(List.of(), 0);
            assertTrue(labels(cards).anyMatch("Nenhuma quadra cadastrada."::equals));
            cards.refresh(gym.courts(), 1);
            assertTrue(labels(cards).anyMatch("Selecionada"::equals));
            assertTrue(labels(cards).anyMatch("Sem câmeras"::equals));
            assertTrue(labels(cards).anyMatch(text -> text.contains("de 2 gravando")));
            assertTrue(selections.isEmpty());
            Component button = descendants(cards).filter(JButton.class::isInstance)
                    .filter(b -> b.getAccessibleContext().getAccessibleName().contains("Selecionada")).findFirst().orElseThrow();
            ((JButton) button).doClick(0);
            assertEquals(List.of(1), selections);
            cards.refresh(gym.courts(), 1);
            assertEquals(List.of(1), selections);
        });
    }
    private static Stream<String> labels(Container panel) {
        return descendants(panel).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .map(JLabel::getText).filter(java.util.Objects::nonNull);
    }
    private static Stream<Component> descendants(Container panel) {
        return Arrays.stream(panel.getComponents()).flatMap(c -> c instanceof Container nested
                ? Stream.concat(Stream.of(c), descendants(nested)) : Stream.of(c));
    }
}
