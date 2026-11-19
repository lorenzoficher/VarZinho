package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static OperatorView operator() throws Exception {
        Constructor<OperatorView> constructor = OperatorView.class.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance("Rafaela Alves", "rafaela");
    }
}
