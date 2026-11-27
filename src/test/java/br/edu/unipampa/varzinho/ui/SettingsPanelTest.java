package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsPanelTest {
    private final CourtPanel courts = new CourtPanel(
            new ManageGym(SampleGym.build(), new InMemoryGymRepository()), 30, message -> { });
    private final List<String> messages = new ArrayList<>();

    @Test
    void enterInTheCourtNameSavesIt() {
        SettingsPanel settings = new SettingsPanel(courts, messages::add);
        JTextField name = Arrays.stream(settings.getComponents()).filter(JTextField.class::isInstance)
                .map(JTextField.class::cast).findFirst().orElseThrow();

        name.setText("Quadra Azul");
        name.postActionEvent();
        SwingWork.awaitRepositoryWork();

        assertEquals("Quadra Azul", courts.nameOf(courts.selectedCourtNumber()));
        assertEquals(List.of("Nome de exibição atualizado."), messages);
    }
}
