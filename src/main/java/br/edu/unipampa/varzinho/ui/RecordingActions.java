package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

/** Opens, renames and deletes the selected archive recording. */
final class RecordingActions {
    private static final Logger LOGGER = Logger.getLogger(RecordingActions.class.getName());
    private static final String NO_PLAYER = "Nenhum reprodutor de vídeo disponível para abrir o clipe.";
    private final ArchiveTable recordings;
    private final JTable table;
    private final DateTimeFormatter timeFormat;
    private final ArchivePanel.ClipPlayer player;
    private final Consumer<String> errorSink;
    private final Runnable refresh;
    private final Runnable recount;
    private final JButton openClip = new JButton("Assistir ao vídeo");

    RecordingActions(ArchiveTable recordings, DateTimeFormatter format, ArchivePanel.ClipPlayer player,
                     Consumer<String> errorSink, Runnable refresh, Runnable recount) {
        this.recordings = recordings;
        this.table = recordings.table();
        this.timeFormat = format;
        this.player = player;
        this.errorSink = errorSink;
        this.refresh = refresh;
        this.recount = recount;
        openClip.setToolTipText("Abrir o lance selecionado (Ctrl+Enter na tabela)");
        openClip.setIcon(VarZinhoIcons.openClip());
        openClip.addActionListener(event -> openSelectedClip());
        table.getSelectionModel().addListSelectionListener(event -> updateOpenClip());
        recordings.onPreview(this::previewSelectedClip);
    }

    JButton openClipButton() { return openClip; }

    void enableEditing(RecordingEditor editor) {
        JButton rename = new JButton("Renomear gravação");
        JButton delete = new JButton("Excluir gravação");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.setOpaque(false);
        actions.add(rename);
        actions.add(delete);
        recordings.addActions(actions);
        Runnable update = () -> {
            boolean oneSelected = table.getSelectedRowCount() == 1;
            rename.setEnabled(oneSelected);
            delete.setEnabled(oneSelected);
        };
        table.getSelectionModel().addListSelectionListener(event -> update.run());
        update.run();
        rename.addActionListener(event -> renameSelection(editor));
        delete.addActionListener(event -> deleteSelection(editor, this::confirmDeletion));
    }

    private void renameSelection(RecordingEditor editor) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = recordings.selectedRecording();
        JTextField name = new JTextField(item.getTitle(), 26);
        name.getAccessibleContext().setAccessibleName("Nome da gravação");
        Object[] options = {"Salvar nome", "Cancelar"};
        int answer = JOptionPane.showOptionDialog(table, new Object[]{"Nome da gravação", name}, "Renomear gravação",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (answer != 0) return;
        try {
            editor.rename(item.getId(), name.getText());
            refresh.run();
        } catch (IllegalArgumentException invalid) {
            errorSink.accept("Informe um nome em uma única linha.");
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not rename recording", failure);
            errorSink.accept("Não foi possível renomear a gravação. Verifique o acervo.");
        }
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

    void deleteSelection(RecordingEditor editor, Predicate<HighlightView> confirm) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = recordings.selectedRecording();
        if (!confirm.test(item)) return;
        try {
            editor.delete(item.getId());
            recordings.forgetCategory(item.getId());
            recount.run();
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not delete recording", failure);
            errorSink.accept("Não foi possível excluir a gravação. Verifique o acervo e o arquivo de vídeo.");
        }
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
