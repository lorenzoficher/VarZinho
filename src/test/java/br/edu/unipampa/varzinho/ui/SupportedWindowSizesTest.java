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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static br.edu.unipampa.varzinho.ui.SwingWork.onSwing;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Lays every page out at each supported window size, in both themes, and looks for broken layout. */
class SupportedWindowSizesTest {
    /** Room the operating system's title bar and frame borders take from the window. */
    private static final int WINDOW_DECORATION_HEIGHT = 40;
    private static final int MAXIMUM_LAYOUT_PASSES = 6;

    private static final List<Dimension> SUPPORTED_SIZES = List.of(
            VarZinhoWindow.minimumSupportedSize(), new Dimension(1366, 768), new Dimension(1920, 1080));

    private static final List<Function<SidebarPanel, JToggleButton>> PAGES = List.of(
            SidebarPanel::dashboardButton, SidebarPanel::courtsButton, SidebarPanel::camerasButton,
            SidebarPanel::highlightsButton, SidebarPanel::archiveButton, SidebarPanel::operatorsButton,
            SidebarPanel::settingsButton);

    @AfterEach
    void restoreLightTheme() {
        onSwing(VarZinhoTheme::install);
    }

    static Stream<Arguments> pagesAtEverySizeInBothThemes() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean dark : new boolean[] {false, true}) {
            for (Dimension size : SUPPORTED_SIZES) {
                for (int page = 0; page < PAGES.size(); page++) {
                    cases.add(Arguments.of(dark ? "dark" : "light", size.width + "x" + size.height, page));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0} theme, {1}, page {2}")
    @MethodSource("pagesAtEverySizeInBothThemes")
    void pageShowsEverythingWithoutClippingOverlapOrSidewaysScrolling(String theme, String size, int page)
            throws RepositoryException {
        SignedInOperator operator = new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!");
        String[] dimensions = size.split("x");
        Dimension window = new Dimension(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
        List<String> problems = new ArrayList<>();
        onSwing(() -> {
            VarZinhoTheme.install();
            if (theme.equals("dark")) VarZinhoTheme.toggle();
            ArenaWorkspace workspace = workspace(operator);
            SidebarPanel sidebar = (SidebarPanel) ((BorderLayout) workspace.getLayout())
                    .getLayoutComponent(BorderLayout.WEST);
            PAGES.get(page).apply(sidebar).doClick();
            workspace.setSize(window.width, window.height - WINDOW_DECORATION_HEIGHT);
            settle(workspace);
            inspect(workspace, problems);
        });
        assertEquals(List.of(), problems);
    }

    private static ArenaWorkspace workspace(SignedInOperator operator) {
        var gym = SampleGym.build();
        var repository = new InMemoryHighlightRepository();
        return new ArenaWorkspace("Arena", operator, new ManageGym(gym, new InMemoryGymRepository()),
                new CaptureHighlight(gym, repository), new BrowseArchive(repository),
                30, new CaptureActivity(), new MessageBar());
    }

    /**
     * Repeats the layout until nothing moves, as Swing does after each {@code revalidate}: a
     * section that stacks changes its height, and its parent only learns that on the next pass.
     */
    private static void settle(Container root) {
        String before = "";
        for (int pass = 0; pass < MAXIMUM_LAYOUT_PASSES; pass++) {
            layOut(root);
            String after = boundsOf(root);
            if (after.equals(before)) return;
            before = after;
        }
    }

    private static String boundsOf(Container container) {
        StringBuilder bounds = new StringBuilder(container.getBounds().toString());
        for (Component child : container.getComponents()) {
            bounds.append(child instanceof Container nested ? boundsOf(nested) : child.getBounds().toString());
        }
        return bounds.toString();
    }

    /** Lays a tree out without a native window, which a test machine may not have. */
    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layOut(nested);
        }
    }

    private static void inspect(Container container, List<String> problems) {
        if (container instanceof JScrollPane scroll && scroll.getHorizontalScrollBar().isVisible()) {
            problems.add("sideways scrolling in " + describe(scroll.getViewport().getView()));
        }
        List<Component> shown = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (!child.isVisible() || child.getWidth() <= 0 || child.getHeight() <= 0) continue;
            shown.add(child);
            if (isClipped(child)) {
                problems.add("clipped text in " + describe(child) + ": " + child.getWidth() + "x" + child.getHeight()
                        + " of " + child.getPreferredSize().width + "x" + child.getPreferredSize().height + " px");
            }
            if (!(container instanceof JViewport) && !new Rectangle(container.getSize()).contains(child.getBounds())) {
                problems.add("cut off at the edge: " + describe(child) + " at " + child.getX() + "," + child.getY()
                        + " " + child.getWidth() + "x" + child.getHeight() + " inside "
                        + container.getWidth() + "x" + container.getHeight());
            }
        }
        if (!(container instanceof JViewport)) reportOverlaps(shown, problems);
        for (Component child : shown) {
            if (child instanceof Container nested && !drawnByLookAndFeel(nested)) inspect(nested, problems);
        }
    }

    /** A spinner or combo box arranges its own arrow buttons and editor, touching on purpose. */
    private static boolean drawnByLookAndFeel(Container container) {
        return container instanceof JSpinner || container instanceof JComboBox;
    }

    private static boolean isClipped(Component component) {
        boolean showsText = (component instanceof JLabel label && hasText(label.getText()))
                || (component instanceof AbstractButton button && hasText(button.getText()));
        Dimension needed = component.getPreferredSize();
        return showsText && (component.getWidth() < needed.width || component.getHeight() < needed.height);
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private static void reportOverlaps(List<Component> siblings, List<String> problems) {
        for (int first = 0; first < siblings.size(); first++) {
            for (int second = first + 1; second < siblings.size(); second++) {
                Rectangle shared = siblings.get(first).getBounds().intersection(siblings.get(second).getBounds());
                if (!shared.isEmpty()) {
                    problems.add(describe(siblings.get(first)) + " overlaps " + describe(siblings.get(second)));
                }
            }
        }
    }

    private static String describe(Component component) {
        String text = component instanceof JLabel label ? label.getText()
                : component instanceof AbstractButton button ? button.getText() : null;
        String owner = component.getParent() == null ? "" : " in " + component.getParent().getClass().getSimpleName();
        return component.getClass().getSimpleName() + (hasText(text) ? " \"" + text + "\"" : "") + owner;
    }
}
