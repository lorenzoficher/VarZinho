package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CourtCardsPanelTest {
    @Test
    void showsRecordingCountsAndDistinguishesEmptyCourtsFromAnEmptyGym() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(2));
        gym.addCourt(new Court(4));
        FixedCamera active = camera("A");
        active.startRecording();
        FixedCamera maintenance = camera("M");
        maintenance.sendToMaintenance();
        gym.installCamera(2, active);
        gym.installCamera(2, camera("B"));
        gym.installCamera(2, maintenance);
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());

        SwingUtilities.invokeAndWait(() -> {
            CourtCardsPanel panel = new CourtCardsPanel(n -> fail("Refresh must be silent"));
            panel.refresh(management.courts());
            assertEquals(2, controls(panel, JToggleButton.class).size());
            assertTrue(card(panel, 2).getText().contains("1 de 3 câmeras gravando"));
            assertTrue(card(panel, 4).getText().contains("Sem câmeras instaladas"));
            panel.refresh(List.of());
            assertTrue(controls(panel, JToggleButton.class).isEmpty());
            assertTrue(controls(panel, JLabel.class).stream().anyMatch(label ->
                    label.isVisible() && "Nenhuma quadra cadastrada.".equals(label.getText())));
        });
    }

    @Test
    void preservesTheSelectedControlOnRefreshWithoutNotifyingTheHost() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(2));
        FixedCamera active = camera("A");
        gym.installCamera(2, active);
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());
        List<CourtView> before = management.courts();
        active.startRecording();
        List<CourtView> after = management.courts();
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> events = new ArrayList<>();
            CourtCardsPanel panel = new CourtCardsPanel(events::add);
            panel.refresh(before);
            panel.selectCourt(1);
            assertTrue(card(panel, 1).isSelected());
            assertTrue(events.isEmpty());
            JToggleButton second = card(panel, 2);
            second.setSize(second.getPreferredSize());
            for (int event : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                second.dispatchEvent(new MouseEvent(second, event, System.currentTimeMillis(),
                        0, 8, 8, 1, false, MouseEvent.BUTTON1));
            }
            panel.refresh(List.of(after.get(1), after.get(0)));
            assertSame(second, card(panel, 2));
            assertTrue(second.isSelected());
            assertTrue(second.getText().contains("Selecionada"));
            assertTrue(second.getText().contains("1 de 1 câmera gravando"));
            assertFalse(card(panel, 1).isSelected());
            assertEquals(List.of(2), events);
            panel.refresh(List.of(after.get(0)));
            assertFalse(card(panel, 1).isSelected());
            assertEquals(List.of(2), events);
        });
    }

    @Test
    void spaceActivatesTheFocusedCardOnce() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(7));
        List<CourtView> courts = new ManageGym(gym, new InMemoryGymRepository()).courts();
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> events = new ArrayList<>();
            CourtCardsPanel panel = new CourtCardsPanel(events::add);
            panel.refresh(courts);
            JToggleButton button = card(panel, 7);
            assertTrue(button.isFocusable());
            for (boolean released : List.of(false, true)) {
                KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, released);
                Object binding = button.getInputMap(JComponent.WHEN_FOCUSED).get(key);
                Action action = button.getActionMap().get(binding);
                assertNotNull(action);
                action.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, ""));
            }
            assertEquals(List.of(7), events);
            assertTrue(button.isSelected());
        });
    }

    private static FixedCamera camera(String id) {
        return new FixedCamera(id, "Fixa", Resolution.HD, 30, 45);
    }

    private static JToggleButton card(Container panel, int number) {
        return controls(panel, JToggleButton.class).stream()
                .filter(button -> ("Quadra " + number).equals(button.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> controls(Container parent, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) matches.add(type.cast(child));
            if (child instanceof Container nested) matches.addAll(controls(nested, type));
        }
        return matches;
    }
}
