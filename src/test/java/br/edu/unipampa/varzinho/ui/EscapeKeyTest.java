package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class EscapeKeyTest {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    @BeforeEach
    void needsAWindowSystem() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "a dialog needs a window system");
    }

    @Test
    void escapeClosesTheDialogAsItsCloseButtonWould() {
        JDialog dialog = new JDialog();
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        AtomicBoolean closing = new AtomicBoolean();
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closing.set(true); }
        });
        dialog.pack();

        EscapeKey.closes(dialog);
        pressEscape(dialog);

        assertTrue(closing.get(), "Esc must go through the same closing path as the close button");
        assertFalse(dialog.isDisplayable());
    }

    @Test
    void theClipDialogPlaysOnEnterAndClosesOnEscape() throws Exception {
        JDialog dialog = ClipDialog.build(new JPanel(), aHighlight(), FORMAT);

        assertEquals("Reproduzir vídeo", dialog.getRootPane().getDefaultButton().getText());
        pressEscape(dialog);
        assertFalse(dialog.isDisplayable());
    }

    @Test
    void escapeClosesTheSignInGateWithoutSigningAnyoneIn() throws Exception {
        SignInDialog gate = new SignInDialog(new OperatorAccess(new InMemoryOperatorStore()));

        pressEscape(gate);

        assertFalse(gate.isDisplayable());
    }

    @Test
    void escapeClosesThePasswordRecovery() {
        PasswordRecoveryDialog recovery =
                new PasswordRecoveryDialog(new JPanel(), new OperatorAccess(new InMemoryOperatorStore()));

        pressEscape(recovery);

        assertFalse(recovery.isDisplayable());
    }

    static void pressEscape(JDialog dialog) {
        var root = dialog.getRootPane();
        var escape = root.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        assertTrue(escape != null, "Esc is bound to nothing in " + dialog.getTitle());
        escape.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "escape"));
    }

    private static HighlightView aHighlight() throws Exception {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(new Highlight("first", Instant.parse("2026-11-20T00:30:00Z"), 1, "camera-1",
                new VideoClip("clips/first.mp4", 30, Resolution.FULL_HD, 12.5)));
        return new BrowseArchive(repository).all().getHighlights().get(0);
    }
}
