package br.edu.unipampa.varzinho.ui;

import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

/** Tab moves focus between controls, never through the cells of a table. */
final class TabKey {
    private TabKey() { }

    /**
     * Makes Tab and Shift+Tab leave the table for the next or previous control. Swing's
     * table keeps Tab to walk its cells and leaves only on Ctrl+Tab, which nobody guesses,
     * so the last stop of a page would trap the keyboard. The arrow keys still move between
     * rows and cells.
     */
    static void leaves(JTable table) {
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.of(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK)));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Set.of(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)));
    }
}
