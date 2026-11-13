package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.ConnectStreamCamera;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.GymSession;
import br.edu.unipampa.varzinho.application.PendingClipWrites;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import br.edu.unipampa.varzinho.infrastructure.stream.PhoneConnection;
import br.edu.unipampa.varzinho.ui.VarZinhoWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The composition root: puts the application together — storage, the saved gym, the use
 * cases and, when there is one, the phone — and hands the result to the window. It is the
 * one place that names every concrete adapter, so a new storage or a new way to reach a
 * camera changes this class, never the window.
 */
public final class Launcher {
    private static final String STREAM_URL_VARIABLE = "VARZINHO_STREAM_URL";
    private static final Path SEGMENT_FOLDER = Path.of("buffer");
    private static final int BUFFER_SECONDS = 30;

    private Launcher() { }

    /**
     * Opens the window on the simulated cameras, or, when {@value #STREAM_URL_VARIABLE}
     * names a phone stream, on that phone as well.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(Path.of("data", "highlights.csv"));
            GymSession session = GymSession.start(new CsvGymRepository(Path.of("data", "gym.csv")), SampleGym.build(),
                    message -> warn(message, "Saved gym not loaded"));
            PendingClipWrites pendingWrites = new PendingClipWrites();
            Optional<PhoneConnection> phone = PhoneConnection.open(System.getenv(STREAM_URL_VARIABLE),
                    SEGMENT_FOLDER, BUFFER_SECONDS, pendingWrites, message -> warn(message, "Phone not connected"));
            if (phone.isEmpty()) {
                window(session, repository, pendingWrites, new LiveFeed(session.gym())).setVisible(true);
            } else {
                openWithPhone(session, repository, pendingWrites, phone.get());
            }
        });
    }

    private static void openWithPhone(GymSession session, HighlightRepository repository,
                                      PendingClipWrites pendingWrites, PhoneConnection phone) {
        Gym gym = session.gym();
        ConnectStreamCamera connection = new ConnectStreamCamera(gym, phone.camera());
        CourtView court = connection.connect();
        // Two feeds, so the simulated courts keep their clock whatever the phone does.
        VarZinhoWindow window = window(session, repository, pendingWrites, new LiveFeed(gym, court.getNumber()));
        Runnable dropped = () -> {
            connection.phoneDropped();
            window.phoneDropped();
        };
        window.followPhone(phone.feed(dropped), court.getNumber(), phone.streamUrl());
        phone.onDrop(() -> SwingUtilities.invokeLater(dropped));
        window.setVisible(true);
    }

    private static VarZinhoWindow window(GymSession session, HighlightRepository repository,
                                         PendingClipWrites pendingWrites, Feed feed) {
        Gym gym = session.gym();
        return new VarZinhoWindow(gym.getName(), session.manageGym(),
                new CaptureHighlight(gym, repository, pendingWrites), new BrowseArchive(repository),
                feed, BUFFER_SECONDS);
    }

    private static void warn(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }
}
