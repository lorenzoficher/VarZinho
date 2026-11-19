package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.PendingCapture;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Save highlight button, also on F2. The whole capture runs on a worker, while the Swing
 * thread pauses feeds and camera changes until the model is returned to it.
 */
final class CapturePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CapturePanel.class.getName());
    private static final String SAVE_SHORTCUT = "save-highlight";
    private final CourtPanel courtPanel;
    private final CaptureHighlight captureHighlight;
    private final CaptureActivity activity;
    private final Consumer<String> errorSink;
    private final Consumer<HighlightView> onSaved;
    private final DateTimeFormatter timeFormat;
    private final JButton save = new JButton();
    private final JLabel confirmation = new JLabel(" ");

    CapturePanel(CourtPanel courtPanel, CaptureHighlight captureHighlight, ZoneId zone,
                 Consumer<String> errorSink, Consumer<HighlightView> onSaved) {
        this(courtPanel, captureHighlight, zone, errorSink, onSaved, new CaptureActivity());
    }

    CapturePanel(CourtPanel courtPanel, CaptureHighlight captureHighlight, ZoneId zone,
                 Consumer<String> errorSink, Consumer<HighlightView> onSaved, CaptureActivity activity) {
        super(new BorderLayout(4, 4));
        if (courtPanel == null) throw new IllegalArgumentException("a capture panel needs a court panel");
        if (captureHighlight == null) throw new IllegalArgumentException("a capture panel needs a way to capture");
        if (zone == null) throw new IllegalArgumentException("a capture panel needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("a capture panel needs somewhere to report errors");
        if (onSaved == null) throw new IllegalArgumentException("a capture panel needs someone to tell about saves");
        if (activity == null) throw new IllegalArgumentException("a capture panel needs a capture activity");
        this.courtPanel = courtPanel;
        this.captureHighlight = captureHighlight;
        this.activity = activity;
        this.timeFormat = ArchivePanel.timeFormatIn(zone);
        this.errorSink = errorSink;
        this.onSaved = onSaved;
        VarZinhoTheme.styleProminentAction(save);
        save.addActionListener(event -> saveHighlight());
        bindShortcut();
        courtPanel.onCourtChange(this::showCourt);
        add(save, BorderLayout.CENTER);
        add(confirmation, BorderLayout.SOUTH);
        showCourt();
    }

    JButton saveButton() { return save; }

    // F2 works wherever the focus is in the window, and goes through the button so a
    // disabled button (a capture already running) refuses the key too.
    private void bindShortcut() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), SAVE_SHORTCUT);
        getActionMap().put(SAVE_SHORTCUT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save.doClick();
            }
        });
    }

    String confirmation() {
        return confirmation.getText().trim();
    }

    private void saveHighlight() {
        // An already queued action must not start a second worker.
        if (activity.isRunning()) return;
        int courtNumber = courtPanel.selectedCourtNumber();
        activity.begin();
        courtPanel.pauseChanges();
        save.setEnabled(false);
        confirmation.setText("Salvando: quadra " + courtNumber + "…");
        new Capture(courtNumber).execute();
    }

    private void archive(PendingCapture capture) {
        HighlightView archived;
        try {
            archived = capture.archive();
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not archive the captured highlight", failure);
            refuse("Não foi possível salvar o lance no acervo.");
            return;
        }
        confirmation.setText("Salvo: quadra " + archived.getCourtNumber()
                + " · câmera " + archived.getCameraId()
                + " · " + timeFormat.format(archived.getCapturedAt())
                + " · " + archived.getDurationSeconds() + " s");
        onSaved.accept(archived);
    }

    private void refuse(String reason) {
        confirmation.setText(" ");
        errorSink.accept(reason);
    }

    private void captureFailed(Throwable cause) {
        if (cause instanceof NoActiveCameraException || cause instanceof EmptyBufferException
                || cause instanceof ClipAssemblyException) {
            if (cause instanceof ClipAssemblyException) {
                LOGGER.log(Level.WARNING, "Could not assemble the clip", cause);
            }
            refuse(PortugueseText.message(cause.getMessage()));
        } else {
            LOGGER.log(Level.SEVERE, "capture failed unexpectedly", cause);
            refuse("Falha inesperada ao capturar o lance.");
        }
    }

    private final class Capture extends SwingWorker<PendingCapture, Void> {
        private final int courtNumber;

        Capture(int courtNumber) {
            this.courtNumber = courtNumber;
        }

        @Override
        protected PendingCapture doInBackground() {
            return captureHighlight.capture(courtNumber);
        }

        @Override
        protected void done() {
            try {
                archive(get());
            } catch (ExecutionException failure) {
                captureFailed(failure.getCause());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Capture interrupted before archiving", interrupted);
                refuse("A captura foi interrompida antes de ser salva.");
            } finally {
                courtPanel.resumeChanges();
                save.setEnabled(true);
                activity.finish();
            }
        }
    }

    private void showCourt() {
        save.setText("Salvar lance (F2) — quadra " + courtPanel.selectedCourtNumber());
        if (!activity.isRunning()) confirmation.setText(" ");
    }
}
