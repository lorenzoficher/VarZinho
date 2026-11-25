package br.edu.unipampa.varzinho.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;

/**
 * Enter inside a form's fields presses the form's button.
 *
 * <p>Pages share one window, so a window-wide default button would let Enter on the
 * archive table or a sidebar item fire some other page's form. Binding Enter to the
 * fields keeps it inside the form they belong to.
 */
final class EnterKey {
    private EnterKey() { }

    /**
     * Makes Enter in any of the fields press {@code action}. A spinner commits the typed
     * value before Enter reaches the button, and a disabled button ignores the press, so
     * a form whose work is still running is not submitted twice.
     *
     * @throws IllegalArgumentException if a field is neither a text field nor a spinner
     *     with a text editor
     */
    static void submits(JButton action, JComponent... fields) {
        for (JComponent field : fields) {
            textOf(field).addActionListener(event -> action.doClick());
        }
    }

    private static JTextField textOf(JComponent field) {
        if (field instanceof JTextField text) return text;
        if (field instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            return editor.getTextField();
        }
        throw new IllegalArgumentException("Enter can submit only from a text field or a spinner, not " + field);
    }
}
