package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.ConnectStreamCamera;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.GymSession;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import br.edu.unipampa.varzinho.infrastructure.stream.PhoneConnection;
import br.edu.unipampa.varzinho.ui.SignInDialog;
import br.edu.unipampa.varzinho.ui.VarZinhoTheme;
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
    private static final Path DATA_FOLDER = Path.of("data");
    private static final Path SEGMENT_FOLDER = Path.of("buffer");
    private static final int BUFFER_SECONDS = 30;

    private Launcher() { }

    /**
     * Asks for an operator to sign in — or to be created, on the first run — and then
     * opens the window on the simulated cameras, or, when {@value #STREAM_URL_VARIABLE}
     * names a phone stream, on that phone as well. Everything, operators included, is kept
     * in the CSV files unless {@value Storage#URL_VARIABLE} names a database; when that
     * database cannot be reached, or the sign-in is closed without an operator, no window
     * opens and the application exits.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            VarZinhoTheme.install();
            Storage storage;
            try {
                storage = Storage.fromEnvironment(System::getenv, DATA_FOLDER);
            } catch (RepositoryException unreachable) {
                refuseToOpen(unreachable.getMessage(), "Database not reached");
                return;
            }
            if (signIn(storage).isEmpty()) {
                return;
            }
            HighlightRepository repository = storage.highlights();
            GymSession session = GymSession.start(storage.gym(), SampleGym.build(),
                    message -> warn(message, "Saved gym not loaded"));
            Optional<PhoneConnection> phone = PhoneConnection.open(System.getenv(STREAM_URL_VARIABLE),
                    SEGMENT_FOLDER, BUFFER_SECONDS, message -> warn(message, "Phone not connected"));
            if (phone.isEmpty()) {
                window(session, repository, new LiveFeed(session.gym())).setVisible(true);
            } else {
                openWithPhone(session, repository, phone.get());
            }
        });
    }

    private static void openWithPhone(GymSession session, HighlightRepository repository,
                                      PhoneConnection phone) {
        Gym gym = session.gym();
        ConnectStreamCamera connection = new ConnectStreamCamera(gym, phone.camera());
        CourtView court = connection.connect();
        // Two feeds, so the simulated courts keep their clock whatever the phone does.
        VarZinhoWindow window = window(session, repository, new LiveFeed(gym, court.getNumber()));
        Runnable dropped = () -> window.phoneDropped(connection::phoneDropped);
        window.followPhone(phone.feed(dropped), court.getNumber(), phone.streamUrl());
        phone.onDrop(() -> SwingUtilities.invokeLater(dropped));
        window.setVisible(true);
    }

    private static VarZinhoWindow window(GymSession session, HighlightRepository repository,
                                         Feed feed) {
        Gym gym = session.gym();
        return new VarZinhoWindow(gym.getName(), session.manageGym(),
                new CaptureHighlight(gym, repository), new BrowseArchive(repository),
                feed, BUFFER_SECONDS);
    }

    // Empty means the application is already on its way out: the gate was closed, or the
    // operators could not be read.
    private static Optional<SignedInOperator> signIn(Storage storage) {
        Optional<SignedInOperator> operator;
        try {
            operator = SignInDialog.ask(new OperatorAccess(storage.operators()));
        } catch (RepositoryException unreadable) {
            refuseToOpen("the operators could not be read (" + unreadable.getMessage() + ")",
                    "Operators not read");
            return Optional.empty();
        }
        if (operator.isEmpty()) {
            System.exit(0);
        }
        return operator;
    }

    private static void refuseToOpen(String reason, String title) {
        JOptionPane.showMessageDialog(null, "VarZinho will not open: " + reason + ".",
                title, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void warn(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }
}
