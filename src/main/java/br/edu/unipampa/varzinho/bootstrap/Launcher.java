package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.ConnectStreamCamera;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.GymSession;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.PendingClipWrites;
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
                refuseToOpen(unreachable.getMessage(), "Banco de dados indisponível");
                return;
            }
            Optional<SignedInOperator> signedIn = signIn(storage);
            if (signedIn.isEmpty()) {
                return;
            }
            HighlightRepository repository = storage.highlights();
            GymSession session = GymSession.start(storage.gym(), SampleGym.build(),
                    message -> warn(message, "Academia salva não carregada"));
            PendingClipWrites pendingWrites = new PendingClipWrites();
            Optional<PhoneConnection> phone = PhoneConnection.open(System.getenv(STREAM_URL_VARIABLE),
                    SEGMENT_FOLDER, BUFFER_SECONDS, pendingWrites, message -> warn(message, "Celular não conectado"));
            if (phone.isEmpty()) {
                window(session, signedIn.get(), repository, pendingWrites, new LiveFeed(session.gym())).setVisible(true);
            } else {
                openWithPhone(session, signedIn.get(), repository, pendingWrites, phone.get());
            }
        });
    }

    private static void openWithPhone(GymSession session, SignedInOperator signedIn, HighlightRepository repository,
                                      PendingClipWrites pendingWrites, PhoneConnection phone) {
        Gym gym = session.gym();
        ConnectStreamCamera connection = new ConnectStreamCamera(gym, phone.camera());
        CourtView court = connection.connect();
        // Two feeds, so the simulated courts keep their clock whatever the phone does.
        VarZinhoWindow window = window(session, signedIn, repository, pendingWrites,
                new LiveFeed(gym, court.getNumber()));
        Runnable dropped = () -> {
            connection.phoneDropped();
            window.phoneDropped();
        };
        window.followPhone(phone.feed(dropped), court.getNumber(), phone.streamUrl());
        phone.onDrop(() -> SwingUtilities.invokeLater(dropped));
        window.setVisible(true);
    }

    private static VarZinhoWindow window(GymSession session, SignedInOperator signedIn,
                                         HighlightRepository repository,
                                         PendingClipWrites pendingWrites, Feed feed) {
        Gym gym = session.gym();
        return new VarZinhoWindow(gym.getName(), signedIn.getOperator(), session.manageGym(),
                new CaptureHighlight(gym, repository, pendingWrites), new BrowseArchive(repository),
                feed, BUFFER_SECONDS);
    }

    // Empty means the application is already on its way out: the gate was closed, or the
    // operators could not be read.
    private static Optional<SignedInOperator> signIn(Storage storage) {
        Optional<SignedInOperator> operator;
        try {
            operator = SignInDialog.ask(new OperatorAccess(storage.operators()));
        } catch (RepositoryException unreadable) {
            refuseToOpen("não foi possível ler os operadores (" + unreadable.getMessage() + ")",
                    "Operadores não carregados");
            return Optional.empty();
        }
        if (operator.isEmpty()) {
            System.exit(0);
        }
        return operator;
    }

    private static void refuseToOpen(String reason, String title) {
        JOptionPane.showMessageDialog(null, "O VarZinho não será aberto: " + reason + ".",
                title, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void warn(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
    }
}
