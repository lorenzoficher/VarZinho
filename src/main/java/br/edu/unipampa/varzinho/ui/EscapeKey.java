package br.edu.unipampa.varzinho.ui;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

/** The keyboard way out of a dialog: Esc does what the title bar's close button does. */
final class EscapeKey {
    private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    private EscapeKey() { }

    /**
     * Makes Esc close the dialog from wherever focus is inside it. It sends the same
     * window-closing event as the close button, so the dialog's close operation and any
     * closing listener run exactly as they would for a click.
     */
    static void closes(JDialog dialog) {
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING)),
                ESCAPE, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
