package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.repository.CsvGymRepository;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Puts the application together — storage, the saved gym and, when there is one, the
 * phone — and hands the result to the window. A new storage or a new way to reach a
 * camera changes this class, never the window.
 */
public final class Launcher {
    private static final String STREAM_URL_VARIABLE = "VARZINHO_STREAM_URL";
    private static final Path SEGMENT_FOLDER = Path.of("buffer");

    private Launcher() { }

    /**
     * Opens the window on the simulated cameras, or, when {@value #STREAM_URL_VARIABLE}
     * names a phone stream, on that phone as well.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            HighlightRepository repository = new CsvHighlightRepository(Path.of("data", "highlights.csv"));
            GymSession session = GymSession.start(new CsvGymRepository(Path.of("data", "gym.csv")),
                    message -> warn(message, "Saved gym not loaded"));
            PendingClipWrites pendingWrites = new PendingClipWrites();
            Optional<PhoneConnection> phone = PhoneConnection.open(System.getenv(STREAM_URL_VARIABLE),
                    SEGMENT_FOLDER, pendingWrites, message -> warn(message, "Phone not connected"));
            if (phone.isEmpty()) {
                new VarZinhoWindow(session, repository, new LiveFeed(session.gym()), pendingWrites)
                        .setVisible(true);
            } else {
                openWithPhone(session, repository, pendingWrites, phone.get());
            }
        });
    }

    /**
     * Records the phone into a court of its own, so a capture there always comes from
     * the phone: a court captures from its first recording camera, and on a shared court
     * that would be whichever camera was installed first.
     */
    private static void openWithPhone(GymSession session, HighlightRepository repository,
                                      PendingClipWrites pendingWrites, PhoneConnection phone) {
        Gym gym = session.gym();
        int phoneCourt = gym.getCourts().stream().mapToInt(Court::getNumber).max().orElse(0) + 1;
        Court court = new Court(phoneCourt);
        gym.addCourt(court);
        gym.installCamera(phoneCourt, phone.camera());
        phone.camera().startRecording();
        // Two feeds, so the simulated courts keep their clock whatever the phone does.
        VarZinhoWindow window = new VarZinhoWindow(session, repository, new LiveFeed(gym, court), pendingWrites);
        Runnable dropped = () -> window.phoneDropped(phone.camera());
        window.followPhone(phone.camera(), phone.feed(dropped), phoneCourt, phone.streamUrl());
        phone.onDrop(() -> SwingUtilities.invokeLater(dropped));
        window.setVisible(true);
    }

    private static void warn(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }
}
