package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.OperatorView;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
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
    void arrowDownOpensTheNextPage() throws Exception {
        AtomicReference<SidebarPanel.Page> shown = new AtomicReference<>();
        SidebarPanel sidebar = new SidebarPanel("Arena", operator(), shown::set);

        press(sidebar.dashboardButton(), KeyEvent.VK_DOWN);

        assertEquals(SidebarPanel.Page.COURTS, shown.get());
        assertTrue(sidebar.courtsButton().isSelected());
    }

    @Test
    void arrowsWrapAroundBothEndsOfTheMenu() throws Exception {
        AtomicReference<SidebarPanel.Page> shown = new AtomicReference<>();
        SidebarPanel sidebar = new SidebarPanel("Arena", operator(), shown::set);

        press(sidebar.dashboardButton(), KeyEvent.VK_UP);
        assertEquals(SidebarPanel.Page.SETTINGS, shown.get());

        press(sidebar.settingsButton(), KeyEvent.VK_DOWN);
        assertEquals(SidebarPanel.Page.DASHBOARD, shown.get());
    }

    private static void press(AbstractButton item, int key) {
        var action = item.getActionForKeyStroke(KeyStroke.getKeyStroke(key, 0));
        assertTrue(action != null, item.getText() + " ignores " + KeyEvent.getKeyText(key));
        action.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, null));
    }

    @ParameterizedTest(name = "dark theme: {0}")
    @ValueSource(booleans = {false, true})
    void everySidebarControlShowsItsFocusOverTheSidebar(boolean dark) throws Exception {
        VarZinhoThemeTest.installTheme(dark);
        try {
            SidebarPanel sidebar = new SidebarPanel("Arena", operator(), page -> { });

            for (AbstractButton control : List.of(sidebar.dashboardButton(), sidebar.courtsButton(),
                    sidebar.camerasButton(), sidebar.highlightsButton(), sidebar.archiveButton(),
                    sidebar.operatorsButton(), sidebar.settingsButton(), themeButton(sidebar))) {
                assertTrue(Contrast.borderWidth(control) > 0, control.getText() + " draws no focus border");
                VarZinhoThemeTest.assertContrasts(Contrast.focusedBorderColor(control), VarZinhoTheme.SIDEBAR,
                        control.getText() + " focus on the sidebar");
            }
        } finally {
            VarZinhoTheme.install();
        }
    }

    private static JButton themeButton(SidebarPanel sidebar) {
        return Arrays.stream(sidebar.getComponents()).filter(JButton.class::isInstance)
                .map(JButton.class::cast).filter(button -> "Alternar tema".equals(button.getText()))
                .findFirst().orElseThrow();
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
