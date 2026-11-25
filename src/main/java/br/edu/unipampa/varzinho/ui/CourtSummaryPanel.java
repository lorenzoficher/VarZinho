package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ArchiveSummary;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The two cards above the court page, answering what an operator actually asks of the
 * selected court: can a highlight be saved now, and how many were saved today. It follows
 * the court panel by itself; a save elsewhere calls {@link #refresh()}.
 */
final class CourtSummaryPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CourtSummaryPanel.class.getName());
    private final CourtPanel courts;
    private final ManageGym management;
    private final BrowseArchive archive;
    private final Consumer<String> errorSink;
    private final DateTimeFormatter hourFormat;
    private final JLabel recording = new JLabel();
    private final JLabel today = new JLabel();

    CourtSummaryPanel(CourtPanel courts, ManageGym management, BrowseArchive archive, ZoneId zone,
                      Consumer<String> errorSink) {
        super(new MigLayout("insets 0, fillx", "[grow,fill]12[grow,fill]", "[]"));
        if (courts == null) throw new IllegalArgumentException("a court summary needs the court panel it follows");
        if (management == null) throw new IllegalArgumentException("a court summary needs the gym's management");
        if (archive == null) throw new IllegalArgumentException("a court summary needs the archive to count");
        if (zone == null) throw new IllegalArgumentException("a court summary needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("a court summary needs somewhere to report errors");
        this.courts = courts;
        this.management = management;
        this.archive = archive;
        this.errorSink = errorSink;
        this.hourFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
        add(card("Câmeras gravando", recording));
        add(card("Lances hoje", today));
        courts.onCourtChange(this::refresh);
        courts.onCamerasChanged(this::refreshRecording);
        refresh();
    }

    void refresh() {
        refreshRecording();
        refreshToday();
    }

    String recordingText() { return recording.getText(); }
    String todayText() { return today.getText(); }

    private void refreshRecording() {
        CourtView court = management.court(courts.selectedCourtNumber());
        recording.setText(court.getRecordingCameraCount() + " de " + court.getCameraCount());
    }

    private void refreshToday() {
        ArchiveSummary summary;
        try {
            summary = archive.todaySummary(courts.selectedCourtNumber());
        } catch (RepositoryException unreadable) {
            LOGGER.log(Level.WARNING, "Could not count today's highlights", unreadable);
            today.setText("—");
            errorSink.accept("Não foi possível contar os lances de hoje.");
            return;
        }
        today.setText(summary.getLastCapturedAt()
                .map(last -> summary.getCount() + ", último às " + hourFormat.format(last))
                .orElse(String.valueOf(summary.getCount())));
    }

    private static JPanel card(String title, JLabel value) {
        JPanel card = new JPanel(new MigLayout("insets 0, wrap 1", "[grow,fill]", "[]2[]"));
        VarZinhoTheme.styleCard(card);
        card.add(new JLabel(title));
        VarZinhoTheme.styleTitle(value);
        card.add(value);
        return card;
    }
}
