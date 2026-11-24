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
import br.edu.unipampa.varzinho.infrastructure.persistence.StoredRecordingEditor;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import br.edu.unipampa.varzinho.infrastructure.stream.PhoneConnection;
import br.edu.unipampa.varzinho.infrastructure.spectator.SpectatorServer;
import br.edu.unipampa.varzinho.ui.SignInDialog;
import br.edu.unipampa.varzinho.ui.VarZinhoTheme;
import br.edu.unipampa.varzinho.ui.VarZinhoWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The composition root: puts the application together — storage, the saved gym, the use
 * cases and, when there is one, the phone — and hands the result to the window. It is the
 * one place that names every concrete adapter, so a new storage or a new way to reach a
 * camera changes this class, never the window.
 */
public final class Launcher {
    private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());
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
                // Storage masks the password in this message; the JDBC cause can repeat the raw URL.
                LOGGER.log(Level.SEVERE, "Database startup failed: {0}", unreachable.getMessage());
                refuseToOpen("não foi possível conectar ao banco de dados", "Banco de dados indisponível");
                return;
            }
            Optional<SignedInOperator> signedIn = signIn(storage);
            if (signedIn.isEmpty()) {
                return;
            }
            HighlightRepository repository = storage.highlights();
            List<String> warnings = new ArrayList<>();
            GymSession session = GymSession.start(storage.gym(), SampleGym.build(),
                    message -> {
                        LOGGER.warning(message);
                        warnings.add("A academia salva não pôde ser carregada.");
                    });
            Optional<String> streamUrl = Optional.empty();
            try {
                streamUrl = phoneAddress(System.getenv(STREAM_URL_VARIABLE), DATA_FOLDER.resolve("stream-url.txt"));
            } catch (IOException unreadable) {
                warnings.add("Não foi possível ler data/stream-url.txt. Conecte o celular pelo dashboard.");
            }
            LiveFeed simulation = new LiveFeed(session.gym());
            VarZinhoWindow window = window(session, signedIn.get(), repository, simulation);
            String demo = System.getenv("VARZINHO_DEMO");
            boolean publicDemo = "public".equalsIgnoreCase(demo);
            SpectatorServer spectators = startSpectatorService(warnings, publicDemo || "local".equalsIgnoreCase(demo));
            window.offerPhoneConnection(streamUrl.orElse(""), address -> {
                PhoneConnection phone;
                try { phone = PhoneConnection.start(address, SEGMENT_FOLDER, BUFFER_SECONDS); }
                catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                phone.awaitFootage();
                if (publicDemo && spectators != null) {
                    try { spectators.shareDemo(); }
                    catch (IOException failure) {
                        return () -> {
                            attachPhone(window, session.gym(), simulation, phone, spectators);
                            window.showStartupWarnings(List.of("O teste funciona neste computador. O túnel público não conectou. Confira o cloudflared e a conexão com a internet."));
                        };
                    }
                }
                return () -> attachPhone(window, session.gym(), simulation, phone, spectators);
            });
            window.showStartupWarnings(warnings);
            window.enableRecordingEditing(new StoredRecordingEditor(
                    repository, Path.of("clips")));
            window.setVisible(true);
        });
    }

    static Optional<String> phoneAddress(String override, Path file) throws IOException {
        if (override != null && !override.isBlank()) return Optional.of(override.strip());
        try { return Optional.of(Files.readString(file).strip()).filter(address -> !address.isEmpty()); }
        catch (NoSuchFileException absent) { return Optional.empty(); }
    }

    private static void attachPhone(VarZinhoWindow window, Gym gym, LiveFeed simulation, PhoneConnection phone,
                                    SpectatorServer spectators) {
        if (gym.getCourts().stream().anyMatch(court -> court.hasCamera(phone.camera().getId()))) {
            phone.disconnect();
            throw new IllegalStateException("O identificador phone-1 já está cadastrado. Confira a câmera duplicada e tente novamente.");
        }
        ConnectStreamCamera connection = new ConnectStreamCamera(gym, phone.camera());
        CourtView court = connection.connect();
        // Two feeds, so the simulated courts keep their clock whatever the phone does.
        simulation.excludeCourt(court.getNumber());
        Runnable dropped = () -> window.phoneDropped(connection::phoneDropped);
        String viewerAddress = phone.streamUrl();
        String publicView = null;
        if (spectators != null) {
            try {
                viewerAddress = spectators.broadcast(phone.liveRelay(), court.getNumber(), DATA_FOLDER.resolve("spectator/live"));
                if (viewerAddress.startsWith("https://")) publicView = viewerAddress;
            }
            catch (java.io.IOException failure) {
                window.showStartupWarnings(List.of("A câmera está conectada, mas a transmissão para espectadores não iniciou."));
            }
        }
        window.followPhone(phone.feed(dropped), court.getNumber(), viewerAddress, publicView);
        phone.onDrop(() -> SwingUtilities.invokeLater(dropped));
    }

    private static SpectatorServer startSpectatorService(List<String> warnings, boolean demo) {
        String address = demo ? null : System.getenv("VARZINHO_PUBLIC_URL");
        try {
            var spectators = SpectatorServer.open(address, System.getenv("VARZINHO_PIX_TOKEN"), DATA_FOLDER.resolve("spectator"), demo);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { spectators.close(); }
                catch (java.io.IOException failure) { LOGGER.warning("Could not clean up the spectator broadcast"); }
            }));
            return spectators;
        } catch (java.io.IOException | IllegalArgumentException failure) {
            warnings.add("Área do espectador indisponível. Confira o endereço HTTPS, o Pix e a porta local 8787.");
            return null;
        }
    }

    private static VarZinhoWindow window(GymSession session, SignedInOperator signedIn, HighlightRepository repository,
                                         Feed feed) {
        Gym gym = session.gym();
        return new VarZinhoWindow(gym.getName(), signedIn, session.manageGym(),
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
            LOGGER.log(Level.SEVERE, "Could not open the operator store", unreadable);
            refuseToOpen("não foi possível ler os operadores",
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

}
