package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitOnSwing;
import static br.edu.unipampa.varzinho.ui.SwingWork.onSwing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class EnterKeyTest {
    private final AtomicInteger presses = new AtomicInteger();
    private JFrame frame;
    private JSpinner spinner;
    private JTextField spinnerText;

    @BeforeEach
    void showAFormWithASpinner() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "a key press needs a window with focus");
        onSwing(() -> {
            spinner = new JSpinner(new SpinnerNumberModel(0, -180, 180, 1));
            spinnerText = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            JButton submit = new JButton("Enviar");
            submit.addActionListener(event -> presses.incrementAndGet());
            EnterKey.submits(submit, spinner);
            JPanel form = new JPanel();
            form.add(spinner);
            form.add(submit);
            frame = new JFrame();
            frame.add(form);
            frame.pack();
            frame.setVisible(true);
            spinnerText.requestFocusInWindow();
        });
        awaitOnSwing(() -> spinnerText.isFocusOwner());
    }

    @AfterEach
    void closeTheForm() {
        if (frame != null) onSwing(frame::dispose);
    }

    @Test
    void enterSubmitsASpinnerValueChangedWithTheArrowKeys() {
        press(KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        press(KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        press(KeyEvent.VK_ENTER, '\n');

        onSwing(() -> {
            assertEquals(2, spinner.getValue());
            assertEquals(1, presses.get());
        });
    }

    @Test
    void enterSubmitsATypedSpinnerValueOnceAfterCommittingIt() {
        onSwing(() -> spinnerText.setText("45"));
        press(KeyEvent.VK_ENTER, '\n');

        onSwing(() -> {
            assertEquals(45, spinner.getValue());
            assertEquals(1, presses.get());
        });
    }

    /** Posts the key to the event queue, the path a real key takes, and waits for it to land. */
    private void press(int keyCode, char keyChar) {
        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        long when = System.currentTimeMillis();
        queue.postEvent(new KeyEvent(spinnerText, KeyEvent.KEY_PRESSED, when, 0, keyCode, keyChar));
        queue.postEvent(new KeyEvent(spinnerText, KeyEvent.KEY_RELEASED, when, 0, keyCode, keyChar));
        onSwing(() -> { });
    }
}
