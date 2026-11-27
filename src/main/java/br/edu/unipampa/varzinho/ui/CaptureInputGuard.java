package br.edu.unipampa.varzinho.ui;

import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseAdapter;

/** Keeps the model with the capture worker without changing the appearance of the window. */
final class CaptureInputGuard {
    private CaptureInputGuard() { }

    static Runnable pause(JRootPane root) {
        Component previous = root.getGlassPane();
        boolean wasVisible = previous.isVisible();
        JPanel shield = new JPanel();
        shield.setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() { };
        shield.addMouseListener(mouse);
        shield.addMouseMotionListener(mouse);
        shield.addMouseWheelListener(event -> event.consume());
        KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyEventDispatcher keys = event -> SwingUtilities.getRootPane(event.getComponent()) == root;
        focus.addKeyEventDispatcher(keys);
        root.setGlassPane(shield);
        shield.setVisible(true);
        return () -> {
            focus.removeKeyEventDispatcher(keys);
            root.setGlassPane(previous);
            previous.setVisible(wasVisible);
        };
    }
}
