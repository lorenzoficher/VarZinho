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
        dialog.setContentPane(content(item, format));
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    static JPanel content(HighlightView item, DateTimeFormatter format) {
        JPanel body = new JPanel(new MigLayout("insets 0, fill, gap 0, wrap 1", "[grow,fill]", "[grow,fill][]"));
        JPanel pitch = PitchPanel.clipCover();
        JButton play = new JButton("Reproduzir vídeo",
                VarZinhoIcons.arena("play-video", 32, VarZinhoTheme.ON_ACCENT));
        VarZinhoTheme.styleProminentAction(play);
        play.setMargin(new Insets(9, 16, 9, 18));
        play.setIconTextGap(10);
        play.setToolTipText("Abrir no reprodutor do computador");
        pitch.add(play);
        body.add(pitch, "w 200:580, h 120:260");
        JPanel info = new JPanel(new MigLayout("insets 18, fillx", "[grow][]"));
        String name = item.getTitle().isEmpty() ? "Melhor momento" : item.getTitle();
        JLabel title = new JLabel(name + " · Quadra " + item.getCourtNumber());
        title.putClientProperty("html.disable", true);
        VarZinhoTheme.styleTitle(title);
        info.add(title, "cell 0 0");
        JButton download = new JButton("Baixar vídeo");
        info.add(download, "cell 1 0 1 2, aligny center");
        info.add(new JLabel(format.format(item.getCapturedAt()) + " · " + item.getDurationSeconds()
                + " s · " + item.getCameraId()), "cell 0 1");
        JLabel feedback = new JLabel();
        info.add(feedback, "cell 0 2 2 1");
        Path source = Path.of(item.getClipPath());
        play.addActionListener(event -> {
            try { Desktop.getDesktop().open(source.toFile()); }
            catch (IOException | UnsupportedOperationException | IllegalArgumentException failure) {
                java.util.logging.Logger.getLogger(ClipDialog.class.getName()).log(java.util.logging.Level.WARNING,
                        "Could not open clip in system player", failure);
                feedback.setText("Não foi possível abrir o reprodutor do sistema.");
            }
        });
        download.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            String suggested = item.getTitle();
            if (suggested.isEmpty() || suggested.length() > 120 || suggested.matches(".*[<>:\"/\\\\|?*].*")
                    || suggested.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) {
                chooser.setSelectedFile(source.getFileName().toFile());
            } else {
                chooser.setSelectedFile(new java.io.File(suggested + ".mp4"));
            }
            if (chooser.showSaveDialog(body) != JFileChooser.APPROVE_OPTION) return;
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
        return body;
    }
}
