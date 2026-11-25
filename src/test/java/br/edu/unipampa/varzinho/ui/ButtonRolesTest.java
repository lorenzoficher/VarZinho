package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static br.edu.unipampa.varzinho.ui.SwingWork.onSwing;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every button on every screen is drawn through one of the shared roles, never styled by hand. */
class ButtonRolesTest {
    private static final List<Function<SidebarPanel, JToggleButton>> PAGES = List.of(
            SidebarPanel::dashboardButton, SidebarPanel::courtsButton, SidebarPanel::camerasButton,
            SidebarPanel::highlightsButton, SidebarPanel::archiveButton, SidebarPanel::operatorsButton,
            SidebarPanel::settingsButton);
    /** Words that only ever name an action the operator cannot undo. */
    private static final List<String> DESTRUCTIVE_WORDS = List.of("Remover", "Excluir");
    /** The roles drawn on the lime accent, whose icons must read on it as their text does. */
    private static final Set<ButtonRole> FILLED_WITH_ACCENT = Set.of(ButtonRole.PROMINENT, ButtonRole.PRIMARY);
    /** WCAG 2.2, 1.4.11: an icon that carries meaning needs 3:1 against its background. */
    private static final double ICON_MINIMUM = 3.0;

    @AfterEach
    void leaveTheNextTestInTheDefaultLightTheme() {
        onSwing(VarZinhoTheme::install);
    }

    @Test
    void everyButtonInTheWorkspaceHasARole() throws RepositoryException {
        SignedInOperator operator = new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!");
        List<String> problems = new ArrayList<>();
        onSwing(() -> {
            VarZinhoTheme.install();
            ArenaWorkspace workspace = workspace(operator);
            SidebarPanel sidebar = (SidebarPanel) ((BorderLayout) workspace.getLayout())
                    .getLayoutComponent(BorderLayout.WEST);
            for (Function<SidebarPanel, JToggleButton> page : PAGES) {
                page.apply(sidebar).doClick();
                inspect(workspace, sidebar, problems);
            }
        });
        assertEquals(List.of(), problems.stream().distinct().toList());
    }

    @Test
    void everyButtonOnTheSignInScreensHasARole() {
        List<String> problems = new ArrayList<>();
        onSwing(() -> {
            VarZinhoTheme.install();
            OperatorAccess access = new OperatorAccess(new InMemoryOperatorStore());
            inspect(new SignInPanel(access, message -> { }, signedIn -> { }), null, problems);
            inspect(new FirstOperatorPanel(access, message -> { }, signedIn -> { }), null, problems);
            inspect(new PasswordRecoveryPanel(access, message -> { }, username -> { }), null, problems);
        });
        assertEquals(List.of(), problems);
    }

    @Test
    void iconsOnTheAccentReadInTheDarkTheme() throws RepositoryException {
        SignedInOperator operator = new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!");
        List<String> problems = new ArrayList<>();
        onSwing(() -> {
            VarZinhoTheme.install();
            VarZinhoTheme.toggle();
            ArenaWorkspace workspace = workspace(operator);
            SidebarPanel sidebar = (SidebarPanel) ((BorderLayout) workspace.getLayout())
                    .getLayoutComponent(BorderLayout.WEST);
            for (Function<SidebarPanel, JToggleButton> page : PAGES) {
                page.apply(sidebar).doClick();
                for (JButton button : buttonsIn(workspace, sidebar)) checkIconOnAccent(button, problems);
            }
        });
        assertEquals(List.of(), problems.stream().distinct().toList());
    }

    private static void inspect(Container root, Container chrome, List<String> problems) {
        for (JButton button : buttonsIn(root, chrome)) check(button, problems);
    }

    private static List<JButton> buttonsIn(Container root, Container chrome) {
        List<JButton> buttons = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && (chrome == null || !SwingUtilities.isDescendingFrom(button, chrome))) {
                buttons.add(button);
            }
            // A selector, spinner, scroll bar or search field draws its own small buttons; they belong to the look and feel.
            boolean drawnByTheLookAndFeel = child instanceof JComboBox || child instanceof JSpinner
                    || child instanceof JScrollBar || child instanceof JTextComponent;
            if (child instanceof Container nested && !drawnByTheLookAndFeel) buttons.addAll(buttonsIn(nested, chrome));
        }
        return buttons;
    }

    private static void checkIconOnAccent(JButton button, List<String> problems) {
        boolean filled = VarZinhoTheme.roleOf(button).filter(FILLED_WITH_ACCENT::contains).isPresent();
        if (!filled || !(button.getIcon() instanceof FlatSVGIcon icon) || icon.getColorFilter() == null) return;
        // The bundled icons are drawn in black ink; the filter decides what that ink becomes.
        Color ink = icon.getColorFilter().filter(Color.BLACK);
        double ratio = Contrast.ratio(ink, VarZinhoTheme.ACCENT);
        if (ratio < ICON_MINIMUM) {
            problems.add("\"" + button.getText() + "\" icon has contrast " + String.format("%.2f", ratio));
        }
    }

    private static void check(JButton button, List<String> problems) {
        String name = (button.getText().isBlank() ? button.getAccessibleContext().getAccessibleName() : button.getText())
                + " in " + button.getParent().getClass().getName();
        var role = VarZinhoTheme.roleOf(button);
        if (role.isEmpty()) {
            problems.add("\"" + name + "\" has no role");
            return;
        }
        boolean destructive = DESTRUCTIVE_WORDS.stream().anyMatch(word -> button.getText().startsWith(word));
        if (destructive != (role.get() == ButtonRole.DESTRUCTIVE)) {
            problems.add("\"" + name + "\" is " + role.get());
        }
    }

    private static ArenaWorkspace workspace(SignedInOperator operator) {
        var gym = SampleGym.build();
        var repository = new InMemoryHighlightRepository();
        return new ArenaWorkspace("Arena", operator, new ManageGym(gym, new InMemoryGymRepository()),
                new CaptureHighlight(gym, repository), new BrowseArchive(repository),
                30, new CaptureActivity(), new MessageBar());
    }
}
