package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.ManageRecordings;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Opens, renames and deletes the selected archive recording. */
final class RecordingActions {
    private static final Logger LOGGER = Logger.getLogger(RecordingActions.class.getName());
    private static final String NO_PLAYER = "Nenhum reprodutor de vídeo disponível para abrir o clipe.";
    private static final String SAVE_NAME = "Salvar nome";
    private final ArchiveTable recordings;
    private final JTable table;
    private final DateTimeFormatter timeFormat;
    private final ArchivePanel.ClipPlayer player;
    private final Consumer<String> errorSink;
    private final Runnable refresh;
    private final Runnable recount;
    private final JButton openClip = new JButton("Assistir ao vídeo");
    private final List<JButton> editActions = new ArrayList<>();

    RecordingActions(ArchiveTable recordings, DateTimeFormatter format, ArchivePanel.ClipPlayer player,
                     Consumer<String> errorSink, Runnable refresh, Runnable recount) {
        this.recordings = recordings;
        this.table = recordings.table();
        this.timeFormat = format;
        this.player = player;
        this.errorSink = errorSink;
        this.refresh = refresh;
        this.recount = recount;
        VarZinhoTheme.styleButton(openClip, ButtonRole.PRIMARY);
        openClip.setToolTipText("Abrir o lance selecionado (Ctrl+Enter na tabela)");
        openClip.setIcon(VarZinhoIcons.openClip(VarZinhoTheme.ON_ACCENT));
        openClip.addActionListener(event -> openSelectedClip());
        table.getSelectionModel().addListSelectionListener(event -> updateOpenClip());
        recordings.onPreview(this::previewSelectedClip);
    }

    JButton openClipButton() { return openClip; }

    void enableEditing(ManageRecordings manageRecordings) {
        JButton rename = new JButton("Renomear gravação");
        JButton delete = new JButton("Excluir gravação");
        VarZinhoTheme.styleButton(rename, ButtonRole.SECONDARY);
        VarZinhoTheme.styleButton(delete, ButtonRole.DESTRUCTIVE);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.setOpaque(false);
        actions.add(rename);
        actions.add(delete);
        recordings.addActions(actions);
        editActions.add(rename);
        editActions.add(delete);
        Runnable update = () -> {
            boolean oneSelected = table.getSelectedRowCount() == 1;
            rename.setEnabled(oneSelected);
            delete.setEnabled(oneSelected);
        };
        table.getSelectionModel().addListSelectionListener(event -> update.run());
        update.run();
        rename.addActionListener(event -> renameSelection(manageRecordings, this::askName));
        delete.addActionListener(event -> deleteSelection(manageRecordings, this::confirmDeletion));
    }

    private Optional<String> askName(HighlightView item) {
        JTextField name = new JTextField(item.getTitle(), 26);
        JDialog prompt = renameDialog(table, name);
        prompt.setVisible(true);
        prompt.dispose();
        return chosenName(name);
    }

    /** @return the name typed in the rename prompt, or empty unless the prompt was saved */
    static Optional<String> chosenName(JTextField name) {
        JOptionPane pane = (JOptionPane) SwingUtilities.getAncestorOfClass(JOptionPane.class, name);
        return pane != null && SAVE_NAME.equals(pane.getValue()) ? Optional.of(name.getText()) : Optional.empty();
    }

    /**
     * Builds the prompt that asks for a recording's new name. It opens with the name field
     * focused and its text selected, so typing replaces the name and Enter saves it; the
     * option pane alone would focus the save button and leave the typing nowhere.
     */
    static JDialog renameDialog(Component parent, JTextField name) {
        name.getAccessibleContext().setAccessibleName("Nome da gravação");
        Object[] options = {SAVE_NAME, "Cancelar"};
        JOptionPane pane = new JOptionPane(new Object[]{"Nome da gravação", name}, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, options[0]);
        JDialog dialog = pane.createDialog(parent, "Renomear gravação");
        // Registered after the pane's own listener, which focuses the save button once the
        // dialog first gains focus; this one runs next and hands the focus to the field.
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                dialog.removeWindowFocusListener(this);
                name.requestFocusInWindow();
                // The caret settles its own position when focus arrives, so select after it.
                SwingUtilities.invokeLater(name::selectAll);
            }
        });
        return dialog;
    }

    /** Renames the selected recording to the name {@code askName} returns; empty means the operator gave up. */
    void renameSelection(ManageRecordings manageRecordings, Function<HighlightView, Optional<String>> askName) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = recordings.selectedRecording();
        Optional<String> chosen = askName.apply(item);
        if (chosen.isEmpty()) return;
        String id = item.getId();
        String title = chosen.get();
        RepositoryTask.<Void>run(editActions, "Renomeando a gravação…", () -> {
            manageRecordings.rename(id, title);
            return null;
        }, renamed -> refresh.run(), failure -> {
            if (failure instanceof IllegalArgumentException) {
                errorSink.accept("Informe um nome em uma única linha.");
                return;
            }
            LOGGER.log(failure instanceof RepositoryException ? Level.WARNING : Level.SEVERE,
                    "Could not rename recording", failure);
            errorSink.accept("Não foi possível renomear a gravação. Verifique o acervo.");
        });
    }

    private boolean confirmDeletion(HighlightView item) {
        String label = item.getTitle().isEmpty() ? item.getId() : item.getTitle();
        String prompt = "Tem certeza de que deseja excluir “" + label + "”?\nQuadra " + item.getCourtNumber()
                + " · " + item.getCameraId() + " · " + timeFormat.format(item.getCapturedAt())
                + "\nO lance será removido do acervo e o MP4 original será apagado, se existir.";
        Object[] options = {"Cancelar", "Excluir gravação"};
        return JOptionPane.showOptionDialog(table, prompt, "Confirmar exclusão", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1;
    }

    List<JButton> editActions() { return List.copyOf(editActions); }

    void deleteSelection(ManageRecordings manageRecordings, Predicate<HighlightView> confirm) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = recordings.selectedRecording();
        if (!confirm.test(item)) return;
        String id = item.getId();
        RepositoryTask.<Void>run(editActions, "Excluindo a gravação…", () -> {
            manageRecordings.delete(id);
            return null;
        }, deleted -> {
            recordings.forgetCategory(id);
            recount.run();
        }, failure -> {
            LOGGER.log(failure instanceof RepositoryException ? Level.WARNING : Level.SEVERE,
                    "Could not delete recording", failure);
            errorSink.accept("Não foi possível excluir a gravação. Verifique o acervo e o arquivo de vídeo.");
        });
    }

    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    private void previewSelectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) {
            errorSink.accept("Selecione um lance no acervo para abrir o clipe.");
            return;
        }
        String clip = recordings.selectedRecording().getClipPath();
        if (!playable(clip)) {
            errorSink.accept("Não há arquivo de vídeo em " + clip
                    + ": somente lances de uma câmera de transmissão são gravados no disco.");
            return;
        }
        ClipDialog.open(table, recordings.selectedRecording(), timeFormat);
    }
    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    void updateOpenClip() {
        Optional<Path> clip = selectedClip();
        boolean exists = clip.filter(Files::exists).isPresent();
        openClip.setEnabled(exists);
        if (clip.isEmpty()) openClip.setToolTipText("Selecione um lance para abrir o clipe.");
        else if (!exists) openClip.setToolTipText("Este lance não tem arquivo de vídeo.");
        else openClip.setToolTipText(null);
    }

    private Optional<Path> selectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) return Optional.empty();
        try {
            return Optional.of(Path.of(recordings.selectedRecording().getClipPath()));
        } catch (InvalidPathException unusable) {
            LOGGER.log(Level.FINE, "A clip path cannot name a file on this system", unusable);
            return Optional.empty();
        }
    }

    private void openSelectedClip() {
        Optional<Path> clip = selectedClip().filter(Files::exists);
        if (clip.isEmpty()) {
            // The file went away after the row was chosen.
            updateOpenClip();
            return;
        }
        try {
            player.open(clip.get());
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException failure) {
            LOGGER.log(Level.WARNING, "Could not open the clip in the system player", failure);
            errorSink.accept(NO_PLAYER);
        }
    }
    static boolean playable(String path) {
        try { return path.toLowerCase(Locale.ROOT).endsWith(".mp4") && Files.isRegularFile(Path.of(path)); }
        catch (InvalidPathException invalid) { return false; }
    }
}
