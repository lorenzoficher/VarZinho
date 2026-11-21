package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.OperatorView;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarPanelTest {
    @Test
    void selectingAnItemChangesPageAndMarksItSelected() throws Exception {
        AtomicReference<SidebarPanel.Page> shown = new AtomicReference<>();
        SidebarPanel sidebar = new SidebarPanel("Arena VarZinho", operator(), shown::set);

        sidebar.archiveButton().doClick();

        assertEquals(SidebarPanel.Page.ARCHIVE, shown.get());
        assertTrue(sidebar.archiveButton().isSelected());
    }

    @Test
    void switchesThemeFromTheSidebarAndBack() throws Exception {
        VarZinhoTheme.install();
        SidebarPanel sidebar = new SidebarPanel("Arena", operator(), page -> { });
        JButton theme = Arrays.stream(sidebar.getComponents()).filter(JButton.class::isInstance)
                .map(JButton.class::cast).filter(button -> "Alternar tema".equals(button.getText()))
                .findFirst().orElseThrow();
        theme.doClick();
        assertTrue(VarZinhoTheme.isDark());
        theme.doClick();
        assertFalse(VarZinhoTheme.isDark());
    }

    @Test
    void offersEveryFunctionalPageFromTheDashboard() throws Exception {
        SidebarPanel sidebar = new SidebarPanel("Arena VarZinho", operator(), page -> { });

        assertTrue(sidebar.dashboardButton().isSelected());
        assertEquals("Quadras", sidebar.courtsButton().getText());
        assertEquals("Câmeras", sidebar.camerasButton().getText());
        assertEquals("Melhores momentos", sidebar.highlightsButton().getText());
        assertEquals("Arquivos", sidebar.archiveButton().getText());
        assertEquals("Configurações", sidebar.settingsButton().getText());
    }

    private static OperatorView operator() throws Exception {
        return new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!").getOperator();
    }
}
