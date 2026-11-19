package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.HighlightView;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

final class ClipDialog {
    private ClipDialog() { }

    static void open(Component owner, HighlightView item, DateTimeFormatter format) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(owner), "Melhor momento",
                Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]"));
        JPanel pitch = new PitchPanel(new GridBagLayout());
        JButton play = new JButton("▶  Reproduzir vídeo");
        VarZinhoTheme.styleProminentAction(play);
        pitch.add(play);
        body.add(pitch, "w 580, h 260");
        JPanel info = new JPanel(new MigLayout("insets 18, fillx", "[grow][]"));
        JLabel title = new JLabel("Melhor momento · Quadra " + item.getCourtNumber());
        VarZinhoTheme.styleTitle(title);
        info.add(title);
        JButton download = new JButton("Baixar vídeo");
        info.add(download, "span 1 2, wrap");
        info.add(new JLabel(format.format(item.getCapturedAt()) + " · " + item.getDurationSeconds()
                + " s · " + item.getCameraId()), "wrap");
        JLabel feedback = new JLabel(" ");
        info.add(feedback, "span");
        Path source = Path.of(item.getClipPath());
        play.addActionListener(event -> {
            try { Desktop.getDesktop().open(source.toFile()); }
            catch (IOException | UnsupportedOperationException failure) {
                feedback.setText("Não foi possível abrir o reprodutor do sistema.");
            }
        });
        download.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(source.getFileName().toFile());
            if (chooser.showSaveDialog(dialog) != JFileChooser.APPROVE_OPTION) return;
            Path destination = chooser.getSelectedFile().toPath();
            download.setEnabled(false);
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws IOException {
                    Files.copy(source, destination);
                    return null;
                }
                protected void done() {
                    download.setEnabled(true);
                    try { get(); feedback.setText("Cópia salva em " + destination); }
                    catch (java.util.concurrent.ExecutionException failure) {
                        feedback.setText("Não foi possível copiar. Escolha um nome disponível.");
                    } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                }
            }.execute();
        });
        body.add(info);
        dialog.setContentPane(body);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
